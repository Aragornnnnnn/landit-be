// 시나리오 총평에서 쓸 직전 교정과 배운 표현 후보를 준비한다.

package com.landit.landitbe.feature.learning.scenario.feedback.service;

import com.landit.landitbe.feature.learning.conversation.dto.LearningSessionSnapshot;
import com.landit.landitbe.feature.learning.conversation.dto.SessionHistoryMessageSnapshot;
import com.landit.landitbe.feature.learning.conversation.history.service.ConversationMessageService;
import com.landit.landitbe.feature.learning.conversation.history.service.SessionHistoryService;
import com.landit.landitbe.feature.learning.conversation.service.LearningSessionService;
import com.landit.landitbe.feature.learning.freetalk.expression.reuse.domain.FreeTalkExpressionReuseSource;
import com.landit.landitbe.feature.learning.freetalk.expression.reuse.dto.FreeTalkLearnedExpression;
import com.landit.landitbe.feature.learning.freetalk.expression.reuse.service.FreeTalkLearnedExpressionSelectionService;
import com.landit.landitbe.feature.learning.scenario.feedback.domain.FeedbackType;
import com.landit.landitbe.feature.learning.scenario.feedback.domain.SessionHistoryMessageFeedback;
import com.landit.landitbe.feature.learning.scenario.feedback.dto.ScenarioFeedbackEvidence;
import com.landit.landitbe.feature.learning.scenario.feedback.dto.ScenarioFeedbackEvidence.LearnedExpressionCandidate;
import com.landit.landitbe.feature.learning.scenario.feedback.dto.ScenarioFeedbackEvidence.PreviousMistake;
import com.landit.landitbe.feature.learning.scenario.feedback.dto.UserMessageContext;
import com.landit.landitbe.shared.domain.ConversationSpeaker;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 직전 시나리오 한 건과 완료한 표현만 읽어 비교에 쓸 근거를 만든다. */
@RequiredArgsConstructor
@Service
public class ScenarioFeedbackEvidenceService {

  private final LearningSessionService learningSessionService;
  private final SessionHistoryService sessionHistoryService;
  private final ConversationMessageService conversationMessageService;
  private final SessionFeedbackDataService sessionFeedbackDataService;
  private final FreeTalkLearnedExpressionSelectionService learnedExpressionSelectionService;

  /**
   * 현재 시나리오와 비교할 직전 완료 시나리오의 교정과 재사용 후보 표현을 준비한다.
   *
   * @param currentSession 현재 완료 학습 세션
   * @param currentMessages 현재 시나리오의 사용자 발화
   * @return AI 입력용 이전 교정과 배운 표현 후보
   */
  @Transactional(readOnly = true)
  public ScenarioFeedbackEvidence load(
      LearningSessionSnapshot currentSession, List<UserMessageContext> currentMessages) {
    var previousSession =
        learningSessionService.findPreviousCompletedScenario(
            currentSession.getUserProfileId(),
            currentSession.getId(),
            currentSession.getStartedAt());
    List<PreviousMistake> previousMistakes =
        previousSession
            .flatMap(session -> sessionHistoryService.findByLearningSessionId(session.getId()))
            .flatMap(
                history ->
                    sessionFeedbackDataService
                        .findSummaryByHistoryId(history.getId())
                        .map(
                            summary ->
                                previousMistakes(
                                    history.getId(),
                                    sessionFeedbackDataService.findMessageFeedbacks(
                                        summary.getId()))))
            .orElseGet(List::of);
    List<LearnedExpressionCandidate> learnedExpressions =
        learnedExpressionSelectionService
            .select(
                currentSession.getUserProfileId(),
                currentSession.getTargetLocale(),
                currentSession.getBaseLocale(),
                currentMessages.stream().map(UserMessageContext::content).toList())
            .stream()
            .map(ScenarioFeedbackEvidenceService::candidate)
            .toList();
    return new ScenarioFeedbackEvidence(
        previousSession.map(session -> session.getEndedAt().toLocalDate()).orElse(null),
        previousMistakes,
        learnedExpressions);
  }

  /** 직전 세션의 USER 발화 중 교정 표현과 사유가 모두 저장된 실제 실수만 후보로 만든다. */
  private List<PreviousMistake> previousMistakes(
      long sessionHistoryId, List<SessionHistoryMessageFeedback> feedbacks) {
    Map<Long, SessionHistoryMessageSnapshot> userMessagesById =
        conversationMessageService.findAll(sessionHistoryId).stream()
            .filter(message -> message.getRole() == ConversationSpeaker.USER)
            .collect(Collectors.toMap(SessionHistoryMessageSnapshot::getId, Function.identity()));
    return feedbacks.stream()
        .filter(feedback -> feedback.getFeedbackType() == FeedbackType.NEEDS_IMPROVEMENT)
        .filter(
            feedback ->
                nonBlank(feedback.getCorrectionExpression())
                    && nonBlank(feedback.getCorrectionReason()))
        .map(
            feedback -> {
              SessionHistoryMessageSnapshot message =
                  userMessagesById.get(feedback.getSessionHistoryMessageId());
              return message == null
                  ? null
                  : new PreviousMistake(
                      message.getId(),
                      message.getContent(),
                      feedback.getCorrectionExpression(),
                      feedback.getCorrectionReason());
            })
        .filter(java.util.Objects::nonNull)
        .toList();
  }

  /** 공통 표현 선정 결과의 학습 당시 출처와 날짜를 보존해 시나리오 비교 후보로 변환한다. */
  private static LearnedExpressionCandidate candidate(FreeTalkLearnedExpression expression) {
    return new LearnedExpressionCandidate(
        expression.expressionId(),
        expression.text(),
        expression.meaning(),
        source(expression.sourceType()),
        expression.learnedOn());
  }

  /** 공통 선정 서비스의 출처를 시나리오 응답이 사용하는 학습 출처 계약으로 변환한다. */
  private static com.landit.landitbe.feature.learning.expression.progress.domain
          .ExpressionLearningSource
      source(FreeTalkExpressionReuseSource source) {
    return switch (source) {
      case SCENARIO ->
          com.landit.landitbe.feature.learning.expression.progress.domain.ExpressionLearningSource
              .SCENARIO;
      case FREE_TALK ->
          com.landit.landitbe.feature.learning.expression.progress.domain.ExpressionLearningSource
              .FREE_TALK;
    };
  }

  private static boolean nonBlank(String value) {
    return value != null && !value.isBlank();
  }
}
