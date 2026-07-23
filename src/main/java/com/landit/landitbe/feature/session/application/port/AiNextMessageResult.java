// AI 다음 메시지 생성 결과를 표현한다.

package com.landit.landitbe.feature.session.application.port;

import com.landit.landitbe.feature.session.domain.GoalCompletionStatus;

/** AI 다음 메시지 생성 결과를 표현한다. */
public record AiNextMessageResult(
    String aiMessage, String translatedMessage, GoalCompletionStatus goalCompletionStatus) {}
