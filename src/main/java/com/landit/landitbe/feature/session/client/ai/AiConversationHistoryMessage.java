// AI 요청에 포함할 누적 대화 히스토리 메시지를 담는다.

package com.landit.landitbe.feature.session.client.ai;

/**
 * AI 요청에 포함할 누적 대화 히스토리 메시지를 담는다.
 *
 * @param messageId 메시지 ID
 * @param turnNumber 대화 턴 번호
 * @param role 메시지 발화자 역할
 * @param content 메시지 본문
 * @param translatedContent 번역된 메시지 본문
 */
public record AiConversationHistoryMessage(
    Long messageId, int turnNumber, String role, String content, String translatedContent) {}
