// 기존 최종 피드백의 식별자를 보관한다.

package com.landit.landitbe.feature.session.application;

import com.landit.landitbe.feature.session.domain.ProcessingStatus;
import com.landit.landitbe.feature.session.domain.SessionHistorySummaryFeedback;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;

/** 기존 최종 피드백의 식별자를 보관한다. */
record ExistingSummaryFeedbackContext(Long summaryFeedbackId) {

  /** 완료 상태의 저장된 summary만 기존 최종 피드백 결과로 허용한다. */
  static ExistingSummaryFeedbackContext from(SessionHistorySummaryFeedback summaryFeedback) {
    if (summaryFeedback.getProcessingStatus() != ProcessingStatus.COMPLETED) {
      throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
    }
    return new ExistingSummaryFeedbackContext(summaryFeedback.getId());
  }
}
