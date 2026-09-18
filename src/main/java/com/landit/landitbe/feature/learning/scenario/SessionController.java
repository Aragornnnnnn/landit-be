// 시나리오 메시지·속마음·피드백·수준 평가 API 요청을 연결한다.

package com.landit.landitbe.feature.learning.scenario;

import com.landit.landitbe.feature.learning.scenario.assessment.dto.SessionLevelAssessmentResponse;
import com.landit.landitbe.feature.learning.scenario.assessment.service.SessionLevelAssessmentGenerationService;
import com.landit.landitbe.feature.learning.scenario.docs.SessionControllerDocs;
import com.landit.landitbe.feature.learning.scenario.feedback.dto.SessionFeedbackResponse;
import com.landit.landitbe.feature.learning.scenario.feedback.service.SessionFeedbackService;
import com.landit.landitbe.feature.learning.scenario.session.innerthought.dto.SessionInnerThoughtResponse;
import com.landit.landitbe.feature.learning.scenario.session.innerthought.service.SessionInnerThoughtQueryService;
import com.landit.landitbe.feature.learning.scenario.session.message.dto.SessionMessageSubmitRequest;
import com.landit.landitbe.feature.learning.scenario.session.message.dto.SessionMessageSubmitResponse;
import com.landit.landitbe.feature.learning.scenario.session.message.service.SessionMessageSubmitService;
import com.landit.landitbe.shared.response.ApiResponse;
import com.landit.landitbe.shared.security.AuthUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 시나리오 메시지·속마음·피드백·수준 평가 API 요청을 연결한다. */
@RequiredArgsConstructor
@RestController
public class SessionController implements SessionControllerDocs {

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
}
