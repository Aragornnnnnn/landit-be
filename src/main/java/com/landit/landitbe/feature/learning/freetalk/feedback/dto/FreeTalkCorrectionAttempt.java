// 복구 워커가 선점한 턴 교정의 한 시도를 가리킨다.

package com.landit.landitbe.feature.learning.freetalk.feedback.dto;

/**
 * 복구 워커가 선점한 시도다. 이 값으로만 그 시도의 결과를 반영할 수 있다.
 *
 * @param messageId 교정 대상 사용자 발화 ID. 예: 55020
 * @param attempt 이번 시도의 순번(첫 시도가 1). 예: 2
 * @param token 이번 시도의 선점 식별자. 예: "0b9c…(UUID)"
 */
public record FreeTalkCorrectionAttempt(long messageId, int attempt, String token) {}
