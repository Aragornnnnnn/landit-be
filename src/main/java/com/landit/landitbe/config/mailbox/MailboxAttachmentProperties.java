// 사용자 문의 첨부를 저장할 비공개 S3 버킷 설정을 정의한다.

package com.landit.landitbe.config.mailbox;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 사용자 문의 첨부용 비공개 저장소 설정이다.
 *
 * @param bucketName 비공개 애플리케이션 버킷 이름. 비어 있으면 첨부 업로드·조회를 거부한다
 * @param region AWS 리전
 */
@ConfigurationProperties(prefix = "landit.mailbox-attachment")
public record MailboxAttachmentProperties(String bucketName, String region) {}
