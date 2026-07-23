// 사용자 발화 저장 결과와 AI 요청에 필요한 컨텍스트를 전달한다.

package com.landit.landitbe.feature.session.application;

import com.landit.landitbe.feature.content.infrastructure.ScenarioQuestionRow;
import com.landit.landitbe.feature.session.application.port.AiConversationHistoryMessage;
import com.landit.landitbe.feature.session.infrastructure.ScenarioSessionMessageContextRow;
import java.util.List;
import java.util.Optional;

/** 사용자 발화 저장 결과와 AI 요청에 필요한 컨텍스트를 전달한다. */
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
