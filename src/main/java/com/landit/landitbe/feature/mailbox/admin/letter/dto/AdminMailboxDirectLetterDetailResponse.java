// 관리자 직접 편지 본문과 수신자별 읽음 정보를 정의한다.

package com.landit.landitbe.feature.mailbox.admin.letter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 직접 편지의 발송 내용과 수신 이력이다.
 *
 * @param letterId 편지 ID
 * @param title 제목
 * @param bodyText 일반 텍스트 본문
 * @param sentAt 발송 시각
 * @param recipientCount 탈퇴 사용자를 포함한 수신자 수
 * @param recipients 사용자 ID 오름차순 수신자 목록
 */
@Schema(description = "관리자 직접 편지 발송 상세")
public record AdminMailboxDirectLetterDetailResponse(
    Long letterId,
    String title,
    String bodyText,
    LocalDateTime sentAt,
    int recipientCount,
    List<Recipient> recipients) {

  /**
   * 수신자의 최초 읽음 정보다. 관리자 조회로는 읽음 처리하지 않는다.
   *
   * @param userProfileId 수신 사용자 ID. 발송 후 탈퇴한 사용자도 포함한다.
   * @param readAt 최초 읽음 시각. 읽지 않았다면 {@code null}이다.
   */
  @Schema(name = "AdminMailboxDirectLetterRecipient", description = "직접 편지 수신자")
  public record Recipient(
      Long userProfileId,
      @Schema(
              description = "최초 읽음 시각. 미열람이면 null",
              nullable = true,
              requiredMode = Schema.RequiredMode.REQUIRED)
          LocalDateTime readAt) {}
}
