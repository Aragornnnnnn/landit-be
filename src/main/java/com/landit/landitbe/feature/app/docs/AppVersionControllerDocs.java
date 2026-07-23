// 앱 버전 정책 API의 OpenAPI 문서를 정의한다.

package com.landit.landitbe.feature.app.docs;

import com.landit.landitbe.feature.app.dto.AppVersionCheckResponse;
import com.landit.landitbe.shared.domain.AppPlatform;
import com.landit.landitbe.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;

/** 앱 버전 정책 API의 OpenAPI 문서를 정의한다. */
@Tag(name = "App Version", description = "앱 버전 정책 API")
public interface AppVersionControllerDocs {

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
  ApiResponse<AppVersionCheckResponse> check(
      @Parameter(description = "앱 플랫폼", example = "IOS", required = true) AppPlatform platform,
      @Parameter(description = "현재 앱 빌드 번호", example = "18", required = true) @Min(1)
          long buildNumber);
}
