// AI 메시지별 피드백 요청 접수 결과를 표현한다.

package com.landit.landitbe.feature.session.client.ai;

import com.landit.landitbe.feature.session.domain.ProcessingStatus;

/**
 * AI 메시지별 피드백 요청 접수 결과를 표현한다.
 *
 * @param sessionId 학습 세션 ID
 * @param messageId 메시지 ID
 * @param feedbackStatus 피드백 처리 상태
 */
public record AiMessageFeedbackResult(
    Long sessionId, Long messageId, ProcessingStatus feedbackStatus) {}
