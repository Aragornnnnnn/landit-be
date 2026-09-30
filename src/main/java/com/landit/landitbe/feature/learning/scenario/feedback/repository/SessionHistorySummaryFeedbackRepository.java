// 세션 히스토리 요약 피드백 엔티티의 조회와 저장을 담당한다.

package com.landit.landitbe.feature.learning.scenario.feedback.repository;

import com.landit.landitbe.feature.learning.scenario.feedback.domain.SessionHistorySummaryFeedback;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 세션 히스토리 요약 피드백 엔티티의 조회와 저장을 담당한다. */
public interface SessionHistorySummaryFeedbackRepository
    extends JpaRepository<SessionHistorySummaryFeedback, Long> {

  /** 세션 히스토리 식별자로 최종 피드백 요약을 조회한다. */
  Optional<SessionHistorySummaryFeedback> findBySessionHistoryId(Long sessionHistoryId);

  /**
   * 여러 회차의 저장된 요약 피드백을 일괄 조회한다.
   *
   * @param historyIds 이력 ID 목록
   * @return 저장된 요약 피드백 목록
   */
  List<SessionHistorySummaryFeedback> findBySessionHistoryIdIn(List<Long> historyIds);
}
