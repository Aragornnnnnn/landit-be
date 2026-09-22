// 문의 이미지의 비공개 업로드·조회·실패 보상 삭제 계약을 정의한다.

package com.landit.landitbe.feature.mailbox.feedback.attachment.client;

/** 문의 이미지의 비공개 업로드·조회·실패 보상 삭제 계약이다. */
public interface MailboxAttachmentClient {

  /**
   * 검증된 이미지를 비공개 객체로 저장한다.
   *
   * @param objectKey 서버가 생성한 객체 키
   * @param content 이미지 바이트
   * @param contentType 검증된 MIME 유형
   */
  void upload(String objectKey, byte[] content, String contentType);

  /**
   * 저장 당시의 크기로 읽기를 제한해 객체를 반환한다.
   *
   * @param objectKey 저장된 객체 키
   * @param fileSize 저장된 파일 크기
   * @return 이미지 바이트
   */
  byte[] download(String objectKey, long fileSize);

  /**
   * 실패한 문의 요청에서 생성했을 수 있는 객체를 삭제한다.
   *
   * @param objectKey 해당 요청이 생성한 객체 키
   */
  void delete(String objectKey);
}
