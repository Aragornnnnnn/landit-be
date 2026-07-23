// 로그인 응답에서 사용자 식별 정보와 신규 가입 여부를 전달한다.

package com.landit.landitbe.feature.auth.dto;

import com.landit.landitbe.feature.auth.domain.SocialProvider;
import com.landit.landitbe.feature.profile.domain.UserProfile;

/** 로그인 응답에서 사용자 식별 정보와 신규 가입 여부를 전달한다. */
public record AuthUserResponse(
    Long userId, String nickname, String email, String provider, boolean newUser) {

  /** 사용자 프로필과 소셜 로그인 결과를 인증 사용자 응답으로 변환한다. */
  public static AuthUserResponse from(
      UserProfile userProfile, SocialProvider provider, boolean newUser) {
    return new AuthUserResponse(
        userProfile.getId(),
        userProfile.getNickname(),
        userProfile.getEmail(),
        provider.name(),
        newUser);
  }
}
