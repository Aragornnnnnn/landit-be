// 관리자 발음 평가 자산 임포트 API의 OpenAPI 문서를 정의한다.

package com.landit.landitbe.feature.content.docs;

import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.feature.content.dto.AdminPronunciationAssetCoverageResponse;
import com.landit.landitbe.feature.content.dto.AdminPronunciationAssetImportResult;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 관리자 발음 평가 자산 임포트 API의 OpenAPI 문서를 정의한다.
 *
 * <p>임포트는 2단계다: ① 기준 데이터(발음 표기) → ② TTS(음성 URL). 순서대로 실행해야 하며, 완료 여부는 커버리지 조회로 확인한다.
 */
@Tag(name = "Admin Pronunciation Asset", description = "관리자 발음 평가 자산 API")
public interface AdminPronunciationAssetControllerDocs {

  /**
   * 1단계 — S3의 기준 데이터 JSON(locale별)을 내려받아 자산의 발음 표기 데이터를 upsert한다.
   *
   * <p>기준 데이터의 문장이 DB의 대표 예문과 다르면 그 건은 실패 목록에 담긴다 (낡은 데이터 방지). 기준 데이터를 재임포트하면 단어별 음성 URL이 초기화되므로 이후
   * TTS 임포트를 다시 실행해야 한다.
   *
   * @param principal 인증된 관리자 사용자
   * @param manifestKey S3 기준 데이터 파일 키
   * @return 삽입·갱신 건수와 실패 목록
   * @throws ApiException 파일이 없거나(404) 형식이 잘못됐을 때(400)
   */
  @Operation(
      summary = "관리자 발음 기준 데이터 S3 임포트 (1단계)",
      description =
          "S3의 locale별 기준 데이터 JSON을 내려받아 (표현, 억양) 단위로 발음 표기 데이터를 upsert한다."
              + " manifestKey는 AI 파이프라인이 업로드한 파일 키를 그대로 사용한다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  ApiResponse<AdminPronunciationAssetImportResult> importReference(
      AuthUserPrincipal principal,
      @Parameter(
              description = "S3 기준 데이터 파일 키",
              example = "content/expression-pronunciation-audio/manifests/reference_EN_US.json")
          String manifestKey);

  /**
   * 2단계 — S3의 TTS 매니페스트를 내려받아 음성 URL을 기존 자산에 붙인다.
   *
   * <p>기준 데이터(1단계)가 없는 (표현, 억양)은 실패 목록에 담긴다.
   *
   * @param principal 인증된 관리자 사용자
   * @param manifestKey S3 TTS 매니페스트 키
   * @return 갱신 건수와 실패 목록
   * @throws ApiException 파일이 없거나(404) 형식이 잘못됐을 때(400)
   */
  @Operation(
      summary = "관리자 발음 TTS 매니페스트 S3 임포트 (2단계)",
      description =
          "S3의 TTS 매니페스트를 내려받아 문장·표현·단어별 음성 URL을 기존 자산에 붙인다." + " 기준 데이터 임포트(1단계)가 먼저 실행돼 있어야 한다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  ApiResponse<AdminPronunciationAssetImportResult> importTts(
      AuthUserPrincipal principal,
      @Parameter(
              description = "S3 TTS 매니페스트 키",
              example = "content/expression-pronunciation-audio/manifests/tts-2026-08-26.json")
          String manifestKey);

  /**
   * 활성 표현 전체의 억양별 자산 커버리지를 조회한다.
   *
   * <p>임포트 후 빠진 표현이 없는지 전수 확인하는 용도다. 기준 데이터 결석과 음성 결석을 따로 보여준다.
   *
   * @return 억양별 보유 수와 빠진 표현 ID 목록
   */
  @Operation(
      summary = "관리자 발음 평가 자산 커버리지 조회",
      description = "활성 표현 전체를 기준으로 억양별 기준 데이터·음성 보유 수와 빠진 표현 ID 목록을 반환한다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  ApiResponse<AdminPronunciationAssetCoverageResponse> coverage();
}
