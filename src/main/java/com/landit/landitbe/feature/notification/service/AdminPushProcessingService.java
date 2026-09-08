// SQS 관리자 푸시 작업에서 Token 페이지와 테스트 알림을 처리한다.

package com.landit.landitbe.feature.notification.service;

import com.landit.landitbe.feature.notification.client.RetryablePushNotificationException;
import com.landit.landitbe.feature.notification.domain.NotificationType;
import com.landit.landitbe.feature.notification.messaging.PushQueuePublisher;
import com.landit.landitbe.feature.notification.repository.AdminPushRepository;
import com.landit.landitbe.feature.notification.repository.AdminPushRepository.Campaign;
import com.landit.landitbe.feature.notification.repository.AdminPushRepository.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** 관리자 캠페인의 다음 Token 페이지 또는 관리자 테스트를 처리한다. */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "landit.notification",
    name = "consumer-enabled",
    havingValue = "true")
public class AdminPushProcessingService {

  private static final int BATCH_SIZE = 100;

  private final AdminPushRepository repository;
  private final PushDeliveryService deliveries;
  private final NotificationDispatchService dispatch;
  private final PushQueuePublisher publisher;
  private final AdminPushCampaignService campaigns;

  /**
   * 캠페인의 다음 Token 페이지를 처리한다.
   *
   * @param campaignId 캠페인 ID
   */
  public void process(UUID campaignId) {
    campaigns.prepareForProcessing(campaignId);
    Campaign campaign = campaign(campaignId);
    if (campaign.status().equals("SCHEDULED") || campaign.status().equals("SCHEDULE_PENDING")) {
      throw new RetryablePushNotificationException("예약 대상 고정을 재시도합니다.");
    }
    if (!campaign.status().equals("QUEUED") && !campaign.status().equals("SENDING")) {
      return;
    }
    List<Target> targets = repository.targets(campaign);
    List<PreparedPushDelivery> prepared = new ArrayList<>();
    for (Target target : targets) {
      deliveries
          .prepare(
              new PreparePushDeliveryCommand(
                  target.userId(),
                  target.tokenId(),
                  NotificationType.ADMIN_BROADCAST,
                  "push:admin-broadcast:"
                      + campaignId
                      + ":"
                      + target.userId()
                      + ":"
                      + target.tokenId(),
                  campaign.content().title(),
                  campaign.content().body(),
                  campaign.content().deepLink()))
          .ifPresent(prepared::add);
    }
    if (!prepared.isEmpty()) {
      dispatch.sendAdminPrepared(prepared);
    }
    String eventId = "admin-broadcast:" + campaignId;
    List<Long> tokenIds = targets.stream().map(Target::tokenId).toList();
    if (deliveries.hasRequestedDeliveries("push:" + eventId + ":", tokenIds)) {
      throw new RetryablePushNotificationException("같은 관리자 푸시 페이지가 처리 중입니다.");
    }
    dispatch.scheduleAcceptedDeliveryReceipts(eventId, tokenIds);
    boolean completed = targets.size() < BATCH_SIZE;
    long lastTargetId = completed ? campaign.maxTargetId() : targets.getLast().id();
    repository.advance(campaignId, lastTargetId, completed);
    if (!completed) {
      publisher.publishAdminCampaign(campaignId);
    }
  }

  /**
   * 관리자 본인의 활성 Token에 테스트 알림을 보낸다.
   *
   * @param campaignId 캠페인 ID
   * @param adminId 관리자 ID
   * @param key 테스트 멱등성 키
   */
  public void test(UUID campaignId, long adminId, String key) {
    Campaign campaign = campaign(campaignId);
    dispatch.send(
        new SendPushNotificationCommand(
            "admin-broadcast-test:" + campaignId + ":" + key,
            adminId,
            NotificationType.ADMIN_BROADCAST_TEST,
            campaign.content().title(),
            campaign.content().body(),
            campaign.content().deepLink()));
  }

  private Campaign campaign(UUID id) {
    return repository.find(id).stream()
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("관리자 캠페인이 존재하지 않습니다."));
  }
}
