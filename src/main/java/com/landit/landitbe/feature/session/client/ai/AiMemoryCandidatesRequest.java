// 장기기억 후보 추출 AI 요청을 담는다.

package com.landit.landitbe.feature.session.client.ai;

import java.util.List;

/**
 * 장기기억 후보 추출에 필요한 프리톡 세션 문맥을 담는다.
 *
 * @param sessionId 프리톡 세션 ID
 * @param characterId 프리톡 캐릭터 ID
 * @param targetLocale 학습 언어 지역
 * @param baseLocale 기준 언어 지역
 * @param timezone 세션 시간대
 * @param conversationHistory 시간 순서가 보존된 대화 히스토리
 */
public record AiMemoryCandidatesRequest(
    Long sessionId,
    String characterId,
    String targetLocale,
    String baseLocale,
    String timezone,
    List<AiConversationHistoryMessage> conversationHistory) {}
