// 사용자의 현재 학습 수준 조회 응답을 표현한다.

package com.landit.landitbe.feature.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 사용자의 현재 학습 수준 조회 응답을 표현한다.
 *
 * @param learningLevel 현재 적용 학습 수준. 신규 사용자 기본값은 3이며 평가 확정 여부와 별개다.
 */
public record UserLearningLevelResponse(
    @Schema(
            description = "현재 적용 학습 수준. 신규 사용자 기본값 3. 평가 확정 여부와 별개",
            requiredMode = Schema.RequiredMode.REQUIRED,
            nullable = true,
            types = {"integer", "null"})
        Integer learningLevel) {}
