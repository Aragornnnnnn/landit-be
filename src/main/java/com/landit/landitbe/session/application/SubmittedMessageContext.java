// 사용자 발화 저장 결과와 AI 요청에 필요한 컨텍스트를 전달한다.
package com.landit.landitbe.session.application;

import com.landit.landitbe.content.infrastructure.ScenarioQuestionRow;
import com.landit.landitbe.session.application.port.AiConversationHistoryMessage;
import com.landit.landitbe.session.infrastructure.ScenarioSessionMessageContextRow;
import java.util.List;
import java.util.Optional;

record SubmittedMessageContext(
    long userId,
    long sessionId,
    Long learningSessionId,
    Long sessionHistoryId,
    Long submittedMessageId,
    int submittedMessageSequence,
    int submittedTurnNumber,
    ScenarioSessionMessageContextRow scenarioContext,
    List<AiConversationHistoryMessage> conversationHistory,
    Optional<ScenarioQuestionRow> nextQuestion,
    boolean createdSessionHistory) {}
