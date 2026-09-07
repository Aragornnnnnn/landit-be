// 관리자 캠페인의 불변 내용과 대상 스냅샷 및 복구 가능한 실행을 관리한다.

package com.landit.landitbe.feature.notification.service;

import com.landit.landitbe.feature.admin.domain.AdminAction;
import com.landit.landitbe.feature.admin.service.AdminAuditService;
import com.landit.landitbe.feature.notification.domain.NotificationType;
import com.landit.landitbe.feature.notification.dto.AdminPushAudiencePreview;
import com.landit.landitbe.feature.notification.dto.AdminPushCampaignRequest;
import com.landit.landitbe.feature.notification.dto.AdminPushCampaignView;
import com.landit.landitbe.feature.notification.dto.AdminPushRunView;
import com.landit.landitbe.feature.notification.repository.AdminPushRepository;
import com.landit.landitbe.feature.notification.repository.AdminPushRepository.Campaign;
import com.landit.landitbe.feature.notification.repository.AdminPushRepository.Run;
import com.landit.landitbe.feature.notification.repository.AdminPushRepository.RunRequest;
import com.landit.landitbe.feature.notification.repository.AdminPushRepository.Target;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관리자 푸시 저장소를 소유하고 HTTP·소비자에게 record 계약을 제공한다. */
@Service
@RequiredArgsConstructor
public class AdminPushCampaignService {
  private final AdminPushRepository repository;
  private final AdminPushInputService input;
  private final AdminAuditService audit;
  private final PushDeliveryService deliveries;

