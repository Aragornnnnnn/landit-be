// 사용자의 현재 학습 수준 조회 응답을 표현한다.

package com.landit.landitbe.feature.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 사용자의 현재 학습 수준 조회 응답을 표현한다.
 *
 * @param learningLevel 사용자가 선택한 학습 수준. 미설정이면 {@code null}
 */
public record UserLearningLevelResponse(
    @Schema(
            description = "사용자가 선택한 학습 수준",
            requiredMode = Schema.RequiredMode.REQUIRED,
            nullable = true,
            types = {"integer", "null"})
        Integer learningLevel) {}
