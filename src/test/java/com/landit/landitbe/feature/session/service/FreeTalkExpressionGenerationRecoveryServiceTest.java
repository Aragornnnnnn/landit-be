// 재시작으로 중단된 프리톡 표현 생성 작업의 복구를 검증한다.

package com.landit.landitbe.feature.session.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.session.domain.ExpressionGenerationStatus;
import com.landit.landitbe.feature.session.domain.FreeTalkSession;
import com.landit.landitbe.feature.session.domain.FreeTalkStartMode;
import com.landit.landitbe.feature.session.repository.FreeTalkSessionRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** 재시작으로 중단된 프리톡 표현 생성 작업의 복구를 검증한다. */
class FreeTalkExpressionGenerationRecoveryServiceTest {

  /** 실행 전 중단된 준비 상태 세션도 재시도할 수 있도록 실패로 전환한다. */
  @Test
  void marksAllPreparingGenerationsAsFailed() {
    FreeTalkSessionRepository freeTalkSessionRepository =
        Mockito.mock(FreeTalkSessionRepository.class);
    FreeTalkSession interruptedSession =
        FreeTalkSession.start(10L, 20L, FreeTalkStartMode.AI_FIRST);
    interruptedSession.completeByTimeLimit();
    when(freeTalkSessionRepository.findByExpressionGenerationStatus(
            ExpressionGenerationStatus.PREPARING))
        .thenReturn(List.of(interruptedSession));
    FreeTalkExpressionGenerationRecoveryService recoveryService =
        new FreeTalkExpressionGenerationRecoveryService(freeTalkSessionRepository);

    recoveryService.recoverInterruptedGenerations();

    assertThat(interruptedSession.getExpressionGenerationStatus())
        .isEqualTo(ExpressionGenerationStatus.FAILED);
    assertThat(interruptedSession.getExpressionGenerationStartedAt()).isNull();
  }
}
