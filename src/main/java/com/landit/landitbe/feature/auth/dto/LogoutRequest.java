// 로그아웃할 refresh token을 전달한다.

package com.landit.landitbe.feature.auth.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

/**
 * 로그아웃할 refresh token을 전달한다.
 *
 * @param refreshToken Refresh token
 * @param installationId 로그아웃할 앱 설치 UUID. 이전 앱은 생략 가능
 */
public record LogoutRequest(@NotBlank String refreshToken, UUID installationId) {
  /** 기존 로그아웃 요청과 서비스 호출의 호환성을 유지한다. */
  public LogoutRequest(String refreshToken) {
    this(refreshToken, null);
  }
}
