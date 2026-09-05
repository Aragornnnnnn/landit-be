// 완료된 세션의 최종 피드백을 생성하거나 저장된 결과를 조회한다.

package com.landit.landitbe.feature.session.service;

import com.landit.landitbe.feature.session.client.ai.AiConversationClient;
import com.landit.landitbe.feature.session.client.ai.AiSessionFeedbackRequest;
import com.landit.landitbe.feature.session.client.ai.AiSessionFeedbackResult;
import com.landit.landitbe.feature.session.domain.SessionHistoryMessageFeedback;
import com.landit.landitbe.feature.session.domain.SessionHistorySummaryFeedback;
import com.landit.landitbe.feature.session.domain.SessionLevelAssessment;
import com.landit.landitbe.feature.session.domain.UserLevelAssessment;
import com.landit.landitbe.feature.session.dto.SessionFeedbackResponse;
import com.landit.landitbe.feature.session.dto.SessionFeedbackResponse.EvaluationContextResponse;
import com.landit.landitbe.feature.session.dto.SessionFeedbackResponse.MessageFeedbackResponse;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** 완료된 세션의 최종 피드백을 생성하거나 저장된 결과를 조회한다. */
@RequiredArgsConstructor
@Service
@Slf4j
public class SessionFeedbackService {

  private final SessionFeedbackContextService contextService;
  private final SessionFeedbackCompletionService completionService;
  private final SessionFeedbackDataService sessionFeedbackDataService;
  private final SessionLevelAssessmentService sessionLevelAssessmentService;
  private final AiConversationClient aiConversationClient;

  /**
   * 완료된 세션의 최종 피드백을 생성하거나 기존 결과를 반환한다.
   *
   * @param userId 세션 소유자 ID
   * @param sessionId 피드백을 조회할 학습 세션 ID
   * @return 세션 요약과 사용자 메시지별 피드백
   * @throws ApiException AI 결과나 저장된 피드백의 정합성이 맞지 않을 때
   */
  public SessionFeedbackResponse getOrCreate(long userId, long sessionId) {
    LoadedSessionFeedbackContext context = contextService.load(userId, sessionId);
    ExistingSummaryFeedbackContext existingSummary = context.existingSummary().orElse(null);
    if (existingSummary != null) {
      if (sessionLevelAssessmentService.findBySessionId(sessionId) == null) {
        completionService.attachLegacyFallback(userId, context);
      }
      // 이미 확정된 결과는 AI를 다시 호출하지 않고 그대로 반환한다.
      return responseFor(context, existingSummary.summaryFeedbackId());
    }

    // 외부 AI 호출은 DB 트랜잭션 밖에서 수행한다.
    AiSessionFeedbackRequest request =
        new AiSessionFeedbackRequest(
            context.sessionId(),
            context.scenario(),
            context.userMessages().stream().map(UserMessageContext::messageId).toList(),
            context.userMessages().stream()
                .map(
                    message ->
                        new AiSessionFeedbackRequest.AssessmentMessage(
                            message.messageId(),
                            message.evaluationContext().content(),
                            message.content(),
                            message.responseDemand(),
                            message.requiredElements()))
                .toList());
    AiSessionFeedbackResult result = generateOrFallback(request);
    Long summaryFeedbackId = recordOrFallback(userId, context, result);
    return responseFor(context, summaryFeedbackId);
  }

  private Long recordOrFallback(
      long userId, LoadedSessionFeedbackContext context, AiSessionFeedbackResult result) {
    try {
      return completionService.record(userId, context, result);
    } catch (ApiException exception) {
      if (exception.getErrorCode() != ErrorCode.AI_RESPONSE_INVALID) {
        throw exception;
      }
      log.warn("invalid session feedback fallback: sessionId={}", context.sessionId());
      return completionService.record(
          userId, context, AiSessionFeedbackResult.fallback(context.sessionId()));
    }
  }

  private AiSessionFeedbackResult generateOrFallback(AiSessionFeedbackRequest request) {
    try {
      return aiConversationClient.generateSessionFeedback(request);
    } catch (ApiException exception) {
      if (exception.getErrorCode() != ErrorCode.AI_RESPONSE_INVALID
          && exception.getErrorCode() != ErrorCode.AI_GENERATION_FAILED
          && exception.getErrorCode() != ErrorCode.FEEDBACK_GENERATION_FAILED) {
        throw exception;
      }
      log.warn(
          "session feedback fallback: sessionId={}, errorCode={}",
          request.sessionId(),
          exception.getErrorCode());
      return AiSessionFeedbackResult.fallback(request.sessionId());
    }
  }

  /** 저장된 최종 피드백과 평가 당시 사용자 메시지 컨텍스트를 API 응답으로 조립한다. */
  private SessionFeedbackResponse responseFor(
      LoadedSessionFeedbackContext context, Long summaryFeedbackId) {
    List<SessionHistoryMessageFeedback> feedbacks =
        sessionFeedbackDataService.findMessageFeedbacks(summaryFeedbackId);
    SessionHistorySummaryFeedback summary =
        sessionFeedbackDataService.requireSummary(summaryFeedbackId);
    UserLevelAssessment storedAssessment =
        sessionLevelAssessmentService.findBySessionId(context.sessionId());
    if (storedAssessment == null) {
      throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
    }
    SessionLevelAssessment levelAssessment = storedAssessment.toAssessment();
    boolean generationFallback =
        feedbacks.isEmpty() && levelAssessment.source() == SessionLevelAssessment.Source.FALLBACK;
    if (!generationFallback && feedbacks.size() != context.userMessages().size()) {
      throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
    }
    Map<Long, SessionHistoryMessageFeedback> feedbackByMessageId =
        feedbacks.stream()
            .collect(
                Collectors.toMap(
                    SessionHistoryMessageFeedback::getSessionHistoryMessageId,
                    Function.identity()));
    return SessionFeedbackResponse.from(
        context.sessionId(),
        summary,
        generationFallback
            ? List.of()
            : context.userMessages().stream()
                .map(
                    userMessage ->
                        messageFeedbackResponse(
                            feedbackByMessageId.get(userMessage.messageId()), userMessage))
                .toList(),
        levelAssessment);
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
