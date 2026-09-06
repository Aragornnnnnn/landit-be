// 세션 완료 후 텍스트 수준 평가를 기존 서버 실행기로 비동기 생성하고 저장한다.

package com.landit.landitbe.feature.session.service;

import com.landit.landitbe.feature.session.client.ai.AiConversationClient;
import com.landit.landitbe.feature.session.client.ai.AiSessionFeedbackRequest;
import com.landit.landitbe.feature.session.client.ai.AiSessionLevelAssessment;
import com.landit.landitbe.feature.session.domain.LearningSession;
import com.landit.landitbe.feature.session.domain.ProcessingStatus;
import com.landit.landitbe.feature.session.domain.UserLevelAssessment;
import com.landit.landitbe.feature.session.dto.SessionLevelAssessmentResponse;
import com.landit.landitbe.feature.session.repository.UserLevelAssessmentRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
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
  private final SessionFeedbackContextService contextService;
  private final SessionLevelAssessmentService assessmentService;
  private final UserLevelAssessmentRepository assessmentRepository;
  private final AiConversationClient aiConversationClient;
  private final TransactionTemplate transactionTemplate;
  private final TaskExecutor taskExecutor;

  SessionLevelAssessmentGenerationService(
      LearningSessionService learningSessionService,
      SessionFeedbackContextService contextService,
      SessionLevelAssessmentService assessmentService,
      UserLevelAssessmentRepository assessmentRepository,
      AiConversationClient aiConversationClient,
      PlatformTransactionManager transactionManager,
      @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor) {
    this.learningSessionService = learningSessionService;
    this.contextService = contextService;
    this.assessmentService = assessmentService;
    this.assessmentRepository = assessmentRepository;
    this.aiConversationClient = aiConversationClient;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
    this.taskExecutor = taskExecutor;
  }

  /** 평가 작업을 한 번만 예약하고 서버 내부 실행기로 시작한다. */
  public void startIfNeeded(long userId, LoadedSessionFeedbackContext context) {
    Boolean reserved =
        transactionTemplate.execute(
            status -> reserve(userId, context.sessionId(), LocalDateTime.now()));
    if (!Boolean.TRUE.equals(reserved)) {
      return;
    }
    try {
      taskExecutor.execute(() -> generateAndPersist(userId, context));
    } catch (RuntimeException exception) {
      log.warn("수준 평가 비동기 작업 등록에 실패했습니다. sessionId={}", context.sessionId(), exception);
      completeFallback(userId, context);
    }
  }

  /** 수준 평가 상태와 저장된 결과를 조회하고 만료된 작업은 fallback으로 종료한다. */
  public SessionLevelAssessmentResponse get(long userId, long sessionId) {
    LearningSession session = learningSessionService.findOwned(userId, sessionId);
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

  private boolean reserve(long userId, long sessionId, LocalDateTime requestedAt) {
    LearningSession session = learningSessionService.findOwnedCompletedForUpdate(userId, sessionId);
    if (assessmentRepository.findByLearningSessionId(sessionId).isPresent()
        || session.getLevelAssessmentProcessingStatus() != null) {
      return false;
    }
    session.prepareLevelAssessment(requestedAt);
    return true;
  }

  private void generateAndPersist(long userId, LoadedSessionFeedbackContext context) {
    AiSessionLevelAssessment aiAssessment = null;
    try {
      AiSessionFeedbackRequest request = SessionFeedbackService.toAiRequest(context);
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
            LearningSession session =
                learningSessionService.findOwnedCompletedForUpdate(userId, context.sessionId());
            if (session.getLevelAssessmentProcessingStatus() != ProcessingStatus.PREPARING
                || assessmentRepository.findByLearningSessionId(context.sessionId()).isPresent()) {
              return;
            }
            assessmentService.assessApplyAndSave(userId, context, aiAssessment);
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
            .isAfter(LocalDateTime.now());
  }
}
