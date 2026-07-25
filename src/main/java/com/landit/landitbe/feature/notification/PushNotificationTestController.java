// Dev에서 복습 리마인더 배치를 즉시 발행하는 테스트 API를 제공한다.

package com.landit.landitbe.feature.notification;

import com.landit.landitbe.feature.notification.messaging.PushQueuePublisher;
import com.landit.landitbe.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Hidden;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** Dev에서 복습 리마인더 배치를 즉시 발행하는 테스트 API를 제공한다. */
@Hidden
@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "landit.notification",
    name = "test-api-enabled",
    havingValue = "true")
public class PushNotificationTestController {

  private final PushQueuePublisher pushQueuePublisher;

  /**
   * 현재 시각 기준 복습 리마인더 배치를 Push Queue에 즉시 발행한다.
   *
   * @return 배치 메시지 발행 수락 응답
   */
  @PostMapping("/api/v1/internal/test/push/review-reminder")
  public ResponseEntity<ApiResponse<Void>> publishReviewReminderBatch() {
    pushQueuePublisher.publishReviewReminderBatch(Instant.now());
    return ApiResponse.success(HttpStatus.ACCEPTED, null);
  }
}
