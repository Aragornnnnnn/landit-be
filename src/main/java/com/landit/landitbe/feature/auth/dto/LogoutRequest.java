// 로그아웃할 refresh token을 전달한다.

package com.landit.landitbe.feature.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 로그아웃할 refresh token을 전달한다.
 *
 * @param refreshToken Refresh token
 */
public record LogoutRequest(@NotBlank String refreshToken) {}
