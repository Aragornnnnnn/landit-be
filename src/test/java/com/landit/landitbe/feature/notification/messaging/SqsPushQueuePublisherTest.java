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
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.LongStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.BatchResultErrorEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResultEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Receipt 확인 메시지의 SQS 본문과 15분 지연 발행 계약을 검증한다. */
@ExtendWith(MockitoExtension.class)
class SqsPushQueuePublisherTest {

  @Mock private SqsAsyncClient sqsAsyncClient;

  private final JsonMapper jsonMapper = JsonMapper.builder().build();
  private NotificationProperties properties;
  private SqsPushQueuePublisher publisher;

  /** 각 테스트에서 발행 가능한 Push Queue 설정과 Publisher를 준비한다. */
  @BeforeEach
  void setUp() {
    properties =
        new NotificationProperties(
            "https://exp.host",
            null,
            Duration.ofSeconds(1),
            Duration.ofSeconds(2),
            "https://sqs.ap-northeast-2.amazonaws.com/123/push",
            900);
    publisher = new SqsPushQueuePublisher(sqsAsyncClient, jsonMapper, properties);
  }

  /** 같은 Push Queue에 Receipt 확인 payload와 900초 지연을 지정해 발행한다. */
  @Test
  void publishesDelayedReceiptCheckMessage() throws Exception {
    stubSqsSendMessage();

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

  /** SQS 응답이 지연되면 설정된 요청 제한 시간 뒤 발행 실패로 처리한다. */
  @Test
  void failsWhenSqsSendExceedsRequestTimeout() {
    NotificationProperties properties =
        new NotificationProperties(
            "https://exp.host",
            null,
            Duration.ofSeconds(1),
            Duration.ofMillis(1),
            "https://sqs.ap-northeast-2.amazonaws.com/123/push",
            900);
    when(sqsAsyncClient.sendMessage(any(SendMessageRequest.class)))
        .thenReturn(new CompletableFuture<>());
    SqsPushQueuePublisher publisher =
        new SqsPushQueuePublisher(sqsAsyncClient, JsonMapper.builder().build(), properties);

    assertThatThrownBy(() -> publisher.scheduleReceiptCheck(10L, 1))
        .isInstanceOf(PushNotificationException.class)
        .hasMessage("Push Receipt 확인 메시지 발행에 실패했습니다.");
  }

  /** SQS 비동기 발행 성공 응답을 준비한다. */
  private void stubSqsSendMessage() {
    when(sqsAsyncClient.sendMessage(any(SendMessageRequest.class)))
        .thenReturn(
            CompletableFuture.completedFuture(
                SendMessageResponse.builder().messageId("sqs-message-id").build()));
  }

  /** 21개 예약은 10·10·1로 분리하고 각 항목의 지연·payload를 유지한다. */
  @Test
  void batchesReceiptsWithEntryDelayAndPayload() {
    when(sqsAsyncClient.sendMessageBatch(any(SendMessageBatchRequest.class)))
        .thenAnswer(
            call -> {
              SendMessageBatchRequest request = call.getArgument(0);
              return CompletableFuture.completedFuture(
                  SendMessageBatchResponse.builder()
                      .successful(
                          request.entries().stream()
                              .map(
                                  e ->
                                      SendMessageBatchResultEntry.builder()
                                          .id(e.id())
                                          .messageId("sqs-" + e.id())
                                          .build())
                              .toList())
                      .build());
            });
    publisher.scheduleReceiptChecks(LongStream.rangeClosed(1, 21).boxed().toList(), 2);
    ArgumentCaptor<SendMessageBatchRequest> captor =
        ArgumentCaptor.forClass(SendMessageBatchRequest.class);
    org.mockito.Mockito.verify(sqsAsyncClient, org.mockito.Mockito.times(3))
        .sendMessageBatch(captor.capture());
    assertThat(captor.getAllValues())
        .extracting(r -> r.entries().size())
        .containsExactly(10, 10, 1);
    assertThat(captor.getAllValues().stream().flatMap(r -> r.entries().stream()).toList())
        .allSatisfy(
            e -> {
              assertThat(e.delaySeconds()).isEqualTo(900);
              JsonNode body = jsonMapper.readTree(e.messageBody());
              assertThat(body.get("payload").get("receiptAttempt").asInt()).isEqualTo(2);
            });
    assertThat(
            captor.getAllValues().stream()
                .flatMap(r -> r.entries().stream())
                .map(
                    e ->
                        jsonMapper
                            .readTree(e.messageBody())
                            .get("payload")
                            .get("pushDeliveryId")
                            .asLong())
                .toList())
        .containsExactlyElementsOf(LongStream.rangeClosed(1, 21).boxed().toList());
  }

  /** HTTP 성공 응답의 개별 실패도 예외로 처리해 접수 이력 기반 재예약을 유도한다. */
  @Test
  void rejectsPartialFailure() {
    when(sqsAsyncClient.sendMessageBatch(any(SendMessageBatchRequest.class)))
        .thenReturn(
            CompletableFuture.completedFuture(
                SendMessageBatchResponse.builder()
                    .successful(
                        SendMessageBatchResultEntry.builder().id("0").messageId("ok").build())
                    .failed(
                        BatchResultErrorEntry.builder()
                            .id("1")
                            .code("InternalError")
                            .senderFault(false)
                            .build())
                    .build()));
    assertThatThrownBy(() -> publisher.scheduleReceiptChecks(List.of(1L, 2L), 1))
        .isInstanceOf(PushNotificationException.class);
  }

  /** 성공 목록에서 누락된 항목을 성공으로 간주하지 않는다. */
  @Test
  void rejectsMissingBatchResult() {
    when(sqsAsyncClient.sendMessageBatch(any(SendMessageBatchRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(SendMessageBatchResponse.builder().build()));
    assertThatThrownBy(() -> publisher.scheduleReceiptChecks(List.of(1L), 1))
        .isInstanceOf(PushNotificationException.class);
  }
}
