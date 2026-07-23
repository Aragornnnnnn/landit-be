// AI 종료 메시지 생성 결과를 표현한다.

package com.landit.landitbe.feature.session.client.ai;

import com.landit.landitbe.shared.domain.InnerThoughtType;

/** AI 종료 메시지 생성 결과를 표현한다. */
public record AiClosingMessageResult(
    String aiMessage,
    String translatedMessage,
    String innerThought,
    InnerThoughtType innerThoughtType) {}
