// DB에 남은 예약 의도를 외부 예약 서비스에 등록하고 실패를 복구한다.

package com.landit.landitbe.feature.notification.service;

import com.landit.landitbe.feature.notification.messaging.NotificationJobScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/** 사용자 목록이 아닌 저장된 예약 미등록 건만 재시도한다. */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    prefix = "landit.notification",
    name = "consumer-enabled",
    havingValue = "true")
public class NotificationJobReservationService {
  private final NotificationJobService jobs;
  private final NotificationJobScheduler scheduler;

  /** 커밋된 예약 의도만 읽어 실패한 외부 예약을 재시도한다. */
  @Scheduled(
      fixedDelayString = "${landit.notification.job-poll-delay:30000}",
      initialDelayString = "${landit.notification.job-poll-delay:30000}")
  public void registerPending() {
    for (var job : jobs.pendingReservations()) {
      if (!jobs.reserve(job.id())) {
        continue;
      }
      try {
        scheduler.schedule(job);
        jobs.registered(job.id());
      } catch (RuntimeException exception) {
        log.error(
            "알림 예약 등록 실패: jobId={}, errorType={}", job.id(), exception.getClass().getSimpleName());
      }
    }
  }
}
