// 후속 질문 조회의 상태 구분과, 저장할 수 없는 질문이 예외 없이 건너뛰어지는지 검증한다.

package com.landit.landitbe.feature.learning.freetalk.followup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.learning.freetalk.followup.domain.FreeTalkFollowUp;
import com.landit.landitbe.feature.learning.freetalk.followup.domain.FreeTalkFollowUpTriggerType;
import com.landit.landitbe.feature.learning.freetalk.followup.dto.FreeTalkFollowUpSummary;
import com.landit.landitbe.feature.learning.freetalk.followup.repository.FreeTalkFollowUpRepository;
import com.landit.landitbe.feature.learning.freetalk.memory.domain.MemoryGenerationStatus;
import com.landit.landitbe.feature.memory.dto.ConversationMemoryFollowUpDraft;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/** 후속 질문 조회의 상태 구분과, 저장할 수 없는 질문이 예외 없이 건너뛰어지는지 검증한다. */
class FreeTalkFollowUpServiceTest {

  private static final long SESSION_ID = 30L;

  private final FreeTalkFollowUpRepository repository = mock(FreeTalkFollowUpRepository.class);
  private final FreeTalkFollowUpService service = new FreeTalkFollowUpService(repository);

  @DisplayName("질문이 없고 장기기억 작업이 아직 준비 중이면 기다리는 중으로 알린다.")
  @Test
  void waitsWhileMemoryGenerationIsPreparing() {
    when(repository.findByFreeTalkSessionId(SESSION_ID)).thenReturn(Optional.empty());

    assertThat(service.findSummary(SESSION_ID, MemoryGenerationStatus.PREPARING))
        .isEqualTo(FreeTalkFollowUpSummary.waiting());
  }

  @DisplayName("질문이 없고 작업이 끝났거나 실패했거나 작업 대상이 아니면 질문 없음으로 알린다.")
  @ParameterizedTest
  @NullSource
  @EnumSource(
      value = MemoryGenerationStatus.class,
      names = {"READY", "FAILED"})
  void reportsNoneWhenNothingMoreWillCome(MemoryGenerationStatus status) {
    when(repository.findByFreeTalkSessionId(SESSION_ID)).thenReturn(Optional.empty());

    assertThat(service.findSummary(SESSION_ID, status)).isEqualTo(FreeTalkFollowUpSummary.none());
  }

  @DisplayName("저장된 질문이 있으면 작업 상태와 상관없이 그 문구를 그대로 돌려준다.")
  @Test
  void returnsStoredFollowUpRegardlessOfStatus() {
    when(repository.findByFreeTalkSessionId(SESSION_ID))
        .thenReturn(
            Optional.of(
                FreeTalkFollowUp.of(
                    1207L,
                    SESSION_ID,
                    5504L,
                    FreeTalkFollowUpTriggerType.CONCERN,
                    "면접 준비, 어떻게 됐어?",
                    "다음엔 그 얘기 하자.")));

    assertThat(service.findSummary(SESSION_ID, MemoryGenerationStatus.PREPARING))
        .isEqualTo(
            new FreeTalkFollowUpSummary(
                false, FreeTalkFollowUpTriggerType.CONCERN, "면접 준비, 어떻게 됐어?", "다음엔 그 얘기 하자."));
  }

  @DisplayName("엔티티 규칙에 어긋나는 질문은 예외를 던지지 않고 사유만 남긴 채 건너뛴다.")
  @Test
  @ExtendWith(OutputCaptureExtension.class)
  void skipsDraftThatBreaksEntityRulesWithoutThrowing(CapturedOutput output) {
    when(repository.findByFreeTalkSessionId(SESSION_ID)).thenReturn(Optional.empty());
    ConversationMemoryFollowUpDraft noneWithMemory =
        new ConversationMemoryFollowUpDraft(5504L, null, "NONE", "비밀질문", "비밀초대");

    assertThatCode(() -> service.record(1207L, SESSION_ID, noneWithMemory, Map.of()))
        .doesNotThrowAnyException();

    verify(repository, never()).save(any());
    assertThat(output.getOut())
        .contains("workflow=free_talk_follow_up_skipped reason=invalid_draft")
        .doesNotContain("비밀");
  }

  @DisplayName("후속 질문이 없으면 아무것도 조회하거나 저장하지 않는다.")
  @Test
  void doesNothingWithoutDraft() {
    service.record(1207L, SESSION_ID, null, Map.of());

    verify(repository, never()).findByFreeTalkSessionId(any());
    verify(repository, never()).save(any());
  }
}
