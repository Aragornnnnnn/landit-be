// 관리자 발음 평가 자산 일괄 임포트 결과를 표현한다.

package com.landit.landitbe.feature.content.dto;

import com.landit.landitbe.shared.domain.AccentLocale;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 관리자 발음 평가 자산 일괄 임포트 결과를 표현한다.
 *
 * <p>실패한 건은 조용히 건너뛰지 않고 사유와 함께 목록으로 반환한다.
 *
 * @param inserted 새로 삽입된 자산 수
 * @param updated 기존 자산이 갱신된 수
 * @param failures 실패한 자산 목록
 */
@Schema(description = "관리자 발음 평가 자산 일괄 임포트 결과")
public record AdminPronunciationAssetImportResult(
    @Schema(description = "새로 삽입된 자산 수", example = "80") int inserted,
    @Schema(description = "기존 자산이 갱신된 수", example = "18") int updated,
    @Schema(description = "실패한 자산 목록") List<Failure> failures) {

  /**
   * 임포트에 실패한 자산 1건을 표현한다.
   *
   * @param expressionId Writing 표현 ID
   * @param accentLocale 억양 locale
   * @param reason 실패 사유
   */
  @Schema(description = "임포트 실패 자산 1건")
  public record Failure(
      @Schema(description = "Writing 표현 ID", example = "9999") Long expressionId,
      @Schema(description = "억양 locale", example = "EN_US") AccentLocale accentLocale,
      @Schema(description = "실패 사유", example = "존재하지 않는 표현입니다.") String reason) {}
}
