// 시나리오 세션 시작 흐름을 조율한다.

package com.landit.landitbe.feature.session.scenario.service;

import com.landit.landitbe.feature.content.domain.ContentLearningLevel;
import com.landit.landitbe.feature.content.scenario.dto.ScenarioStartContext;
import com.landit.landitbe.feature.content.scenario.service.CurrentScenarioSelectionService;
import com.landit.landitbe.feature.learning.access.service.ScenarioAccessService;
import com.landit.landitbe.feature.learning.progress.service.LearningProgressService;
import com.landit.landitbe.feature.profile.dto.UserLearningProfile;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import com.landit.landitbe.feature.session.domain.LearningSession;
import com.landit.landitbe.feature.session.history.domain.SessionHistory;
import com.landit.landitbe.feature.session.history.domain.SessionHistoryMessage;
import com.landit.landitbe.feature.session.history.service.SessionHistoryService;
import com.landit.landitbe.feature.session.scenario.domain.ScenarioSession;
import com.landit.landitbe.feature.session.scenario.dto.SessionStartResponse;
import com.landit.landitbe.feature.session.scenario.dto.SessionStartResponse.CurrentMessageResponse;
import com.landit.landitbe.feature.session.service.LearningSessionService;
import com.landit.landitbe.shared.domain.ActiveStatus;
import com.landit.landitbe.shared.domain.ConversationSpeaker;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 시나리오 세션 시작 흐름을 조율한다. */
@RequiredArgsConstructor
@Service
@Slf4j
public class ScenarioSessionStartService {

  private static final String DAILY_SCENARIO_NOT_AVAILABLE = "DAILY_SCENARIO_NOT_AVAILABLE";
  private static final ZoneId SERVICE_ZONE_ID = ZoneId.of("Asia/Seoul");

  private final UserProfileService userProfileService;
  private final LearningProgressService learningProgressService;
  private final ScenarioAccessService scenarioAccessService;
  private final CurrentScenarioSelectionService scenarioProgressionService;
  private final LearningSessionService learningSessionService;
  private final ScenarioSessionService scenarioSessionService;
  private final SessionHistoryService sessionHistoryService;
  private final SessionMessageService sessionMessageService;
  private final Clock clock;

  /**
   * 선택한 시나리오의 접근 조건을 검증하고 학습 세션을 시작한다.
   *
   * @param userId 세션을 시작할 사용자 ID
   * @param scenarioId 학습할 시나리오 ID
   * @return 생성된 세션과 첫 메시지 정보
   * @throws ApiException 시나리오가 없거나 잠겨 있거나 시작 조건을 충족하지 못했을 때
   */
  @Transactional
  public SessionStartResponse startScenarioSession(long userId, long scenarioId) {
    return startScenarioSession(userId, scenarioId, true);
  }

  /** 일반 사용자와 관리자 테스트의 공통 세션 시작 흐름을 처리한다. */
  private SessionStartResponse startScenarioSession(
      long userId, long scenarioId, boolean enforceProgression) {
    Instant startedInstant = clock.instant();
    UserLearningProfile userProfile = findActiveUser(userId);
    ContentLearningLevel questionLevelGroup =
        ContentLearningLevel.from(userProfile.learningLevel());
    ScenarioStartContext startRow = findStartRow(userId, scenarioId, questionLevelGroup);

    assertContentActive(startRow);

    // 일반 사용자에게만 복습 권한, 진행 순서, 하루 제한을 검증한다.
    if (enforceProgression) {
      assertCurrentScenarioOrReplay(userProfile, startRow.scenarioId(), startedInstant);
    }

    LocalDateTime now = LocalDateTime.ofInstant(startedInstant, SERVICE_ZONE_ID);

    // 관리자 테스트도 실제 학습과 동일한 진행도와 세션 기록을 남긴다.
    ensureProgress(userProfile, startRow, now);
    LearningSession learningSession =
        createLearningSession(userId, userProfile, startRow, questionLevelGroup, now);

    CurrentMessageResponse currentMessage = null;
    if (startRow.firstSpeaker() == ConversationSpeaker.AI) {
      currentMessage = saveAiOpeningMessage(learningSession.getId(), userProfile, startRow, now);
    }

    SessionStartResponse response =
        SessionStartResponse.from(learningSession, startRow, currentMessage);
    log.info(
        "scenario session started: userId={}, scenarioId={}, sessionId={}",
        userId,
        scenarioId,
        learningSession.getId());
    return response;
  }

  /** 개발 환경 전용 관리자 Service에서 진행 제한 없이 세션을 시작할 때 사용한다. */
  SessionStartResponse startScenarioSessionWithoutProgression(long userId, long scenarioId) {
    return startScenarioSession(userId, scenarioId, false);
  }

