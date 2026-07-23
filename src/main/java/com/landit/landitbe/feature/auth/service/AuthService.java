// OIDC 검증 결과를 바탕으로 사용자를 가입 또는 갱신하고 자체 토큰을 발급한다.

package com.landit.landitbe.feature.auth.service;

import com.landit.landitbe.config.auth.TokenProperties;
import com.landit.landitbe.feature.auth.client.oidc.OidcTokenVerifier;
import com.landit.landitbe.feature.auth.client.oidc.OidcUserInfo;
import com.landit.landitbe.feature.auth.domain.OauthIdentity;
import com.landit.landitbe.feature.auth.domain.OauthIdentityStatus;
import com.landit.landitbe.feature.auth.domain.RefreshToken;
import com.landit.landitbe.feature.auth.domain.SocialProvider;
import com.landit.landitbe.feature.auth.dto.AuthTokenResponse;
import com.landit.landitbe.feature.auth.dto.AuthUserResponse;
import com.landit.landitbe.feature.auth.dto.LogoutRequest;
import com.landit.landitbe.feature.auth.dto.SocialLoginRequest;
import com.landit.landitbe.feature.auth.dto.TokenRefreshRequest;
import com.landit.landitbe.feature.auth.dto.TokenRefreshResponse;
import com.landit.landitbe.feature.auth.repository.OauthIdentityRepository;
import com.landit.landitbe.feature.auth.repository.RefreshTokenRepository;
import com.landit.landitbe.feature.content.service.AiTutorService;
import com.landit.landitbe.feature.profile.dto.AuthProfile;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import com.landit.landitbe.shared.domain.AccentLocale;
import com.landit.landitbe.shared.domain.Locale;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** OIDC 검증 결과를 바탕으로 사용자를 가입 또는 갱신하고 자체 토큰을 발급한다. */
@Service
@Slf4j
public class AuthService {

  private static final String TOKEN_TYPE = "Bearer";
  private static final String GUEST_NICKNAME = "Guest";
  private static final AccentLocale DEFAULT_AI_TUTOR_ACCENT_LOCALE = AccentLocale.EN_US;
  private static final Locale DEFAULT_AI_TUTOR_TARGET_LOCALE = Locale.EN;

  private final UserProfileService userProfileService;
  private final AiTutorService aiTutorService;
  private final OauthIdentityRepository oauthIdentityRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final OidcTokenVerifier oidcTokenVerifier;
  private final LanditTokenService tokenService;
  private final TokenProperties tokenProperties;

  /**
   * 로그인부터 토큰 발급까지 필요한 인증 협력 객체를 주입받는다.
   *
   * @param userProfileService 사용자 프로필 Service
   * @param aiTutorService AI 튜터 Service
   * @param oauthIdentityRepository OAuth 연결 Repository
   * @param refreshTokenRepository Refresh token Repository
   * @param oidcTokenVerifier OIDC ID Token 검증기
   * @param tokenService 자체 토큰 Service
   * @param tokenProperties 자체 토큰 설정
   */
  public AuthService(
      UserProfileService userProfileService,
      AiTutorService aiTutorService,
      OauthIdentityRepository oauthIdentityRepository,
      RefreshTokenRepository refreshTokenRepository,
      OidcTokenVerifier oidcTokenVerifier,
      LanditTokenService tokenService,
      TokenProperties tokenProperties) {
    this.userProfileService = userProfileService;
    this.aiTutorService = aiTutorService;
    this.oauthIdentityRepository = oauthIdentityRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.oidcTokenVerifier = oidcTokenVerifier;
    this.tokenService = tokenService;
    this.tokenProperties = tokenProperties;
  }

  /**
   * 소셜 로그인 요청을 처리하고 자체 access token과 refresh token을 발급한다.
   *
   * @param request OIDC ID Token과 로그인 부가 정보
   * @return 자체 토큰과 로그인 사용자 정보
   * @throws ApiException OIDC 검증에 실패하거나 활성 사용자 또는 AI 튜터가 없을 때
   */
  @Transactional
  public AuthTokenResponse socialLogin(SocialLoginRequest request) {
    SocialProvider provider = SocialProvider.from(request.provider());
    OidcUserInfo userInfo = oidcTokenVerifier.verify(provider, request.idToken(), request.nonce());
    String nickname = resolveNickname(provider, request.nickname(), userInfo.nickname());
    UserResult userResult = findOrCreateUser(userInfo, nickname);
    IssuedTokens issuedTokens = issueTokens(userResult.authProfile());

    AuthTokenResponse response =
        AuthTokenResponse.from(
            TOKEN_TYPE,
            issuedTokens.accessToken(),
            tokenProperties.accessExpiresInSeconds(),
            issuedTokens.refreshToken(),
            tokenProperties.refreshExpiresInSeconds(),
            userResponse(userResult));
    log.info(
        "social login completed: userId={}, provider={}, newUser={}",
        userResult.authProfile().userId(),
        provider,
        userResult.newUser());
    return response;
  }

