// 영속 LearningSession의 조회 상태를 분리된 값으로 전달한다.

package com.landit.landitbe.feature.session.dto;

import com.landit.landitbe.feature.session.domain.CompletionReason;
import com.landit.landitbe.feature.session.domain.InputMode;
import com.landit.landitbe.feature.session.domain.LearningSession;
import com.landit.landitbe.feature.session.domain.LearningSessionStatus;
import com.landit.landitbe.feature.session.domain.ProcessingStatus;
import com.landit.landitbe.feature.session.domain.SessionEndActor;
import com.landit.landitbe.feature.session.domain.SessionType;
import com.landit.landitbe.shared.domain.Locale;
import java.time.LocalDateTime;

/**
 * 대화 저장소와 분리된 조회 시점의 상태다.
 *
 * @param id 식별자
 * @param userProfileId 사용자 ID
 * @param sessionType 세션 종류
 * @param aiTutorId 튜터 ID
 * @param targetLocale 학습 언어
 * @param baseLocale 기준 언어
 * @param inputMode 입력 모드
 * @param status 진행 상태
 * @param endedBy 종료 주체
 * @param completionReason 종료 사유
 * @param startedAt 시작 시각
 * @param endedAt 종료 시각
 * @param levelAssessmentProcessingStatus 수준 평가 상태
 * @param levelAssessmentRequestedAt 수준 평가 요청 시각
 * @param createdAt 생성 시각
 * @param updatedAt 수정 시각
 */
public record LearningSessionSnapshot(
    Long id,
    Long userProfileId,
    SessionType sessionType,
    Long aiTutorId,
    Locale targetLocale,
    Locale baseLocale,
    InputMode inputMode,
    LearningSessionStatus status,
    SessionEndActor endedBy,
    CompletionReason completionReason,
    LocalDateTime startedAt,
    LocalDateTime endedAt,
    ProcessingStatus levelAssessmentProcessingStatus,
    LocalDateTime levelAssessmentRequestedAt,
    java.time.LocalDateTime createdAt,
    java.time.LocalDateTime updatedAt) {
  /**
   * 영속 상태를 복사한다.
   *
   * @param entity 원본 상태
   * @return 조회 시점의 값
   */
  public static LearningSessionSnapshot from(LearningSession entity) {
    return new LearningSessionSnapshot(
        entity.getId(),
        entity.getUserProfileId(),
        entity.getSessionType(),
        entity.getAiTutorId(),
        entity.getTargetLocale(),
        entity.getBaseLocale(),
        entity.getInputMode(),
        entity.getStatus(),
        entity.getEndedBy(),
        entity.getCompletionReason(),
        entity.getStartedAt(),
        entity.getEndedAt(),
        entity.getLevelAssessmentProcessingStatus(),
        entity.getLevelAssessmentRequestedAt(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }

  /**
   * 식별자 값을 반환한다.
   *
   * @return 조회 당시 식별자
   */
  public Long getId() {
    return id;
  }

  /**
   * 사용자 ID 값을 반환한다.
   *
   * @return 조회 당시 사용자 ID
   */
  public Long getUserProfileId() {
    return userProfileId;
  }

  /**
   * 세션 종류 값을 반환한다.
   *
   * @return 조회 당시 세션 종류
   */
  public SessionType getSessionType() {
    return sessionType;
  }

  /**
   * 튜터 ID 값을 반환한다.
   *
   * @return 조회 당시 튜터 ID
   */
  public Long getAiTutorId() {
    return aiTutorId;
  }

  /**
   * 학습 언어 값을 반환한다.
   *
   * @return 조회 당시 학습 언어
   */
  public Locale getTargetLocale() {
    return targetLocale;
  }

  /**
   * 기준 언어 값을 반환한다.
   *
   * @return 조회 당시 기준 언어
   */
  public Locale getBaseLocale() {
    return baseLocale;
  }

  /**
   * 입력 모드 값을 반환한다.
   *
   * @return 조회 당시 입력 모드
   */
  public InputMode getInputMode() {
    return inputMode;
  }

  /**
   * 진행 상태 값을 반환한다.
   *
   * @return 조회 당시 진행 상태
   */
  public LearningSessionStatus getStatus() {
    return status;
  }

  /**
   * 종료 주체 값을 반환한다.
   *
   * @return 조회 당시 종료 주체
   */
  public SessionEndActor getEndedBy() {
    return endedBy;
  }

  /**
   * 종료 사유 값을 반환한다.
   *
   * @return 조회 당시 종료 사유
   */
  public CompletionReason getCompletionReason() {
    return completionReason;
  }

  /**
   * 시작 시각 값을 반환한다.
   *
   * @return 조회 당시 시작 시각
   */
  public LocalDateTime getStartedAt() {
    return startedAt;
  }

  /**
   * 종료 시각 값을 반환한다.
   *
   * @return 조회 당시 종료 시각
   */
  public LocalDateTime getEndedAt() {
    return endedAt;
  }

  /**
   * 수준 평가 상태 값을 반환한다.
   *
   * @return 조회 당시 수준 평가 상태
   */
  public ProcessingStatus getLevelAssessmentProcessingStatus() {
    return levelAssessmentProcessingStatus;
  }

  /**
   * 수준 평가 요청 시각 값을 반환한다.
   *
   * @return 조회 당시 수준 평가 요청 시각
   */
  public LocalDateTime getLevelAssessmentRequestedAt() {
    return levelAssessmentRequestedAt;
  }

  /**
   * 생성 시각 값을 반환한다.
   *
   * @return 조회 당시 생성 시각
   */
  public java.time.LocalDateTime getCreatedAt() {
    return createdAt;
  }

  /**
   * 수정 시각 값을 반환한다.
   *
   * @return 조회 당시 수정 시각
   */
  public java.time.LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  /**
   * 조회 시점에 학습이 진행 중인지 확인한다.
   *
   * @return 진행 중이면 true
   */
  public boolean isInProgress() {
    return status == LearningSessionStatus.IN_PROGRESS;
  }
}
