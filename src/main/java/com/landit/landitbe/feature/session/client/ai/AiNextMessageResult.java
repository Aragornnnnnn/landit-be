// AI 다음 메시지 생성 결과를 표현한다.

package com.landit.landitbe.feature.session.client.ai;

import com.landit.landitbe.feature.session.domain.GoalCompletionStatus;

/**
 * AI 다음 메시지 생성 결과를 표현한다.
 *
 * @param aiMessage AI가 생성한 메시지
 * @param translatedMessage 번역된 AI 메시지
 * @param goalCompletionStatus 대화 목표 달성 상태
 */
public record AiNextMessageResult(
    String aiMessage, String translatedMessage, GoalCompletionStatus goalCompletionStatus) {}
