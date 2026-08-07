// 세션 기능에서 의도한 실패를 기능 오류 코드와 함께 표현한다.

package com.landit.landitbe.feature.session.exception;

import com.landit.landitbe.shared.exception.FeatureException;

/** 세션 기능에서 의도한 실패를 기능 오류 코드와 함께 표현한다. */
public class SessionException extends FeatureException {

  private final SessionErrorCode errorCode;

  /**
   * 세션 오류 코드의 기본 메시지로 예외를 생성한다.
   *
   * @param errorCode 세션 오류 코드
   */
  public SessionException(SessionErrorCode errorCode) {
    super(errorCode.name(), errorCode.getStatus(), errorCode.getMessage());
    this.errorCode = errorCode;
  }

  /**
   * 세션 오류 코드를 반환한다.
   *
   * @return 세션 오류 코드
   */
  public SessionErrorCode getErrorCode() {
    return errorCode;
  }
}
