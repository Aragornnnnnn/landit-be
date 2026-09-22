// 턴 교정 판정 결과가 다시 해 볼 실패와 끝난 실패를 구분하는지 검증한다.

package com.landit.landitbe.feature.learning.freetalk.feedback.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.landit.landitbe.feature.learning.conversation.domain.ProcessingStatus;
import com.landit.landitbe.feature.learning.freetalk.feedback.domain.FreeTalkMistakePattern;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 턴 교정 판정 결과가 다시 해 볼 실패와 끝난 실패를 구분하는지 검증한다. */
class FreeTalkTurnCorrectionTest {

  @DisplayName("AI가 판정을 돌려주지 못한 실패만 다시 시도할 수 있고, 계약 위반 실패와 완료된 판정은 다시 시도하지 않는다.")
  @Test
  void onlyUnavailableJudgmentIsRetryable() {
    assertThat(FreeTalkTurnCorrection.unavailable().status()).isEqualTo(ProcessingStatus.FAILED);
    assertThat(FreeTalkTurnCorrection.unavailable().retryable()).isTrue();
    assertThat(FreeTalkTurnCorrection.failed().retryable()).isFalse();
    assertThat(FreeTalkTurnCorrection.completed(null, true).retryable()).isFalse();
    // 저장된 교정을 읽어 만든 결과는 끝난 판정이라 다시 시도하지 않는다.
    assertThat(new FreeTalkTurnCorrection(ProcessingStatus.FAILED, null, null).retryable())
        .isFalse();
    // 둘은 값이 달라야 저장 계층이 실패를 확정할지 다음 시도로 넘길지 가를 수 있다.
    assertThat(FreeTalkTurnCorrection.unavailable()).isNotEqualTo(FreeTalkTurnCorrection.failed());
  }

  @DisplayName("실패가 아닌 판정을 다시 시도할 실패로 표시할 수 없다.")
  @Test
  void rejectsRetryableNonFailure() {
    assertThatThrownBy(
            () -> new FreeTalkTurnCorrection(ProcessingStatus.COMPLETED, null, true, true))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () -> new FreeTalkTurnCorrection(ProcessingStatus.PREPARING, null, null, true))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @DisplayName("사용례는 판정을 마친 결과에만 붙고, 실패 결과에 붙이면 거부한다.")
  @Test
  void allowsPatternUsagesOnlyOnCompletedJudgment() {
    FreeTalkPatternUsageDraft usage =
        new FreeTalkPatternUsageDraft(
            FreeTalkMistakePattern.TENSE, "I went to the gym.", "went", true);

    assertThat(FreeTalkTurnCorrection.completed(null, true, List.of(usage)).patternUsages())
        .containsExactly(usage);
    assertThat(FreeTalkTurnCorrection.completed(null, true).patternUsages()).isEmpty();
    assertThat(FreeTalkTurnCorrection.unavailable().patternUsages()).isEmpty();
    assertThatThrownBy(
            () ->
                new FreeTalkTurnCorrection(
                    ProcessingStatus.FAILED, null, null, false, List.of(usage)))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
