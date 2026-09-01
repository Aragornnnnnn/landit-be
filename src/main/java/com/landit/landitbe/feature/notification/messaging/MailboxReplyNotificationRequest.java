// Push Queue에 발행할 편지함 답장 알림 정보를 정의한다.

package com.landit.landitbe.feature.notification.messaging;

import java.time.Instant;
import java.util.List;

/**
 * Push Queue에 발행할 편지함 답장 알림 정보다.
 *
 * @param letterId 답장 편지 ID
 * @param userProfileIds 답장 수신 사용자 ID 목록
 * @param replyTitle 답장 제목
 * @param occurredAt 답장 알림 발생 시각
 */
public record MailboxReplyNotificationRequest(
    Long letterId, List<Long> userProfileIds, String replyTitle, Instant occurredAt) {}
