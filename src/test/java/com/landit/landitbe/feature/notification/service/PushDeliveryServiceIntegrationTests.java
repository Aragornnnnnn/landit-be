// 실제 DB에서 푸시 발송 선점 멱등성과 무효 Token 전이를 검증한다.

package com.landit.landitbe.feature.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.landit.landitbe.feature.notification.client.PushReceiptResult;
import com.landit.landitbe.feature.notification.client.PushTicketResult;
import com.landit.landitbe.feature.notification.domain.NotificationContentVariant;
import com.landit.landitbe.feature.notification.domain.NotificationType;
import com.landit.landitbe.feature.notification.domain.UserPushToken;
import com.landit.landitbe.feature.notification.domain.UserPushTokenStatus;
import com.landit.landitbe.feature.notification.repository.PushDeliveryRepository;
import com.landit.landitbe.feature.notification.repository.UserPushTokenRepository;
import com.landit.landitbe.shared.domain.AppPlatform;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** 실제 DB에서 푸시 발송 선점 멱등성과 무효 Token 전이를 검증한다. */
@ActiveProfiles("test")
@SpringBootTest
@Transactional
class PushDeliveryServiceIntegrationTests {

  private static final long USER_ID = 996001L;
  private static final String SENT_EXPO_PUSH_TOKEN = "ExponentPushToken[delivery-service-token]";
  private static final String REFRESHED_EXPO_PUSH_TOKEN =
      "ExponentPushToken[delivery-service-refreshed-token]";

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private UserPushTokenRepository userPushTokenRepository;

  @Autowired private PushDeliveryRepository pushDeliveryRepository;

  @Autowired private PushDeliveryService pushDeliveryService;

  private UserPushToken userPushToken;

