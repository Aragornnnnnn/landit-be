// 사용자 프로필 설정 API의 OpenAPI 문서를 정의한다.

package com.landit.landitbe.feature.profile.docs;

import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.feature.profile.dto.UserLearningLevelResponse;
import com.landit.landitbe.feature.profile.dto.UserLearningLevelUpdateRequest;
import com.landit.landitbe.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** 사용자 프로필 설정 API의 OpenAPI 문서를 정의한다. */
@Tag(name = "User Profile", description = "사용자 프로필 설정 API")
public interface UserProfileControllerDocs {

  /**
   * 인증된 사용자의 현재 학습 수준을 조회한다.
   *
   * @param principal 인증된 사용자
   * @return 사용자가 선택한 학습 수준. 미설정이면 {@code null}
   */
  @Operation(
      summary = "사용자 학습 수준 조회",
      description = "인증된 사용자가 선택한 1부터 5까지의 학습 수준을 조회합니다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 실패")
  })
  ApiResponse<UserLearningLevelResponse> getLearningLevel(AuthUserPrincipal principal);

  /**
   * 인증된 사용자의 학습 수준을 저장하거나 변경한다.
   *
   * @param principal 인증된 사용자
   * @param request 변경할 학습 수준
   * @return 데이터가 없는 성공 응답
   */
  @Operation(
      summary = "사용자 학습 수준 변경",
      description = "온보딩에서 선택한 1부터 5까지의 학습 수준을 저장합니다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "변경 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "요청 검증 실패"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 실패")
  })
  ApiResponse<Void> updateLearningLevel(
      AuthUserPrincipal principal, @Valid UserLearningLevelUpdateRequest request);
}
