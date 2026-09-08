// 한국 시간의 일회성 EventBridge 예약으로 기존 Push SQS 메시지를 발행한다.

package com.landit.landitbe.feature.notification.messaging;

import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.scheduler.SchedulerClient;
import software.amazon.awssdk.services.scheduler.model.ConflictException;
import software.amazon.awssdk.services.scheduler.model.ResourceNotFoundException;
import software.amazon.awssdk.services.scheduler.model.Target;
import tools.jackson.databind.json.JsonMapper;

/** 운영 자격 증명과 예약 실행 역할을 사용하는 EventBridge Adapter다. */
@Component
public class EventBridgeAdminPushScheduler implements AdminPushScheduler {
  private final SchedulerClient client;
  private final JsonMapper mapper;
  private final String group;
  private final String queueArn;
  private final String roleArn;

  /**
   * 예약 클라이언트와 실행 대상 설정을 받는다.
   *
   * @param client 예약 클라이언트
   * @param mapper SQS 직렬화기
   * @param group 환경별 예약 그룹
   * @param queueArn 기존 Push SQS ARN
   * @param roleArn SQS 전송을 허용한 Scheduler 실행 역할 ARN
   */
  public EventBridgeAdminPushScheduler(
      SchedulerClient client,
      JsonMapper mapper,
      @Value("${landit.notification.scheduler.group:default}") String group,
      @Value("${landit.notification.scheduler.queue-arn:}") String queueArn,
      @Value("${landit.notification.scheduler.role-arn:}") String roleArn) {
    this.client = client;
    this.mapper = mapper;
    this.group = group;
    this.queueArn = queueArn;
    this.roleArn = roleArn;
  }

  /** {@inheritDoc} */
  @Override
  public void schedule(UUID campaignId, Instant time) {
    validateConfiguration();
    String expression =
        "at("
            + DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
                .withZone(ZoneId.of("Asia/Seoul"))
                .format(time)
            + ")";
    String input =
        mapper.writeValueAsString(
            new PushQueueMessage(
                1,
                "admin-campaign:" + campaignId,
                PushQueueMessage.ADMIN_PUSH_CAMPAIGN,
                time,
                new PushQueuePayload(null, null, null, null, null, campaignId, null, null)));
    Target target = Target.builder().arn(queueArn).roleArn(roleArn).input(input).build();
    try {
      client.createSchedule(
          builder ->
              builder
                  .name(name(campaignId))
                  .groupName(group)
                  .clientToken(campaignId.toString())
                  .scheduleExpression(expression)
                  .scheduleExpressionTimezone("Asia/Seoul")
                  .flexibleTimeWindow(window -> window.mode("OFF"))
                  .actionAfterCompletion("DELETE")
                  .target(target));
    } catch (ConflictException exception) {
      var existing = client.getSchedule(builder -> builder.name(name(campaignId)).groupName(group));
      if (!expression.equals(existing.scheduleExpression())
          || !"Asia/Seoul".equals(existing.scheduleExpressionTimezone())
          || !queueArn.equals(existing.target().arn())
          || !roleArn.equals(existing.target().roleArn())
          || !input.equals(existing.target().input())) {
        throw new ApiException(ErrorCode.CONFLICT);
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public void cancel(UUID campaignId) {
    validateConfiguration();
    try {
      client.deleteSchedule(builder -> builder.name(name(campaignId)).groupName(group));
    } catch (ResourceNotFoundException exception) {
      // 완료 후 자동 삭제 또는 중복 취소는 성공이다.
      return;
    }
  }

  private String name(UUID campaignId) {
    return "admin-push-" + campaignId;
  }

  private void validateConfiguration() {
    if (queueArn.isBlank() || roleArn.isBlank() || group.isBlank()) {
      throw new ApiException(ErrorCode.SERVICE_UNAVAILABLE);
    }
  }
}
