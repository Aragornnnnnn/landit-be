// 예약 알림 유형별 문구와 프런트 딥링크 계약을 검증한다.

package com.landit.landitbe.feature.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.landit.landitbe.feature.notification.domain.NotificationType;
import org.junit.jupiter.api.Test;

/** 예약 알림 유형별 문구와 프런트 딥링크 계약을 검증한다. */
class ScheduledNotificationContentTest {

  /** 오늘의 시나리오 알림은 시나리오 대화 화면으로 연결한다. */
  @Test
  void createsDailyScenarioReminderContent() {
    SelectedNotificationTarget target =
        new SelectedNotificationTarget(NotificationType.DAILY_SCENARIO_REMINDER, 10L, null);
    ScheduledNotificationContent content = ScheduledNotificationContent.from(target);

    assertThat(content.deepLink())
        .isEqualTo(
            "/conversation/scenario/10?utm_source=push&utm_medium=notification&"
                + "utm_campaign=daily_scenario_reminder");
  }

  /** 표현 이어 하기 알림은 부모 시나리오와 표현을 포함한 학습 화면으로 연결한다. */
  @Test
  void createsContinueExpressionContent() {
    SelectedNotificationTarget target =
        new SelectedNotificationTarget(NotificationType.CONTINUE_EXPRESSION, 100L, 10L);
    ScheduledNotificationContent content = ScheduledNotificationContent.from(target);

    assertThat(content.deepLink())
        .isEqualTo(
            "/expressions/scenario/10/100?utm_source=push&utm_medium=notification&"
                + "utm_campaign=continue_expression");
  }

  /** 스몰톡 알림은 사용자가 모드와 파트너를 고를 수 있는 스몰톡 탭으로 연결한다. */
  @Test
  void createsSmallTalkReminderContent() {
    ScheduledNotificationContent content =
        ScheduledNotificationContent.from(
            new SelectedNotificationTarget(NotificationType.SMALL_TALK_REMINDER, null, null));

    assertThat(content.deepLink())
        .isEqualTo(
            "/smalltalk?utm_source=push&utm_medium=notification&utm_campaign=small_talk_reminder");
  }
}
