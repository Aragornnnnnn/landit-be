// 사용자 발화 이후 생성할 AI 메시지를 요청한다.

package com.landit.landitbe.feature.session.service;

import com.landit.landitbe.feature.content.dto.NextQuestionContext;
import com.landit.landitbe.feature.session.client.ai.AiClosingMessageRequest;
import com.landit.landitbe.feature.session.client.ai.AiClosingMessageResult;
import com.landit.landitbe.feature.session.client.ai.AiClosingReason;
import com.landit.landitbe.feature.session.client.ai.AiConversationClient;
import com.landit.landitbe.feature.session.client.ai.AiConversationHistoryMessage;
import com.landit.landitbe.feature.session.client.ai.AiConversationSettings;
import com.landit.landitbe.feature.session.client.ai.AiNextMessageRequest;
import com.landit.landitbe.feature.session.client.ai.AiNextMessageResult;
import com.landit.landitbe.feature.session.client.ai.AiNextQuestion;
import com.landit.landitbe.feature.session.client.ai.AiScenarioContext;
import com.landit.landitbe.feature.session.domain.CompletionReason;
import com.landit.landitbe.feature.session.domain.GoalCompletionStatus;
import com.landit.landitbe.feature.session.repository.projection.ScenarioSessionMessageContextProjection;
import com.landit.landitbe.shared.domain.InnerThoughtType;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 사용자 발화 이후 생성할 AI 메시지를 요청한다. */
@RequiredArgsConstructor
@Service
class SessionMessageAiGenerator {

  private final AiConversationClient aiConversationClient;
  private final AiConversationSettings aiConversationSettings;

  /** 다음 AI 메시지 또는 종료 메시지를 생성한다. */
  Generation generate(Request request) {
    Optional<NextQuestionContext> nextQuestion = request.nextQuestion();

    // 다음 질문이 없으면 최대 턴에 도달한 것으로 보고
    // 종료 메시지를 생성한다.
    if (nextQuestion.isEmpty()) {
      return generateClosingMessage(
          request,
          AiClosingReason.MAX_TURNS_REACHED,
          GoalCompletionStatus.COMPLETED,
          CompletionReason.MAX_TURNS_REACHED);
    }

    return generateNextMessage(request, nextQuestion.get());
  }

  private Generation generateNextMessage(Request request, NextQuestionContext nextQuestion) {
    AiNextMessageResult nextMessageResult =
        aiConversationClient.generateNextMessage(toNextMessageRequest(request, nextQuestion));
    assertNextMessageResult(nextMessageResult);

    return new Generation(
        nextMessageResult.aiMessage(),
        nextMessageResult.translatedMessage(),
        null,
        null,
        nextMessageResult.goalCompletionStatus(),
        false,
        null);
  }

  private AiNextMessageRequest toNextMessageRequest(
      Request request, NextQuestionContext nextQuestion) {
    return new AiNextMessageRequest(
        request.learningSessionId(),
        request.submittedMessageId(),
        request.submittedTurnNumber(),
        AiScenarioContext.from(request.scenarioContext(), aiConversationSettings),
        request.conversationHistory(),
        toAiNextQuestion(nextQuestion));
  }

  private Generation generateClosingMessage(
      Request request,
      AiClosingReason closingReason,
      GoalCompletionStatus goalCompletionStatus,
      CompletionReason completionReason) {
    AiClosingMessageResult closingMessageResult =
        aiConversationClient.generateClosingMessage(
            new AiClosingMessageRequest(
                request.learningSessionId(),
                request.submittedMessageId(),
                request.submittedTurnNumber(),
                AiScenarioContext.from(request.scenarioContext(), aiConversationSettings),
                request.conversationHistory(),
                closingReason,
                goalCompletionStatus));
    assertClosingMessageResult(closingMessageResult);
    return new Generation(
        closingMessageResult.aiMessage(),
        closingMessageResult.translatedMessage(),
        closingMessageResult.innerThought(),
        closingMessageResult.innerThoughtType(),
        goalCompletionStatus,
        true,
        completionReason);
  }

  private AiNextQuestion toAiNextQuestion(NextQuestionContext nextQuestion) {
    return new AiNextQuestion(
        nextQuestion.questionId(),
        nextQuestion.sequence(),
        nextQuestion.questionText(),
        nextQuestion.questionTranslation());
  }

  private void assertNextMessageResult(AiNextMessageResult result) {
    if (result == null
        || blank(result.aiMessage())
        || blank(result.translatedMessage())
        || result.goalCompletionStatus() == null) {
      throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
    }
  }

  private void assertClosingMessageResult(AiClosingMessageResult result) {
    if (result == null
        || blank(result.aiMessage())
        || blank(result.translatedMessage())
        || blank(result.innerThought())
        || result.innerThoughtType() == null) {
      throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
    }
  }

  private boolean blank(String value) {
    return value == null || value.isBlank();
  }

  record Request(
      Long learningSessionId,
      Long submittedMessageId,
      int submittedTurnNumber,
      ScenarioSessionMessageContextProjection scenarioContext,
      List<AiConversationHistoryMessage> conversationHistory,
      Optional<NextQuestionContext> nextQuestion) {}

  record Generation(
      String aiMessage,
      String translatedMessage,
      String innerThought,
      InnerThoughtType innerThoughtType,
      GoalCompletionStatus goalCompletionStatus,
      boolean completed,
      CompletionReason completionReason) {}
}
