// 관리자 문의 검색과 답장 이력을 조회한다.

package com.landit.landitbe.feature.mailbox.admin.feedback.service;

import com.landit.landitbe.feature.mailbox.admin.feedback.dto.AdminMailboxFeedbackDetailResponse;
import com.landit.landitbe.feature.mailbox.admin.feedback.dto.AdminMailboxFeedbackListResponse;
import com.landit.landitbe.feature.mailbox.admin.feedback.dto.AdminMailboxFeedbackResponse;
import com.landit.landitbe.feature.mailbox.feedback.domain.MailboxFeedbackSort;
import com.landit.landitbe.feature.mailbox.feedback.domain.UserFeedbackStatus;
import com.landit.landitbe.feature.mailbox.feedback.domain.UserFeedbackType;
import com.landit.landitbe.feature.mailbox.feedback.repository.AdminMailboxFeedbackRepository;
import com.landit.landitbe.feature.mailbox.feedback.repository.AdminMailboxFeedbackRepository.AdminMailboxFeedbackSummary;
import com.landit.landitbe.feature.mailbox.letter.domain.MailboxLetter;
import com.landit.landitbe.feature.mailbox.letter.repository.AdminMailboxLetterRepository;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관리자 문의 검색과 답장 이력을 조회한다. */
@Service
@RequiredArgsConstructor
public class AdminMailboxFeedbackQueryService {

  private static final int MAX_PAGE_SIZE = 100;

  private final AdminMailboxLetterRepository letterRepository;
  private final AdminMailboxFeedbackRepository feedbackRepository;

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
      MailboxFeedbackSort sort) {
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

  /**
   * 피드백 상세와 대표 피드백에 연결된 최신 답장을 조회한다.
   *
   * @param feedbackId 피드백 ID
   * @return 피드백 상세 응답
   * @throws ApiException 피드백이 없을 때
   */
  @Transactional(readOnly = true)
  public AdminMailboxFeedbackDetailResponse getFeedback(Long feedbackId) {
    AdminMailboxFeedbackSummary feedback =
        feedbackRepository
            .findSummaryByFeedbackId(feedbackId)
            .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
    Long representativeFeedbackId =
        feedback.getResolvedByFeedbackId() == null
            ? feedbackId
            : feedback.getResolvedByFeedbackId();
    Set<Long> replyFeedbackIds = new LinkedHashSet<>();
    replyFeedbackIds.add(representativeFeedbackId);
    replyFeedbackIds.add(feedbackId);
    MailboxLetter reply =
        letterRepository
            .findRepliesByFeedbackIds(
                feedback.getUserProfileId(), replyFeedbackIds, PageRequest.of(0, 1))
            .stream()
            .findFirst()
            .orElse(null);
    return new AdminMailboxFeedbackDetailResponse(
        feedback.getFeedbackId(),
        feedback.getUserProfileId(),
        feedback.getEmail(),
        feedback.getNickname(),
        feedback.getType(),
        feedback.getContent(),
        feedback.getStatus(),
        feedback.getResolvedByFeedbackId(),
        feedback.getCreatedAt(),
        feedback.getUpdatedAt(),
        toFeedbackReply(reply));
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

  private PageRequest feedbackPageRequest(int page, int size, MailboxFeedbackSort sort) {
    Sort.Direction direction =
        sort == MailboxFeedbackSort.OLDEST ? Sort.Direction.ASC : Sort.Direction.DESC;
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

  private AdminMailboxFeedbackDetailResponse.Reply toFeedbackReply(MailboxLetter letter) {
    if (letter == null) {
      return null;
    }
    return new AdminMailboxFeedbackDetailResponse.Reply(
        letter.getId(), letter.getTitle(), letter.getBodyText(), letter.getPublishedAt());
  }
}
