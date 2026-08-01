// 공개 업데이트 확인과 관리자 앱 버전 정책 관리를 담당한다.

package com.landit.landitbe.feature.app.service;

import com.landit.landitbe.feature.admin.domain.AdminAction;
import com.landit.landitbe.feature.admin.service.AdminAuditService;
import com.landit.landitbe.feature.app.domain.AppVersion;
import com.landit.landitbe.feature.app.domain.AppVersionName;
import com.landit.landitbe.feature.app.dto.AdminAppVersionResponse;
import com.landit.landitbe.feature.app.dto.AdminAppVersionUpdateRequest;
import com.landit.landitbe.feature.app.dto.AppVersionCheckResponse;
import com.landit.landitbe.feature.app.dto.AppVersionCheckResponse.UpdateType;
import com.landit.landitbe.feature.app.repository.AppVersionRepository;
import com.landit.landitbe.shared.domain.AppPlatform;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 공개 업데이트 확인과 관리자 앱 버전 정책 관리를 담당한다. */
@Service
public class AppVersionService {

  private final AppVersionRepository appVersionRepository;
  private final AdminAuditService adminAuditService;

  /**
   * 앱 버전 Repository와 관리자 감사 Service를 주입받는다.
   *
   * @param appVersionRepository 앱 버전 정책 Repository
   * @param adminAuditService 관리자 감사 기록 Service
   */
  public AppVersionService(
      AppVersionRepository appVersionRepository, AdminAuditService adminAuditService) {
    this.appVersionRepository = appVersionRepository;
    this.adminAuditService = adminAuditService;
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
            .orElseThrow(() -> new ApiException(ErrorCode.APP_VERSION_POLICY_NOT_CONFIGURED));
    UpdateType updateType = updateType(currentVersionName, policy);
    return AppVersionCheckResponse.from(policy, updateType, reason(updateType, policy));
  }

  /**
   * 관리자 화면에 표시할 전체 앱 버전 정책 목록을 반환한다.
   *
   * @return 플랫폼 순으로 정렬된 앱 버전 정책 목록
   */
  @Transactional(readOnly = true)
  public List<AdminAppVersionResponse> list() {
    return appVersionRepository.findAllByOrderByPlatformAsc().stream()
        .map(AdminAppVersionResponse::from)
        .toList();
  }

  /**
   * 관리자 입력으로 플랫폼의 단일 앱 버전 정책을 수정한다.
   *
   * @param adminUserProfileId 작업을 수행한 관리자 사용자 프로필 ID
   * @param platform 수정할 앱 플랫폼
   * @param request 앱 버전 정책 수정 요청
   * @return 수정된 앱 버전 정책
   * @throws ApiException 정책이 없거나 최소 지원 버전이 최신 버전보다 높을 때
   */
  @Transactional
  public AdminAppVersionResponse update(
      Long adminUserProfileId, AppPlatform platform, AdminAppVersionUpdateRequest request) {
    validateVersionRange(request.versionName(), request.minimumSupportedVersionName());

    AppVersion appVersion = requireForUpdate(platform);
    String beforeValue = auditValue(appVersion);

    appVersion.update(
        request.versionName(),
        request.buildNumber(),
        request.minimumSupportedVersionName(),
        request.forceUpdateReason(),
        request.softUpdateReason(),
        request.releaseNote(),
        request.releasedAt());

    adminAuditService.record(
        adminUserProfileId,
        AdminAction.APP_VERSION_UPDATED,
        "APP_VERSION",
        platform.name(),
        beforeValue,
        auditValue(appVersion));

    return AdminAppVersionResponse.from(appVersion);
  }

  /** 플랫폼의 단일 앱 버전 정책을 비관적 잠금으로 조회한다. */
  private AppVersion requireForUpdate(AppPlatform platform) {
    return appVersionRepository
        .findByPlatformForUpdate(platform)
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "앱 버전 정책을 찾을 수 없습니다."));
  }

  /** 최소 지원 버전이 최신 버전보다 높지 않은지 검증한다. */
  private void validateVersionRange(String versionName, String minimumSupportedVersionName) {
    if (AppVersionName.parse(minimumSupportedVersionName)
            .compareTo(AppVersionName.parse(versionName))
        > 0) {
      throw new ApiException(ErrorCode.INVALID_REQUEST, "최소 지원 버전은 최신 버전보다 높을 수 없습니다.");
    }
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

  /** 감사 기록에 저장할 앱 버전 정책 스냅샷을 만든다. */
  private String auditValue(AppVersion appVersion) {
    return "platform="
        + appVersion.getPlatform()
        + ",versionName="
        + appVersion.getVersionName()
        + ",buildNumber="
        + appVersion.getBuildNumber()
        + ",minimumSupportedVersionName="
        + appVersion.getMinimumSupportedVersionName()
        + ",forceUpdateReason="
        + appVersion.getForceUpdateReason()
        + ",softUpdateReason="
        + appVersion.getSoftUpdateReason()
        + ",releaseNote="
        + appVersion.getReleaseNote()
        + ",active="
        + appVersion.isActive()
        + ",releasedAt="
        + appVersion.getReleasedAt();
  }
}
