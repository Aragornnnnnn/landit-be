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

/** 관리자 발음 평가 자산 임포트 API의 OpenAPI 문서를 정의한다. */
@Tag(name = "Admin Pronunciation Asset", description = "관리자 발음 평가 자산 API")
public interface AdminPronunciationAssetControllerDocs {

  /**
   * S3의 매니페스트를 내려받아 발음 평가 자산을 (표현, 억양) 단위로 일괄 upsert한다.
   *
   * <p>TTS 사전 생성 배치(landit-iac)가 매니페스트를 S3에 업로드한 뒤, 배치가 출력한 매니페스트 키를 그대로 전달해 1회 호출한다. 실패한 건은 사유와 함께
   * 결과 목록으로 반환된다.
   *
   * @param principal 인증된 관리자 사용자
   * @param manifestKey S3 매니페스트 키
   * @return 삽입·갱신 건수와 실패 목록
   * @throws ApiException 매니페스트가 없거나(404) 형식이 잘못됐을 때(400)
   */
  @Operation(
      summary = "관리자 발음 평가 자산 S3 임포트",
      description =
          "S3 콘텐츠 버킷의 매니페스트 JSON을 내려받아 (표현, 억양) 단위로 upsert한다."
              + " manifestKey는 TTS 생성 배치가 출력한 키를 그대로 사용한다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  ApiResponse<AdminPronunciationAssetImportResult> importFromS3(
      AuthUserPrincipal principal,
      @Parameter(
              description = "S3 매니페스트 키",
              example = "content/expression-pronunciation-audio/manifests/2026-08-26-full.json")
          String manifestKey);

  /**
   * 활성 표현 전체의 억양별 자산 커버리지를 조회한다.
   *
   * <p>임포트 후 빠진 표현이 없는지 전수 확인하는 용도다.
   *
   * @return 억양별 보유 수와 빠진 표현 ID 목록
   */
  @Operation(
      summary = "관리자 발음 평가 자산 커버리지 조회",
      description = "활성 표현 전체를 기준으로 억양별 자산 보유 수와 빠진 표현 ID 목록을 반환한다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  ApiResponse<AdminPronunciationAssetCoverageResponse> coverage();
}
