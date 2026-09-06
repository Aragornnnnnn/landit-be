// 커밋된 편지함 답장을 Push Queue 알림으로 발행한다.

package com.landit.landitbe.feature.notification.service;

import com.landit.landitbe.feature.mailbox.service.MailboxReplyCreatedEvent;
import com.landit.landitbe.feature.notification.client.PushNotificationException;
import com.landit.landitbe.feature.notification.messaging.MailboxReplyNotificationRequest;
import com.landit.landitbe.feature.notification.messaging.PushQueuePublisher;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** 커밋된 편지함 답장을 Push Queue 알림으로 발행한다. */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    prefix = "landit.notification",
    name = "consumer-enabled",
    havingValue = "true")
public class MailboxReplyNotificationListener {

  private final PushQueuePublisher pushQueuePublisher;

  /**
   * 답장 트랜잭션 커밋 후 수신자 일괄 알림을 Queue에 발행한다.
   *
   * @param event 저장된 답장과 수신자 정보
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void publish(MailboxReplyCreatedEvent event) {
    try {
      pushQueuePublisher.publishMailboxReply(
          new MailboxReplyNotificationRequest(
              event.letterId(), event.userProfileIds(), event.replyTitle(), Instant.now()));
    } catch (PushNotificationException exception) {
      log.error(
          "편지함 답장 Push 메시지를 발행하지 못했습니다. workflow=mailbox_reply_notification"
              + " letterId={} recipientCount={}",
          event.letterId(),
          event.userProfileIds().size(),
          exception);
    }
  }
}
