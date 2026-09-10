// 사용자 프로필 설정 API의 OpenAPI 문서를 정의한다.

package com.landit.landitbe.feature.profile.docs;

import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.feature.profile.dto.AccentLocaleOptionResponse;
import com.landit.landitbe.feature.profile.dto.UserAccentLocaleResponse;
import com.landit.landitbe.feature.profile.dto.UserAccentLocaleUpdateRequest;
import com.landit.landitbe.feature.profile.dto.UserAlarmResponse;
import com.landit.landitbe.feature.profile.dto.UserAlarmUpdateRequest;
import com.landit.landitbe.feature.profile.dto.UserLearningLevelResponse;
import com.landit.landitbe.feature.profile.dto.UserLearningLevelUpdateRequest;
import com.landit.landitbe.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;

/** 사용자 프로필 설정 API의 OpenAPI 문서를 정의한다. */
@Tag(name = "User Profile", description = "사용자 프로필 설정 API")
public interface UserProfileControllerDocs {

  /**
   * 인증된 사용자의 일일 알람 설정을 조회한다.
   *
   * @param principal 인증된 사용자
   * @return 저장된 알람 설정. 미설정이면 time은 {@code null}, enabled는 {@code false}
   */
  @Operation(
      summary = "사용자 일일 알람 설정 조회",
      description = "매일 기기 현지 시간에 반복할 알람 1개의 설정을 조회합니다. 실제 예약과 전면 표시는 클라이언트가 처리합니다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 실패")
  })
  ApiResponse<UserAlarmResponse> getAlarm(AuthUserPrincipal principal);

  /**
   * 인증된 사용자의 일일 알람 시각과 활성 상태를 함께 저장한다.
   *
   * @param principal 인증된 사용자
   * @param request 변경할 알람 설정 전체
   * @return 저장한 알람 설정
   */
  @Operation(
      summary = "사용자 일일 알람 설정 변경",
      description =
          """
          사용자당 알람 1개의 설정 전체를 저장하거나 변경합니다. time은 00:00~23:59의 HH:mm 형식이며,
          비활성화 시에도 유지할 시각을 함께 전달해야 합니다. 서버는 시간대를 변환하거나 알람을 예약하지 않습니다.
          클라이언트는 저장된 설정으로 기존 기기 예약을 취소·재예약해야 합니다.
          성공 응답은 서버 설정 저장을 의미하며 기기 예약이나 전면 표시 성공을 보장하지 않습니다.
          """,
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "저장 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "요청 검증 실패"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 실패")
  })
  ApiResponse<UserAlarmResponse> updateAlarm(
      AuthUserPrincipal principal, @Valid UserAlarmUpdateRequest request);

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
   * 지원하는 영어 억양 선택지를 조회한다.
   *
   * @return 미국, 영국, 호주 억양 선택지
   */
  @Operation(summary = "영어 억양 선택지 조회", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 실패")
  })
  ApiResponse<List<AccentLocaleOptionResponse>> getAccentLocales();

  /**
   * 인증된 사용자의 현재 영어 억양을 조회한다.
   *
   * @param principal 인증된 사용자
   * @return 사용자의 현재 영어 억양
   */
  @Operation(summary = "사용자 현재 영어 억양 조회", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 실패")
  })
  ApiResponse<UserAccentLocaleResponse> getAccentLocale(AuthUserPrincipal principal);

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

  /**
   * 인증된 사용자의 영어 억양을 저장하거나 변경한다.
   *
   * @param principal 인증된 사용자
   * @param request 변경할 영어 억양
   * @return 데이터가 없는 성공 응답
   */
  @Operation(
      summary = "사용자 영어 억양 변경",
      description = "온보딩에서 선택한 미국, 영국, 호주 영어 억양을 저장합니다.",
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
  ApiResponse<Void> updateAccentLocale(
      AuthUserPrincipal principal, @Valid UserAccentLocaleUpdateRequest request);
}
