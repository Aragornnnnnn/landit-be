// 시나리오 세션 시작 유스케이스를 조율한다.

package com.landit.landitbe.feature.session.service;

import com.landit.landitbe.feature.content.dto.TtsVoiceResponse;
import com.landit.landitbe.feature.learning.domain.UserScenarioProgress;
import com.landit.landitbe.feature.learning.domain.UserScenarioProgressStatus;
import com.landit.landitbe.feature.learning.repository.UserScenarioProgressRepository;
import com.landit.landitbe.feature.profile.domain.UserProfile;
import com.landit.landitbe.feature.profile.repository.UserProfileRepository;
import com.landit.landitbe.feature.session.domain.LearningSession;
import com.landit.landitbe.feature.session.domain.ScenarioSession;
import com.landit.landitbe.feature.session.domain.SessionHistory;
import com.landit.landitbe.feature.session.domain.SessionHistoryMessage;
import com.landit.landitbe.feature.session.domain.SessionType;
import com.landit.landitbe.feature.session.dto.SessionStartResponse;
import com.landit.landitbe.feature.session.dto.SessionStartResponse.CurrentMessageResponse;
import com.landit.landitbe.feature.session.dto.SessionStartResponse.SessionProgressResponse;
import com.landit.landitbe.feature.session.repository.LearningSessionRepository;
import com.landit.landitbe.feature.session.repository.ScenarioSessionRepository;
import com.landit.landitbe.feature.session.repository.ScenarioSessionStartQueryRepository;
import com.landit.landitbe.feature.session.repository.SessionHistoryMessageRepository;
import com.landit.landitbe.feature.session.repository.SessionHistoryRepository;
import com.landit.landitbe.feature.session.repository.projection.ScenarioSessionLockProjection;
import com.landit.landitbe.feature.session.repository.projection.ScenarioSessionStartProjection;
import com.landit.landitbe.shared.domain.ActiveStatus;
import com.landit.landitbe.shared.domain.ConversationSpeaker;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 시나리오 세션 시작 유스케이스를 조율한다. */
@RequiredArgsConstructor
@Service
public class ScenarioSessionStartUseCase {

  private static final String PREVIOUS_SCENARIO_NOT_COMPLETED = "PREVIOUS_SCENARIO_NOT_COMPLETED";

  // DB Repository는 현재 기준상 Port로 감싸지 않고
  // UseCase 트랜잭션에서 직접 조율한다.
  private final UserProfileRepository userProfileRepository;
  private final ScenarioSessionStartQueryRepository scenarioSessionStartQueryRepository;
  private final UserScenarioProgressRepository userScenarioProgressRepository;
  private final LearningSessionRepository learningSessionRepository;
  private final ScenarioSessionRepository scenarioSessionRepository;
  private final SessionHistoryRepository sessionHistoryRepository;
  private final SessionHistoryMessageRepository sessionHistoryMessageRepository;

  /** 선택한 시나리오로 학습 세션을 시작한다. */
  @Transactional
  public SessionStartResponse startScenarioSession(long userId, long scenarioId) {
    UserProfile userProfile = findActiveUser(userId);
    ScenarioSessionStartProjection startRow = findStartRow(userId, scenarioId);
    assertPlayable(userId, startRow);

    LocalDateTime now = LocalDateTime.now();
    ensureProgress(userProfile, startRow, now);
    LearningSession learningSession = createLearningSession(userId, userProfile, startRow, now);
    CurrentMessageResponse currentMessage = null;
    if (startRow.firstSpeaker() == ConversationSpeaker.AI) {
      currentMessage = saveAiOpeningMessage(learningSession.getId(), userProfile, startRow, now);
    }

    return toStartResponse(learningSession, startRow, currentMessage);
  }

