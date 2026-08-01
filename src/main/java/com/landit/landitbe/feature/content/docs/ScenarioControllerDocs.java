// 시나리오 조회 API의 OpenAPI 문서를 정의한다.

package com.landit.landitbe.feature.content.docs;

import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.feature.content.domain.ScenarioCalendarType;
import com.landit.landitbe.feature.content.dto.DailyScenarioResponse;
import com.landit.landitbe.feature.content.dto.ScenarioCalendarResponse;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;

/** 날짜별 시나리오 조회 API의 OpenAPI 문서를 정의한다. */
@Tag(name = "Scenario", description = "시나리오 API")
public interface ScenarioControllerDocs {

  /**
   * 인증된 사용자의 오늘 배정 시나리오 또는 과거 최초 완료 이력을 조회한다.
   *
   * @param principal 인증된 사용자 정보
   * @param date 조회 날짜
   * @return 날짜별 시나리오 조회 응답
   * @throws ApiException 미래 날짜이거나 사용자·시나리오 정보를 찾을 수 없을 때
   */
  @Operation(
      summary = "날짜별 시나리오 조회",
      description = "오늘 배정된 시나리오 또는 과거 날짜에 최초 완료한 시나리오를 조회한다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "날짜 누락·형식 오류 또는 미래 날짜"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 실패")
  })
  ApiResponse<DailyScenarioResponse> getDailyScenario(
      AuthUserPrincipal principal,
      @Parameter(description = "조회 날짜", example = "2026-07-30", required = true)
          LocalDate date);

  /**
   * 인증된 사용자의 주·월 단위 시나리오 캘린더를 조회한다.
   *
   * @param principal 인증된 사용자 정보
   * @param type 캘린더 조회 단위
   * @param date 창의 기준 날짜. 생략하면 서버 기준 오늘
   * @return 창의 모든 칸이 채워진 캘린더 응답
   */
  @Operation(
      summary = "시나리오 캘린더 조회",
      description =
          "기준 날짜가 포함된 창의 모든 칸을 반환한다. WEEK은 그 날짜가 속한 주(일요일 시작) 7칸이며 이웃 달 날짜가 섞일 수 있다."
              + " MONTH은 그 달 1일부터 말일까지만 반환한다."
              + " 완료한 날은 완료 시나리오 ID와 썸네일이 담기고, 미완료 오늘 칸은 배정된 시나리오 ID만 담긴다. 그 외 칸은 비어 있다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "type 값 오류 또는 date 형식 오류"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 실패")
  })
  ApiResponse<ScenarioCalendarResponse> getCalendar(
      AuthUserPrincipal principal,
      @Parameter(description = "캘린더 조회 단위") ScenarioCalendarType type,
      @Parameter(description = "창의 기준 날짜(yyyy-MM-dd). 생략하면 서버 기준 오늘", example = "2026-07-30")
          LocalDate date);
}
