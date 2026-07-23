// 사용자 발화 저장 결과와 AI 요청에 필요한 컨텍스트를 전달한다.

package com.landit.landitbe.feature.session.service;

import com.landit.landitbe.feature.content.repository.projection.ScenarioQuestionProjection;
import com.landit.landitbe.feature.session.client.ai.AiConversationHistoryMessage;
import com.landit.landitbe.feature.session.repository.projection.ScenarioSessionMessageContextProjection;
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
    ScenarioSessionMessageContextProjection scenarioContext,
    List<AiConversationHistoryMessage> conversationHistory,
    Optional<ScenarioQuestionProjection> nextQuestion,
    boolean createdSessionHistory) {}
