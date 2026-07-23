// 활성 앱 버전 정책을 조회하고 클라이언트의 업데이트 필요 수준을 계산한다.

package com.landit.landitbe.feature.app.service;

import com.landit.landitbe.feature.app.domain.AppVersion;
import com.landit.landitbe.feature.app.dto.AppVersionCheckResponse;
import com.landit.landitbe.feature.app.dto.AppVersionCheckResponse.UpdateType;
import com.landit.landitbe.feature.app.repository.AppVersionRepository;
import com.landit.landitbe.shared.domain.AppPlatform;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
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
    return AppVersionCheckResponse.from(policy, updateType, reason(updateType, policy));
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
