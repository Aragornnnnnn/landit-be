// 인증된 사용자의 Expo Push Token 상태 변경 요청을 처리한다.

package com.landit.landitbe.feature.notification.token;

import com.landit.landitbe.feature.notification.token.docs.ExpoPushTokenControllerDocs;
import com.landit.landitbe.feature.notification.token.dto.ExpoPushTokenUpdateRequest;
import com.landit.landitbe.feature.notification.token.service.ExpoPushTokenService;
import com.landit.landitbe.shared.response.ApiResponse;
import com.landit.landitbe.shared.security.AuthUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 인증된 사용자의 Expo Push Token 상태 변경 요청을 처리한다. */
@RestController
public class ExpoPushTokenController implements ExpoPushTokenControllerDocs {

  private final ExpoPushTokenService expoPushTokenService;

  /**
   * Expo Push Token Service를 주입받는다.
   *
   * @param expoPushTokenService Expo Push Token Service
   */
  public ExpoPushTokenController(ExpoPushTokenService expoPushTokenService) {
    this.expoPushTokenService = expoPushTokenService;
  }

  /** {@inheritDoc} */
  @Override
  @PutMapping("/api/v1/me/expo-push-token")
  public ApiResponse<Void> update(
      @AuthenticationPrincipal AuthUserPrincipal principal,
      @Valid @RequestBody ExpoPushTokenUpdateRequest request) {
    expoPushTokenService.update(principal.userId(), request);
    return ApiResponse.success(null);
  }
}
