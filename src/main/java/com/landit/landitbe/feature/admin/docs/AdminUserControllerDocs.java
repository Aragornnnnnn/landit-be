// 관리자 사용자 API의 OpenAPI 문서를 정의한다.

package com.landit.landitbe.feature.admin.docs;

import com.landit.landitbe.feature.admin.dto.AdminUserDetailResponse;
import com.landit.landitbe.feature.admin.dto.AdminUserListResponse;
import com.landit.landitbe.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/** 관리자 사용자 API의 OpenAPI 문서를 정의한다. */
@Tag(name = "Admin User", description = "관리자 사용자 조회 API")
public interface AdminUserControllerDocs {

  /**
   * 관리자 사용자 목록을 조회한다.
   *
   * @param page 페이지 번호
   * @param size 페이지 크기
   * @return 관리자 사용자 목록 응답
   */
  @Operation(
      summary = "관리자 사용자 목록 조회",
      description = "사용자 기본 정보를 가입일 최신순으로 페이지 조회한다.",
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
  ApiResponse<AdminUserListResponse> list(
      @Parameter(description = "0부터 시작하는 페이지 번호", example = "0") int page,
      @Parameter(description = "페이지 크기 (1~50)", example = "20") int size);

  /**
   * 관리자 사용자 상세를 조회한다.
   *
   * @param userProfileId 사용자 프로필 ID
   * @return 관리자 사용자 상세 응답
   */
  @Operation(
      summary = "관리자 사용자 상세 조회",
      description = "사용자 프로필과 최소 학습 요약을 조회한다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 실패"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "403",
        description = "관리자 권한 없음"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "사용자 없음")
  })
  ApiResponse<AdminUserDetailResponse> detail(
      @Parameter(description = "사용자 프로필 ID", example = "1") long userProfileId);
}
