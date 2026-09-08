// 관리자 푸시 캠페인의 생성, 테스트, 일괄 발송을 관리한다.

package com.landit.landitbe.feature.notification.service;

import com.landit.landitbe.feature.admin.domain.AdminAction;
import com.landit.landitbe.feature.admin.service.AdminAuditService;
import com.landit.landitbe.feature.notification.dto.AdminPushAudiencePreview;
import com.landit.landitbe.feature.notification.dto.AdminPushCampaignRequest;
import com.landit.landitbe.feature.notification.dto.AdminPushCampaignView;
import com.landit.landitbe.feature.notification.messaging.PushQueuePublisher;
import com.landit.landitbe.feature.notification.repository.AdminPushRepository;
import com.landit.landitbe.feature.notification.repository.AdminPushRepository.Campaign;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/** 관리자 푸시 저장소를 소유하고 API와 SQS 소비 흐름을 제공한다. */
@Service
@RequiredArgsConstructor
public class AdminPushCampaignService {

  private final AdminPushRepository repository;
  private final AdminPushInputService input;
  private final AdminAuditService audit;
  private final PushQueuePublisher publisher;

  /**
   * 멱등성 키에 대해 캠페인 하나를 생성한다.
   *
   * @param adminId 인증 관리자 ID
   * @param key 생성 요청 키
   * @param request 알림 원문
   * @return 생성되거나 기존에 있던 캠페인
   */
  public AdminPushCampaignView create(long adminId, String key, AdminPushCampaignRequest request) {
    input.validateKey(key);
    AdminPushCampaignRequest content = input.validate(request);
    if (!repository.usersExist(content.userProfileIds())) {
      throw new ApiException(ErrorCode.INVALID_REQUEST);
    }
    String hash = input.fingerprint(content);
    Campaign campaign =
        new Campaign(
            UUID.randomUUID(),
            content,
            adminId,
            hash,
            "DRAFT",
            0,
            0,
            0,
            0,
            LocalDateTime.now(),
            null);
    try {
      repository.insert(campaign, key);
    } catch (DuplicateKeyException exception) {
      Campaign existing = repository.byKey(adminId, key).getFirst();
      if (!existing.hash().equals(hash)) {
        throw new ApiException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT);
      }
      return repository.view(existing);
    }
    audit.record(
        adminId,
        AdminAction.PUSH_CAMPAIGN_CREATED,
        "PUSH_CAMPAIGN",
        campaign.id().toString(),
        null,
        "DRAFT");
    return repository.view(campaign);
  }

  /**
   * 캠페인을 최신순으로 조회한다.
   *
   * @param page 페이지 번호
   * @param size 페이지 크기
   * @return 캠페인 목록
   */
  public List<AdminPushCampaignView> list(int page, int size) {
    if (page < 0 || size < 1 || size > 50) {
      throw new ApiException(ErrorCode.INVALID_REQUEST);
    }
    return repository.list(page, size).stream().map(repository::view).toList();
  }

  /**
   * 캠페인 상세와 현재 집계를 조회한다.
   *
   * @param id 캠페인 ID
   * @return 캠페인 상세
   */
  public AdminPushCampaignView detail(UUID id) {
    return repository.view(requireCampaign(id));
  }

  /**
   * 현재 활성 사용자와 Token 수를 미리 확인한다.
   *
   * @param id 캠페인 ID
   * @return 예상 대상 수
   */
  public AdminPushAudiencePreview preview(UUID id) {
    requireCampaign(id);
    return repository.preview(id);
  }

  /**
   * 인증 관리자의 활성 Token에 테스트 알림을 보낸다.
   *
   * @param id 캠페인 ID
   * @param adminId 인증 관리자 ID
   * @param key 테스트 멱등성 키
   * @return 캠페인 상세
   */
  public AdminPushCampaignView test(UUID id, long adminId, String key) {
    input.validateKey(key);
    Campaign campaign = requireCampaign(id);
    if (!campaign.status().equals("DRAFT")) {
      throw new ApiException(ErrorCode.CONFLICT);
    }
    audit.record(
        adminId, AdminAction.PUSH_CAMPAIGN_TESTED, "PUSH_CAMPAIGN", id.toString(), null, "TESTED");
    publisher.publishAdminTest(id, adminId, key);
    return repository.view(campaign);
  }

  /**
   * 전체 발송 대상을 고정하고 첫 SQS 작업을 발행한다.
   *
   * @param id 캠페인 ID
   * @param adminId 인증 관리자 ID
   * @param key 요청 멱등성 키
   * @return 캠페인 상세
   */
  public AdminPushCampaignView send(UUID id, long adminId, String key) {
    input.validateKey(key);
    requireCampaign(id);
    boolean firstRequest = repository.queueAndCaptureTargets(id);
    Campaign campaign = requireCampaign(id);
    if (firstRequest) {
      audit.record(
          adminId,
          AdminAction.PUSH_CAMPAIGN_SENT,
          "PUSH_CAMPAIGN",
          id.toString(),
          "DRAFT",
          "QUEUED");
    }
    if (!campaign.status().equals("COMPLETED")) {
      publisher.publishAdminCampaign(id);
    }
    return repository.view(campaign);
  }

  private Campaign requireCampaign(UUID id) {
    return repository.find(id).stream()
        .findFirst()
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
  }
}
