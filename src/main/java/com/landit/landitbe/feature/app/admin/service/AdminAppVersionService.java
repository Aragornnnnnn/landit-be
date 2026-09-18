// 관리자 앱 버전 정책 조회·수정과 감사 기록을 담당한다.

package com.landit.landitbe.feature.app.admin.service;

import com.landit.landitbe.feature.app.admin.dto.AdminAppVersionResponse;
import com.landit.landitbe.feature.app.admin.dto.AdminAppVersionUpdateRequest;
import com.landit.landitbe.feature.app.domain.AppVersion;
import com.landit.landitbe.feature.app.domain.AppVersionName;
import com.landit.landitbe.feature.app.repository.AppVersionRepository;
import com.landit.landitbe.feature.audit.domain.AdminAction;
import com.landit.landitbe.feature.audit.service.AdminAuditService;
import com.landit.landitbe.feature.profile.dto.UserProfileNickname;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import com.landit.landitbe.shared.domain.AppPlatform;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관리자 앱 버전 정책 조회·수정과 감사 기록을 담당한다. */
@Service
public class AdminAppVersionService {

  private final AppVersionRepository appVersionRepository;
  private final AdminAuditService adminAuditService;
  private final UserProfileService userProfileService;

  /**
   * 앱 버전 Repository와 관리자 감사·프로필 Service를 주입받는다.
   *
   * @param appVersionRepository 앱 버전 정책 Repository
   * @param adminAuditService 관리자 감사 기록 Service
   * @param userProfileService 사용자 프로필 Service
   */
  public AdminAppVersionService(
      AppVersionRepository appVersionRepository,
      AdminAuditService adminAuditService,
      UserProfileService userProfileService) {
    this.appVersionRepository = appVersionRepository;
    this.adminAuditService = adminAuditService;
    this.userProfileService = userProfileService;
  }

  /**
   * 관리자 화면에 표시할 전체 앱 버전 정책 목록을 반환한다.
   *
   * @return 플랫폼 순으로 정렬된 앱 버전 정책 목록
   */
  @Transactional(readOnly = true)
  public List<AdminAppVersionResponse> list() {
    return appVersionRepository.findAllByOrderByPlatformAsc().stream()
        .map(this::toAdminResponse)
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
        request.releasedAt(),
        adminUserProfileId);

    adminAuditService.record(
        adminUserProfileId,
        AdminAction.APP_VERSION_UPDATED,
        "APP_VERSION",
        platform.name(),
        beforeValue,
        auditValue(appVersion));

    return toAdminResponse(appVersion);
  }

  /** 앱 버전 정책과 마지막 수정자 닉네임을 관리자 응답으로 변환한다. */
  private AdminAppVersionResponse toAdminResponse(AppVersion appVersion) {
    String updatedBy =
        userProfileService
            .findNickname(appVersion.getUpdatedByUserProfileId())
            .map(UserProfileNickname::nickname)
            .orElse(null);
    return AdminAppVersionResponse.from(appVersion, updatedBy);
  }

  /** 플랫폼의 단일 앱 버전 정책을 비관적 잠금으로 조회한다. */
  private AppVersion requireForUpdate(AppPlatform platform) {
    return appVersionRepository
        .findByPlatformForUpdate(platform)
        .orElseThrow(
            () -> {
              String message = "앱 버전 정책을 찾을 수 없습니다.";
              return new ApiException(ErrorCode.RESOURCE_NOT_FOUND, message);
            });
  }

  /** 최소 지원 버전이 최신 버전보다 높지 않은지 검증한다. */
  private void validateVersionRange(String versionName, String minimumSupportedVersionName) {
    if (AppVersionName.parse(minimumSupportedVersionName)
            .compareTo(AppVersionName.parse(versionName))
        > 0) {
      String message = "최소 지원 버전은 최신 버전보다 높을 수 없습니다.";
      throw new ApiException(ErrorCode.INVALID_REQUEST, message);
    }
  }

  /** 감사 기록에 저장할 앱 버전 정책 스냅샷을 만든다. */
  private String auditValue(AppVersion appVersion) {
    return ("platform=%s,versionName=%s,buildNumber=%s,minimumSupportedVersionName=%s,"
            + "forceUpdateReason=%s,softUpdateReason=%s,releaseNote=%s,active=%s,releasedAt=%s")
        .formatted(
            appVersion.getPlatform(),
            appVersion.getVersionName(),
            appVersion.getBuildNumber(),
            appVersion.getMinimumSupportedVersionName(),
            appVersion.getForceUpdateReason(),
            appVersion.getSoftUpdateReason(),
            appVersion.getReleaseNote(),
            appVersion.isActive(),
            appVersion.getReleasedAt());
  }
}
