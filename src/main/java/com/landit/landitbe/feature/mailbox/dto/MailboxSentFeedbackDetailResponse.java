// 보낸 편지함 피드백 상세 응답을 정의한다.

package com.landit.landitbe.feature.mailbox.dto;

import com.landit.landitbe.feature.mailbox.domain.UserFeedbackStatus;
import com.landit.landitbe.feature.mailbox.domain.UserFeedbackType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 보낸 편지함 피드백 상세 응답이다.
 *
 * @param feedbackId 피드백 ID
 * @param type 피드백 유형
 * @param title 피드백 유형 표시 제목
 * @param content 피드백 본문
 * @param status 처리 상태
 * @param resolvedByFeedbackId 함께 처리된 대표 피드백 ID
 * @param createdAt 등록 시각
 * @param updatedAt 수정 시각
 * @param replies 연결된 답장 목록
 */
@Schema(description = "보낸 편지함 피드백 상세 응답")
public record MailboxSentFeedbackDetailResponse(
    @Schema(description = "피드백 ID", example = "101") Long feedbackId,
    @Schema(description = "피드백 유형", example = "QUESTION") UserFeedbackType type,
    @Schema(description = "피드백 유형 표시 제목", example = "문의") String title,
    @Schema(description = "피드백 내용") String content,
    @Schema(description = "피드백 처리 상태", example = "PENDING") UserFeedbackStatus status,
    @Schema(description = "대표 피드백 ID. 없으면 null") Long resolvedByFeedbackId,
    @Schema(description = "등록 시각") LocalDateTime createdAt,
    @Schema(description = "수정 시각") LocalDateTime updatedAt,
    @Schema(description = "연결된 답장 목록") List<Reply> replies) {

  /**
   * 피드백에 연결된 답장 요약이다.
   *
   * @param letterId 답장 편지 ID
   * @param title 답장 제목
   * @param bodyText 답장 본문
   * @param sentAt 답장 발송 시각
   */
  public record Reply(
      @Schema(description = "답장 편지 ID", example = "201") Long letterId,
      @Schema(description = "답장 제목") String title,
      @Schema(description = "답장 본문") String bodyText,
      @Schema(description = "답장 발송 시각") LocalDateTime sentAt) {}
}
