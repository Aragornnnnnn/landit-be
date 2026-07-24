// 앱 설치의 푸시 수신 상태를 동기화하는 요청을 정의한다.

package com.landit.landitbe.feature.notification.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.landit.landitbe.shared.domain.AppPlatform;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 앱 설치의 푸시 수신 상태를 동기화하는 요청을 정의한다.
 *
 * @param platform 기기 플랫폼
 * @param pushEnabled Landit 푸시 알림 수신 여부
 * @param expoPushToken Expo Push Token
 */
@Schema(description = "푸시 알림 설치 상태 동기화 요청")
public record PushDeviceSyncRequest(
    @NotNull @Schema(description = "기기 플랫폼", example = "IOS") AppPlatform platform,
    @NotNull @Schema(description = "Landit 푸시 알림 수신 여부", example = "true") Boolean pushEnabled,
    @Size(max = 500)
        @Schema(
            description = "Expo Push Token. pushEnabled가 true이면 필수입니다.",
            example = "ExponentPushToken[xxxxxxxxxxxxxxxxxxxxxx]",
            nullable = true)
        String expoPushToken) {

  /**
   * 알림 활성 요청에 비어 있지 않은 Expo Push Token이 포함됐는지 검증한다.
   *
   * @return 활성 요청이 유효하면 {@code true}
   */
  @AssertTrue
  @JsonIgnore
  public boolean isExpoPushTokenPresentWhenEnabled() {
    return !Boolean.TRUE.equals(pushEnabled) || (expoPushToken != null && !expoPushToken.isBlank());
  }

  /**
   * 빈 Expo Push Token을 null로 정규화한다.
   *
   * @return 정규화된 Expo Push Token
   */
  public String normalizedExpoPushToken() {
    return expoPushToken == null || expoPushToken.isBlank() ? null : expoPushToken;
  }
}
