// 사용자 프로필 기능에서 의도한 실패를 기능 오류 코드와 함께 표현한다.

package com.landit.landitbe.feature.profile.exception;

import com.landit.landitbe.shared.exception.FeatureException;

/** 사용자 프로필 기능에서 의도한 실패를 기능 오류 코드와 함께 표현한다. */
public class UserProfileException extends FeatureException {

  private final UserProfileErrorCode errorCode;

  /**
   * 사용자 프로필 오류 코드의 기본 메시지로 예외를 생성한다.
   *
   * @param errorCode 사용자 프로필 오류 코드
   */
  public UserProfileException(UserProfileErrorCode errorCode) {
    super(errorCode.name(), errorCode.getStatus(), errorCode.getMessage());
    this.errorCode = errorCode;
  }

  /**
   * 사용자 프로필 오류 코드를 반환한다.
   *
   * @return 사용자 프로필 오류 코드
   */
  public UserProfileErrorCode getErrorCode() {
    return errorCode;
  }
}
