// 세션 텍스트 수준 평가의 비동기 상태와 결과 응답을 정의한다.

package com.landit.landitbe.feature.session.dto;

import com.landit.landitbe.feature.session.domain.LearningSession;
import com.landit.landitbe.feature.session.domain.ProcessingStatus;
import com.landit.landitbe.feature.session.domain.SessionLevelAssessment;

/** 세션 텍스트 수준 평가의 비동기 상태와 결과 응답을 정의한다. */
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

  /** 저장된 세션과 평가 이력을 조회 응답으로 변환한다. */
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
