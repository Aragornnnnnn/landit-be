// 명시적으로 안전한 외부 Push 재시도 오류를 구분한다.

package com.landit.landitbe.feature.notification.client;

/** 외부 제공자가 요청을 처리하지 않았다고 판단할 수 있어 같은 발송을 재시도할 수 있는 오류다. */
public class RetryablePushNotificationException extends PushNotificationException {

  /**
   * 재시도 가능한 Push 연동 오류를 생성한다.
   *
   * @param message Token 원문을 제외한 오류 설명
   */
  public RetryablePushNotificationException(String message) {
    super(message);
  }

  /**
   * 원인이 있는 재시도 가능한 Push 연동 오류를 생성한다.
   *
   * @param message Token 원문을 제외한 오류 설명
   * @param cause 원본 예외
   */
  public RetryablePushNotificationException(String message, Throwable cause) {
    super(message, cause);
  }
}
