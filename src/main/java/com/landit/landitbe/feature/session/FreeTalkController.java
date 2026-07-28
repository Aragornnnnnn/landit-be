// 프리톡 주제 조회와 세션 시작 HTTP 요청을 처리한다.

package com.landit.landitbe.feature.session;

import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.feature.session.docs.FreeTalkControllerDocs;
import com.landit.landitbe.feature.session.dto.FreeTalkSessionStartRequest;
import com.landit.landitbe.feature.session.dto.FreeTalkSessionStartResponse;
import com.landit.landitbe.feature.session.dto.FreeTalkTopicResponse;
import com.landit.landitbe.feature.session.service.FreeTalkSessionStartService;
import com.landit.landitbe.feature.session.service.FreeTalkTopicService;
import com.landit.landitbe.shared.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 프리톡 주제 조회와 세션 시작 HTTP 요청을 처리한다. */
@RequiredArgsConstructor
@RestController
public class FreeTalkController implements FreeTalkControllerDocs {

  private final FreeTalkTopicService freeTalkTopicService;
  private final FreeTalkSessionStartService freeTalkSessionStartService;

  /** {@inheritDoc} */
  @Override
  @GetMapping("/api/v1/free-talk/topics")
  public ResponseEntity<ApiResponse<List<FreeTalkTopicResponse>>> getTopics(
      @AuthenticationPrincipal AuthUserPrincipal principal) {
    return ResponseEntity.ok(ApiResponse.success(freeTalkTopicService.getActiveTopics()));
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
}
