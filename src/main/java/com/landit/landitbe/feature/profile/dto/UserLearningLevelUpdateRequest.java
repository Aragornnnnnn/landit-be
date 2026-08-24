// 사용자가 온보딩에서 선택한 학습 수준 변경 요청을 전달한다.

package com.landit.landitbe.feature.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 사용자가 온보딩에서 선택한 학습 수준 변경 요청을 전달한다.
 *
 * @param learningLevel 사용자가 선택한 1부터 5까지의 학습 수준
 */
@Schema(description = "사용자 학습 수준 변경 요청")
public record UserLearningLevelUpdateRequest(
    @NotNull @Min(1) @Max(5) @Schema(description = "1부터 5까지의 학습 수준", example = "3")
        Integer learningLevel) {}
