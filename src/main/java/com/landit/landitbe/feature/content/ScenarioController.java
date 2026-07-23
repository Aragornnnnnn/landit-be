// 시나리오 목록 조회 API 요청을 받아 공통 응답으로 반환한다.

package com.landit.landitbe.feature.content;

import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.feature.content.docs.ScenarioControllerDocs;
import com.landit.landitbe.feature.content.dto.ScenarioListResponse;
import com.landit.landitbe.feature.content.service.ScenarioQueryService;
import com.landit.landitbe.shared.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 시나리오 목록 조회 API 요청을 받아 공통 응답으로 반환한다. */
@RequestMapping("/api/v1/scenarios")
@RequiredArgsConstructor
@RestController
public class ScenarioController implements ScenarioControllerDocs {

  private final ScenarioQueryService scenarioQueryService;

  /** 인증된 사용자의 카테고리별 시나리오 목록을 조회한다. */
  @Override
  @GetMapping
  public ApiResponse<ScenarioListResponse> listScenarios(
      @AuthenticationPrincipal AuthUserPrincipal principal) {
    return ApiResponse.success(scenarioQueryService.getScenarioList(principal.userId()));
  }
}
