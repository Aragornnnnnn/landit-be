// 실제 DB에서 푸시 발송 선점 멱등성과 무효 Token 전이를 검증한다.

package com.landit.landitbe.feature.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.landit.landitbe.feature.notification.client.PushReceiptResult;
import com.landit.landitbe.feature.notification.client.PushTicketResult;
import com.landit.landitbe.feature.notification.domain.NotificationType;
import com.landit.landitbe.feature.notification.domain.PushDevice;
import com.landit.landitbe.feature.notification.domain.PushTokenStatus;
import com.landit.landitbe.feature.notification.repository.PushDeliveryRepository;
import com.landit.landitbe.feature.notification.repository.PushDeviceRepository;
import com.landit.landitbe.shared.domain.AppPlatform;
import java.time.LocalDate;
import java.util.UUID;
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
  private static final UUID INSTALLATION_ID =
      UUID.fromString("550e8400-e29b-41d4-a716-446655440020");
  private static final String SENT_EXPO_PUSH_TOKEN = "ExponentPushToken[delivery-service-token]";
  private static final String REFRESHED_EXPO_PUSH_TOKEN =
      "ExponentPushToken[delivery-service-refreshed-token]";

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private PushDeviceRepository pushDeviceRepository;

  @Autowired private PushDeliveryRepository pushDeliveryRepository;

  @Autowired private PushDeliveryService pushDeliveryService;

  private PushDevice pushDevice;

  /** 각 테스트에서 사용할 사용자와 발송 가능한 설치를 저장한다. */
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
    pushDevice =
        pushDeviceRepository.saveAndFlush(
            PushDevice.create(
                USER_ID, INSTALLATION_ID, AppPlatform.IOS, true, SENT_EXPO_PUSH_TOKEN));
  }

  /** 같은 날짜·사용자·설치 발송을 반복 선점해도 발송 이력은 한 건만 생성한다. */
  @Test
  void preparesSameReviewReminderOnlyOnce() {
    PreparePushDeliveryCommand command = command();

    assertThat(pushDeliveryService.prepare(command)).isPresent();
    assertThat(pushDeliveryService.prepare(command)).isEmpty();
    assertThat(pushDeliveryRepository.count()).isEqualTo(1);
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

  /** 재시도 전에 설치 Token이 바뀌면 오래된 이력을 새 Token으로 발송하지 않는다. */
  @Test
  void skipsRetryWhenCurrentTokenDiffersFromSentToken() {
    PreparePushDeliveryCommand command = command();
    PreparedPushDelivery prepared = pushDeliveryService.prepare(command).orElseThrow();
    pushDeliveryService.markRetryable(prepared.pushDeliveryId());
    pushDevice.synchronize(USER_ID, AppPlatform.IOS, true, REFRESHED_EXPO_PUSH_TOKEN);
    pushDeviceRepository.saveAndFlush(pushDevice);

    assertThat(pushDeliveryService.prepare(command)).isEmpty();
  }

  /** DeviceNotRegistered Receipt를 기록하면 실제 Push Device Token을 INVALID로 변경한다. */
  @Test
  void invalidatesPushDeviceAfterDeviceNotRegisteredReceipt() {
    PreparedPushDelivery prepared = pushDeliveryService.prepare(command()).orElseThrow();
    pushDeliveryService.recordTicketResult(
        prepared.pushDeliveryId(), PushTicketResult.accepted("ticket-1"));

    pushDeliveryService.recordReceiptResult(
        prepared.pushDeliveryId(), PushReceiptResult.failed("DeviceNotRegistered"));

    assertThat(pushDeviceRepository.findById(pushDevice.getId()).orElseThrow().getTokenStatus())
        .isEqualTo(PushTokenStatus.INVALID);
  }

  /** 발송 뒤 같은 설치의 Token이 갱신되면 오래된 Receipt가 새 Token을 무효화하지 않는다. */
  @Test
  void keepsRefreshedTokenActiveAfterOldTokenReceiptFailure() {
    PreparedPushDelivery prepared = pushDeliveryService.prepare(command()).orElseThrow();
    pushDeliveryService.recordTicketResult(
        prepared.pushDeliveryId(), PushTicketResult.accepted("ticket-1"));
    pushDevice.synchronize(USER_ID, AppPlatform.IOS, true, REFRESHED_EXPO_PUSH_TOKEN);
    pushDeviceRepository.saveAndFlush(pushDevice);

    pushDeliveryService.recordReceiptResult(
        prepared.pushDeliveryId(), PushReceiptResult.failed("DeviceNotRegistered"));

    PushDevice refreshedDevice = pushDeviceRepository.findById(pushDevice.getId()).orElseThrow();
    assertThat(refreshedDevice.getExpoPushToken()).isEqualTo(REFRESHED_EXPO_PUSH_TOKEN);
    assertThat(refreshedDevice.getTokenStatus()).isEqualTo(PushTokenStatus.ACTIVE);
  }

  /** 발송 Token이 다른 설치로 이전되면 오래된 Receipt가 그 Token의 현재 소유자를 무효화한다. */
  @Test
  void invalidatesCurrentOwnerAfterSentTokenTransfer() {
    PreparedPushDelivery prepared = pushDeliveryService.prepare(command()).orElseThrow();
    pushDeliveryService.recordTicketResult(
        prepared.pushDeliveryId(), PushTicketResult.accepted("ticket-1"));
    pushDevice.detachToken();
    pushDeviceRepository.saveAndFlush(pushDevice);
    PushDevice transferredOwner =
        pushDeviceRepository.saveAndFlush(
            PushDevice.create(
                USER_ID,
                UUID.fromString("550e8400-e29b-41d4-a716-446655440021"),
                AppPlatform.ANDROID,
                true,
                SENT_EXPO_PUSH_TOKEN));

    pushDeliveryService.recordReceiptResult(
        prepared.pushDeliveryId(), PushReceiptResult.failed("DeviceNotRegistered"));

    assertThat(
            pushDeviceRepository.findById(transferredOwner.getId()).orElseThrow().getTokenStatus())
        .isEqualTo(PushTokenStatus.INVALID);
  }

  /** 복습 리마인더 발송 선점 명령을 생성한다. */
  private PreparePushDeliveryCommand command() {
    LocalDate reviewDate = LocalDate.of(2026, 7, 24);
    return new PreparePushDeliveryCommand(
        USER_ID,
        pushDevice.getId(),
        NotificationType.REVIEW_REMINDER,
        "review-reminder:" + reviewDate + ":" + USER_ID + ":" + pushDevice.getId(),
        "복습할 시간이에요",
        "오늘의 표현을 다시 볼까요?",
        "/expressions");
  }
}
