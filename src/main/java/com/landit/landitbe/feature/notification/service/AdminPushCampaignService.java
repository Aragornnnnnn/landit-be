// 관리자 푸시 캠페인의 생성, 테스트, 일괄 발송을 관리한다.

package com.landit.landitbe.feature.notification.service;

import com.landit.landitbe.feature.admin.domain.AdminAction;
import com.landit.landitbe.feature.admin.service.AdminAuditService;
import com.landit.landitbe.feature.notification.dto.AdminPushAudiencePreview;
import com.landit.landitbe.feature.notification.dto.AdminPushCampaignPage;
import com.landit.landitbe.feature.notification.dto.AdminPushCampaignRequest;
import com.landit.landitbe.feature.notification.dto.AdminPushCampaignView;
import com.landit.landitbe.feature.notification.messaging.PushQueuePublisher;
import com.landit.landitbe.feature.notification.repository.AdminPushRepository;
import com.landit.landitbe.feature.notification.repository.AdminPushRepository.Campaign;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
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
  private final AdminPushAudienceSqlService audienceSql;
  private final com.landit.landitbe.feature.notification.messaging.AdminPushScheduler scheduler;

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
    if (!repository.usersExist(content.userProfileIds())
        || !repository.usersExist(content.excludedUserProfileIds())) {
      throw new ApiException(ErrorCode.INVALID_REQUEST);
    }
    String hash = input.fingerprint(request);
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
   * 예약 여부와 상태로 캠페인 목록과 전체 페이지 수를 조회한다.
   *
   * @param scheduled true이면 예약, false이면 예약 없는 캠페인. null이면 전체
   * @param status 상태 필터. null이면 모든 상태
   * @param page 0부터 시작하는 페이지 번호
   * @param size 페이지 크기. 1부터 50까지
   * @return 캠페인 목록 페이지
   * @throws ApiException 지원하지 않는 상태 또는 페이지 입력일 때 발생
   */
  public AdminPushCampaignPage list(Boolean scheduled, String status, int page, int size) {
    if (page < 0
        || size < 1
        || size > 50
        || (status != null
            && !Set.of(
                    "DRAFT",
                    "PENDING",
                    "SCHEDULE_PENDING",
                    "SCHEDULED",
                    "QUEUED",
                    "SENDING",
                    "COMPLETED",
                    "CANCELLED")
                .contains(status))) {
      throw new ApiException(ErrorCode.INVALID_REQUEST);
    }
    String filter = status == null ? "" : status;
    long totalCount = repository.count(scheduled, filter);
    long totalPages = totalCount / size + (totalCount % size == 0 ? 0 : 1);
    List<AdminPushCampaignView> items = repository.list(scheduled, filter, page, size);
    return new AdminPushCampaignPage(
        items, page, size, (long) page + 1 < totalPages, totalCount, totalPages);
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
    Campaign campaign = requireCampaign(id);
    return campaign.content().audienceSql() == null
        ? repository.preview(id)
        : repository.previewUsers(resolveUsers(campaign));
  }

  /**
   * SQL 결과 ID를 미리 조회해 개별 선택과 함께 사용할 수 있게 한다.
   *
   * @param adminId 관리자 ID
   * @param sql 대상 SQL
   * @return 중복 없는 조회 ID
   */
  public List<Long> queryAudience(long adminId, String sql) {
    audit.record(
        adminId, AdminAction.PUSH_AUDIENCE_QUERIED, "PUSH_AUDIENCE", "SQL", null, "REQUESTED");
    return audienceSql.query(sql);
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
    Campaign previous = requireCampaign(id);
    if (!Set.of("DRAFT", "PENDING", "QUEUED", "SENDING", "COMPLETED").contains(previous.status())) {
      throw new ApiException(ErrorCode.CONFLICT);
    }
    boolean firstRequest =
        previous.content().audienceSql() == null
            ? repository.queueAndCaptureTargets(id)
            : repository.requestSend(id);
    Campaign campaign = requireCampaign(id);
    if (!Set.of("PENDING", "QUEUED", "SENDING", "COMPLETED").contains(campaign.status())) {
      throw new ApiException(ErrorCode.CONFLICT);
    }
    if (firstRequest) {
      audit.record(
          adminId,
          AdminAction.PUSH_CAMPAIGN_SENT,
          "PUSH_CAMPAIGN",
          id.toString(),
          "DRAFT",
          campaign.status());
    }
    if (!campaign.status().equals("COMPLETED")) {
      publisher.publishAdminCampaign(id);
    }
    return repository.view(campaign);
  }

  /**
   * 한국 시간으로 지정한 일회성 예약을 등록한다. 같은 시각의 재시도는 허용한다.
   *
   * @param id 캠페인 ID
   * @param adminId 관리자 ID
   * @param key 요청 키
   * @param time +09:00 오프셋의 예약 시각
   * @return 예약 상태
   */
  public AdminPushCampaignView schedule(UUID id, long adminId, String key, OffsetDateTime time) {
    input.validateKey(key);
    if (time == null || !time.getOffset().equals(ZoneOffset.ofHours(9)) || time.getNano() != 0) {
      throw new ApiException(ErrorCode.INVALID_REQUEST);
    }
    Instant instant = time.toInstant();
    Campaign previous = requireCampaign(id);
    if (previous.status().equals("DRAFT")) {
      if (instant.isBefore(Instant.now().plusSeconds(60))) {
        throw new ApiException(ErrorCode.INVALID_REQUEST);
      }
      if (repository.requestSchedule(id, instant)) {
        audit.record(
            adminId,
            AdminAction.PUSH_CAMPAIGN_SCHEDULED,
            "PUSH_CAMPAIGN",
            id.toString(),
            "DRAFT",
            "SCHEDULE_PENDING");
      }
    }
    Campaign campaign = requireCampaign(id);
    if (!instant.equals(campaign.scheduledAt())
        || campaign.status().equals("CANCELLED")
        || campaign.status().equals("DRAFT")) {
      throw new ApiException(ErrorCode.CONFLICT);
    }
    if (campaign.status().equals("SCHEDULE_PENDING")) {
      if (instant.isAfter(Instant.now())) {
        scheduler.schedule(id, instant);
      } else {
        publisher.publishAdminCampaign(id);
      }
      repository.scheduled(id);
      if (requireCampaign(id).status().equals("CANCELLED")) {
        scheduler.cancel(id);
      }
    }
    return detail(id);
  }

  /**
   * 시작 전 예약을 취소한다. 남아 있는 SQS 메시지도 DB 상태에 의해 무시된다.
   *
   * @param id 캠페인 ID
   * @param adminId 관리자 ID
   * @return 취소 상태
   */
  public AdminPushCampaignView cancelSchedule(UUID id, long adminId) {
    requireCampaign(id);
    if (repository.cancelSchedule(id)) {
      audit.record(
          adminId,
          AdminAction.PUSH_CAMPAIGN_CANCELLED,
          "PUSH_CAMPAIGN",
          id.toString(),
          null,
          "CANCELLED");
    }
    if (!requireCampaign(id).status().equals("CANCELLED")) {
      throw new ApiException(ErrorCode.CONFLICT);
    }
    scheduler.cancel(id);
    return detail(id);
  }

  /**
   * Worker가 발송 시점의 SQL 결과를 조회하고 캠페인의 대상을 고정한다.
   *
   * @param id 캠페인 ID
   */
  public void prepareForProcessing(UUID id) {
    Campaign campaign = requireCampaign(id);
    boolean scheduled = Set.of("SCHEDULE_PENDING", "SCHEDULED").contains(campaign.status());
    if (!campaign.status().equals("PENDING") && !scheduled) {
      return;
    }
    if (scheduled && campaign.scheduledAt().isAfter(Instant.now())) {
      throw new com.landit.landitbe.feature.notification.client.RetryablePushNotificationException(
          "예약 시각 전에는 발송할 수 없습니다.");
    }
    repository.queueAndCaptureTargets(
        id, campaign.content().audienceSql() == null ? null : resolveUsers(campaign));
  }

  private List<Long> resolveUsers(Campaign campaign) {
    TreeSet<Long> ids = new TreeSet<>(audienceSql.query(campaign.content().audienceSql()));
    ids.addAll(campaign.content().userProfileIds());
    ids.removeAll(campaign.content().excludedUserProfileIds());
    return List.copyOf(ids);
  }

  private Campaign requireCampaign(UUID id) {
    return repository.find(id).stream()
        .findFirst()
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
  }
}
