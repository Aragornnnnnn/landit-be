// 저장된 시나리오 전체 완료 회차의 대화와 피드백을 읽기 전용으로 조립한다.

package com.landit.landitbe.feature.learning.scenario.history.service;

import com.landit.landitbe.feature.learning.conversation.domain.ProcessingStatus;
import com.landit.landitbe.feature.learning.conversation.dto.SessionHistoryMessageSnapshot;
import com.landit.landitbe.feature.learning.conversation.history.service.ConversationMessageService;
import com.landit.landitbe.feature.learning.scenario.feedback.domain.SessionHistoryMessageFeedback;
import com.landit.landitbe.feature.learning.scenario.feedback.domain.SessionHistorySummaryFeedback;
import com.landit.landitbe.feature.learning.scenario.feedback.dto.SessionFeedbackResponse;
import com.landit.landitbe.feature.learning.scenario.feedback.dto.SessionFeedbackResponse.EvaluationContextResponse;
import com.landit.landitbe.feature.learning.scenario.feedback.dto.SessionFeedbackResponse.MessageFeedbackResponse;
import com.landit.landitbe.feature.learning.scenario.feedback.repository.SessionHistoryMessageFeedbackRepository;
import com.landit.landitbe.feature.learning.scenario.feedback.repository.SessionHistorySummaryFeedbackRepository;
import com.landit.landitbe.feature.learning.scenario.feedback.service.ScenarioFeedbackAccessService;
import com.landit.landitbe.feature.learning.scenario.history.dto.ScenarioHistoryResponse;
import com.landit.landitbe.feature.learning.scenario.history.repository.ScenarioHistoryQueryRepository;
import com.landit.landitbe.feature.learning.scenario.history.repository.projection.ScenarioHistoryProjection;
import com.landit.landitbe.feature.learning.scenario.session.message.feedback.client.ai.AiMessageFeedbackEvaluationContextType;
import com.landit.landitbe.shared.domain.ConversationSpeaker;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 시나리오 완료 기록을 저장 당시 대화와 기존 피드백 공개 정책으로 조회한다. */
@Service
@RequiredArgsConstructor
public class ScenarioHistoryQueryService {
  private final ScenarioHistoryQueryRepository histories;
  private final ConversationMessageService messages;
  private final SessionHistorySummaryFeedbackRepository summaries;
  private final SessionHistoryMessageFeedbackRepository feedbacks;
  private final ScenarioFeedbackAccessService feedbackAccess;

  /**
   * 로그인 사용자가 완료한 시나리오의 모든 회차를 조회한다. AI 호출이나 상태 변경은 수행하지 않는다.
   *
   * @param userId 로그인 사용자 ID
   * @param scenarioId 시나리오 ID
   * @return 완료 시각 내림차순의 회차 목록. 기록이 없으면 빈 목록
   */
  @Transactional(readOnly = true)
  public ScenarioHistoryResponse findHistory(long userId, long scenarioId) {
    List<ScenarioHistoryProjection> sessions = histories.findCompleted(userId, scenarioId);
    if (sessions.isEmpty()) {
      return new ScenarioHistoryResponse(scenarioId, List.of());
    }
    List<Long> historyIds = sessions.stream().map(ScenarioHistoryProjection::historyId).toList();
    var messagesByHistory =
        messages.findAllByHistoryIds(historyIds).stream()
            .collect(Collectors.groupingBy(SessionHistoryMessageSnapshot::sessionHistoryId));
    var summariesByHistory =
        summaries.findBySessionHistoryIdIn(historyIds).stream()
            .filter(summary -> summary.getProcessingStatus() == ProcessingStatus.COMPLETED)
            .collect(
                Collectors.toMap(
                    SessionHistorySummaryFeedback::getSessionHistoryId, Function.identity()));
    var feedbackByMessage = feedbackByMessage(summariesByHistory.values().stream().toList());
    return new ScenarioHistoryResponse(
        scenarioId,
        sessions.stream()
            .map(
                session ->
                    toSession(
                        userId,
                        session,
                        messagesByHistory.getOrDefault(session.historyId(), List.of()),
                        summariesByHistory.get(session.historyId()),
                        feedbackByMessage))
            .toList());
  }

  private Map<Long, SessionHistoryMessageFeedback> feedbackByMessage(
      List<SessionHistorySummaryFeedback> completedSummaries) {
    if (completedSummaries.isEmpty()) {
      return Map.of();
    }
    return feedbacks
        .findBySessionHistorySummaryFeedbackIdIn(
            completedSummaries.stream().map(SessionHistorySummaryFeedback::getId).toList())
        .stream()
        .filter(feedback -> feedback.getProcessingStatus() == ProcessingStatus.COMPLETED)
        .collect(
            Collectors.toMap(
                SessionHistoryMessageFeedback::getSessionHistoryMessageId, Function.identity()));
  }

  private ScenarioHistoryResponse.Session toSession(
      long userId,
      ScenarioHistoryProjection session,
      List<SessionHistoryMessageSnapshot> historyMessages,
      SessionHistorySummaryFeedback summary,
      Map<Long, SessionHistoryMessageFeedback> feedbackByMessage) {
    SessionFeedbackResponse feedback = null;
    if (summary != null) {
      boolean locked = feedbackAccess.detailFeedbackLocked(userId, session.sessionId());
      feedback =
          SessionFeedbackResponse.from(
              session.sessionId(),
              summary,
              locked
                  ? List.of()
                  : messageFeedbacks(session, historyMessages, summary.getId(), feedbackByMessage),
              locked);
    }
    return new ScenarioHistoryResponse.Session(
        session.sessionId(),
        session.startedAt(),
        session.endedAt(),
        historyMessages.stream().map(ScenarioHistoryResponse.Message::from).toList(),
        feedback);
  }

  // 평가 문맥은 현재 질문 콘텐츠가 아니라 당시 메시지와 선톡 안내 스냅샷으로 복원한다.
  private List<MessageFeedbackResponse> messageFeedbacks(
      ScenarioHistoryProjection session,
      List<SessionHistoryMessageSnapshot> historyMessages,
      Long summaryId,
      Map<Long, SessionHistoryMessageFeedback> feedbackByMessage) {
    List<MessageFeedbackResponse> result = new ArrayList<>();
    for (int index = 0; index < historyMessages.size(); index++) {
      var message = historyMessages.get(index);
      var feedback = feedbackByMessage.get(message.id());
      if (message.role() == ConversationSpeaker.USER
          && feedback != null
          && summaryId.equals(feedback.getSessionHistorySummaryFeedbackId())) {
        result.add(
            MessageFeedbackResponse.from(
                feedback,
                message.turnNumber(),
                message.content(),
                evaluationContext(session, historyMessages, index)));
      }
    }
    return List.copyOf(result);
  }

  private EvaluationContextResponse evaluationContext(
      ScenarioHistoryProjection session,
      List<SessionHistoryMessageSnapshot> historyMessages,
      int index) {
    if (index == 0) {
      return session.userOpeningInstruction() == null
          ? null
          : new EvaluationContextResponse(
              AiMessageFeedbackEvaluationContextType.SCENARIO_OPENING_INSTRUCTION,
              session.userOpeningInstruction(),
              null);
    }
    var preceding = historyMessages.get(index - 1);
    return preceding.role() == ConversationSpeaker.AI
        ? new EvaluationContextResponse(
            AiMessageFeedbackEvaluationContextType.AI_MESSAGE,
            preceding.content(),
            preceding.translatedContent())
        : null;
  }
}
