// AuthService의 프로필 잠금과 Refresh Token 원자적 회전을 단위 검증한다.

package com.landit.landitbe.feature.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.landit.landitbe.config.auth.TokenProperties;
import com.landit.landitbe.feature.auth.client.oidc.OidcTokenVerifier;
import com.landit.landitbe.feature.auth.domain.RefreshToken;
import com.landit.landitbe.feature.auth.dto.LogoutRequest;
import com.landit.landitbe.feature.auth.dto.TokenRefreshRequest;
import com.landit.landitbe.feature.auth.dto.TokenRefreshResponse;
import com.landit.landitbe.feature.auth.repository.OauthIdentityRepository;
import com.landit.landitbe.feature.auth.repository.RefreshTokenRepository;
import com.landit.landitbe.feature.content.service.AiTutorService;
import com.landit.landitbe.feature.memory.service.ConversationMemoryDeletionService;
import com.landit.landitbe.feature.profile.domain.UserProfileStatus;
import com.landit.landitbe.feature.profile.domain.UserRole;
import com.landit.landitbe.feature.profile.dto.AuthProfile;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

/** AuthService의 프로필 잠금과 Refresh Token 원자적 회전을 단위 검증한다. */
class AuthServiceTest {

  private static final Long USER_ID = 1L;
  private static final String CURRENT_TOKEN = "current-refresh-token";
  private static final String CURRENT_TOKEN_HASH = "current-refresh-token-hash";

  private final UserProfileService userProfileService = mock(UserProfileService.class);
  private final AiTutorService aiTutorService = mock(AiTutorService.class);
  private final OauthIdentityRepository oauthIdentityRepository =
      mock(OauthIdentityRepository.class);
  private final RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
  private final OidcTokenVerifier oidcTokenVerifier = mock(OidcTokenVerifier.class);
  private final LanditTokenService tokenService = mock(LanditTokenService.class);
  private final ConversationMemoryDeletionService conversationMemoryDeletionService =
      mock(ConversationMemoryDeletionService.class);

  private AuthService authService;

  /** 테스트마다 고정된 토큰 만료 설정으로 AuthService를 생성한다. */
  @BeforeEach
  void setUp() {
    authService =
        new AuthService(
            userProfileService,
            aiTutorService,
            oauthIdentityRepository,
            refreshTokenRepository,
            oidcTokenVerifier,
            conversationMemoryDeletionService,
            tokenService,
            new TokenProperties("test-secret", 1800, 1209600));
  }

  /** 프로필을 먼저 잠근 뒤 기존 Refresh Token을 조건부 폐기하고 새 토큰을 발급한다. */
  @Test
  void refreshLocksProfileBeforeRevokingToken() {
    AuthProfile authProfile =
        new AuthProfile(
            USER_ID, "nickname", "user@example.com", UserRole.USER, UserProfileStatus.ACTIVE);
    when(tokenService.hashToken(CURRENT_TOKEN)).thenReturn(CURRENT_TOKEN_HASH);
    when(refreshTokenRepository.findUserProfileIdByTokenHash(CURRENT_TOKEN_HASH))
        .thenReturn(Optional.of(USER_ID));
    when(userProfileService.findAuthenticationProfileForUpdate(USER_ID))
        .thenReturn(Optional.of(authProfile));
    when(refreshTokenRepository.revokeActiveByTokenHash(
            eq(CURRENT_TOKEN_HASH), any(LocalDateTime.class)))
        .thenReturn(1);
    when(tokenService.createAccessToken(USER_ID)).thenReturn("new-access-token");
    when(tokenService.createRefreshToken()).thenReturn("new-refresh-token");
    when(tokenService.hashToken("new-refresh-token")).thenReturn("new-refresh-token-hash");

    TokenRefreshResponse response = authService.refresh(new TokenRefreshRequest(CURRENT_TOKEN));

    InOrder lockOrder = inOrder(refreshTokenRepository, userProfileService);
    lockOrder.verify(refreshTokenRepository).findUserProfileIdByTokenHash(CURRENT_TOKEN_HASH);
    lockOrder.verify(userProfileService).findAuthenticationProfileForUpdate(USER_ID);
    lockOrder
        .verify(refreshTokenRepository)
        .revokeActiveByTokenHash(eq(CURRENT_TOKEN_HASH), any(LocalDateTime.class));
    assertThat(response.accessToken()).isEqualTo("new-access-token");
    assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
    verify(refreshTokenRepository).save(any(RefreshToken.class));
  }

