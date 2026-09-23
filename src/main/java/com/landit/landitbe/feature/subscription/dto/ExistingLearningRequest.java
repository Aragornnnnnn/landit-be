// 기존 학습 권한과 도입 전 시작 이력의 평가 입력을 전달한다.

package com.landit.landitbe.feature.subscription.dto;

import java.time.LocalDateTime;

/**
 * 소유 업무가 확인한 학습 요청이다. 구독은 세션을 다시 조회하지 않는다.
 *
 * @param kind 학습 종류
 * @param targetId 대상 식별자
 * @param attemptId 요청한 표현 학습 시도 또는 null
 * @param completion 완료 저장의 재요청 여부
 * @param startedAt 소유자와 종류를 확인한 기존 세션의 시작 시각. 표현 또는 미확인 세션이면 null
 */
public record ExistingLearningRequest(
    String kind, long targetId, String attemptId, boolean completion, LocalDateTime startedAt) {}
