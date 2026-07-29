// 인증 사용자의 Expo Push Token 등록과 비활성화를 담당한다.

package com.landit.landitbe.feature.notification.service;

import com.landit.landitbe.feature.notification.domain.UserPushToken;
import com.landit.landitbe.feature.notification.dto.ExpoPushTokenUpdateRequest;
import com.landit.landitbe.feature.notification.repository.UserPushTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 인증 사용자의 Expo Push Token 등록과 비활성화를 담당한다. */
@Service
public class ExpoPushTokenService {

  private final UserPushTokenRepository userPushTokenRepository;

  /**
   * 사용자 Expo Push Token Repository를 주입받는다.
   *
   * @param userPushTokenRepository 사용자 Expo Push Token Repository
   */
  public ExpoPushTokenService(UserPushTokenRepository userPushTokenRepository) {
    this.userPushTokenRepository = userPushTokenRepository;
  }

  /**
   * 요청한 활성 상태에 따라 Expo Push Token을 등록·갱신하거나 비활성화한다.
   *
   * @param userProfileId 인증된 사용자 프로필 ID
   * @param request Expo Push Token 상태 변경 요청
   */
  @Transactional
  public void update(Long userProfileId, ExpoPushTokenUpdateRequest request) {
    if (request.enabled()) {
      upsert(userProfileId, request);
      return;
    }
    revokeOwnedToken(userProfileId, request.expoPushToken());
  }

  private void upsert(Long userProfileId, ExpoPushTokenUpdateRequest request) {
    userPushTokenRepository
        .findByExpoPushToken(request.expoPushToken())
        .ifPresentOrElse(
            token -> token.claim(userProfileId, request.platform()),
            () ->
                userPushTokenRepository.save(
                    UserPushToken.register(
                        userProfileId, request.platform(), request.expoPushToken())));
  }

  private void revokeOwnedToken(Long userProfileId, String expoPushToken) {
    userPushTokenRepository
        .findByUserProfileIdAndExpoPushToken(userProfileId, expoPushToken)
        .ifPresent(UserPushToken::revoke);
  }
}
