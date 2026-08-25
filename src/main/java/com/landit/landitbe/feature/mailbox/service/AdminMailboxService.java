// 편지함 어드민 콘텐츠 관리와 피드백 일괄 답장을 담당한다.

package com.landit.landitbe.feature.mailbox.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.landitbe.feature.admin.domain.AdminAction;
import com.landit.landitbe.feature.admin.service.AdminAuditService;
import com.landit.landitbe.feature.mailbox.domain.MailboxFeedback;
import com.landit.landitbe.feature.mailbox.domain.MailboxLetter;
import com.landit.landitbe.feature.mailbox.domain.MailboxLetterRecipient;
import com.landit.landitbe.feature.mailbox.domain.MailboxLetterType;
import com.landit.landitbe.feature.mailbox.domain.MailboxPublicationStatus;
import com.landit.landitbe.feature.mailbox.domain.UserFeedbackStatus;
import com.landit.landitbe.feature.mailbox.domain.UserFeedbackType;
import com.landit.landitbe.feature.mailbox.dto.AdminMailboxFeedbackListResponse;
import com.landit.landitbe.feature.mailbox.dto.AdminMailboxFeedbackResponse;
import com.landit.landitbe.feature.mailbox.dto.AdminMailboxLetterCreateRequest;
import com.landit.landitbe.feature.mailbox.dto.AdminMailboxLetterListResponse;
import com.landit.landitbe.feature.mailbox.dto.AdminMailboxLetterPatchRequest;
import com.landit.landitbe.feature.mailbox.dto.AdminMailboxLetterResponse;
import com.landit.landitbe.feature.mailbox.dto.AdminMailboxReplyRequest;
import com.landit.landitbe.feature.mailbox.dto.AdminMailboxReplyResponse;
import com.landit.landitbe.feature.mailbox.repository.AdminMailboxFeedbackRepository;
import com.landit.landitbe.feature.mailbox.repository.AdminMailboxFeedbackRepository.AdminMailboxFeedbackSummary;
import com.landit.landitbe.feature.mailbox.repository.AdminMailboxLetterRecipientRepository;
import com.landit.landitbe.feature.mailbox.repository.AdminMailboxLetterRepository;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 편지함 어드민 콘텐츠 관리와 피드백 일괄 답장을 담당한다. */
@Service
public class AdminMailboxService {

  private static final int MAX_PAGE_SIZE = 100;
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final AdminMailboxLetterRepository letterRepository;
  private final AdminMailboxFeedbackRepository feedbackRepository;
  private final AdminMailboxLetterRecipientRepository recipientRepository;
  private final AdminAuditService adminAuditService;
  private final EntityManager entityManager;

  /**
   * 편지함 어드민 저장소와 감사 Service를 주입받는다.
   *
   * @param letterRepository 어드민 편지 저장소
   * @param feedbackRepository 어드민 피드백 저장소
   * @param recipientRepository 편지 수신자 저장소
   * @param adminAuditService 관리자 감사 Service
   * @param entityManager 영속 상태를 DB 저장 값과 동기화할 EntityManager
   */
  public AdminMailboxService(
      AdminMailboxLetterRepository letterRepository,
      AdminMailboxFeedbackRepository feedbackRepository,
      AdminMailboxLetterRecipientRepository recipientRepository,
      AdminAuditService adminAuditService,
      EntityManager entityManager) {
    this.letterRepository = letterRepository;
    this.feedbackRepository = feedbackRepository;
    this.recipientRepository = recipientRepository;
    this.adminAuditService = adminAuditService;
    this.entityManager = entityManager;
  }

  /**
   * 공지·업데이트 초안을 생성한다.
   *
   * @param adminUserProfileId 작업 관리자 ID
   * @param request 초안 생성 요청
   * @return 생성된 초안
   * @throws ApiException 요청 본문이 올바르지 않을 때
   */
  @Transactional
  public AdminMailboxLetterResponse createLetter(
      Long adminUserProfileId, AdminMailboxLetterCreateRequest request) {
    validateLetterType(request.type());
    JsonNode contentBlocks = toJsonNode(request.contentBlocks());
    validateContent(request.title(), contentBlocks, request.preview());
    MailboxLetter letter =
        letterRepository.save(
            new MailboxLetter(
                request.type(),
                request.title(),
                contentBlocks,
                null,
                request.preview(),
                MailboxPublicationStatus.DRAFT,
                false,
                null));
    adminAuditService.record(
        adminUserProfileId,
        AdminAction.MAILBOX_LETTER_CREATED,
        "MAILBOX_LETTER",
        String.valueOf(letter.getId()),
        null,
        letterSummary(letter));
    return AdminMailboxLetterResponse.from(letter);
  }

