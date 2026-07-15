// AI 속마음 생성 결과를 표현한다.
package com.landit.landitbe.session.application.port;

import com.landit.landitbe.common.domain.InnerThoughtType;

public record AiInnerThoughtResult(
    Long sessionId, Long messageId, String innerThought, InnerThoughtType innerThoughtType) {}
