// 프리톡 주제 조회와 세션 시작 HTTP 요청을 처리한다.

package com.landit.landitbe.feature.session;

import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.feature.session.docs.FreeTalkControllerDocs;
import com.landit.landitbe.feature.session.dto.FreeTalkExitDecisionRequest;
import com.landit.landitbe.feature.session.dto.FreeTalkMainResponse;
import com.landit.landitbe.feature.session.dto.FreeTalkMessageSubmitRequest;
import com.landit.landitbe.feature.session.dto.FreeTalkMessageSubmitResponse;
import com.landit.landitbe.feature.session.dto.FreeTalkSessionStartRequest;
import com.landit.landitbe.feature.session.dto.FreeTalkSessionStartResponse;
import com.landit.landitbe.feature.session.service.FreeTalkMessageService;
import com.landit.landitbe.feature.session.service.FreeTalkSessionStartService;
import com.landit.landitbe.feature.session.service.FreeTalkTopicService;
import com.landit.landitbe.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 프리톡 주제 조회와 세션 시작 HTTP 요청을 처리한다. */
@RequiredArgsConstructor
@RestController
public class FreeTalkController implements FreeTalkControllerDocs {

  private final FreeTalkTopicService freeTalkTopicService;
  private final FreeTalkSessionStartService freeTalkSessionStartService;
  private final FreeTalkMessageService freeTalkMessageService;

  /** {@inheritDoc} */
  @Override
  @GetMapping("/api/v1/free-talk/topics")
  public ResponseEntity<ApiResponse<FreeTalkMainResponse>> getTopics(
      @AuthenticationPrincipal AuthUserPrincipal principal) {
    return ResponseEntity.ok(ApiResponse.success(freeTalkTopicService.getMain(principal.userId())));
  }

  /** {@inheritDoc} */
  @Override
  @PostMapping("/api/v1/free-talk/sessions")
  public ResponseEntity<ApiResponse<FreeTalkSessionStartResponse>> startSession(
      @AuthenticationPrincipal AuthUserPrincipal principal,
      @Valid @RequestBody FreeTalkSessionStartRequest request) {
    return ApiResponse.success(
        HttpStatus.CREATED,
        freeTalkSessionStartService.startFreeTalkSession(principal.userId(), request));
  }

  /** {@inheritDoc} */
  @Override
  @PostMapping("/api/v1/free-talk/sessions/{sessionId}/messages")
  public ResponseEntity<ApiResponse<FreeTalkMessageSubmitResponse>> submitMessage(
      @AuthenticationPrincipal AuthUserPrincipal principal,
      @PathVariable long sessionId,
      @Valid @RequestBody FreeTalkMessageSubmitRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(freeTalkMessageService.submit(principal.userId(), sessionId, request)));
  }

  /** {@inheritDoc} */
  @Override
  @PostMapping("/api/v1/free-talk/sessions/{sessionId}/exit-decision")
  public ResponseEntity<ApiResponse<FreeTalkMessageSubmitResponse>> decideExit(
      @AuthenticationPrincipal AuthUserPrincipal principal,
      @PathVariable long sessionId,
      @Valid @RequestBody FreeTalkExitDecisionRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            freeTalkMessageService.decideExit(principal.userId(), sessionId, request)));
  }
}
