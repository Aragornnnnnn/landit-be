// 푸시 등록이 로그아웃과 같은 프로필 우선 잠금 순서를 지키는지 검증한다.

package com.landit.landitbe.feature.notification.token.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;

import com.landit.landitbe.feature.notification.token.dto.ExpoPushTokenUpdateRequest;
import com.landit.landitbe.feature.notification.token.dto.PushDeviceUpdateRequest;
import com.landit.landitbe.feature.notification.token.repository.UserPushTokenRepository;
import com.landit.landitbe.feature.profile.preference.service.ProfilePreferenceService;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import com.landit.landitbe.shared.domain.AppPlatform;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.CannotAcquireLockException;

/** 로그아웃이 프로필을 점유한 동안 등록 요청이 Token을 선점하지 않도록 검증한다. */
@ExtendWith(MockitoExtension.class)
class PushTokenLockOrderTest {
  @Mock private UserPushTokenRepository tokens;
  @Mock private ProfilePreferenceService preferences;
  @Mock private UserProfileService profiles;
  @Mock private EntityManager entityManager;

  @Test
  void installationSyncDoesNotLockTokensWhileWaitingForProfile() {
    CannotAcquireLockException blocked = blockProfile();
    PushDevicePersistenceService service =
        new PushDevicePersistenceService(tokens, preferences, entityManager, profiles);

    assertThatThrownBy(
            () ->
                service.update(
                    1L,
                    UUID.randomUUID(),
                    new PushDeviceUpdateRequest(AppPlatform.IOS, "ExpoPushToken[lock]", true)))
        .isSameAs(blocked);
    verifyNoInteractions(tokens, preferences, entityManager);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void legacyRegistrationDoesNotLockTokensWhileWaitingForProfile(boolean retry) {
    CannotAcquireLockException blocked = blockProfile();
    ExpoPushTokenPersistenceService service =
        new ExpoPushTokenPersistenceService(tokens, preferences, profiles);
    ExpoPushTokenUpdateRequest request =
        new ExpoPushTokenUpdateRequest(AppPlatform.IOS, "ExpoPushToken[lock]", true);

    assertThatThrownBy(
            () -> {
              if (retry) {
                service.claimExisting(1L, request);
              } else {
                service.registerOrClaim(1L, request);
              }
            })
        .isSameAs(blocked);
    verifyNoInteractions(tokens, preferences);
  }

  private CannotAcquireLockException blockProfile() {
    CannotAcquireLockException blocked = new CannotAcquireLockException("profile is locked");
    doThrow(blocked).when(profiles).requireActiveForUpdate(1L);
    return blocked;
  }
}