  /**
   * 어드민 편지 목록을 페이지로 조회한다.
   *
   * @param page 0부터 시작하는 페이지 번호
   * @param size 페이지 크기
   * @param type 편지 유형 필터
   * @param publicationStatus 게시 상태 필터
   * @param pinned 상단 고정 필터
   * @return 어드민 편지 페이지
   * @throws ApiException 페이지 조건이 올바르지 않을 때
   */
  @Transactional(readOnly = true)
  public AdminMailboxLetterListResponse getLetters(
      int page,
      int size,
      MailboxLetterType type,
      MailboxPublicationStatus publicationStatus,
      Boolean pinned) {
    validatePage(page, size);
    validateLetterType(type);
    Page<MailboxLetter> letters =
        letterRepository.search(type, publicationStatus, pinned, PageRequest.of(page, size));
    return new AdminMailboxLetterListResponse(
        letters.getContent().stream().map(AdminMailboxLetterResponse::from).toList(),
        page,
        size,
        letters.getTotalElements(),
        letters.getTotalPages());
  }

  /**
   * 공지·업데이트의 콘텐츠와 게시 상태를 수정한다.
   *
   * @param adminUserProfileId 작업 관리자 ID
   * @param letterId 수정할 편지 ID
   * @param request 부분 수정 요청
   * @return 수정된 편지
   * @throws ApiException 편지가 없거나 상태 전이가 올바르지 않을 때
   */
  @Transactional
  public AdminMailboxLetterResponse updateLetter(
      Long adminUserProfileId, Long letterId, AdminMailboxLetterPatchRequest request) {
    if (!request.hasChanges()) {
      throw new ApiException(ErrorCode.INVALID_REQUEST, "수정할 항목이 없습니다.");
    }
    MailboxLetter letter = requireLetterForUpdate(letterId);
    final String beforeValue = letterSummary(letter);

    updateContent(letter, request);
    if (request.publicationStatus() != null) {
      updatePublicationStatus(letter, request.publicationStatus());
    }
    updatePinned(letter, request.pinned());

    adminAuditService.record(
        adminUserProfileId,
        AdminAction.MAILBOX_LETTER_UPDATED,
        "MAILBOX_LETTER",
        String.valueOf(letterId),
        beforeValue,
        letterSummary(letter) + ",changedFields=" + changedFields(request));
    letterRepository.flush();
    // DB가 보존한 timestamp 정밀도를 응답에도 동일하게 반영한다.
    entityManager.refresh(letter);
    return AdminMailboxLetterResponse.from(letter);
  }

  /**
   * 어드민 피드백을 검색·필터링해 페이지로 조회한다.
   *
   * @param keyword 본문 검색어
   * @param type 피드백 유형
   * @param status 처리 상태
   * @param createdFrom 검색 시작일
   * @param createdTo 검색 종료일
   * @param page 0부터 시작하는 페이지 번호
   * @param size 페이지 크기
   * @param sort 정렬 방향
   * @return 어드민 피드백 페이지
   * @throws ApiException 페이지나 기간이 올바르지 않을 때
   */
  @Transactional(readOnly = true)
  public AdminMailboxFeedbackListResponse getFeedbacks(
      String keyword,
      UserFeedbackType type,
      UserFeedbackStatus status,
      LocalDate createdFrom,
      LocalDate createdTo,
      int page,
      int size,
      FeedbackSort sort) {
    validatePage(page, size);
    validateFeedbackSearchPeriod(createdFrom, createdTo);
    LocalDateTime from = createdFrom == null ? null : createdFrom.atStartOfDay();
    LocalDateTime to = createdTo == null ? null : createdTo.plusDays(1).atStartOfDay();
    String normalizedKeyword = normalizeSearchKeyword(keyword);
    PageRequest pageRequest = feedbackPageRequest(page, size, sort);
    Page<AdminMailboxFeedbackSummary> feedbacks =
        findFeedbacks(normalizedKeyword, type, status, from, to, pageRequest);
    return new AdminMailboxFeedbackListResponse(
        feedbacks.getContent().stream().map(this::toFeedbackResponse).toList(),
        page,
        size,
        feedbacks.getTotalElements(),
        feedbacks.getTotalPages());
  }

