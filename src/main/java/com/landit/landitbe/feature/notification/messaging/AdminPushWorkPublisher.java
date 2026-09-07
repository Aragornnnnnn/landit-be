// DB에 남은 관리자 푸시 작업을 재발행해 SQS 발행 유실을 복구한다.

package com.landit.landitbe.feature.notification.messaging;

import com.landit.landitbe.feature.notification.service.AdminPushCampaignService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 실행 행을 지속성 작업 목록으로 사용하여 짧은 SQS 메시지를 발행한다. */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    prefix = "landit.notification",
    name = "consumer-enabled",
    havingValue = "true")
public class AdminPushWorkPublisher {
  private final AdminPushCampaignService campaigns;
  private final PushQueuePublisher publisher;

  /** 만기 작업을 발행하고 실패 시 DB에 재시도 시각을 남긴다. */
  @Scheduled(
      fixedDelayString = "${landit.notification.admin-poll-delay-ms:1000}",
      initialDelayString = "${landit.notification.admin-poll-initial-delay-ms:10000}")
  public void publishDue() {
    for (var run : campaigns.publications()) {
      try {
        publisher.publishAdminRun(run.id(), run.version());
      } catch (RuntimeException exception) {
        campaigns.publicationFailed(run);
        log.warn("관리자 푸시 작업 발행 실패: runId={}, version={}", run.id(), run.version());
      }
    }
  }
}
