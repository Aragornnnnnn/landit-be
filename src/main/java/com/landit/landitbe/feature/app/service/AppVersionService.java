// 사용자의 앱 버전에 따른 업데이트 정책을 조회한다.

package com.landit.landitbe.feature.app.service;

import com.landit.landitbe.feature.app.domain.AppVersion;
import com.landit.landitbe.feature.app.domain.AppVersionName;
import com.landit.landitbe.feature.app.dto.AppVersionCheckResponse;
import com.landit.landitbe.feature.app.dto.AppVersionCheckResponse.UpdateType;
import com.landit.landitbe.feature.app.exception.AppErrorCode;
import com.landit.landitbe.feature.app.repository.AppVersionRepository;
import com.landit.landitbe.shared.domain.AppPlatform;
import com.landit.landitbe.shared.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 사용자의 앱 버전에 따른 업데이트 정책을 조회한다. */
@Service
public class AppVersionService {

  private final AppVersionRepository appVersionRepository;

  /**
   * 앱 버전 정책 저장소를 주입받는다.
   *
   * @param appVersionRepository 앱 버전 정책 Repository
   */
  public AppVersionService(AppVersionRepository appVersionRepository) {
    this.appVersionRepository = appVersionRepository;
  }

  /**
   * 플랫폼별 단일 정책을 기준으로 앱 업데이트 필요 수준을 반환한다.
   *
   * @param platform 앱 플랫폼
   * @param currentVersionName 현재 앱 버전명
   * @return 앱 업데이트 필요 수준과 플랫폼 정책 정보
   * @throws ApiException 앱 버전 정책이 설정되지 않았을 때
   */
  @Transactional(readOnly = true)
  public AppVersionCheckResponse check(AppPlatform platform, String currentVersionName) {
    AppVersion policy =
        appVersionRepository
            .findByPlatform(platform)
            .orElseThrow(() -> new ApiException(AppErrorCode.APP_VERSION_POLICY_NOT_CONFIGURED));
    UpdateType updateType = updateType(currentVersionName, policy);
    return AppVersionCheckResponse.from(policy, updateType, reason(updateType, policy));
  }

  /** 현재 앱 버전과 플랫폼 정책을 비교해 업데이트 수준을 계산한다. */
  private UpdateType updateType(String currentVersionName, AppVersion policy) {
    AppVersionName currentVersion = AppVersionName.parse(currentVersionName);
    if (currentVersion.compareTo(AppVersionName.parse(policy.getMinimumSupportedVersionName()))
        < 0) {
      return UpdateType.FORCE;
    }
    if (currentVersion.compareTo(AppVersionName.parse(policy.getVersionName())) < 0) {
      return UpdateType.SOFT;
    }
    return UpdateType.NONE;
  }

  /** 업데이트 수준별 안내 문구를 선택한다. */
  private String reason(UpdateType updateType, AppVersion policy) {
    return switch (updateType) {
      case FORCE -> policy.getForceUpdateReason();
      case SOFT -> policy.getSoftUpdateReason();
      case NONE -> null;
    };
  }
}
