// 사용자 메시지와 평가 컨텍스트를 보관한다.

package com.landit.landitbe.feature.session.service;

import com.landit.landitbe.feature.content.domain.ResponseDemand;
import com.landit.landitbe.feature.session.client.ai.AiMessageFeedbackEvaluationContext;
import java.util.List;

/** 사용자 메시지와 평가 컨텍스트를 보관한다. */
record UserMessageContext(
    Long messageId,
    int turnNumber,
    String content,
    AiMessageFeedbackEvaluationContext evaluationContext,
    ResponseDemand responseDemand,
    List<String> requiredElements) {}
