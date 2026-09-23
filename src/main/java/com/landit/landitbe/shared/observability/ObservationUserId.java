// 오류 관측에 사용할 내부 사용자 ID의 형식을 검증한다.

package com.landit.landitbe.shared.observability;

/** 인증 경계에서 제공한 양의 Long 사용자 ID만 관측 정보로 허용한다. */
public final class ObservationUserId {
  private ObservationUserId() {}

  /**
   * 사용자 값이나 임의 문자열이 ID 필드로 전달되지 않도록 검증한다.
   *
   * @param value 인증된 내부 사용자 ID
   * @return 양의 Long 문자열 또는 유효하지 않으면 null
   */
  public static String validate(String value) {
    if (value == null || !value.matches("[1-9][0-9]{0,18}")) {
      return null;
    }
    try {
      Long.parseLong(value);
      return value;
    } catch (NumberFormatException ignored) {
      return null;
    }
  }
}
