// Expo Push Token 상태를 독립된 트랜잭션으로 저장한다.

package com.landit.landitbe.feature.notification.service;

import com.landit.landitbe.feature.notification.domain.UserPushToken;
import com.landit.landitbe.feature.notification.dto.ExpoPushTokenUpdateRequest;
import com.landit.landitbe.feature.notification.repository.UserPushTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Expo Push Token 상태를 독립된 트랜잭션으로 저장한다. */
@Service
public class ExpoPushTokenPersistenceService {

  private final UserPushTokenRepository userPushTokenRepository;

  /**
   * 사용자 Expo Push Token Repository를 주입받는다.
   *
   * @param userPushTokenRepository 사용자 Expo Push Token Repository
   */
  public ExpoPushTokenPersistenceService(UserPushTokenRepository userPushTokenRepository) {
    this.userPushTokenRepository = userPushTokenRepository;
  }

  /**
   * Expo Push Token이 있으면 잠금 후 갱신하고, 없으면 새로 저장한다.
   *
   * @param userProfileId 인증된 사용자 프로필 ID
   * @param request Expo Push Token 상태 변경 요청
   */
  @Transactional
  public void registerOrClaim(Long userProfileId, ExpoPushTokenUpdateRequest request) {
    userPushTokenRepository
        .findByExpoPushTokenForUpdate(request.expoPushToken())
        .ifPresentOrElse(
            token -> token.claim(userProfileId, request.platform()),
            () ->
                userPushTokenRepository.saveAndFlush(
                    UserPushToken.register(
                        userProfileId, request.platform(), request.expoPushToken())));
  }

  /**
   * 동시 등록으로 먼저 생성된 Expo Push Token을 잠금 후 갱신한다.
   *
   * @param userProfileId 인증된 사용자 프로필 ID
   * @param request Expo Push Token 상태 변경 요청
   * @return 먼저 생성된 Token을 찾았으면 {@code true}
   */
  @Transactional
  public boolean claimExisting(Long userProfileId, ExpoPushTokenUpdateRequest request) {
    return userPushTokenRepository
        .findByExpoPushTokenForUpdate(request.expoPushToken())
        .map(
            token -> {
              token.claim(userProfileId, request.platform());
              return true;
            })
        .orElse(false);
  }

  /**
   * 현재 사용자가 소유한 Expo Push Token을 잠금 후 비활성화한다.
   *
   * @param userProfileId 인증된 사용자 프로필 ID
   * @param expoPushToken Expo Push Token
   */
  @Transactional
  public void revokeOwnedToken(Long userProfileId, String expoPushToken) {
    userPushTokenRepository
        .findOwnedTokenForUpdate(userProfileId, expoPushToken)
        .ifPresent(UserPushToken::revoke);
  }
}
