// 로그인 사용자의 시나리오 전체 완료 회차 조회 요청을 처리한다.

package com.landit.landitbe.feature.learning.scenario.history;

import com.landit.landitbe.feature.learning.scenario.history.docs.ScenarioHistoryControllerDocs;
import com.landit.landitbe.feature.learning.scenario.history.dto.ScenarioHistoryResponse;
import com.landit.landitbe.feature.learning.scenario.history.service.ScenarioHistoryQueryService;
import com.landit.landitbe.shared.response.ApiResponse;
import com.landit.landitbe.shared.security.AuthUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** 시나리오 히스토리 조회 API를 제공한다. */
@RestController
@RequiredArgsConstructor
public class ScenarioHistoryController implements ScenarioHistoryControllerDocs {
  private final ScenarioHistoryQueryService historyService;

  /** {@inheritDoc} */
  @Override
  @GetMapping("/api/v1/scenarios/{scenarioId}/history")
  public ApiResponse<ScenarioHistoryResponse> getHistory(
      @AuthenticationPrincipal AuthUserPrincipal principal, @PathVariable Long scenarioId) {
    return ApiResponse.success(historyService.findHistory(principal.userId(), scenarioId));
  }
}
