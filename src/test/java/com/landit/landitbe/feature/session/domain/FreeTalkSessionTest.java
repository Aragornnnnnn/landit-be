// 프리톡 세션의 대화 상태와 사용자 발화 시간 전이를 검증한다.

package com.landit.landitbe.feature.session.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

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
