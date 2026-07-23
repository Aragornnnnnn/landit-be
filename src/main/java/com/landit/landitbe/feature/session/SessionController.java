// 기존 학습 세션을 조작하는 API 요청을 처리한다.

package com.landit.landitbe.feature.session;

import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.feature.session.docs.SessionControllerDocs;
import com.landit.landitbe.feature.session.dto.SessionFeedbackResponse;
import com.landit.landitbe.feature.session.dto.SessionInnerThoughtResponse;
import com.landit.landitbe.feature.session.dto.SessionMessageSubmitRequest;
import com.landit.landitbe.feature.session.dto.SessionMessageSubmitResponse;
import com.landit.landitbe.feature.session.service.SessionEndUseCase;
import com.landit.landitbe.feature.session.service.SessionFeedbackUseCase;
import com.landit.landitbe.feature.session.service.SessionInnerThoughtQueryService;
import com.landit.landitbe.feature.session.service.SessionMessageSubmitUseCase;
import com.landit.landitbe.shared.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 기존 학습 세션을 조작하는 API 요청을 처리한다. */
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
@RestController
public class SessionController implements SessionControllerDocs {

  private final SessionEndUseCase sessionEndUseCase;
  private final SessionFeedbackUseCase sessionFeedbackUseCase;
  private final SessionMessageSubmitUseCase sessionMessageSubmitUseCase;
  private final SessionInnerThoughtQueryService sessionInnerThoughtQueryService;

  /** 사용자 발화를 저장하고 다음 AI 메시지를 생성한다. */
  @Override
  @PostMapping("/{sessionId}/messages")
  public ResponseEntity<ApiResponse<SessionMessageSubmitResponse>> submitMessage(
      @AuthenticationPrincipal AuthUserPrincipal principal,
      @PathVariable Long sessionId,
      @RequestBody SessionMessageSubmitRequest request) {
    return ApiResponse.success(
        HttpStatus.OK,
        sessionMessageSubmitUseCase.submitMessage(principal.userId(), sessionId, request));
  }

  /** 사용자 메시지의 상대 역할 속마음 처리 상태를 조회한다. */
  @Override
  @GetMapping("/{sessionId}/messages/{messageId}/inner-thought")
  public ResponseEntity<ApiResponse<SessionInnerThoughtResponse>> getInnerThought(
      @AuthenticationPrincipal AuthUserPrincipal principal,
      @PathVariable Long sessionId,
      @PathVariable Long messageId) {
    return ApiResponse.success(
        HttpStatus.OK,
        sessionInnerThoughtQueryService.get(principal.userId(), sessionId, messageId));
  }

  /** 완료된 세션의 최종 피드백을 생성하거나 저장된 결과를 조회한다. */
  @Override
  @PostMapping("/{sessionId}/feedback")
  public ResponseEntity<ApiResponse<SessionFeedbackResponse>> getOrCreateFeedback(
      @AuthenticationPrincipal AuthUserPrincipal principal, @PathVariable Long sessionId) {
    return ApiResponse.success(
        HttpStatus.OK, sessionFeedbackUseCase.getOrCreate(principal.userId(), sessionId));
  }

  /** 진행 중인 학습 세션을 사용자가 중도 종료한다. */
  @Override
  @PatchMapping("/{sessionId}/end")
  public ApiResponse<Void> endSession(
      @AuthenticationPrincipal AuthUserPrincipal principal, @PathVariable Long sessionId) {
    sessionEndUseCase.endSession(principal.userId(), sessionId);
    return ApiResponse.success(null);
  }
}
