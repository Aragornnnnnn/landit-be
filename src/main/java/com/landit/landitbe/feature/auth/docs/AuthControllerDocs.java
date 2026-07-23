// 인증 API의 OpenAPI 문서를 정의한다.

package com.landit.landitbe.feature.auth.docs;

import com.landit.landitbe.feature.auth.dto.AuthTokenResponse;
import com.landit.landitbe.feature.auth.dto.LogoutRequest;
import com.landit.landitbe.feature.auth.dto.SocialLoginRequest;
import com.landit.landitbe.feature.auth.dto.TokenRefreshRequest;
import com.landit.landitbe.feature.auth.dto.TokenRefreshResponse;
import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/** 인증 API의 OpenAPI 문서를 정의한다. */
@Tag(name = "Auth", description = "사용자 인증 API")
public interface AuthControllerDocs {

  /** OIDC ID Token을 검증하고 서비스 토큰을 발급한다. */
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

  /** Refresh token을 회전하고 새 서비스 토큰을 발급한다. */
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

  /** 전달받은 refresh token을 폐기한다. */
  @Operation(summary = "로그아웃", description = "전달받은 refresh token을 폐기한다.")
  ApiResponse<Void> logout(LogoutRequest request);

  /** 현재 인증된 사용자를 탈퇴 처리한다. */
  @Operation(
      summary = "회원 탈퇴",
      description = "현재 사용자를 탈퇴 처리하고 활성 refresh token을 폐기한다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  ApiResponse<Void> withdraw(AuthUserPrincipal principal);
}
