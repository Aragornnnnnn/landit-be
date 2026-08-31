// 푸시 발송 이력의 Ticket과 Receipt 상태 전이를 검증한다.

package com.landit.landitbe.feature.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/** 푸시 발송 이력의 Ticket과 Receipt 상태 전이를 검증한다. */
class PushDeliveryTest {

  private static final LocalDateTime REQUESTED_AT = LocalDateTime.of(2026, 7, 24, 20, 0);
  private static final LocalDateTime CHECKED_AT = LocalDateTime.of(2026, 7, 24, 20, 15);
  private static final String SENT_EXPO_PUSH_TOKEN = "ExponentPushToken[domain-snapshot]";

  /** Expo 호출 전 발송 이력을 요청 상태로 생성한다. */
  @Test
  void createsRequestedDelivery() {
    PushDelivery delivery = requestedDelivery();

    assertThat(delivery.getUserProfileId()).isEqualTo(1L);
    assertThat(delivery.getUserPushTokenId()).isEqualTo(2L);
    assertThat(delivery.getNotificationType()).isEqualTo(NotificationType.SMALL_TALK_REMINDER);
    assertThat(delivery.getStatus()).isEqualTo(PushDeliveryStatus.REQUESTED);
    assertThat(delivery.getRequestedAt()).isEqualTo(REQUESTED_AT);
    assertThat(SENT_EXPO_PUSH_TOKEN.equals(delivery.getSentExpoPushToken())).isTrue();
  }

  /** Expo Ticket ID를 기록하면 접수 완료 상태로 전환한다. */
  @Test
  void acceptsExpoTicket() {
    PushDelivery delivery = requestedDelivery();

    delivery.acceptTicket("ticket-1");

    assertThat(delivery.getExpoTicketId()).isEqualTo("ticket-1");
    assertThat(delivery.getStatus()).isEqualTo(PushDeliveryStatus.TICKET_ACCEPTED);
  }

