// 알림 작업의 일회성 예약 또는 즉시 큐 발행 계약을 정의한다.

package com.landit.landitbe.feature.notification.messaging;

import com.landit.landitbe.feature.notification.dto.NotificationJob;

/** 알림 예약을 외부 서비스에 등록한다. */
public interface NotificationJobScheduler {
  /**
   * 미래 작업은 예약하고 이미 도래한 작업은 큐에 발행한다.
   *
   * @param job 저장된 발송 의도
   */
  void schedule(NotificationJob job);
}
