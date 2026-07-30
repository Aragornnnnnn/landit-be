// 시나리오 목록 조회 API 요청을 받아 공통 응답으로 반환한다.

package com.landit.landitbe.feature.content;

import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.feature.content.docs.ScenarioControllerDocs;
import com.landit.landitbe.feature.content.dto.DailyScenarioResponse;
import com.landit.landitbe.feature.content.service.DailyScenarioQueryService;
import com.landit.landitbe.shared.response.ApiResponse;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 날짜별 시나리오 조회 API 요청을 받아 공통 응답으로 반환한다. */
@RequiredArgsConstructor
@RestController
public class ScenarioController implements ScenarioControllerDocs {

  private final DailyScenarioQueryService dailyScenarioQueryService;

  /** 인증된 사용자의 오늘 배정 시나리오 또는 과거 최초 완료 이력을 조회한다. */
  @Override
  @GetMapping("/api/v1/scenarios/daily")
  public ApiResponse<DailyScenarioResponse> getDailyScenario(
      @AuthenticationPrincipal AuthUserPrincipal principal,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    return ApiResponse.success(
        dailyScenarioQueryService.getDailyScenario(principal.userId(), date));
  }
}
