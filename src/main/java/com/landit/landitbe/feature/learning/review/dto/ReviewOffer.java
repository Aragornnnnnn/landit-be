// 알림 기능에 복습의 식별자와 문제 수만 전달한다.

package com.landit.landitbe.feature.learning.review.dto;

import java.util.UUID;

/**
 * 생성되거나 재시도에 재사용할 복습 알림 대상이다.
 *
 * @param reviewId 복습 ID
 * @param userId 대상 사용자
 * @param questionCount 실제 문제 수
 */
public record ReviewOffer(UUID reviewId, long userId, int questionCount) {}
