// 관리자 시나리오 테스트 목록 요청을 처리한다.

package com.landit.landitbe.feature.content;

import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.feature.content.docs.AdminScenarioControllerDocs;
import com.landit.landitbe.feature.content.dto.AdminScenarioListResponse;
import com.landit.landitbe.feature.content.service.AdminScenarioQueryService;
import com.landit.landitbe.shared.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 시나리오 테스트 목록 요청을 처리한다. */
@Profile("develop")
@RequiredArgsConstructor
@RestController
public class AdminScenarioController implements AdminScenarioControllerDocs {

  private final AdminScenarioQueryService adminScenarioQueryService;

  /** {@inheritDoc} */
  @Override
  @GetMapping("/api/v1/admin/scenarios")
  public ApiResponse<AdminScenarioListResponse> list(
      @AuthenticationPrincipal AuthUserPrincipal principal) {
    return ApiResponse.success(adminScenarioQueryService.getAdminScenarioList(principal.userId()));
  }
}
