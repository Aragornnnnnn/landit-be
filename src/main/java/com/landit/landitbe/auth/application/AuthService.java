// OIDC 검증 결과를 바탕으로 사용자를 가입 또는 갱신하고 자체 토큰을 발급한다.
package com.landit.landitbe.auth.application;

import com.landit.landitbe.auth.api.dto.AuthTokenResponse;
import com.landit.landitbe.auth.api.dto.AuthUserResponse;
import com.landit.landitbe.auth.api.dto.LogoutRequest;
import com.landit.landitbe.auth.api.dto.SocialLoginRequest;
import com.landit.landitbe.auth.api.dto.TokenRefreshRequest;
import com.landit.landitbe.auth.api.dto.TokenRefreshResponse;
import com.landit.landitbe.auth.domain.OauthIdentity;
import com.landit.landitbe.auth.domain.OauthIdentityStatus;
import com.landit.landitbe.auth.domain.RefreshToken;
import com.landit.landitbe.auth.domain.SocialProvider;
import com.landit.landitbe.auth.domain.UserProfile;
import com.landit.landitbe.auth.domain.UserProfileStatus;
import com.landit.landitbe.auth.infrastructure.OauthIdentityRepository;
import com.landit.landitbe.auth.infrastructure.RefreshTokenRepository;
import com.landit.landitbe.auth.infrastructure.UserProfileRepository;
import com.landit.landitbe.common.exception.ApiException;
import com.landit.landitbe.common.exception.ErrorCode;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final UserProfileRepository userProfileRepository;
    private final OauthIdentityRepository oauthIdentityRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OidcTokenVerifier oidcTokenVerifier;
    private final LanditTokenService tokenService;
    private final TokenProperties tokenProperties;

    public AuthService(
            UserProfileRepository userProfileRepository,
            OauthIdentityRepository oauthIdentityRepository,
            RefreshTokenRepository refreshTokenRepository,
            OidcTokenVerifier oidcTokenVerifier,
            LanditTokenService tokenService,
            TokenProperties tokenProperties
    ) {
        this.userProfileRepository = userProfileRepository;
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
        UserResult userResult = findOrCreateUser(userInfo);
        IssuedTokens issuedTokens = issueTokens(userResult.userProfile());

        return new AuthTokenResponse(
                TOKEN_TYPE,
                issuedTokens.accessToken(),
                tokenProperties.accessExpiresInSeconds(),
                issuedTokens.refreshToken(),
                tokenProperties.refreshExpiresInSeconds(),
                userResponse(userResult)
        );
    }

    /** refresh token을 회전하고 새 자체 토큰을 발급한다. */
    @Transactional
    public TokenRefreshResponse refresh(TokenRefreshRequest request) {
        LocalDateTime now = LocalDateTime.now();
        String refreshTokenHash = tokenService.hashToken(request.refreshToken());
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(refreshTokenHash)
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
                tokenProperties.refreshExpiresInSeconds()
        );
    }

    /** 전달받은 refresh token을 폐기한다. */
    @Transactional
    public void logout(LogoutRequest request) {
        refreshTokenRepository.findByTokenHash(tokenService.hashToken(request.refreshToken()))
                .ifPresent(refreshToken -> refreshToken.revoke(LocalDateTime.now()));
    }

    /** 현재 사용자를 탈퇴 처리하고 활성 refresh token을 모두 폐기한다. */
    @Transactional
    public void withdraw(Long userId) {
        UserProfile userProfile = userProfileRepository.findByIdAndStatus(userId, UserProfileStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(ErrorCode.AUTH_REQUIRED));
        refreshTokenRepository.revokeAllActiveByUserProfileId(userId, LocalDateTime.now());
        oauthIdentityRepository.findAllByUserProfileIdAndStatus(userId, OauthIdentityStatus.ACTIVE)
                .forEach(OauthIdentity::unlink);
        userProfile.withdraw();
    }

    private UserResult findOrCreateUser(OidcUserInfo userInfo) {
        return oauthIdentityRepository.findByProviderAndProviderUserIdAndStatus(
                        userInfo.provider(),
                        userInfo.sub(),
                        OauthIdentityStatus.ACTIVE
                )
                .map(identity -> {
                    UserProfile userProfile = identity.getUserProfile();
                    if (!userProfile.isActive()) {
                        throw new ApiException(ErrorCode.AUTH_REQUIRED);
                    }
                    userProfile.updateProfile(userInfo.email(), userInfo.nickname());
                    identity.updateProviderEmail(userInfo.email());
                    return new UserResult(userProfile, identity.getProvider(), false);
                })
                .orElseGet(() -> {
                    UserProfile userProfile = userProfileRepository.save(new UserProfile(
                            userInfo.email(),
                            userInfo.nickname()
                    ));
                    oauthIdentityRepository.save(new OauthIdentity(
                            userProfile,
                            userInfo.provider(),
                            userInfo.sub(),
                            userInfo.email()
                    ));
                    return new UserResult(userProfile, userInfo.provider(), true);
                });
    }

    private AuthUserResponse userResponse(UserResult userResult) {
        UserProfile userProfile = userResult.userProfile();
        return new AuthUserResponse(
                userProfile.getId(),
                userProfile.getNickname(),
                userProfile.getEmail(),
                userResult.provider().name(),
                userResult.newUser()
        );
    }

    private IssuedTokens issueTokens(UserProfile userProfile) {
        String accessToken = tokenService.createAccessToken(userProfile);
        String refreshToken = tokenService.createRefreshToken();
        refreshTokenRepository.save(new RefreshToken(
                userProfile,
                tokenService.hashToken(refreshToken),
                LocalDateTime.now().plusSeconds(tokenProperties.refreshExpiresInSeconds())
        ));
        return new IssuedTokens(accessToken, refreshToken);
    }

    private record UserResult(UserProfile userProfile, SocialProvider provider, boolean newUser) {
    }

    private record IssuedTokens(String accessToken, String refreshToken) {
    }
}
