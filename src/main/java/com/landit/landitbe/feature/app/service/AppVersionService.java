// 공개 업데이트 확인과 관리자 앱 버전 정책 관리를 담당한다.

package com.landit.landitbe.feature.app.service;

import com.landit.landitbe.feature.admin.domain.AdminAction;
import com.landit.landitbe.feature.admin.service.AdminAuditService;
import com.landit.landitbe.feature.app.domain.AppVersion;
import com.landit.landitbe.feature.app.dto.AdminAppVersionCreateRequest;
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
   * 플랫폼별 활성 정책을 기준으로 앱 업데이트 필요 수준을 반환한다.
   *
   * @param platform 앱 플랫폼
   * @param currentBuildNumber 현재 앱 빌드 번호
   * @return 앱 업데이트 필요 수준과 활성 정책 정보
   * @throws ApiException 활성 앱 버전 정책이 설정되지 않았을 때
   */
  @Transactional(readOnly = true)
  public AppVersionCheckResponse check(AppPlatform platform, long currentBuildNumber) {
    AppVersion policy =
        appVersionRepository
            .findByPlatformAndActiveTrue(platform)
            .orElseThrow(() -> new ApiException(ErrorCode.APP_VERSION_POLICY_NOT_CONFIGURED));
    UpdateType updateType = updateType(currentBuildNumber, policy);
    return AppVersionCheckResponse.from(policy, updateType, reason(updateType, policy));
  }

  /**
   * 관리자 화면에 표시할 전체 앱 버전 정책 목록을 반환한다.
   *
   * @return 플랫폼과 빌드 번호 순으로 정렬된 앱 버전 정책 목록
   */
  @Transactional(readOnly = true)
  public List<AdminAppVersionResponse> list() {
    return appVersionRepository.findAllByOrderByPlatformAscBuildNumberDesc().stream()
        .map(AdminAppVersionResponse::from)
        .toList();
  }

  /**
   * 관리자 입력으로 비활성 앱 버전 정책을 등록한다.
   *
   * @param adminUserProfileId 작업을 수행한 관리자 사용자 프로필 ID
   * @param request 앱 버전 정책 등록 요청
   * @return 등록된 비활성 앱 버전 정책
   * @throws ApiException 빌드 번호 범위가 유효하지 않거나 같은 정책이 이미 있을 때
   */
  @Transactional
  public AdminAppVersionResponse create(
      Long adminUserProfileId, AdminAppVersionCreateRequest request) {
    validateBuildRange(request.buildNumber(), request.minimumSupportedBuildNumber());
    if (appVersionRepository.existsByPlatformAndBuildNumber(
        request.platform(), request.buildNumber())) {
      throw new ApiException(ErrorCode.CONFLICT, "같은 플랫폼과 빌드 번호의 정책이 이미 존재합니다.");
    }
    AppVersion appVersion =
        appVersionRepository.save(
            AppVersion.create(
                request.platform(),
                request.versionName(),
                request.buildNumber(),
                request.minimumSupportedBuildNumber(),
                request.forceUpdateReason(),
                request.softUpdateReason(),
                request.releaseNote(),
                request.releasedAt()));
    adminAuditService.record(
        adminUserProfileId,
        AdminAction.APP_VERSION_CREATED,
        "APP_VERSION",
        appVersion.getId().toString(),
        null,
        auditValue(appVersion));
    return AdminAppVersionResponse.from(appVersion);
  }

  /**
   * 관리자 입력으로 앱 버전 정책을 수정한다.
   *
   * @param adminUserProfileId 작업을 수행한 관리자 사용자 프로필 ID
   * @param appVersionId 수정할 앱 버전 정책 ID
   * @param request 앱 버전 정책 수정 요청
   * @return 수정된 앱 버전 정책
   * @throws ApiException 정책이 없거나 빌드 번호 범위 또는 중복 정책이 유효하지 않을 때
   */
  @Transactional
  public AdminAppVersionResponse update(
      Long adminUserProfileId, Long appVersionId, AdminAppVersionUpdateRequest request) {
    validateBuildRange(request.buildNumber(), request.minimumSupportedBuildNumber());
    AppVersion appVersion = require(appVersionId);
    if (appVersion.getBuildNumber() != request.buildNumber()
        && appVersionRepository.existsByPlatformAndBuildNumber(
            appVersion.getPlatform(), request.buildNumber())) {
      throw new ApiException(ErrorCode.CONFLICT, "같은 플랫폼과 빌드 번호의 정책이 이미 존재합니다.");
    }
    String beforeValue = auditValue(appVersion);
    appVersion.update(
        request.versionName(),
        request.buildNumber(),
        request.minimumSupportedBuildNumber(),
        request.forceUpdateReason(),
        request.softUpdateReason(),
        request.releaseNote(),
        request.releasedAt());
    adminAuditService.record(
        adminUserProfileId,
        AdminAction.APP_VERSION_UPDATED,
        "APP_VERSION",
        appVersionId.toString(),
        beforeValue,
        auditValue(appVersion));
    return AdminAppVersionResponse.from(appVersion);
  }

  /**
   * 대상 정책을 활성화하고 같은 플랫폼의 기존 활성 정책을 비활성화한다.
   *
   * @param adminUserProfileId 작업을 수행한 관리자 사용자 프로필 ID
   * @param appVersionId 활성화할 앱 버전 정책 ID
   * @return 활성화된 앱 버전 정책
   * @throws ApiException 대상 앱 버전 정책이 없을 때
   */
  @Transactional
  public AdminAppVersionResponse activate(Long adminUserProfileId, Long appVersionId) {
    AppVersion appVersion = require(appVersionId);
    String beforeValue = auditValue(appVersion);
    appVersionRepository.findAllByPlatformForUpdate(appVersion.getPlatform()).stream()
        .filter(AppVersion::isActive)
        .filter(activePolicy -> !activePolicy.getId().equals(appVersionId))
        .forEach(AppVersion::deactivate);
    appVersion.activate();
    adminAuditService.record(
        adminUserProfileId,
        AdminAction.APP_VERSION_ACTIVATED,
        "APP_VERSION",
        appVersionId.toString(),
        beforeValue,
        auditValue(appVersion));
    return AdminAppVersionResponse.from(appVersion);
  }

  /** 앱 버전 정책을 ID로 조회한다. */
  private AppVersion require(Long appVersionId) {
    return appVersionRepository
        .findById(appVersionId)
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "앱 버전 정책을 찾을 수 없습니다."));
  }

  /** DB 제약보다 먼저 빌드 번호 관계를 API 오류로 검증한다. */
  private void validateBuildRange(long buildNumber, long minimumSupportedBuildNumber) {
    if (minimumSupportedBuildNumber > buildNumber) {
      throw new ApiException(ErrorCode.INVALID_REQUEST, "최소 지원 빌드는 최신 빌드보다 클 수 없습니다.");
    }
  }

  /** 현재 빌드와 활성 정책을 비교해 업데이트 수준을 계산한다. */
  private UpdateType updateType(long currentBuildNumber, AppVersion policy) {
    if (currentBuildNumber < policy.getMinimumSupportedBuildNumber()) {
      return UpdateType.FORCE;
    }
    if (currentBuildNumber < policy.getBuildNumber()) {
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
        + ",minimumSupportedBuildNumber="
        + appVersion.getMinimumSupportedBuildNumber()
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
