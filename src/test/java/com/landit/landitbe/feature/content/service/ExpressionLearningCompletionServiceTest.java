// 원어민 표현 학습 완료 흐름의 완료 기록 생성, 멱등 처리, 잠금/미존재 예외를 단위 검증한다.

package com.landit.landitbe.feature.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.landit.landitbe.feature.content.domain.WritingExpression;
import com.landit.landitbe.feature.content.repository.WritingExpressionRepository;
import com.landit.landitbe.feature.learning.dto.CompletedExpressionIds;
import com.landit.landitbe.feature.learning.service.LearningProgressService;
import com.landit.landitbe.feature.profile.dto.UserLocale;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import com.landit.landitbe.shared.domain.ActiveStatus;
import com.landit.landitbe.shared.domain.Locale;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

/** 원어민 표현 학습 완료 흐름의 완료 기록 생성, 멱등 처리, 잠금/미존재 예외를 단위 검증한다. */
@ExtendWith(MockitoExtension.class)
class ExpressionLearningCompletionServiceTest {

  private static final Long USER_ID = 1L;
  private static final Long SCENARIO_ID = 10L;
  private static final Locale TARGET_LOCALE = Locale.EN;
  private static final Locale BASE_LOCALE = Locale.KR;

  // 학습 순서: 201(1번) → 202(2번) → 203(3번)
  private static final Long UNLOCKED_EXPRESSION_ID = 201L;
  private static final Long LOCKED_EXPRESSION_ID = 203L;

  @Mock private WritingExpressionRepository writingExpressionRepository;

  @Mock private UserProfileService userProfileService;

  @Mock private LearningProgressService learningProgressService;

  @InjectMocks private ExpressionLearningCompletionService expressionLearningCompletionService;

  /** 없는(또는 INACTIVE) 표현을 완료하려 하면 RESOURCE_NOT_FOUND 예외를 던지고 아무것도 저장하지 않는다. */
  @Test
  void shouldThrowWhenExpressionNotFound() {
    // given: 해당 ID의 활성 표현이 없음
    when(writingExpressionRepository.findByIdAndStatus(UNLOCKED_EXPRESSION_ID, ActiveStatus.ACTIVE))
        .thenReturn(Optional.empty());

    // when & then
    assertThatThrownBy(
            () ->
                expressionLearningCompletionService.completeLearning(
                    USER_ID, UNLOCKED_EXPRESSION_ID))
        .isInstanceOf(ApiException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);

    verify(learningProgressService, never())
        .completeExpression(USER_ID, SCENARIO_ID, UNLOCKED_EXPRESSION_ID);
  }

  /** 해금된(미완료 중 학습 순서가 가장 앞선) 표현을 완료하면 완료 기록을 올바른 값으로 저장한다. */
  @Test
  void shouldSaveCompletionForUnlockedExpression() {
    // given: 아무것도 완료하지 않은 사용자 + 학습 순서 201→202→203인 시나리오
    givenExpressionAndNoUserCompletion();
    givenUserLocaleExpressionList(
        orderedExpression(UNLOCKED_EXPRESSION_ID),
        orderedExpression(202L),
        orderedExpression(LOCKED_EXPRESSION_ID));

    // when: 첫 번째(해금된) 표현을 완료하면
    expressionLearningCompletionService.completeLearning(USER_ID, UNLOCKED_EXPRESSION_ID);

    // then: 완료 기록이 사용자/시나리오/표현 ID를 담아 저장된다
    verify(learningProgressService)
        .completeExpression(USER_ID, SCENARIO_ID, UNLOCKED_EXPRESSION_ID);
  }

  /** 이미 완료한 표현을 다시 완료 요청하면(멱등) 예외 없이 정상 종료하고 새 기록을 저장하지 않는다. last_completed_at 필드만 갱신된다. */
  @Test
  void shouldUpdateLastCompletedAtForRepeatedCompletion() {
    // given: 표현이 존재하고, 사용자가 이미 그 표현을 완료한 상태
    WritingExpression expression = expressionInScenario();
    when(writingExpressionRepository.findByIdAndStatus(UNLOCKED_EXPRESSION_ID, ActiveStatus.ACTIVE))
        .thenReturn(Optional.of(expression));
    when(learningProgressService.findCompletedExpressionIds(USER_ID, SCENARIO_ID))
        .thenReturn(new CompletedExpressionIds(Set.of(UNLOCKED_EXPRESSION_ID)));

    // when: 같은 표현을 다시 완료해도
    expressionLearningCompletionService.completeLearning(USER_ID, UNLOCKED_EXPRESSION_ID);

    // then: 새 완료 기록은 저장되지 않고(멱등), 기존 기록의 마지막 완료 시각만 갱신된다 (복습 시각 기록)
    verify(learningProgressService)
        .completeExpression(USER_ID, SCENARIO_ID, UNLOCKED_EXPRESSION_ID);
  }