  private Page<AdminMailboxFeedbackSummary> findFeedbacks(
      String keyword,
      UserFeedbackType type,
      UserFeedbackStatus status,
      LocalDateTime from,
      LocalDateTime to,
      PageRequest pageRequest) {
    if (from == null && to == null) {
      return feedbackRepository.searchWithoutCreatedRange(keyword, type, status, pageRequest);
    }
    if (from == null) {
      return feedbackRepository.searchWithoutCreatedFrom(keyword, type, status, to, pageRequest);
    }
    if (to == null) {
      return feedbackRepository.searchWithoutCreatedTo(keyword, type, status, from, pageRequest);
    }
    return feedbackRepository.search(keyword, type, status, from, to, pageRequest);
  }

  /**
   * 여러 사용자에게 같은 답장을 보내고 선택 피드백을 처리 완료한다.
   *
   * @param adminUserProfileId 작업 관리자 ID
   * @param request 일괄 답장 요청
   * @return 일괄 답장 처리 결과
   * @throws ApiException 피드백 ID가 없거나 요청 값이 올바르지 않을 때
   */
  @Transactional
  public AdminMailboxReplyResponse sendReplies(
      Long adminUserProfileId, AdminMailboxReplyRequest request) {
    List<MailboxFeedback> feedbacks = findFeedbacksForReply(request.feedbackIds());
    MailboxLetter reply = createReply(request);
    Map<Long, List<MailboxFeedback>> feedbacksByUser = groupFeedbacksByUser(feedbacks);
    Map<Long, MailboxFeedback> representatives = findRepresentatives(feedbacksByUser);
    int completedFeedbackCount = completePendingFeedbacks(feedbacksByUser, representatives);
    List<MailboxLetterRecipient> recipients = createRecipients(reply.getId(), representatives);
    recipientRepository.saveAll(recipients);

    List<Long> representativeFeedbackIds =
        representatives.values().stream().map(MailboxFeedback::getId).toList();
    adminAuditService.record(
        adminUserProfileId,
        AdminAction.MAILBOX_REPLY_SENT,
        "MAILBOX_REPLY_BATCH",
        String.valueOf(reply.getId()),
        null,
        "recipientCount=%d,completedFeedbackCount=%d,representativeFeedbackIds=%s"
            .formatted(recipients.size(), completedFeedbackCount, representativeFeedbackIds));
    return new AdminMailboxReplyResponse(
        reply.getId(), recipients.size(), completedFeedbackCount, representativeFeedbackIds);
  }

  private MailboxLetter requireLetterForUpdate(Long letterId) {
    MailboxLetter letter =
        letterRepository
            .findByIdForUpdate(letterId)
            .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
    if (letter.getLetterType() == MailboxLetterType.REPLY) {
      throw new ApiException(ErrorCode.INVALID_REQUEST, "답장 편지는 수정할 수 없습니다.");
    }
    return letter;
  }

  private void updateContent(MailboxLetter letter, AdminMailboxLetterPatchRequest request) {
    if (!hasContentChange(request)) {
      return;
    }
    MailboxLetterType type = request.type() == null ? letter.getLetterType() : request.type();
    String title = request.title() == null ? letter.getTitle() : request.title();
    String preview = request.preview() == null ? letter.getPreviewText() : request.preview();
    JsonNode contentBlocks =
        request.contentBlocks() == null
            ? letter.getContentBlocks()
            : toJsonNode(request.contentBlocks());

    validateLetterType(type);
    validateContent(title, contentBlocks, preview);
    letter.updateContent(type, title, contentBlocks, preview);
  }

  private void updatePinned(MailboxLetter letter, Boolean pinned) {
    if (pinned == null) {
      return;
    }
    if (pinned && letter.getPublicationStatus() != MailboxPublicationStatus.PUBLISHED) {
      throw new ApiException(ErrorCode.INVALID_REQUEST, "게시된 편지만 고정할 수 있습니다.");
    }
    letter.changePinned(pinned);
  }