  /** 각 테스트에서 사용할 사용자와 활성 Expo Push Token을 저장한다. */
  @BeforeEach
  void setUp() {
    jdbcTemplate.update(
        """
        insert into user_profile (
            id, nickname, target_locale, base_locale, current_level,
            push_permission_status, status, created_at, updated_at
        )
        values (?, 'delivery-service-user', 'EN', 'KR', 1, 'NOT_DETERMINED',
            'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        USER_ID);
    userPushToken =
        userPushTokenRepository.saveAndFlush(
            UserPushToken.register(USER_ID, AppPlatform.IOS, SENT_EXPO_PUSH_TOKEN));
  }

  /** 같은 이벤트·사용자·Token 발송을 반복 선점해도 발송 이력은 한 건만 생성한다. */
  @Test
  void preparesSameReviewReminderOnlyOnce() {
    PreparePushDeliveryCommand command = command();

    assertThat(pushDeliveryService.prepare(command)).isPresent();
    assertThat(pushDeliveryService.prepare(command)).isEmpty();
    assertThat(pushDeliveryRepository.count()).isEqualTo(1);
  }

  /** 예약 알림의 문구 변형을 동일한 발송 이력에 저장한다. */
  @Test
  void persistsContentVariantSnapshot() {
    PreparedPushDelivery prepared = pushDeliveryService.prepare(commandWithVariant()).orElseThrow();

    assertThat(
            pushDeliveryRepository
                .findById(prepared.pushDeliveryId())
                .orElseThrow()
                .getContentVariant())
        .isEqualTo(NotificationContentVariant.EXPRESSION_DYNAMIC);
  }

  /** 일시 오류가 기록된 발송은 같은 이력 ID로 재시도하고 새 행을 만들지 않는다. */
  @Test
  void reusesSameDeliveryAfterTemporaryProviderFailure() {
    PreparePushDeliveryCommand command = command();
    PreparedPushDelivery first = pushDeliveryService.prepare(command).orElseThrow();
    pushDeliveryService.markRetryable(first.pushDeliveryId());

    PreparedPushDelivery retry = pushDeliveryService.prepare(command).orElseThrow();

    assertThat(retry.pushDeliveryId()).isEqualTo(first.pushDeliveryId());
    assertThat(retry.expoPushToken()).isEqualTo(SENT_EXPO_PUSH_TOKEN);
    assertThat(pushDeliveryService.prepare(command)).isEmpty();
    assertThat(pushDeliveryRepository.count()).isEqualTo(1);
  }

  /** 재시도 전에 Token이 해제되면 오래된 이력을 다시 발송하지 않는다. */
  @Test
  void skipsRetryWhenTokenIsRevoked() {
    PreparePushDeliveryCommand command = command();
    PreparedPushDelivery prepared = pushDeliveryService.prepare(command).orElseThrow();
    pushDeliveryService.markRetryable(prepared.pushDeliveryId());
    userPushToken.revoke();
    userPushTokenRepository.saveAndFlush(userPushToken);

    assertThat(pushDeliveryService.prepare(command)).isEmpty();
  }

  /** DeviceNotRegistered Receipt를 기록하면 실제 Expo Push Token을 REVOKED로 변경한다. */
  @Test
  void revokesTokenAfterDeviceNotRegisteredReceipt() {
    PreparedPushDelivery prepared = pushDeliveryService.prepare(command()).orElseThrow();
    pushDeliveryService.recordTicketResult(
        prepared.pushDeliveryId(), PushTicketResult.accepted("ticket-1"));

    pushDeliveryService.recordReceiptResult(
        prepared.pushDeliveryId(), PushReceiptResult.failed("DeviceNotRegistered"));

    assertThat(userPushTokenRepository.findById(userPushToken.getId()).orElseThrow().getStatus())
        .isEqualTo(UserPushTokenStatus.REVOKED);
  }

  /** 오래된 Token의 Receipt 실패가 새로 등록된 다른 Token을 비활성화하지 않는다. */
  @Test
  void keepsNewTokenActiveAfterOldTokenReceiptFailure() {
    PreparedPushDelivery prepared = pushDeliveryService.prepare(command()).orElseThrow();
    pushDeliveryService.recordTicketResult(
        prepared.pushDeliveryId(), PushTicketResult.accepted("ticket-1"));
    UserPushToken refreshedToken =
        userPushTokenRepository.saveAndFlush(
            UserPushToken.register(USER_ID, AppPlatform.IOS, REFRESHED_EXPO_PUSH_TOKEN));

    pushDeliveryService.recordReceiptResult(
        prepared.pushDeliveryId(), PushReceiptResult.failed("DeviceNotRegistered"));

    assertThat(userPushTokenRepository.findById(refreshedToken.getId()).orElseThrow().getStatus())
        .isEqualTo(UserPushTokenStatus.ACTIVE);
  }

  /** 복습 리마인더 발송 선점 명령을 생성한다. */
  private PreparePushDeliveryCommand command() {
    LocalDate reviewDate = LocalDate.of(2026, 7, 24);
    return new PreparePushDeliveryCommand(
        USER_ID,
        userPushToken.getId(),
        NotificationType.SMALL_TALK_REMINDER,
        "review-reminder:" + reviewDate + ":" + USER_ID + ":" + userPushToken.getId(),
        "복습할 시간이에요",
        "오늘의 표현을 다시 볼까요?",
        "/expressions");
  }

  private PreparePushDeliveryCommand commandWithVariant() {
    LocalDate reviewDate = LocalDate.of(2026, 7, 24);
    return new PreparePushDeliveryCommand(
        USER_ID,
        userPushToken.getId(),
        NotificationType.CONTINUE_EXPRESSION,
        NotificationContentVariant.EXPRESSION_DYNAMIC,
        "review-reminder:" + reviewDate + ":" + USER_ID + ":" + userPushToken.getId(),
        "“break the ice”, 어떤 상황에서 쓸까요?",
        "오늘 시나리오에서 이어지는 표현을 배워보세요.",
        "/expressions/scenario/10/100");
  }
}
