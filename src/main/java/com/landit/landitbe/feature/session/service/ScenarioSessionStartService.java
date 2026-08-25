// 시나리오 세션 시작 흐름을 조율한다.

package com.landit.landitbe.feature.session.service;

import com.landit.landitbe.feature.content.service.ScenarioProgressionService;
import com.landit.landitbe.feature.learning.service.LearningProgressService;
import com.landit.landitbe.feature.learning.service.ScenarioAccessService;
import com.landit.landitbe.feature.profile.domain.UserProfile;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import com.landit.landitbe.feature.session.domain.LearningSession;
import com.landit.landitbe.feature.session.domain.ScenarioSession;
import com.landit.landitbe.feature.session.domain.SessionHistory;
import com.landit.landitbe.feature.session.domain.SessionHistoryMessage;
import com.landit.landitbe.feature.session.dto.SessionStartResponse;
import com.landit.landitbe.feature.session.dto.SessionStartResponse.CurrentMessageResponse;
import com.landit.landitbe.feature.session.repository.projection.ScenarioSessionStartProjection;
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
  private final ScenarioProgressionService scenarioProgressionService;
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
    UserProfile userProfile = findActiveUser(userId);
    ScenarioSessionStartProjection startRow = findStartRow(userId, scenarioId);

    assertContentActive(startRow);

    // 일반 사용자에게만 복습 권한, 진행 순서, 하루 제한을 검증한다.
    if (enforceProgression) {
      assertCurrentScenarioOrReplay(userProfile, startRow.scenarioId(), startedInstant);
    }

    LocalDateTime now = LocalDateTime.ofInstant(startedInstant, SERVICE_ZONE_ID);

    // 관리자 테스트도 실제 학습과 동일한 진행도와 세션 기록을 남긴다.
    ensureProgress(userProfile, startRow, now);
    LearningSession learningSession = createLearningSession(userId, userProfile, startRow, now);

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

  /** 카테고리 잠금과 시나리오 비활성 상태를 API 오류 코드로 변환한다. */
  private void assertContentActive(ScenarioSessionStartProjection startRow) {
    if (inactive(startRow.categoryStatus())) {
      throw new ApiException(ErrorCode.CATEGORY_LOCKED);
    }

    if (inactive(startRow.scenarioStatus()) || inactive(startRow.variantStatus())) {
      throw new ApiException(ErrorCode.SCENARIO_LOCKED);
    }
  }

  /** 복습 권한이 없으면 해당 시각에 사용자에게 제공된 시나리오인지 확인한다. */
  private void assertCurrentScenarioOrReplay(
      UserProfile userProfile, Long scenarioId, Instant startedInstant) {
    if (scenarioAccessService.hasAccess(
        userProfile.getId(), scenarioId, userProfile.getTargetLocale())) {
      return;
    }

    if (!scenarioProgressionService.isCurrentScenario(
        userProfile.getId(), scenarioId, userProfile.getTargetLocale(), startedInstant)) {
      throw new ApiException(ErrorCode.SCENARIO_LOCKED, DAILY_SCENARIO_NOT_AVAILABLE);
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

    return CurrentMessageResponse.from(message, startRow.openingQuestionAudioUrl());
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
