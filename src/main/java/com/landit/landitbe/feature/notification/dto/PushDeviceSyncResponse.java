// 동기화된 푸시 설치 상태를 Token 원문 없이 반환한다.

package com.landit.landitbe.feature.notification.dto;

import com.landit.landitbe.feature.notification.domain.PushDevice;
import com.landit.landitbe.feature.notification.domain.PushTokenStatus;
import com.landit.landitbe.shared.domain.AppPlatform;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 동기화된 푸시 설치 상태를 Token 원문 없이 반환한다.
 *
 * @param installationId 앱 설치 ID
 * @param platform 기기 플랫폼
 * @param pushEnabled Landit 푸시 알림 수신 여부
 * @param pushTokenRegistered Expo Push Token 저장 여부
 * @param updatedAt 마지막 동기화 시각
 */
@Schema(description = "푸시 알림 설치 상태 동기화 결과")
public record PushDeviceSyncResponse(
    @Schema(description = "앱 설치 ID") UUID installationId,
    @Schema(description = "기기 플랫폼") AppPlatform platform,
    @Schema(description = "Landit 푸시 알림 수신 여부") boolean pushEnabled,
    @Schema(description = "발송 가능한 Expo Push Token 등록 여부") boolean pushTokenRegistered,
    @Schema(description = "마지막 동기화 시각") LocalDateTime updatedAt) {

  /**
   * Push Device에서 Token 원문을 제외한 응답을 생성한다.
   *
   * @param pushDevice 동기화된 Push Device
   * @return 클라이언트에 반환할 설치 상태
   */
  public static PushDeviceSyncResponse from(PushDevice pushDevice) {
    return new PushDeviceSyncResponse(
        pushDevice.getInstallationId(),
        pushDevice.getPlatform(),
        pushDevice.isPushEnabled(),
        pushDevice.getExpoPushToken() != null
            && pushDevice.getTokenStatus() == PushTokenStatus.ACTIVE,
        pushDevice.getUpdatedAt());
  }
}
