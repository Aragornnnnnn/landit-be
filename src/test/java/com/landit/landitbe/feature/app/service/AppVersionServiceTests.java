// 앱 버전 정책 활성화의 잠금 및 감사 기록 동작을 검증한다.

package com.landit.landitbe.feature.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.admin.domain.AdminAction;
import com.landit.landitbe.feature.admin.service.AdminAuditService;
import com.landit.landitbe.feature.app.domain.AppVersion;
import com.landit.landitbe.feature.app.repository.AppVersionRepository;
import com.landit.landitbe.shared.domain.AppPlatform;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** 앱 버전 정책 활성화의 잠금 및 감사 기록 동작을 검증한다. */
@ExtendWith(MockitoExtension.class)
class AppVersionServiceTests {

  @Mock private AppVersionRepository appVersionRepository;

  @Mock private AdminAuditService adminAuditService;

  /** 활성화 감사 기록은 잠금 전에 읽은 값이 아니라 잠긴 정책의 현재 상태를 사용한다. */
  @Test
  void recordsActivationAuditFromLockedPolicy() {
    AppVersion lockedPolicy = policy(1L, true);
    when(appVersionRepository.findPlatformById(1L)).thenReturn(Optional.of(AppPlatform.IOS));
    when(appVersionRepository.findAllByPlatformForUpdate(AppPlatform.IOS))
        .thenReturn(List.of(lockedPolicy));
    AppVersionService appVersionService =
        new AppVersionService(appVersionRepository, adminAuditService);

    appVersionService.activate(10L, 1L);

    ArgumentCaptor<String> beforeValue = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> afterValue = ArgumentCaptor.forClass(String.class);
    verify(adminAuditService)
        .record(
            eq(10L),
            eq(AdminAction.APP_VERSION_ACTIVATED),
            eq("APP_VERSION"),
            eq("1"),
            beforeValue.capture(),
            afterValue.capture());
    assertThat(beforeValue.getValue()).contains("active=true");
    assertThat(afterValue.getValue()).contains("active=true");
  }

  /** 지정한 ID와 활성 상태를 가진 iOS 정책을 만든다. */
  private AppVersion policy(Long id, boolean active) {
    AppVersion appVersion =
        AppVersion.create(
            AppPlatform.IOS,
            "1.0.0",
            1,
            1,
            null,
            null,
            "릴리스 노트",
            LocalDateTime.of(2026, 7, 30, 9, 0));
    ReflectionTestUtils.setField(appVersion, "id", id);
    if (active) {
      appVersion.activate();
    }
    return appVersion;
  }
}
