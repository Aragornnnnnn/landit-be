// 사용자별 하루 한 건의 학습 알림 대상 선정 규칙을 검증한다.

package com.landit.landitbe.feature.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.landit.landitbe.feature.notification.domain.NotificationType;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 사용자별 하루 한 건의 학습 알림 대상 선정 규칙을 검증한다. */
class NotificationTargetSelectionServiceTest {

  private final NotificationTargetSelectionService selectionService =
      new NotificationTargetSelectionService();

  /** 최근에 시나리오를 완료했으면 같은 카테고리의 다음 접근 가능한 시나리오를 우선한다. */
  @Test
  void prioritizesNextScenarioInCategoryOfLatestScenarioCompletion() {
    NotificationTargetSelectionInput input =
        new NotificationTargetSelectionInput(
            1L,
            LocalDateTime.of(2026, 7, 26, 19, 0),
            10L,
            LocalDateTime.of(2026, 7, 26, 18, 0),
            10L,
            List.of(
                new ScenarioNotificationCandidate(1L, 1L, true, 10L, 1, true, true, true),
                new ScenarioNotificationCandidate(1L, 1L, true, 11L, 2, true, true, false)),
            List.of());

    SelectedNotificationTarget result = selectionService.select(input).orElseThrow();

    assertThat(result.notificationType()).isEqualTo(NotificationType.CONTINUE_SCENARIO);
    assertThat(result.targetId()).isEqualTo(11L);
  }

  /** 최근 시나리오의 다음 후보가 없으면 완료한 부모 시나리오의 표현 학습으로 대체한다. */
  @Test
  void fallsBackToExpressionWhenPreferredScenarioHasNoCandidate() {
    NotificationTargetSelectionInput input =
        new NotificationTargetSelectionInput(
            1L,
            LocalDateTime.of(2026, 7, 26, 19, 0),
            10L,
            null,
            null,
            List.of(new ScenarioNotificationCandidate(1L, 1L, true, 10L, 1, true, true, true)),
            List.of(new ExpressionNotificationCandidate(1L, 10L, 1, 100L, 1, true, false)));

    SelectedNotificationTarget result = selectionService.select(input).orElseThrow();

    assertThat(result.notificationType()).isEqualTo(NotificationType.CONTINUE_EXPRESSION);
    assertThat(result.targetId()).isEqualTo(100L);
  }

  /** 신규 사용자는 접근 가능한 첫 미완료 시나리오를 선택한다. */
  @Test
  void selectsFirstAccessibleScenarioForNewUser() {
    NotificationTargetSelectionInput input =
        new NotificationTargetSelectionInput(
            1L,
            null,
            null,
            null,
            null,
            List.of(
                new ScenarioNotificationCandidate(1L, 1L, true, 10L, 1, true, true, false),
                new ScenarioNotificationCandidate(1L, 1L, true, 11L, 2, true, true, false)),
            List.of());

    SelectedNotificationTarget result = selectionService.select(input).orElseThrow();

    assertThat(result.notificationType()).isEqualTo(NotificationType.CONTINUE_SCENARIO);
    assertThat(result.targetId()).isEqualTo(10L);
  }

  /** 모든 활성 시나리오와 표현을 완료한 사용자는 ID 없이 복습 알림을 받는다. */
  @Test
  void selectsReviewLearningForUserWhoCompletedAllCurrentContent() {
    NotificationTargetSelectionInput input =
        new NotificationTargetSelectionInput(
            1L,
            LocalDateTime.of(2026, 7, 26, 19, 0),
            10L,
            LocalDateTime.of(2026, 7, 26, 18, 0),
            10L,
            List.of(new ScenarioNotificationCandidate(1L, 1L, true, 10L, 1, true, true, true)),
            List.of(new ExpressionNotificationCandidate(1L, 10L, 1, 100L, 1, true, true)));

    SelectedNotificationTarget result = selectionService.select(input).orElseThrow();

    assertThat(result.notificationType()).isEqualTo(NotificationType.REVIEW_LEARNING);
    assertThat(result.targetId()).isNull();
  }

  /** 현재 사용자 언어에 활성 콘텐츠가 하나도 없으면 복습 알림을 발송하지 않는다. */
  @Test
  void doesNotSelectReviewLearningWhenNoActiveContentExists() {
    NotificationTargetSelectionInput input =
        new NotificationTargetSelectionInput(
            1L, LocalDateTime.of(2026, 7, 26, 19, 0), 10L, null, null, List.of(), List.of());

    assertThat(selectionService.select(input)).isEmpty();
  }

  /** 비활성 시나리오만 존재하고 표현이 없으면 복습 알림을 발송하지 않는다. */
  @Test
  void doesNotSelectReviewLearningWhenOnlyInactiveScenariosExist() {
    NotificationTargetSelectionInput input =
        new NotificationTargetSelectionInput(
            1L,
            LocalDateTime.of(2026, 7, 26, 19, 0),
            10L,
            null,
            null,
            List.of(new ScenarioNotificationCandidate(1L, 1L, false, 10L, 1, true, true, true)),
            List.of());

    assertThat(selectionService.select(input)).isEmpty();
  }
}