  /** 아직 잠긴(미완료 중 학습 순서가 앞선 표현이 남은) 표현을 완료하려 하면 EXPRESSION_LOCKED 예외 + 경고 로그, 저장 없음. */
  @Test
  void shouldLogAndThrowWhenExpressionIsLocked() {
    // given: 로그 검증용 ListAppender 부착
    Logger logger =
        (Logger)
            LoggerFactory.getLogger(
                ExpressionLearningCompletionService
                    .class); // "ExpressionLearningCompletionService 클래스가 로그를 찍을 때 쓰는 그 Logger"
    // 인스턴스를
    // 가져오는 역할.
    ListAppender<ILoggingEvent> logAppender = new ListAppender<>(); // 찍히는 로그를 리스트에 차곡차곡 저장해주는 역할.
    logAppender.start();
    logger.addAppender(logAppender);

    // given: 아무것도 완료하지 않은 사용자 + 학습 순서 201→202→203 (201이 해금 대상)
    givenExpressionAndNoUserCompletion();
    givenUserLocaleExpressionList(
        orderedExpression(UNLOCKED_EXPRESSION_ID),
        orderedExpression(202L),
        orderedExpression(LOCKED_EXPRESSION_ID));

    // when & then: 아직 잠긴 203번을 완료하려 하면 EXPRESSION_LOCKED
    assertThatThrownBy(
            () ->
                expressionLearningCompletionService.completeLearning(USER_ID, LOCKED_EXPRESSION_ID))
        .isInstanceOf(ApiException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.EXPRESSION_LOCKED);

    // then: 저장 없음 + 어떤 사용자/표현이 막혔는지 warn 로그
    verify(learningProgressService, never())
        .completeExpression(USER_ID, SCENARIO_ID, LOCKED_EXPRESSION_ID);
    assertThat(logAppender.list)
        .anySatisfy(
            event -> {
              assertThat(event.getLevel()).isEqualTo(Level.WARN);
              assertThat(event.getFormattedMessage())
                  .contains(String.valueOf(LOCKED_EXPRESSION_ID));
            });

    logger.detachAppender(logAppender);
  }

  // ===== 헬퍼 =====

  /** "표현이 존재하고(ACTIVE) 사용자가 이 시나리오에서 완료한 표현이 없다"는 상황을 만든다. */
  private void givenExpressionAndNoUserCompletion() {
    WritingExpression expression = expressionInScenario();

    when(writingExpressionRepository.findByIdAndStatus(
            any(), org.mockito.ArgumentMatchers.eq(ActiveStatus.ACTIVE)))
        .thenReturn(Optional.of(expression));
    when(learningProgressService.findCompletedExpressionIds(USER_ID, SCENARIO_ID))
        .thenReturn(new CompletedExpressionIds(Set.of()));
  }

  /** 사용자 locale(EN/KR) 기준으로 이 시나리오의 표현 목록(학습 순서)이 이렇게 조회된다고 스터빙한다. */
  private void givenUserLocaleExpressionList(WritingExpression... expressions) {
    when(userProfileService.getUserLocale(USER_ID))
        .thenReturn(new UserLocale(TARGET_LOCALE, BASE_LOCALE));
    when(writingExpressionRepository
            .findByScenarioIdAndTargetLocaleAndBaseLocaleAndStatusOrderByDisplayOrderAsc(
                SCENARIO_ID, TARGET_LOCALE, BASE_LOCALE, ActiveStatus.ACTIVE))
        .thenReturn(List.of(expressions));
  }

  /** 완료 대상 표현 mock. scenarioId만 있으면 되므로 그것만 스터빙한다. */
  private WritingExpression expressionInScenario() {
    WritingExpression expression = mock(WritingExpression.class);
    when(expression.getScenarioId()).thenReturn(SCENARIO_ID);
    return expression;
  }

  /** 학습 순서 목록에 들어갈 표현 mock. 해금 판정은 id로 하므로 getId만 스터빙한다. */
  private WritingExpression orderedExpression(Long id) {
    WritingExpression expression = mock(WritingExpression.class);
    // 해금 판정의 findFirst()가 첫 원소에서 멈추면 뒤 mock의 getId는 호출되지 않으므로,
    lenient()
        .when(expression.getId())
        .thenReturn(id); // lenient = 이 스터빙은 이번 테스트에서 호출이 안 돼도 괜찮으니까, 안 쓰인다고 에러 내지 마라
    return expression;
  }
}
