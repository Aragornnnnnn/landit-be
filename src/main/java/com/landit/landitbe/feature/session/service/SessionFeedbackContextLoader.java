// 최종 피드백 생성에 필요한 완료 세션 컨텍스트를 불변 값으로 조회한다.

package com.landit.landitbe.feature.session.service;

import com.landit.landitbe.feature.session.client.ai.AiConversationHistoryMessage;
import com.landit.landitbe.feature.session.domain.LearningSession;
import com.landit.landitbe.feature.session.domain.SessionHistory;
import com.landit.landitbe.feature.session.domain.SessionHistoryMessage;
import com.landit.landitbe.feature.session.repository.ScenarioSessionMessageQueryRepository;
import com.landit.landitbe.feature.session.repository.SessionHistoryMessageRepository;
import com.landit.landitbe.feature.session.repository.SessionHistoryRepository;
import com.landit.landitbe.feature.session.repository.SessionHistorySummaryFeedbackRepository;
import com.landit.landitbe.feature.session.repository.projection.ScenarioSessionMessageContextProjection;
import com.landit.landitbe.shared.domain.ConversationSpeaker;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 최종 피드백 생성에 필요한 완료 세션 컨텍스트를 불변 값으로 조회한다. */
@RequiredArgsConstructor
@Component
class SessionFeedbackContextLoader {

  private final LearningSessionFinder learningSessionFinder;
  private final SessionHistoryRepository sessionHistoryRepository;
  private final SessionHistoryMessageRepository sessionHistoryMessageRepository;
  private final ScenarioSessionMessageQueryRepository scenarioSessionMessageQueryRepository;
  private final SessionHistorySummaryFeedbackRepository sessionHistorySummaryFeedbackRepository;
  private final AiScenarioContextMapper aiScenarioContextMapper;

  /** 소유한 완료 시나리오 세션의 최종 피드백 입력을 불변 값으로 조회한다. */
  @Transactional(readOnly = true)
  public LoadedSessionFeedbackContext load(long userId, long sessionId) {
    LearningSession learningSession = learningSessionFinder.findOwnedCompleted(userId, sessionId);
    SessionHistory sessionHistory =
        sessionHistoryRepository
            .findByLearningSessionId(sessionId)
            .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_SERVER_ERROR));
    ScenarioSessionMessageContextProjection scenarioContext =
        scenarioSessionMessageQueryRepository
            .findContextByLearningSessionId(sessionId)
            .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_SERVER_ERROR));
    List<SessionHistoryMessage> historyMessages =
        sessionHistoryMessageRepository.findBySessionHistoryIdOrderByMessageSequenceAsc(
            sessionHistory.getId());

    // 이후 AI 호출과 응답 조립에 필요한 값을 트랜잭션 안에서 모두 읽어 불변 컨텍스트로 넘긴다.
    return new LoadedSessionFeedbackContext(
        learningSession.getId(),
        sessionHistory.getId(),
        learningSession.getTargetLocale(),
        learningSession.getBaseLocale(),
        aiScenarioContextMapper.map(scenarioContext),
        userMessages(historyMessages, scenarioContext),
        sessionHistorySummaryFeedbackRepository
            .findBySessionHistoryId(sessionHistory.getId())
            .map(ExistingSummaryFeedbackContext::from));
  }

  /** 세션 전체 히스토리에서 사용자 메시지와 평가 당시 기준 컨텍스트를 순서대로 구성한다. */
  private List<UserMessageContext> userMessages(
      List<SessionHistoryMessage> historyMessages,
      ScenarioSessionMessageContextProjection scenarioContext) {
    List<AiConversationHistoryMessage> conversationHistory =
        historyMessages.stream()
            .map(
                message ->
                    new AiConversationHistoryMessage(
                        message.getId(),
                        message.getTurnNumber(),
                        message.getRole().name(),
                        message.getContent(),
                        message.getTranslatedContent()))
            .toList();
    List<UserMessageContext> userMessages = new ArrayList<>();
    for (int index = 0; index < historyMessages.size(); index++) {
      SessionHistoryMessage message = historyMessages.get(index);
      if (message.getRole() != ConversationSpeaker.USER) {
        continue;
      }
      userMessages.add(
          new UserMessageContext(
              message.getId(),
              message.getTurnNumber(),
              message.getContent(),
              SessionMessageFeedbackRequester.evaluationContext(
                  scenarioContext, conversationHistory.subList(0, index + 1))));
    }
    if (userMessages.isEmpty()) {
      throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
    }
    return List.copyOf(userMessages);
  }
}
