// 스몰톡 메인에 보여 줄 캐릭터별 이어가기 질문을 제공한다.

package com.landit.landitbe.feature.learning.freetalk.followup.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 사용 가능한 이어가기 질문이다.
 *
 * @param characterId 질문을 만든 대화 상대
 * @param followUpId 서버가 다시 검증할 질문 ID
 * @param question 카드에 표시할 예고 질문
 */
@Schema(description = "지난 대화 이어가기 질문")
public record FreeTalkAvailableFollowUp(String characterId, long followUpId, String question) {}
