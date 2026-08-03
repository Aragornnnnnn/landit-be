// 인증 사용자의 Expo Push Token 등록과 비활성화를 담당한다.

package com.landit.landitbe.feature.notification.service;

import com.landit.landitbe.feature.notification.dto.ExpoPushTokenUpdateRequest;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/** 인증 사용자의 Expo Push Token 등록과 비활성화를 담당한다. */
@Service
public class ExpoPushTokenService {

  private final ExpoPushTokenPersistenceService expoPushTokenPersistenceService;

  /**
   * Expo Push Token 영속화 Service를 주입받는다.
   *
   * @param expoPushTokenPersistenceService Expo Push Token 영속화 Service
   */
  public ExpoPushTokenService(ExpoPushTokenPersistenceService expoPushTokenPersistenceService) {
    this.expoPushTokenPersistenceService = expoPushTokenPersistenceService;
  }

  /**
   * 요청한 활성 상태에 따라 Expo Push Token을 등록·갱신하거나 비활성화한다.
   *
   * @param userProfileId 인증된 사용자 프로필 ID
   * @param request Expo Push Token 상태 변경 요청
   */
  public void update(Long userProfileId, ExpoPushTokenUpdateRequest request) {
    if (request.enabled()) {
      registerOrClaim(userProfileId, request);
      return;
    }
    expoPushTokenPersistenceService.revokeOwnedToken(userProfileId, request.expoPushToken());
  }

  // 토큰 등록 충돌 시 기존 토큰의 소유권을 현재 사용자에게 이전한다.
  private void registerOrClaim(Long userProfileId, ExpoPushTokenUpdateRequest request) {
    try {
      expoPushTokenPersistenceService.registerOrClaim(userProfileId, request);
    } catch (DataIntegrityViolationException exception) {
      if (!expoPushTokenPersistenceService.claimExisting(userProfileId, request)) {
        throw new ApiException(ErrorCode.CONFLICT);
      }
    }
  }
}
