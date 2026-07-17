// NPS 제출 API의 요청 본문을 정의한다.

package com.landit.landitbe.nps.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** NPS 제출 API의 요청 본문을 정의한다. */
@Schema(description = "NPS 제출 요청")
public record NpsSubmitRequest(
    @NotNull @Min(1) @Max(5) @Schema(description = "1부터 5까지의 만족도 점수", example = "3") Integer score,
    @Schema(description = "선택 사용자 의견", example = "피드백은 좋았지만 기다리는 시간이 길었어요.") String opinionText) {}
