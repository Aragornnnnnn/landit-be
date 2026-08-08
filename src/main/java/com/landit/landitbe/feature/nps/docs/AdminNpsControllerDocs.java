// 관리자 NPS 목록 API의 OpenAPI 문서를 정의한다.

package com.landit.landitbe.feature.nps.docs;

import com.landit.landitbe.feature.nps.dto.AdminNpsResponsePage;
import com.landit.landitbe.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/** 관리자 NPS 목록 API의 OpenAPI 문서를 정의한다. */
@Tag(name = "Admin NPS", description = "관리자 NPS API")
public interface AdminNpsControllerDocs {

  /**
   * 최신 NPS 응답을 작성자 정보와 함께 조회한다.
   *
   * @param page 페이지 번호
   * @param size 페이지 크기
   * @return 관리자 NPS 목록 페이지 응답
   */
  @Operation(
      summary = "관리자 NPS 목록 조회",
      description = "NPS 응답을 제출 시각 최신순으로 페이지 조회하고 " + "작성자 정보를 함께 반환한다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "페이지 요청 오류"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 실패"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "403",
        description = "관리자 권한 없음")
  })
  ApiResponse<AdminNpsResponsePage> list(
      @Parameter(description = "0부터 시작하는 페이지 번호", example = "0") int page,
      @Parameter(description = "페이지 크기 (1~50)", example = "20") int size);
}
