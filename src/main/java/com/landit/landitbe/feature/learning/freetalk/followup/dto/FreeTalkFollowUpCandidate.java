// 예고 질문과 질문을 만든 대화 상대를 함께 조회한다.

package com.landit.landitbe.feature.learning.freetalk.followup.dto;

import com.landit.landitbe.feature.learning.freetalk.followup.domain.FreeTalkFollowUp;

/**
 * 사용 가능한 질문 후보와 해당 캐릭터다.
 *
 * @param followUp 저장된 질문
 * @param characterId 질문을 만든 캐릭터 ID
 */
public record FreeTalkFollowUpCandidate(FreeTalkFollowUp followUp, String characterId) {}
