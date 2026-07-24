// 앱 설치별 푸시 수신 상태와 Expo Token 상태 전이를 검증한다.

package com.landit.landitbe.feature.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.landit.landitbe.shared.domain.AppPlatform;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** 앱 설치별 푸시 수신 상태와 Expo Token 상태 전이를 검증한다. */
class PushDeviceTest {

  private static final UUID INSTALLATION_ID =
      UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
  private static final String EXPO_PUSH_TOKEN = "ExponentPushToken[device-token]";

  /** Token이 있는 활성 설치를 발송 가능한 상태로 생성한다. */
  @Test
  void createsEnabledDeviceWithActiveToken() {
    PushDevice device =
        PushDevice.create(1L, INSTALLATION_ID, AppPlatform.IOS, true, EXPO_PUSH_TOKEN);

    assertThat(device.getUserProfileId()).isEqualTo(1L);
    assertThat(device.getInstallationId()).isEqualTo(INSTALLATION_ID);
    assertThat(device.getPlatform()).isEqualTo(AppPlatform.IOS);
    assertThat(device.isPushEnabled()).isTrue();
    assertThat(device.getExpoPushToken()).isEqualTo(EXPO_PUSH_TOKEN);
    assertThat(device.getTokenStatus()).isEqualTo(PushTokenStatus.ACTIVE);
    assertThat(device.isSendable()).isTrue();
  }

  /** Token 없이 비활성 설치 상태를 저장할 수 있다. */
  @Test
  void createsDisabledDeviceWithoutToken() {
    PushDevice device = PushDevice.create(1L, INSTALLATION_ID, AppPlatform.ANDROID, false, null);

    assertThat(device.getExpoPushToken()).isNull();
    assertThat(device.getTokenStatus()).isNull();
    assertThat(device.isSendable()).isFalse();
  }

  /** 설치를 다른 사용자에게 연결하면 소유권을 옮기고 Token을 활성화한다. */
  @Test
  void synchronizingDeviceMovesOwnershipAndReactivatesToken() {
    PushDevice device =
        PushDevice.create(1L, INSTALLATION_ID, AppPlatform.IOS, true, EXPO_PUSH_TOKEN);
    device.invalidateToken();

    device.synchronize(2L, AppPlatform.ANDROID, true, EXPO_PUSH_TOKEN);

    assertThat(device.getUserProfileId()).isEqualTo(2L);
    assertThat(device.getPlatform()).isEqualTo(AppPlatform.ANDROID);
    assertThat(device.getTokenStatus()).isEqualTo(PushTokenStatus.ACTIVE);
    assertThat(device.isSendable()).isTrue();
  }

  /** Token이 없는 상태를 동기화하면 기존 Token 연결과 상태를 제거한다. */
  @Test
  void synchronizingNullTokenDetachesPreviousToken() {
    PushDevice device =
        PushDevice.create(1L, INSTALLATION_ID, AppPlatform.IOS, true, EXPO_PUSH_TOKEN);

    device.synchronize(1L, AppPlatform.IOS, false, null);

    assertThat(device.getExpoPushToken()).isNull();
    assertThat(device.getTokenStatus()).isNull();
    assertThat(device.isSendable()).isFalse();
  }

  /** 알림을 끈 설치는 Token을 유지하더라도 발송 대상에서 제외한다. */
  @Test
  void disabledDeviceKeepsTokenButIsNotSendable() {
    PushDevice device =
        PushDevice.create(1L, INSTALLATION_ID, AppPlatform.IOS, true, EXPO_PUSH_TOKEN);

    device.synchronize(1L, AppPlatform.IOS, false, EXPO_PUSH_TOKEN);

    assertThat(device.getExpoPushToken()).isEqualTo(EXPO_PUSH_TOKEN);
    assertThat(device.getTokenStatus()).isEqualTo(PushTokenStatus.ACTIVE);
    assertThat(device.isSendable()).isFalse();
  }

  /** Expo가 무효화한 Token은 발송 대상에서 제외한다. */
  @Test
  void invalidTokenIsNotSendable() {
    PushDevice device =
        PushDevice.create(1L, INSTALLATION_ID, AppPlatform.IOS, true, EXPO_PUSH_TOKEN);

    device.invalidateToken();

    assertThat(device.getTokenStatus()).isEqualTo(PushTokenStatus.INVALID);
    assertThat(device.isSendable()).isFalse();
  }

  /** Token 연결을 해제하면 Token 상태도 함께 제거한다. */
  @Test
  void detachingTokenClearsTokenState() {
    PushDevice device =
        PushDevice.create(1L, INSTALLATION_ID, AppPlatform.IOS, true, EXPO_PUSH_TOKEN);

    device.detachToken();

    assertThat(device.getExpoPushToken()).isNull();
    assertThat(device.getTokenStatus()).isNull();
    assertThat(device.isSendable()).isFalse();
  }
}
