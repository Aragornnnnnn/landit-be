// 관리자 시나리오 테스트 세션 시작 요청을 처리한다.

package com.landit.landitbe.feature.session.scenario;

import com.landit.landitbe.feature.session.scenario.docs.AdminScenarioSessionControllerDocs;
import com.landit.landitbe.feature.session.scenario.dto.SessionStartResponse;
import com.landit.landitbe.feature.session.scenario.service.AdminScenarioSessionStartService;
import com.landit.landitbe.shared.response.ApiResponse;
import com.landit.landitbe.shared.security.AuthUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 시나리오 테스트 세션 시작 요청을 처리한다. */
@Profile("develop")
@RequiredArgsConstructor
@RestController
public class AdminScenarioSessionController implements AdminScenarioSessionControllerDocs {

  private final AdminScenarioSessionStartService adminScenarioSessionStartService;

  /** {@inheritDoc} */
  @Override
  @PostMapping("/api/v1/admin/scenarios/{scenarioId}/sessions")
  public ResponseEntity<ApiResponse<SessionStartResponse>> start(
      @AuthenticationPrincipal AuthUserPrincipal principal, @PathVariable Long scenarioId) {
    return ApiResponse.success(
        HttpStatus.CREATED,
        adminScenarioSessionStartService.startScenarioSession(principal.userId(), scenarioId));
  }
}
