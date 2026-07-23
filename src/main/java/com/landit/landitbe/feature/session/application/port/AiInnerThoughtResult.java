// AI 속마음 생성 결과를 표현한다.

package com.landit.landitbe.feature.session.application.port;

import com.landit.landitbe.shared.domain.InnerThoughtType;

/** AI 속마음 생성 결과를 표현한다. */
public record AiInnerThoughtResult(
    Long sessionId, Long messageId, String innerThought, InnerThoughtType innerThoughtType) {}
