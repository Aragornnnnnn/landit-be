// 스몰톡 요약 마지막에 보여 줄 후속 질문을 세션마다 한 번 저장한다.

package com.landit.landitbe.feature.learning.freetalk.followup.service;

import com.landit.landitbe.feature.learning.freetalk.followup.domain.FreeTalkFollowUp;
import com.landit.landitbe.feature.learning.freetalk.followup.domain.FreeTalkFollowUpTriggerType;
import com.landit.landitbe.feature.learning.freetalk.followup.dto.FreeTalkFollowUpSummary;
import com.landit.landitbe.feature.learning.freetalk.followup.repository.FreeTalkFollowUpRepository;
import com.landit.landitbe.feature.learning.freetalk.memory.domain.MemoryGenerationStatus;
import com.landit.landitbe.feature.memory.dto.ConversationMemoryFollowUpDraft;
import com.landit.landitbe.feature.memory.service.ConversationMemoryWriteService;
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
  private final ConversationMemoryWriteService memoryWriteService;

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
   * 요약 화면에 보여 줄 후속 질문을 조회한다. 세션 소유권은 호출하는 쪽이 먼저 확인한다.
   *
   * <p>후속 질문은 세션 종료 후 장기기억 작업이 만들기 때문에, 그 작업이 아직 끝나지 않았고 질문도 없으면 기다리는 중으로 알린다.
   *
   * @param freeTalkSessionId 프리톡 세션 ID
   * @param memoryGenerationStatus 그 세션의 장기기억 작업 상태. 작업 대상이 아니면 null
   * @return 저장된 질문, 기다리는 중, 질문 없음 중 하나
   */
  @Transactional(readOnly = true)
  public FreeTalkFollowUpSummary findSummary(
      long freeTalkSessionId, MemoryGenerationStatus memoryGenerationStatus) {
    return followUpRepository
        .findByFreeTalkSessionId(freeTalkSessionId)
        .map(FreeTalkFollowUpSummary::of)
        .orElseGet(
            () ->
                memoryGenerationStatus == MemoryGenerationStatus.PREPARING
                    ? FreeTalkFollowUpSummary.waiting()
                    : FreeTalkFollowUpSummary.none());
  }

  /**
   * 장기기억 저장과 같은 트랜잭션에서 후속 질문을 저장한다.
   *
   * <p>후속 질문은 덤이므로 어떤 경우에도 기억 저장을 실패시키지 않는다. 저장할 수 없는 질문은 사유만 남기고 건너뛴다. 근거 후보가 기억으로 저장되지
   * 않았으면(IGNORE) 문구는 남기고 근거 기억만 비운다.
   *
   * <p>기존 기억이 근거면 기억 저장을 마친 지금도 활성인지 다시 본다. 이번 저장 계획이나 그사이 끝난 다른 작업이 그 기억을 대체했다면("면접 준비 중" → "면접
   * 취소") 옛 내용으로 만든 질문은 틀린 기록이 되므로 남기지 않는다.
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
    if (draft.memoryId() != null
        && !memoryWriteService.isActiveAfterPersistence(userProfileId, draft.memoryId())) {
      warnSkipped("source_memory_not_active", freeTalkSessionId);
      return;
    }
    // 같은 세션의 질문이 이미 있으면 유일 제약 위반이 기억 저장 트랜잭션까지 되돌리므로 먼저 확인한다.
    if (followUpRepository.findByFreeTalkSessionId(freeTalkSessionId).isPresent()) {
      warnSkipped("already_recorded", freeTalkSessionId);
      return;
    }
    FreeTalkFollowUp followUp;
    try {
      followUp =
          FreeTalkFollowUp.of(
              userProfileId,
              freeTalkSessionId,
              memoryIdOf(draft, savedMemoryIdsByPlanIndex),
              triggerType,
              draft.question(),
              draft.invite());
    } catch (IllegalArgumentException exception) {
      // 검증은 기억 기능이 먼저 하지만, 그 검증이 바뀌어도 질문 하나가 기억 저장을 되돌리지 않게 한다.
      warnSkipped("invalid_draft", freeTalkSessionId);
      return;
    }
    followUpRepository.save(followUp);
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
