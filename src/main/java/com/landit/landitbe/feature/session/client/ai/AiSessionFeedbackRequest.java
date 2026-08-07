// AI 세션 최종 피드백 생성을 요청하는 본문을 표현한다.

package com.landit.landitbe.feature.session.client.ai;

import java.util.List;

/**
 * AI 세션 최종 피드백 생성을 요청하는 본문을 표현한다.
 *
 * @param sessionId 학습 세션 ID
 * @param scenario AI 요청용 시나리오 컨텍스트
 * @param expectedMessageIds 피드백을 생성할 메시지 ID 목록
 */
public record AiSessionFeedbackRequest(
    Long sessionId, AiScenarioContext scenario, List<Long> expectedMessageIds) {}
