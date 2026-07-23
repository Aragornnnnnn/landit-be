// AI 최종 피드백을 저장하고 세션 결과와 진행도를 최초 한 번 확정한다.

package com.landit.landitbe.feature.session.service;

import com.landit.landitbe.feature.learning.service.LearningProgressService;
import com.landit.landitbe.feature.session.client.ai.AiSessionFeedbackResult;
import com.landit.landitbe.feature.session.client.ai.AiSessionMessageFeedbackResult;
import com.landit.landitbe.feature.session.domain.FeedbackType;
import com.landit.landitbe.feature.session.domain.LearningSession;
import com.landit.landitbe.feature.session.domain.SessionHistory;
import com.landit.landitbe.feature.session.domain.SessionHistoryMessageFeedback;
import com.landit.landitbe.feature.session.domain.SessionHistorySummaryFeedback;
import com.landit.landitbe.shared.domain.ConversationSpeaker;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
class SessionFeedbackCompletionService {

  private final LearningSessionService learningSessionService;
  private final SessionHistoryService sessionHistoryService;
  private final SessionFeedbackDataService sessionFeedbackDataService;
  private final SessionMessageService sessionMessageService;
  private final LearningProgressService learningProgressService;

  /** 유효한 AI 최종 피드백을 저장하고 세션 결과를 최초 한 번 확정한다. */
  @Transactional
  Long record(long userId, LoadedSessionFeedbackContext context, AiSessionFeedbackResult result) {
    validateResult(context, result);
    BigDecimal starRating = result.starRating();
    // 동시 요청이 같은 세션 결과와 진행도를 두 번 확정하지 않도록 세션 row를 잠근다.
    LearningSession learningSession =
        learningSessionService.findOwnedCompletedForUpdate(userId, context.sessionId());
    SessionHistorySummaryFeedback existing =
        sessionFeedbackDataService.findSummaryByHistoryId(context.sessionHistoryId()).orElse(null);
    if (existing != null) {
      return ExistingSummaryFeedbackContext.from(existing).summaryFeedbackId();
    }

    SessionHistorySummaryFeedback summaryFeedback =
        saveSummaryFeedback(context, result, starRating);
    saveMessageFeedbacks(context, result, summaryFeedback.getId());
    completeSessionHistory(context, learningSession);
    completeScenarioProgress(context, learningSession, result.nativeScore(), starRating);
    return summaryFeedback.getId();
  }

  /** AI 응답의 세션 식별자, 점수, 필수 요약 필드가 계약을 만족하는지 검증한다. */
  private void validateResult(
      LoadedSessionFeedbackContext context, AiSessionFeedbackResult result) {
    if (result == null
        || !context.sessionId().equals(result.sessionId())
        || result.nativeScore() < 0
        || result.nativeScore() > 100
        || !validStarRating(result.starRating())
        || blank(result.highlightMessage())
        || blank(result.summaryMessage())) {
      throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
    }
    validateMessageFeedbacks(context.userMessages(), result.messageFeedbacks());
  }

  /** AI가 반환할 수 있는 별점 값인지 검증한다. */
  private boolean validStarRating(BigDecimal starRating) {
    return starRating != null
        && (starRating.compareTo(new BigDecimal("1.0")) == 0
            || starRating.compareTo(new BigDecimal("1.5")) == 0
            || starRating.compareTo(new BigDecimal("2.0")) == 0
            || starRating.compareTo(new BigDecimal("2.5")) == 0
            || starRating.compareTo(new BigDecimal("3.0")) == 0);
  }

  /** AI가 요청한 사용자 메시지 순서와 동일한 개수·식별자의 피드백을 반환했는지 검증한다. */
  private void validateMessageFeedbacks(
      List<UserMessageContext> userMessages,
      List<AiSessionMessageFeedbackResult> messageFeedbacks) {
    if (messageFeedbacks == null || userMessages.size() != messageFeedbacks.size()) {
      throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
    }
    for (int index = 0; index < userMessages.size(); index++) {
      UserMessageContext userMessage = userMessages.get(index);
      AiSessionMessageFeedbackResult feedback = messageFeedbacks.get(index);
      if (feedback == null
          || !userMessage.messageId().equals(feedback.messageId())
          || feedback.feedbackType() == null
          || blank(feedback.baseLocaleAnalogy())) {
        throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
      }
      validateRequiredFields(feedback);
    }
  }

