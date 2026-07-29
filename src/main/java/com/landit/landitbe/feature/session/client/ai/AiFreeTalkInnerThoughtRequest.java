// 프리톡 사용자 발화의 비동기 속마음 생성 요청을 담는다.

package com.landit.landitbe.feature.session.client.ai;

import java.util.List;

/** 프리톡 사용자 발화의 비동기 속마음 생성 요청을 담는다. */
public record AiFreeTalkInnerThoughtRequest(
    Long sessionId,
    Long submittedMessageId,
    int submittedTurnNumber,
    String targetLocale,
    String baseLocale,
    AiFreeTalkTopic topic,
    List<AiConversationHistoryMessage> conversationHistory) {}
