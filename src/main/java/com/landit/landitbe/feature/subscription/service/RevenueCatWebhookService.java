// RevenueCat 웹훅을 검증하고 이벤트 타입에 따라 사용자 구독 상태를 갱신한다.

package com.landit.landitbe.feature.subscription.service;

import com.landit.landitbe.config.subscription.RevenueCatProperties;
import com.landit.landitbe.feature.profile.domain.SubscriptionStatus;
import com.landit.landitbe.feature.profile.dto.SubscriptionUpdateResult;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import com.landit.landitbe.feature.subscription.dto.RevenueCatWebhookEvent;
import com.landit.landitbe.feature.subscription.dto.RevenueCatWebhookRequest;
import com.landit.landitbe.feature.subscription.exception.SubscriptionErrorCode;
import com.landit.landitbe.feature.subscription.exception.SubscriptionException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** RevenueCat 웹훅을 검증하고 이벤트 타입에 따라 사용자 구독 상태를 갱신한다. */
@Slf4j
@Service
@RequiredArgsConstructor
public class RevenueCatWebhookService {

  private static final String EVENT_INITIAL_PURCHASE = "INITIAL_PURCHASE";
  private static final String EVENT_RENEWAL = "RENEWAL";
  private static final String EVENT_UNCANCELLATION = "UNCANCELLATION";
  private static final String EVENT_CANCELLATION = "CANCELLATION";
  private static final String EVENT_EXPIRATION = "EXPIRATION";

  /** RevenueCat은 환불을 별도 이벤트 대신 이 해지 사유를 가진 CANCELLATION으로 보낸다. */
  private static final String CANCEL_REASON_REFUND = "CUSTOMER_SUPPORT";

  private final RevenueCatProperties revenueCatProperties;
  private final UserProfileService userProfileService;
  private final Clock clock;

  /**
   * Authorization 헤더를 검증한 뒤 웹훅 이벤트를 사용자 구독 상태에 반영한다.
   *
   * <p>구독 상태와 무관한 이벤트 타입이나 Landit 사용자와 연결할 수 없는 이벤트는 로그만 남기고 정상 처리로 응답해 RevenueCat이 재시도하지 않게 한다.
   *
   * @param authorization 요청의 Authorization 헤더 값. 없으면 null
   * @param request 웹훅 요청 본문
   * @throws SubscriptionException Authorization 헤더가 설정값과 다르거나 설정값이 비어 있을 때
   */
  public void handle(String authorization, RevenueCatWebhookRequest request) {
    verifyAuthorization(authorization);
    RevenueCatWebhookEvent event = request.event();
    Optional<SubscriptionStatus> targetStatus = resolveTargetStatus(event);
    if (targetStatus.isEmpty()) {
      log.info("RevenueCat 웹훅 무시: 구독 상태와 무관한 이벤트. eventId={}, type={}", event.id(), event.type());
      return;
    }
    applyToUser(event, targetStatus.get());
  }

  private void verifyAuthorization(String authorization) {
    if (!revenueCatProperties.hasWebhookAuthorization()) {
      log.warn("RevenueCat 웹훅 거절: LANDIT_REVENUECAT_WEBHOOK_AUTHORIZATION이 설정되지 않았다.");
      throw new SubscriptionException(SubscriptionErrorCode.WEBHOOK_UNAUTHORIZED);
    }
    if (authorization == null
        || !constantTimeEquals(authorization, revenueCatProperties.webhookAuthorization())) {
      log.warn("RevenueCat 웹훅 거절: Authorization 헤더가 설정값과 다르다.");
      throw new SubscriptionException(SubscriptionErrorCode.WEBHOOK_UNAUTHORIZED);
    }
  }

