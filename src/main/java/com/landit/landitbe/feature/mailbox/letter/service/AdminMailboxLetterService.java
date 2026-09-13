// 관리자 편지 초안·게시와 수정 흐름을 처리한다.

package com.landit.landitbe.feature.mailbox.letter.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.landitbe.feature.audit.domain.AdminAction;
import com.landit.landitbe.feature.audit.service.AdminAuditService;
import com.landit.landitbe.feature.mailbox.letter.domain.MailboxLetter;
import com.landit.landitbe.feature.mailbox.letter.domain.MailboxLetterType;
import com.landit.landitbe.feature.mailbox.letter.domain.MailboxPublicationStatus;
import com.landit.landitbe.feature.mailbox.letter.dto.AdminMailboxLetterCreateRequest;
import com.landit.landitbe.feature.mailbox.letter.dto.AdminMailboxLetterListResponse;
import com.landit.landitbe.feature.mailbox.letter.dto.AdminMailboxLetterPatchRequest;
import com.landit.landitbe.feature.mailbox.letter.dto.AdminMailboxLetterResponse;
import com.landit.landitbe.feature.mailbox.letter.repository.AdminMailboxLetterRepository;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관리자 편지 초안·게시와 수정 흐름을 처리한다. */
@Service
@RequiredArgsConstructor
public class AdminMailboxLetterService {

  private static final int MAX_PAGE_SIZE = 100;
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final AdminMailboxLetterRepository letterRepository;
  private final AdminAuditService adminAuditService;
  private final EntityManager entityManager;

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
}
