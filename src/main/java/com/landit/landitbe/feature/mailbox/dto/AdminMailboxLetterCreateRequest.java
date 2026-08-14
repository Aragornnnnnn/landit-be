// 편지함 어드민 공지·업데이트 생성 요청을 정의한다.

package com.landit.landitbe.feature.mailbox.dto;

import com.landit.landitbe.feature.mailbox.domain.MailboxLetterType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 편지함 어드민 공지·업데이트 생성 요청이다.
 *
 * @param type 편지 유형
 * @param title 편지 제목
 * @param contentBlocks 구조화된 본문 블록
 * @param preview 목록 미리보기
 */
@Schema(description = "편지함 어드민 공지·업데이트 생성 요청")
public record AdminMailboxLetterCreateRequest(
    @NotNull @Schema(description = "편지 유형", example = "NOTICE") MailboxLetterType type,
    @NotBlank @Size(max = 200) @Schema(description = "편지 제목") String title,
    @NotNull @Schema(description = "구조화된 본문 블록") List<Object> contentBlocks,
    @NotBlank @Schema(description = "목록 미리보기") String preview) {}