  private static boolean constantTimeEquals(String actual, String expected) {
    return MessageDigest.isEqual(
        actual.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * 이벤트 타입을 목표 구독 상태로 변환한다. 구독 상태와 무관한 타입이면 빈 값을 반환한다.
   *
   * <p>환불은 RevenueCat이 별도 이벤트 대신 cancel_reason이 CUSTOMER_SUPPORT인 CANCELLATION으로 보내므로, 이 경우 해지 예약이
   * 아니라 즉시 종료(EXPIRED)로 처리한다.
   */
  private static Optional<SubscriptionStatus> resolveTargetStatus(RevenueCatWebhookEvent event) {
    return switch (event.type()) {
      case EVENT_INITIAL_PURCHASE, EVENT_RENEWAL, EVENT_UNCANCELLATION ->
          Optional.of(SubscriptionStatus.ACTIVE);
      case EVENT_CANCELLATION ->
          Optional.of(
              CANCEL_REASON_REFUND.equals(event.cancelReason())
                  ? SubscriptionStatus.EXPIRED
                  : SubscriptionStatus.CANCELED);
      case EVENT_EXPIRATION -> Optional.of(SubscriptionStatus.EXPIRED);
      default -> Optional.empty();
    };
  }

  private void applyToUser(RevenueCatWebhookEvent event, SubscriptionStatus targetStatus) {
    List<Long> candidateUserIds = resolveCandidateUserIds(event);
    if (candidateUserIds.isEmpty()) {
      log.warn(
          "RevenueCat 웹훅 무시: Landit 사용자 ID로 해석할 수 없는 app_user_id. eventId={}, type={},"
              + " appUserId={}",
          event.id(),
          event.type(),
          event.appUserId());
      return;
    }
    LocalDateTime eventAt =
        toLocalDateTime(event.eventTimestampMs()).orElseGet(() -> LocalDateTime.now(clock));
    // 프리미엄이 꺼지는 상태에서는 만료 시각을 비워 응답에서 남은 기간처럼 보이지 않게 한다.
    LocalDateTime expiresAt =
        targetStatus.isPremium() ? toLocalDateTime(event.expirationAtMs()).orElse(null) : null;
    for (Long userId : candidateUserIds) {
      SubscriptionUpdateResult result =
          userProfileService.updateSubscription(userId, targetStatus, expiresAt, eventAt);
      if (result != SubscriptionUpdateResult.USER_NOT_FOUND) {
        log.info(
            "RevenueCat 웹훅 처리: result={}, userId={}, status={}, eventId={}, type={},"
                + " environment={}",
            result,
            userId,
            targetStatus,
            event.id(),
            event.type(),
            event.environment());
        return;
      }
    }
    log.warn(
        "RevenueCat 웹훅 무시: 일치하는 사용자 프로필이 없다. eventId={}, type={}, candidateUserIds={}",
        event.id(),
        event.type(),
        candidateUserIds);
  }

  /** App User ID, original App User ID, aliases 순으로 숫자 형태의 Landit 사용자 ID 후보를 모은다. */
  private static List<Long> resolveCandidateUserIds(RevenueCatWebhookEvent event) {
    List<String> aliases = event.aliases() == null ? List.of() : event.aliases();
    return Stream.concat(Stream.of(event.appUserId(), event.originalAppUserId()), aliases.stream())
        .filter(Objects::nonNull)
        .map(RevenueCatWebhookService::parseUserId)
        .flatMap(Optional::stream)
        .distinct()
        .toList();
  }

  private static Optional<Long> parseUserId(String appUserId) {
    try {
      return Optional.of(Long.parseLong(appUserId.trim()));
    } catch (NumberFormatException exception) {
      // RevenueCat 익명 ID($RCAnonymousID:...)처럼 숫자가 아닌 값은 후보에서 제외한다.
      return Optional.empty();
    }
  }

  private Optional<LocalDateTime> toLocalDateTime(Long epochMillis) {
    if (epochMillis == null) {
      return Optional.empty();
    }
    return Optional.of(LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), clock.getZone()));
  }
}
