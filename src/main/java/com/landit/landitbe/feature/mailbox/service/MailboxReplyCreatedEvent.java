// 저장된 편지함 답장의 알림 발행 정보를 전달하는 이벤트다.

package com.landit.landitbe.feature.mailbox.service;

import java.util.List;

/**
 * 저장된 편지함 답장의 알림 발행 정보를 전달한다.
 *
 * @param letterId 답장 편지 ID
 * @param userProfileIds 답장 수신 사용자 ID 목록
 * @param replyTitle 답장 제목
 */
public record MailboxReplyCreatedEvent(
    Long letterId, List<Long> userProfileIds, String replyTitle) {}
