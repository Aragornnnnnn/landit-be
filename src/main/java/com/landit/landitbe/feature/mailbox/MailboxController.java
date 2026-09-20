// 편지함 사용자 API의 HTTP 요청을 처리한다.

package com.landit.landitbe.feature.mailbox;

import com.landit.landitbe.feature.mailbox.docs.MailboxControllerDocs;
import com.landit.landitbe.feature.mailbox.dto.MailboxUnreadCountResponse;
import com.landit.landitbe.feature.mailbox.feedback.dto.MailboxFeedbackSubmitRequest;
import com.landit.landitbe.feature.mailbox.feedback.dto.MailboxSentFeedbackDetailResponse;
import com.landit.landitbe.feature.mailbox.feedback.dto.MailboxSentFeedbackListResponse;
import com.landit.landitbe.feature.mailbox.feedback.service.MailboxFeedbackService;
import com.landit.landitbe.feature.mailbox.letter.dto.MailboxReceivedDetailResponse;
import com.landit.landitbe.feature.mailbox.letter.dto.MailboxReceivedListResponse;
import com.landit.landitbe.feature.mailbox.letter.service.MailboxLetterService;
import com.landit.landitbe.shared.response.ApiResponse;
import com.landit.landitbe.shared.security.AuthUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 편지함 사용자 API의 HTTP 요청을 처리한다. */
@RestController
@RequiredArgsConstructor
public class MailboxController implements MailboxControllerDocs {

  private final MailboxFeedbackService mailboxFeedbackService;
  private final MailboxLetterService mailboxLetterService;

  /** {@inheritDoc} */
  @Override
  @PostMapping("/api/v1/mailbox/feedbacks")
  public ResponseEntity<ApiResponse<Void>> submitFeedback(
      @AuthenticationPrincipal AuthUserPrincipal principal,
      @Valid @RequestBody MailboxFeedbackSubmitRequest request) {
    mailboxFeedbackService.submitFeedback(principal.userId(), request);
    return ApiResponse.success(HttpStatus.CREATED, null);
  }

  /** {@inheritDoc} */
  @Override
  @GetMapping("/api/v1/mailbox/sent")
  public ResponseEntity<ApiResponse<MailboxSentFeedbackListResponse>> getSentFeedbacks(
      @AuthenticationPrincipal AuthUserPrincipal principal,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") int size) {
    return ResponseEntity.ok(
        ApiResponse.success(
            mailboxFeedbackService.getSentFeedbacks(principal.userId(), cursor, size)));
  }

  /** {@inheritDoc} */
  @Override
  @GetMapping("/api/v1/mailbox/sent/{feedbackId}")
  public ResponseEntity<ApiResponse<MailboxSentFeedbackDetailResponse>> getSentFeedback(
      @AuthenticationPrincipal AuthUserPrincipal principal, @PathVariable long feedbackId) {
    return ResponseEntity.ok(
        ApiResponse.success(
            mailboxFeedbackService.getSentFeedback(principal.userId(), feedbackId)));
  }

  /** {@inheritDoc} */
  @Override
  @GetMapping("/api/v1/mailbox/received")
  public ResponseEntity<ApiResponse<MailboxReceivedListResponse>> getReceivedLetters(
      @AuthenticationPrincipal AuthUserPrincipal principal,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") int size) {
    return ResponseEntity.ok(
        ApiResponse.success(
            mailboxLetterService.getReceivedLetters(principal.userId(), cursor, size)));
  }

  /** {@inheritDoc} */
  @Override
  @GetMapping("/api/v1/mailbox/received/{letterId}")
  public ResponseEntity<ApiResponse<MailboxReceivedDetailResponse>> getReceivedLetter(
      @AuthenticationPrincipal AuthUserPrincipal principal, @PathVariable long letterId) {
    return ResponseEntity.ok(
        ApiResponse.success(mailboxLetterService.getReceivedLetter(principal.userId(), letterId)));
  }

  /** {@inheritDoc} */
  @Override
  @GetMapping("/api/v1/mailbox/unread-count")
  public ResponseEntity<ApiResponse<MailboxUnreadCountResponse>> getUnreadCount(
      @AuthenticationPrincipal AuthUserPrincipal principal) {
    return ResponseEntity.ok(
        ApiResponse.success(mailboxLetterService.getUnreadCount(principal.userId())));
  }
}
