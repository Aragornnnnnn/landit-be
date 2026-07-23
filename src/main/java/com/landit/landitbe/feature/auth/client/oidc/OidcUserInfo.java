// OIDC ID Token에서 검증된 사용자 식별 정보만 담는다.

package com.landit.landitbe.feature.auth.client.oidc;

import com.landit.landitbe.feature.auth.domain.SocialProvider;

/**
 * OIDC ID Token에서 검증된 사용자 식별 정보만 담는다.
 *
 * @param provider 소셜 로그인 제공자
 * @param sub OIDC 제공자 사용자 식별자
 * @param email 사용자 이메일
 * @param nickname 사용자 닉네임
 */
public record OidcUserInfo(SocialProvider provider, String sub, String email, String nickname) {}
