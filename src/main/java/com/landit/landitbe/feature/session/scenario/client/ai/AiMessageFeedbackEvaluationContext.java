// AI 메시지별 피드백에서 사용자 발화를 평가할 기준 정보를 담는다.

package com.landit.landitbe.feature.session.scenario.client.ai;

import com.landit.landitbe.feature.session.client.ai.AiConversationHistoryMessage;
import com.landit.landitbe.feature.session.scenario.repository.projection.ScenarioSessionMessageContextProjection;
import com.landit.landitbe.shared.domain.ConversationSpeaker;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.List;

/**
 * AI 메시지별 피드백에서 사용자 발화를 평가할 기준 정보를 담는다.
 *
 * @param type 평가 컨텍스트 유형
 * @param content 메시지 본문
 * @param translatedContent 번역된 메시지 본문
 */
public record AiMessageFeedbackEvaluationContext(
    AiMessageFeedbackEvaluationContextType type, String content, String translatedContent) {
  /**
   * 시나리오 시작 발화와 직전 AI 메시지에 맞는 평가 기준을 조립한다.
   *
   * @param scenarioContext 시나리오 컨텍스트
   * @param conversationHistory 현재 사용자 메시지까지의 이력
   * @return 발화 평가 기준
   * @throws ApiException 시작 안내 또는 직전 AI 메시지가 없을 때
   */
  public static AiMessageFeedbackEvaluationContext from(
      ScenarioSessionMessageContextProjection scenarioContext,
      List<AiConversationHistoryMessage> conversationHistory) {
    if (isUserFirstOpeningMessage(scenarioContext, conversationHistory)) {
      return scenarioOpeningInstructionContext(scenarioContext);
    }
    return precedingAiMessageContext(conversationHistory);
  }

  /** USER First 시나리오의 첫 사용자 메시지인지 판별한다. */
  private static boolean isUserFirstOpeningMessage(
      ScenarioSessionMessageContextProjection scenarioContext,
      List<AiConversationHistoryMessage> conversationHistory) {
    return scenarioContext.firstSpeaker() == ConversationSpeaker.USER
        && conversationHistory.size() == 1;
  }

  /** USER First 시작 안내를 평가 기준으로 변환한다. */
  private static AiMessageFeedbackEvaluationContext scenarioOpeningInstructionContext(
      ScenarioSessionMessageContextProjection scenarioContext) {
    String instruction = scenarioContext.userOpeningInstruction();
    if (instruction == null || instruction.isBlank()) {
      throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
    }
    return new AiMessageFeedbackEvaluationContext(
        AiMessageFeedbackEvaluationContextType.SCENARIO_OPENING_INSTRUCTION, instruction, null);
  }

  /** 직전 AI 메시지를 사용자 발화의 평가 기준으로 변환한다. */
  private static AiMessageFeedbackEvaluationContext precedingAiMessageContext(
      List<AiConversationHistoryMessage> conversationHistory) {
    if (conversationHistory.size() < 2) {
      throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
    }
    AiConversationHistoryMessage precedingMessage =
        conversationHistory.get(conversationHistory.size() - 2);
    if (!ConversationSpeaker.AI.name().equals(precedingMessage.role())) {
      throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
    }
    return new AiMessageFeedbackEvaluationContext(
        AiMessageFeedbackEvaluationContextType.AI_MESSAGE,
        precedingMessage.content(),
        precedingMessage.translatedContent());
  }
}
