// RevenueCat 웹훅을 검증하고 결제 이력을 저장한 뒤 이벤트 타입에 따라 사용자 구독 상태를 갱신한다.

package com.landit.landitbe.feature.subscription.service;

import com.landit.landitbe.config.subscription.RevenueCatProperties;
import com.landit.landitbe.feature.profile.domain.SubscriptionPeriodType;
import com.landit.landitbe.feature.profile.domain.SubscriptionStatus;
import com.landit.landitbe.feature.profile.domain.SubscriptionStore;
import com.landit.landitbe.feature.profile.dto.SubscriptionUpdateCommand;
import com.landit.landitbe.feature.profile.dto.SubscriptionUpdateResult;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import com.landit.landitbe.feature.subscription.domain.SubscriptionEvent;
import com.landit.landitbe.feature.subscription.domain.SubscriptionEventType;
import com.landit.landitbe.feature.subscription.dto.RevenueCatWebhookEvent;
import com.landit.landitbe.feature.subscription.dto.RevenueCatWebhookRequest;
import com.landit.landitbe.feature.subscription.exception.SubscriptionErrorCode;
import com.landit.landitbe.feature.subscription.exception.SubscriptionException;
import com.landit.landitbe.feature.subscription.repository.SubscriptionEventRepository;
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
import org.springframework.transaction.annotation.Transactional;

/** RevenueCat 웹훅을 검증하고 결제 이력을 저장한 뒤 이벤트 타입에 따라 사용자 구독 상태를 갱신한다. */
@Slf4j
@Service
@RequiredArgsConstructor
public class RevenueCatWebhookService {

  /** RevenueCat은 환불을 별도 이벤트 대신 이 해지 사유를 가진 CANCELLATION으로 보낸다. */
  private static final String CANCEL_REASON_REFUND = "CUSTOMER_SUPPORT";

  private final RevenueCatProperties revenueCatProperties;
  private final UserProfileService userProfileService;
  private final SubscriptionEventRepository subscriptionEventRepository;
  private final Clock clock;

