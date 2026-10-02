// 세션 히스토리 메시지 피드백 엔티티의 조회와 저장을 담당한다.

package com.landit.landitbe.feature.learning.scenario.feedback.repository;

import com.landit.landitbe.feature.learning.scenario.feedback.domain.SessionHistoryMessageFeedback;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 세션 히스토리 메시지 피드백 엔티티의 조회와 저장을 담당한다. */
public interface SessionHistoryMessageFeedbackRepository
    extends JpaRepository<SessionHistoryMessageFeedback, Long> {

  /** 요약 피드백에 속한 메시지 피드백을 메시지 식별자 순으로 조회한다. */
  List<SessionHistoryMessageFeedback>
      findBySessionHistorySummaryFeedbackIdOrderBySessionHistoryMessageIdAsc(Long summaryId);

  /**
   * 여러 요약에 속한 메시지 피드백을 일괄 조회한다.
   *
   * @param summaryIds 요약 피드백 ID 목록
   * @return 저장된 메시지 피드백 목록
   */
  List<SessionHistoryMessageFeedback> findBySessionHistorySummaryFeedbackIdIn(
      List<Long> summaryIds);
}
