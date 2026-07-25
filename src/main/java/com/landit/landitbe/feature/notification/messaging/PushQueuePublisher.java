// Receipt 확인 메시지를 Push 전용 Queue에 지연 발행하는 Port다.

package com.landit.landitbe.feature.notification.messaging;

import java.time.Instant;

/** Receipt 확인 메시지를 Push 전용 Queue에 지연 발행하는 Port다. */
public interface PushQueuePublisher {

  /**
   * 현재 시각 기준 복습 리마인더 배치 메시지를 Push 전용 Queue에 즉시 발행한다.
   *
   * @param occurredAt 복습 대상 날짜를 계산할 기준 시각
   */
  void publishReviewReminderBatch(Instant occurredAt);

  /**
   * Expo Receipt 확인 메시지를 지정된 초기 지연으로 발행한다.
   *
   * @param pushDeliveryId 확인할 Push Delivery ID
   * @param attempt Receipt 확인 시도 횟수
   */
  void scheduleReceiptCheck(Long pushDeliveryId, int attempt);
}
