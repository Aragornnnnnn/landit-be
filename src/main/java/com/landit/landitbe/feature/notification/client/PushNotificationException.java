// 외부 Push 연동 중 발생한 오류를 호출자에게 전달한다.

package com.landit.landitbe.feature.notification.client;

/** 외부 Push 요청의 처리 결과를 확정할 수 없거나 연동 계약을 해석할 수 없을 때 발생한다. */
public class PushNotificationException extends RuntimeException {

  /**
   * Push 연동 오류를 생성한다.
   *
   * @param message Token 원문을 제외한 오류 설명
   */
  public PushNotificationException(String message) {
    super(message);
  }

  /**
   * 원인이 있는 Push 연동 오류를 생성한다.
   *
   * @param message Token 원문을 제외한 오류 설명
   * @param cause 원본 예외
   */
  public PushNotificationException(String message, Throwable cause) {
    super(message, cause);
  }
}
