// 프리톡 세션 시작 레코드의 짧은 트랜잭션 저장과 삭제를 담당한다.

package com.landit.landitbe.feature.session.service;

import com.landit.landitbe.feature.content.dto.TtsVoiceResponse;
import com.landit.landitbe.feature.content.service.AiTutorService;
import com.landit.landitbe.feature.content.service.AiTutorService.FreeTalkPartner;
import com.landit.landitbe.feature.profile.domain.UserProfile;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkOpeningResult;
import com.landit.landitbe.feature.session.domain.FreeTalkSession;
import com.landit.landitbe.feature.session.domain.FreeTalkStartMode;
import com.landit.landitbe.feature.session.domain.FreeTalkTopic;
import com.landit.landitbe.feature.session.domain.LearningSession;
import com.landit.landitbe.feature.session.domain.SessionHistory;
import com.landit.landitbe.feature.session.domain.SessionHistoryMessage;
import com.landit.landitbe.feature.session.dto.FreeTalkSessionStartRequest;
import com.landit.landitbe.feature.session.dto.FreeTalkSessionStartResponse.CurrentMessageResponse;
import com.landit.landitbe.feature.session.repository.FreeTalkSessionRepository;
import com.landit.landitbe.feature.session.repository.FreeTalkTopicRepository;
import com.landit.landitbe.feature.session.repository.LearningSessionRepository;
import com.landit.landitbe.feature.session.repository.SessionHistoryMessageRepository;
import com.landit.landitbe.feature.session.repository.SessionHistoryRepository;
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

  private final UserProfileService userProfileService;
  private final AiTutorService aiTutorService;
  private final LearningSessionRepository learningSessionRepository;
  private final FreeTalkSessionRepository freeTalkSessionRepository;
  private final FreeTalkTopicRepository freeTalkTopicRepository;
  private final SessionHistoryRepository sessionHistoryRepository;
  private final SessionHistoryMessageRepository sessionHistoryMessageRepository;

  /** 사용자 잠금 안에서 프리톡 시작 레코드와 빈 히스토리를 생성한다. */
  @Transactional
  public StartedFreeTalkSession createStart(long userId, FreeTalkSessionStartRequest request) {
    validateStartRequest(request);
    UserProfile userProfile = userProfileService.requireActiveForUpdate(userId);
    FreeTalkTopic topic = findTopic(request);
    FreeTalkPartner partner = requirePartner(userProfile);
    LocalDateTime startedAt = LocalDateTime.now();
    LearningSession learningSession =
        learningSessionRepository.save(
            LearningSession.startFreeTalk(
                userProfile.getId(),
                requireAiTutorId(userProfile),
                userProfile.getTargetLocale(),
                userProfile.getBaseLocale(),
                startedAt));
    FreeTalkSession freeTalkSession =
        freeTalkSessionRepository.save(
            FreeTalkSession.start(
                learningSession.getId(),
                topic == null ? null : topic.getId(),
                request.startMode()));
    if (topic != null) {
      freeTalkSession.assignTitle(topic.getDisplayName());
    }
    SessionHistory sessionHistory =
        sessionHistoryRepository.save(
            SessionHistory.startedFreeTalk(
                learningSession.getId(),
                userProfile.getId(),
                userProfile.getTargetLocale(),
                userProfile.getBaseLocale(),
                startedAt));
    return new StartedFreeTalkSession(
        learningSession.getId(),
        sessionHistory.getId(),
        request.startMode(),
        topic == null ? null : topic.getId(),
        topic == null ? null : topic.getDisplayName(),
        topic == null ? null : topic.getPromptDescription(),
        userProfile.getTargetLocale().name(),
        userProfile.getBaseLocale().name(),
        partner.displayName(),
        partner.accentLocale(),
        TtsVoiceResponse.from(
            partner.ttsVoiceProvider(),
            partner.ttsVoiceModel(),
            partner.ttsVoiceProviderVoiceId(),
            partner.ttsVoiceGender()));
  }

  /** AI opening 생성 결과를 별도 트랜잭션에서 첫 AI 메시지로 저장한다. */
  @Transactional
  public CurrentMessageResponse saveOpening(
      StartedFreeTalkSession startedSession, AiFreeTalkOpeningResult openingResult) {
    SessionHistoryMessage openingMessage =
        sessionHistoryMessageRepository.save(
            SessionHistoryMessage.freeTalkAi(
                startedSession.sessionHistoryId(),
                1,
                1,
                openingResult.aiMessage(),
                openingResult.translatedMessage(),
                openingResult.emotion()));
    return CurrentMessageResponse.from(openingMessage);
  }

  /** AI opening 실패 뒤 시작 중 생성한 모든 레코드를 삭제한다. */
  @Transactional
  public void deleteStart(long learningSessionId) {
    sessionHistoryRepository
        .findByLearningSessionId(learningSessionId)
        .ifPresent(
            history -> {
              sessionHistoryMessageRepository.deleteAll(
                  sessionHistoryMessageRepository.findBySessionHistoryIdOrderByMessageSequenceAsc(
                      history.getId()));
              sessionHistoryMessageRepository.flush();
              sessionHistoryRepository.delete(history);
              sessionHistoryRepository.flush();
            });
    freeTalkSessionRepository
        .findByLearningSessionId(learningSessionId)
        .ifPresent(
            freeTalkSession -> {
              freeTalkSessionRepository.delete(freeTalkSession);
              freeTalkSessionRepository.flush();
            });
    learningSessionRepository.deleteById(learningSessionId);
    learningSessionRepository.flush();
  }

  private void validateStartRequest(FreeTalkSessionStartRequest request) {
    if (request == null || request.startMode() == null) {
      throw new ApiException(ErrorCode.INVALID_REQUEST);
    }
    boolean aiFirstWithTopic =
        request.startMode() == FreeTalkStartMode.AI_FIRST && request.topicId() != null;
    boolean userFirstWithoutTopic =
        request.startMode() == FreeTalkStartMode.USER_FIRST && request.topicId() == null;
    if (!aiFirstWithTopic && !userFirstWithoutTopic) {
      throw new ApiException(ErrorCode.INVALID_REQUEST);
    }
  }

  private FreeTalkTopic findTopic(FreeTalkSessionStartRequest request) {
    if (request.startMode() == FreeTalkStartMode.USER_FIRST) {
      return null;
    }
    return freeTalkTopicRepository
        .findByIdAndStatus(request.topicId(), ActiveStatus.ACTIVE)
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
  }

  private FreeTalkPartner requirePartner(UserProfile userProfile) {
    return aiTutorService.requireFreeTalkPartner(
        requireAiTutorId(userProfile), userProfile.getBaseLocale());
  }

  private Long requireAiTutorId(UserProfile userProfile) {
    if (Objects.isNull(userProfile.getAiTutorId())) {
      throw new ApiException(ErrorCode.DEFAULT_AI_TUTOR_NOT_CONFIGURED);
    }
    return userProfile.getAiTutorId();
  }

  /** 외부 AI 호출과 응답 생성에 필요한 시작 레코드 정보다. */
  public record StartedFreeTalkSession(
      Long learningSessionId,
      Long sessionHistoryId,
      FreeTalkStartMode startMode,
      Long topicId,
      String title,
      String topicPromptDescription,
      String targetLocale,
      String baseLocale,
      String partnerDisplayName,
      String accentLocale,
      TtsVoiceResponse ttsVoice) {}
}
