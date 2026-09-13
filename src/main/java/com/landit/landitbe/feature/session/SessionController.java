// 기존 학습 세션을 조작하는 API 요청을 처리한다.

package com.landit.landitbe.feature.session;

import com.landit.landitbe.feature.session.assessment.dto.SessionLevelAssessmentResponse;
import com.landit.landitbe.feature.session.assessment.service.SessionLevelAssessmentGenerationService;
import com.landit.landitbe.feature.session.docs.SessionControllerDocs;
import com.landit.landitbe.feature.session.feedback.dto.SessionFeedbackResponse;
import com.landit.landitbe.feature.session.feedback.service.SessionFeedbackService;
import com.landit.landitbe.feature.session.scenario.dto.SessionInnerThoughtResponse;
import com.landit.landitbe.feature.session.scenario.dto.SessionMessageSubmitRequest;
import com.landit.landitbe.feature.session.scenario.dto.SessionMessageSubmitResponse;
import com.landit.landitbe.feature.session.scenario.service.SessionInnerThoughtQueryService;
import com.landit.landitbe.feature.session.scenario.service.SessionMessageSubmitService;
import com.landit.landitbe.feature.session.service.LearningSessionService;
import com.landit.landitbe.shared.response.ApiResponse;
import com.landit.landitbe.shared.security.AuthUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 기존 학습 세션을 조작하는 API 요청을 처리한다. */
@RequiredArgsConstructor
@RestController
public class SessionController implements SessionControllerDocs {

  private final LearningSessionService learningSessionService;
  private final SessionFeedbackService sessionFeedbackService;
  private final SessionMessageSubmitService sessionMessageSubmitService;
  private final SessionInnerThoughtQueryService sessionInnerThoughtQueryService;
  private final SessionLevelAssessmentGenerationService levelAssessmentGenerationService;

  /** {@inheritDoc} */
  @Override
  @PostMapping("/api/v1/sessions/{sessionId}/messages")
  public ResponseEntity<ApiResponse<SessionMessageSubmitResponse>> submitMessage(
      @AuthenticationPrincipal AuthUserPrincipal principal,
      @PathVariable Long sessionId,
      @RequestBody SessionMessageSubmitRequest request) {
    return ApiResponse.success(
        HttpStatus.OK,
        sessionMessageSubmitService.submitMessage(principal.userId(), sessionId, request));
  }

  /** {@inheritDoc} */
  @Override
  @GetMapping("/api/v1/sessions/{sessionId}/messages/{messageId}/inner-thought")
  public ResponseEntity<ApiResponse<SessionInnerThoughtResponse>> getInnerThought(
      @AuthenticationPrincipal AuthUserPrincipal principal,
      @PathVariable Long sessionId,
      @PathVariable Long messageId) {
    return ApiResponse.success(
        HttpStatus.OK,
        sessionInnerThoughtQueryService.get(principal.userId(), sessionId, messageId));
  }

  /** {@inheritDoc} */
  @Override
  @PostMapping("/api/v1/sessions/{sessionId}/feedback")
  public ResponseEntity<ApiResponse<SessionFeedbackResponse>> getOrCreateFeedback(
      @AuthenticationPrincipal AuthUserPrincipal principal, @PathVariable Long sessionId) {
    return ApiResponse.success(
        HttpStatus.OK, sessionFeedbackService.getOrCreate(principal.userId(), sessionId));
  }

  /** {@inheritDoc} */
  @Override
  @GetMapping("/api/v1/sessions/{sessionId}/level-assessment")
  public ResponseEntity<ApiResponse<SessionLevelAssessmentResponse>> getLevelAssessment(
      @AuthenticationPrincipal AuthUserPrincipal principal, @PathVariable Long sessionId) {
    return ApiResponse.success(
        HttpStatus.OK, levelAssessmentGenerationService.get(principal.userId(), sessionId));
  }

  /** {@inheritDoc} */
  @Override
  @PatchMapping("/api/v1/sessions/{sessionId}/end")
  public ApiResponse<Void> endSession(
      @AuthenticationPrincipal AuthUserPrincipal principal, @PathVariable Long sessionId) {
    learningSessionService.endSession(principal.userId(), sessionId);
    return ApiResponse.success(null);
  }
}
