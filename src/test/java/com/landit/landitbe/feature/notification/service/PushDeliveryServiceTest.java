// 푸시 발송 선점과 Ticket·Receipt 상태 기록 계약을 검증한다.

package com.landit.landitbe.feature.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.notification.client.PushReceiptResult;
import com.landit.landitbe.feature.notification.client.PushTicketResult;
import com.landit.landitbe.feature.notification.domain.NotificationType;
import com.landit.landitbe.feature.notification.domain.PushDelivery;
import com.landit.landitbe.feature.notification.domain.PushDeliveryStatus;
import com.landit.landitbe.feature.notification.repository.PushDeliveryRepository;
import com.landit.landitbe.feature.notification.repository.UserPushTokenRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 푸시 발송 선점과 Ticket·Receipt 상태 기록 계약을 검증한다. */
@ExtendWith(MockitoExtension.class)
class PushDeliveryServiceTest {

  private static final long USER_ID = 1L;
  private static final long USER_PUSH_TOKEN_ID = 2L;
  private static final long PUSH_DELIVERY_ID = 3L;

  @Mock private PushDeliveryRepository pushDeliveryRepository;

  @Mock private UserPushTokenDeliveryService userPushTokenDeliveryService;

  private PushDeliveryService pushDeliveryService;

  /** 각 테스트에서 발송 이력 Repository와 사용자 Push Token Service를 사용하는 Service를 생성한다. */
  @BeforeEach
  void setUp() {
    pushDeliveryService =
        new PushDeliveryService(pushDeliveryRepository, userPushTokenDeliveryService);
  }

  /** 발송 직전에 설치가 비활성화되면 발송 이력을 만들지 않는다. */
  @Test
  void skipsTokenThatIsNoLongerSendable() {
    when(userPushTokenDeliveryService.findLockedSendableDeliveryTarget(USER_PUSH_TOKEN_ID, USER_ID))
        .thenReturn(Optional.empty());

    assertThat(pushDeliveryService.prepare(command())).isEmpty();
    verify(pushDeliveryRepository, never()).saveAndFlush(any());
  }

  /** Push Delivery Service는 User Push Token Repository를 직접 소유하지 않는다. */
  @Test
  void doesNotOwnUserPushTokenRepository() {
    assertThat(PushDeliveryService.class.getDeclaredFields())
        .extracting(java.lang.reflect.Field::getType)
        .doesNotContain(UserPushTokenRepository.class);
  }

  /** DeviceNotRegistered Ticket 오류를 기록하고 해당 설치 Token을 무효화한다. */
  @Test
  void revokesTokenForRejectedTicket() {
    PushDelivery delivery = mock(PushDelivery.class);
    when(delivery.getSentExpoPushToken()).thenReturn("ExponentPushToken[sent-token]");
    when(delivery.failTicket(any(), any())).thenReturn(true);
    when(pushDeliveryRepository.findByIdForUpdate(PUSH_DELIVERY_ID))
        .thenReturn(Optional.of(delivery));

    pushDeliveryService.recordTicketResult(
        PUSH_DELIVERY_ID, PushTicketResult.failed("DeviceNotRegistered"));

    verify(delivery).failTicket(any(), any());
    verify(userPushTokenDeliveryService).revokeCurrentTokenOwner("ExponentPushToken[sent-token]");
  }

  /** Ticket 접수 상태의 발송 이력만 Receipt 조회 대상으로 반환한다. */
  @Test
  void findsAcceptedDeliveryForReceiptCheck() {
    PushDelivery delivery = mock(PushDelivery.class);
    when(delivery.getId()).thenReturn(PUSH_DELIVERY_ID);
    when(delivery.getExpoTicketId()).thenReturn("ticket-1");
    when(delivery.getStatus()).thenReturn(PushDeliveryStatus.TICKET_ACCEPTED);
    when(pushDeliveryRepository.findById(PUSH_DELIVERY_ID)).thenReturn(Optional.of(delivery));

    assertThat(pushDeliveryService.findReceiptTarget(PUSH_DELIVERY_ID))
        .contains(new PushReceiptTarget(PUSH_DELIVERY_ID, "ticket-1"));
  }

  /** Receipt 성공을 발송 완료 상태로 기록한다. */
  @Test
  void recordsDeliveredReceipt() {
    PushDelivery delivery = mock(PushDelivery.class);
    when(delivery.getStatus()).thenReturn(PushDeliveryStatus.TICKET_ACCEPTED);
    when(pushDeliveryRepository.findByIdForUpdate(PUSH_DELIVERY_ID))
        .thenReturn(Optional.of(delivery));

    pushDeliveryService.recordReceiptResult(PUSH_DELIVERY_ID, PushReceiptResult.delivered());

    verify(delivery).delivered(any());
  }

  /** 복습 리마인더 발송 선점 명령을 생성한다. */
  private PreparePushDeliveryCommand command() {
    return new PreparePushDeliveryCommand(
        USER_ID,
        USER_PUSH_TOKEN_ID,
        NotificationType.REVIEW_REMINDER,
        "review-reminder:" + LocalDate.of(2026, 7, 24) + ":" + USER_ID + ":" + USER_PUSH_TOKEN_ID,
        "복습할 시간이에요",
        "오늘의 표현을 다시 볼까요?",
        "/expressions");
  }
}
