// 비공개 애플리케이션 버킷에 문의 이미지를 저장하고 조회·삭제한다.

package com.landit.landitbe.feature.mailbox.feedback.attachment.client;

import com.landit.landitbe.config.mailbox.MailboxAttachmentProperties;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/** 비공개 애플리케이션 버킷에 문의 이미지를 저장하고 조회·삭제한다. */
@Component
public class S3MailboxAttachmentClient implements MailboxAttachmentClient {

  private final S3Client client;
  private final MailboxAttachmentProperties properties;

  /**
   * 문의 첨부 전용 S3 클라이언트와 비공개 버킷 설정을 주입받는다.
   *
   * @param client 문의 첨부 S3 클라이언트
   * @param properties 비공개 버킷 설정
   */
  public S3MailboxAttachmentClient(
      @Qualifier("mailboxAttachmentS3Client") S3Client client,
      MailboxAttachmentProperties properties) {
    this.client = client;
    this.properties = properties;
  }

  /** {@inheritDoc} */
  @Override
  public void upload(String objectKey, byte[] content, String contentType) {
    String bucket = requireBucket();
    try {
      client.putObject(
          PutObjectRequest.builder()
              .bucket(bucket)
              .key(objectKey)
              .contentType(contentType)
              .contentLength((long) content.length)
              .cacheControl("private, no-store")
              .build(),
          RequestBody.fromBytes(content));
    } catch (SdkException exception) {
      throw new ApiException(ErrorCode.SERVICE_UNAVAILABLE, "문의 이미지 저장에 실패했습니다.");
    }
  }

  /** {@inheritDoc} */
  @Override
  public byte[] download(String objectKey, long fileSize) {
    String bucket = requireBucket();
    try {
      byte[] content =
          client
              .getObjectAsBytes(
                  GetObjectRequest.builder()
                      .bucket(bucket)
                      .key(objectKey)
                      .range("bytes=0-" + (fileSize - 1))
                      .build())
              .asByteArray();
      if (content.length != fileSize) {
        throw new ApiException(ErrorCode.SERVICE_UNAVAILABLE, "문의 이미지 크기가 일치하지 않습니다.");
      }
      return content;
    } catch (SdkException exception) {
      throw new ApiException(ErrorCode.SERVICE_UNAVAILABLE, "문의 이미지를 불러오지 못했습니다.");
    }
  }

  /** {@inheritDoc} */
  @Override
  public void delete(String objectKey) {
    String bucket = requireBucket();
    try {
      client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(objectKey).build());
    } catch (SdkException exception) {
      throw new ApiException(ErrorCode.SERVICE_UNAVAILABLE, "미완료 문의 이미지 정리에 실패했습니다.");
    }
  }

  private String requireBucket() {
    if (properties.bucketName() == null || properties.bucketName().isBlank()) {
      throw new ApiException(ErrorCode.SERVICE_UNAVAILABLE);
    }
    return properties.bucketName();
  }
}
