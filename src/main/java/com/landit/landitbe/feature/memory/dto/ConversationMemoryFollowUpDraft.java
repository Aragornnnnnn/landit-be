// AI가 만든 후속 질문을 기억 저장 계획과 연결해 세션 기능에 전달한다.

package com.landit.landitbe.feature.memory.dto;

/**
 * 구조 검증을 통과한 후속 질문이다. 근거는 기존 기억이거나 이번에 저장할 계획 중 하나이고, 기본 문구면 둘 다 없다.
 *
 * @param memoryId 근거가 된 기존 장기기억 ID. 아니면 null
 * @param planIndex 근거가 된 후보의 저장 계획 순번(0부터). 저장 뒤 새 기억 ID로 바꿔 읽는다. 아니면 null
 * @param triggerType AI가 분류한 계기. 값의 유효성은 받는 기능이 판단한다
 * @param question 질문 문구
 * @param invite 초대 문구
 */
public record ConversationMemoryFollowUpDraft(
    Long memoryId, Integer planIndex, String triggerType, String question, String invite) {}
