// 발음 평가 자산의 억양별 커버리지 현황을 표현한다.

package com.landit.landitbe.feature.content.dto;

import com.landit.landitbe.shared.domain.AccentLocale;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 발음 평가 자산의 억양별 커버리지 현황을 표현한다.
 *
 * <p>임포트 후 빠진 표현이 없는지 전수 확인하는 용도다. 어드민 페이지의 자산 현황 카드가 이 응답을 표시한다.
 *
 * @param totalActiveExpressions 활성 표현 전체 수
 * @param locales 억양별 커버리지 목록
 */
@Schema(description = "발음 평가 자산 커버리지 현황")
public record AdminPronunciationAssetCoverageResponse(
    @Schema(description = "활성 표현 전체 수", example = "981") int totalActiveExpressions,
    @Schema(description = "억양별 커버리지 목록") List<LocaleCoverage> locales) {

  /**
   * 억양 1개의 커버리지를 표현한다.
   *
   * @param accentLocale 억양 locale
   * @param covered 자산이 있는 활성 표현 수
   * @param missing 자산이 없는 활성 표현 ID 목록
   */
  @Schema(description = "억양 1개의 커버리지")
  public record LocaleCoverage(
      @Schema(description = "억양 locale", example = "EN_US") AccentLocale accentLocale,
      @Schema(description = "자산이 있는 활성 표현 수", example = "979") int covered,
      @Schema(description = "자산이 없는 활성 표현 ID 목록", example = "[455, 812]") List<Long> missing) {}
}
