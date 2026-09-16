// 복습의 소유자와 서버가 확정한 시작·완료 시각을 표현한다.

package com.landit.landitbe.feature.learning.review.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 저장한 복습 생명주기다.
 *
 * @param id 복습 ID
 * @param userId 소유 사용자
 * @param scheduledDate 배치 날짜
 * @param createdAt 생성 시각
 * @param availableUntil 시작 기한
 * @param startedAt 최초 시작 시각
 * @param expiresAt 진행 기한
 * @param completedAt 완료 시각
 */
public record ExpressionReview(
    UUID id,
    long userId,
    LocalDate scheduledDate,
    LocalDateTime createdAt,
    LocalDateTime availableUntil,
    LocalDateTime startedAt,
    LocalDateTime expiresAt,
    LocalDateTime completedAt) {

  /**
   * 현재 시각에 따른 사용자 표시 상태를 반환한다.
   *
   * @param now 요청 시각
   * @return READY, IN_PROGRESS, COMPLETED 또는 EXPIRED
   */
  public String status(LocalDateTime now) {
    if (completedAt != null) {
      return "COMPLETED";
    }
    if (!now.isBefore(startedAt == null ? availableUntil : expiresAt)) {
      return "EXPIRED";
    }
    return startedAt == null ? "READY" : "IN_PROGRESS";
  }
}
