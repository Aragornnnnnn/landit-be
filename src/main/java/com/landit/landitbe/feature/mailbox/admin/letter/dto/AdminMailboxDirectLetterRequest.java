// 특정 사용자에게 직접 발송할 편지 요청을 정의한다.

package com.landit.landitbe.feature.mailbox.admin.letter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 특정 사용자에게 직접 발송할 편지 요청이다.
 *
 * @param userProfileIds 중복 없는 활성 수신자 ID 목록
 * @param title 편지 제목
 * @param bodyText 일반 텍스트 본문
 */
@Schema(description = "특정 사용자에게 직접 발송할 편지 요청")
public record AdminMailboxDirectLetterRequest(
    @NotEmpty @Size(max = 100) @Schema(description = "활성 수신자 ID 1~100개. 중복 불가")
        List<@NotNull @Positive Long> userProfileIds,
    @NotBlank @Size(max = 200) @Schema(description = "편지 제목") String title,
    @NotBlank @Schema(description = "일반 텍스트 본문") String bodyText) {}
