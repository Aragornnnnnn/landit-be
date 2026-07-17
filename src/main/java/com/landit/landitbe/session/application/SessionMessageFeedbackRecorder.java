// 비동기 메시지별 피드백 실패 상태를 저장한다.

package com.landit.landitbe.session.application;

import com.landit.landitbe.session.domain.ProcessingStatus;
import com.landit.landitbe.session.infrastructure.SessionHistoryMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
class SessionMessageFeedbackRecorder {

  private final SessionHistoryMessageRepository sessionHistoryMessageRepository;

  /** 준비 상태인 사용자 메시지의 피드백 처리를 실패로 확정한다. */
  @Transactional
  void fail(long submittedMessageId) {
    sessionHistoryMessageRepository.markFeedbackFailedIfPreparing(
        submittedMessageId, ProcessingStatus.FAILED, ProcessingStatus.PREPARING);
  }
}
