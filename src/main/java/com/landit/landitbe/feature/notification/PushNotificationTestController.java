// Dev에서 인증 사용자에게 일반 푸시를 즉시 발행하는 테스트 API를 제공한다.

package com.landit.landitbe.feature.notification;

import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.feature.notification.domain.NotificationType;
import com.landit.landitbe.feature.notification.messaging.PushNotificationRequest;
import com.landit.landitbe.feature.notification.messaging.PushQueuePublisher;
import com.landit.landitbe.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Hidden;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** Dev에서 인증 사용자에게 일반 푸시를 즉시 발행하는 테스트 API를 제공한다. */
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

  private final PushQueuePublisher pushQueuePublisher;

  /**
   * 인증 사용자에게 보낼 일반 테스트 알림을 Push Queue에 즉시 발행한다.
   *
   * @param principal 인증 사용자
   * @return 테스트 메시지 발행 수락 응답
   */
  @PostMapping("/api/v1/internal/test/push")
  public ResponseEntity<ApiResponse<Void>> publishTestNotification(
      @AuthenticationPrincipal AuthUserPrincipal principal) {
    pushQueuePublisher.publishNotification(
        new PushNotificationRequest(
            UUID.randomUUID().toString(),
            principal.userId(),
            NotificationType.TEST_NOTIFICATION,
            TITLE,
            BODY,
            DEEP_LINK,
            Instant.now()));
    return ApiResponse.success(HttpStatus.ACCEPTED, null);
  }
}
