// 스트릭 조회 API의 OpenAPI 문서를 정의한다.

package com.landit.landitbe.feature.character.docs;

import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.feature.character.dto.CurrentStreakResponse;
import com.landit.landitbe.feature.character.dto.StreakCalendarResponse;
import com.landit.landitbe.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** 스트릭 조회 API의 OpenAPI 문서를 정의한다. */
@Tag(name = "Streak", description = "스트릭 API")
public interface StreakControllerDocs {

  /**
   * 인증된 사용자의 현재 스트릭과 오늘 완료 여부를 조회한다.
   *
   * @param principal 인증된 사용자 정보
   * @return 현재 스트릭 API 응답
   */
  @Operation(
      summary = "현재 스트릭 조회",
      description = "현재 연속 학습 일수와 오늘 정상 완료 여부를 조회한다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 실패")
  })
  ApiResponse<CurrentStreakResponse> getCurrentStreak(AuthUserPrincipal principal);

  /**
   * 인증된 사용자의 월별 스트릭 달력 정보를 조회한다.
   *
   * @param principal 인증된 사용자 정보
   * @param year 조회 연도
   * @param month 조회 월
   * @return 월별 스트릭 달력 API 응답
   * @throws ConstraintViolationException year 또는 month가 허용 범위를 벗어난 경우
   */
  @Operation(
      summary = "월별 스트릭 달력 조회",
      description = "현재 스트릭 통계와 요청한 연·월의 완료 날짜를 조회한다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "요청 값 오류"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 실패")
  })
  ApiResponse<StreakCalendarResponse> getCalendar(
      AuthUserPrincipal principal,
      @Parameter(description = "조회 연도", example = "2026", required = true) @Min(1) int year,
      @Parameter(description = "조회 월", example = "7", required = true) @Min(1) @Max(12) int month);
}
