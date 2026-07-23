// 원어민 표현 학습 완료 유스케이스의 완료 기록 생성, 멱등 처리, 잠금/미존재 예외를 단위 검증한다.

package com.landit.landitbe.feature.content.application;

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
import com.landit.landitbe.feature.auth.application.UserLocale;
import com.landit.landitbe.feature.auth.application.UserProfileService;
import com.landit.landitbe.feature.content.domain.UserWritingExpressionCompletion;
import com.landit.landitbe.feature.content.domain.WritingExpression;
import com.landit.landitbe.feature.content.infrastructure.UserWritingExpressionCompletionRepository;
import com.landit.landitbe.feature.content.infrastructure.WritingExpressionRepository;
import com.landit.landitbe.shared.domain.ActiveStatus;
import com.landit.landitbe.shared.domain.Locale;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

/** 원어민 표현 학습 완료 유스케이스의 완료 기록 생성, 멱등 처리, 잠금/미존재 예외를 단위 검증한다. */
@ExtendWith(MockitoExtension.class)
class CompleteExpressionLearningUseCaseTest {

  private static final Long USER_ID = 1L;
  private static final Long SCENARIO_ID = 10L;
  private static final Locale TARGET_LOCALE = Locale.EN;
  private static final Locale BASE_LOCALE = Locale.KR;

  // 학습 순서: 201(1번) → 202(2번) → 203(3번)
  private static final Long UNLOCKED_EXPRESSION_ID = 201L;
  private static final Long LOCKED_EXPRESSION_ID = 203L;

  @Mock private WritingExpressionRepository writingExpressionRepository;

  @Mock private UserProfileService userProfileService;

  @Mock private UserWritingExpressionCompletionRepository userWritingExpressionCompletionRepository;

  @InjectMocks private CompleteExpressionLearningUseCase completeExpressionLearningUseCase;

  /** 없는(또는 INACTIVE) 표현을 완료하려 하면 RESOURCE_NOT_FOUND 예외를 던지고 아무것도 저장하지 않는다. */
  @Test
  void shouldThrowWhenExpressionNotFound() {
    // given: 해당 ID의 활성 표현이 없음
    when(writingExpressionRepository.findByIdAndStatus(UNLOCKED_EXPRESSION_ID, ActiveStatus.ACTIVE))
        .thenReturn(Optional.empty());

    // when & then
    assertThatThrownBy(
            () ->
                completeExpressionLearningUseCase.completeLearning(USER_ID, UNLOCKED_EXPRESSION_ID))
        .isInstanceOf(ApiException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);

    verify(userWritingExpressionCompletionRepository, never()).save(any());
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
    completeExpressionLearningUseCase.completeLearning(USER_ID, UNLOCKED_EXPRESSION_ID);

    // then: 완료 기록이 사용자/시나리오/표현 ID를 담아 저장된다
    ArgumentCaptor<UserWritingExpressionCompletion> captor =
        ArgumentCaptor.forClass(
            UserWritingExpressionCompletion.class); // captor = 메서드에 넘어온 인자를 담는 그릇

    // verify = 이 메서드가 진짜로 호출됐는지 확인해라.
    verify(userWritingExpressionCompletionRepository)
        .save(captor.capture()); // save()가 호출됐는지 확인 + 그때 넘어온 인자를 그릇에 담기

    UserWritingExpressionCompletion saved =
        captor.getValue(); // captor.getValue() = 잡아온 인자를 꺼내서 세부 검증
    assertThat(saved.getUserProfileId()).isEqualTo(USER_ID);
    assertThat(saved.getScenarioId()).isEqualTo(SCENARIO_ID);
    assertThat(saved.getWritingExpressionId()).isEqualTo(UNLOCKED_EXPRESSION_ID);
    assertThat(saved.getCompletedAt()).isNotNull();
  }

  /** 이미 완료한 표현을 다시 완료 요청하면(멱등) 예외 없이 정상 종료하고 새 기록을 저장하지 않는다. last_completed_at 필드만 갱신된다. */
  @Test
  void shouldUpdateLastCompletedAtForRepeatedCompletion() {
    // given: 표현이 존재하고, 사용자가 이미 그 표현을 완료한 상태
    WritingExpression expression = expressionInScenario();
    UserWritingExpressionCompletion completedRecord =
        completion(UNLOCKED_EXPRESSION_ID); // 표현 학습 이미 완료해서 userWritingExpressionCompletion이 존재함

    when(writingExpressionRepository.findByIdAndStatus(UNLOCKED_EXPRESSION_ID, ActiveStatus.ACTIVE))
        .thenReturn(Optional.of(expression));
    when(userWritingExpressionCompletionRepository.findAllByUserProfileIdAndScenarioId(
            USER_ID, SCENARIO_ID))
        .thenReturn(List.of(completedRecord));

    // when: 같은 표현을 다시 완료해도
    completeExpressionLearningUseCase.completeLearning(USER_ID, UNLOCKED_EXPRESSION_ID);

    // then: 새 완료 기록은 저장되지 않고(멱등), 기존 기록의 마지막 완료 시각만 갱신된다 (복습 시각 기록)
    verify(userWritingExpressionCompletionRepository, never()).save(any());
    verify(completedRecord).markCompletedAgain();
  }

  /** 아직 잠긴(미완료 중 학습 순서가 앞선 표현이 남은) 표현을 완료하려 하면 EXPRESSION_LOCKED 예외 + 경고 로그, 저장 없음. */
  @Test
  void shouldLogAndThrowWhenExpressionIsLocked() {
    // given: 로그 검증용 ListAppender 부착
    Logger logger =
        (Logger)
            LoggerFactory.getLogger(
                CompleteExpressionLearningUseCase
                    .class); // "CompleteExpressionLearningUseCase 클래스가 로그를 찍을 때 쓰는 그 Logger" 인스턴스를
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
            () -> completeExpressionLearningUseCase.completeLearning(USER_ID, LOCKED_EXPRESSION_ID))
        .isInstanceOf(ApiException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.EXPRESSION_LOCKED);

    // then: 저장 없음 + 어떤 사용자/표현이 막혔는지 warn 로그
    verify(userWritingExpressionCompletionRepository, never()).save(any());
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
    when(userWritingExpressionCompletionRepository.findAllByUserProfileIdAndScenarioId(
            USER_ID, SCENARIO_ID))
        .thenReturn(List.of()); // 빈리스트 = 해당 유저가 시나리오 id에서 아직 완료한 표현이 없음
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

  /** "이 표현을 완료했다"는 기록 mock. 완료 여부 판정에 쓰는 getWritingExpressionId만 스터빙한다. */
  private UserWritingExpressionCompletion completion(Long writingExpressionId) {
    UserWritingExpressionCompletion completion = mock(UserWritingExpressionCompletion.class);
    when(completion.getWritingExpressionId()).thenReturn(writingExpressionId);
    return completion;
  }
}
