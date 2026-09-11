// 세션 완료 후 텍스트 수준 평가를 기존 서버 실행기로 비동기 생성하고 저장한다.

package com.landit.landitbe.feature.session.service;

import com.landit.landitbe.feature.profile.service.UserProfileService;
import com.landit.landitbe.feature.session.client.ai.AiConversationClient;
import com.landit.landitbe.feature.session.client.ai.AiSessionFeedbackRequest;
import com.landit.landitbe.feature.session.client.ai.AiSessionLevelAssessment;
import com.landit.landitbe.feature.session.domain.LearningSession;
import com.landit.landitbe.feature.session.domain.ProcessingStatus;
import com.landit.landitbe.feature.session.domain.UserLevelAssessment;
import com.landit.landitbe.feature.session.dto.SessionLevelAssessmentResponse;
import com.landit.landitbe.feature.session.repository.UserLevelAssessmentRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** 세션 완료 후 텍스트 수준 평가를 기존 서버 실행기로 비동기 생성하고 저장한다. */
@Service
@Slf4j
public class SessionLevelAssessmentGenerationService {

  private static final Duration PREPARING_TIMEOUT = Duration.ofSeconds(120);

  private final LearningSessionService learningSessionService;
  private final UserProfileService userProfileService;
  private final SessionFeedbackContextService contextService;
  private final SessionLevelAssessmentService assessmentService;
  private final UserLevelAssessmentRepository assessmentRepository;
  private final AiConversationClient aiConversationClient;
  private final TransactionTemplate transactionTemplate;
  private final TaskExecutor taskExecutor;
  private final Clock clock;
  private final SessionLevelAssessmentLaunchService launchService;

  SessionLevelAssessmentGenerationService(
      LearningSessionService learningSessionService,
      UserProfileService userProfileService,
      SessionFeedbackContextService contextService,
      SessionLevelAssessmentService assessmentService,
      UserLevelAssessmentRepository assessmentRepository,
      AiConversationClient aiConversationClient,
      PlatformTransactionManager transactionManager,
      @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor,
      Clock clock,
      SessionLevelAssessmentLaunchService launchService) {
    this.learningSessionService = learningSessionService;
    this.userProfileService = userProfileService;
    this.contextService = contextService;
    this.assessmentService = assessmentService;
    this.assessmentRepository = assessmentRepository;
    this.aiConversationClient = aiConversationClient;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
    this.taskExecutor = taskExecutor;
    this.clock = clock;
    this.launchService = launchService;
  }

  /** 완료 트랜잭션에서 예약한 평가를 서버 내부 실행기로 시작한다. */
  private void dispatch(long userId, LoadedSessionFeedbackContext context) {
    try {
      taskExecutor.execute(() -> generateAndPersist(userId, context));
    } catch (RuntimeException exception) {
      log.warn("수준 평가 비동기 작업 등록에 실패했습니다. sessionId={}", context.sessionId(), exception);
      completeFallback(userId, context);
    }
  }

  /**
   * 완료 커밋 이후 세션 컨텍스트를 조회해 수준 평가를 시작한다.
   *
   * @param userId 세션 소유 사용자 ID
   * @param sessionId 완료 트랜잭션에서 평가가 예약된 세션 ID
   */
  public void startIfNeeded(long userId, long sessionId) {
    if (!launchService.isEnabledFor(userId)) {
      return;
    }
    try {
      LearningSession session = learningSessionService.findOwned(userId, sessionId);
      if (!canProcess(session)) {
        return;
      }
      LoadedSessionFeedbackContext context = contextService.load(userId, sessionId);
      dispatch(userId, context);
    } catch (RuntimeException exception) {
      log.warn("수준 평가 시작에 필요한 세션 조회에 실패했습니다. sessionId={}", sessionId, exception);
    }
  }

