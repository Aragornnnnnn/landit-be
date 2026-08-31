// 기존 Expo Push Token을 발송 대상으로 조회하고 무효화하는 계약을 검증한다.

package com.landit.landitbe.feature.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.notification.domain.UserPushToken;
import com.landit.landitbe.feature.notification.domain.UserPushTokenStatus;
import com.landit.landitbe.feature.notification.repository.UserPushTokenRepository;
import com.landit.landitbe.shared.domain.AppPlatform;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** 기존 Expo Push Token을 발송 대상으로 조회하고 무효화하는 계약을 검증한다. */
@ExtendWith(MockitoExtension.class)
class UserPushTokenDeliveryServiceTest {

  @Mock private UserPushTokenRepository userPushTokenRepository;

  @InjectMocks private UserPushTokenDeliveryService userPushTokenDeliveryService;

  /** 활성 Token이 요청한 사용자의 소유일 때만 발송 대상으로 반환한다. */
  @Test
  void findsActiveOwnedTokenForDelivery() {
    UserPushToken token = token(11L, 7L, "ExponentPushToken[active-owner]");
    when(userPushTokenRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(token));

    Optional<UserPushTokenDeliveryTarget> target =
        userPushTokenDeliveryService.findLockedSendableDeliveryTarget(11L, 7L);

    assertThat(target)
        .contains(new UserPushTokenDeliveryTarget(11L, "ExponentPushToken[active-owner]"));
  }

  /** Expo가 사용할 수 없다고 응답한 현재 Token을 발송 대상에서 제외한다. */
  @Test
  void revokesCurrentTokenOwner() {
    UserPushToken token = token(12L, 8L, "ExponentPushToken[unregistered]");
    when(userPushTokenRepository.findByExpoPushTokenForUpdate(token.getExpoPushToken()))
        .thenReturn(Optional.of(token));

    userPushTokenDeliveryService.revokeCurrentTokenOwner(token.getExpoPushToken());

    assertThat(token.getStatus()).isEqualTo(UserPushTokenStatus.REVOKED);
  }

  /** 여러 사용자의 활성 Token ID를 사용자별로 묶어 반환한다. */
  @Test
  void groupsActiveTokenIdsByUser() {
    UserPushToken first = token(21L, 3L, "ExponentPushToken[first]");
    UserPushToken second = token(22L, 3L, "ExponentPushToken[second]");
    UserPushToken third = token(23L, 4L, "ExponentPushToken[third]");
    when(userPushTokenRepository.findAllByUserProfileIdInAndStatusOrderByUserProfileIdAscIdAsc(
            List.of(3L, 4L), UserPushTokenStatus.ACTIVE))
        .thenReturn(List.of(first, second, third));

    Map<Long, List<Long>> result =
        userPushTokenDeliveryService.findSendableTokenIdsByUserProfileIds(List.of(3L, 4L));

    assertThat(result).containsEntry(3L, List.of(21L, 22L)).containsEntry(4L, List.of(23L));
  }

  /** 식별자를 포함한 활성 Token 테스트 데이터를 생성한다. */
  private UserPushToken token(Long id, Long userProfileId, String expoPushToken) {
    UserPushToken token = UserPushToken.register(userProfileId, AppPlatform.IOS, expoPushToken);
    ReflectionTestUtils.setField(token, "id", id);
    return token;
  }
}
