// RevenueCat 웹훅을 검증하고 결제 이력을 저장한 뒤 이벤트 타입에 따라 사용자 구독 상태를 갱신하거나 계정 간에 옮긴다.

package com.landit.landitbe.feature.subscription.event.service;

import com.landit.landitbe.config.subscription.RevenueCatProperties;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import com.landit.landitbe.feature.profile.subscription.domain.SubscriptionPeriodType;
import com.landit.landitbe.feature.profile.subscription.domain.SubscriptionStatus;
import com.landit.landitbe.feature.profile.subscription.domain.SubscriptionStore;
import com.landit.landitbe.feature.profile.subscription.dto.SubscriptionExpiryExtensionResult;
import com.landit.landitbe.feature.profile.subscription.dto.SubscriptionTransferResult;
import com.landit.landitbe.feature.profile.subscription.dto.SubscriptionUpdateCommand;
import com.landit.landitbe.feature.profile.subscription.dto.SubscriptionUpdateResult;
import com.landit.landitbe.feature.profile.subscription.dto.UserSubscriptionSnapshot;
import com.landit.landitbe.feature.profile.subscription.service.ProfileSubscriptionService;
import com.landit.landitbe.feature.subscription.event.domain.SubscriptionEvent;
import com.landit.landitbe.feature.subscription.event.domain.SubscriptionEventType;
import com.landit.landitbe.feature.subscription.event.dto.RevenueCatWebhookEvent;
import com.landit.landitbe.feature.subscription.event.dto.RevenueCatWebhookRequest;
import com.landit.landitbe.feature.subscription.event.dto.SubscriptionChangedEvent;
import com.landit.landitbe.feature.subscription.event.repository.SubscriptionEventRepository;
import com.landit.landitbe.feature.subscription.exception.SubscriptionErrorCode;
import com.landit.landitbe.feature.subscription.exception.SubscriptionException;
import com.landit.landitbe.shared.observability.FailureObservation;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RevenueCat 웹훅을 검증하고 결제 이력을 저장한 뒤 이벤트 타입에 따라 사용자 구독 상태를 갱신하거나 계정 간에 옮긴다. */
@Slf4j
@Service
@RequiredArgsConstructor
public class RevenueCatWebhookService {

  /** RevenueCat은 환불을 별도 이벤트 대신 이 해지 사유를 가진 CANCELLATION으로 보낸다. */
  private static final String CANCEL_REASON_REFUND = "CUSTOMER_SUPPORT";

  /**
   * 스토어가 갱신 결제 실패를 알릴 때 BILLING_ISSUE와 함께 보내는 CANCELLATION의 해지 사유. 자동 갱신은 켜진 채 스토어가 재시도하므로 구독 상태를
   * 바꾸지 않는다.
   */
  private static final String CANCEL_REASON_BILLING_ERROR = "BILLING_ERROR";

  private final RevenueCatProperties revenueCatProperties;
  private final UserProfileService userProfileService;
  private final ProfileSubscriptionService profileSubscriptionService;
  private final SubscriptionEventRepository subscriptionEventRepository;
  private final Clock clock;
  private final ApplicationEventPublisher eventPublisher;

