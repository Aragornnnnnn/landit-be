// 인증 API 요청을 받아 소셜 로그인 결과를 공통 응답으로 반환한다.

package com.landit.landitbe.feature.auth;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 인증 API 요청을 받아 소셜 로그인 결과를 공통 응답으로 반환한다. */
@RequestMapping("/api/v1/auth")
@RestController
public class AuthController {

  private final AuthService authService;

  /** 동작을 수행한다. */
  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  /** OIDC ID Token과 nonce를 검증한 뒤 자체 토큰을 발급한다. */
  @PostMapping("/social-login")
  public ApiResponse<AuthTokenResponse> socialLogin(
      @Valid @RequestBody SocialLoginRequest request) {
    return ApiResponse.success(authService.socialLogin(request));
  }

  /** Refresh token을 회전하고 새 자체 토큰을 발급한다. */
  @PostMapping("/token/refresh")
  public ApiResponse<TokenRefreshResponse> refresh(
      @Valid @RequestBody TokenRefreshRequest request) {
    return ApiResponse.success(authService.refresh(request));
  }

  /** 전달받은 refresh token을 폐기한다. */
  @PostMapping("/logout")
  public ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest request) {
    authService.logout(request);
    return ApiResponse.success(null);
  }

  /** 현재 인증된 사용자를 탈퇴 처리한다. */
  @DeleteMapping("/me")
  public ApiResponse<Void> withdraw(@AuthenticationPrincipal AuthUserPrincipal principal) {
    authService.withdraw(principal.userId());
    return ApiResponse.success(null);
  }
}
