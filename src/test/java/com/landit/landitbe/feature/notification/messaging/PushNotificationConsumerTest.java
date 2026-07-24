// Push SQS Listener의 위임, 동시성, 성공 시 삭제 계약을 검증한다.

package com.landit.landitbe.feature.notification.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import io.awspring.cloud.sqs.annotation.SqsListener;
import java.lang.reflect.Method;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Push SQS Listener의 위임, 동시성, 성공 시 삭제 계약을 검증한다. */
class PushNotificationConsumerTest {

  /** 수신한 Push Queue 메시지를 Handler에 그대로 위임한다. */
  @Test
  void delegatesMessageToHandler() {
    PushQueueMessageHandler handler = org.mockito.Mockito.mock(PushQueueMessageHandler.class);
    PushNotificationConsumer consumer = new PushNotificationConsumer(handler);
    PushQueueMessage message =
        new PushQueueMessage(
            1,
            "message-id",
            "REVIEW_REMINDER_BATCH",
            Instant.parse("2026-07-24T11:00:00Z"),
            new PushQueuePayload(null, null));

    consumer.consume(message);

    verify(handler).handle(message);
  }

  /** Listener는 Push Queue URL, 동시성 2, ON_SUCCESS acknowledgement를 사용한다. */
  @Test
  void configuresListenerConcurrencyAndAcknowledgement() throws Exception {
    Method consumeMethod =
        PushNotificationConsumer.class.getDeclaredMethod("consume", PushQueueMessage.class);
    SqsListener listener = consumeMethod.getAnnotation(SqsListener.class);

    assertThat(listener.value()).containsExactly("${landit.notification.queue-url}");
    assertThat(listener.maxConcurrentMessages()).isEqualTo("2");
    assertThat(listener.maxMessagesPerPoll()).isEqualTo("2");
    assertThat(listener.acknowledgementMode()).isEqualTo("ON_SUCCESS");
  }
}
