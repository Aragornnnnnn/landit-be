// 시나리오에서 학습 세션을 시작하는 API 요청을 처리한다.

package com.landit.landitbe.session.api;

import com.landit.landitbe.auth.security.AuthUserPrincipal;
import com.landit.landitbe.common.response.ApiResponse;
import com.landit.landitbe.session.api.docs.ScenarioSessionControllerDocs;
import com.landit.landitbe.session.api.dto.SessionStartResponse;
import com.landit.landitbe.session.application.ScenarioSessionStartUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 시나리오에서 학습 세션을 시작하는 API 요청을 처리한다. */
@RequestMapping("/api/v1/scenarios")
@RequiredArgsConstructor
@RestController
public class ScenarioSessionController implements ScenarioSessionControllerDocs {

  private final ScenarioSessionStartUseCase scenarioSessionStartUseCase;

  /** 선택한 시나리오로 학습 세션을 시작한다. */
  @Override
  @PostMapping("/{scenarioId}/sessions")
  public ResponseEntity<ApiResponse<SessionStartResponse>> startScenarioSession(
      @AuthenticationPrincipal AuthUserPrincipal principal, @PathVariable Long scenarioId) {
    return ApiResponse.success(
        HttpStatus.CREATED,
        scenarioSessionStartUseCase.startScenarioSession(principal.userId(), scenarioId));
  }
}
