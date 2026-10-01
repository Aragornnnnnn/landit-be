// 프리톡 세션 시작 레코드의 짧은 트랜잭션 저장과 삭제를 담당한다.

package com.landit.landitbe.feature.learning.freetalk.start.service;

import com.landit.landitbe.feature.content.exception.ContentErrorCode;
import com.landit.landitbe.feature.content.tutor.dto.TtsVoiceResponse;
import com.landit.landitbe.feature.content.tutor.service.ConversationCharacterService;
import com.landit.landitbe.feature.learning.conversation.dto.LearningSessionSnapshot;
import com.landit.landitbe.feature.learning.conversation.dto.SessionHistoryMessageSnapshot;
import com.landit.landitbe.feature.learning.conversation.dto.SessionHistorySnapshot;
import com.landit.landitbe.feature.learning.conversation.history.service.ConversationMessageService;
import com.landit.landitbe.feature.learning.conversation.history.service.SessionHistoryService;
import com.landit.landitbe.feature.learning.conversation.service.LearningSessionService;
import com.landit.landitbe.feature.learning.freetalk.domain.FreeTalkCharacter;
import com.landit.landitbe.feature.learning.freetalk.domain.FreeTalkSession;
import com.landit.landitbe.feature.learning.freetalk.domain.FreeTalkStartMode;
import com.landit.landitbe.feature.learning.freetalk.followup.dto.AiFreeTalkPendingFollowUp;
import com.landit.landitbe.feature.learning.freetalk.followup.service.FreeTalkFollowUpService;
import com.landit.landitbe.feature.learning.freetalk.message.client.ai.AiFreeTalkOpeningResult;
import com.landit.landitbe.feature.learning.freetalk.repository.FreeTalkSessionRepository;
import com.landit.landitbe.feature.learning.freetalk.start.dto.FreeTalkSessionStartRequest;
import com.landit.landitbe.feature.learning.freetalk.start.dto.FreeTalkSessionStartResponse.CurrentMessageResponse;
import com.landit.landitbe.feature.learning.freetalk.start.dto.StartedFreeTalkSession;
import com.landit.landitbe.feature.learning.freetalk.topic.domain.FreeTalkTopic;
import com.landit.landitbe.feature.learning.freetalk.topic.repository.FreeTalkTopicRepository;
import com.landit.landitbe.feature.learning.freetalk.usage.service.FreeTalkDailySpeakingUsageService;
import com.landit.landitbe.feature.profile.learning.dto.UserLearningProfile;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import com.landit.landitbe.shared.domain.ActiveStatus;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 프리톡 세션 시작 레코드의 짧은 트랜잭션 저장과 삭제를 담당한다. */
@RequiredArgsConstructor
@Service
public class FreeTalkSessionService {

  private final com.landit.landitbe.feature.subscription.service.LearningAccessGrantService
      accessGrants;
  private final UserProfileService userProfileService;
  private final com.landit.landitbe.feature.learning.freetalk.context.service
          .FreeTalkContextSummaryService
      contextSummaryService;
  private final LearningSessionService learningSessionService;
  private final FreeTalkSessionRepository freeTalkSessionRepository;
  private final FreeTalkTopicRepository freeTalkTopicRepository;
  private final SessionHistoryService sessionHistoryService;
  private final ConversationMessageService conversationMessageService;
  private final FreeTalkDailySpeakingUsageService dailySpeakingUsageService;
  private final ConversationCharacterService conversationCharacterService;
  private final FreeTalkFollowUpService followUpService;

  /**
   * 사용자 잠금 안에서 프리톡 시작 레코드와 빈 히스토리를 생성한다.
   *
   * @param userId 세션을 시작할 사용자 ID
   * @param request 시작 방식과 선택 주제
   * @return 외부 AI 호출에 사용할 시작 레코드
   * @throws ApiException 요청, 사용자, 주제 또는 AI 상대 설정이 유효하지 않을 때
   * @throws com.landit.landitbe.feature.learning.conversation.exception.SessionException 당일 발화 한도
   *     또는 일일·분당 요청 한도에 도달했을 때
   */
  @Transactional
  public StartedFreeTalkSession createStart(long userId, FreeTalkSessionStartRequest request) {
    validateStartRequest(request);
    UserLearningProfile userProfile = userProfileService.requireActiveForUpdate(userId);
    var startAccess = accessGrants.requirePremiumStart(userId);
    dailySpeakingUsageService.requireRemaining(userId);
    FreeTalkTopic topic = findTopic(request);
    FreeTalkCharacter character = FreeTalkCharacter.fromId(request.characterId());
    final TtsVoiceResponse ttsVoice =
        conversationCharacterService.requireActiveTtsVoice(character.id());
    dailySpeakingUsageService.reserveRequest(userId);
    LocalDateTime startedAt = LocalDateTime.now();
    LearningSessionSnapshot learningSession =
        learningSessionService.startFreeTalk(
            userProfile.id(),
            requireAiTutorId(userProfile),
            userProfile.targetLocale(),
            userProfile.baseLocale(),
            startedAt);
    accessGrants.recordFreeTalk(userId, learningSession.getId(), startedAt, startAccess);
    FreeTalkSession freeTalkSession =
        freeTalkSessionRepository.save(
            FreeTalkSession.start(
                learningSession.getId(),
                topic == null ? null : topic.getId(),
                request.startMode(),
                character));
    AiFreeTalkPendingFollowUp pendingFollowUp =
        request.followUpId() == null
            ? null
            : followUpService.claim(
                userProfile.id(), character.id(), request.followUpId(), freeTalkSession.getId());
    contextSummaryService.initialize(userId, freeTalkSession.getId());
    if (topic != null) {
      freeTalkSession.assignTitle(topic.getDisplayName());
    } else if (pendingFollowUp != null) {
      freeTalkSession.assignTitle("지난 대화 이어가기");
    }
    SessionHistorySnapshot sessionHistory =
        sessionHistoryService.startFreeTalk(
            learningSession.getId(),
            userProfile.id(),
            userProfile.targetLocale(),
            userProfile.baseLocale(),
            startedAt);
    return new StartedFreeTalkSession(
        learningSession.getId(),
        sessionHistory.getId(),
        freeTalkSession.getId(),
        request.startMode(),
        character.id(),
        topic == null ? null : topic.getId(),
        freeTalkSession.getTitle(),
        topic == null ? null : topic.getPromptDescription(),
        userProfile.targetLocale().name(),
        userProfile.baseLocale().name(),
        ttsVoice,
        pendingFollowUp);
  }

