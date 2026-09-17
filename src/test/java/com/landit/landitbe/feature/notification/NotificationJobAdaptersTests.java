// 예약 전달 계약과 SES 응답의 재시도 분류를 검증한다.

package com.landit.landitbe.feature.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.landit.landitbe.config.notification.EmailProperties;
import com.landit.landitbe.config.notification.NotificationProperties;
import com.landit.landitbe.feature.notification.client.EmailSendResult.Status;
import com.landit.landitbe.feature.notification.client.ses.SesEmailSender;
import com.landit.landitbe.feature.notification.dto.NotificationJob;
import com.landit.landitbe.feature.notification.messaging.EventBridgeNotificationJobScheduler;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.scheduler.SchedulerClient;
import software.amazon.awssdk.services.scheduler.model.ConflictException;
import software.amazon.awssdk.services.scheduler.model.CreateScheduleRequest;
import software.amazon.awssdk.services.scheduler.model.GetScheduleRequest;
import software.amazon.awssdk.services.scheduler.model.GetScheduleResponse;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SendEmailResponse;
import software.amazon.awssdk.services.sesv2.model.SesV2Exception;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import tools.jackson.databind.json.JsonMapper;

/** 실제 AWS 호출 없이 일회성 예약과 중복 이메일 방지 경계를 확인한다. */
class NotificationJobAdaptersTests {
  private static final Instant NOW = Instant.parse("2026-09-16T00:00:00Z");
  private final SchedulerClient scheduler = mock(SchedulerClient.class);
  private final SqsAsyncClient sqs = mock(SqsAsyncClient.class);

