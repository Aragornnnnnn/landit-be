// 받은 편지함 상세 응답을 정의한다.

package com.landit.landitbe.feature.mailbox.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.landit.landitbe.feature.mailbox.domain.MailboxLetterType;
import com.landit.landitbe.feature.mailbox.domain.UserFeedbackType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 받은 편지함 상세 응답이다.
 *
 * @param letterId 편지 ID
 * @param letterType 편지 유형
 * @param title 편지 제목
 * @param contentBlocks 구조화된 공지·업데이트 본문. 답장이면 {@code null}
 * @param bodyText 답장 본문. 공지·업데이트면 {@code null}
 * @param feedbackType 답장과 연결된 원본 피드백 유형. 공지·업데이트면 {@code null}
 * @param quotedFeedbackContent 답장과 연결된 원본 피드백 내용. 공지·업데이트면 {@code null}
 * @param pinned 상단 고정 여부
 * @param sentAt 발송 시각
 * @param readAt 읽은 시각
 */
@Schema(description = "받은 편지함 상세 응답")
public record MailboxReceivedDetailResponse(
    @Schema(description = "편지 ID", example = "101") Long letterId,
    @Schema(description = "편지 유형", example = "NOTICE") MailboxLetterType letterType,
    @Schema(description = "편지 제목") String title,
    @Schema(description = "구조화된 공지·업데이트 본문. 답장은 null") JsonNode contentBlocks,
    @Schema(description = "답장 본문. 공지·업데이트는 null") String bodyText,
    @Schema(description = "답장과 연결된 원본 피드백 유형. 공지·업데이트는 null") UserFeedbackType feedbackType,
    @Schema(description = "답장과 연결된 원본 피드백 내용. 공지·업데이트는 null") String quotedFeedbackContent,
    @Schema(description = "상단 고정 여부") boolean pinned,
    @Schema(description = "편지 발송 시각") LocalDateTime sentAt,
    @Schema(description = "읽은 시각") LocalDateTime readAt) {}
