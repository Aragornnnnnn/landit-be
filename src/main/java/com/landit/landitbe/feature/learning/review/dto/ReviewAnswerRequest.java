// 복습 답안의 멱등 키와 선택한 토큰 배열을 검증한다.

package com.landit.landitbe.feature.learning.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * 같은 제출의 네트워크 재시도에는 동일한 submissionId와 내용을 사용한다.
 *
 * @param submissionId 제출 멱등 키
 * @param questionId 답안을 제출할 문제 ID
 * @param words 선택한 토큰. 공백·문장부호를 임의 정규화하지 않는다
 */
public record ReviewAnswerRequest(
    @NotNull UUID submissionId,
    @NotNull UUID questionId,
    @NotNull @Size(min = 1, max = 100) List<@NotBlank @Size(max = 200) String> words) {}
