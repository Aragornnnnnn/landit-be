// 앱 버전 확인 요청을 받아 플랫폼별 업데이트 정책을 반환한다.

package com.landit.landitbe.feature.app;

import com.landit.landitbe.feature.app.dto.AppVersionCheckResponse;
import com.landit.landitbe.feature.app.service.AppVersionQueryService;
import com.landit.landitbe.shared.domain.AppPlatform;
import com.landit.landitbe.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 앱 버전 확인 요청을 받아 플랫폼별 업데이트 정책을 반환한다. */
@RequestMapping("/api/v1/app-versions")
@RequiredArgsConstructor
@RestController
@Validated
@Tag(name = "App Version", description = "앱 버전 정책 API")
public class AppVersionController {

  private final AppVersionQueryService appVersionQueryService;

  /** 현재 앱 빌드의 업데이트 필요 수준을 확인한다. */
  @Operation(
      summary = "앱 버전 업데이트 확인",
      description = "플랫폼과 현재 빌드 번호를 활성 버전 정책과 비교해 업데이트 필요 수준을 반환한다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "업데이트 정책 조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "요청값 검증 실패"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "500",
        description = "활성 앱 버전 정책 미설정 또는 서버 오류")
  })
  @GetMapping("/check")
  public ApiResponse<AppVersionCheckResponse> check(
      @Parameter(description = "앱 플랫폼", example = "IOS", required = true) @RequestParam
          AppPlatform platform,
      @Parameter(description = "현재 앱 빌드 번호", example = "18", required = true) @RequestParam @Min(1)
          long buildNumber) {
    return ApiResponse.success(appVersionQueryService.check(platform, buildNumber));
  }
}
