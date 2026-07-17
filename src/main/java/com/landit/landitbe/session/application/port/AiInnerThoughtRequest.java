// AI 속마음 생성을 요청하는 대화 컨텍스트를 표현한다.

package com.landit.landitbe.session.application.port;

import java.util.List;

/** AI 속마음 생성을 요청하는 대화 컨텍스트를 표현한다. */
public record AiInnerThoughtRequest(
    Long sessionId,
    Long submittedMessageId,
    int submittedTurnNumber,
    AiScenarioContext scenario,
    List<AiConversationHistoryMessage> conversationHistory) {}
