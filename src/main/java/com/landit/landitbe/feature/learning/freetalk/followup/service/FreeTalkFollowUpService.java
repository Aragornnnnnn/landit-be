// 스몰톡 요약 마지막에 보여 줄 후속 질문을 세션마다 한 번 저장한다.

package com.landit.landitbe.feature.learning.freetalk.followup.service;

import com.landit.landitbe.feature.learning.freetalk.followup.domain.FreeTalkFollowUp;
import com.landit.landitbe.feature.learning.freetalk.followup.domain.FreeTalkFollowUpTriggerType;
import com.landit.landitbe.feature.learning.freetalk.followup.repository.FreeTalkFollowUpRepository;
import com.landit.landitbe.feature.memory.dto.ConversationMemoryFollowUpDraft;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 스몰톡 요약 마지막에 보여 줄 후속 질문을 세션마다 한 번 저장한다. */
@Slf4j
@RequiredArgsConstructor
@Service
public class FreeTalkFollowUpService {

  private final FreeTalkFollowUpRepository followUpRepository;

  /**
   * 사용자가 지금까지 받은 후속 질문들이 근거로 쓴 장기기억 ID를 조회한다.
   *
   * @param userProfileId 사용자 프로필 ID
   * @return 이미 질문의 근거로 쓴 장기기억 ID 목록
   */
  @Transactional(readOnly = true)
  public List<Long> findUsedMemoryIds(long userProfileId) {
    return followUpRepository.findUsedMemoryIds(userProfileId);
  }

  /**
   * 장기기억 저장과 같은 트랜잭션에서 후속 질문을 저장한다.
   *
   * <p>후속 질문은 덤이므로 어떤 경우에도 기억 저장을 실패시키지 않는다. 저장할 수 없는 질문은 사유만 남기고 건너뛴다. 근거 후보가 기억으로 저장되지
   * 않았으면(IGNORE) 문구는 남기고 근거 기억만 비운다.
   *
   * @param userProfileId 질문을 받을 사용자 프로필 ID
   * @param freeTalkSessionId 질문을 만든 프리톡 세션 ID
   * @param draft 구조 검증을 통과한 후속 질문. 없으면 null
   * @param savedMemoryIdsByPlanIndex 저장 계획 순번별 새 기억 ID
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public void record(
      long userProfileId,
      long freeTalkSessionId,
      ConversationMemoryFollowUpDraft draft,
      Map<Integer, Long> savedMemoryIdsByPlanIndex) {
    if (draft == null) {
      return;
    }
    FreeTalkFollowUpTriggerType triggerType = triggerTypeOf(draft.triggerType());
    if (triggerType == null) {
      warnSkipped("unknown_trigger_type", freeTalkSessionId);
      return;
    }
    // 같은 세션의 질문이 이미 있으면 유일 제약 위반이 기억 저장 트랜잭션까지 되돌리므로 먼저 확인한다.
    if (followUpRepository.findByFreeTalkSessionId(freeTalkSessionId).isPresent()) {
      warnSkipped("already_recorded", freeTalkSessionId);
      return;
    }
    followUpRepository.save(
        FreeTalkFollowUp.of(
            userProfileId,
            freeTalkSessionId,
            memoryIdOf(draft, savedMemoryIdsByPlanIndex),
            triggerType,
            draft.question(),
            draft.invite()));
  }

  // 근거가 기존 기억이면 그 ID를, 이번 후보면 방금 저장된 새 기억 ID를 쓴다.
  private static Long memoryIdOf(
      ConversationMemoryFollowUpDraft draft, Map<Integer, Long> savedMemoryIdsByPlanIndex) {
    if (draft.memoryId() != null) {
      return draft.memoryId();
    }
    if (draft.planIndex() == null) {
      return null;
    }
    return savedMemoryIdsByPlanIndex.get(draft.planIndex());
  }

  // AI 서버가 계기를 새로 추가해도 기억 저장은 계속돼야 하므로 모르는 값은 예외 대신 null로 돌려준다.
  private static FreeTalkFollowUpTriggerType triggerTypeOf(String triggerType) {
    for (FreeTalkFollowUpTriggerType candidate : FreeTalkFollowUpTriggerType.values()) {
      if (candidate.name().equals(triggerType)) {
        return candidate;
      }
    }
    return null;
  }

  // 문구는 사용자 발화에서 나온 내용이라 로그에 남기지 않는다.
  private static void warnSkipped(String reason, long freeTalkSessionId) {
    log.warn(
        "workflow=free_talk_follow_up_skipped reason={} freeTalkSessionId={}",
        reason,
        freeTalkSessionId);
  }
}
