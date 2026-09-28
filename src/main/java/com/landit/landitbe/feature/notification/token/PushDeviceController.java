// 인증 사용자의 앱 설치별 푸시 상태를 동기화한다.

package com.landit.landitbe.feature.notification.token;

import com.landit.landitbe.feature.notification.token.docs.PushDeviceControllerDocs;
import com.landit.landitbe.feature.notification.token.dto.PushDeviceUpdateRequest;
import com.landit.landitbe.feature.notification.token.service.PushDeviceService;
import com.landit.landitbe.shared.response.ApiResponse;
import com.landit.landitbe.shared.security.AuthUserPrincipal;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 인증 사용자의 앱 설치별 푸시 상태를 동기화한다. */
@RestController
@RequiredArgsConstructor
public class PushDeviceController implements PushDeviceControllerDocs {
  private final PushDeviceService service;

  /** {@inheritDoc} */
  @Override
  @PutMapping("/api/v1/me/push-devices/{installationId}")
  public ApiResponse<Void> update(
      @AuthenticationPrincipal AuthUserPrincipal principal,
      @PathVariable UUID installationId,
      @Valid @RequestBody PushDeviceUpdateRequest request) {
    service.update(principal.userId(), installationId, request);
    return ApiResponse.success(null);
  }
}