  @Test
  void schedulesUtcOneTimeJobWithOnlyIdAndVerifiesConflictingReservation() {
    var adapter = schedulerAdapter();
    NotificationJob job = job(NOW.plusSeconds(86400));
    adapter.schedule(job);
    var capture = ArgumentCaptor.forClass(CreateScheduleRequest.class);
    verify(scheduler).createSchedule(capture.capture());
    var request = capture.getValue();
    assertThat(request.scheduleExpression()).isEqualTo("at(2026-09-17T00:00:00)");
    assertThat(request.scheduleExpressionTimezone()).isEqualTo("UTC");
    assertThat(request.actionAfterCompletionAsString()).isEqualTo("DELETE");
    assertThat(request.target().input())
        .contains(job.id().toString(), "NOTIFICATION_JOB")
        .doesNotContain("recipient@example.com");
    verifyNoInteractions(sqs);
    when(scheduler.createSchedule(any(CreateScheduleRequest.class)))
        .thenThrow(ConflictException.builder().build());
    when(scheduler.getSchedule(any(GetScheduleRequest.class)))
        .thenReturn(
            GetScheduleResponse.builder()
                .scheduleExpression(request.scheduleExpression())
                .scheduleExpressionTimezone("UTC")
                .target(request.target())
                .build());
    adapter.schedule(job);
    when(scheduler.getSchedule(any(GetScheduleRequest.class)))
        .thenReturn(
            GetScheduleResponse.builder()
                .scheduleExpression("at(2026-09-18T00:00:00)")
                .scheduleExpressionTimezone("UTC")
                .target(request.target())
                .build());
    assertThatThrownBy(() -> adapter.schedule(job)).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void immediateTestUsesQueueInsteadOfCreatingPastSchedule() {
    when(sqs.sendMessage(any(SendMessageRequest.class)))
        .thenReturn(
            CompletableFuture.completedFuture(
                SendMessageResponse.builder().messageId("sqs-1").build()));
    schedulerAdapter().schedule(job(NOW));
    var capture = ArgumentCaptor.forClass(SendMessageRequest.class);
    verify(sqs).sendMessage(capture.capture());
    assertThat(capture.getValue().delaySeconds()).isBetween(0, 1);
    assertThat(capture.getValue().messageBody()).doesNotContain("recipient@example.com");
    verifyNoInteractions(scheduler);
  }

  @Test
  void sesUsesConfiguredSenderAndOnlyExplicitThrottlingIsRetryable() {
    SesV2Client client = mock(SesV2Client.class);
    SesEmailSender sender =
        new SesEmailSender(
            client, new EmailProperties("Landit <no-reply@landit.im>", "develop-mail"));
    when(client.sendEmail(any(SendEmailRequest.class)))
        .thenReturn(SendEmailResponse.builder().messageId("ses-1").build());
    assertThat(
            sender
                .send("recipient@example.com", "subject", "body", "<img src=\"cid:landit-banner\">")
                .status())
        .isEqualTo(Status.ACCEPTED);
    var capture = ArgumentCaptor.forClass(SendEmailRequest.class);
    verify(client).sendEmail(capture.capture());
    assertThat(capture.getValue().replyToAddresses()).isEmpty();
    assertThat(capture.getValue().configurationSetName()).isEqualTo("develop-mail");
    assertThat(capture.getValue().fromEmailAddress()).isEqualTo("Landit <no-reply@landit.im>");
    var content = capture.getValue().content().simple();
    assertThat(content.body().text().data()).isEqualTo("body");
    assertThat(content.body().html().data()).contains("cid:landit-banner");
    assertThat(content.attachments()).hasSize(1);
    var banner = content.attachments().getFirst();
    assertThat(banner.contentId()).isEqualTo("landit-banner");
    assertThat(banner.contentDispositionAsString()).isEqualTo("INLINE");
    assertThat(banner.contentTransferEncodingAsString()).isEqualTo("BASE64");
    assertThat(banner.contentType()).isEqualTo("image/png");
    assertThat(banner.rawContent().asByteArray())
        .startsWith((byte) 0x89, (byte) 0x50, (byte) 0x4e, (byte) 0x47);
    when(client.sendEmail(any(SendEmailRequest.class)))
        .thenThrow(SesV2Exception.builder().statusCode(429).build());
    assertThat(
            sender
                .send("recipient@example.com", "subject", "body", "<img src=\"cid:landit-banner\">")
                .status())
        .isEqualTo(Status.RETRYABLE);
    when(client.sendEmail(any(SendEmailRequest.class)))
        .thenThrow(SesV2Exception.builder().statusCode(400).build());
    assertThat(
            sender
                .send("recipient@example.com", "subject", "body", "<img src=\"cid:landit-banner\">")
                .status())
        .isEqualTo(Status.FAILED);
    when(client.sendEmail(any(SendEmailRequest.class)))
        .thenThrow(SdkClientException.create("connection interrupted"));
    assertThat(
            sender
                .send("recipient@example.com", "subject", "body", "<img src=\"cid:landit-banner\">")
                .status())
        .isEqualTo(Status.UNKNOWN);
  }

  private EventBridgeNotificationJobScheduler schedulerAdapter() {
    var properties =
        new NotificationProperties(
            "",
            "",
            Duration.ofSeconds(1),
            Duration.ofSeconds(1),
            "https://queue.example.com/push",
            60);
    var adapter =
        new EventBridgeNotificationJobScheduler(
            scheduler, sqs, new JsonMapper(), properties, Clock.fixed(NOW, ZoneOffset.UTC));
    ReflectionTestUtils.setField(adapter, "group", "test-group");
    ReflectionTestUtils.setField(adapter, "queueArn", "queue-arn");
    ReflectionTestUtils.setField(adapter, "roleArn", "role-arn");
    ReflectionTestUtils.setField(adapter, "dlqArn", "dlq-arn");
    return adapter;
  }

  private NotificationJob job(Instant scheduled) {
    return new NotificationJob(
        UUID.randomUUID(),
        "TEST_EMAIL",
        1L,
        null,
        null,
        null,
        null,
        scheduled,
        "recipient@example.com",
        "PENDING",
        null,
        null,
        null,
        null);
  }
}
