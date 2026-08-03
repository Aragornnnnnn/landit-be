// Major.Minor.Patch 형식의 앱 버전명을 숫자로 비교한다.

package com.landit.landitbe.feature.app.domain;

import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Major.Minor.Patch 형식의 앱 버전명을 숫자로 비교한다.
 *
 * @param major 주 버전 번호
 * @param minor 부 버전 번호
 * @param patch 패치 버전 번호
 */
public record AppVersionName(BigInteger major, BigInteger minor, BigInteger patch)
    implements Comparable<AppVersionName> {

  private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)");

  /**
   * 문자열 앱 버전명을 숫자 구성 요소로 변환한다.
   *
   * @param versionName 변환할 Major.Minor.Patch 형식 버전명
   * @return 숫자로 분리된 앱 버전명
   * @throws IllegalArgumentException 버전명이 Major.Minor.Patch 형식이 아닐 때
   */
  public static AppVersionName parse(String versionName) {
    Matcher matcher = VERSION_PATTERN.matcher(versionName);
    if (!matcher.matches()) {
      throw new IllegalArgumentException("앱 버전명은 Major.Minor.Patch 형식이어야 합니다.");
    }
    return new AppVersionName(
        new BigInteger(matcher.group(1)),
        new BigInteger(matcher.group(2)),
        new BigInteger(matcher.group(3)));
  }

  /**
   * 다른 앱 버전명과 Major, Minor, Patch 순으로 비교한다.
   *
   * @param other 비교할 앱 버전명
   * @return 현재 버전이 낮으면 음수, 같으면 0, 높으면 양수
   */
  @Override
  public int compareTo(AppVersionName other) {
    int majorComparison = major.compareTo(other.major);
    if (majorComparison != 0) {
      return majorComparison;
    }
    int minorComparison = minor.compareTo(other.minor);
    if (minorComparison != 0) {
      return minorComparison;
    }
    return patch.compareTo(other.patch);
  }
}
