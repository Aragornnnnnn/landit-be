// 활성 앱 버전 정책을 조회하고 클라이언트의 업데이트 필요 수준을 계산한다.

package com.landit.landitbe.app.application;

import com.landit.landitbe.app.api.dto.AppVersionCheckResponse;
import com.landit.landitbe.app.api.dto.AppVersionCheckResponse.UpdateType;
import com.landit.landitbe.app.domain.AppVersion;
import com.landit.landitbe.app.infrastructure.AppVersionRepository;
import com.landit.landitbe.common.domain.AppPlatform;
import com.landit.landitbe.common.exception.ApiException;
import com.landit.landitbe.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 활성 앱 버전 정책을 조회하고 클라이언트의 업데이트 필요 수준을 계산한다. */
@RequiredArgsConstructor
@Service
public class AppVersionQueryService {

  private final AppVersionRepository appVersionRepository;

  /** 플랫폼별 활성 정책을 기준으로 앱 업데이트 필요 수준을 반환한다. */
  @Transactional(readOnly = true)
  public AppVersionCheckResponse check(AppPlatform platform, long currentBuildNumber) {
    AppVersion policy =
        appVersionRepository
            .findByPlatformAndActiveTrue(platform)
            .orElseThrow(() -> new ApiException(ErrorCode.APP_VERSION_POLICY_NOT_CONFIGURED));
    UpdateType updateType = updateType(currentBuildNumber, policy);
    return new AppVersionCheckResponse(
        updateType,
        policy.getVersionName(),
        policy.getBuildNumber(),
        policy.getMinimumSupportedBuildNumber(),
        reason(updateType, policy),
        policy.getReleasedAt());
  }

  private UpdateType updateType(long currentBuildNumber, AppVersion policy) {
    if (currentBuildNumber < policy.getMinimumSupportedBuildNumber()) {
      return UpdateType.FORCE;
    }
    if (currentBuildNumber < policy.getBuildNumber()) {
      return UpdateType.SOFT;
    }
    return UpdateType.NONE;
  }

  private String reason(UpdateType updateType, AppVersion policy) {
    return switch (updateType) {
      case FORCE -> policy.getForceUpdateReason();
      case SOFT -> policy.getSoftUpdateReason();
      case NONE -> null;
    };
  }
}
