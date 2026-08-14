// 편지함 어드민 일괄 답장 요청을 정의한다.

package com.landit.landitbe.feature.mailbox.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 편지함 어드민 일괄 답장 요청이다.
 *
 * @param feedbackIds 답장을 연결할 피드백 ID 목록
 * @param title 답장 제목
 * @param bodyText 답장 본문
 */
@Schema(description = "편지함 어드민 일괄 답장 요청")
public record AdminMailboxReplyRequest(
    @NotEmpty @Size(max = 100) @Schema(description = "피드백 ID 목록") List<Long> feedbackIds,
    @NotBlank @Size(max = 200) @Schema(description = "답장 제목") String title,
    @NotBlank @Schema(description = "답장 본문") String bodyText) {}
