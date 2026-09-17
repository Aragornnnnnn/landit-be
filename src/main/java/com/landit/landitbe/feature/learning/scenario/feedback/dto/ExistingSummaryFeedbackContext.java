// 기존 최종 피드백의 식별자를 보관한다.

package com.landit.landitbe.feature.learning.scenario.feedback.dto;

import com.landit.landitbe.feature.learning.conversation.domain.ProcessingStatus;
import com.landit.landitbe.feature.learning.scenario.feedback.domain.SessionHistorySummaryFeedback;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;

/**
 * 기존 최종 피드백의 식별자를 보관한다.
 *
 * @param summaryFeedbackId 기존 최종 피드백 ID
 */
public record ExistingSummaryFeedbackContext(Long summaryFeedbackId) {

  /**
   * 완료 상태의 저장된 summary만 기존 최종 피드백 결과로 허용한다.
   *
   * @param summaryFeedback 저장된 요약 피드백
   * @return 완료된 요약 피드백의 식별자
   * @throws ApiException 저장된 요약 피드백이 완료 상태가 아닐 때
   */
  public static ExistingSummaryFeedbackContext from(SessionHistorySummaryFeedback summaryFeedback) {
    if (summaryFeedback.getProcessingStatus() != ProcessingStatus.COMPLETED) {
      throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
    }
    return new ExistingSummaryFeedbackContext(summaryFeedback.getId());
  }
}
