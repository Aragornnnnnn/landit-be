// AI가 만든 후속 질문의 구조 검증과 저장 계획 연결을 검증한다.

package com.landit.landitbe.feature.memory.planning.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.landit.landitbe.feature.memory.client.ai.AiFreeTalkMemoryContext;
import com.landit.landitbe.feature.memory.domain.ConversationMemoryType;
import com.landit.landitbe.feature.memory.dto.ConversationMemoryFollowUpDraft;
import com.landit.landitbe.feature.memory.planning.client.ai.AiMemoryCandidatesResult.FollowUpQuestion;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/** 후속 질문의 구조 검증과 저장 계획 연결을 검증한다. */
class ConversationMemoryFollowUpResolverTest {

  private static final List<AiFreeTalkMemoryContext> EXISTING_MEMORIES =
      List.of(new AiFreeTalkMemoryContext(42L, ConversationMemoryType.EVENT, "다음 주에 면접이 있다."));
  // 후보 순번이 0부터 이어지지 않아도 저장 계획의 순번은 후보 목록에서의 위치다.
  private static final List<FreeTalkMemoryCandidate> CANDIDATES =
      List.of(
          new FreeTalkMemoryCandidate(3, null, null, List.of()),
          new FreeTalkMemoryCandidate(5, null, null, List.of()));

  private final ConversationMemoryFollowUpResolver resolver =
      new ConversationMemoryFollowUpResolver();

  @DisplayName("구버전 AI 서버 응답처럼 후속 질문이 없으면 없는 그대로 둔다.")
  @Test
  void returnsNullWhenAiDidNotSendFollowUp() {
    assertThat(resolver.resolve(300L, null, EXISTING_MEMORIES, CANDIDATES, 2)).isNull();
  }

  @DisplayName("기존 기억을 근거로 한 질문은 그 기억 ID를 그대로 담고 문구의 앞뒤 공백을 없앤다.")
  @Test
  void keepsExistingMemoryAsSource() {
    ConversationMemoryFollowUpDraft draft =
        resolver.resolve(
            300L,
            new FollowUpQuestion(42L, null, "CONCERN", " 면접 준비, 어떻게 됐어? ", "다음엔 그 얘기 하자. "),
            EXISTING_MEMORIES,
            CANDIDATES,
            CANDIDATES.size());

    assertThat(draft)
        .isEqualTo(
            new ConversationMemoryFollowUpDraft(
                42L, null, "CONCERN", "면접 준비, 어떻게 됐어?", "다음엔 그 얘기 하자."));
  }

  @DisplayName("이번 후보를 근거로 한 질문은 그 후보의 저장 계획 순번으로 연결한다.")
  @Test
  void linksCandidateSourceToPlanPosition() {
    ConversationMemoryFollowUpDraft draft =
        resolver.resolve(
            300L,
            new FollowUpQuestion(null, 5, "CUT_OFF", "아까 회사 얘기 하다 끊겼잖아.", "다음에 이어서 해줄래?"),
            EXISTING_MEMORIES,
            CANDIDATES,
            CANDIDATES.size());

    assertThat(draft.memoryId()).isNull();
    assertThat(draft.planIndex()).isEqualTo(1);
  }

  @DisplayName("물어볼 기억이 없는 기본 문구는 근거 없이 통과한다.")
  @Test
  void acceptsDefaultFollowUpWithoutSource() {
    ConversationMemoryFollowUpDraft draft =
        resolver.resolve(
            300L,
            new FollowUpQuestion(null, null, "NONE", "요즘 빠져 있는 거 얘기해줘.", "기억해둘게."),
            EXISTING_MEMORIES,
            CANDIDATES,
            CANDIDATES.size());

    assertThat(draft.memoryId()).isNull();
    assertThat(draft.planIndex()).isNull();
    assertThat(draft.triggerType()).isEqualTo("NONE");
  }