  /**
   * Refresh token을 회전하고 새 자체 토큰을 발급한다.
   *
   * @param request 기존 Refresh token
   * @return 새 access token과 Refresh token
   * @throws ApiException Refresh token이 없거나 만료 또는 폐기됐을 때
   */
  @Transactional
  public TokenRefreshResponse refresh(TokenRefreshRequest request) {
    String refreshTokenHash = tokenService.hashToken(request.refreshToken());
    Long userProfileId =
        refreshTokenRepository
            .findUserProfileIdByTokenHash(refreshTokenHash)
            .orElseThrow(() -> new ApiException(ErrorCode.REFRESH_TOKEN_INVALID));
    AuthProfile authProfile =
        userProfileService
            .findAuthenticationProfileForUpdate(userProfileId)
            .orElseThrow(() -> new ApiException(ErrorCode.REFRESH_TOKEN_INVALID));
    LocalDateTime now = LocalDateTime.now();
    if (refreshTokenRepository.revokeActiveByTokenHash(refreshTokenHash, now) != 1) {
      throw new ApiException(ErrorCode.REFRESH_TOKEN_INVALID);
    }
    IssuedTokens issuedTokens = issueTokens(authProfile);
    TokenRefreshResponse response =
        TokenRefreshResponse.from(
            TOKEN_TYPE,
            issuedTokens.accessToken(),
            tokenProperties.accessExpiresInSeconds(),
            issuedTokens.refreshToken(),
            tokenProperties.refreshExpiresInSeconds());
    log.info("auth token refreshed: userId={}", authProfile.userId());
    return response;
  }

  /**
   * 전달받은 Refresh token을 폐기한다.
   *
   * @param request 폐기할 Refresh token
   */
  @Transactional
  public void logout(LogoutRequest request) {
    String refreshTokenHash = tokenService.hashToken(request.refreshToken());
    refreshTokenRepository
        .findUserProfileIdByTokenHash(refreshTokenHash)
        .flatMap(userProfileService::findAuthenticationProfileForUpdate)
        .ifPresent(
            ignored ->
                refreshTokenRepository.revokeActiveByTokenHash(
                    refreshTokenHash, LocalDateTime.now()));
    log.info("logout request completed");
  }

  /**
   * 현재 사용자를 탈퇴 처리하고 활성 Refresh token을 모두 폐기한다.
   *
   * @param userId 탈퇴할 사용자 ID
   * @throws ApiException 활성 사용자를 찾을 수 없을 때
   */
  @Transactional
  public void withdraw(Long userId) {
    if (!userProfileService.withdrawIfActiveForUpdate(userId)) {
      throw new ApiException(ErrorCode.INVALID_TOKEN);
    }
    refreshTokenRepository.revokeAllActiveByUserProfileId(userId, LocalDateTime.now());
    oauthIdentityRepository
        .findAllByUserProfileIdAndStatus(userId, OauthIdentityStatus.ACTIVE)
        .forEach(OauthIdentity::unlink);
    log.info("user withdrawal completed: userId={}", userId);
  }

  /** 소셜 제공자별 닉네임 제공 방식 차이를 프로필에 저장할 값으로 정규화한다. */
  private String resolveNickname(
      SocialProvider provider, String requestNickname, String oidcNickname) {
    if (provider != SocialProvider.APPLE) {
      return oidcNickname;
    }
    return requestNickname == null || requestNickname.isBlank() ? null : requestNickname;
  }

  /** 기존 소셜 연결 사용자를 갱신하거나, 기본 AI 튜터가 설정된 신규 프로필을 생성한다. */
  private UserResult findOrCreateUser(OidcUserInfo userInfo, String nickname) {
    return oauthIdentityRepository
        .findByProviderAndProviderUserIdAndStatus(
            userInfo.provider(), userInfo.sub(), OauthIdentityStatus.ACTIVE)
        .map(
            identity -> {
              AuthProfile authProfile =
                  userProfileService
                      .updateAuthenticationProfileForUpdate(
                          identity.getUserProfileId(), userInfo.email(), nickname)
                      .orElseThrow(() -> new ApiException(ErrorCode.INVALID_TOKEN));
              identity.updateProviderEmail(userInfo.email());
              return new UserResult(authProfile, identity.getProvider(), false);
            })
        .orElseGet(
            () -> {
              Long defaultAiTutorId = requireDefaultAiTutorId();
              AuthProfile authProfile =
                  userProfileService.createAuthenticationProfile(
                      userInfo.email(),
                      nickname == null ? GUEST_NICKNAME : nickname,
                      defaultAiTutorId);
              oauthIdentityRepository.save(
                  new OauthIdentity(
                      authProfile.userId(), userInfo.provider(), userInfo.sub(), userInfo.email()));
              return new UserResult(authProfile, userInfo.provider(), true);
            });
  }

  /** 신규 회원에게 할당할 활성 미국 영어 튜터가 정확히 하나인지 검증하고 ID를 반환한다. */
  private Long requireDefaultAiTutorId() {
    return aiTutorService.requireSingleActiveTutorId(
        DEFAULT_AI_TUTOR_ACCENT_LOCALE, DEFAULT_AI_TUTOR_TARGET_LOCALE);
  }

  /** 내부 사용자 및 로그인 결과를 인증 API 응답 형식으로 변환한다. */
  private AuthUserResponse userResponse(UserResult userResult) {
    return AuthUserResponse.from(
        userResult.authProfile(), userResult.provider(), userResult.newUser());
  }

  /** Access token과 회전용 refresh token을 발급하고 refresh token 해시를 저장한다. */
  private IssuedTokens issueTokens(AuthProfile authProfile) {
    String accessToken = tokenService.createAccessToken(authProfile.userId());
    String refreshToken = tokenService.createRefreshToken();
    refreshTokenRepository.save(
        new RefreshToken(
            authProfile.userId(),
            tokenService.hashToken(refreshToken),
            LocalDateTime.now().plusSeconds(tokenProperties.refreshExpiresInSeconds())));
    return new IssuedTokens(accessToken, refreshToken);
  }

  private record UserResult(AuthProfile authProfile, SocialProvider provider, boolean newUser) {}

  private record IssuedTokens(String accessToken, String refreshToken) {}
}
