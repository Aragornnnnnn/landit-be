// Expo Push Token과 사용자 푸시 권한 상태를 같은 트랜잭션으로 저장한다.

package com.landit.landitbe.feature.notification.service;

import com.landit.landitbe.feature.notification.domain.UserPushToken;
import com.landit.landitbe.feature.notification.dto.ExpoPushTokenUpdateRequest;
import com.landit.landitbe.feature.notification.repository.UserPushTokenRepository;
import com.landit.landitbe.feature.profile.exception.UserProfileException;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Expo Push Token과 사용자 푸시 권한 상태를 같은 트랜잭션으로 저장한다. */
@Service
public class ExpoPushTokenPersistenceService {

  private final UserPushTokenRepository userPushTokenRepository;
  private final UserProfileService userProfileService;

  /**
   * 사용자 Expo Push Token Repository를 주입받는다.
   *
   * @param userPushTokenRepository 사용자 Expo Push Token Repository
   * @param userProfileService 사용자 프로필 Service
   */
  public ExpoPushTokenPersistenceService(
      UserPushTokenRepository userPushTokenRepository, UserProfileService userProfileService) {
    this.userPushTokenRepository = userPushTokenRepository;
    this.userProfileService = userProfileService;
  }

  /**
   * Expo Push Token을 등록·갱신하고 사용자 푸시 권한을 허용 상태로 저장한다.
   *
   * @param userProfileId 인증된 사용자 프로필 ID
   * @param request Expo Push Token 상태 변경 요청
   * @throws UserProfileException 활성 사용자 프로필이 없을 때
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
    userProfileService.grantPushPermission(userProfileId);
  }

  /**
   * 동시 등록으로 먼저 생성된 Expo Push Token을 갱신하고 사용자 푸시 권한을 허용 상태로 저장한다.
   *
   * @param userProfileId 인증된 사용자 프로필 ID
   * @param request Expo Push Token 상태 변경 요청
   * @return 먼저 생성된 Token을 찾았으면 {@code true}
   * @throws UserProfileException Token을 찾았지만 활성 사용자 프로필이 없을 때
   */
  @Transactional
  public boolean claimExisting(Long userProfileId, ExpoPushTokenUpdateRequest request) {
    return userPushTokenRepository
        .findByExpoPushTokenForUpdate(request.expoPushToken())
        .map(
            token -> {
              token.claim(userProfileId, request.platform());
              userProfileService.grantPushPermission(userProfileId);
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
