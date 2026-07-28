// 재시작으로 중단된 프리톡 표현 생성 작업을 재시도 가능 상태로 복구한다.

package com.landit.landitbe.feature.session.service;

import com.landit.landitbe.feature.session.domain.ExpressionGenerationStatus;
import com.landit.landitbe.feature.session.repository.FreeTalkSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 재시작으로 중단된 프리톡 표현 생성 작업을 재시도 가능 상태로 복구한다. */
@RequiredArgsConstructor
@Service
public class FreeTalkExpressionGenerationRecoveryService {

  private final FreeTalkSessionRepository freeTalkSessionRepository;

  /** 애플리케이션 시작 시 중단된 표현 생성 작업을 실패 상태로 전환한다. */
  @EventListener(ApplicationReadyEvent.class)
  @Transactional
  public void recoverInterruptedGenerations() {
    freeTalkSessionRepository
        .findByExpressionGenerationStatusAndExpressionGenerationStartedAtIsNotNull(
            ExpressionGenerationStatus.PREPARING)
        .forEach(session -> session.failExpressionGeneration());
  }
}