  private void updatePublicationStatus(
      MailboxLetter letter, MailboxPublicationStatus publicationStatus) {
    if (publicationStatus == letter.getPublicationStatus()) {
      return;
    }
    // 초안은 처음 게시할 수 있고, 이후에는 게시와 게시 중단 사이만 이동한다.
    boolean validTransition =
        (letter.getPublicationStatus() == MailboxPublicationStatus.DRAFT
                && publicationStatus == MailboxPublicationStatus.PUBLISHED)
            || (letter.getPublicationStatus() == MailboxPublicationStatus.PUBLISHED
                && publicationStatus == MailboxPublicationStatus.UNPUBLISHED)
            || (letter.getPublicationStatus() == MailboxPublicationStatus.UNPUBLISHED
                && publicationStatus == MailboxPublicationStatus.PUBLISHED);
    if (!validTransition) {
      throw new ApiException(ErrorCode.INVALID_REQUEST, "편지 게시 상태를 변경할 수 없습니다.");
    }
    letter.changePublicationStatus(
        publicationStatus,
        publicationStatus == MailboxPublicationStatus.PUBLISHED ? LocalDateTime.now() : null);
  }

  private boolean hasContentChange(AdminMailboxLetterPatchRequest request) {
    return request.type() != null
        || request.title() != null
        || request.contentBlocks() != null
        || request.preview() != null;
  }

  private void validateLetterType(MailboxLetterType type) {
    if (type == MailboxLetterType.REPLY) {
      throw new ApiException(ErrorCode.INVALID_REQUEST, "공지와 업데이트만 관리할 수 있습니다.");
    }
  }

  private JsonNode toJsonNode(Object contentBlocks) {
    return OBJECT_MAPPER.valueToTree(contentBlocks);
  }

