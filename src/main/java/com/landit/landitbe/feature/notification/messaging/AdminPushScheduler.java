// 관리자 캠페인의 일회성 SQS 예약을 외부 스케줄러에 위임한다.

package com.landit.landitbe.feature.notification.messaging;

import java.time.Instant;
import java.util.UUID;

/** 캠페인별 고정 이름으로 예약을 만들고 제거하는 외부 Port다. */
public interface AdminPushScheduler {
  /**
   * 같은 캠페인과 시각의 예약을 멱등하게 등록한다.
   *
   * @param campaignId 캠페인 ID
   * @param time UTC 예약 시각
   */
  void schedule(UUID campaignId, Instant time);

  /**
   * 캠페인 예약을 제거한다. 이미 없으면 성공한다.
   *
   * @param campaignId 캠페인 ID
   */
  void cancel(UUID campaignId);
}
