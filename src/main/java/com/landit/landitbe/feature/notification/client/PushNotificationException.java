// 일시적인 외부 Push 제공자 오류를 SQS 재시도로 전달한다.

package com.landit.landitbe.feature.notification.client;

/** 일시적인 외부 Push 제공자 오류를 SQS 재시도로 전달한다. */
public class PushNotificationException extends RuntimeException {

  /**
   * 일시적인 Push 연동 오류를 생성한다.
   *
   * @param message Token 원문을 제외한 오류 설명
   */
  public PushNotificationException(String message) {
    super(message);
  }

  /**
   * 원인이 있는 일시적인 Push 연동 오류를 생성한다.
   *
   * @param message Token 원문을 제외한 오류 설명
   * @param cause 원본 예외
   */
  public PushNotificationException(String message, Throwable cause) {
    super(message, cause);
  }
}