  /**
   * Authorization 헤더를 검증한 뒤 웹훅 이벤트를 결제 이력으로 저장하고 사용자 구독 상태에 반영한다.
   *
   * <p>이력 저장과 상태 갱신은 한 트랜잭션으로 묶어 한쪽만 반영되지 않게 한다. 이력·상태 모두와 무관한 이벤트 타입, Landit 사용자와 연결할 수 없는 이벤트, 이미
   * 저장한 이벤트 ID의 재전송은 로그만 남기고 정상 처리로 응답해 RevenueCat이 재시도하지 않게 한다.
   *
   * @param authorization 요청의 Authorization 헤더 값. 없으면 null
   * @param request 웹훅 요청 본문
   * @throws SubscriptionException Authorization 헤더가 설정값과 다르거나 설정값이 비어 있을 때
   */
  @Transactional
  public void handle(String authorization, RevenueCatWebhookRequest request) {
    verifyAuthorization(authorization);
    RevenueCatWebhookEvent event = request.event();
    Optional<SubscriptionEventType> eventType = SubscriptionEventType.fromRevenueCat(event.type());
    Optional<SubscriptionStatus> targetStatus = resolveTargetStatus(event);
    if (eventType.isEmpty() && targetStatus.isEmpty()) {
      log.info("RevenueCat 웹훅 무시: 구독 상태와 무관한 이벤트. eventId={}, type={}", event.id(), event.type());
      return;
    }
    Optional<Long> userId = resolveUserId(event);
    if (userId.isEmpty()) {
      return;
    }
    if (event.id() != null && subscriptionEventRepository.existsByEventId(event.id())) {
      log.info(
          "RevenueCat 웹훅 무시: 이미 저장한 이벤트의 재전송. eventId={}, type={}, userId={}",
          event.id(),
          event.type(),
          userId.get());
      return;
    }
    eventType.ifPresent(type -> saveEvent(event, type, userId.get()));
    targetStatus.ifPresent(status -> applyToUser(event, status, userId.get()));
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
   * 아니라 즉시 종료(EXPIRED)로 처리한다. BILLING_ISSUE와 PRODUCT_CHANGE는 결제 이력으로만 남기고 상태는 바꾸지 않는다.
   */
  private static Optional<SubscriptionStatus> resolveTargetStatus(RevenueCatWebhookEvent event) {
    return SubscriptionEventType.fromRevenueCat(event.type())
        .flatMap(
            type ->
                switch (type) {
                  case INITIAL_PURCHASE, RENEWAL, UNCANCELLATION ->
                      Optional.of(SubscriptionStatus.ACTIVE);
                  case CANCELLATION ->
                      Optional.of(
                          CANCEL_REASON_REFUND.equals(event.cancelReason())
                              ? SubscriptionStatus.EXPIRED
                              : SubscriptionStatus.CANCELED);
                  case EXPIRATION -> Optional.of(SubscriptionStatus.EXPIRED);
                  case BILLING_ISSUE, PRODUCT_CHANGE -> Optional.empty();
                });
  }

  /** App User ID 후보 가운데 실제로 존재하는 Landit 사용자를 찾는다. 없으면 경고를 남기고 빈 값을 반환한다. */
  private Optional<Long> resolveUserId(RevenueCatWebhookEvent event) {
    List<Long> candidateUserIds = resolveCandidateUserIds(event);
    if (candidateUserIds.isEmpty()) {
      log.warn(
          "RevenueCat 웹훅 무시: Landit 사용자 ID로 해석할 수 없는 app_user_id. eventId={}, type={},"
              + " appUserId={}",
          event.id(),
          event.type(),
          event.appUserId());
      return Optional.empty();
    }
    Optional<Long> userId = userProfileService.findExistingUserId(candidateUserIds);
    if (userId.isEmpty()) {
      log.warn(
          "RevenueCat 웹훅 무시: 일치하는 사용자 프로필이 없다. eventId={}, type={}, candidateUserIds={}",
          event.id(),
          event.type(),
          candidateUserIds);
    }
    return userId;
  }

  /** 이벤트를 결제 이력으로 저장한다. 이벤트 ID가 없으면 중복을 막을 수 없으므로 경고만 남기고 저장하지 않는다. */
  private void saveEvent(RevenueCatWebhookEvent event, SubscriptionEventType type, Long userId) {
    if (event.id() == null) {
      log.warn(
          "RevenueCat 웹훅 이력 저장 생략: 이벤트 ID가 없다. type={}, userId={}, eventTimestampMs={}",
          event.type(),
          userId,
          event.eventTimestampMs());
      return;
    }
    LocalDateTime occurredAt =
        toLocalDateTime(event.purchasedAtMs())
            .or(() -> toLocalDateTime(event.eventTimestampMs()))
            .orElseGet(() -> LocalDateTime.now(clock));
    subscriptionEventRepository.save(
        SubscriptionEvent.record(
            event.id(),
            userId,
            type,
            event.productId(),
            resolvePeriodType(event),
            event.priceInPurchasedCurrency(),
            event.currency(),
            resolveStore(event),
            event.environment(),
            event.cancelReason(),
            occurredAt,
            toLocalDateTime(event.expirationAtMs()).orElse(null)));
  }

  private void applyToUser(
      RevenueCatWebhookEvent event, SubscriptionStatus targetStatus, Long userId) {
    SubscriptionUpdateCommand command = toCommand(event, targetStatus);
    SubscriptionUpdateResult result = userProfileService.updateSubscription(userId, command);
    log.info(
        "RevenueCat 웹훅 처리: result={}, userId={}, status={}, periodType={}, productId={}, store={},"
            + " eventId={}, type={}, environment={}",
        result,
        userId,
        command.status(),
        command.periodType(),
        command.productId(),
        command.store(),
        event.id(),
        event.type(),
        event.environment());
  }

  /** 프리미엄이 꺼지는 상태에서는 기간 종류·만료 시각·상품·스토어를 비워 응답에서 남은 구독처럼 보이지 않게 한다. */
  private SubscriptionUpdateCommand toCommand(
      RevenueCatWebhookEvent event, SubscriptionStatus targetStatus) {
    LocalDateTime eventAt =
        toLocalDateTime(event.eventTimestampMs()).orElseGet(() -> LocalDateTime.now(clock));
    if (!targetStatus.isPremium()) {
      return new SubscriptionUpdateCommand(targetStatus, null, null, eventAt, null, null);
    }
    return new SubscriptionUpdateCommand(
        targetStatus,
        resolvePeriodType(event),
        toLocalDateTime(event.expirationAtMs()).orElse(null),
        eventAt,
        event.productId(),
        resolveStore(event));
  }

  /** RevenueCat period_type을 기간 종류로 바꾼다. 알 수 없는 값은 경고를 남기고 null로 저장해 처리는 계속 진행한다. */
  private static SubscriptionPeriodType resolvePeriodType(RevenueCatWebhookEvent event) {
    Optional<SubscriptionPeriodType> periodType =
        SubscriptionPeriodType.fromRevenueCat(event.periodType());
    if (periodType.isEmpty() && event.periodType() != null) {
      log.warn(
          "RevenueCat 웹훅 period_type 해석 실패: 알 수 없는 값이라 기간 종류를 비운다. eventId={}, periodType={}",
          event.id(),
          event.periodType());
    }
    return periodType.orElse(null);
  }

  /** RevenueCat store를 스토어로 바꾼다. 알 수 없는 값은 경고를 남기고 null로 저장해 처리는 계속 진행한다. */
  private static SubscriptionStore resolveStore(RevenueCatWebhookEvent event) {
    Optional<SubscriptionStore> store = SubscriptionStore.fromRevenueCat(event.store());
    if (store.isEmpty() && event.store() != null) {
      log.warn(
          "RevenueCat 웹훅 store 해석 실패: 알 수 없는 값이라 스토어를 비운다. eventId={}, store={}",
          event.id(),
          event.store());
    }
    return store.orElse(null);
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
