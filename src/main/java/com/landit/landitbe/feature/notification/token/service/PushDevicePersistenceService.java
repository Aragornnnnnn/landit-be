// 설치 식별자로 현재 로그인 계정과 Expo Token을 원자적으로 연결한다.

package com.landit.landitbe.feature.notification.token.service;

import com.landit.landitbe.feature.notification.token.domain.UserPushToken;
import com.landit.landitbe.feature.notification.token.dto.PushDeviceUpdateRequest;
import com.landit.landitbe.feature.notification.token.repository.UserPushTokenRepository;
import com.landit.landitbe.feature.profile.preference.service.ProfilePreferenceService;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 설치 식별자로 현재 로그인 계정과 Expo Token을 원자적으로 연결한다. */
@Service
@RequiredArgsConstructor
public class PushDevicePersistenceService {
  private final UserPushTokenRepository tokens;
  private final ProfilePreferenceService profilePreferences;
  private final EntityManager entityManager;

  /**
   * 현재 인증 계정의 설치 푸시 수신 상태를 갱신한다.
   *
   * @param userProfileId 인증된 사용자 ID
   * @param installationId 앱 설치 UUID
   * @param request 현재 설치의 푸시 상태
   */
  @Transactional
  public void update(Long userProfileId, UUID installationId, PushDeviceUpdateRequest request) {
    Optional<UserPushToken> installation = tokens.findByInstallationIdForUpdate(installationId);
    if (!request.pushEnabled()) {
      installation.ifPresent(token -> token.bindDisabled(userProfileId, request.platform()));
      return;
    }
    Optional<UserPushToken> matchingToken =
        tokens.findByExpoPushTokenForUpdate(request.expoPushToken());
    UserPushToken target = matchingToken.or(() -> installation).orElse(null);
    if (installation.isPresent() && installation.get() != target) {
      installation.get().detachInstallation();
    }
    entityManager.flush();
    if (target == null) {
      target =
          tokens.save(
              UserPushToken.register(userProfileId, request.platform(), request.expoPushToken()));
    }
    target.bind(installationId, userProfileId, request.platform(), request.expoPushToken());
    tokens.flush();
    profilePreferences.grantPushPermission(userProfileId);
  }

  /**
   * 로그아웃한 계정이 여전히 해당 설치를 소유할 때만 발송을 중지한다.
   *
   * @param userProfileId 로그아웃 계정 ID
   * @param installationId 앱 설치 UUID
   */
  @Transactional
  public void revokeIfOwned(Long userProfileId, UUID installationId) {
    tokens
        .findByInstallationIdForUpdate(installationId)
        .filter(token -> token.getUserProfileId().equals(userProfileId))
        .ifPresent(UserPushToken::revoke);
  }
}
