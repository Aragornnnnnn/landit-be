// 관리자 앱 버전 정책 수정의 잠금 조회를 검증한다.

package com.landit.landitbe.feature.app.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.admin.service.AdminAuditService;
import com.landit.landitbe.feature.app.domain.AppVersion;
import com.landit.landitbe.feature.app.dto.AdminAppVersionUpdateRequest;
import com.landit.landitbe.feature.app.repository.AppVersionRepository;
import com.landit.landitbe.shared.domain.AppPlatform;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 관리자 앱 버전 정책 수정의 잠금 조회를 검증한다. */
@ExtendWith(MockitoExtension.class)
class AppVersionServiceTests {

  @Mock private AppVersionRepository appVersionRepository;

  @Mock private AdminAuditService adminAuditService;

  @Mock private AppVersion appVersion;

  /** 정책 수정은 감사 기록의 이전 값을 만들기 전에 비관적 잠금을 획득한다. */
  @Test
  void updateLoadsPolicyWithWriteLockBeforeRecordingAuditSnapshot() {
    when(appVersionRepository.findByPlatformForUpdate(AppPlatform.IOS))
        .thenReturn(Optional.of(appVersion));
    AppVersionService appVersionService =
        new AppVersionService(appVersionRepository, adminAuditService);

    appVersionService.update(1L, AppPlatform.IOS, updateRequest());

    verify(appVersionRepository).findByPlatformForUpdate(AppPlatform.IOS);
    verify(appVersionRepository, never()).findByPlatform(AppPlatform.IOS);
  }

  /** 유효한 관리자 앱 버전 정책 수정 요청을 만든다. */
  private AdminAppVersionUpdateRequest updateRequest() {
    LocalDateTime releasedAt = LocalDateTime.of(2026, 7, 31, 10, 0);
    return new AdminAppVersionUpdateRequest(
        "1.1.0", 11, "1.0.0", "강제 업데이트", "권장 업데이트", "릴리스 노트", releasedAt);
  }
}
