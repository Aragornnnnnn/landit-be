// 문의 상세에서 반환할 첨부 정보와 인증 조회 경로를 정의한다.

package com.landit.landitbe.feature.mailbox.feedback.attachment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 문의 상세에서 반환할 첨부 정보다. S3 객체 키와 공개 URL은 노출하지 않는다.
 *
 * @param attachmentId 첨부 ID
 * @param contentType 이미지 MIME 유형
 * @param fileSize 파일 크기(바이트)
 * @param downloadUrl 작성자 또는 관리자 인증이 필요한 API 상대 경로
 */
public record MailboxFeedbackAttachmentResponse(
    Long attachmentId,
    String contentType,
    long fileSize,
    @Schema(description = "Bearer 인증을 붙여 이미지 바이트를 조회할 API 상대 경로") String downloadUrl) {}
