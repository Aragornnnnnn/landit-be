// 예약 알림 유형별 문구와 프런트 딥링크 계약을 검증한다.

package com.landit.landitbe.feature.notification.scheduled.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.landit.landitbe.feature.notification.domain.NotificationContentVariant;
import com.landit.landitbe.feature.notification.domain.NotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** 예약 알림 유형별 문구와 프런트 딥링크 계약을 검증한다. */
class ScheduledNotificationContentTest {

  @DisplayName("발행한 문구 슬러그는 고정하며 같은 문구인 A2와 R0는 같은 값으로 집계한다.")
  @ParameterizedTest
  @CsvSource({
    "SCENARIO_A1, scenario_daily_fruit",
    "SCENARIO_A2, scenario_bird_tip",
    "SCENARIO_A3, scenario_five_minutes",
    "SCENARIO_A4, scenario_streak_record",
    "SCENARIO_R0, scenario_bird_tip",
    "SCENARIO_R1, scenario_restart_today",
    "SCENARIO_R2, scenario_waiting_days",
    "SCENARIO_R3, scenario_keep_habit",
    "SCENARIO_R4, scenario_feedback",
    "SCENARIO_R5, scenario_missing_you",
    "SCENARIO_R6, scenario_please_study",
    "EXPRESSION_DYNAMIC, expression_usage_question",
    "EXPRESSION_GENERIC, expression_continue",
    "SMALL_TALK_DYNAMIC, small_talk_continue_topic",
    "SMALL_TALK_GENERIC, small_talk_teddy_waiting"
  })
  void preservesPublishedContentSlugs(NotificationContentVariant variant, String slug) {
    assertThat(variant.contentSlug()).isEqualTo(slug).matches("[a-z]+(?:_[a-z]+)*");
  }

  /** 오늘의 시나리오 알림은 시나리오 홈으로 연결한다. */
  @DisplayName("오늘의 시나리오 알림은 시나리오 홈으로 연결한다.")
  @Test
  void createsDailyScenarioReminderContent() {
    SelectedNotificationTarget target =
        new SelectedNotificationTarget(NotificationType.DAILY_SCENARIO_REMINDER, 10L, null);
    ScheduledNotificationContent content = ScheduledNotificationContent.from(target);

    assertThat(content.deepLink())
        .isEqualTo(
            "/scenario?utm_source=push&utm_medium=notification&"
                + "utm_campaign=daily_scenario_reminder&utm_content=scenario_bird_tip");
  }

  /** 표현 이어 하기 알림은 부모 시나리오와 표현을 포함한 학습 화면으로 연결한다. */
  @DisplayName("표현 이어 하기 알림은 부모 시나리오와 표현을 포함한 학습 화면으로 연결한다.")
  @Test
  void createsContinueExpressionContent() {
    SelectedNotificationTarget target =
        new SelectedNotificationTarget(NotificationType.CONTINUE_EXPRESSION, 100L, 10L);
    ScheduledNotificationContent content = ScheduledNotificationContent.from(target);

    assertThat(content.deepLink())
        .isEqualTo(
            "/expressions/scenario/10/100?utm_source=push&utm_medium=notification&"
                + "utm_campaign=continue_expression&utm_content=expression_continue");
  }

  /** 스몰톡 알림은 사용자가 모드와 파트너를 고를 수 있는 스몰톡 탭으로 연결한다. */
  @DisplayName("스몰톡 알림은 사용자가 모드와 파트너를 고를 수 있는 스몰톡 탭으로 연결한다.")
  @Test
  void createsSmallTalkReminderContent() {
    ScheduledNotificationContent content =
        ScheduledNotificationContent.from(
            new SelectedNotificationTarget(NotificationType.SMALL_TALK_REMINDER, null, null));

    assertThat(content.deepLink())
        .isEqualTo(
            "/smalltalk?utm_source=push&utm_medium=notification&utm_campaign=small_talk_reminder"
                + "&utm_content=small_talk_teddy_waiting");
  }
}
