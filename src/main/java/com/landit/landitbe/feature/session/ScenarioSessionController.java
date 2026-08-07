// 시나리오에서 학습 세션을 시작하는 API 요청을 처리한다.

package com.landit.landitbe.feature.session;

import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.feature.session.docs.ScenarioSessionControllerDocs;
import com.landit.landitbe.feature.session.dto.SessionStartResponse;
import com.landit.landitbe.feature.session.service.ScenarioSessionStartService;
import com.landit.landitbe.shared.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** 시나리오에서 학습 세션을 시작하는 API 요청을 처리한다. */
@RequiredArgsConstructor
@RestController
public class ScenarioSessionController implements ScenarioSessionControllerDocs {

  private final ScenarioSessionStartService scenarioSessionStartService;

  /** {@inheritDoc} */
  @Override
  @PostMapping("/api/v1/scenarios/{scenarioId}/sessions")
  public ResponseEntity<ApiResponse<SessionStartResponse>> startScenarioSession(
      @AuthenticationPrincipal AuthUserPrincipal principal, @PathVariable Long scenarioId) {
    return ApiResponse.success(
        HttpStatus.CREATED,
        scenarioSessionStartService.startScenarioSession(principal.userId(), scenarioId));
  }
}
