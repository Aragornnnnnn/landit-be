// 프리톡 세션의 대화 상태와 사용자 발화 시간 전이를 검증한다.

package com.landit.landitbe.feature.session.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/** 프리톡 세션의 대화 상태와 사용자 발화 시간 전이를 검증한다. */
class FreeTalkSessionTest {

  /** 종료 의사 확인을 취소하면 다시 대화를 이어갈 수 있다. */
  @Test
  void returnsToInProgressWhenExitDecisionIsContinue() {
    FreeTalkSession session = newSession();

    session.awaitExitDecision();
    session.continueConversation();

    assertThat(session.getConversationStatus()).isEqualTo(FreeTalkConversationStatus.IN_PROGRESS);
  }

  /** 진행 중 세션은 사용자의 종료 확정으로 완료된다. */
  @Test
  void completesWhenUserConfirmsExit() {
    FreeTalkSession session = newSession();

    session.completeByUserExit();

    assertThat(session.getConversationStatus()).isEqualTo(FreeTalkConversationStatus.COMPLETED);
    assertThat(session.getExpressionGenerationStatus())
        .isEqualTo(ExpressionGenerationStatus.PREPARING);
  }

  /** 종료 확인 대기 세션도 사용자의 종료 확정으로 완료된다. */
  @Test
  void completesWhenAwaitingExitDecisionIsConfirmed() {
    FreeTalkSession session = newSession();
    session.awaitExitDecision();

    session.completeByUserExit();

    assertThat(session.getConversationStatus()).isEqualTo(FreeTalkConversationStatus.COMPLETED);
  }

  /** 같은 세션의 표현 생성 작업을 두 번 선점할 수 없다. */
  @Test
  void rejectsDuplicateExpressionGenerationStart() {
    FreeTalkSession session = newSession();
    session.completeByTimeLimit();
    session.startExpressionGeneration();

    assertThatIllegalStateException().isThrownBy(session::startExpressionGeneration);
  }

  /** 완료된 세션이 처음 기억 생성을 준비하면 준비 상태로 전환된다. */
  @Test
  void preparesMemoryGenerationOnlyAfterConversationCompletion() {
    FreeTalkSession session = newSession();

    session.prepareMemoryGeneration();

    assertThat(session.getMemoryGenerationStatus()).isNull();

    session.completeByTimeLimit();
    session.prepareMemoryGeneration();

    assertThat(session.getMemoryGenerationStatus()).isEqualTo(MemoryGenerationStatus.PREPARING);
    assertThat(session.getMemoryGenerationStartedAt()).isNull();
  }

  /** 기억 생성 작업은 준비 상태에서 한 번만 시작할 수 있다. */
  @Test
  void rejectsDuplicateMemoryGenerationStart() {
    FreeTalkSession session = newSession();
    session.completeByTimeLimit();
    session.prepareMemoryGeneration();

    session.startMemoryGeneration(LocalDateTime.now());

    assertThat(session.getMemoryGenerationStartedAt()).isNotNull();
    assertThatIllegalStateException()
        .isThrownBy(() -> session.startMemoryGeneration(LocalDateTime.now()));
  }

  /** 시작 시각 없이 기억 생성을 완료할 수 없다. */
  @Test
  void rejectsMemoryGenerationCompletionBeforeStart() {
    FreeTalkSession session = newSession();
    session.completeByTimeLimit();
    session.prepareMemoryGeneration();

    assertThatIllegalStateException().isThrownBy(session::completeMemoryGeneration);
  }

  /** 시작된 기억 생성 작업을 완료하면 준비 시각을 지우고 완료 상태로 전환한다. */
  @Test
  void completesStartedMemoryGeneration() {
    FreeTalkSession session = newSession();
    session.completeByTimeLimit();
    session.prepareMemoryGeneration();
    session.startMemoryGeneration(LocalDateTime.now());

    session.completeMemoryGeneration();

    assertThat(session.getMemoryGenerationStatus()).isEqualTo(MemoryGenerationStatus.READY);
    assertThat(session.getMemoryGenerationStartedAt()).isNull();
  }

  /** 시작된 기억 생성 작업을 실패 처리하면 준비 시각을 지우고 실패 상태로 전환한다. */
  @Test
  void failsStartedMemoryGeneration() {
    FreeTalkSession session = newSession();
    session.completeByTimeLimit();
    session.prepareMemoryGeneration();
    session.startMemoryGeneration(LocalDateTime.now());

    session.failMemoryGeneration();

    assertThat(session.getMemoryGenerationStatus()).isEqualTo(MemoryGenerationStatus.FAILED);
    assertThat(session.getMemoryGenerationStartedAt()).isNull();
  }

  /** 아직 선점되지 않은 준비 작업도 실패 상태로 정리할 수 있다. */
  @Test
  void failsPreparedMemoryGenerationBeforeStart() {
    FreeTalkSession session = newSession();
    session.completeByTimeLimit();
    session.prepareMemoryGeneration();

    session.failMemoryGeneration();

    assertThat(session.getMemoryGenerationStatus()).isEqualTo(MemoryGenerationStatus.FAILED);
    assertThat(session.getMemoryGenerationStartedAt()).isNull();
  }

  /** 완료된 세션은 더 이상 대화 상태를 변경할 수 없다. */
  @Test
  void rejectsConversationStateChangesAfterCompletion() {
    FreeTalkSession session = newSession();
    session.completeByTimeLimit();

    assertThatIllegalStateException().isThrownBy(session::awaitExitDecision);
  }

  /** 사용자 발화 시간은 0 이상인 값만 누적한다. */
  @Test
  void accumulatesOnlyNonNegativeSpeakingDuration() {
    FreeTalkSession session = newSession();

    session.addSpeakingDuration(4_200);

    assertThat(session.getAccumulatedSpeakingDurationMs()).isEqualTo(4_200);
    assertThatIllegalStateException().isThrownBy(() -> session.addSpeakingDuration(-1));
  }

  /** 완료된 프리톡 세션에는 사용자 발화 시간을 더할 수 없다. */
  @Test
  void rejectsSpeakingDurationAfterCompletion() {
    FreeTalkSession session = newSession();
    session.completeByTimeLimit();

    assertThatIllegalStateException().isThrownBy(() -> session.addSpeakingDuration(1));
  }

  private FreeTalkSession newSession() {
    return FreeTalkSession.start(10L, 20L, FreeTalkStartMode.AI_FIRST);
  }
}