  /**
   * AI opening 생성 결과를 별도 트랜잭션에서 첫 AI 메시지로 저장한다.
   *
   * @param startedSession 생성 직후의 프리톡 시작 레코드
   * @param openingResult AI가 생성한 첫 메시지
   * @return 저장된 첫 AI 메시지 응답
   */
  @Transactional
  public CurrentMessageResponse saveOpening(
      StartedFreeTalkSession startedSession, AiFreeTalkOpeningResult openingResult) {
    if (startedSession.pendingFollowUp() != null
        && (!openingResult.followUpAsked()
            || !Long.valueOf(startedSession.pendingFollowUp().followUpId())
                .equals(openingResult.followUpId()))) {
      throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
    }
    SessionHistoryMessageSnapshot openingMessage =
        conversationMessageService.recordFreeTalkAi(
            startedSession.sessionHistoryId(),
            1,
            1,
            openingResult.aiMessage(),
            openingResult.translatedMessage(),
            openingResult.emotion());
    if (startedSession.pendingFollowUp() != null) {
      followUpService.markAsked(
          startedSession.pendingFollowUp().followUpId(),
          startedSession.freeTalkSessionId(),
          openingMessage.getId());
    }
    return CurrentMessageResponse.from(openingMessage);
  }

  /**
   * AI opening 실패 뒤 시작 중 생성한 모든 레코드를 삭제한다.
   *
   * @param learningSessionId 삭제할 학습 세션 ID
   */
  @Transactional
  public void deleteStart(long learningSessionId) {
    sessionHistoryService
        .findByLearningSessionId(learningSessionId)
        .ifPresent(
            history -> {
              conversationMessageService.deleteHistoryMessages(history.getId());
              sessionHistoryService.deleteStart(history.getId());
            });
    freeTalkSessionRepository
        .findByLearningSessionId(learningSessionId)
        .ifPresent(
            freeTalkSession -> {
              followUpService.release(freeTalkSession.getId());
              freeTalkSessionRepository.delete(freeTalkSession);
              freeTalkSessionRepository.flush();
            });
    learningSessionService.deleteStart(learningSessionId);
  }

  private void validateStartRequest(FreeTalkSessionStartRequest request) {
    if (request == null || request.startMode() == null || request.characterId() == null) {
      throw new ApiException(ErrorCode.INVALID_REQUEST);
    }
    boolean aiFirstWithTopic =
        request.startMode() == FreeTalkStartMode.AI_FIRST
            && (request.topicId() != null) != (request.followUpId() != null);
    boolean userFirstWithoutTopic =
        request.startMode() == FreeTalkStartMode.USER_FIRST
            && request.topicId() == null
            && request.followUpId() == null;
    if ((!aiFirstWithTopic && !userFirstWithoutTopic)
        || (request.followUpId() != null && request.followUpId() <= 0)) {
      throw new ApiException(ErrorCode.INVALID_REQUEST);
    }
  }

  private FreeTalkTopic findTopic(FreeTalkSessionStartRequest request) {
    if (request.startMode() == FreeTalkStartMode.USER_FIRST || request.followUpId() != null) {
      return null;
    }
    return freeTalkTopicRepository
        .findByIdAndStatus(request.topicId(), ActiveStatus.ACTIVE)
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
  }

  private Long requireAiTutorId(UserLearningProfile userProfile) {
    if (Objects.isNull(userProfile.aiTutorId())) {
      throw new ApiException(ContentErrorCode.DEFAULT_AI_TUTOR_NOT_CONFIGURED);
    }
    return userProfile.aiTutorId();
  }
}
