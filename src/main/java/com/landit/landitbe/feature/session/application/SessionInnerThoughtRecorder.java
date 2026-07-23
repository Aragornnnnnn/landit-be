// 비동기 AI 속마음 생성 결과를 세션 메시지에 반영한다.

package com.landit.landitbe.feature.session.application;

import com.landit.landitbe.feature.session.application.port.AiInnerThoughtResult;
import com.landit.landitbe.feature.session.domain.ProcessingStatus;
import com.landit.landitbe.feature.session.infrastructure.SessionHistoryMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 비동기 AI 속마음 생성 결과를 세션 메시지에 반영한다. */
@RequiredArgsConstructor
@Component
class SessionInnerThoughtRecorder {

  private final SessionHistoryMessageRepository sessionHistoryMessageRepository;

  /** 준비 상태인 사용자 메시지에만 속마음 생성 결과를 기록한다. */
  @Transactional
  void complete(AiInnerThoughtResult result) {
    sessionHistoryMessageRepository.completeInnerThoughtIfPreparing(
        result.messageId(),
        result.innerThought(),
        result.innerThoughtType(),
        ProcessingStatus.COMPLETED,
        ProcessingStatus.PREPARING);
  }

  /** 준비 상태인 사용자 메시지의 속마음 생성을 실패로 확정한다. */
  @Transactional
  void fail(long submittedMessageId) {
    sessionHistoryMessageRepository.markInnerThoughtFailedIfPreparing(
        submittedMessageId, ProcessingStatus.FAILED, ProcessingStatus.PREPARING);
  }
}
