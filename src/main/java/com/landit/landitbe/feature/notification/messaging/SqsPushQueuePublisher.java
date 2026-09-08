// 편지함 답장과 Expo Receipt 확인 메시지를 Push 전용 SQS에 발행한다.

package com.landit.landitbe.feature.notification.messaging;

import com.landit.landitbe.config.notification.NotificationProperties;
import com.landit.landitbe.feature.notification.client.PushNotificationException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/** 편지함 답장과 Expo Receipt 확인 메시지를 Push 전용 SQS에 발행한다. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "landit.notification",
    name = "consumer-enabled",
    havingValue = "true")
public class SqsPushQueuePublisher implements PushQueuePublisher {

  private static final int MESSAGE_VERSION = 1;
  private static final int RECEIPT_DELAY_SECONDS = 900;

  private final SqsAsyncClient sqsAsyncClient;
  private final JsonMapper jsonMapper;
  private final NotificationProperties properties;

  /** {@inheritDoc} */
  @Override
  public void scheduleReceiptChecks(List<Long> pushDeliveryIds, int attempt) {
    if (pushDeliveryIds.isEmpty()) {
      return;
    }
    validateConfiguration();
    List<Long> ids = pushDeliveryIds.stream().distinct().toList();
    for (int offset = 0; offset < ids.size(); offset += 10) {
      sendReceiptBatch(ids.subList(offset, Math.min(offset + 10, ids.size())), attempt);
    }
  }

  private void sendReceiptBatch(List<Long> ids, int attempt) {
    try {
      List<SendMessageBatchRequestEntry> entries = new ArrayList<>();
      for (int i = 0; i < ids.size(); i++) {
        PushQueueMessage message =
            new PushQueueMessage(
                MESSAGE_VERSION,
                UUID.randomUUID().toString(),
                PushQueueMessage.PUSH_RECEIPT_CHECK,
                Instant.now(),
                PushQueuePayload.receipt(ids.get(i), attempt));
        entries.add(
            SendMessageBatchRequestEntry.builder()
                .id(Integer.toString(i))
                .delaySeconds(properties.receiptDelaySeconds())
                .messageBody(jsonMapper.writeValueAsString(message))
                .build());
      }
      SendMessageBatchResponse response =
          sqsAsyncClient
              .sendMessageBatch(
                  SendMessageBatchRequest.builder()
                      .queueUrl(properties.queueUrl())
                      .entries(entries)
                      .build())
              .orTimeout(properties.requestTimeout().toMillis(), TimeUnit.MILLISECONDS)
              .join();
      Set<String> expected =
          new HashSet<>(entries.stream().map(SendMessageBatchRequestEntry::id).toList());
      List<String> succeeded = response.successful().stream().map(result -> result.id()).toList();
      if (!response.failed().isEmpty()
          || succeeded.size() != expected.size()
          || !new HashSet<>(succeeded).equals(expected)) {
        throw new PushNotificationException("Push Receipt 묶음에 실패하거나 누락된 항목이 있습니다.");
      }
    } catch (JacksonException | CompletionException exception) {
      throw new PushNotificationException("Push Receipt 묶음 발행에 실패했습니다.", exception);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void publishMailboxReply(MailboxReplyNotificationRequest request) {
    validateConfiguration();
    PushQueueMessage message =
        new PushQueueMessage(
            MESSAGE_VERSION,
            "mailbox-reply:" + request.letterId(),
            PushQueueMessage.MAILBOX_REPLY_NOTIFICATION_BATCH,
            request.occurredAt(),
            PushQueuePayload.mailboxReply(request));
    send(message, 0, "편지함 답장 Push 메시지 발행에 실패했습니다.");
  }

  /** {@inheritDoc} */
  @Override
  public void scheduleReceiptCheck(Long pushDeliveryId, int attempt) {
    validateConfiguration();
    PushQueueMessage message =
        new PushQueueMessage(
            MESSAGE_VERSION,
            UUID.randomUUID().toString(),
            PushQueueMessage.PUSH_RECEIPT_CHECK,
            Instant.now(),
            PushQueuePayload.receipt(pushDeliveryId, attempt));
    send(message, properties.receiptDelaySeconds(), "Push Receipt 확인 메시지 발행에 실패했습니다.");
  }

  /** Push Queue 메시지를 지정된 지연 시간으로 SQS에 발행한다. */
  private void send(PushQueueMessage message, int delaySeconds, String failureMessage) {
    try {
      SendMessageRequest request =
          SendMessageRequest.builder()
              .queueUrl(properties.queueUrl())
              .delaySeconds(delaySeconds)
              .messageBody(jsonMapper.writeValueAsString(message))
              .build();
      sqsAsyncClient
          .sendMessage(request)
          .orTimeout(properties.requestTimeout().toMillis(), TimeUnit.MILLISECONDS)
          .join();
    } catch (JacksonException | CompletionException exception) {
      throw new PushNotificationException(failureMessage, exception);
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
