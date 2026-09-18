// 인증된 사용자의 일일 알람 설정 조회와 변경 요청을 처리한다.

package com.landit.landitbe.feature.profile.alarm;

import com.landit.landitbe.feature.profile.alarm.docs.UserAlarmControllerDocs;
import com.landit.landitbe.feature.profile.alarm.dto.UserAlarmResponse;
import com.landit.landitbe.feature.profile.alarm.dto.UserAlarmUpdateRequest;
import com.landit.landitbe.feature.profile.alarm.service.UserAlarmService;
import com.landit.landitbe.shared.response.ApiResponse;
import com.landit.landitbe.shared.security.AuthUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 인증된 사용자의 일일 알람 설정 API 요청을 처리한다. */
@RestController
@RequiredArgsConstructor
public class UserAlarmController implements UserAlarmControllerDocs {

  private final UserAlarmService userAlarmService;

  /** {@inheritDoc} */
  @Override
  @GetMapping("/api/v1/me/alarm")
  public ApiResponse<UserAlarmResponse> getAlarm(
      @AuthenticationPrincipal AuthUserPrincipal principal) {
    return ApiResponse.success(userAlarmService.getAlarm(principal.userId()));
  }

  /** {@inheritDoc} */
  @Override
  @PutMapping("/api/v1/me/alarm")
  public ApiResponse<UserAlarmResponse> updateAlarm(
      @AuthenticationPrincipal AuthUserPrincipal principal,
      @Valid @RequestBody UserAlarmUpdateRequest request) {
    return ApiResponse.success(userAlarmService.updateAlarm(principal.userId(), request));
  }
}