  /**
   * Authorization 헤더를 검증한 뒤 웹훅 이벤트를 결제 이력으로 저장하고 사용자 구독 상태에 반영한다.
   *
   * <p>이력 저장과 상태 갱신은 한 트랜잭션으로 묶어 한쪽만 반영되지 않게 한다. 이력·상태 모두와 무관한 이벤트 타입, Landit 사용자와 연결할 수 없는 이벤트, 이미
   * 저장한 이벤트 ID의 재전송은 로그만 남기고 정상 처리로 응답해 RevenueCat이 재시도하지 않게 한다. TRANSFER는 app_user_id 대신
   * transferred_from·transferred_to로 계정을 찾아 구독 상태를 옮긴다. 인증을 통과한 웹훅은 처리 전에 API 버전과 함께 수신 로그를 남기고,
   * 샌드박스 이벤트 반영이 꺼져 있으면 SANDBOX 이벤트는 로그만 남기고 끝낸다. BILLING_ISSUE는 이력을 남기고, 결제 유예 종료 시각이 있으면 프리미엄
   * 사용자의 만료 시각만 그 시각까지 늘린다.
   *
   * @param authorization 요청의 Authorization 헤더 값. 없으면 null
   * @param request 웹훅 요청 본문
   * @throws SubscriptionException Authorization 헤더가 설정값과 다를 때
   * @throws com.landit.landitbe.shared.exception.ApiException 서버 인증 설정이 누락됐을 때
   */
  @Transactional
  public void handle(String authorization, RevenueCatWebhookRequest request) {
    verifyAuthorization(authorization);
    RevenueCatWebhookEvent event = request.event();
    log.info("RevenueCat webhook received");
    if (!revenueCatProperties.applySandboxEvents() && "SANDBOX".equals(event.environment())) {
      log.info("RevenueCat sandbox event ignored");
      return;
    }
    Optional<SubscriptionEventType> eventType = SubscriptionEventType.fromRevenueCat(event.type());
    if (eventType.filter(SubscriptionEventType.TRANSFER::equals).isPresent()) {
      handleTransfer(event);
      return;
    }
    Optional<SubscriptionStatus> targetStatus = resolveTargetStatus(event);
    if (eventType.isEmpty() && targetStatus.isEmpty()) {
      FailureObservation.observed(
          "subscription_webhook", "routing", "unsupported_event", "expected_rejection");
      return;
    }
    Optional<Long> userId = resolveUserId(event);
    if (userId.isEmpty()) {
      return;
    }
    if (subscriptionEventRepository.existsByEventId(event.id())) {
      log.info("RevenueCat duplicate event ignored");
      return;
    }
    applyEvent(event, eventType, targetStatus, userId.get());
  }

  /**
   * 이벤트를 결제 이력으로 저장하고 사용자 구독 상태에 반영한다.
   *
   * <p>BILLING_ISSUE는 목표 상태가 없어 상태 갱신 대신 결제 유예 반영만 거친다.
   *
   * @param event 웹훅 이벤트
   * @param eventType 결제 이력 이벤트 타입. 이력으로 남기지 않는 타입이면 빈 값
   * @param targetStatus 목표 구독 상태. 구독 상태와 무관한 타입이면 빈 값
   * @param userId 반영할 사용자 ID
   */
  private void applyEvent(
      RevenueCatWebhookEvent event,
      Optional<SubscriptionEventType> eventType,
      Optional<SubscriptionStatus> targetStatus,
      Long userId) {
    eventType.ifPresent(type -> saveEvent(event, type, userId));
    targetStatus.ifPresent(status -> applyToUser(event, status, userId));
    if (eventType.filter(SubscriptionEventType.BILLING_ISSUE::equals).isPresent()) {
      applyGracePeriod(event, userId);
    }
  }

  /**
   * BILLING_ISSUE의 결제 유예 종료 시각까지 사용자 구독 만료 시각을 늘린다.
   *
   * <p>grace_period_expiration_at_ms가 없으면(스토어 유예 기간 미설정, Google 계정 보류 등) 아무것도 바꾸지 않는다. 구독 상태 전환이
   * 아니므로 구독 변경 이벤트는 발행하지 않는다. 체험 전환 결제가 실패한 경우에도 유예 종료 시각을 체험 종료로 알리면 안 되므로 체험 알림은 다시 예약하지 않는다. 결제가
   * 복구되면 RevenueCat이 RENEWAL을 보내고, 유예가 끝나도 복구되지 않으면 EXPIRATION을 보낸다.
   *
   * @param event BILLING_ISSUE 웹훅 이벤트
   * @param userId 반영할 사용자 ID
   */
  private void applyGracePeriod(RevenueCatWebhookEvent event, Long userId) {
    Optional<LocalDateTime> graceExpiresAt = toLocalDateTime(event.gracePeriodExpirationAtMs());
    if (graceExpiresAt.isEmpty()) {
      log.info(
          "RevenueCat 웹훅 처리: BILLING_ISSUE에 유예 종료 시각이 없어 만료 시각을 유지한다. userId={}, eventId={}",
          userId,
          event.id());
      return;
    }
    LocalDateTime eventAt =
        toLocalDateTime(event.eventTimestampMs()).orElseGet(() -> LocalDateTime.now(clock));
    SubscriptionExpiryExtensionResult result =
        profileSubscriptionService.extendSubscriptionExpiry(userId, graceExpiresAt.get(), eventAt);
    log.info(
        "RevenueCat 웹훅 처리: BILLING_ISSUE 유예 반영 result={}, userId={}, graceExpiresAt={}, eventId={},"
            + " environment={}",
        result,
        userId,
        graceExpiresAt.get(),
        event.id(),
        event.environment());
  }

