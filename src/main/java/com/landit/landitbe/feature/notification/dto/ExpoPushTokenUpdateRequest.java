// Expo Push Token의 등록 상태 변경 요청을 검증한다.

package com.landit.landitbe.feature.notification.dto;

import com.landit.landitbe.shared.domain.AppPlatform;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.regex.Pattern;

/** Expo Push Token의 등록 상태 변경 요청을 검증한다. */
public record ExpoPushTokenUpdateRequest(
    @NotNull AppPlatform platform,
    @NotBlank @Size(max = 500) String expoPushToken,
    @NotNull Boolean enabled) {

  private static final Pattern UUID_TOKEN_PATTERN =
      Pattern.compile(
          "^[a-z\\d]{8}-[a-z\\d]{4}-[a-z\\d]{4}-[a-z\\d]{4}-[a-z\\d]{12}$",
          Pattern.CASE_INSENSITIVE);

  /**
   * Expo 공식 SDK가 지원하는 Push Token 형식인지 확인한다.
   *
   * @return 지원하는 Expo Push Token 형식이면 {@code true}
   */
  @AssertTrue(message = "Expo Push Token 형식이 올바르지 않습니다.")
  public boolean isExpoPushTokenFormatValid() {
    if (expoPushToken == null || expoPushToken.isBlank()) {
      return true;
    }
    return isBracketToken(expoPushToken) || UUID_TOKEN_PATTERN.matcher(expoPushToken).matches();
  }

  private boolean isBracketToken(String token) {
    return (token.startsWith("ExponentPushToken[") || token.startsWith("ExpoPushToken["))
        && token.endsWith("]");
  }
}
