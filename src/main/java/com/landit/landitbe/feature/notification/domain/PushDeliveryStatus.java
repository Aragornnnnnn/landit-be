// Expo Push 발송 이력의 처리 상태를 정의한다.

package com.landit.landitbe.feature.notification.domain;

/** Expo Push 발송 이력의 처리 상태를 정의한다. */
public enum PushDeliveryStatus {
  REQUESTED,
  TICKET_ACCEPTED,
  DELIVERED,
  FAILED
}
