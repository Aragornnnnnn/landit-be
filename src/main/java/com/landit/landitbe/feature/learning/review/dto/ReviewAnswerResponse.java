// 서버 채점 결과와 갱신된 복습 상태를 전달한다.

package com.landit.landitbe.feature.learning.review.dto;

/**
 * 제출 결과와 최신 진행 상태다.
 *
 * @param correct 해당 제출의 정답 여부
 * @param review 최신 복습 상태. 멱등 재전송 시에도 최신 상태를 반환한다
 */
public record ReviewAnswerResponse(boolean correct, ReviewResponse review) {}
