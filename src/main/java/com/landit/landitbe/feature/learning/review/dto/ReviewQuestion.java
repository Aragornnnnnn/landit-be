// 복습의 고정 문제와 재도전 진행 상태를 전달한다.

package com.landit.landitbe.feature.learning.review.dto;

import com.landit.landitbe.feature.content.expression.practice.dto.WritingSentenceResponse;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 고정된 문제와 진행 상태다.
 *
 * @param questionId 문제 ID
 * @param expressionId 표현 ID
 * @param targetExpressionText 복습 표현
 * @param baseExpressionMeaningText 표현 뜻
 * @param quiz 출제 언어·예문·정답·보기 스냅샷
 * @param displayOrder 최초 출제 순서
 * @param queueOrder 현재 재도전 순서
 * @param wrongCount 오답 횟수
 * @param completedAt 문제 완료 시각
 */
public record ReviewQuestion(
    UUID questionId,
    long expressionId,
    String targetExpressionText,
    String baseExpressionMeaningText,
    WritingSentenceResponse quiz,
    int displayOrder,
    int queueOrder,
    int wrongCount,
    LocalDateTime completedAt) {}