  /**
   * 요청 키에 대해 불변 캠페인 하나를 생성한다.
   *
   * @param adminId 인증 관리자
   * @param key 요청 키
   * @param request 표시 내용
   * @return 저장된 캠페인
   */
  @Transactional
  public AdminPushCampaignView create(long adminId, String key, AdminPushCampaignRequest request) {
    input.validateKey(key);
    AdminPushCampaignRequest content = input.validate(request);
    String hash = input.fingerprint(content);
    repository.lockGate();
    List<Campaign> existing = repository.byKey(adminId, key);
    if (!existing.isEmpty()) {
      if (!hash.equals(existing.getFirst().hash())) {
        throw new ApiException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT);
      }
      return view(existing.getFirst());
    }
    Campaign row = new Campaign(UUID.randomUUID(), content, adminId, hash, LocalDateTime.now());
    repository.insertCampaign(row, key);
    audit.record(
        adminId,
        AdminAction.PUSH_CAMPAIGN_CREATED,
        "PUSH_CAMPAIGN",
        row.id().toString(),
        null,
        "CREATED");
    return view(row);
  }

  /**
   * 캠페인을 페이지 조회한다.
   *
   * @param page 페이지 번호
   * @param size 페이지 크기
   * @return 캠페인 목록
   */
  @Transactional(readOnly = true)
  public List<AdminPushCampaignView> list(int page, int size) {
    if (page < 0 || size < 1 || size > 50) {
      throw new ApiException(ErrorCode.INVALID_REQUEST);
    }
    return repository.list(page, size).stream().map(this::view).toList();
  }

  /**
   * 캠페인과 전체·테스트 실행을 조회한다.
   *
   * @param id 캠페인 ID
   * @return 상세
   */
  @Transactional(readOnly = true)
  public AdminPushCampaignView detail(UUID id) {
    return view(requireCampaign(id));
  }

  /**
   * 실제 스냅샷과 구분되는 예상 대상 수를 조회한다.
   *
   * @param id 캠페인 ID
   * @return 예상 대상
   */
  @Transactional(readOnly = true)
  public AdminPushAudiencePreview preview(UUID id) {
    requireCampaign(id);
    return repository.preview();
  }

  /**
   * 캠페인당 유일한 전체 실행 또는 요청자 본인 테스트를 저장한다.
   *
   * @param id 캠페인 ID
   * @param adminId 인증 관리자
   * @param key 요청 키
   * @param test 테스트 여부
   * @return 기존 또는 신규 실행
   */
  @Transactional
  public AdminPushRunView start(UUID id, long adminId, String key, boolean test) {
    input.validateKey(key);
    repository.lockGate();
    requireCampaign(id);
    List<Run> runs = repository.runs(id);
    Optional<Run> broadcast = runs.stream().filter(r -> r.mode().equals("BROADCAST")).findFirst();
    if (!test && broadcast.isPresent()) {
      return repository.view(broadcast.get());
    }
    Optional<Run> existing =
        runs.stream()
            .filter(r -> r.mode().equals("TEST") && r.adminId() == adminId && r.key().equals(key))
            .findFirst();
    if (test && existing.isPresent()) {
      return repository.view(existing.get());
    }
    if (test && broadcast.isPresent()) {
      throw new ApiException(ErrorCode.CONFLICT);
    }
    if (test && repository.recentTest(adminId)) {
      throw new ApiException(ErrorCode.TOO_MANY_REQUESTS);
    }
    UUID runId = UUID.randomUUID();
    repository.insertRun(runId, id, new RunRequest(adminId, key, test));
    audit.record(
        adminId,
        test ? AdminAction.PUSH_CAMPAIGN_TESTED : AdminAction.PUSH_CAMPAIGN_SENT,
        "PUSH_RUN",
        runId.toString(),
        null,
        "QUEUED");
    return repository.view(requireRun(runId));
  }

  /**
   * 캠페인 소속 실행 결과를 조회한다.
   *
   * @param campaignId 캠페인 ID
   * @param runId 실행 ID
   * @return 상태와 집계
   */
  @Transactional
  public AdminPushRunView run(UUID campaignId, UUID runId) {
    Run row = requireRun(runId);
    if (!row.campaignId().equals(campaignId)) {
      throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
    }
    return repository.view(row);
  }

  /**
   * 중단된 실행의 미처리 작업만 재개한다.
   *
   * @param campaignId 캠페인 ID
   * @param runId 실행 ID
   * @param adminId 요청 관리자
   * @return 같은 실행 상태
   */
  @Transactional
  public AdminPushRunView resume(UUID campaignId, UUID runId, long adminId) {
    repository.lockGate();
    Run row = requireRun(runId);
    if (!row.campaignId().equals(campaignId)) {
      throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
    }
    if (row.status().equals("COMPLETED")) {
      throw new ApiException(ErrorCode.CONFLICT);
    }
    if (row.status().equals("BLOCKED")) {
      repository.resume(row);
      audit.record(
          adminId,
          AdminAction.PUSH_CAMPAIGN_RESUMED,
          "PUSH_RUN",
          runId.toString(),
          "BLOCKED",
          "QUEUED");
    }
    return repository.view(requireRun(runId));
  }

  /**
   * 만기가 된 지속성 작업의 발행 시도를 선점한다.
   *
   * @return 발행할 실행 목록
   */
  @Transactional
  public List<Run> publications() {
    repository.lockGate();
    repository.reopenReceipts();
    return repository.due().stream().filter(repository::claimPublication).toList();
  }

  /**
   * SQS 메시지의 버전과 lease를 검사해 실행을 선점한다.
   *
   * @param id 실행 ID
   * @param version 메시지 작업 버전
   * @return 선점된 작업 또는 중복 메시지의 빈 결과
   */
  @Transactional
  public Optional<Work> claim(UUID id, long version) {
    repository.lockGate();
    Run row = requireRun(id);
    if (version > row.version()) {
      throw new IllegalArgumentException("미래 작업 버전입니다.");
    }
    if (version != row.version()
        || row.status().equals("COMPLETED")
        || row.status().equals("BLOCKED")
        || row.nextAt().isAfter(LocalDateTime.now())
        || (row.leaseUntil() != null && row.leaseUntil().isAfter(LocalDateTime.now()))) {
      return Optional.empty();
    }
    UUID owner = UUID.randomUUID();
    repository.claim(row, owner);
    return Optional.of(new Work(id, version, owner));
  }

  /**
   * 확정 대상 또는 다음 100개 토큰을 DB에서 선점한다.
   *
   * @param work 작업 소유권
   * @return 외부에서 수행할 제출 또는 Receipt 조회
   */
  @Transactional
  public Batch prepare(Work work) {
    repository.lockGate();
    Run row = owned(work);
    if (row.capturedAt() == null) {
      repository.capture(row);
      return new Batch(List.of(), List.of());
    }
    repository.stale(row.id()).forEach(id -> deliveries.adminUnknown(id, "PROCESS_INTERRUPTED"));
    List<Target> targets = repository.targets(row.id());
    if (targets.isEmpty()) {
      return new Batch(List.of(), repository.receipt(row.id()));
    }
    if (!repository.acquireSubmission(work.owner())) {
      return new Batch(List.of(), List.of());
    }
    Campaign campaign = requireCampaign(row.campaignId());
    List<PreparedPushDelivery> prepared = new ArrayList<>();
    for (Target target : targets) {
      String exclusion = repository.exclusion(target);
      if (exclusion != null) {
        repository.exclude(target.id(), exclusion);
        continue;
      }
      boolean test = row.mode().equals("TEST");
      String key =
          test
              ? "push:admin-broadcast-test:" + row.id() + ":" + target.tokenId()
              : "push:admin-broadcast:" + row.campaignId() + ":" + target.tokenId();
      PreparePushDeliveryCommand command =
          new PreparePushDeliveryCommand(
              target.userId(),
              target.tokenId(),
              test ? NotificationType.ADMIN_BROADCAST_TEST : NotificationType.ADMIN_BROADCAST,
              key,
              campaign.content().title(),
              campaign.content().body(),
              campaign.content().deepLink());
      deliveries.prepareAdmin(command, target.id()).ifPresent(prepared::add);
    }
    repository.advanceCursor(row.id(), targets.stream().mapToLong(Target::id).max().orElse(0));
    return new Batch(List.copyOf(prepared), List.of());
  }

  /**
   * 외부 제출 직전 작업과 공용 선점의 유효성을 확인한다.
   *
   * @param work 소유 작업
   * @return 제출 가능 여부
   */
  @Transactional
  public boolean maySubmit(Work work) {
    repository.lockGate();
    owned(work);
    return repository.ownsSubmission(work.owner());
  }

  /**
   * 페이지 처리 후 이력 기반으로 다음 작업과 완료 상태를 결정한다.
   *
   * @param work 소유 작업
   */
  @Transactional
  public void finish(Work work) {
    repository.lockGate();
    Run row = owned(work);
    repository.releaseSubmission(work.owner());
    AdminPushRunView view = repository.view(row);
    String status =
        view.pendingCount() == 0
            ? "COMPLETED"
            : repository.unsent(row.id()) > 0 ? "SENDING" : "AWAITING_RECEIPTS";
    LocalDateTime next = repository.nextDue(row.id());
    LocalDateTime minimum = LocalDateTime.now().plusSeconds(1);
    repository.finish(
        row, work.owner(), status, next == null || next.isBefore(minimum) ? minimum : next);
  }

  /**
   * 소비 작업 장애를 기록한다.
   *
   * @param work 소유 작업
   */
  @Transactional
  public void failed(Work work) {
    repository.lockGate();
    Run row = requireRun(work.id());
    if (row.version() != work.version() || !work.owner().equals(row.owner())) {
      return;
    }
    repository.releaseSubmission(work.owner());
    repository.failure(row, work.owner());
  }

  /**
   * SQS 발행 실패를 같은 작업에 기록한다.
   *
   * @param publication 발행 시도
   */
  @Transactional
  public void publicationFailed(Run publication) {
    repository.lockGate();
    Run row = requireRun(publication.id());
    if (row.version() == publication.version() && row.owner() == null) {
      repository.failure(row, null);
    }
  }

  private Run owned(Work work) {
    Run row = requireRun(work.id());
    if (row.version() != work.version()
        || !work.owner().equals(row.owner())
        || row.leaseUntil() == null
        || !row.leaseUntil().isAfter(LocalDateTime.now())) {
      throw new IllegalStateException("작업 선점이 만료됐습니다.");
    }
    return row;
  }

  private Run requireRun(UUID id) {
    return repository.lockRun(id).stream()
        .findFirst()
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
  }

  private Campaign requireCampaign(UUID id) {
    return repository.campaign(id).stream()
        .findFirst()
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
  }

  private AdminPushCampaignView view(Campaign c) {
    List<AdminPushRunView> runs = repository.runs(c.id()).stream().map(repository::view).toList();
    AdminPushRunView broadcast =
        runs.stream().filter(r -> r.mode().equals("BROADCAST")).findFirst().orElse(null);
    return new AdminPushCampaignView(
        c.id(),
        c.content().title(),
        c.content().body(),
        c.content().deepLink(),
        c.adminId(),
        c.createdAt(),
        broadcast == null ? "DRAFT" : broadcast.status(),
        broadcast,
        runs.stream().filter(r -> r.mode().equals("TEST")).toList());
  }

  /**
   * 소비자가 가진 작업 소유권이다.
   *
   * @param id 실행 ID
   * @param version 작업 버전
   * @param owner 선점 식별자
   */
  public record Work(UUID id, long version, UUID owner) {}

  /**
   * 트랜잭션 밖에서 실행할 외부 작업이다.
   *
   * @param deliveries 선점된 최대 100개 발송
   * @param receiptIds 조회할 Receipt 이력 ID 목록
   */
  public record Batch(List<PreparedPushDelivery> deliveries, List<Long> receiptIds) {}
}
