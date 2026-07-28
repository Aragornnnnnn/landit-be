// 프리톡 사용자 발화 제출 요청을 검증한다.

package com.landit.landitbe.feature.session.dto;

import com.landit.landitbe.feature.session.domain.SessionMessageInputType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** 프리톡 사용자 발화 제출 요청을 검증한다. */
public record FreeTalkMessageSubmitRequest(
    @NotBlank
        @Pattern(
            regexp =
                "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-"
                    + "[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$")
        String clientMessageId,
    @NotBlank String content,
    @NotNull SessionMessageInputType inputType,
    @NotNull @Min(0) Long utteranceDurationMs,
    @NotNull Boolean timeLimitReached) {}
