// 완료된 세션의 최종 피드백을 생성하거나 저장된 결과를 조회한다.

package com.landit.landitbe.feature.session.service;

import com.landit.landitbe.feature.session.client.ai.AiConversationClient;
import com.landit.landitbe.feature.session.client.ai.AiSessionFeedbackRequest;
import com.landit.landitbe.feature.session.client.ai.AiSessionFeedbackResult;
import com.landit.landitbe.feature.session.domain.SessionHistoryMessageFeedback;
import com.landit.landitbe.feature.session.domain.SessionHistorySummaryFeedback;
import com.landit.landitbe.feature.session.dto.SessionFeedbackResponse;
import com.landit.landitbe.feature.session.dto.SessionFeedbackResponse.EvaluationContextResponse;
import com.landit.landitbe.feature.session.dto.SessionFeedbackResponse.MessageFeedbackResponse;
import com.landit.landitbe.feature.session.repository.SessionHistoryMessageFeedbackRepository;
import com.landit.landitbe.feature.session.repository.SessionHistorySummaryFeedbackRepository;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 완료된 세션의 최종 피드백을 생성하거나 저장된 결과를 조회한다. */
@RequiredArgsConstructor
@Service
public class SessionFeedbackUseCase {

  private final SessionFeedbackContextLoader contextLoader;
  private final SessionFeedbackRecorder recorder;
  private final SessionHistoryMessageFeedbackRepository messageFeedbackRepository;
  private final SessionHistorySummaryFeedbackRepository summaryFeedbackRepository;
  private final AiConversationClient aiConversationClient;

  /** 완료된 세션의 최종 피드백을 생성하거나 기존 결과를 반환한다. */
  public SessionFeedbackResponse getOrCreate(long userId, long sessionId) {
    LoadedSessionFeedbackContext context = contextLoader.load(userId, sessionId);
    ExistingSummaryFeedbackContext existingSummary = context.existingSummary().orElse(null);
    if (existingSummary != null) {
      // 이미 확정된 결과는 AI를 다시 호출하지 않고 그대로 반환한다.
      return responseFor(context, existingSummary.summaryFeedbackId());
    }

    // 외부 AI 호출은 DB 트랜잭션 밖에서 수행한다.
    AiSessionFeedbackResult result =
        aiConversationClient.generateSessionFeedback(
            new AiSessionFeedbackRequest(
                context.sessionId(),
                context.scenario(),
                context.userMessages().stream().map(UserMessageContext::messageId).toList()));
    Long summaryFeedbackId = recorder.record(userId, context, result);
    return responseFor(context, summaryFeedbackId);
  }

  /** 저장된 최종 피드백과 평가 당시 사용자 메시지 컨텍스트를 API 응답으로 조립한다. */
  private SessionFeedbackResponse responseFor(
      LoadedSessionFeedbackContext context, Long summaryFeedbackId) {
    List<SessionHistoryMessageFeedback> feedbacks =
        messageFeedbackRepository
            .findBySessionHistorySummaryFeedbackIdOrderBySessionHistoryMessageIdAsc(
                summaryFeedbackId);
    if (feedbacks.size() != context.userMessages().size()) {
      throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
    }
    Map<Long, SessionHistoryMessageFeedback> feedbackByMessageId =
        feedbacks.stream()
            .collect(
                Collectors.toMap(
                    SessionHistoryMessageFeedback::getSessionHistoryMessageId,
                    Function.identity()));
    SessionHistorySummaryFeedback summary =
        summaryFeedbackRepository
            .findById(summaryFeedbackId)
            .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_SERVER_ERROR));

    return SessionFeedbackResponse.from(
        context.sessionId(),
        summary,
        context.userMessages().stream()
            .map(
                userMessage ->
                    messageFeedbackResponse(
                        feedbackByMessageId.get(userMessage.messageId()), userMessage))
            .toList());
  }

  /** 메시지별 피드백과 평가 기준을 FE가 표시할 단일 메시지 응답으로 변환한다. */
  private MessageFeedbackResponse messageFeedbackResponse(
      SessionHistoryMessageFeedback feedback, UserMessageContext userMessage) {
    if (feedback == null) {
      throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
    }
    return MessageFeedbackResponse.from(
        feedback,
        userMessage.turnNumber(),
        userMessage.content(),
        EvaluationContextResponse.from(
            userMessage.evaluationContext().type(),
            userMessage.evaluationContext().content(),
            userMessage.evaluationContext().translatedContent()));
  }
}