  @DisplayName("근거와 문구가 계약과 다르면 임의로 고치지 않고 질문만 버린 뒤 이유를 로그로 남긴다.")
  @ParameterizedTest(name = "{0}")
  @CsvSource(
      delimiter = '|',
      nullValues = "NULL",
      value = {
        "none_with_source        | 42   | NULL | NONE    | 비밀질문",
        "source_not_exactly_one  | 42   | 3    | CONCERN | 비밀질문",
        "source_not_exactly_one  | NULL | NULL | CONCERN | 비밀질문",
        "unknown_memory_id       | 99   | NULL | CONCERN | 비밀질문",
        "unknown_candidate_index | NULL | 4    | CUT_OFF | 비밀질문",
        "blank_text              | 42   | NULL | CONCERN | '   '",
        "blank_text              | 42   | NULL | NULL    | 비밀질문"
      })
  @ExtendWith(OutputCaptureExtension.class)
  void dropsFollowUpThatBreaksContract(
      String expectedReason,
      Long memoryId,
      Integer candidateIndex,
      String triggerType,
      String question,
      CapturedOutput output) {
    ConversationMemoryFollowUpDraft draft =
        resolver.resolve(
            300L,
            new FollowUpQuestion(memoryId, candidateIndex, triggerType, question, "비밀초대"),
            EXISTING_MEMORIES,
            CANDIDATES,
            CANDIDATES.size());

    assertThat(draft).isNull();
    assertThat(output.getOut())
        .contains("workflow=free_talk_follow_up_invalid reason=" + expectedReason)
        .contains("sessionId=300")
        .doesNotContain("비밀");
  }

  @DisplayName("화면 한 줄을 넘는 비정상적으로 긴 문구는 기록으로 남기지 않고 버린다.")
  @Test
  @ExtendWith(OutputCaptureExtension.class)
  void dropsFollowUpWithTooLongText(CapturedOutput output) {
    String tooLong = "가".repeat(201);

    assertThat(
            resolver.resolve(
                300L,
                new FollowUpQuestion(42L, null, "CONCERN", tooLong, "다음엔 그 얘기 하자."),
                EXISTING_MEMORIES,
                CANDIDATES,
                CANDIDATES.size()))
        .isNull();
    assertThat(
            resolver.resolve(
                300L,
                new FollowUpQuestion(42L, null, "CONCERN", "어떻게 됐어?", tooLong),
                EXISTING_MEMORIES,
                CANDIDATES,
                CANDIDATES.size()))
        .isNull();
    assertThat(
            resolver.resolve(
                300L,
                new FollowUpQuestion(42L, null, "CONCERN", "가".repeat(200), "다음엔 그 얘기 하자."),
                EXISTING_MEMORIES,
                CANDIDATES,
                CANDIDATES.size()))
        .isNotNull();
    assertThat(output.getOut()).contains("reason=text_too_long");
  }

  @DisplayName("저장 계획 수가 후보 수와 다르면 엉뚱한 기억에 연결될 수 있어 후보 근거 질문을 버린다.")
  @Test
  @ExtendWith(OutputCaptureExtension.class)
  void dropsCandidateSourceWhenPlansDoNotLineUpWithCandidates(CapturedOutput output) {
    FollowUpQuestion fromCandidate =
        new FollowUpQuestion(null, 5, "CUT_OFF", "아까 회사 얘기 하다 끊겼잖아.", "다음에 이어서 해줄래?");
    FollowUpQuestion fromMemory =
        new FollowUpQuestion(42L, null, "CONCERN", "면접 준비, 어떻게 됐어?", "다음엔 그 얘기 하자.");

    assertThat(resolver.resolve(300L, fromCandidate, EXISTING_MEMORIES, CANDIDATES, 1)).isNull();
    assertThat(output.getOut()).contains("reason=plan_count_mismatch");
    // 기존 기억이 근거면 계획 순번을 쓰지 않으므로 영향을 받지 않는다.
    assertThat(resolver.resolve(300L, fromMemory, EXISTING_MEMORIES, CANDIDATES, 1)).isNotNull();
  }
}
