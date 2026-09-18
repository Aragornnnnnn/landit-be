// 구독 변경 이벤트를 동기 처리해 같은 트랜잭션에 체험 알림 예약을 기록한다.

package com.landit.landitbe.feature.notification.job.service;

import com.landit.landitbe.feature.subscription.event.dto.SubscriptionChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/** 구독 구현의 알림 역참조 없이 구독 갱신과 예약의 원자성을 유지한다. */
@Service
@RequiredArgsConstructor
public class SubscriptionTrialReminderService {
  private final NotificationJobService jobs;

  /**
   * 구독 갱신 트랜잭션 안에서 알림 예약을 기록하고 실패를 호출자에게 전파한다.
   *
   * @param event 반영된 구독 변경
   */
  @EventListener
  public void onSubscriptionChanged(SubscriptionChangedEvent event) {
    jobs.recordTrial(event.userId(), event.environment());
  }
}
