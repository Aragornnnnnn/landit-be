// 관리자 SQS 작업을 짧은 트랜잭션과 외부 제출 단계로 나누어 실행한다.

package com.landit.landitbe.feature.notification.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** 저장된 실행의 한 페이지 또는 Receipt 하나를 처리한다. */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "landit.notification",
    name = "consumer-enabled",
    havingValue = "true")
public class AdminPushProcessingService {
  private final AdminPushCampaignService campaigns;
  private final NotificationDispatchService dispatch;
  private final PushReceiptService receipts;

  /**
   * 중복 메시지를 걸러내고 하나의 지속성 작업을 수행한다.
   *
   * @param runId 실행 ID
   * @param version 메시지 버전
   */
  public void process(UUID runId, long version) {
    campaigns
        .claim(runId, version)
        .ifPresent(
            work -> {
              try {
                AdminPushCampaignService.Batch batch = campaigns.prepare(work);
                if (!batch.deliveries().isEmpty() && campaigns.maySubmit(work)) {
                  dispatch.sendAdminPrepared(batch.deliveries());
                }
                if (!batch.receiptIds().isEmpty()) {
                  receipts.checkAdmin(batch.receiptIds());
                }
                campaigns.finish(work);
              } catch (RuntimeException exception) {
                campaigns.failed(work);
                throw exception;
              }
            });
  }
}