  private void validateContent(String title, JsonNode contentBlocks, String preview) {
    if (title == null || title.isBlank() || preview == null || preview.isBlank()) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED);
    }
    if (contentBlocks == null || !contentBlocks.isArray() || contentBlocks.isEmpty()) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "본문 블록은 비어 있지 않은 배열이어야 합니다.");
    }
  }

  private void validatePage(int page, int size) {
    if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED);
    }
  }

  private void validateFeedbackSearchPeriod(LocalDate createdFrom, LocalDate createdTo) {
    if (createdFrom != null && createdTo != null && createdFrom.isAfter(createdTo)) {
      throw new ApiException(ErrorCode.INVALID_REQUEST, "검색 시작일은 종료일보다 늦을 수 없습니다.");
    }
  }

  private PageRequest feedbackPageRequest(int page, int size, FeedbackSort sort) {
    Sort.Direction direction =
        sort == FeedbackSort.OLDEST ? Sort.Direction.ASC : Sort.Direction.DESC;
    Sort createdOrder = Sort.by(direction, "createdAt").and(Sort.by(direction, "id"));
    return PageRequest.of(page, size, createdOrder);
  }

  private String normalizeSearchKeyword(String keyword) {
    if (keyword == null || keyword.isBlank()) {
      return null;
    }
    // 사용자가 입력한 LIKE 예약 문자를 와일드카드가 아닌 검색어로 취급한다.
    return keyword.trim().replace("!", "!!").replace("%", "!%").replace("_", "!_");
  }

  private List<Long> uniqueFeedbackIds(List<Long> feedbackIds) {
    if (feedbackIds == null
        || feedbackIds.isEmpty()
        || feedbackIds.stream().anyMatch(id -> id == null)) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED);
    }
    Set<Long> uniqueIds = new LinkedHashSet<>(feedbackIds);
    if (uniqueIds.size() != feedbackIds.size()) {
      throw new ApiException(ErrorCode.INVALID_REQUEST, "피드백 ID가 중복됐습니다.");
    }
    return uniqueIds.stream().sorted().toList();
  }

  private List<MailboxFeedback> findFeedbacksForReply(List<Long> requestedFeedbackIds) {
    List<Long> feedbackIds = uniqueFeedbackIds(requestedFeedbackIds);
    List<MailboxFeedback> feedbacks = feedbackRepository.findAllByIdInOrderByIdAsc(feedbackIds);
    if (feedbacks.size() != feedbackIds.size()) {
      throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "피드백을 찾을 수 없습니다.");
    }
    return feedbacks;
  }

  private MailboxLetter createReply(AdminMailboxReplyRequest request) {
    LocalDateTime publishedAt = LocalDateTime.now();
    return letterRepository.save(
        new MailboxLetter(
            MailboxLetterType.REPLY,
            request.title(),
            null,
            request.bodyText(),
            request.bodyText(),
            MailboxPublicationStatus.PUBLISHED,
            false,
            publishedAt));
  }

  private Map<Long, List<MailboxFeedback>> groupFeedbacksByUser(List<MailboxFeedback> feedbacks) {
    return feedbacks.stream()
        .collect(
            Collectors.groupingBy(
                MailboxFeedback::getUserProfileId, LinkedHashMap::new, Collectors.toList()));
  }

  /** 사용자별로 작성 시각과 ID가 가장 작은 피드백을 대표 건으로 선택한다. */
  private Map<Long, MailboxFeedback> findRepresentatives(
      Map<Long, List<MailboxFeedback>> feedbacksByUser) {
    Map<Long, MailboxFeedback> representatives = new LinkedHashMap<>();
    feedbacksByUser.forEach(
        (userProfileId, feedbacks) ->
            representatives.put(userProfileId, findRepresentative(feedbacks)));
    return representatives;
  }

  /** 이미 완료된 피드백은 유지하고 대기 중인 피드백만 사용자별 대표 건과 함께 완료한다. */
  private int completePendingFeedbacks(
      Map<Long, List<MailboxFeedback>> feedbacksByUser,
      Map<Long, MailboxFeedback> representatives) {
    int completedFeedbackCount = 0;
    for (Map.Entry<Long, List<MailboxFeedback>> entry : feedbacksByUser.entrySet()) {
      MailboxFeedback representative = representatives.get(entry.getKey());
      for (MailboxFeedback feedback : entry.getValue()) {
        if (feedback.getProcessingStatus() == UserFeedbackStatus.PENDING) {
          feedback.complete(feedback.equals(representative) ? null : representative.getId());
          completedFeedbackCount++;
        }
      }
    }
    return completedFeedbackCount;
  }

  private List<MailboxLetterRecipient> createRecipients(
      Long replyId, Map<Long, MailboxFeedback> representatives) {
    return representatives.entrySet().stream()
        .map(entry -> new MailboxLetterRecipient(replyId, entry.getKey(), entry.getValue().getId()))
        .toList();
  }

  private MailboxFeedback findRepresentative(Collection<MailboxFeedback> feedbacks) {
    return feedbacks.stream()
        .min(
            Comparator.comparing(MailboxFeedback::getCreatedAt)
                .thenComparing(MailboxFeedback::getId))
        .orElseThrow();
  }

  private AdminMailboxFeedbackResponse toFeedbackResponse(AdminMailboxFeedbackSummary feedback) {
    return new AdminMailboxFeedbackResponse(
        feedback.getFeedbackId(),
        feedback.getUserProfileId(),
        feedback.getEmail(),
        feedback.getNickname(),
        feedback.getType(),
        feedback.getContent(),
        feedback.getStatus(),
        feedback.getResolvedByFeedbackId(),
        feedback.getCreatedAt(),
        feedback.getUpdatedAt());
  }

  private String letterSummary(MailboxLetter letter) {
    return "type=%s,status=%s,pinned=%s,publishedAt=%s"
        .formatted(
            letter.getLetterType(),
            letter.getPublicationStatus(),
            letter.isPinned(),
            letter.getPublishedAt());
  }

  private String changedFields(AdminMailboxLetterPatchRequest request) {
    List<String> changedFields = new ArrayList<>();
    if (request.type() != null) {
      changedFields.add("type");
    }
    if (request.title() != null) {
      changedFields.add("title");
    }
    if (request.contentBlocks() != null) {
      changedFields.add("contentBlocks");
    }
    if (request.preview() != null) {
      changedFields.add("preview");
    }
    if (request.publicationStatus() != null) {
      changedFields.add("publicationStatus");
    }
    if (request.pinned() != null) {
      changedFields.add("pinned");
    }
    return String.join(",", changedFields);
  }

  /** 어드민 피드백 목록 정렬 방향이다. */
  public enum FeedbackSort {
    /** 최신 피드백부터 정렬한다. */
    NEWEST,

    /** 오래된 피드백부터 정렬한다. */
    OLDEST
  }
}
