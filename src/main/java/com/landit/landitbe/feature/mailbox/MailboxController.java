// 편지함 사용자 API의 HTTP 요청을 처리한다.

package com.landit.landitbe.feature.mailbox;

import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.feature.mailbox.docs.MailboxControllerDocs;
import com.landit.landitbe.feature.mailbox.dto.MailboxFeedbackSubmitRequest;
import com.landit.landitbe.feature.mailbox.dto.MailboxReceivedDetailResponse;
import com.landit.landitbe.feature.mailbox.dto.MailboxReceivedListResponse;
import com.landit.landitbe.feature.mailbox.dto.MailboxSentFeedbackDetailResponse;
import com.landit.landitbe.feature.mailbox.dto.MailboxSentFeedbackListResponse;
import com.landit.landitbe.feature.mailbox.dto.MailboxUnreadCountResponse;
import com.landit.landitbe.feature.mailbox.service.MailboxService;
import com.landit.landitbe.shared.response.ApiResponse;
import jakarta.validation.Valid;
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
public class MailboxController implements MailboxControllerDocs {

  private final MailboxService mailboxService;

  /**
   * 편지함 Service를 주입받는다.
   *
   * @param mailboxService 편지함 Service
   */
  public MailboxController(MailboxService mailboxService) {
    this.mailboxService = mailboxService;
  }

  /** {@inheritDoc} */
  @Override
  @PostMapping("/api/v1/mailbox/feedbacks")
  public ResponseEntity<ApiResponse<Void>> submitFeedback(
      @AuthenticationPrincipal AuthUserPrincipal principal,
      @Valid @RequestBody MailboxFeedbackSubmitRequest request) {
    mailboxService.submitFeedback(principal.userId(), request);
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
        ApiResponse.success(mailboxService.getSentFeedbacks(principal.userId(), cursor, size)));
  }

  /** {@inheritDoc} */
  @Override
  @GetMapping("/api/v1/mailbox/sent/{feedbackId}")
  public ResponseEntity<ApiResponse<MailboxSentFeedbackDetailResponse>> getSentFeedback(
      @AuthenticationPrincipal AuthUserPrincipal principal, @PathVariable long feedbackId) {
    return ResponseEntity.ok(
        ApiResponse.success(mailboxService.getSentFeedback(principal.userId(), feedbackId)));
  }

  /** {@inheritDoc} */
  @Override
  @GetMapping("/api/v1/mailbox/received")
  public ResponseEntity<ApiResponse<MailboxReceivedListResponse>> getReceivedLetters(
      @AuthenticationPrincipal AuthUserPrincipal principal,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") int size) {
    return ResponseEntity.ok(
        ApiResponse.success(mailboxService.getReceivedLetters(principal.userId(), cursor, size)));
  }

  /** {@inheritDoc} */
  @Override
  @GetMapping("/api/v1/mailbox/received/{letterId}")
  public ResponseEntity<ApiResponse<MailboxReceivedDetailResponse>> getReceivedLetter(
      @AuthenticationPrincipal AuthUserPrincipal principal, @PathVariable long letterId) {
    return ResponseEntity.ok(
        ApiResponse.success(mailboxService.getReceivedLetter(principal.userId(), letterId)));
  }

  /** {@inheritDoc} */
  @Override
  @GetMapping("/api/v1/mailbox/unread-count")
  public ResponseEntity<ApiResponse<MailboxUnreadCountResponse>> getUnreadCount(
      @AuthenticationPrincipal AuthUserPrincipal principal) {
    return ResponseEntity.ok(
        ApiResponse.success(mailboxService.getUnreadCount(principal.userId())));
  }
}
