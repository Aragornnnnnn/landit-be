// Dev에서 인증 사용자에게 일반 푸시를 즉시 발송하는 테스트 API를 제공한다.

package com.landit.landitbe.feature.notification;

import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.feature.notification.domain.NotificationType;
import com.landit.landitbe.feature.notification.service.NotificationDispatchService;
import com.landit.landitbe.feature.notification.service.SendPushNotificationCommand;
import com.landit.landitbe.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** Dev에서 인증 사용자에게 일반 푸시를 즉시 발송하는 테스트 API를 제공한다. */
@Hidden
@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "landit.notification",
    name = "test-api-enabled",
    havingValue = "true")
public class PushNotificationTestController {

  private static final String TITLE = "Landit 알림 테스트";
  private static final String BODY = "푸시 알림이 정상적으로 도착했어요.";
  private static final String DEEP_LINK = "/home";

  private final NotificationDispatchService notificationDispatchService;

  /**
   * 인증 사용자에게 보낼 일반 테스트 알림을 즉시 발송한다.
   *
   * @param principal 인증 사용자
   * @return 테스트 메시지 발송 수락 응답
   */
  @PostMapping("/api/v1/internal/test/push")
  public ResponseEntity<ApiResponse<Void>> publishTestNotification(
      @AuthenticationPrincipal AuthUserPrincipal principal) {
    notificationDispatchService.send(
        new SendPushNotificationCommand(
            UUID.randomUUID().toString(),
            principal.userId(),
            NotificationType.TEST_NOTIFICATION,
            TITLE,
            BODY,
            DEEP_LINK));
    return ApiResponse.success(HttpStatus.ACCEPTED, null);
  }
}
