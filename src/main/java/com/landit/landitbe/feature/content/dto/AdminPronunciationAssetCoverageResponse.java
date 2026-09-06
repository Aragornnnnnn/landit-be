// 발음 평가 자산의 억양별 커버리지 현황을 표현한다.

package com.landit.landitbe.feature.content.dto;

import com.landit.landitbe.shared.domain.AccentLocale;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 발음 평가 자산의 억양별 커버리지 현황을 표현한다.
 *
 * <p>임포트가 2단계(기준 데이터 → TTS)라 상태도 둘로 나눠 보여준다: 기준 데이터가 아예 없는 표현과, 기준 데이터는 있는데 음성이 아직 없는 표현. 임포트 후 두
 * missing이 모두 비어 있어야 완료다. 어드민 페이지의 자산 현황 카드가 이 응답을 표시한다.
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
   * @param referenceCovered 기준 데이터가 있는 활성 표현 수
   * @param referenceMissing 기준 데이터가 없는 활성 표현 ID 목록
   * @param audioCovered 음성(TTS)까지 완성된 활성 표현 수
   * @param audioMissing 기준 데이터는 있으나 음성이 없는 활성 표현 ID 목록
   */
  @Schema(description = "억양 1개의 커버리지")
  public record LocaleCoverage(
      @Schema(description = "억양 locale", example = "EN_US") AccentLocale accentLocale,
      @Schema(description = "기준 데이터가 있는 활성 표현 수", example = "981") int referenceCovered,
      @Schema(description = "기준 데이터가 없는 활성 표현 ID 목록", example = "[455, 812]")
          List<Long> referenceMissing,
      @Schema(description = "음성(TTS)까지 완성된 활성 표현 수", example = "979") int audioCovered,
      @Schema(description = "기준 데이터는 있으나 음성이 없는 활성 표현 ID 목록", example = "[977]")
          List<Long> audioMissing) {}
}
