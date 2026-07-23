// AI 종료 메시지 생성 결과를 표현한다.

package com.landit.landitbe.feature.session.client.ai;

import com.landit.landitbe.shared.domain.InnerThoughtType;

/**
 * AI 종료 메시지 생성 결과를 표현한다.
 *
 * @param aiMessage AI가 생성한 메시지
 * @param translatedMessage 번역된 AI 메시지
 * @param innerThought AI 속마음
 * @param innerThoughtType AI 속마음 유형
 */
public record AiClosingMessageResult(
    String aiMessage,
    String translatedMessage,
    String innerThought,
    InnerThoughtType innerThoughtType) {}
