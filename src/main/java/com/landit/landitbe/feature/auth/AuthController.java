// 인증 API 요청을 받아 소셜 로그인 결과를 공통 응답으로 반환한다.

package com.landit.landitbe.feature.auth;

import com.landit.landitbe.feature.auth.docs.AuthControllerDocs;
import com.landit.landitbe.feature.auth.dto.AuthTokenResponse;
import com.landit.landitbe.feature.auth.dto.LogoutRequest;
import com.landit.landitbe.feature.auth.dto.SocialLoginRequest;
import com.landit.landitbe.feature.auth.dto.TokenRefreshRequest;
import com.landit.landitbe.feature.auth.dto.TokenRefreshResponse;
import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.feature.auth.service.AuthService;
import com.landit.landitbe.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 인증 API 요청을 받아 소셜 로그인 결과를 공통 응답으로 반환한다. */
@RestController
public class AuthController implements AuthControllerDocs {

  private final AuthService authService;

  /**
   * 인증 요청 흐름을 처리할 Service를 주입받는다.
   *
   * @param authService 인증 Service
   */
  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  /** OIDC ID Token과 nonce를 검증한 뒤 자체 토큰을 발급한다. */
  @Override
  @PostMapping("/api/v1/auth/social-login")
  public ApiResponse<AuthTokenResponse> socialLogin(
      @Valid @RequestBody SocialLoginRequest request) {
    return ApiResponse.success(authService.socialLogin(request));
  }

  /** Refresh token을 회전하고 새 자체 토큰을 발급한다. */
  @Override
  @PostMapping("/api/v1/auth/token/refresh")
  public ApiResponse<TokenRefreshResponse> refresh(
      @Valid @RequestBody TokenRefreshRequest request) {
    return ApiResponse.success(authService.refresh(request));
  }

  /** 전달받은 refresh token을 폐기한다. */
  @Override
  @PostMapping("/api/v1/auth/logout")
  public ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest request) {
    authService.logout(request);
    return ApiResponse.success(null);
  }

  /** 현재 인증된 사용자를 탈퇴 처리한다. */
  @Override
  @DeleteMapping("/api/v1/auth/me")
  public ApiResponse<Void> withdraw(@AuthenticationPrincipal AuthUserPrincipal principal) {
    authService.withdraw(principal.userId());
    return ApiResponse.success(null);
  }
}
