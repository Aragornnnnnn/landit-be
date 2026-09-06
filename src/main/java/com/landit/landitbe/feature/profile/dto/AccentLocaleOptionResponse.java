// 지원하는 영어 억양 선택지를 API 응답으로 제공한다.

package com.landit.landitbe.feature.profile.dto;

import com.landit.landitbe.shared.domain.AccentLocale;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 지원하는 영어 억양 선택지를 API 응답으로 제공한다.
 *
 * @param code 억양 코드
 * @param name 나라 이름
 */
@Schema(description = "영어 억양 선택지")
public record AccentLocaleOptionResponse(
    @Schema(description = "억양 코드", example = "EN_US") String code,
    @Schema(description = "나라 이름", example = "미국") String name) {

  /**
   * 억양 locale을 선택지 응답으로 변환한다.
   *
   * @param accentLocale 변환할 억양 locale
   * @return 억양 선택지 응답
   */
  public static AccentLocaleOptionResponse from(AccentLocale accentLocale) {
    return new AccentLocaleOptionResponse(accentLocale.name(), accentLocale.countryName());
  }
}
