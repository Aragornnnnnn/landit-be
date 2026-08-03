// 원어민 표현 학습 완료와 예외 흐름을 단위 검증한다.

package com.landit.landitbe.feature.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
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
import com.landit.landitbe.feature.learning.repository.UserWritingExpressionCompletionRepository;
import com.landit.landitbe.feature.learning.service.LearningProgressService;
import com.landit.landitbe.feature.profile.dto.UserLocale;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import com.landit.landitbe.feature.session.domain.ExpressionGenerationStatus;
import com.landit.landitbe.feature.session.domain.FreeTalkConversationStatus;
import com.landit.landitbe.feature.session.domain.FreeTalkSession;
import com.landit.landitbe.feature.session.domain.FreeTalkSessionExpression;
import com.landit.landitbe.feature.session.domain.LearningSession;
import com.landit.landitbe.feature.session.domain.LearningSessionStatus;
import com.landit.landitbe.feature.session.repository.FreeTalkSessionExpressionRepository;
import com.landit.landitbe.feature.session.repository.FreeTalkSessionRepository;
import com.landit.landitbe.feature.session.repository.LearningSessionRepository;
import com.landit.landitbe.shared.domain.ActiveStatus;
import com.landit.landitbe.shared.domain.Locale;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

/** 원어민 표현 학습 완료와 예외 흐름을 단위 검증한다. */
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

  @Mock private UserWritingExpressionCompletionRepository expressionCompletionRepository;

  @Mock private FreeTalkSessionRepository freeTalkSessionRepository;

  @Mock private LearningSessionRepository learningSessionRepository;

  @Mock private FreeTalkSessionExpressionRepository sessionExpressionRepository;

  @InjectMocks private ExpressionLearningCompletionService expressionLearningCompletionService;

  /** 존재하지 않거나 비활성인 표현은 완료 이력을 저장하지 않는다. */
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

  /** 해금된 표현은 완료 이력을 조회하고 갱신하기 전에 잠금 조회한다. */
  @Test
  void shouldLockUnlockedExpressionBeforeReadingCompletionHistory() {
    // given: 아무것도 완료하지 않은 사용자 + 학습 순서 201→202→203인 시나리오
    givenExpressionAndNoUserCompletion();
    givenUserLocaleExpressionList(
        orderedExpression(UNLOCKED_EXPRESSION_ID),
        orderedExpression(202L),
        orderedExpression(LOCKED_EXPRESSION_ID));

    // when: 첫 번째(해금된) 표현을 완료하면
    expressionLearningCompletionService.completeLearning(USER_ID, UNLOCKED_EXPRESSION_ID);

    // then: 완료 이력을 확인하고 갱신하기 전에 표현 잠금을 획득한다
    InOrder inOrder = inOrder(writingExpressionRepository, learningProgressService);
    inOrder
        .verify(writingExpressionRepository)
        .findByIdAndStatusForUpdate(UNLOCKED_EXPRESSION_ID, ActiveStatus.ACTIVE);
    inOrder.verify(learningProgressService).findCompletedExpressionIds(USER_ID, SCENARIO_ID);
    inOrder
        .verify(learningProgressService)
        .completeExpression(USER_ID, SCENARIO_ID, UNLOCKED_EXPRESSION_ID);
  }

  /** 이미 완료한 표현은 새 기록 없이 마지막 완료 시각만 갱신한다. */
  @Test
  void shouldUpdateLastCompletedAtForRepeatedCompletion() {
    // given: 표현이 존재하고, 사용자가 이미 그 표현을 완료한 상태
    WritingExpression expression = expressionInScenario();
    when(writingExpressionRepository.findByIdAndStatus(UNLOCKED_EXPRESSION_ID, ActiveStatus.ACTIVE))
        .thenReturn(Optional.of(expression));
    when(writingExpressionRepository.findByIdAndStatusForUpdate(
            UNLOCKED_EXPRESSION_ID, ActiveStatus.ACTIVE))
        .thenReturn(Optional.of(expression));
    when(learningProgressService.findCompletedExpressionIds(USER_ID, SCENARIO_ID))
        .thenReturn(new CompletedExpressionIds(Set.of(UNLOCKED_EXPRESSION_ID)));

    // when: 같은 표현을 다시 완료해도
    expressionLearningCompletionService.completeLearning(USER_ID, UNLOCKED_EXPRESSION_ID);

    // then: 새 기록 없이 기존 완료 시각만 갱신한다.
    verify(learningProgressService)
        .completeExpression(USER_ID, SCENARIO_ID, UNLOCKED_EXPRESSION_ID);
  }

  /** 아직 잠긴 표현의 완료 요청은 경고를 남기고 저장 없이 거부한다. */
  @Test
  void shouldLogAndThrowWhenExpressionIsLocked() {
    // given: 로그 검증용 ListAppender 부착
    Logger logger = (Logger) LoggerFactory.getLogger(ExpressionLearningCompletionService.class);
    ListAppender<ILoggingEvent> logAppender = new ListAppender<>();
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

  /** 프리톡 추천 표현은 시나리오 학습 순서와 관계없이 완료한다. */
  @Test
  void shouldCompleteScenarioExpressionFromFreeTalkWithoutOrderLock() {
    long learningSessionId = 701L;
    long freeTalkSessionId = 901L;
    WritingExpression expression = expressionInScenario();
    FreeTalkSession freeTalkSession = mock(FreeTalkSession.class);
    LearningSession learningSession = mock(LearningSession.class);
    FreeTalkSessionExpression sessionExpression =
        FreeTalkSessionExpression.link(freeTalkSessionId, LOCKED_EXPRESSION_ID, 1);
    when(writingExpressionRepository.findByIdAndStatus(LOCKED_EXPRESSION_ID, ActiveStatus.ACTIVE))
        .thenReturn(Optional.of(expression));
    when(writingExpressionRepository.findByIdAndStatusForUpdate(
            LOCKED_EXPRESSION_ID, ActiveStatus.ACTIVE))
        .thenReturn(Optional.of(expression));
    when(freeTalkSessionRepository.findByLearningSessionId(learningSessionId))
        .thenReturn(Optional.of(freeTalkSession));
    when(freeTalkSession.getId()).thenReturn(freeTalkSessionId);
    when(freeTalkSession.getLearningSessionId()).thenReturn(learningSessionId);
    when(freeTalkSession.getConversationStatus()).thenReturn(FreeTalkConversationStatus.COMPLETED);
    when(freeTalkSession.getExpressionGenerationStatus())
        .thenReturn(ExpressionGenerationStatus.READY);
    when(learningSessionRepository.findById(learningSessionId))
        .thenReturn(Optional.of(learningSession));
    when(learningSession.getUserProfileId()).thenReturn(USER_ID);
    when(learningSession.getStatus()).thenReturn(LearningSessionStatus.COMPLETED);
    when(sessionExpressionRepository.findByFreeTalkSessionIdAndWritingExpressionId(
            freeTalkSessionId, LOCKED_EXPRESSION_ID))
        .thenReturn(Optional.of(sessionExpression));
    expressionLearningCompletionService.completeLearning(
        USER_ID, LOCKED_EXPRESSION_ID, learningSessionId);

    assertThat(sessionExpression.getCompletedAt()).isNotNull();
    verify(learningProgressService)
        .completeFreeTalkExpression(USER_ID, expression.getScenarioId(), LOCKED_EXPRESSION_ID);
    verify(writingExpressionRepository)
        .findByIdAndStatusForUpdate(LOCKED_EXPRESSION_ID, ActiveStatus.ACTIVE);
    verify(learningProgressService, never())
        .completeExpression(USER_ID, SCENARIO_ID, LOCKED_EXPRESSION_ID);
  }

  /** 다른 사용자의 프리톡 세션으로는 표현을 완료할 수 없다. */
  @Test
  void shouldRejectFreeTalkCompletionForAnotherUser() {
    long learningSessionId = 701L;
    FreeTalkSession freeTalkSession = mock(FreeTalkSession.class);
    LearningSession learningSession = mock(LearningSession.class);
    WritingExpression expression = expressionInScenario();
    when(writingExpressionRepository.findByIdAndStatus(LOCKED_EXPRESSION_ID, ActiveStatus.ACTIVE))
        .thenReturn(Optional.of(expression));
    when(freeTalkSessionRepository.findByLearningSessionId(learningSessionId))
        .thenReturn(Optional.of(freeTalkSession));
    when(freeTalkSession.getLearningSessionId()).thenReturn(learningSessionId);
    when(learningSessionRepository.findById(learningSessionId))
        .thenReturn(Optional.of(learningSession));
    when(learningSession.getUserProfileId()).thenReturn(USER_ID + 1);

    assertThatThrownBy(
            () ->
                expressionLearningCompletionService.completeLearning(
                    USER_ID, LOCKED_EXPRESSION_ID, learningSessionId))
        .isInstanceOf(ApiException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.FORBIDDEN);

    verify(learningProgressService, never())
        .completeFreeTalkExpression(USER_ID, SCENARIO_ID, LOCKED_EXPRESSION_ID);
  }

  /** 완료되지 않은 프리톡 세션의 표현 완료를 시도하면 RESOURCE_NOT_FOUND 예외를 던진다. */
  @Test
  void shouldRejectFreeTalkCompletionForIncompleteSession() {
    long learningSessionId = 701L;
    FreeTalkSession freeTalkSession = mock(FreeTalkSession.class);
    LearningSession learningSession = mock(LearningSession.class);
    WritingExpression expression = expressionInScenario();
    when(writingExpressionRepository.findByIdAndStatus(LOCKED_EXPRESSION_ID, ActiveStatus.ACTIVE))
        .thenReturn(Optional.of(expression));
    when(freeTalkSessionRepository.findByLearningSessionId(learningSessionId))
        .thenReturn(Optional.of(freeTalkSession));
    when(freeTalkSession.getLearningSessionId()).thenReturn(learningSessionId);
    when(freeTalkSession.getConversationStatus())
        .thenReturn(FreeTalkConversationStatus.IN_PROGRESS);
    when(learningSessionRepository.findById(learningSessionId))
        .thenReturn(Optional.of(learningSession));
    when(learningSession.getUserProfileId()).thenReturn(USER_ID);
    when(learningSession.getStatus()).thenReturn(LearningSessionStatus.COMPLETED);

    assertThatThrownBy(
            () ->
                expressionLearningCompletionService.completeLearning(
                    USER_ID, LOCKED_EXPRESSION_ID, learningSessionId))
        .isInstanceOf(ApiException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);

    verify(learningProgressService, never())
        .completeFreeTalkExpression(USER_ID, SCENARIO_ID, LOCKED_EXPRESSION_ID);
  }

  // ===== 헬퍼 =====

  /** 활성 표현이 있고 사용자의 완료 이력은 없는 상태를 만든다. */
  private void givenExpressionAndNoUserCompletion() {
    WritingExpression expression = expressionInScenario();

    when(writingExpressionRepository.findByIdAndStatus(
            any(), org.mockito.ArgumentMatchers.eq(ActiveStatus.ACTIVE)))
        .thenReturn(Optional.of(expression));
    lenient()
        .when(
            writingExpressionRepository.findByIdAndStatusForUpdate(
                any(), org.mockito.ArgumentMatchers.eq(ActiveStatus.ACTIVE)))
        .thenReturn(Optional.of(expression));
    when(learningProgressService.findCompletedExpressionIds(USER_ID, SCENARIO_ID))
        .thenReturn(new CompletedExpressionIds(Set.of()));
  }

  /** 사용자 로케일에 맞는 시나리오 표현 목록을 스터빙한다. */
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
    // 테스트마다 사용 여부가 달라 lenient로 설정한다.
    lenient().when(expression.getId()).thenReturn(id);
    return expression;
  }
}