  /** 이미 소비된 Refresh Token이면 새 자격증명을 발급하지 않는다. */
  @Test
  void refreshRejectsConcurrentlyConsumedToken() {
    AuthProfile authProfile =
        new AuthProfile(
            USER_ID, "nickname", "user@example.com", UserRole.USER, UserProfileStatus.ACTIVE);
    when(tokenService.hashToken(CURRENT_TOKEN)).thenReturn(CURRENT_TOKEN_HASH);
    when(refreshTokenRepository.findUserProfileIdByTokenHash(CURRENT_TOKEN_HASH))
        .thenReturn(Optional.of(USER_ID));
    when(userProfileService.findAuthenticationProfileForUpdate(USER_ID))
        .thenReturn(Optional.of(authProfile));
    when(refreshTokenRepository.revokeActiveByTokenHash(
            eq(CURRENT_TOKEN_HASH), any(LocalDateTime.class)))
        .thenReturn(0);

    assertThatThrownBy(() -> authService.refresh(new TokenRefreshRequest(CURRENT_TOKEN)))
        .isInstanceOf(ApiException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.REFRESH_TOKEN_INVALID);
    verify(tokenService, never()).createAccessToken(any());
    verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
  }

  /** 로그아웃도 프로필을 먼저 잠근 뒤 Refresh Token을 조건부 폐기한다. */
  @Test
  void logoutLocksProfileBeforeRevokingToken() {
    AuthProfile authProfile =
        new AuthProfile(
            USER_ID, "nickname", "user@example.com", UserRole.USER, UserProfileStatus.ACTIVE);
    when(tokenService.hashToken(CURRENT_TOKEN)).thenReturn(CURRENT_TOKEN_HASH);
    when(refreshTokenRepository.findUserProfileIdByTokenHash(CURRENT_TOKEN_HASH))
        .thenReturn(Optional.of(USER_ID));
    when(userProfileService.findAuthenticationProfileForUpdate(USER_ID))
        .thenReturn(Optional.of(authProfile));

    authService.logout(new LogoutRequest(CURRENT_TOKEN));

    InOrder lockOrder = inOrder(refreshTokenRepository, userProfileService);
    lockOrder.verify(refreshTokenRepository).findUserProfileIdByTokenHash(CURRENT_TOKEN_HASH);
    lockOrder.verify(userProfileService).findAuthenticationProfileForUpdate(USER_ID);
    lockOrder
        .verify(refreshTokenRepository)
        .revokeActiveByTokenHash(eq(CURRENT_TOKEN_HASH), any(LocalDateTime.class));
  }

  @Test
  void withdrawDeletesMemoryBeforeRevokingAuthenticationArtifacts() {
    when(userProfileService.withdrawIfActiveForUpdate(USER_ID)).thenReturn(true);
    when(oauthIdentityRepository.findAllByUserProfileIdAndStatus(any(), any()))
        .thenReturn(List.of());

    authService.withdraw(USER_ID);

    InOrder withdrawalOrder =
        inOrder(userProfileService, conversationMemoryDeletionService, refreshTokenRepository);
    withdrawalOrder.verify(userProfileService).withdrawIfActiveForUpdate(USER_ID);
    withdrawalOrder.verify(conversationMemoryDeletionService).deleteAllByUserProfileId(USER_ID);
    withdrawalOrder
        .verify(refreshTokenRepository)
        .revokeAllActiveByUserProfileId(eq(USER_ID), any(LocalDateTime.class));
  }
}
