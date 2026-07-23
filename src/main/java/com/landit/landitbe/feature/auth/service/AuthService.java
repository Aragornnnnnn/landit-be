// OIDC 검증 결과를 바탕으로 사용자를 가입 또는 갱신하고 자체 토큰을 발급한다.

package com.landit.landitbe.feature.auth.service;

import com.landit.landitbe.config.auth.TokenProperties;
import com.landit.landitbe.feature.auth.client.oidc.OidcTokenVerifier;
import com.landit.landitbe.feature.auth.client.oidc.OidcUserInfo;
import com.landit.landitbe.feature.auth.domain.OauthIdentity;
import com.landit.landitbe.feature.auth.domain.OauthIdentityStatus;
import com.landit.landitbe.feature.auth.domain.RefreshToken;
import com.landit.landitbe.feature.auth.domain.SocialProvider;
import com.landit.landitbe.feature.auth.domain.UserProfile;
import com.landit.landitbe.feature.auth.domain.UserProfileStatus;
import com.landit.landitbe.feature.auth.dto.AuthTokenResponse;
import com.landit.landitbe.feature.auth.dto.AuthUserResponse;
import com.landit.landitbe.feature.auth.dto.LogoutRequest;
import com.landit.landitbe.feature.auth.dto.SocialLoginRequest;
import com.landit.landitbe.feature.auth.dto.TokenRefreshRequest;
import com.landit.landitbe.feature.auth.dto.TokenRefreshResponse;
import com.landit.landitbe.feature.auth.repository.OauthIdentityRepository;
import com.landit.landitbe.feature.auth.repository.RefreshTokenRepository;
import com.landit.landitbe.feature.auth.repository.UserProfileRepository;
import com.landit.landitbe.feature.content.domain.AiTutor;
import com.landit.landitbe.feature.content.repository.AiTutorRepository;
import com.landit.landitbe.shared.domain.AccentLocale;
import com.landit.landitbe.shared.domain.ActiveStatus;
import com.landit.landitbe.shared.domain.Locale;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** OIDC 검증 결과를 바탕으로 사용자를 가입 또는 갱신하고 자체 토큰을 발급한다. */
@Service
public class AuthService {

  private static final String TOKEN_TYPE = "Bearer";
  private static final String GUEST_NICKNAME = "Guest";
  private static final AccentLocale DEFAULT_AI_TUTOR_ACCENT_LOCALE = AccentLocale.EN_US;
  private static final Locale DEFAULT_AI_TUTOR_TARGET_LOCALE = Locale.EN;

  private final UserProfileRepository userProfileRepository;
  private final AiTutorRepository aiTutorRepository;
  private final OauthIdentityRepository oauthIdentityRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final OidcTokenVerifier oidcTokenVerifier;
  private final LanditTokenService tokenService;
  private final TokenProperties tokenProperties;

  /** 동작을 수행한다. */
  public AuthService(
      UserProfileRepository userProfileRepository,
      AiTutorRepository aiTutorRepository,
      OauthIdentityRepository oauthIdentityRepository,
      RefreshTokenRepository refreshTokenRepository,
      OidcTokenVerifier oidcTokenVerifier,
      LanditTokenService tokenService,
      TokenProperties tokenProperties) {
    this.userProfileRepository = userProfileRepository;
    this.aiTutorRepository = aiTutorRepository;
    this.oauthIdentityRepository = oauthIdentityRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.oidcTokenVerifier = oidcTokenVerifier;
    this.tokenService = tokenService;
    this.tokenProperties = tokenProperties;
  }

  /** 소셜 로그인 요청을 처리하고 자체 access token과 refresh token을 발급한다. */
  @Transactional
  public AuthTokenResponse socialLogin(SocialLoginRequest request) {
    SocialProvider provider = SocialProvider.from(request.provider());
    OidcUserInfo userInfo = oidcTokenVerifier.verify(provider, request.idToken(), request.nonce());
    String nickname = resolveNickname(provider, request.nickname(), userInfo.nickname());
    UserResult userResult = findOrCreateUser(userInfo, nickname);
    IssuedTokens issuedTokens = issueTokens(userResult.userProfile());

    return new AuthTokenResponse(
        TOKEN_TYPE,
        issuedTokens.accessToken(),
        tokenProperties.accessExpiresInSeconds(),
        issuedTokens.refreshToken(),
        tokenProperties.refreshExpiresInSeconds(),
        userResponse(userResult));
  }

