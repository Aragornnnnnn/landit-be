// 세션 히스토리 요약 피드백 엔티티의 조회와 저장을 담당한다.

package com.landit.landitbe.session.infrastructure;

import com.landit.landitbe.session.domain.SessionHistorySummaryFeedback;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 세션 히스토리 요약 피드백 엔티티의 조회와 저장을 담당한다. */
public interface SessionHistorySummaryFeedbackRepository
    extends JpaRepository<SessionHistorySummaryFeedback, Long> {

  /** 세션 히스토리 식별자로 최종 피드백 요약을 조회한다. */
  Optional<SessionHistorySummaryFeedback> findBySessionHistoryId(Long sessionHistoryId);
}
