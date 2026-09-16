// 이메일 발송 제공자와 알림 기능 사이의 계약을 정의한다.

package com.landit.landitbe.feature.notification.client;

/** 이메일 발송 제공자의 접수 결과를 반환한다. */
public interface EmailSender {
  /**
   * 한 수신자에게 발신 전용 이메일을 보낸다.
   *
   * @param recipient 수신 이메일
   * @param subject 제목
   * @param body 일반 텍스트 본문
   * @return 접수 결과이며 실제 메일함 도착을 보장하지 않는다
   */
  EmailSendResult send(String recipient, String subject, String body);
}
