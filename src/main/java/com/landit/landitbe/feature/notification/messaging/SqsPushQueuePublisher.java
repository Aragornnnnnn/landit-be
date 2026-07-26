// Expo Receipt 확인 메시지를 Push 전용 SQS에 15분 지연 발행한다.

package com.landit.landitbe.feature.notification.messaging;

import com.landit.landitbe.config.notification.NotificationProperties;
import com.landit.landitbe.feature.notification.client.PushNotificationException;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/** Expo Receipt 확인 메시지를 Push 전용 SQS에 15분 지연 발행한다. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "landit.notification",
    name = "consumer-enabled",
    havingValue = "true")
public class SqsPushQueuePublisher implements PushQueuePublisher {

  private static final int MESSAGE_VERSION = 1;
  private static final int RECEIPT_DELAY_SECONDS = 900;
  private static final String PUSH_RECEIPT_CHECK = "PUSH_RECEIPT_CHECK";

  private final SqsAsyncClient sqsAsyncClient;
  private final JsonMapper jsonMapper;
  private final NotificationProperties properties;

  /** {@inheritDoc} */
  @Override
  public void scheduleReceiptCheck(Long pushDeliveryId, int attempt) {
    validateConfiguration();
    PushQueueMessage message =
        new PushQueueMessage(
            MESSAGE_VERSION,
            UUID.randomUUID().toString(),
            PUSH_RECEIPT_CHECK,
            Instant.now(),
            new PushQueuePayload(pushDeliveryId, attempt));
    try {
      SendMessageRequest request =
          SendMessageRequest.builder()
              .queueUrl(properties.queueUrl())
              .delaySeconds(properties.receiptDelaySeconds())
              .messageBody(jsonMapper.writeValueAsString(message))
              .build();
      sqsAsyncClient.sendMessage(request).join();
    } catch (JacksonException | CompletionException exception) {
      throw new PushNotificationException("Push Receipt 확인 메시지 발행에 실패했습니다.", exception);
    }
  }

  /** SQS Queue URL과 메시지 지연 시간이 발행 가능한 값인지 검증한다. */
  private void validateConfiguration() {
    if (properties.queueUrl() == null
        || properties.queueUrl().isBlank()
        || properties.receiptDelaySeconds() != RECEIPT_DELAY_SECONDS) {
      throw new PushNotificationException("Push Queue 설정이 올바르지 않습니다.");
    }
  }
}
