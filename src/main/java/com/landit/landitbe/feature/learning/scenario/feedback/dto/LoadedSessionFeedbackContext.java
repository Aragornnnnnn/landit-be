// 최종 피드백 생성에 필요한 완료 세션 데이터를 보관한다.

package com.landit.landitbe.feature.learning.scenario.feedback.dto;

import com.landit.landitbe.feature.content.domain.ContentLearningLevel;
import com.landit.landitbe.feature.learning.scenario.session.client.ai.AiScenarioContext;
import com.landit.landitbe.shared.domain.Locale;
import java.util.List;
import java.util.Optional;

/**
 * 최종 피드백 생성에 필요한 완료 세션 데이터를 보관한다.
 *
 * @param sessionId 완료 세션 ID
 * @param sessionHistoryId 대화 이력 ID
 * @param targetLocale 학습 언어
 * @param baseLocale 기준 언어
 * @param questionLevelGroup 세션 질문 수준
 * @param scenario AI 요청 시나리오
 * @param userMessages 평가할 사용자 발화
 * @param existingSummary 기존 최종 피드백
 */
public record LoadedSessionFeedbackContext(
    Long sessionId,
    Long sessionHistoryId,
    Locale targetLocale,
    Locale baseLocale,
    ContentLearningLevel questionLevelGroup,
    AiScenarioContext scenario,
    List<UserMessageContext> userMessages,
    Optional<ExistingSummaryFeedbackContext> existingSummary) {}