  /** Refresh token을 회전하고 새 자체 토큰을 발급한다. */
  @Transactional
  public TokenRefreshResponse refresh(TokenRefreshRequest request) {
    LocalDateTime now = LocalDateTime.now();
    String refreshTokenHash = tokenService.hashToken(request.refreshToken());
    RefreshToken refreshToken =
        refreshTokenRepository
            .findByTokenHash(refreshTokenHash)
            .orElseThrow(() -> new ApiException(ErrorCode.REFRESH_TOKEN_INVALID));
    UserProfile userProfile = refreshToken.getUserProfile();
    if (!refreshToken.isActive(now) || !userProfile.isActive()) {
      throw new ApiException(ErrorCode.REFRESH_TOKEN_INVALID);
    }

    refreshToken.revoke(now);
    IssuedTokens issuedTokens = issueTokens(userProfile);
    return new TokenRefreshResponse(
        TOKEN_TYPE,
        issuedTokens.accessToken(),
        tokenProperties.accessExpiresInSeconds(),
        issuedTokens.refreshToken(),
        tokenProperties.refreshExpiresInSeconds());
  }

  /** 전달받은 refresh token을 폐기한다. */
  @Transactional
  public void logout(LogoutRequest request) {
    refreshTokenRepository
        .findByTokenHash(tokenService.hashToken(request.refreshToken()))
        .ifPresent(refreshToken -> refreshToken.revoke(LocalDateTime.now()));
  }

  /** 현재 사용자를 탈퇴 처리하고 활성 refresh token을 모두 폐기한다. */
  @Transactional
  public void withdraw(Long userId) {
    UserProfile userProfile =
        userProfileRepository
            .findByIdAndStatus(userId, UserProfileStatus.ACTIVE)
            .orElseThrow(() -> new ApiException(ErrorCode.INVALID_TOKEN));
    refreshTokenRepository.revokeAllActiveByUserProfileId(userId, LocalDateTime.now());
    oauthIdentityRepository
        .findAllByUserProfileIdAndStatus(userId, OauthIdentityStatus.ACTIVE)
        .forEach(OauthIdentity::unlink);
    userProfile.withdraw();
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
              UserProfile userProfile = identity.getUserProfile();
              if (!userProfile.isActive()) {
                throw new ApiException(ErrorCode.INVALID_TOKEN);
              }
              userProfile.updateProfile(userInfo.email(), nickname);
              identity.updateProviderEmail(userInfo.email());
              return new UserResult(userProfile, identity.getProvider(), false);
            })
        .orElseGet(
            () -> {
              Long defaultAiTutorId = requireDefaultAiTutorId();
              UserProfile userProfile =
                  userProfileRepository.save(
                      new UserProfile(
                          userInfo.email(),
                          nickname == null ? GUEST_NICKNAME : nickname,
                          defaultAiTutorId));
              oauthIdentityRepository.save(
                  new OauthIdentity(
                      userProfile, userInfo.provider(), userInfo.sub(), userInfo.email()));
              return new UserResult(userProfile, userInfo.provider(), true);
            });
  }

  /** 신규 회원에게 할당할 활성 미국 영어 튜터가 정확히 하나인지 검증하고 ID를 반환한다. */
  private Long requireDefaultAiTutorId() {
    List<AiTutor> defaultTutorCandidates =
        aiTutorRepository.findAllByAccentLocaleAndTargetLocaleAndStatus(
            DEFAULT_AI_TUTOR_ACCENT_LOCALE, DEFAULT_AI_TUTOR_TARGET_LOCALE, ActiveStatus.ACTIVE);
    if (defaultTutorCandidates.size() != 1) {
      throw new ApiException(ErrorCode.DEFAULT_AI_TUTOR_NOT_CONFIGURED);
    }
    return defaultTutorCandidates.getFirst().getId();
  }

  /** 내부 사용자 및 로그인 결과를 인증 API 응답 형식으로 변환한다. */
  private AuthUserResponse userResponse(UserResult userResult) {
    UserProfile userProfile = userResult.userProfile();
    return new AuthUserResponse(
        userProfile.getId(),
        userProfile.getNickname(),
        userProfile.getEmail(),
        userResult.provider().name(),
        userResult.newUser());
  }

  /** Access token과 회전용 refresh token을 발급하고 refresh token 해시를 저장한다. */
  private IssuedTokens issueTokens(UserProfile userProfile) {
    String accessToken = tokenService.createAccessToken(userProfile);
    String refreshToken = tokenService.createRefreshToken();
    refreshTokenRepository.save(
        new RefreshToken(
            userProfile,
            tokenService.hashToken(refreshToken),
            LocalDateTime.now().plusSeconds(tokenProperties.refreshExpiresInSeconds())));
    return new IssuedTokens(accessToken, refreshToken);
  }

  private record UserResult(UserProfile userProfile, SocialProvider provider, boolean newUser) {}

  private record IssuedTokens(String accessToken, String refreshToken) {}
}
