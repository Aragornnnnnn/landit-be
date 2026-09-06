// 세션 텍스트 수준 평가의 비동기 상태와 결과 응답을 정의한다.

package com.landit.landitbe.feature.session.dto;

import com.landit.landitbe.feature.session.domain.LearningSession;
import com.landit.landitbe.feature.session.domain.ProcessingStatus;
import com.landit.landitbe.feature.session.domain.SessionLevelAssessment;

/**
 * 세션 텍스트 수준 평가의 비동기 상태와 결과 응답을 정의한다.
 *
 * @param sessionId 평가 대상 세션 ID
 * @param processingStatus 비동기 평가 처리 상태
 * @param levelAssessment 저장된 평가 결과. 미평가 또는 저장 실패 시 null
 */
public record SessionLevelAssessmentResponse(
    Long sessionId,
    LevelAssessmentProcessingStatus processingStatus,
    SessionLevelAssessment levelAssessment) {

  /** 처리 상태가 기록되지 않은 과거 세션의 조회 상태다. */
  public enum LevelAssessmentProcessingStatus {
    NOT_REQUESTED,
    PREPARING,
    COMPLETED,
    FAILED
  }

  /**
   * 저장된 세션과 평가 이력을 조회 응답으로 변환한다.
   *
   * @param session 소유권이 확인된 세션
   * @param assessment 저장된 평가 결과. 미평가이면 null
   * @return 세션 처리 상태와 선택적 평가 결과
   */
  public static SessionLevelAssessmentResponse from(
      LearningSession session, SessionLevelAssessment assessment) {
    return new SessionLevelAssessmentResponse(
        session.getId(),
        statusOf(session.getLevelAssessmentProcessingStatus(), assessment != null),
        assessment);
  }

  private static LevelAssessmentProcessingStatus statusOf(
      ProcessingStatus status, boolean hasAssessment) {
    if (status == null) {
      return hasAssessment
          ? LevelAssessmentProcessingStatus.COMPLETED
          : LevelAssessmentProcessingStatus.NOT_REQUESTED;
    }
    return LevelAssessmentProcessingStatus.valueOf(status.name());
  }
}
