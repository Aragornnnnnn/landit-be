// 편지함 안 읽은 편지 개수 응답을 정의한다.

package com.landit.landitbe.feature.mailbox.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 편지함 안 읽은 편지 개수 응답이다.
 *
 * @param unreadCount 안 읽은 편지 개수
 */
@Schema(description = "편지함 안 읽은 편지 개수 응답")
public record MailboxUnreadCountResponse(
    @Schema(description = "안 읽은 편지 개수", example = "3") long unreadCount) {}