  /** 세션 시작 흐름을 직렬화할 수 있도록 활성 사용자 프로필을 쓰기 잠금으로 조회한다. */
  private UserProfile findActiveUser(long userId) {
    // 같은 사용자의 동시 세션 시작 요청이 progress row 생성 구간을
    // 동시에 통과하지 못하도록 사용자 row를 잠근다.
    return userProfileRepository
        .findActiveByIdForUpdate(userId)
        .orElseThrow(() -> new ApiException(ErrorCode.INVALID_TOKEN));
  }

  /** 사용자 언어 설정에 맞는 시나리오 시작 콘텐츠와 TTS 정보를 조회한다. */
  private ScenarioSessionStartProjection findStartRow(long userId, long scenarioId) {
    return scenarioSessionStartQueryRepository
        .findStartRow(userId, scenarioId)
        .orElseThrow(() -> new ApiException(ErrorCode.SCENARIO_NOT_FOUND));
  }

  /** 학습 세션에 반드시 연결할 AI 튜터 ID의 존재를 검증한다. */
  private Long requireAiTutorId(UserProfile userProfile) {
    if (userProfile.getAiTutorId() == null) {
      throw new ApiException(ErrorCode.INVALID_REQUEST, "AI 튜터가 설정되지 않았습니다.");
    }
    return userProfile.getAiTutorId();
  }

  /** 콘텐츠 활성 상태와 직전 시나리오 완료 조건을 모두 검증한다. */
  private void assertPlayable(long userId, ScenarioSessionStartProjection startRow) {
    assertContentActive(startRow);
    assertPreviousScenarioCleared(userId, startRow);
  }

  /** 카테고리 잠금과 시나리오 비활성 상태를 API 오류 코드로 변환한다. */
  private void assertContentActive(ScenarioSessionStartProjection startRow) {
    if (inactive(startRow.categoryStatus())) {
      throw new ApiException(ErrorCode.CATEGORY_LOCKED);
    }
    if (inactive(startRow.scenarioStatus()) || inactive(startRow.variantStatus())) {
      throw new ApiException(ErrorCode.SCENARIO_LOCKED);
    }
  }

  /** 같은 카테고리 안에서 displayOrder 기준 직전 시나리오 완료 여부만 확인한다. */
  private void assertPreviousScenarioCleared(long userId, ScenarioSessionStartProjection startRow) {
    Optional<ScenarioSessionLockProjection> previousScenario =
        scenarioSessionStartQueryRepository.findPreviousScenarioLockRow(
            userId, startRow.scenarioId());
    if (previousScenario.isEmpty()) {
      return;
    }
    if (previousScenario.get().progressStatus() != UserScenarioProgressStatus.CLEARED) {
      throw new ApiException(ErrorCode.SCENARIO_LOCKED, PREVIOUS_SCENARIO_NOT_COMPLETED);
    }
  }

  /** 최초 시작과 재시도를 같은 흐름으로 처리하되, 기존 완료 성과는 유지한다. */
  private void ensureProgress(
      UserProfile userProfile, ScenarioSessionStartProjection startRow, LocalDateTime startedAt) {
    userScenarioProgressRepository
        .findByUserProfileIdAndScenarioIdAndTargetLocale(
            userProfile.getId(), startRow.scenarioId(), userProfile.getTargetLocale())
        .ifPresentOrElse(
            progress -> progress.markStarted(startedAt),
            () ->
                userScenarioProgressRepository.save(
                    UserScenarioProgress.start(
                        userProfile.getId(),
                        startRow.scenarioId(),
                        userProfile.getTargetLocale(),
                        startedAt)));
  }

  /** 학습 세션과 시나리오 세션을 함께 생성해 시작한 언어 variant를 연결한다. */
  private LearningSession createLearningSession(
      long userId,
      UserProfile userProfile,
      ScenarioSessionStartProjection startRow,
      LocalDateTime startedAt) {
    LearningSession learningSession =
        learningSessionRepository.save(
            LearningSession.startScenario(
                userId,
                requireAiTutorId(userProfile),
                userProfile.getTargetLocale(),
                userProfile.getBaseLocale(),
                startedAt));
    scenarioSessionRepository.save(
        ScenarioSession.start(
            learningSession.getId(),
            startRow.variantId(),
            startRow.firstSpeaker() == ConversationSpeaker.USER
                ? startRow.userOpeningInstruction()
                : null));
    return learningSession;
  }