  /** Expo Ticket ID가 없으면 접수 상태로 전환하지 않는다. */
  @Test
  void rejectsBlankExpoTicket() {
    PushDelivery delivery = requestedDelivery();

    assertThatThrownBy(() -> delivery.acceptTicket(" "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Expo Ticket ID는 비어 있을 수 없습니다.");

    assertThat(delivery.getStatus()).isEqualTo(PushDeliveryStatus.REQUESTED);
  }

  /** Receipt가 성공하면 배달 완료 시각을 기록한다. */
  @Test
  void marksReceiptDelivered() {
    PushDelivery delivery = requestedDelivery();
    delivery.acceptTicket("ticket-1");

    delivery.delivered(CHECKED_AT);

    assertThat(delivery.getStatus()).isEqualTo(PushDeliveryStatus.DELIVERED);
    assertThat(delivery.getReceiptCheckedAt()).isEqualTo(CHECKED_AT);
    assertThat(delivery.getErrorCode()).isNull();
  }

  /** Ticket 또는 Receipt 오류 코드와 확인 시각을 기록한다. */
  @Test
  void recordsFailure() {
    PushDelivery delivery = requestedDelivery();

    delivery.failTicket("DeviceNotRegistered", CHECKED_AT);

    assertThat(delivery.getStatus()).isEqualTo(PushDeliveryStatus.FAILED);
    assertThat(delivery.getErrorCode()).isEqualTo("DeviceNotRegistered");
    assertThat(delivery.getReceiptCheckedAt()).isEqualTo(CHECKED_AT);
  }

  /** 최종 상태가 된 발송 이력은 늦게 도착한 Ticket 결과로 덮어쓰지 않는다. */
  @Test
  void keepsTerminalStateWhenStaleResultArrives() {
    PushDelivery delivery = requestedDelivery();
    delivery.failTicket("MessageTooBig", CHECKED_AT);

    assertThat(delivery.acceptTicket("ticket-1")).isFalse();
    assertThat(delivery.delivered(CHECKED_AT)).isFalse();

    assertThat(delivery.getStatus()).isEqualTo(PushDeliveryStatus.FAILED);
    assertThat(delivery.getExpoTicketId()).isNull();
    assertThat(delivery.getErrorCode()).isEqualTo("MessageTooBig");
  }

  /** 일시적인 외부 제공자 오류는 같은 발송 이력을 재시도 가능한 요청 상태로 유지한다. */
  @Test
  void marksTemporaryProviderFailureAsRetryable() {
    PushDelivery delivery = requestedDelivery();

    delivery.markRetryable();

    assertThat(delivery.getStatus()).isEqualTo(PushDeliveryStatus.REQUESTED);
    assertThat(delivery.isRetryable()).isTrue();

    delivery.acceptTicket("ticket-1");

    assertThat(delivery.isRetryable()).isFalse();
    assertThat(delivery.getErrorCode()).isNull();
  }

  /** 재시도 표식은 첫 선점만 성공시키고 즉시 소비한다. */
  @Test
  void claimsRetryOnlyOnce() {
    PushDelivery delivery = requestedDelivery();
    delivery.markRetryable();

    assertThat(delivery.claimRetry()).isTrue();
    assertThat(delivery.claimRetry()).isFalse();
    assertThat(delivery.isRetryable()).isFalse();
  }

  /** Ticket 접수 뒤 도착한 오래된 Ticket 실패 결과는 접수 상태를 덮지 않는다. */
  @Test
  void ignoresStaleTicketFailureAfterAcceptedTicket() {
    PushDelivery delivery = requestedDelivery();
    delivery.acceptTicket("ticket-1");

    delivery.failTicket("DeviceNotRegistered", CHECKED_AT);

    assertThat(delivery.getStatus()).isEqualTo(PushDeliveryStatus.TICKET_ACCEPTED);
    assertThat(delivery.getExpoTicketId()).isEqualTo("ticket-1");
    assertThat(delivery.getErrorCode()).isNull();
  }

  /** 중복 Ticket 접수 결과는 최초 Ticket ID를 덮지 않는다. */
  @Test
  void keepsFirstAcceptedTicketWhenDuplicateResultArrives() {
    PushDelivery delivery = requestedDelivery();
    delivery.acceptTicket("ticket-1");

    delivery.acceptTicket("ticket-2");

    assertThat(delivery.getStatus()).isEqualTo(PushDeliveryStatus.TICKET_ACCEPTED);
    assertThat(delivery.getExpoTicketId()).isEqualTo("ticket-1");
  }

  /** 실패가 확정된 뒤 도착한 오래된 Ticket 접수 결과는 최종 상태를 덮지 않는다. */
  @Test
  void ignoresStaleAcceptedTicketAfterFailure() {
    PushDelivery delivery = requestedDelivery();
    delivery.failTicket("MessageTooBig", CHECKED_AT);

    delivery.acceptTicket("ticket-1");

    assertThat(delivery.getStatus()).isEqualTo(PushDeliveryStatus.FAILED);
    assertThat(delivery.getExpoTicketId()).isNull();
    assertThat(delivery.getErrorCode()).isEqualTo("MessageTooBig");
  }

  /** Ticket 접수 전 Receipt 결과는 요청 상태를 변경하지 않는다. */
  @Test
  void ignoresReceiptResultBeforeAcceptedTicket() {
    PushDelivery delivery = requestedDelivery();

    delivery.delivered(CHECKED_AT);
    delivery.failReceipt("DeviceNotRegistered", CHECKED_AT);

    assertThat(delivery.getStatus()).isEqualTo(PushDeliveryStatus.REQUESTED);
    assertThat(delivery.getReceiptCheckedAt()).isNull();
  }

  private PushDelivery requestedDelivery() {
    return PushDelivery.requested(
        1L,
        2L,
        SENT_EXPO_PUSH_TOKEN,
        NotificationType.SMALL_TALK_REMINDER,
        "review-reminder:2026-07-24:1:2",
        "복습할 시간이에요",
        "오늘의 표현을 다시 볼까요?",
        "/expressions",
        REQUESTED_AT);
  }
}
