// AI 메시지별 피드백에서 사용자 발화를 평가할 기준 정보를 담는다.

package com.landit.landitbe.feature.session.client.ai;

/**
 * AI 메시지별 피드백에서 사용자 발화를 평가할 기준 정보를 담는다.
 *
 * @param type 평가 컨텍스트 유형
 * @param content 메시지 본문
 * @param translatedContent 번역된 메시지 본문
 */
public record AiMessageFeedbackEvaluationContext(
    AiMessageFeedbackEvaluationContextType type, String content, String translatedContent) {}
