// AI가 만든 후속 질문의 구조를 검증하고 근거를 기억 저장 계획과 연결한다.

package com.landit.landitbe.feature.memory.planning.service;

import com.landit.landitbe.feature.memory.client.ai.AiFreeTalkMemoryContext;
import com.landit.landitbe.feature.memory.dto.ConversationMemoryFollowUpDraft;
import com.landit.landitbe.feature.memory.planning.client.ai.AiMemoryCandidatesResult;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 후속 질문은 부가 결과라 계약과 달라도 기억 저장을 막지 않는다. 임의 값으로 고치지 않고 질문만 버린 뒤 이유를 남긴다.
 *
 * <p>계기 값 자체가 아는 값인지는 질문을 받아 쓰는 기능이 판단한다. 여기서는 근거와 문구의 구조만 본다.
 */
@Slf4j
@Component
class ConversationMemoryFollowUpResolver {

  private static final String NO_FOLLOW_UP_TRIGGER = "NONE";

  /**
   * 후속 질문을 검증해 저장 계획 순번과 연결한다.
   *
   * @param sessionId 로그에 남길 학습 세션 ID
   * @param followUp AI가 만든 후속 질문. 구버전 AI 서버 응답이면 null
   * @param existingMemories 이번 요청에 실어 보낸 기존 장기기억
   * @param candidates 저장 계획과 같은 순서의 이번 기억 후보
   * @return 검증을 통과한 후속 질문. 없거나 계약과 다르면 null
   */
  ConversationMemoryFollowUpDraft resolve(
      long sessionId,
      AiMemoryCandidatesResult.FollowUpQuestion followUp,
      List<AiFreeTalkMemoryContext> existingMemories,
      List<FreeTalkMemoryCandidate> candidates) {
    if (followUp == null) {
      return null;
    }
    if (blank(followUp.triggerType()) || blank(followUp.question()) || blank(followUp.invite())) {
      return dropped(sessionId, "blank_text");
    }
    boolean hasMemory = followUp.memoryId() != null;
    boolean hasCandidate = followUp.candidateIndex() != null;
    if (NO_FOLLOW_UP_TRIGGER.equals(followUp.triggerType())) {
      return hasMemory || hasCandidate
          ? dropped(sessionId, "none_with_source")
          : draft(followUp, null, null);
    }
    if (hasMemory == hasCandidate) {
      return dropped(sessionId, "source_not_exactly_one");
    }
    if (hasMemory) {
      return existingMemories.stream()
              .anyMatch(memory -> followUp.memoryId().equals(memory.memoryId()))
          ? draft(followUp, followUp.memoryId(), null)
          : dropped(sessionId, "unknown_memory_id");
    }
    Integer planIndex = planIndexOf(followUp.candidateIndex(), candidates);
    return planIndex == null
        ? dropped(sessionId, "unknown_candidate_index")
        : draft(followUp, null, planIndex);
  }

  // 저장 계획은 후보와 같은 순서로 만들어지므로 후보의 위치가 곧 계획의 순번이다.
  private static Integer planIndexOf(int candidateIndex, List<FreeTalkMemoryCandidate> candidates) {
    for (int position = 0; position < candidates.size(); position++) {
      if (candidates.get(position).candidateIndex() == candidateIndex) {
        return position;
      }
    }
    return null;
  }

  private static ConversationMemoryFollowUpDraft draft(
      AiMemoryCandidatesResult.FollowUpQuestion followUp, Long memoryId, Integer planIndex) {
    return new ConversationMemoryFollowUpDraft(
        memoryId,
        planIndex,
        followUp.triggerType(),
        followUp.question().strip(),
        followUp.invite().strip());
  }

  private static ConversationMemoryFollowUpDraft dropped(long sessionId, String reason) {
    log.warn(
        "후속 질문이 계약과 달라 질문만 버립니다. workflow=free_talk_follow_up_invalid reason={} sessionId={}",
        reason,
        sessionId);
    return null;
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }
}
