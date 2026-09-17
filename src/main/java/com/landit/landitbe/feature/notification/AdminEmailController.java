// 관리자 이메일 테스트와 무료 체험 알림 채널 설정 API를 제공한다.

package com.landit.landitbe.feature.notification;

import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.feature.notification.docs.AdminEmailControllerDocs;
import com.landit.landitbe.feature.notification.dto.AdminEmailTestRequest;
import com.landit.landitbe.feature.notification.dto.NotificationJobView;
import com.landit.landitbe.feature.notification.dto.TrialReminderSettings;
import com.landit.landitbe.feature.notification.service.NotificationJobService;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import com.landit.landitbe.shared.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 기존 관리자 권한 필터로 보호하는 이메일 테스트 API다. */
@RestController
@RequestMapping("/api/v1/admin/notifications")
@RequiredArgsConstructor
public class AdminEmailController implements AdminEmailControllerDocs {
  private final NotificationJobService jobs;

  /** {@inheritDoc} */
  @Override
  @PostMapping("/email-tests")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public ApiResponse<NotificationJobView> test(
      @AuthenticationPrincipal AuthUserPrincipal principal,
      @RequestHeader("Idempotency-Key") UUID key,
      @Valid @RequestBody AdminEmailTestRequest request) {
    return ApiResponse.success(jobs.requestTest(principal.userId(), key, request.recipient()));
  }

  /** {@inheritDoc} */
  @Override
  @GetMapping("/jobs/{id}")
  public ApiResponse<NotificationJobView> job(@PathVariable UUID id) {
    return ApiResponse.success(
        jobs.find(id).orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND)).view());
  }

  /** {@inheritDoc} */
  @Override
  @GetMapping("/trial-reminder-settings")
  public ApiResponse<TrialReminderSettings> settings() {
    return ApiResponse.success(jobs.settings());
  }

  /** {@inheritDoc} */
  @Override
  @PutMapping("/trial-reminder-settings")
  public ApiResponse<TrialReminderSettings> update(
      @AuthenticationPrincipal AuthUserPrincipal principal,
      @Valid @RequestBody TrialReminderSettings settings) {
    return ApiResponse.success(jobs.updateSettings(principal.userId(), settings));
  }
}
