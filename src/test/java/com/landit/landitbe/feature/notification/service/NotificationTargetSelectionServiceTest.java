// 오늘 배정된 시나리오와 스몰톡 사용량에 따른 예약 알림 우선순위를 검증한다.

package com.landit.landitbe.feature.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.landit.landitbe.feature.notification.domain.NotificationType;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 오늘의 학습 상태에 따라 사용자별 하루 한 건의 예약 알림을 선정하는 규칙을 검증한다. */
class NotificationTargetSelectionServiceTest {

  private final NotificationTargetSelectionService selectionService =
      new NotificationTargetSelectionService();

  /** 오늘 배정된 시나리오가 미완료면 다른 상태보다 시나리오 알림을 우선한다. */
  @Test
  void selectsDailyScenarioReminderWhenTodaysScenarioIsIncomplete() {
    NotificationTargetSelectionInput input =
        input(10L, false, 0L, List.of(new ExpressionNotificationCandidate(10L, 100L, false)));

    SelectedNotificationTarget result = selectionService.select(input).orElseThrow();

    assertThat(result.notificationType()).isEqualTo(NotificationType.DAILY_SCENARIO_REMINDER);
    assertThat(result.targetId()).isEqualTo(10L);
    assertThat(result.scenarioId()).isNull();
  }

  /** 오늘 시나리오를 완료했으면 같은 시나리오의 첫 미완료 표현을 선택한다. */
  @Test
  void selectsIncompleteExpressionFromTodaysCompletedScenario() {
    NotificationTargetSelectionInput input =
        input(
            10L,
            true,
            0L,
            List.of(
                new ExpressionNotificationCandidate(9L, 90L, false),
                new ExpressionNotificationCandidate(10L, 100L, true),
                new ExpressionNotificationCandidate(10L, 101L, false)));

    SelectedNotificationTarget result = selectionService.select(input).orElseThrow();

    assertThat(result.notificationType()).isEqualTo(NotificationType.CONTINUE_EXPRESSION);
    assertThat(result.targetId()).isEqualTo(101L);
    assertThat(result.scenarioId()).isEqualTo(10L);
  }

  /** 오늘 시나리오의 표현을 모두 완료했고 스몰톡을 쓰지 않았으면 스몰톡 알림을 선택한다. */
  @Test
  void selectsSmallTalkReminderWhenLearningIsCompleteAndSmallTalkIsUnused() {
    NotificationTargetSelectionInput input =
        input(10L, true, 0L, List.of(new ExpressionNotificationCandidate(10L, 100L, true)));

    SelectedNotificationTarget result = selectionService.select(input).orElseThrow();

    assertThat(result.notificationType()).isEqualTo(NotificationType.SMALL_TALK_REMINDER);
    assertThat(result.targetId()).isNull();
    assertThat(result.scenarioId()).isNull();
  }

  /** 스몰톡을 일부라도 사용했으면 스몰톡 알림을 보내지 않는다. */
  @Test
  void doesNotSelectSmallTalkReminderAfterAnyUsage() {
    NotificationTargetSelectionInput input = input(10L, true, 1L, List.of());

    assertThat(selectionService.select(input)).isEmpty();
  }

  /** 콘텐츠를 전부 완료한 이론적 상태에서도 스몰톡을 이미 썼으면 알림을 보내지 않는다. */
  @Test
  void doesNotSelectReminderAfterSmallTalkLimitIsExhausted() {
    NotificationTargetSelectionInput input = input(10L, true, 60_000L, List.of());

    assertThat(selectionService.select(input)).isEmpty();
  }

  /** 오늘의 시나리오를 결정할 수 없으면 다른 유형으로 추론하지 않는다. */
  @Test
  void doesNotSelectFallbackWhenDailyScenarioIsUnavailable() {
    NotificationTargetSelectionInput input = input(null, false, 0L, List.of());

    assertThat(selectionService.select(input)).isEmpty();
  }

  /** 테스트에 필요한 오늘의 알림 선정 입력을 만든다. */
  private NotificationTargetSelectionInput input(
      Long dailyScenarioId,
      boolean dailyScenarioCompleted,
      long freeTalkUsedSpeakingDurationMs,
      List<ExpressionNotificationCandidate> expressions) {
    return new NotificationTargetSelectionInput(
        1L,
        dailyScenarioId,
        dailyScenarioCompleted,
        freeTalkUsedSpeakingDurationMs,
        null,
        null,
        expressions);
  }
}
