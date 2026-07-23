// 시나리오 세션 시작 흐름을 조율한다.

package com.landit.landitbe.feature.session.service;

import com.landit.landitbe.feature.learning.domain.UserScenarioProgressStatus;
import com.landit.landitbe.feature.learning.service.LearningProgressService;
import com.landit.landitbe.feature.profile.domain.UserProfile;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import com.landit.landitbe.feature.session.domain.LearningSession;
import com.landit.landitbe.feature.session.domain.ScenarioSession;
import com.landit.landitbe.feature.session.domain.SessionHistory;
import com.landit.landitbe.feature.session.domain.SessionHistoryMessage;
import com.landit.landitbe.feature.session.dto.SessionStartResponse;
import com.landit.landitbe.feature.session.dto.SessionStartResponse.CurrentMessageResponse;
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

/** 시나리오 세션 시작 흐름을 조율한다. */
@RequiredArgsConstructor
@Service
public class ScenarioSessionStartService {

  private static final String PREVIOUS_SCENARIO_NOT_COMPLETED = "PREVIOUS_SCENARIO_NOT_COMPLETED";

  private final UserProfileService userProfileService;
  private final LearningProgressService learningProgressService;
  private final LearningSessionService learningSessionService;
  private final ScenarioSessionService scenarioSessionService;
  private final SessionHistoryService sessionHistoryService;
  private final SessionMessageService sessionMessageService;

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

    return SessionStartResponse.from(learningSession, startRow, currentMessage);
  }

  /** 세션 시작 흐름을 직렬화할 수 있도록 활성 사용자 프로필을 쓰기 잠금으로 조회한다. */
  private UserProfile findActiveUser(long userId) {
    // 같은 사용자의 동시 세션 시작 요청이 progress row 생성 구간을
    // 동시에 통과하지 못하도록 사용자 row를 잠근다.
    return userProfileService.requireActiveForUpdate(userId);
  }

  /** 사용자 언어 설정에 맞는 시나리오 시작 콘텐츠와 TTS 정보를 조회한다. */
  private ScenarioSessionStartProjection findStartRow(long userId, long scenarioId) {
    return scenarioSessionService.requireStartProjection(userId, scenarioId);
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
        scenarioSessionService.findPreviousScenarioLock(userId, startRow.scenarioId());
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
    learningProgressService.startScenario(
        userProfile.getId(), startRow.scenarioId(), userProfile.getTargetLocale(), startedAt);
  }

  /** 학습 세션과 시나리오 세션을 함께 생성해 시작한 언어 variant를 연결한다. */
  private LearningSession createLearningSession(
      long userId,
      UserProfile userProfile,
      ScenarioSessionStartProjection startRow,
      LocalDateTime startedAt) {
    LearningSession learningSession =
        learningSessionService.save(
            LearningSession.startScenario(
                userId,
                requireAiTutorId(userProfile),
                userProfile.getTargetLocale(),
                userProfile.getBaseLocale(),
                startedAt));
    scenarioSessionService.save(
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
    return CurrentMessageResponse.from(message);
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
        sessionHistoryService.save(
            SessionHistory.startedScenario(
                learningSessionId,
                userProfile.getId(),
                userProfile.getTargetLocale(),
                userProfile.getBaseLocale(),
                startedAt));
    SessionHistoryMessage message =
        sessionMessageService.save(
            SessionHistoryMessage.aiOpening(
                sessionHistory.getId(),
                startRow.aiOpeningMessage(),
                startRow.aiOpeningMessageTranslation(),
                startRow.aiOpeningInnerThought(),
                startRow.aiOpeningInnerThoughtType()));
    return message;
  }

  /** 활성 상태가 아닌 카테고리와 시나리오 콘텐츠를 잠금 대상으로 판단한다. */
  private boolean inactive(ActiveStatus status) {
    return status != ActiveStatus.ACTIVE;
  }
}
