// 프리톡 주제 조회와 세션 시작 HTTP 요청을 처리한다.

package com.landit.landitbe.feature.learning.freetalk;

import com.landit.landitbe.feature.learning.freetalk.docs.FreeTalkControllerDocs;
import com.landit.landitbe.feature.learning.freetalk.expression.dto.FreeTalkExpressionRetryResponse;
import com.landit.landitbe.feature.learning.freetalk.expression.service.FreeTalkExpressionGenerationDispatcher;
import com.landit.landitbe.feature.learning.freetalk.expression.service.FreeTalkExpressionRetryService;
import com.landit.landitbe.feature.learning.freetalk.history.dto.FreeTalkSessionDetailResponse;
import com.landit.landitbe.feature.learning.freetalk.history.dto.FreeTalkSessionListResponse;
import com.landit.landitbe.feature.learning.freetalk.history.service.FreeTalkHistoryQueryService;
import com.landit.landitbe.feature.learning.freetalk.message.dto.FreeTalkExitDecisionRequest;
import com.landit.landitbe.feature.learning.freetalk.message.dto.FreeTalkMessageSubmitRequest;
import com.landit.landitbe.feature.learning.freetalk.message.dto.FreeTalkMessageSubmitResponse;
import com.landit.landitbe.feature.learning.freetalk.message.service.FreeTalkMessageService;
import com.landit.landitbe.feature.learning.freetalk.start.dto.FreeTalkSessionStartRequest;
import com.landit.landitbe.feature.learning.freetalk.start.dto.FreeTalkSessionStartResponse;
import com.landit.landitbe.feature.learning.freetalk.start.service.FreeTalkSessionStartService;
import com.landit.landitbe.feature.learning.freetalk.topic.dto.FreeTalkMainResponse;
import com.landit.landitbe.feature.learning.freetalk.topic.service.FreeTalkTopicService;
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

/** 프리톡 주제 조회와 세션 시작 HTTP 요청을 처리한다. */
@RequiredArgsConstructor
@RestController
public class FreeTalkController implements FreeTalkControllerDocs {

  private final FreeTalkTopicService freeTalkTopicService;
  private final FreeTalkSessionStartService freeTalkSessionStartService;
  private final FreeTalkMessageService freeTalkMessageService;
  private final FreeTalkHistoryQueryService freeTalkHistoryQueryService;
  private final FreeTalkExpressionRetryService freeTalkExpressionRetryService;
  private final FreeTalkExpressionGenerationDispatcher expressionGenerationDispatcher;

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

  /** {@inheritDoc} */
  @Override
  @GetMapping("/api/v1/free-talk/sessions")
  public ResponseEntity<ApiResponse<FreeTalkSessionListResponse>> getSessions(
      @AuthenticationPrincipal AuthUserPrincipal principal,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    if (page < 0 || size < 1 || size > 50) {
      throw new com.landit.landitbe.shared.exception.ApiException(
          com.landit.landitbe.shared.exception.ErrorCode.INVALID_REQUEST);
    }
    return ResponseEntity.ok(
        ApiResponse.success(
            freeTalkHistoryQueryService.getSessions(principal.userId(), page, size)));
  }

  /** {@inheritDoc} */
  @Override
  @GetMapping("/api/v1/free-talk/sessions/{sessionId}")
  public ResponseEntity<ApiResponse<FreeTalkSessionDetailResponse>> getSession(
      @AuthenticationPrincipal AuthUserPrincipal principal, @PathVariable long sessionId) {
    return ResponseEntity.ok(
        ApiResponse.success(freeTalkHistoryQueryService.getSession(principal.userId(), sessionId)));
  }

  /** {@inheritDoc} */
  @Override
  @PostMapping("/api/v1/free-talk/sessions/{sessionId}/expressions/retry")
  public ResponseEntity<ApiResponse<FreeTalkExpressionRetryResponse>> retryExpressions(
      @AuthenticationPrincipal AuthUserPrincipal principal, @PathVariable long sessionId) {
    FreeTalkExpressionRetryResponse response =
        freeTalkExpressionRetryService.retry(principal.userId(), sessionId);
    expressionGenerationDispatcher.dispatch(sessionId);
    return ApiResponse.success(HttpStatus.ACCEPTED, response);
  }
}
