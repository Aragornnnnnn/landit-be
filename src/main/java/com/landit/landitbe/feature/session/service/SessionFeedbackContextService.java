// 최종 피드백 생성에 필요한 완료 세션 컨텍스트를 불변 값으로 조회한다.

package com.landit.landitbe.feature.session.service;

import com.landit.landitbe.feature.session.client.ai.AiConversationHistoryMessage;
import com.landit.landitbe.feature.session.client.ai.AiConversationSettings;
import com.landit.landitbe.feature.session.client.ai.AiScenarioContext;
import com.landit.landitbe.feature.session.domain.LearningSession;
import com.landit.landitbe.feature.session.domain.SessionHistory;
import com.landit.landitbe.feature.session.domain.SessionHistoryMessage;
import com.landit.landitbe.feature.session.exception.SessionException;
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
class SessionFeedbackContextService {

  private final LearningSessionService learningSessionService;
  private final SessionHistoryService sessionHistoryService;
  private final SessionMessageService sessionMessageService;
  private final ScenarioSessionService scenarioSessionService;
  private final SessionFeedbackDataService sessionFeedbackDataService;
  private final AiConversationSettings aiConversationSettings;

  /**
   * 소유한 완료 시나리오 세션의 최종 피드백 입력을 불변 값으로 조회한다.
   *
   * @param userId 세션 소유자 ID
   * @param sessionId 학습 세션 ID
   * @return 최종 피드백 생성에 필요한 불변 컨텍스트
   * @throws SessionException 세션이 없거나 접근할 수 없거나 완료되지 않았을 때
   * @throws ApiException 세션 히스토리 또는 사용자 메시지가 없을 때
   */
  @Transactional(readOnly = true)
  public LoadedSessionFeedbackContext load(long userId, long sessionId) {
    LearningSession learningSession = learningSessionService.findOwnedCompleted(userId, sessionId);
    SessionHistory sessionHistory =
        sessionHistoryService
            .findByLearningSessionId(sessionId)
            .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_SERVER_ERROR));
    ScenarioSessionMessageContextProjection scenarioContext =
        scenarioSessionService.requireMessageContext(sessionId);
    List<SessionHistoryMessage> historyMessages =
        sessionMessageService.findAll(sessionHistory.getId());

    // 이후 AI 호출과 응답 조립에 필요한 값을 트랜잭션 안에서 모두 읽어 불변 컨텍스트로 넘긴다.
    return new LoadedSessionFeedbackContext(
        learningSession.getId(),
        sessionHistory.getId(),
        learningSession.getTargetLocale(),
        learningSession.getBaseLocale(),
        AiScenarioContext.from(scenarioContext, aiConversationSettings),
        userMessages(historyMessages, scenarioContext),
        sessionFeedbackDataService
            .findSummaryByHistoryId(sessionHistory.getId())
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
