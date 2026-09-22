// 직접 편지 발송 결과를 정의한다.

package com.landit.landitbe.feature.mailbox.admin.letter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 직접 편지 발송 결과다.
 *
 * @param letterId 생성된 편지 ID
 * @param recipientCount 수신자 수
 * @param sentAt 발송 시각
 */
@Schema(description = "직접 편지 발송 결과")
public record AdminMailboxDirectLetterResponse(
    @Schema(description = "편지 ID") Long letterId,
    @Schema(description = "수신자 수") int recipientCount,
    @Schema(description = "발송 시각") LocalDateTime sentAt) {}
