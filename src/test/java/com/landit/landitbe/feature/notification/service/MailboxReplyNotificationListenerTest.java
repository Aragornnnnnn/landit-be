// 편지함 답장 알림 발행 실패가 답장 처리로 전파되지 않는지 검증한다.

package com.landit.landitbe.feature.notification.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.landit.landitbe.feature.mailbox.service.MailboxReplyCreatedEvent;
import com.landit.landitbe.feature.notification.client.PushNotificationException;
import com.landit.landitbe.feature.notification.messaging.PushQueuePublisher;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 편지함 답장 알림 발행 실패가 답장 처리로 전파되지 않는지 검증한다. */
@ExtendWith(MockitoExtension.class)
class MailboxReplyNotificationListenerTest {

  @Mock private PushQueuePublisher pushQueuePublisher;

  @InjectMocks private MailboxReplyNotificationListener listener;

  @Test
  void isolatesPushQueuePublicationFailure() {
    doThrow(new PushNotificationException("SQS unavailable"))
        .when(pushQueuePublisher)
        .publishMailboxReply(any());
    MailboxReplyCreatedEvent event = new MailboxReplyCreatedEvent(10L, List.of(1L), "답변 제목");

    assertThatCode(() -> listener.publish(event)).doesNotThrowAnyException();
  }
}
