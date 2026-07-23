// 사용자 발화 저장 결과와 AI 요청에 필요한 컨텍스트를 전달한다.

package com.landit.landitbe.feature.session.service;

import com.landit.landitbe.feature.content.dto.NextQuestionContext;
import com.landit.landitbe.feature.session.client.ai.AiConversationHistoryMessage;
import com.landit.landitbe.feature.session.repository.projection.ScenarioSessionMessageContextProjection;
import java.util.List;
import java.util.Optional;

/**
 * 사용자 발화 저장 결과와 AI 요청에 필요한 컨텍스트를 전달한다.
 *
 * @param userId 발화를 제출한 사용자 ID
 * @param sessionId 발화가 속한 세션 ID
 * @param learningSessionId 시나리오 학습 세션 ID
 * @param sessionHistoryId 발화를 저장한 세션 이력 ID
 * @param submittedMessageId 저장된 사용자 발화 ID
 * @param submittedMessageSequence 세션 이력 내 사용자 발화 순번
 * @param submittedTurnNumber 사용자 발화의 대화 턴 번호
 * @param scenarioContext 시나리오 세션과 현재 질문 컨텍스트
 * @param conversationHistory AI 요청에 포함할 이전 대화 이력
 * @param nextQuestion 다음 고정 질문. 남은 질문이 없으면 빈 값
 * @param createdSessionHistory 이번 요청에서 세션 이력을 새로 생성했는지 여부
 */
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
    Optional<NextQuestionContext> nextQuestion,
    boolean createdSessionHistory) {}
