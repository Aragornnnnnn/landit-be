// 관리자 직접 편지 발송 목록의 요약을 정의한다.

package com.landit.landitbe.feature.mailbox.admin.letter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 한 번의 직접 편지 발송 요약이다.
 *
 * @param letterId 편지 ID
 * @param title 제목
 * @param sentAt 발송 시각
 * @param recipientCount 탈퇴 사용자를 포함한 수신자 수
 */
@Schema(description = "직접 편지 발송 요약")
public record AdminMailboxDirectLetterSummary(
    Long letterId, String title, LocalDateTime sentAt, long recipientCount) {}
