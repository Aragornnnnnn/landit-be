// 앱 버전 확인 요청을 받아 플랫폼별 업데이트 정책을 반환한다.

package com.landit.landitbe.feature.app;

import com.landit.landitbe.feature.app.docs.AppVersionControllerDocs;
import com.landit.landitbe.feature.app.dto.AppVersionCheckResponse;
import com.landit.landitbe.feature.app.service.AppVersionQueryService;
import com.landit.landitbe.shared.domain.AppPlatform;
import com.landit.landitbe.shared.response.ApiResponse;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 앱 버전 확인 요청을 받아 플랫폼별 업데이트 정책을 반환한다. */
@RequiredArgsConstructor
@RestController
@Validated
public class AppVersionController implements AppVersionControllerDocs {

  private final AppVersionQueryService appVersionQueryService;

  /** {@inheritDoc} */
  @Override
  @GetMapping("/api/v1/app-versions/check")
  public ApiResponse<AppVersionCheckResponse> check(
      @RequestParam AppPlatform platform, @RequestParam @Min(1) long buildNumber) {
    return ApiResponse.success(appVersionQueryService.check(platform, buildNumber));
  }
}
