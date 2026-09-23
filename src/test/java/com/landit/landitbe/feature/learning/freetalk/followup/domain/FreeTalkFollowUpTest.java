// 스몰톡 후속 질문이 만들어질 때 지키는 규칙을 검증한다.

package com.landit.landitbe.feature.learning.freetalk.followup.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 스몰톡 후속 질문의 생성 규칙을 검증한다. */
class FreeTalkFollowUpTest {

  @DisplayName("기억을 근거로 한 후속 질문은 근거 기억과 문구를 그대로 담는다.")
  @Test
  void createsFollowUpGroundedInMemory() {
    FreeTalkFollowUp followUp =
        FreeTalkFollowUp.of(
            1L, 30L, 42L, FreeTalkFollowUpTriggerType.CONCERN, "면접 준비, 어떻게 됐어?", "다음엔 그 얘기 하자.");

    assertThat(followUp.getUserProfileId()).isEqualTo(1L);
    assertThat(followUp.getFreeTalkSessionId()).isEqualTo(30L);
    assertThat(followUp.getMemoryId()).isEqualTo(42L);
    assertThat(followUp.getTriggerType()).isEqualTo(FreeTalkFollowUpTriggerType.CONCERN);
    assertThat(followUp.getQuestion()).isEqualTo("면접 준비, 어떻게 됐어?");
    assertThat(followUp.getInvite()).isEqualTo("다음엔 그 얘기 하자.");
  }

  @DisplayName("물어볼 기억이 없는 기본 문구는 근거 기억 없이 만든다.")
  @Test
  void createsDefaultFollowUpWithoutMemory() {
    FreeTalkFollowUp followUp =
        FreeTalkFollowUp.of(
            1L, 30L, null, FreeTalkFollowUpTriggerType.NONE, "요즘 빠져 있는 거 얘기해줘.", "기억해둘게.");

    assertThat(followUp.getMemoryId()).isNull();
  }

  @DisplayName("기본 문구에 근거 기억을 붙이거나 문구를 비우면 만들 수 없다.")
  @Test
  void rejectsDefaultWithMemoryAndBlankText() {
    assertThatThrownBy(
            () ->
                FreeTalkFollowUp.of(
                    1L, 30L, 42L, FreeTalkFollowUpTriggerType.NONE, "기본 문구", "기억해둘게."))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () -> FreeTalkFollowUp.of(1L, 30L, null, FreeTalkFollowUpTriggerType.MOOD, " ", "초대"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () -> FreeTalkFollowUp.of(1L, 30L, null, FreeTalkFollowUpTriggerType.MOOD, "질문", null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> FreeTalkFollowUp.of(1L, 30L, null, null, "질문", "초대"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