  /**
   * 수준 평가 상태와 저장된 결과를 조회하고 만료된 작업은 fallback으로 종료한다.
   *
   * @param userId 세션 소유 사용자 ID
   * @param sessionId 조회할 세션 ID
   * @return 처리 상태와 저장된 평가 결과. 비활성 또는 도입 전 완료 세션이면 null
   */
  public @Nullable SessionLevelAssessmentResponse get(long userId, long sessionId) {
    LearningSession session = learningSessionService.findOwned(userId, sessionId);
    if (!launchService.isEnabledFor(userId)
        || (session.getEndedAt() != null
            && !launchService.includes(session.getUserProfileId(), session.getEndedAt()))) {
      return null;
    }
    UserLevelAssessment assessment =
        assessmentRepository.findByLearningSessionId(sessionId).orElse(null);
    if (isExpired(session, assessment)) {
      LoadedSessionFeedbackContext context = contextService.load(userId, sessionId);
      completeFallback(userId, context);
      session = learningSessionService.findOwned(userId, sessionId);
      assessment = assessmentRepository.findByLearningSessionId(sessionId).orElse(null);
    }
    return SessionLevelAssessmentResponse.from(
        session, assessment == null ? null : assessment.toAssessment());
  }

  private void generateAndPersist(long userId, LoadedSessionFeedbackContext context) {
    AiSessionLevelAssessment aiAssessment = null;
    try {
      LearningSession session = learningSessionService.findOwned(userId, context.sessionId());
      if (!canProcess(session)) {
        return;
      }
      if (isExpired(session, null)) {
        completeFallback(userId, context);
        return;
      }
      AiSessionFeedbackRequest request = SessionFeedbackService.toAiLevelAssessmentRequest(context);
      aiAssessment = aiConversationClient.generateSessionLevelAssessment(request);
    } catch (RuntimeException exception) {
      log.warn("수준 평가 AI 호출에 실패해 fallback을 사용합니다. sessionId={}", context.sessionId(), exception);
    }
    persist(userId, context, aiAssessment);
  }

  private void persist(
      long userId, LoadedSessionFeedbackContext context, AiSessionLevelAssessment aiAssessment) {
    try {
      transactionTemplate.executeWithoutResult(
          status -> {
            userProfileService.requireActiveForUpdate(userId);
            LearningSession session =
                learningSessionService.findOwnedCompletedForUpdate(userId, context.sessionId());
            if (!canProcess(session)
                || assessmentRepository.findByLearningSessionId(context.sessionId()).isPresent()) {
              return;
            }
            assessmentService.assessApplyAndSave(
                userId,
                context,
                isExpired(session, null) ? null : aiAssessment,
                learningSessionService.isLatestCompletedScenario(session),
                session.getLevelAssessmentRequestedAt());
            session.completeLevelAssessment();
          });
    } catch (RuntimeException exception) {
      markFailed(userId, context.sessionId());
      log.error("수준 평가 결과 저장에 실패했습니다. sessionId={}", context.sessionId(), exception);
    }
  }

  private void completeFallback(long userId, LoadedSessionFeedbackContext context) {
    persist(userId, context, null);
  }

  private void markFailed(long userId, long sessionId) {
    try {
      transactionTemplate.executeWithoutResult(
          status -> {
            LearningSession session =
                learningSessionService.findOwnedCompletedForUpdate(userId, sessionId);
            if (session.getLevelAssessmentProcessingStatus() == ProcessingStatus.PREPARING) {
              session.failLevelAssessment();
            }
          });
    } catch (RuntimeException ignored) {
      // DB 장애 중에는 상태도 저장할 수 없으므로 다음 조회에서 오류를 반환한다.
    }
  }

  private boolean isExpired(LearningSession session, UserLevelAssessment assessment) {
    return assessment == null
        && session.getLevelAssessmentProcessingStatus() == ProcessingStatus.PREPARING
        && session.getLevelAssessmentRequestedAt() != null
        && !session
            .getLevelAssessmentRequestedAt()
            .plus(PREPARING_TIMEOUT)
            .isAfter(LocalDateTime.now(clock));
  }

  private boolean canProcess(LearningSession session) {
    return launchService.includes(session.getUserProfileId(), session.getEndedAt())
        && session.getLevelAssessmentProcessingStatus() == ProcessingStatus.PREPARING;
  }
}