  /** AI first 시나리오는 세션 시작과 동시에 히스토리와 첫 AI 메시지를 저장한다. */
  private CurrentMessageResponse saveAiOpeningMessage(
      Long learningSessionId,
      UserProfile userProfile,
      ScenarioSessionStartProjection startRow,
      LocalDateTime startedAt) {
    assertAiOpeningMessageConfigured(startRow);
    SessionHistoryMessage message =
        saveAiOpeningHistoryMessage(learningSessionId, userProfile, startRow, startedAt);
    return toCurrentMessageResponse(message);
  }

  /** AI first 시작 데이터가 비어 있으면 콘텐츠 설정 오류로 본다. */
  private void assertAiOpeningMessageConfigured(ScenarioSessionStartProjection startRow) {
    if (startRow.aiOpeningMessage() == null || startRow.aiOpeningMessage().isBlank()) {
      throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, "AI 시작 메시지가 설정되지 않았습니다.");
    }
  }

  /** AI first 시나리오의 세션 히스토리와 첫 AI 메시지를 저장한다. */
  private SessionHistoryMessage saveAiOpeningHistoryMessage(
      Long learningSessionId,
      UserProfile userProfile,
      ScenarioSessionStartProjection startRow,
      LocalDateTime startedAt) {
    SessionHistory sessionHistory =
        sessionHistoryRepository.save(
            SessionHistory.startedScenario(
                learningSessionId,
                userProfile.getId(),
                userProfile.getTargetLocale(),
                userProfile.getBaseLocale(),
                startedAt));
    SessionHistoryMessage message =
        sessionHistoryMessageRepository.save(
            SessionHistoryMessage.aiOpening(
                sessionHistory.getId(),
                startRow.aiOpeningMessage(),
                startRow.aiOpeningMessageTranslation(),
                startRow.aiOpeningInnerThought(),
                startRow.aiOpeningInnerThoughtType()));
    return message;
  }

  /** 저장된 AI 시작 메시지를 세션 시작 응답의 현재 메시지 형식으로 변환한다. */
  private CurrentMessageResponse toCurrentMessageResponse(SessionHistoryMessage message) {
    return new CurrentMessageResponse(
        message.getId(),
        message.getTurnNumber(),
        message.getMessageSequence(),
        message.getRole().name(),
        message.getContent(),
        message.getTranslatedContent(),
        message.getInnerThought(),
        message.getInnerThoughtType() == null ? null : message.getInnerThoughtType().name());
  }

  /** FirstSpeaker에 맞춰 AI 메시지 또는 USER first 시작 안내만 응답에 담는다. */
  private SessionStartResponse toStartResponse(
      LearningSession learningSession,
      ScenarioSessionStartProjection startRow,
      CurrentMessageResponse currentMessage) {
    String userOpeningInstruction = null;
    if (startRow.firstSpeaker() == ConversationSpeaker.USER) {
      userOpeningInstruction = startRow.userOpeningInstruction();
    }
    return new SessionStartResponse(
        learningSession.getId(),
        startRow.scenarioId(),
        SessionType.SCENARIO.name(),
        startRow.firstSpeaker().name(),
        userOpeningInstruction,
        TtsVoiceResponse.from(
            startRow.ttsVoiceProvider(),
            startRow.ttsVoiceModel(),
            startRow.providerVoiceId(),
            startRow.ttsVoiceGender()),
        currentMessage,
        new SessionProgressResponse(1, startRow.totalQuestionCount(), false));
  }

  /** 활성 상태가 아닌 카테고리와 시나리오 콘텐츠를 잠금 대상으로 판단한다. */
  private boolean inactive(ActiveStatus status) {
    return status != ActiveStatus.ACTIVE;
  }
}
