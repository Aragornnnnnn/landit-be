// EventBridge 예약의 한국 시간·SQS 메시지 계약과 중복 등록을 검증한다.

package com.landit.landitbe.feature.notification.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.landit.landitbe.shared.exception.ApiException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.scheduler.SchedulerClient;
import software.amazon.awssdk.services.scheduler.model.ConflictException;
import software.amazon.awssdk.services.scheduler.model.CreateScheduleRequest;
import software.amazon.awssdk.services.scheduler.model.DeleteScheduleRequest;
import software.amazon.awssdk.services.scheduler.model.GetScheduleRequest;
import software.amazon.awssdk.services.scheduler.model.GetScheduleResponse;
import software.amazon.awssdk.services.scheduler.model.ResourceNotFoundException;
import tools.jackson.databind.json.JsonMapper;

/** 실제 AWS 호출 없이 SDK 요청과 응답 유실 후 재시도 계약을 확인한다. */
class EventBridgeAdminPushSchedulerTest {
  private final SchedulerClient client = mock(SchedulerClient.class, CALLS_REAL_METHODS);
  private final JsonMapper mapper = JsonMapper.builder().build();

  private final EventBridgeAdminPushScheduler scheduler =
      new EventBridgeAdminPushScheduler(
          client,
          mapper,
          "test",
          "arn:aws:sqs:ap-northeast-2:123456789012:push",
          "arn:aws:iam::123456789012:role/scheduler",
          "arn:aws:sqs:ap-northeast-2:123456789012:push-dlq");

  @org.junit.jupiter.api.BeforeEach
  void stubSdkOperationsWhileKeepingConsumerBuilders() {
    org.mockito.Mockito.doReturn(null)
        .when(client)
        .createSchedule(any(CreateScheduleRequest.class));
    org.mockito.Mockito.doReturn(null).when(client).getSchedule(any(GetScheduleRequest.class));
    org.mockito.Mockito.doReturn(null)
        .when(client)
        .deleteSchedule(any(DeleteScheduleRequest.class));
  }

  @Test
  void schedulesExistingQueuePayloadAtKoreanTime() {
    UUID id = UUID.randomUUID();
    Instant time = Instant.parse("2026-09-10T10:00:00Z");
    scheduler.schedule(id, time);
    var captor = ArgumentCaptor.forClass(CreateScheduleRequest.class);
    verify(client).createSchedule(captor.capture());
    CreateScheduleRequest request = captor.getValue();
    assertThat(request.name()).isEqualTo("admin-push-" + id);
    assertThat(request.clientToken()).isEqualTo(id.toString());
    assertThat(request.scheduleExpression()).isEqualTo("at(2026-09-10T19:00:00)");
    assertThat(request.scheduleExpressionTimezone()).isEqualTo("Asia/Seoul");
    assertThat(request.actionAfterCompletionAsString()).isEqualTo("DELETE");
    assertThat(request.flexibleTimeWindow().modeAsString()).isEqualTo("OFF");
    assertThat(request.target().deadLetterConfig().arn())
        .isEqualTo("arn:aws:sqs:ap-northeast-2:123456789012:push-dlq");
    PushQueueMessage message = mapper.readValue(request.target().input(), PushQueueMessage.class);
    assertThat(message.payload().campaignId()).isEqualTo(id);
    assertThat(message.messageType()).isEqualTo(PushQueueMessage.ADMIN_PUSH_CAMPAIGN);
    assertThat(message.occurredAt()).isEqualTo(time);
  }

  @Test
  void acceptsMatchingExistingScheduleButRejectsDifferentDestination() {
    UUID id = UUID.randomUUID();
    Instant time = Instant.parse("2026-09-10T10:00:00Z");
    scheduler.schedule(id, time);
    var captor = ArgumentCaptor.forClass(CreateScheduleRequest.class);
    verify(client).createSchedule(captor.capture());
    CreateScheduleRequest request = captor.getValue();
    doThrow(ConflictException.builder().build())
        .when(client)
        .createSchedule(any(CreateScheduleRequest.class));
    GetScheduleResponse existing =
        GetScheduleResponse.builder()
            .scheduleExpression(request.scheduleExpression())
            .scheduleExpressionTimezone("Asia/Seoul")
            .target(request.target())
            .build();
    doReturn(existing).when(client).getSchedule(any(GetScheduleRequest.class));
    assertThatCode(() -> scheduler.schedule(id, time)).doesNotThrowAnyException();
    doReturn(
            existing.toBuilder()
                .target(
                    request.target().toBuilder()
                        .deadLetterConfig(dlq -> dlq.arn("different-dlq"))
                        .build())
                .build())
        .when(client)
        .getSchedule(any(GetScheduleRequest.class));
    assertThatThrownBy(() -> scheduler.schedule(id, time)).isInstanceOf(ApiException.class);

    doReturn(
            existing.toBuilder()
                .target(request.target().toBuilder().arn("different").build())
                .build())
        .when(client)
        .getSchedule(any(GetScheduleRequest.class));
    assertThatThrownBy(() -> scheduler.schedule(id, time)).isInstanceOf(ApiException.class);
  }

  @Test
  void repeatedCancellationSucceedsAfterAutomaticDeletion() {
    doThrow(ResourceNotFoundException.builder().build())
        .when(client)
        .deleteSchedule(any(DeleteScheduleRequest.class));
    assertThatCode(() -> scheduler.cancel(UUID.randomUUID())).doesNotThrowAnyException();
  }
}
