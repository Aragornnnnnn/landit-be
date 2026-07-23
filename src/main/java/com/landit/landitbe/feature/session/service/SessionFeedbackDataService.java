// 세션 요약·메시지 피드백 Repository를 소유한다.

package com.landit.landitbe.feature.session.service;

import com.landit.landitbe.feature.session.domain.SessionHistoryMessageFeedback;
import com.landit.landitbe.feature.session.domain.SessionHistorySummaryFeedback;
import com.landit.landitbe.feature.session.repository.SessionHistoryMessageFeedbackRepository;
import com.landit.landitbe.feature.session.repository.SessionHistorySummaryFeedbackRepository;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 세션 요약·메시지 피드백 Repository를 소유한다. */
@Service
@RequiredArgsConstructor
public class SessionFeedbackDataService {

  private final SessionHistoryMessageFeedbackRepository messageFeedbackRepository;
  private final SessionHistorySummaryFeedbackRepository summaryFeedbackRepository;

  /** 세션 히스토리에 저장된 요약 피드백을 조회한다. */
  public Optional<SessionHistorySummaryFeedback> findSummaryByHistoryId(long sessionHistoryId) {
    return summaryFeedbackRepository.findBySessionHistoryId(sessionHistoryId);
  }

  /** 요약 피드백을 ID로 조회한다. */
  public SessionHistorySummaryFeedback requireSummary(long summaryFeedbackId) {
    return summaryFeedbackRepository
        .findById(summaryFeedbackId)
        .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_SERVER_ERROR));
  }

  /** 요약 피드백에 속한 메시지 피드백을 순서대로 조회한다. */
  public List<SessionHistoryMessageFeedback> findMessageFeedbacks(long summaryFeedbackId) {
    return messageFeedbackRepository
        .findBySessionHistorySummaryFeedbackIdOrderBySessionHistoryMessageIdAsc(summaryFeedbackId);
  }

  /** 요약 피드백을 저장한다. */
  public SessionHistorySummaryFeedback saveSummary(SessionHistorySummaryFeedback summary) {
    return summaryFeedbackRepository.save(summary);
  }

  /** 메시지별 피드백 목록을 저장한다. */
  public List<SessionHistoryMessageFeedback> saveMessageFeedbacks(
      List<SessionHistoryMessageFeedback> feedbacks) {
    return messageFeedbackRepository.saveAll(feedbacks);
  }
}
