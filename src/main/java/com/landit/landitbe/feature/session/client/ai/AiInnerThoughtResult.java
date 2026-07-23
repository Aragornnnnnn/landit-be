// AI 속마음 생성 결과를 표현한다.

package com.landit.landitbe.feature.session.client.ai;

import com.landit.landitbe.shared.domain.InnerThoughtType;

/**
 * AI 속마음 생성 결과를 표현한다.
 *
 * @param sessionId 학습 세션 ID
 * @param messageId 메시지 ID
 * @param innerThought AI 속마음
 * @param innerThoughtType AI 속마음 유형
 */
public record AiInnerThoughtResult(
    Long sessionId, Long messageId, String innerThought, InnerThoughtType innerThoughtType) {}
