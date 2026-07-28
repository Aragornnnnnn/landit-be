// 유실된 프리톡 표현 생성 작업을 다시 제출한다.

package com.landit.landitbe.feature.session.service;

import com.landit.landitbe.feature.session.domain.ExpressionGenerationStatus;
import com.landit.landitbe.feature.session.repository.FreeTalkSessionRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 유실된 프리톡 표현 생성 작업을 다시 제출한다. */
@RequiredArgsConstructor
@Component
public class FreeTalkExpressionRecoveryScheduler {

  private final FreeTalkSessionRepository freeTalkSessionRepository;
  private final FreeTalkExpressionGenerationDispatcher dispatcher;

  /** 5분 이상 실행 중인 작업을 다시 제출한다. */
  @Scheduled(fixedDelay = 60_000)
  public void recoverStaleGenerations() {
    LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
    freeTalkSessionRepository
        .findByExpressionGenerationStatusAndExpressionGenerationStartedAtBefore(
            ExpressionGenerationStatus.PREPARING, threshold)
        .forEach(session -> dispatcher.dispatchRecovery(session.getLearningSessionId()));
  }
}
