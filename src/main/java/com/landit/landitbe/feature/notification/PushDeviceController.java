// 인증된 사용자의 푸시 알림 설치 상태 동기화 요청을 처리한다.

package com.landit.landitbe.feature.notification;

import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.feature.notification.docs.PushDeviceControllerDocs;
import com.landit.landitbe.feature.notification.dto.PushDeviceSyncRequest;
import com.landit.landitbe.feature.notification.dto.PushDeviceSyncResponse;
import com.landit.landitbe.feature.notification.service.PushDeviceService;
import com.landit.landitbe.shared.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 인증된 사용자의 푸시 알림 설치 상태 동기화 요청을 처리한다. */
@RestController
@RequiredArgsConstructor
public class PushDeviceController implements PushDeviceControllerDocs {

  private final PushDeviceService pushDeviceService;

  /** {@inheritDoc} */
  @Override
  @PutMapping("/api/v1/me/push-devices/{installationId}")
  public ResponseEntity<ApiResponse<PushDeviceSyncResponse>> synchronize(
      @AuthenticationPrincipal AuthUserPrincipal principal,
      @PathVariable UUID installationId,
      @Valid @RequestBody PushDeviceSyncRequest request) {
    PushDeviceSyncResponse response =
        pushDeviceService.synchronize(principal.userId(), installationId, request);
    return ResponseEntity.ok(ApiResponse.success(response));
  }
}
