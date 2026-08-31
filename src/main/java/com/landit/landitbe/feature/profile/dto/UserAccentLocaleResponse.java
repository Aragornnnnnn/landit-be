// 사용자의 현재 영어 억양을 API 응답으로 제공한다.

package com.landit.landitbe.feature.profile.dto;

import com.landit.landitbe.shared.domain.AccentLocale;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 사용자의 현재 영어 억양을 API 응답으로 제공한다.
 *
 * @param accentLocale 현재 억양 코드
 * @param name 나라 이름
 */
@Schema(description = "사용자 현재 영어 억양")
public record UserAccentLocaleResponse(
    @Schema(description = "현재 억양 코드", example = "EN_US") AccentLocale accentLocale,
    @Schema(description = "나라 이름", example = "미국") String name) {

  /**
   * 억양 locale을 현재 억양 응답으로 변환한다.
   *
   * @param accentLocale 변환할 억양 locale
   * @return 현재 억양 응답
   */
  public static UserAccentLocaleResponse from(AccentLocale accentLocale) {
    return new UserAccentLocaleResponse(accentLocale, accentLocale.countryName());
  }
}
