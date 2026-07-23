// 세션 히스토리 메시지 피드백 엔티티의 조회와 저장을 담당한다.

package com.landit.landitbe.feature.session.repository;

import com.landit.landitbe.feature.session.domain.SessionHistoryMessageFeedback;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 세션 히스토리 메시지 피드백 엔티티의 조회와 저장을 담당한다. */
public interface SessionHistoryMessageFeedbackRepository
    extends JpaRepository<SessionHistoryMessageFeedback, Long> {

  /** 요약 피드백에 속한 메시지 피드백을 메시지 식별자 순으로 조회한다. */
  List<SessionHistoryMessageFeedback>
      findBySessionHistorySummaryFeedbackIdOrderBySessionHistoryMessageIdAsc(Long summaryId);
}