  /**
   * Authorization 헤더가 설정된 웹훅 인증값과 같은지 확인한다.
   *
   * @param authorization 요청의 Authorization 헤더 값. 없으면 null
   * @throws SubscriptionException 헤더가 설정값과 다를 때
   * @throws com.landit.landitbe.shared.exception.ApiException 서버 인증 설정이 누락됐을 때
   */
  private void verifyAuthorization(String authorization) {
    if (!revenueCatProperties.hasWebhookAuthorization()) {
      var exception =
          new com.landit.landitbe.shared.exception.ApiException(
              com.landit.landitbe.shared.exception.ErrorCode.SERVICE_UNAVAILABLE);
      FailureObservation.failed(
          "subscription_webhook", "configuration", "authentication_not_configured", exception);
      throw exception;
    }
    if (authorization == null
        || !constantTimeEquals(authorization, revenueCatProperties.webhookAuthorization())) {
      FailureObservation.observed(
          "subscription_webhook", "authentication", "invalid_credentials", "expected_rejection");
      throw new SubscriptionException(SubscriptionErrorCode.WEBHOOK_UNAUTHORIZED);
    }
  }

  /**
   * 두 문자열을 길이와 무관하게 일정한 시간으로 비교한다. 비교 시간 차이로 인증값을 추측하는 타이밍 공격을 막는다.
   *
   * @param actual 요청으로 받은 값
   * @param expected 설정된 기댓값
   * @return 두 값이 같으면 {@code true}
   */
  private static boolean constantTimeEquals(String actual, String expected) {
    return MessageDigest.isEqual(
        actual.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * 이벤트 타입을 목표 구독 상태로 변환한다. 구독 상태와 무관한 타입이면 빈 값을 반환한다.
   *
   * <p>환불은 RevenueCat이 별도 이벤트 대신 cancel_reason이 CUSTOMER_SUPPORT인 CANCELLATION으로 보내므로, 이 경우 해지 예약이
   * 아니라 즉시 종료(EXPIRED)로 처리한다. cancel_reason이 BILLING_ERROR인 CANCELLATION은 갱신 결제 실패와 함께 오는 스토어 재시도
   * 알림이라 이력만 남기고 상태는 유지한다. 실제 종료는 유예가 끝난 뒤 EXPIRATION(expiration_reason=BILLING_ERROR)으로 온다.
   * BILLING_ISSUE와 PRODUCT_CHANGE는 결제 이력으로만 남기고 상태는 바꾸지 않는다. 단, BILLING_ISSUE에 유예 종료 시각이 있으면 만료 시각만
   * 늘린다. NON_RENEWING_PURCHASE는 RevenueCat 대시보드에서 프로모션 권한을 부여했을 때 오므로 구매와 같이 ACTIVE로 처리하고, 만료는 다른
   * 구독처럼 EXPIRATION으로 온다.
   *
   * @param event 웹훅 이벤트
   * @return 목표 구독 상태. 구독 상태와 무관한 타입이면 빈 값
   */
  private static Optional<SubscriptionStatus> resolveTargetStatus(RevenueCatWebhookEvent event) {
    return SubscriptionEventType.fromRevenueCat(event.type())
        .flatMap(
            type ->
                switch (type) {
                  case INITIAL_PURCHASE, RENEWAL, UNCANCELLATION, NON_RENEWING_PURCHASE ->
                      Optional.of(SubscriptionStatus.ACTIVE);
                  case CANCELLATION -> resolveCancellationStatus(event.cancelReason());
                  case EXPIRATION -> Optional.of(SubscriptionStatus.EXPIRED);
                  case BILLING_ISSUE, PRODUCT_CHANGE, TRANSFER -> Optional.empty();
                });
  }

  /**
   * CANCELLATION 이벤트의 해지 사유를 목표 구독 상태로 변환한다.
   *
   * @param cancelReason RevenueCat cancel_reason 값. 없으면 null
   * @return 환불이면 EXPIRED, 갱신 결제 실패면 빈 값(상태 유지), 그 외에는 CANCELED
   */
  private static Optional<SubscriptionStatus> resolveCancellationStatus(String cancelReason) {
    if (CANCEL_REASON_BILLING_ERROR.equals(cancelReason)) {
      return Optional.empty();
    }
    return Optional.of(
        CANCEL_REASON_REFUND.equals(cancelReason)
            ? SubscriptionStatus.EXPIRED
            : SubscriptionStatus.CANCELED);
  }

  /**
   * 구독 이전 이벤트(Transfer)를 반영한다.
   *
   * <p>넘겨준 계정과 넘겨받은 계정을 각각 App User ID 목록에서 찾고, 프로필 기능에 상태 이전을 맡긴다. 이전이 반영되면 넘겨받은 계정의 이력에만
   * TRANSFER를 남긴다. 어느 한쪽 계정을 찾지 못하면 RevenueCat이 이후 실제 구독 이벤트를 새 계정으로 보내므로 로그만 남기고 끝낸다.
   *
   * @param event TRANSFER 웹훅 이벤트
   */
  private void handleTransfer(RevenueCatWebhookEvent event) {
    if (subscriptionEventRepository.existsByEventId(event.id())) {
      log.debug("RevenueCat duplicate transfer ignored");
      return;
    }

    Optional<Long> fromUserId = findExistingUserId(event.transferredFrom());
    Optional<Long> toUserId = findExistingUserId(event.transferredTo());
    if (fromUserId.isEmpty() || toUserId.isEmpty()) {
      logUnresolvedTransfer(event);
      return;
    }

    LocalDateTime eventAt =
        toLocalDateTime(event.eventTimestampMs()).orElseGet(() -> LocalDateTime.now(clock));
    SubscriptionTransferResult transfer =
        profileSubscriptionService.transferSubscription(fromUserId.get(), toUserId.get(), eventAt);

    boolean saved = transfer.moved() != null;
    if (saved) {
      saveTransferEvent(event, toUserId.get(), transfer.moved(), eventAt);
      eventPublisher.publishEvent(
          new SubscriptionChangedEvent(toUserId.get(), event.environment()));
    }
    logTransfer(event, fromUserId.get(), toUserId.get(), transfer, saved);
  }

  /**
   * TRANSFER의 넘겨준 계정이나 넘겨받은 계정을 찾지 못해 무시한 사실을 남긴다.
   *
   * @param event TRANSFER 웹훅 이벤트
   */
  private void logUnresolvedTransfer(RevenueCatWebhookEvent event) {
    FailureObservation.observed(
        "subscription_webhook", "identity", "transfer_identity_unresolved", "expected_rejection");
  }

  /**
   * 넘겨받은 계정의 이력에 TRANSFER를 남긴다. 구독 상세는 이벤트에 없으므로 넘겨준 계정에서 복사한 값을 쓴다.
   *
   * @param event TRANSFER 웹훅 이벤트
   * @param toUserId 구독을 넘겨받은 사용자 ID
   * @param moved 넘겨준 계정에서 복사한 구독 정보
   * @param eventAt 이벤트 발생 시각
   */
  private void saveTransferEvent(
      RevenueCatWebhookEvent event,
      Long toUserId,
      UserSubscriptionSnapshot moved,
      LocalDateTime eventAt) {
    subscriptionEventRepository.save(
        SubscriptionEvent.record(
            event.id(),
            toUserId,
            SubscriptionEventType.TRANSFER,
            moved.productId(),
            moved.periodType(),
            null,
            null,
            Optional.ofNullable(resolveStore(event)).orElse(moved.store()),
            event.environment(),
            null,
            eventAt,
            moved.expiresAt()));
  }

  /**
   * TRANSFER 처리 결과와 결제 이력 저장 여부를 남긴다.
   *
   * @param event TRANSFER 웹훅 이벤트
   * @param fromUserId 구독을 넘겨준 사용자 ID
   * @param toUserId 구독을 넘겨받은 사용자 ID
   * @param transfer 구독 이전 처리 결과
   * @param saved 넘겨받은 계정에 TRANSFER 결제 이력을 저장했는지
   */
  private void logTransfer(
      RevenueCatWebhookEvent event,
      Long fromUserId,
      Long toUserId,
      SubscriptionTransferResult transfer,
      boolean saved) {
    log.info("RevenueCat transfer result={} saved={}", transfer.result(), saved);
  }

  /**
   * App User ID 목록에서 숫자 형태의 Landit 사용자 ID만 추려 실제로 존재하는 첫 사용자를 찾는다.
   *
   * @param appUserIds RevenueCat이 보낸 App User ID 목록. 익명 ID($RCAnonymousID:...)가 섞일 수 있고 null도 허용한다
   * @return 목록 순서상 처음으로 존재하는 Landit 사용자 ID. 없으면 빈 값
   */
  private Optional<Long> findExistingUserId(List<String> appUserIds) {
    if (appUserIds == null) {
      return Optional.empty();
    }
    List<Long> candidateUserIds =
        appUserIds.stream()
            .filter(Objects::nonNull)
            .map(RevenueCatWebhookService::parseUserId)
            .flatMap(Optional::stream)
            .distinct()
            .toList();
    return userProfileService.findExistingUserId(candidateUserIds);
  }

  /**
   * App User ID 후보 가운데 실제로 존재하는 Landit 사용자를 찾는다. 없으면 경고를 남기고 빈 값을 반환한다.
   *
   * @param event 웹훅 이벤트
   * @return 존재하는 Landit 사용자 ID. 없으면 빈 값
   */
  private Optional<Long> resolveUserId(RevenueCatWebhookEvent event) {
    List<Long> candidateUserIds = resolveCandidateUserIds(event);
    if (candidateUserIds.isEmpty()) {
      FailureObservation.observed(
          "subscription_webhook", "identity", "invalid_user_id", "expected_rejection");
      return Optional.empty();
    }
    Optional<Long> userId = userProfileService.findExistingUserId(candidateUserIds);
    if (userId.isEmpty()) {
      FailureObservation.observed(
          "subscription_webhook", "identity", "user_not_found", "expected_rejection");
    }
    return userId;
  }

  /**
   * 이벤트를 결제 이력으로 저장한다. 발생 시각은 결제 시각을 우선하고, 없으면 이벤트 생성 시각을 쓴다.
   *
   * @param event 웹훅 이벤트
   * @param type 결제 이력 이벤트 타입
   * @param userId 이력을 남길 사용자 ID
   */
  private void saveEvent(RevenueCatWebhookEvent event, SubscriptionEventType type, Long userId) {
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

  /**
   * 사용자 구독 상태를 목표 상태로 갱신하고 처리 결과를 남긴다.
   *
   * @param event 웹훅 이벤트
   * @param targetStatus 목표 구독 상태
   * @param userId 갱신할 사용자 ID
   */
  private void applyToUser(
      RevenueCatWebhookEvent event, SubscriptionStatus targetStatus, Long userId) {
    SubscriptionUpdateCommand command = toCommand(event, targetStatus);
    SubscriptionUpdateResult result =
        profileSubscriptionService.updateSubscription(userId, command);
    if (result == SubscriptionUpdateResult.APPLIED) {
      eventPublisher.publishEvent(new SubscriptionChangedEvent(userId, event.environment()));
    }
    log.info("RevenueCat webhook result={} status={}", result, command.status());
  }

  /**
   * 웹훅 이벤트를 구독 상태 갱신 명령으로 바꾼다.
   *
   * <p>프리미엄이 꺼지는 상태에서는 기간 종류·만료 시각·상품·스토어를 비워 응답에서 남은 구독처럼 보이지 않게 한다.
   *
   * @param event 웹훅 이벤트
   * @param targetStatus 목표 구독 상태
   * @return 구독 상태 갱신 명령
   */
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

  /**
   * RevenueCat period_type을 기간 종류로 바꾼다. 알 수 없는 값은 경고를 남기고 null로 저장해 처리는 계속 진행한다.
   *
   * @param event 웹훅 이벤트
   * @return 기간 종류. 값이 없거나 알 수 없으면 null
   */
  private static SubscriptionPeriodType resolvePeriodType(RevenueCatWebhookEvent event) {
    Optional<SubscriptionPeriodType> periodType =
        SubscriptionPeriodType.fromRevenueCat(event.periodType());
    if (periodType.isEmpty() && event.periodType() != null) {
      FailureObservation.observed(
          "subscription_webhook", "normalization", "unknown_period_type", "recovered");
    }
    return periodType.orElse(null);
  }

  /**
   * RevenueCat store를 스토어로 바꾼다. 알 수 없는 값은 경고를 남기고 null로 저장해 처리는 계속 진행한다.
   *
   * @param event 웹훅 이벤트
   * @return 스토어. 값이 없거나 알 수 없으면 null
   */
  private static SubscriptionStore resolveStore(RevenueCatWebhookEvent event) {
    Optional<SubscriptionStore> store = SubscriptionStore.fromRevenueCat(event.store());
    if (store.isEmpty() && event.store() != null) {
      FailureObservation.observed(
          "subscription_webhook", "normalization", "unknown_store", "recovered");
    }
    return store.orElse(null);
  }

  /**
   * App User ID, original App User ID, aliases 순으로 숫자 형태의 Landit 사용자 ID 후보를 모은다.
   *
   * @param event 웹훅 이벤트
   * @return 중복을 뺀 Landit 사용자 ID 후보 목록. 없으면 빈 목록
   */
  private static List<Long> resolveCandidateUserIds(RevenueCatWebhookEvent event) {
    List<String> aliases = event.aliases() == null ? List.of() : event.aliases();
    return Stream.concat(Stream.of(event.appUserId(), event.originalAppUserId()), aliases.stream())
        .filter(Objects::nonNull)
        .map(RevenueCatWebhookService::parseUserId)
        .flatMap(Optional::stream)
        .distinct()
        .toList();
  }

  /**
   * App User ID를 Landit 사용자 ID로 해석한다.
   *
   * @param appUserId RevenueCat App User ID
   * @return 숫자 형태면 Landit 사용자 ID. 익명 ID처럼 숫자가 아니면 빈 값
   */
  private static Optional<Long> parseUserId(String appUserId) {
    try {
      return Optional.of(Long.parseLong(appUserId.trim()));
    } catch (NumberFormatException exception) {
      // RevenueCat 익명 ID($RCAnonymousID:...)처럼 숫자가 아닌 값은 후보에서 제외한다.
      return Optional.empty();
    }
  }

  /**
   * 밀리초 단위 epoch 값을 서비스 시간대의 시각으로 바꾼다.
   *
   * @param epochMillis epoch 밀리초. 없으면 null
   * @return 서비스 시간대 기준 시각. 입력이 null이면 빈 값
   */
  private Optional<LocalDateTime> toLocalDateTime(Long epochMillis) {
    if (epochMillis == null) {
      return Optional.empty();
    }
    return Optional.of(LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), clock.getZone()));
  }
}
