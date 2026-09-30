// 인증 API의 OpenAPI 문서를 정의한다.

package com.landit.landitbe.feature.auth.docs;

import com.landit.landitbe.feature.auth.dto.AuthTokenResponse;
import com.landit.landitbe.feature.auth.dto.LogoutRequest;
import com.landit.landitbe.feature.auth.dto.SocialLoginRequest;
import com.landit.landitbe.feature.auth.dto.TokenRefreshRequest;
import com.landit.landitbe.feature.auth.dto.TokenRefreshResponse;
import com.landit.landitbe.shared.response.ApiResponse;
import com.landit.landitbe.shared.security.AuthUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/** 인증 API의 OpenAPI 문서를 정의한다. */
@Tag(name = "Auth", description = "사용자 인증 API")
public interface AuthControllerDocs {

  /**
   * OIDC ID Token을 검증하고 서비스 토큰을 발급한다.
   *
   * @param request OIDC ID Token과 로그인 부가 정보
   * @return 자체 토큰과 로그인 사용자 정보
   */
  @Operation(summary = "소셜 로그인", description = "OIDC ID Token과 nonce를 검증하고 서비스 토큰을 발급한다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "로그인 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "OIDC 인증 실패")
  })
  ApiResponse<AuthTokenResponse> socialLogin(SocialLoginRequest request);

  /**
   * Refresh token을 회전하고 새 서비스 토큰을 발급한다.
   *
   * @param request 기존 Refresh token
   * @return 새 access token과 Refresh token
   */
  @Operation(summary = "토큰 갱신", description = "유효한 refresh token을 회전하고 새 토큰을 발급한다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "갱신 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "refresh token 오류")
  })
  ApiResponse<TokenRefreshResponse> refresh(TokenRefreshRequest request);

  /**
   * 전달받은 refresh token을 폐기한다.
   *
   * @param request 폐기할 Refresh token
   * @return 데이터가 없는 성공 응답
   */
  @Operation(
      summary = "로그아웃",
      description = "전달받은 refresh token을 폐기하고 installationId가 있으면 해당 설치의 푸시를 중지한다.")
  ApiResponse<Void> logout(LogoutRequest request);

  /**
   * 현재 인증된 사용자를 탈퇴 처리한다.
   *
   * @param principal 인증된 사용자
   * @return 데이터가 없는 성공 응답
   */
  @Operation(
      summary = "회원 탈퇴",
      description =
          "회원 행과 문의·첨부파일은 유지하고 닉네임을 '탈퇴한 사용자'로 덮어쓴다. "
              + "이메일·프로필 이미지·소셜 식별 원본과 refresh token을 정리하고, "
              + "장기기억을 삭제하며 계정 소유의 모든 푸시 Token을 비활성화한다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  ApiResponse<Void> withdraw(AuthUserPrincipal principal);
}
