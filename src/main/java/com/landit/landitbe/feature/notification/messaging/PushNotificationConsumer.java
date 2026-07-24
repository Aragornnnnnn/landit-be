// Push 전용 SQS 메시지를 동시성 2로 소비해 Handler에 전달한다.

package com.landit.landitbe.feature.notification.messaging;

import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Push 전용 SQS 메시지를 동시성 2로 소비해 Handler에 전달한다. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "landit.notification",
    name = "consumer-enabled",
    havingValue = "true")
public class PushNotificationConsumer {

  private final PushQueueMessageHandler pushQueueMessageHandler;

  /**
   * Push Queue 메시지를 처리하고 성공한 경우에만 SQS에서 삭제되도록 반환한다.
   *
   * @param message Push Queue 메시지
   */
  @SqsListener(
      value = "${landit.notification.queue-url}",
      maxConcurrentMessages = "2",
      maxMessagesPerPoll = "2",
      acknowledgementMode = "ON_SUCCESS")
  public void consume(PushQueueMessage message) {
    pushQueueMessageHandler.handle(message);
  }
}
