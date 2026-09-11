// 만료된 프리톡 표현 생성만 복구하고 다른 인스턴스의 진행 작업은 유지한다.

package com.landit.landitbe.feature.session.service;

import com.landit.landitbe.config.ai.AiClientProperties;
import com.landit.landitbe.feature.session.domain.ExpressionGenerationStatus;
import com.landit.landitbe.feature.session.repository.FreeTalkSessionRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** 기존 입력으로 다시 실행 가능한 표현 작업만 복구한다. */
@Service
public class FreeTalkExpressionGenerationRecoveryService {
  private final FreeTalkSessionRepository repository;
  private final FreeTalkExpressionGenerationService generation;
  private final AiClientProperties properties;
  private final Clock clock;
  private final TransactionTemplate transaction;
  private final TaskExecutor executor;

  /** 외부 AI 실행과 만료 판정을 별도 트랜잭션으로 연결한다. */
  public FreeTalkExpressionGenerationRecoveryService(
      FreeTalkSessionRepository repository,
      FreeTalkExpressionGenerationService generation,
      AiClientProperties properties,
      Clock clock,
      PlatformTransactionManager manager,
      @Qualifier("applicationTaskExecutor") TaskExecutor executor) {
    this.repository = repository;
    this.generation = generation;
    this.properties = properties;
    this.clock = clock;
    this.transaction = new TransactionTemplate(manager);
    this.executor = executor;
  }

  /** 시작 및 주기 점검에서 만료된 임대만 해제하고 최대 세 번 복구한다. */
  @EventListener(ApplicationReadyEvent.class)
  @Scheduled(fixedDelayString = "${landit.ai.expression-recovery-interval-ms:30000}")
  public void recoverInterruptedGenerations() {
    for (var candidate :
        repository.findByExpressionGenerationStatus(ExpressionGenerationStatus.PREPARING).stream()
            .filter(
                session ->
                    session.getExpressionGenerationStartedAt() == null
                        || !LocalDateTime.now(clock)
                            .isBefore(
                                session
                                    .getExpressionGenerationStartedAt()
                                    .plus(properties.requestTimeout().multipliedBy(2))
                                    .plusSeconds(60)))
            .limit(10)
            .toList()) {
      Boolean ready =
          transaction.execute(
              status ->
                  repository
                      .findByLearningSessionIdForUpdate(candidate.getLearningSessionId())
                      .map(
                          session -> {
                            if (session.getExpressionGenerationStatus()
                                != ExpressionGenerationStatus.PREPARING) {
                              return false;
                            }
                            LocalDateTime startedAt = session.getExpressionGenerationStartedAt();
                            if (startedAt != null) {
                              var expiry =
                                  startedAt
                                      .plus(properties.requestTimeout().multipliedBy(2))
                                      .plusSeconds(60);
                              if (LocalDateTime.now(clock).isBefore(expiry)) {
                                return false;
                              }
                              session.failExpressionGeneration();
                              if (session.getExpressionGenerationAttempt() >= 3) {
                                return false;
                              }
                              session.retryExpressionGeneration();
                            }
                            return true;
                          })
                      .orElse(false));
      if (Boolean.TRUE.equals(ready)) {
        try {
          executor.execute(() -> generation.generate(candidate.getLearningSessionId()));
        } catch (RuntimeException exception) {
          generation.markFailed(candidate.getLearningSessionId());
        }
      }
    }
  }
}
