// 구독 기능에서 의도한 실패를 기능 오류 코드와 함께 표현한다.

package com.landit.landitbe.feature.subscription.exception;

import com.landit.landitbe.shared.exception.FeatureException;

/** 구독 기능에서 의도한 실패를 기능 오류 코드와 함께 표현한다. */
public class SubscriptionException extends FeatureException {

  private final SubscriptionErrorCode errorCode;

  /**
   * 구독 오류 코드의 기본 메시지로 예외를 생성한다.
   *
   * @param errorCode 구독 오류 코드
   */
  public SubscriptionException(SubscriptionErrorCode errorCode) {
    super(errorCode.name(), errorCode.getStatus(), errorCode.getMessage());
    this.errorCode = errorCode;
  }

  /**
   * 구독 오류 코드를 반환한다.
   *
   * @return 구독 오류 코드
   */
  public SubscriptionErrorCode getErrorCode() {
    return errorCode;
  }
}
