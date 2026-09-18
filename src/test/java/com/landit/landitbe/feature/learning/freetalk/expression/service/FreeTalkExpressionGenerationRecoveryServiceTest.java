// 새 인스턴스가 다른 인스턴스의 작업을 실패시키지 않고 만료된 작업만 복구하는지 검증한다.

package com.landit.landitbe.feature.learning.freetalk.expression.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.landit.landitbe.config.ai.AiClientProperties;
import com.landit.landitbe.feature.learning.freetalk.domain.FreeTalkSession;
import com.landit.landitbe.feature.learning.freetalk.domain.FreeTalkStartMode;
import com.landit.landitbe.feature.learning.freetalk.expression.domain.ExpressionGenerationStatus;
import com.landit.landitbe.feature.learning.freetalk.repository.FreeTalkSessionRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

class FreeTalkExpressionGenerationRecoveryServiceTest {
  private final Clock clock = Clock.systemDefaultZone();
  private final FreeTalkSessionRepository repository = mock(FreeTalkSessionRepository.class);
  private final FreeTalkExpressionGenerationService generator =
      mock(FreeTalkExpressionGenerationService.class);

  @DisplayName("다른 인스턴스가 진행 중인 표현 생성 작업을 침범하지 않는다.")
  @Test
  void preservesAnotherInstancesLiveGeneration() {
    var session = session();
    session.startExpressionGeneration();
    recovery(session).recoverInterruptedGenerations();
    assertThat(session.getExpressionGenerationStatus())
        .isEqualTo(ExpressionGenerationStatus.PREPARING);
    assertThat(session.getExpressionGenerationStartedAt()).isNotNull();
    verify(generator, never()).generate(10L);
  }

  @DisplayName("대기 및 만료된 표현 생성 작업을 재개하되 3회 시도 후에는 중단한다.")
  @Test
  void resumesQueuedAndExpiredWorkButStopsAfterThreeAttempts() {
    var session = session();
    var recovery = recovery(session);
    recovery.recoverInterruptedGenerations();
    verify(generator).generate(10L);
    session.startExpressionGeneration();
    ReflectionTestUtils.setField(
        session, "expressionGenerationStartedAt", LocalDateTime.now(clock).minusMinutes(5));
    recovery.recoverInterruptedGenerations();
    assertThat(session.getExpressionGenerationStartedAt()).isNull();
    assertThat(session.getExpressionGenerationStatus())
        .isEqualTo(ExpressionGenerationStatus.PREPARING);
    session.startExpressionGeneration();
    ReflectionTestUtils.setField(
        session, "expressionGenerationStartedAt", LocalDateTime.now(clock).minusMinutes(5));
    ReflectionTestUtils.setField(session, "expressionGenerationAttempt", 3);
    recovery.recoverInterruptedGenerations();
    assertThat(session.getExpressionGenerationStatus())
        .isEqualTo(ExpressionGenerationStatus.FAILED);
  }

  private FreeTalkSession session() {
    var session = FreeTalkSession.start(10L, 20L, FreeTalkStartMode.AI_FIRST);
    session.completeByTimeLimit();
    return session;
  }

  private FreeTalkExpressionGenerationRecoveryService recovery(FreeTalkSession session) {
    when(repository.findByExpressionGenerationStatus(ExpressionGenerationStatus.PREPARING))
        .thenReturn(List.of(session));
    when(repository.findByLearningSessionIdForUpdate(10L)).thenReturn(Optional.of(session));
    var manager = mock(PlatformTransactionManager.class);
    when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    var properties =
        new AiClientProperties(
            "",
            "local",
            "",
            Duration.ofSeconds(5),
            Duration.ofSeconds(60),
            Duration.ofSeconds(120),
            Duration.ofSeconds(20));
    return new FreeTalkExpressionGenerationRecoveryService(
        repository, generator, properties, clock, manager, Runnable::run);
  }
}
