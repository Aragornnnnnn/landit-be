// AI 생성 결과를 세션 히스토리와 세션 상태에 반영한다.

package com.landit.landitbe.feature.learning.scenario.session.message.service;

import com.landit.landitbe.feature.character.service.StreakService;
import com.landit.landitbe.feature.learning.conversation.domain.ProcessingStatus;
import com.landit.landitbe.feature.learning.conversation.dto.LearningSessionSnapshot;
import com.landit.landitbe.feature.learning.conversation.dto.SessionHistoryMessageSnapshot;
import com.landit.landitbe.feature.learning.conversation.history.service.ConversationMessageService;
import com.landit.landitbe.feature.learning.conversation.service.LearningSessionService;
import com.landit.landitbe.feature.learning.scenario.access.service.ScenarioAccessService;
import com.landit.landitbe.feature.learning.scenario.assessment.service.SessionLevelAssessmentLaunchService;
import com.landit.landitbe.feature.learning.scenario.session.domain.ScenarioSession;
import com.landit.landitbe.feature.learning.scenario.session.message.dto.SessionMessageSubmitResponse;
import com.landit.landitbe.feature.learning.scenario.session.service.ScenarioSessionService;
import com.landit.landitbe.feature.profile.learning.service.ProfileLearningService;
import com.landit.landitbe.shared.domain.ConversationSpeaker;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** AI 생성 결과를 세션 히스토리와 세션 상태에 반영한다. */
@RequiredArgsConstructor
@Component
class GeneratedMessageService {

  private final LearningSessionService learningSessionService;
  private final tools.jackson.databind.json.JsonMapper mapper;
  private final ScenarioSessionService scenarioSessionService;
  private final ConversationMessageService conversationMessageService;
  private final ScenarioAccessService scenarioAccessService;
  private final Clock clock;
  private final StreakService streakService;
  private final SessionLevelAssessmentLaunchService assessmentLaunchService;
  private final ProfileLearningService profileLearningService;

  /** AI 생성 결과를 저장하고 사용자에게 반환할 메시지 제출 응답을 만든다. */
  SessionMessageSubmitResponse record(
      SubmittedMessageContext submittedContext,
      SessionMessageAiGenerator.Generation generation,
      ProcessingStatus feedbackProcessingStatus) {
    final LearningSessionSnapshot learningSession =
        learningSessionService.findOwnedInProgressForUpdate(
            submittedContext.userId(), submittedContext.sessionId());
    final ScenarioSession scenarioSession = findScenarioSession(submittedContext.sessionId());
    SessionHistoryMessageSnapshot submittedMessage = findSubmittedMessage(submittedContext);
    assertSubmittedMessageMatches(submittedContext, submittedMessage);
    if (feedbackProcessingStatus == ProcessingStatus.FAILED) {
      submittedMessage = conversationMessageService.markFeedbackFailed(submittedMessage.getId());
    }

    if (generation.completed()) {
      submittedMessage =
          conversationMessageService.recordInnerThought(
              submittedMessage.getId(), generation.innerThought(), generation.innerThoughtType());
    }
    scenarioSession.updateGoalCompletionStatus(generation.goalCompletionStatus());
    SessionHistoryMessageSnapshot nextMessage =
        saveAiMessage(submittedMessage, generation.aiMessage(), generation.translatedMessage());
    if (generation.completed()) {
      LocalDateTime completedAt = LocalDateTime.now(clock);
      learningSessionService.completeBySystem(
          learningSession.getId(), generation.completionReason(), completedAt);
      scenarioSession.recordCompletedLearningLevel(
          profileLearningService.getLearningLevel(submittedContext.userId()).learningLevel());
      if (assessmentLaunchService.includes(submittedContext.userId(), completedAt)) {
        learningSessionService.prepareLevelAssessment(learningSession.getId(), completedAt);
      }
      grantScenarioAccess(learningSession, submittedContext, completedAt);
      streakService.recordCompletedConversation(learningSession.getUserProfileId(), completedAt);
    }
    SessionMessageSubmitResponse response =
        SessionMessageSubmitResponse.from(
            submittedContext.sessionId(),
            submittedMessage,
            feedbackProcessingStatus,
            nextMessage,
            generation.ttsText(),
            generation.fixedQuestionText(),
            generation.questionAudioUrl(),
            submittedContext.scenarioContext().totalQuestionCount(),
            generation.completed());
    conversationMessageService.recordScenarioResponse(
        submittedMessage.getId(), mapper.writeValueAsString(response));
    return response;
  }

  /** 시나리오 세션을 정상 완료하면 해당 시나리오의 복습 권한을 멱등하게 부여한다. */
  private void grantScenarioAccess(
      LearningSessionSnapshot learningSession,
      SubmittedMessageContext submittedContext,
      LocalDateTime completedAt) {
    scenarioAccessService.grantAccess(
        learningSession.getUserProfileId(),
        submittedContext.scenarioContext().scenarioId(),
        learningSession.getTargetLocale(),
        completedAt);
  }

  private ScenarioSession findScenarioSession(long sessionId) {
    return scenarioSessionService.requireByLearningSessionId(sessionId);
  }

  private SessionHistoryMessageSnapshot findSubmittedMessage(
      SubmittedMessageContext submittedContext) {
    return conversationMessageService.require(submittedContext.submittedMessageId());
  }

  private void assertSubmittedMessageMatches(
      SubmittedMessageContext submittedContext, SessionHistoryMessageSnapshot submittedMessage) {
    if (!java.util.Objects.equals(
            submittedContext.attemptToken(), submittedMessage.getScenarioAttemptToken())
        || submittedMessage.getScenarioResponsePayload() != null
        || submittedMessage.getRole() != ConversationSpeaker.USER
        || submittedMessage.getMessageSequence() != submittedContext.submittedMessageSequence()
        || submittedMessage.getTurnNumber() != submittedContext.submittedTurnNumber()) {
      throw new ApiException(ErrorCode.CONFLICT, "처리 중인 사용자 메시지가 변경되었습니다.");
    }
  }

  private SessionHistoryMessageSnapshot saveAiMessage(
      SessionHistoryMessageSnapshot submittedMessage, String content, String translatedContent) {
    return conversationMessageService.recordAiGenerated(
        submittedMessage.getSessionHistoryId(),
        submittedMessage.getMessageSequence() + 1,
        submittedMessage.getTurnNumber() + 1,
        content,
        translatedContent);
  }
}
