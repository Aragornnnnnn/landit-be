// 영속 SessionHistory의 조회 상태를 분리된 값으로 전달한다.

package com.landit.landitbe.feature.learning.conversation.dto;

import com.landit.landitbe.feature.learning.conversation.domain.SessionType;
import com.landit.landitbe.feature.learning.conversation.history.domain.SessionHistory;
import com.landit.landitbe.shared.domain.Locale;
import java.time.LocalDateTime;

/**
 * 대화 저장소와 분리된 조회 시점의 상태다.
 *
 * @param id 식별자
 * @param learningSessionId 학습 세션 ID
 * @param userProfileId 사용자 ID
 * @param sessionType 세션 종류
 * @param targetLocale 학습 언어
 * @param baseLocale 기준 언어
 * @param startedAt 시작 시각
 * @param endedAt 종료 시각
 * @param durationSeconds 학습 시간 초
 * @param userMessageCount 사용자 발화 수
 * @param xpReward 경험치
 * @param createdAt 생성 시각
 */
public record SessionHistorySnapshot(
    Long id,
    Long learningSessionId,
    Long userProfileId,
    SessionType sessionType,
    Locale targetLocale,
    Locale baseLocale,
    LocalDateTime startedAt,
    LocalDateTime endedAt,
    int durationSeconds,
    int userMessageCount,
    Integer xpReward,
    java.time.LocalDateTime createdAt) {
  /**
   * 영속 상태를 복사한다.
   *
   * @param entity 원본 상태
   * @return 조회 시점의 값
   */
  public static SessionHistorySnapshot from(SessionHistory entity) {
    return new SessionHistorySnapshot(
        entity.getId(),
        entity.getLearningSessionId(),
        entity.getUserProfileId(),
        entity.getSessionType(),
        entity.getTargetLocale(),
        entity.getBaseLocale(),
        entity.getStartedAt(),
        entity.getEndedAt(),
        entity.getDurationSeconds(),
        entity.getUserMessageCount(),
        entity.getXpReward(),
        entity.getCreatedAt());
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
   * 학습 세션 ID 값을 반환한다.
   *
   * @return 조회 당시 학습 세션 ID
   */
  public Long getLearningSessionId() {
    return learningSessionId;
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
   * 학습 시간 초 값을 반환한다.
   *
   * @return 조회 당시 학습 시간 초
   */
  public int getDurationSeconds() {
    return durationSeconds;
  }

  /**
   * 사용자 발화 수 값을 반환한다.
   *
   * @return 조회 당시 사용자 발화 수
   */
  public int getUserMessageCount() {
    return userMessageCount;
  }

  /**
   * 경험치 값을 반환한다.
   *
   * @return 조회 당시 경험치
   */
  public Integer getXpReward() {
    return xpReward;
  }

  /**
   * 생성 시각 값을 반환한다.
   *
   * @return 조회 당시 생성 시각
   */
  public java.time.LocalDateTime getCreatedAt() {
    return createdAt;
  }
}
