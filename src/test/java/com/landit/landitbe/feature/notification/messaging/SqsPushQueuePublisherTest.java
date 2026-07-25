// Receipt 확인 메시지의 SQS 본문과 15분 지연 발행 계약을 검증한다.

package com.landit.landitbe.feature.notification.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.landit.landitbe.config.notification.NotificationProperties;
import com.landit.landitbe.feature.notification.client.PushNotificationException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Receipt 확인 메시지의 SQS 본문과 15분 지연 발행 계약을 검증한다. */
@ExtendWith(MockitoExtension.class)
class SqsPushQueuePublisherTest {

  @Mock private SqsAsyncClient sqsAsyncClient;

  /** 같은 Push Queue에 Receipt 확인 payload와 900초 지연을 지정해 발행한다. */
  @Test
  void publishesDelayedReceiptCheckMessage() throws Exception {
    JsonMapper jsonMapper = JsonMapper.builder().build();
    NotificationProperties properties =
        new NotificationProperties(
            "https://exp.host",
            null,
            Duration.ofSeconds(1),
            Duration.ofSeconds(2),
            "https://sqs.ap-northeast-2.amazonaws.com/123/push",
            900);
    when(sqsAsyncClient.sendMessage(any(SendMessageRequest.class)))
        .thenReturn(
            CompletableFuture.completedFuture(
                SendMessageResponse.builder().messageId("sqs-message-id").build()));
    SqsPushQueuePublisher publisher =
        new SqsPushQueuePublisher(sqsAsyncClient, jsonMapper, properties);

    publisher.scheduleReceiptCheck(10L, 2);

    ArgumentCaptor<SendMessageRequest> requestCaptor =
        ArgumentCaptor.forClass(SendMessageRequest.class);
    verify(sqsAsyncClient).sendMessage(requestCaptor.capture());
    SendMessageRequest request = requestCaptor.getValue();
    JsonNode body = jsonMapper.readTree(request.messageBody());
    assertThat(request.queueUrl()).isEqualTo(properties.queueUrl());
    assertThat(request.delaySeconds()).isEqualTo(900);
    assertThat(body.get("version").asInt()).isEqualTo(1);
    assertThat(body.get("messageId").asString()).isNotBlank();
    assertThat(body.get("messageType").asString()).isEqualTo("PUSH_RECEIPT_CHECK");
    assertThat(body.get("occurredAt").asString()).isNotBlank();
    assertThat(body.get("payload").get("pushDeliveryId").asLong()).isEqualTo(10L);
    assertThat(body.get("payload").get("receiptAttempt").asInt()).isEqualTo(2);
  }

  /** Receipt 확인 지연 시간은 Expo Receipt 조회 계약에 맞춰 900초만 허용한다. */
  @ParameterizedTest
  @ValueSource(ints = {0, 899, 901})
  void rejectsReceiptDelayThatDiffersFromFifteenMinutes(int receiptDelaySeconds) {

  /** 복습 리마인더 배치는 지연 없이 기존 Consumer가 처리하는 메시지 계약으로 발행한다. */
  @Test
  void publishesImmediateReviewReminderBatchMessage() throws Exception {
    JsonMapper jsonMapper = JsonMapper.builder().build();
    NotificationProperties properties =
        new NotificationProperties(
            "https://exp.host",
            null,
            Duration.ofSeconds(1),
            Duration.ofSeconds(2),
            "https://sqs.ap-northeast-2.amazonaws.com/123/push",
            900);
    when(sqsAsyncClient.sendMessage(any(SendMessageRequest.class)))
        .thenReturn(
            CompletableFuture.completedFuture(
                SendMessageResponse.builder().messageId("sqs-message-id").build()));
    SqsPushQueuePublisher publisher =
        new SqsPushQueuePublisher(sqsAsyncClient, jsonMapper, properties);
    Instant occurredAt = Instant.parse("2026-07-25T11:00:00Z");

    publisher.publishReviewReminderBatch(occurredAt);

    ArgumentCaptor<SendMessageRequest> requestCaptor =
        ArgumentCaptor.forClass(SendMessageRequest.class);
    verify(sqsAsyncClient).sendMessage(requestCaptor.capture());
    SendMessageRequest request = requestCaptor.getValue();
    JsonNode body = jsonMapper.readTree(request.messageBody());
    assertThat(request.queueUrl()).isEqualTo(properties.queueUrl());
    assertThat(request.delaySeconds()).isZero();
    assertThat(body.get("version").asInt()).isEqualTo(1);
    assertThat(body.get("messageId").asString()).isNotBlank();
    assertThat(body.get("messageType").asString()).isEqualTo("REVIEW_REMINDER_BATCH");
    assertThat(body.get("occurredAt").asString()).isEqualTo(occurredAt.toString());
    assertThat(body.get("payload").isObject()).isTrue();
  }

  /** Receipt 확인 지연 시간은 Expo Receipt 조회 계약에 맞춰 900초만 허용한다. */
  @ParameterizedTest
  @ValueSource(ints = {0, 899, 901})
  void rejectsReceiptDelayThatDiffersFromFifteenMinutes(int receiptDelaySeconds) {
    NotificationProperties properties =
        new NotificationProperties(
            "https://exp.host",
            null,
            Duration.ofSeconds(1),
            Duration.ofSeconds(2),
            "https://sqs.ap-northeast-2.amazonaws.com/123/push",
            receiptDelaySeconds);
    SqsPushQueuePublisher publisher =
        new SqsPushQueuePublisher(sqsAsyncClient, JsonMapper.builder().build(), properties);

    assertThatThrownBy(() -> publisher.scheduleReceiptCheck(10L, 1))
        .isInstanceOf(PushNotificationException.class)
        .hasMessage("Push Queue 설정이 올바르지 않습니다.");
  }
}
