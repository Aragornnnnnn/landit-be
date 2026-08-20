// 로그인 응답에서 사용자 식별 정보와 신규 가입 여부를 전달한다.

package com.landit.landitbe.feature.auth.dto;

import com.landit.landitbe.feature.auth.domain.SocialProvider;
import com.landit.landitbe.feature.profile.domain.UserProfileStatus;
import com.landit.landitbe.feature.profile.domain.UserRole;
import com.landit.landitbe.feature.profile.dto.AuthProfile;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 로그인 응답에서 사용자 식별 정보와 신규 가입 여부를 전달한다.
 *
 * @param userId 사용자 ID
 * @param nickname 사용자 닉네임
 * @param email 사용자 이메일
 * @param provider 소셜 로그인 제공자
 * @param newUser 신규 가입 사용자 여부
 * @param role 사용자 역할
 * @param status 사용자 프로필 상태
 */
public record AuthUserResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long userId,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String nickname,
    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            nullable = true,
            types = {"string", "null"})
        String email,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String provider,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean newUser,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UserRole role,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UserProfileStatus status) {

  /**
   * 사용자 프로필과 소셜 로그인 결과를 인증 사용자 응답으로 변환한다.
   *
   * @param authProfile 인증 기능용 사용자 프로필
   * @param provider 소셜 로그인 제공자
   * @param newUser 신규 가입 사용자 여부
   * @return 로그인 사용자 응답
   */
  public static AuthUserResponse from(
      AuthProfile authProfile, SocialProvider provider, boolean newUser) {
    return new AuthUserResponse(
        authProfile.userId(),
        authProfile.nickname(),
        authProfile.email(),
        provider.name(),
        newUser,
        authProfile.role(),
        authProfile.status());
  }
}
