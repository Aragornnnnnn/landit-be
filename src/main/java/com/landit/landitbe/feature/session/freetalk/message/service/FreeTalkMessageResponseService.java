// 프리톡 처리 결과와 저장된 재요청 결과의 응답을 구성한다.

package com.landit.landitbe.feature.session.freetalk.message.service;

import com.landit.landitbe.feature.session.freetalk.domain.FreeTalkConversationStatus;
import com.landit.landitbe.feature.session.freetalk.domain.FreeTalkSession;
import com.landit.landitbe.feature.session.freetalk.domain.FreeTalkTurnStatus;
import com.landit.landitbe.feature.session.freetalk.expression.domain.ExpressionGenerationStatus;
import com.landit.landitbe.feature.session.freetalk.message.dto.FreeTalkMessageSubmitResponse;
import com.landit.landitbe.feature.session.freetalk.message.dto.FreeTalkMessageSubmitResponse.NextMessageResponse;
import com.landit.landitbe.feature.session.freetalk.message.dto.FreeTalkMessageSubmitResponse.ProgressResponse;
import com.landit.landitbe.feature.session.freetalk.message.dto.FreeTalkMessageSubmitResponse.SubmittedMessageResponse;
import com.landit.landitbe.feature.session.freetalk.usage.dto.DailySpeakingUsage;
import com.landit.landitbe.feature.session.freetalk.usage.service.FreeTalkDailySpeakingUsageService;
import com.landit.landitbe.feature.session.history.domain.SessionHistoryMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 프리톡 처리 결과와 저장된 재요청 결과의 응답을 구성한다. */
@Service
@RequiredArgsConstructor
class FreeTalkMessageResponseService {

  private final FreeTalkDailySpeakingUsageService dailySpeakingUsageService;

  FreeTalkMessageSubmitResponse buildResponse(
      long learningSessionId,
      FreeTalkSession session,
      FreeTalkTurnStatus turnStatus,
      SessionHistoryMessage userMessage,
      SessionHistoryMessage aiMessage,
      long userId) {
    DailySpeakingUsage dailyUsage = dailySpeakingUsageService.usage(userId);
    return new FreeTalkMessageSubmitResponse(
        learningSessionId,
        session.getTitle(),
        turnStatus,
        SubmittedMessageResponse.from(userMessage),
        aiMessage == null ? null : NextMessageResponse.from(aiMessage),
        new ProgressResponse(
            session.getConversationStatus(),
            session.getAccumulatedSpeakingDurationMs(),
            dailySpeakingUsageService.speakingTimeLimitMs(),
            dailyUsage.usedSpeakingDurationMs(),
            dailyUsage.remainingMs(),
            session.getExpressionGenerationStatus()));
  }

  FreeTalkMessageSubmitResponse buildReplayResponse(
      long learningSessionId,
      String title,
      FreeTalkTurnStatus turnStatus,
      SessionHistoryMessage userMessage,
      SessionHistoryMessage aiMessage,
      FreeTalkConversationStatus conversationStatus,
      long accumulatedSpeakingDurationMs,
      long userId,
      ExpressionGenerationStatus expressionGenerationStatus) {
    DailySpeakingUsage dailyUsage = dailySpeakingUsageService.usage(userId);
    return new FreeTalkMessageSubmitResponse(
        learningSessionId,
        title,
        turnStatus,
        SubmittedMessageResponse.from(userMessage),
        aiMessage == null ? null : NextMessageResponse.from(aiMessage),
        new ProgressResponse(
            conversationStatus,
            accumulatedSpeakingDurationMs,
            dailySpeakingUsageService.speakingTimeLimitMs(),
            dailyUsage.usedSpeakingDurationMs(),
            dailyUsage.remainingMs(),
            expressionGenerationStatus));
  }
}
