// 복습 시작·조회·완료 화면에서 사용하는 응답을 정의한다.

package com.landit.landitbe.feature.learning.review.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 복습 화면 상태다. 시작 전과 만료 상태에서는 문제를 제공하지 않는다.
 *
 * @param reviewId 복습 ID
 * @param status READY, IN_PROGRESS, COMPLETED 또는 EXPIRED
 * @param availableUntil 시작 기한
 * @param expiresAt 시작 후 진행 기한
 * @param completedAt 전체 완료 시각
 * @param currentQuestionId 다음에 풀 문제. 시작 전·완료·만료이면 null
 * @param questions 최초 출제 순서의 문제와 진행 상태
 */
public record ReviewResponse(
    UUID reviewId,
    String status,
    LocalDateTime availableUntil,
    LocalDateTime expiresAt,
    LocalDateTime completedAt,
    UUID currentQuestionId,
    List<ReviewQuestion> questions) {}
