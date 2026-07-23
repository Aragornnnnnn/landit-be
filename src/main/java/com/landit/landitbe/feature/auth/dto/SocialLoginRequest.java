// 소셜 로그인 요청의 제공자, ID Token, nonce, nickname 값을 전달한다.

package com.landit.landitbe.feature.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 소셜 로그인 요청의 제공자, ID Token, nonce, nickname 값을 전달한다.
 *
 * @param provider 소셜 로그인 제공자
 * @param idToken OIDC ID Token
 * @param nonce 재전송 공격 방지 nonce
 * @param nickname 사용자 닉네임
 */
public record SocialLoginRequest(
    @NotBlank String provider, @NotBlank String idToken, String nonce, String nickname) {}