  /** 세션 시작 흐름을 직렬화할 수 있도록 활성 사용자 프로필을 쓰기 잠금으로 조회한다. */
  private UserLearningProfile findActiveUser(long userId) {
    // 같은 사용자의 동시 세션 시작 요청이 progress row 생성 구간을
    // 동시에 통과하지 못하도록 사용자 row를 잠근다.
    return userProfileService.requireActiveForUpdate(userId);
  }

  /** 사용자 언어 설정에 맞는 시나리오 시작 콘텐츠와 TTS 정보를 조회한다. */
  private ScenarioStartContext findStartRow(
      long userId, long scenarioId, ContentLearningLevel questionLevelGroup) {
    return scenarioSessionService.requireStartProjection(userId, scenarioId, questionLevelGroup);
  }

  /** 학습 세션에 반드시 연결할 AI 튜터 ID의 존재를 검증한다. */
  private Long requireAiTutorId(UserLearningProfile userProfile) {
    if (userProfile.aiTutorId() == null) {
      throw new ApiException(ErrorCode.INVALID_REQUEST, "AI 튜터가 설정되지 않았습니다.");
    }
    return userProfile.aiTutorId();
  }

  /** 카테고리 잠금과 시나리오 비활성 상태를 API 오류 코드로 변환한다. */
  private void assertContentActive(ScenarioStartContext startRow) {
    if (inactive(startRow.categoryStatus())) {
      throw new ApiException(ErrorCode.CATEGORY_LOCKED);
    }

    if (inactive(startRow.scenarioStatus()) || inactive(startRow.variantStatus())) {
      throw new ApiException(ErrorCode.SCENARIO_LOCKED);
    }
  }

  /** 복습 권한이 없으면 해당 시각에 사용자에게 제공된 시나리오인지 확인한다. */
  private void assertCurrentScenarioOrReplay(
      UserLearningProfile userProfile, Long scenarioId, Instant startedInstant) {
    if (scenarioAccessService.hasAccess(userProfile.id(), scenarioId, userProfile.targetLocale())) {
      return;
    }

    if (!scenarioProgressionService.isCurrentScenario(
        userProfile.id(), scenarioId, userProfile.targetLocale(), startedInstant)) {
      throw new ApiException(ErrorCode.SCENARIO_LOCKED, DAILY_SCENARIO_NOT_AVAILABLE);
    }
  }

  /** 최초 시작과 재시도를 같은 흐름으로 처리하되, 기존 완료 성과는 유지한다. */
  private void ensureProgress(
      UserLearningProfile userProfile, ScenarioStartContext startRow, LocalDateTime startedAt) {
    learningProgressService.startScenario(
        userProfile.id(), startRow.scenarioId(), userProfile.targetLocale(), startedAt);
  }

  /** 학습 세션과 시나리오 세션을 함께 생성해 시작한 언어 variant를 연결한다. */
  private LearningSession createLearningSession(
      long userId,
      UserLearningProfile userProfile,
      ScenarioStartContext startRow,
      ContentLearningLevel questionLevelGroup,
      LocalDateTime startedAt) {
    LearningSession learningSession =
        learningSessionService.save(
            LearningSession.startScenario(
                userId,
                requireAiTutorId(userProfile),
                userProfile.targetLocale(),
                userProfile.baseLocale(),
                startedAt));

    scenarioSessionService.save(
        ScenarioSession.start(
            learningSession.getId(),
            startRow.variantId(),
            questionLevelGroup,
            startRow.firstSpeaker() == ConversationSpeaker.USER
                ? startRow.userOpeningInstruction()
                : null));

    return learningSession;
  }

  /** AI first 시나리오는 세션 시작과 동시에 히스토리와 첫 AI 메시지를 저장한다. */
  private CurrentMessageResponse saveAiOpeningMessage(
      Long learningSessionId,
      UserLearningProfile userProfile,
      ScenarioStartContext startRow,
      LocalDateTime startedAt) {
    assertAiOpeningMessageConfigured(startRow);

    SessionHistoryMessage message =
        saveAiOpeningHistoryMessage(learningSessionId, userProfile, startRow, startedAt);

    return CurrentMessageResponse.from(message, startRow.openingQuestionAudioUrl());
  }

  /** AI first 시작 데이터가 비어 있으면 콘텐츠 설정 오류로 본다. */
  private void assertAiOpeningMessageConfigured(ScenarioStartContext startRow) {
    if (startRow.aiOpeningMessage() == null || startRow.aiOpeningMessage().isBlank()) {
      throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, "AI 시작 메시지가 설정되지 않았습니다.");
    }
  }

  /** AI first 시나리오의 세션 히스토리와 첫 AI 메시지를 저장한다. */
  private SessionHistoryMessage saveAiOpeningHistoryMessage(
      Long learningSessionId,
      UserLearningProfile userProfile,
      ScenarioStartContext startRow,
      LocalDateTime startedAt) {
    SessionHistory sessionHistory =
        sessionHistoryService.save(
            SessionHistory.startedScenario(
                learningSessionId,
                userProfile.id(),
                userProfile.targetLocale(),
                userProfile.baseLocale(),
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
