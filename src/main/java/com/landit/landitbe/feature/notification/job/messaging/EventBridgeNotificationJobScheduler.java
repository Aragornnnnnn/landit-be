// 채널별 알림을 EventBridge 또는 SQS로 멱등 등록한다.

package com.landit.landitbe.feature.notification.job.messaging;

import com.landit.landitbe.config.notification.NotificationProperties;
import com.landit.landitbe.feature.notification.delivery.messaging.PushQueueMessage;
import com.landit.landitbe.feature.notification.delivery.messaging.PushQueuePayload;
import com.landit.landitbe.feature.notification.job.dto.NotificationJob;
import java.time.Clock;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.scheduler.SchedulerClient;
import software.amazon.awssdk.services.scheduler.model.ConflictException;
import software.amazon.awssdk.services.scheduler.model.CreateScheduleRequest;
import software.amazon.awssdk.services.scheduler.model.GetScheduleRequest;
import software.amazon.awssdk.services.scheduler.model.Target;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import tools.jackson.databind.json.JsonMapper;

/** 이메일 주소 없이 작업 ID만 큐에 전달한다. */
@Component
public class EventBridgeNotificationJobScheduler implements NotificationJobScheduler {
  private final SchedulerClient scheduler;
  private final SqsAsyncClient sqs;
  private final JsonMapper mapper;
  private final NotificationProperties properties;
  private final Clock clock;

  @Value("${landit.notification.scheduler.group:default}")
  private String group;

  @Value("${landit.notification.scheduler.queue-arn:}")
  private String queueArn;

  @Value("${landit.notification.scheduler.role-arn:}")
  private String roleArn;

  @Value("${landit.notification.scheduler.dlq-arn:}")
  private String dlqArn;

  /**
   * 예약과 큐 클라이언트를 주입한다.
   *
   * @param scheduler 예약 클라이언트
   * @param sqsAsyncClient 큐 클라이언트
   * @param mapper 직렬화기
   * @param properties 큐 설정
   * @param clock 현재 시각
   */
  public EventBridgeNotificationJobScheduler(
      SchedulerClient scheduler,
      SqsAsyncClient sqsAsyncClient,
      JsonMapper mapper,
      NotificationProperties properties,
      Clock clock) {
    this.scheduler = scheduler;
    this.sqs = sqsAsyncClient;
    this.mapper = mapper;
    this.properties = properties;
    this.clock = clock;
  }

  /** {@inheritDoc} */
  @Override
  public void schedule(NotificationJob job) {
    String input =
        mapper.writeValueAsString(
            new PushQueueMessage(
                1,
                job.id().toString(),
                PushQueueMessage.NOTIFICATION_JOB,
                job.scheduledAt(),
                new PushQueuePayload(null, null)));
    if (!job.scheduledAt().isAfter(clock.instant().plusSeconds(60))) {
      publish(input, job);
      return;
    }
    if (group.isBlank() || queueArn.isBlank() || roleArn.isBlank() || dlqArn.isBlank()) {
      throw new IllegalStateException("알림 예약 설정이 없습니다.");
    }
    String name = "notification-job-" + job.id();
    String expression =
        "at("
            + DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
                .withZone(ZoneOffset.UTC)
                .format(job.scheduledAt())
            + ")";
    Target target =
        Target.builder()
            .arn(queueArn)
            .roleArn(roleArn)
            .input(input)
            .deadLetterConfig(d -> d.arn(dlqArn))
            .build();
    try {
      scheduler.createSchedule(
          CreateScheduleRequest.builder()
              .name(name)
              .groupName(group)
              .clientToken(job.id().toString())
              .scheduleExpression(expression)
              .scheduleExpressionTimezone("UTC")
              .flexibleTimeWindow(w -> w.mode("OFF"))
              .actionAfterCompletion("DELETE")
              .target(target)
              .build());
    } catch (ConflictException exception) {
      var existing =
          scheduler.getSchedule(GetScheduleRequest.builder().name(name).groupName(group).build());
      if (!expression.equals(existing.scheduleExpression())
          || !"UTC".equals(existing.scheduleExpressionTimezone())
          || !target.arn().equals(existing.target().arn())
          || !target.roleArn().equals(existing.target().roleArn())
          || !input.equals(existing.target().input())
          || !target.deadLetterConfig().equals(existing.target().deadLetterConfig())) {
        throw new IllegalStateException("동일 작업의 예약 내용이 일치하지 않습니다.");
      }
    }
  }

  private void publish(String input, NotificationJob job) {
    if (properties.queueUrl() == null || properties.queueUrl().isBlank()) {
      throw new IllegalStateException("알림 큐 설정이 없습니다.");
    }
    int delay =
        (int)
            Math.max(
                0, java.time.Duration.between(clock.instant(), job.scheduledAt()).toSeconds() + 1);
    sqs.sendMessage(
            SendMessageRequest.builder()
                .queueUrl(properties.queueUrl())
                .messageBody(input)
                .delaySeconds(delay)
                .build())
        .orTimeout(10, TimeUnit.SECONDS)
        .join();
  }
}
