// 검증된 이미지 바이트와 MIME 유형을 전달한다.

package com.landit.landitbe.feature.mailbox.feedback.attachment.dto;

/**
 * 검증된 이미지 바이트와 MIME 유형이다.
 *
 * @param content 이미지 바이트
 * @param contentType 검증된 이미지 MIME 유형
 */
public record MailboxAttachmentImage(byte[] content, String contentType) {}
