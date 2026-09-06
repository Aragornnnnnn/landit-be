// Push 전용 SQS 메시지를 동시성 2로 소비해 Handler에 전달한다.

package com.landit.landitbe.feature.notification.messaging;

import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.listener.Visibility;
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
   * @param visibility 현재 SQS 메시지의 visibility를 연장하는 객체
   */
  @SqsListener(
      value = "${landit.notification.queue-url}",
      maxConcurrentMessages = "2",
      maxMessagesPerPoll = "2",
      acknowledgementMode = "ON_SUCCESS")
  public void consume(PushQueueMessage message, Visibility visibility) {
    pushQueueMessageHandler.handle(message, () -> visibility.changeTo(300));
  }

  /**
   * 단위 테스트와 직접 호출에서 SQS visibility 연장 없이 메시지를 처리한다.
   *
   * @param message Push Queue 메시지
   */
  public void consume(PushQueueMessage message) {
    pushQueueMessageHandler.handle(message);
  }
}
