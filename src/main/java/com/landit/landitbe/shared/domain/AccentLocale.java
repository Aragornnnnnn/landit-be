// 학습 튜터와 TTS 음성의 지역별 발음 locale을 정의한다.

package com.landit.landitbe.shared.domain;

/** 학습 튜터와 TTS 음성의 지역별 발음 locale을 정의한다. */
public enum AccentLocale {
  EN_US,
  EN_AU,
  EN_GB;

  /**
   * 억양 locale에 대응하는 나라 이름을 반환한다.
   *
   * @return 나라 이름
   */
  public String countryName() {
    return switch (this) {
      case EN_US -> "미국";
      case EN_GB -> "영국";
      case EN_AU -> "호주";
    };
  }
}
