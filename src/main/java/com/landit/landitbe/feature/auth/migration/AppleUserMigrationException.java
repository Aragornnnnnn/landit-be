// Apple 사용자 이전 실패를 비식별 오류 코드로 전달한다.

package com.landit.landitbe.feature.auth.migration;

/** Apple 사용자 이전 실패를 비식별 오류 코드로 전달한다. */
public class AppleUserMigrationException extends RuntimeException {

  private final String failureCode;

  /**
   * 비식별 오류 코드로 예외를 생성한다.
   *
   * @param failureCode 운영 로그에 기록할 고정 오류 코드
   */
  public AppleUserMigrationException(String failureCode) {
    super("Apple user migration failed: " + failureCode);
    this.failureCode = failureCode;
  }

  /**
   * 비식별 오류 코드와 원인으로 예외를 생성한다.
   *
   * @param failureCode 운영 로그에 기록할 고정 오류 코드
   * @param cause 원인 예외
   */
  public AppleUserMigrationException(String failureCode, Throwable cause) {
    super("Apple user migration failed: " + failureCode, cause);
    this.failureCode = failureCode;
  }

  /**
   * 운영 로그에 기록 가능한 비식별 오류 코드를 반환한다.
   *
   * @return 비식별 오류 코드
   */
  public String failureCode() {
    return failureCode;
  }
}
