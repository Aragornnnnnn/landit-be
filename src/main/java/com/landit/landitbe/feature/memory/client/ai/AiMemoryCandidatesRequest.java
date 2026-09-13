// 장기기억 후보 추출 AI 요청을 담는다.

package com.landit.landitbe.feature.memory.client.ai;

import java.util.List;

/** 장기기억 후보 추출에 필요한 프리톡 세션 문맥을 담는다. */
public record AiMemoryCandidatesRequest(
    Long sessionId,
    String characterId,
    String targetLocale,
    String baseLocale,
    String timezone,
    List<ConversationMemoryHistoryMessage> conversationHistory) {}