  /** 피드백 유형별로 반드시 포함되어야 하는 설명 또는 교정 필드를 검증한다. */
  private void validateRequiredFields(AiSessionMessageFeedbackResult feedback) {
    if (feedback.feedbackType() == FeedbackType.GOOD && blank(feedback.feedbackDetail())) {
      throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
    }
    if (feedback.feedbackType() == FeedbackType.NEEDS_IMPROVEMENT
        && (blank(feedback.positiveFeedback())
            || blank(feedback.correctionExpression())
            || blank(feedback.correctionReason()))) {
      throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
    }
  }

  /** 세션 전체 점수와 요약 문구를 완료 상태의 summary feedback으로 저장한다. */
  private SessionHistorySummaryFeedback saveSummaryFeedback(
      LoadedSessionFeedbackContext context, AiSessionFeedbackResult result, BigDecimal starRating) {
    int nativeLikeMessageCount =
        (int)
            result.messageFeedbacks().stream()
                .filter(feedback -> feedback.feedbackType() == FeedbackType.GOOD)
                .count();
    return sessionFeedbackDataService.saveSummary(
        SessionHistorySummaryFeedback.completed(
            context.sessionHistoryId(),
            result.nativeScore(),
            starRating,
            context.userMessages().size(),
            nativeLikeMessageCount,
            result.highlightMessage(),
            result.summaryMessage()));
  }

  /** AI 응답 순서에 맞춰 각 사용자 메시지의 상세 피드백을 저장한다. */
  private void saveMessageFeedbacks(
      LoadedSessionFeedbackContext context,
      AiSessionFeedbackResult result,
      Long summaryFeedbackId) {
    List<SessionHistoryMessageFeedback> feedbacks = new java.util.ArrayList<>();
    for (int index = 0; index < context.userMessages().size(); index++) {
      UserMessageContext userMessage = context.userMessages().get(index);
      AiSessionMessageFeedbackResult feedback = result.messageFeedbacks().get(index);
      feedbacks.add(
          SessionHistoryMessageFeedback.completed(
              summaryFeedbackId,
              userMessage.messageId(),
              context.targetLocale(),
              context.baseLocale(),
              feedback.feedbackType(),
              feedback.baseLocaleAnalogy(),
              feedback.positiveFeedback(),
              feedback.feedbackDetail(),
              feedback.correctionExpression(),
              feedback.correctionReason(),
              feedback.benchmarkMessage()));
    }
    sessionFeedbackDataService.saveMessageFeedbacks(feedbacks);
    sessionMessageService.completeFeedback(
        context.userMessages().stream().map(UserMessageContext::messageId).toList());
  }

  /** 세션 종료 시각을 기준으로 히스토리의 종료 정보와 사용자 메시지 수를 확정한다. */
  private void completeSessionHistory(
      LoadedSessionFeedbackContext context, LearningSession learningSession) {
    SessionHistory sessionHistory = sessionHistoryService.require(context.sessionHistoryId());
    int userMessageCount =
        Math.toIntExact(
            sessionMessageService.countByRole(sessionHistory.getId(), ConversationSpeaker.USER));
    sessionHistory.complete(learningSession.getEndedAt(), userMessageCount);
  }

  /** 신규 최종 피드백 저장 시점에만 시나리오 진행도와 최고 성과를 갱신한다. */
  private void completeScenarioProgress(
      LoadedSessionFeedbackContext context,
      LearningSession learningSession,
      int nativeScore,
      BigDecimal starRating) {
    learningProgressService.completeScenario(
        learningSession.getUserProfileId(),
        context.scenario().scenarioId(),
        learningSession.getTargetLocale(),
        starRating,
        nativeScore,
        learningSession.getEndedAt());
  }

  /** 필수 문자열이 null 또는 공백인지 확인한다. */
  private boolean blank(String value) {
    return value == null || value.isBlank();
  }
}
