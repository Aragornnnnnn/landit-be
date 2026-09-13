// 편지함 어드민 API의 HTTP 요청을 처리한다.

package com.landit.landitbe.feature.mailbox;

import com.landit.landitbe.feature.mailbox.docs.AdminMailboxControllerDocs;
import com.landit.landitbe.feature.mailbox.feedback.domain.MailboxFeedbackSort;
import com.landit.landitbe.feature.mailbox.feedback.domain.UserFeedbackStatus;
import com.landit.landitbe.feature.mailbox.feedback.domain.UserFeedbackType;
import com.landit.landitbe.feature.mailbox.feedback.dto.AdminMailboxFeedbackDetailResponse;
import com.landit.landitbe.feature.mailbox.feedback.dto.AdminMailboxFeedbackListResponse;
import com.landit.landitbe.feature.mailbox.feedback.dto.AdminMailboxReplyRequest;
import com.landit.landitbe.feature.mailbox.feedback.dto.AdminMailboxReplyResponse;
import com.landit.landitbe.feature.mailbox.feedback.service.AdminMailboxFeedbackQueryService;
import com.landit.landitbe.feature.mailbox.feedback.service.AdminMailboxReplyService;
import com.landit.landitbe.feature.mailbox.letter.domain.MailboxLetterType;
import com.landit.landitbe.feature.mailbox.letter.domain.MailboxPublicationStatus;
import com.landit.landitbe.feature.mailbox.letter.dto.AdminMailboxLetterCreateRequest;
import com.landit.landitbe.feature.mailbox.letter.dto.AdminMailboxLetterListResponse;
import com.landit.landitbe.feature.mailbox.letter.dto.AdminMailboxLetterPatchRequest;
import com.landit.landitbe.feature.mailbox.letter.dto.AdminMailboxLetterResponse;
import com.landit.landitbe.feature.mailbox.letter.service.AdminMailboxLetterService;
import com.landit.landitbe.shared.response.ApiResponse;
import com.landit.landitbe.shared.security.AuthUserPrincipal;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 편지함 어드민 API의 HTTP 요청을 처리한다. */
@RestController
@RequiredArgsConstructor
public class AdminMailboxController implements AdminMailboxControllerDocs {

  private final AdminMailboxFeedbackQueryService adminMailboxFeedbackQueryService;
  private final AdminMailboxReplyService adminMailboxReplyService;
  private final AdminMailboxLetterService adminMailboxLetterService;

  /** {@inheritDoc} */
  @Override
  @GetMapping("/api/v1/admin/mailbox/letters")
  public ApiResponse<AdminMailboxLetterListResponse> getLetters(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) MailboxLetterType type,
      @RequestParam(required = false) MailboxPublicationStatus publicationStatus,
      @RequestParam(required = false) Boolean pinned) {
    return ApiResponse.success(
        adminMailboxLetterService.getLetters(page, size, type, publicationStatus, pinned));
  }

  /** {@inheritDoc} */
  @Override
  @PostMapping("/api/v1/admin/mailbox/letters")
  public ResponseEntity<ApiResponse<AdminMailboxLetterResponse>> createLetter(
      @AuthenticationPrincipal AuthUserPrincipal principal,
      @Valid @RequestBody AdminMailboxLetterCreateRequest request) {
    return ApiResponse.success(
        HttpStatus.CREATED, adminMailboxLetterService.createLetter(principal.userId(), request));
  }

  /** {@inheritDoc} */
  @Override
  @PatchMapping("/api/v1/admin/mailbox/letters/{letterId}")
  public ApiResponse<AdminMailboxLetterResponse> updateLetter(
      @AuthenticationPrincipal AuthUserPrincipal principal,
      @PathVariable Long letterId,
      @Valid @RequestBody AdminMailboxLetterPatchRequest request) {
    return ApiResponse.success(
        adminMailboxLetterService.updateLetter(principal.userId(), letterId, request));
  }

  /** {@inheritDoc} */
  @Override
  @GetMapping("/api/v1/admin/mailbox/feedbacks")
  public ApiResponse<AdminMailboxFeedbackListResponse> getFeedbacks(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) UserFeedbackType type,
      @RequestParam(required = false) UserFeedbackStatus status,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate createdFrom,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate createdTo,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "NEWEST") MailboxFeedbackSort sort) {
    return ApiResponse.success(
        adminMailboxFeedbackQueryService.getFeedbacks(
            keyword, type, status, createdFrom, createdTo, page, size, sort));
  }

  /** {@inheritDoc} */
  @Override
  @GetMapping("/api/v1/admin/mailbox/feedbacks/{feedbackId}")
  public ApiResponse<AdminMailboxFeedbackDetailResponse> getFeedback(
      @PathVariable Long feedbackId) {
    return ApiResponse.success(adminMailboxFeedbackQueryService.getFeedback(feedbackId));
  }

  /** {@inheritDoc} */
  @Override
  @PostMapping("/api/v1/admin/mailbox/replies")
  public ResponseEntity<ApiResponse<AdminMailboxReplyResponse>> sendReplies(
      @AuthenticationPrincipal AuthUserPrincipal principal,
      @Valid @RequestBody AdminMailboxReplyRequest request) {
    return ApiResponse.success(
        HttpStatus.CREATED, adminMailboxReplyService.sendReplies(principal.userId(), request));
  }
}
