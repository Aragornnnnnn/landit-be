// LearningProgressService의 표현 완료와 시나리오 시작 저장 정책을 검증한다.

package com.landit.landitbe.feature.learning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.learning.domain.UserScenarioProgress;
import com.landit.landitbe.feature.learning.domain.UserWritingExpressionCompletion;
import com.landit.landitbe.feature.learning.dto.CompletedExpressionIds;
import com.landit.landitbe.feature.learning.repository.UserScenarioProgressRepository;
import com.landit.landitbe.feature.learning.repository.UserWritingExpressionCompletionRepository;
import com.landit.landitbe.shared.domain.Locale;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** LearningProgressService의 표현 완료와 시나리오 시작 저장 정책을 검증한다. */
@ExtendWith(MockitoExtension.class)
class LearningProgressServiceTest {

  private static final Long USER_ID = 1L;
  private static final Long SCENARIO_ID = 2L;
  private static final Long EXPRESSION_ID = 3L;

  @Mock private UserScenarioProgressRepository userScenarioProgressRepository;

  @Mock private UserWritingExpressionCompletionRepository expressionCompletionRepository;

  @InjectMocks private LearningProgressService learningProgressService;

  /** 처음 완료한 표현은 새 완료 기록으로 저장한다. */
  @Test
  void savesFirstExpressionCompletion() {
    when(expressionCompletionRepository.findAllByUserProfileIdAndScenarioId(USER_ID, SCENARIO_ID))
        .thenReturn(List.of());

    learningProgressService.completeExpression(USER_ID, SCENARIO_ID, EXPRESSION_ID);

    verify(expressionCompletionRepository).save(any(UserWritingExpressionCompletion.class));
  }

  /** 이미 완료한 표현은 새 row 대신 마지막 완료 시각만 갱신한다. */
  @Test
  void updatesRepeatedExpressionCompletion() {
    UserWritingExpressionCompletion completion = mock(UserWritingExpressionCompletion.class);
    when(completion.getWritingExpressionId()).thenReturn(EXPRESSION_ID);
    when(expressionCompletionRepository.findAllByUserProfileIdAndScenarioId(USER_ID, SCENARIO_ID))
        .thenReturn(List.of(completion));

    learningProgressService.completeExpression(USER_ID, SCENARIO_ID, EXPRESSION_ID);

    verify(completion).markCompletedAgain();
    verify(expressionCompletionRepository, never()).save(any());
  }

  /** 다른 기능에는 학습 완료 엔티티 대신 완료한 표현 ID record를 반환한다. */
  @Test
  void returnsCompletedExpressionIds() {
    UserWritingExpressionCompletion first = mock(UserWritingExpressionCompletion.class);
    UserWritingExpressionCompletion second = mock(UserWritingExpressionCompletion.class);
    when(first.getWritingExpressionId()).thenReturn(10L);
    when(second.getWritingExpressionId()).thenReturn(20L);
    when(expressionCompletionRepository.findAllByUserProfileIdAndScenarioId(USER_ID, SCENARIO_ID))
        .thenReturn(List.of(first, second));

    CompletedExpressionIds result =
        learningProgressService.findCompletedExpressionIds(USER_ID, SCENARIO_ID);

    assertThat(result.values()).containsExactlyInAnyOrder(10L, 20L);
  }

  /** 기존 시나리오 진행도가 있으면 새 row를 만들지 않고 시작 시각을 갱신한다. */
  @Test
  void updatesExistingScenarioProgress() {
    LocalDateTime startedAt = LocalDateTime.of(2026, 7, 23, 12, 0);
    UserScenarioProgress progress = mock(UserScenarioProgress.class);
    when(userScenarioProgressRepository.findByUserProfileIdAndScenarioIdAndTargetLocale(
            USER_ID, SCENARIO_ID, Locale.EN))
        .thenReturn(Optional.of(progress));

    learningProgressService.startScenario(USER_ID, SCENARIO_ID, Locale.EN, startedAt);

    verify(progress).markStarted(startedAt);
    verify(userScenarioProgressRepository, never()).save(any());
  }
}
