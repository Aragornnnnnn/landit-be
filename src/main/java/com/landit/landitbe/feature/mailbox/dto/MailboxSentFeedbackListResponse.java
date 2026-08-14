// 보낸 편지함 피드백 목록 응답을 정의한다.

package com.landit.landitbe.feature.mailbox.dto;

import com.landit.landitbe.feature.mailbox.domain.UserFeedbackStatus;
import com.landit.landitbe.feature.mailbox.domain.UserFeedbackType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 보낸 편지함 피드백 목록의 커서 페이지 응답이다.
 *
 * @param items 피드백 요약 목록
 * @param nextCursor 다음 페이지 조회용 커서
 * @param hasNext 다음 페이지 존재 여부
 */
@Schema(description = "보낸 편지함 피드백 목록 응답")
public record MailboxSentFeedbackListResponse(
    @Schema(description = "피드백 요약 목록") List<Item> items,
    @Schema(description = "다음 페이지 커서. 없으면 null") String nextCursor,
    @Schema(description = "다음 페이지 존재 여부") boolean hasNext) {

  /**
   * 보낸 피드백 요약 항목이다.
   *
   * @param feedbackId 피드백 ID
   * @param type 피드백 유형
   * @param title 피드백 유형 표시 제목
   * @param preview 피드백 미리보기
   * @param status 처리 상태
   * @param createdAt 등록 시각
   */
  public record Item(
      @Schema(description = "피드백 ID", example = "101") Long feedbackId,
      @Schema(description = "피드백 유형", example = "QUESTION") UserFeedbackType type,
      @Schema(description = "피드백 유형 표시 제목", example = "문의") String title,
      @Schema(description = "피드백 미리보기. 전체 문자열을 반환") String preview,
      @Schema(description = "처리 상태", example = "PENDING") UserFeedbackStatus status,
      @Schema(description = "등록 시각") LocalDateTime createdAt) {}
}
