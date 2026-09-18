// 저장한 복습 제출의 원문과 판정을 재전송 검증에 사용한다.

package com.landit.landitbe.feature.learning.review.domain;

import java.util.List;
import java.util.UUID;

/**
 * 이미 처리한 제출이다.
 *
 * @param questionId 제출 대상 문제
 * @param words 제출한 토큰 순서
 * @param correct 정답 여부
 */
public record ReviewSubmission(UUID questionId, List<String> words, boolean correct) {}
