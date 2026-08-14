// 받은 편지함 목록 응답을 정의한다.

package com.landit.landitbe.feature.mailbox.dto;

import com.landit.landitbe.feature.mailbox.domain.MailboxLetterType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 받은 편지함 목록의 커서 페이지 응답이다.
 *
 * @param items 받은 편지 요약 목록
 * @param nextCursor 다음 페이지 조회용 커서
 * @param hasNext 다음 페이지 존재 여부
 */
@Schema(description = "받은 편지함 목록 응답")
public record MailboxReceivedListResponse(
    @Schema(description = "받은 편지 요약 목록") List<Item> items,
    @Schema(description = "다음 페이지 커서. 없으면 null") String nextCursor,
    @Schema(description = "다음 페이지 존재 여부") boolean hasNext) {

  /**
   * 받은 편지 요약 항목이다.
   *
   * @param letterId 편지 ID
   * @param letterType 편지 유형
   * @param title 편지 제목
   * @param preview 목록 미리보기
   * @param pinned 상단 고정 여부
   * @param sentAt 발송 시각
   * @param unread 읽지 않음 여부
   */
  public record Item(
      @Schema(description = "편지 ID", example = "101") Long letterId,
      @Schema(description = "편지 유형", example = "NOTICE") MailboxLetterType letterType,
      @Schema(description = "편지 제목") String title,
      @Schema(description = "목록 미리보기") String preview,
      @Schema(description = "상단 고정 여부") boolean pinned,
      @Schema(description = "편지 발송 시각") LocalDateTime sentAt,
      @Schema(description = "읽지 않음 여부") boolean unread) {}
}
