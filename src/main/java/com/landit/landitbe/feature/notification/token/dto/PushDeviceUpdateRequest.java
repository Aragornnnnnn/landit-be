// 앱 설치의 푸시 수신 상태와 현재 Expo Token을 전달한다.

package com.landit.landitbe.feature.notification.token.dto;

import com.landit.landitbe.shared.domain.AppPlatform;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 앱 설치의 푸시 수신 상태를 동기화한다.
 *
 * @param platform 앱 플랫폼
 * @param expoPushToken 활성화 시 현재 Expo Push Token
 * @param pushEnabled 기기의 푸시 수신 여부
 */
public record PushDeviceUpdateRequest(
    @NotNull AppPlatform platform,
    @Size(max = 500) String expoPushToken,
    @NotNull Boolean pushEnabled) {

  /** 활성화 요청에만 Expo Token을 요구하고, 제공된 값은 Expo 형식으로 검사한다. */
  @AssertTrue(message = "활성화 시 올바른 Expo Push Token이 필요합니다.")
  public boolean isExpoPushTokenValid() {
    if (pushEnabled == null) {
      return true;
    }
    if (expoPushToken == null || expoPushToken.isBlank()) {
      return !pushEnabled;
    }
    return new ExpoPushTokenUpdateRequest(platform, expoPushToken, pushEnabled)
        .isExpoPushTokenFormatValid();
  }
}
