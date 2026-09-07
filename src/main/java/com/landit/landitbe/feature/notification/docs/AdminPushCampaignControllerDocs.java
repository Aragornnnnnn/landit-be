// 관리자 푸시 캠페인 API의 멱등성, 실행 상태와 집계 계약을 문서화한다.

package com.landit.landitbe.feature.notification.docs;

import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.feature.notification.dto.AdminPushAudiencePreview;
import com.landit.landitbe.feature.notification.dto.AdminPushCampaignRequest;
import com.landit.landitbe.feature.notification.dto.AdminPushCampaignView;
import com.landit.landitbe.feature.notification.dto.AdminPushRunView;
import com.landit.landitbe.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

/** 관리자 푸시 캠페인의 API 계약이다. 토큰 원문은 응답에 포함하지 않는다. */
@Tag(name = "Admin Push Campaign", description = "관리자 푸시 캠페인과 비동기 실행 관리")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "400",
      description = "입력 검증 실패"),
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "403",
      description = "관리자 권한 필요"),
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "404",
      description = "캠페인 또는 실행 없음")
})
public interface AdminPushCampaignControllerDocs {

  /**
   * 생성 즉시 원문이 고정되는 캠페인을 생성한다.
   *
   * @param principal 인증 관리자
   * @param key 관리자별 생성 멱등성 키
   * @param request 제목, 본문과 내부 경로 또는 HTTPS URL
   * @return 캠페인 원문과 식별자
   */
  @Operation(
      summary = "푸시 캠페인 생성",
      description = "원문은 생성 후 수정할 수 없습니다. 같은 키와 같은 입력은 기존 ID를 반환합니다. 표시 필드 JSON은 3,000바이트 이하입니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "201",
        description = "캠페인 생성 또는 기존 캠페인 반환"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "409",
        description = "IDEMPOTENCY_KEY_CONFLICT: 같은 키에 다른 입력 사용")
  })
  ApiResponse<AdminPushCampaignView> create(
      @Parameter(hidden = true) AuthUserPrincipal principal,
      @Parameter(description = "1~128자 ASCII 영숫자, 대시, 밑줄") String key,
      @Valid AdminPushCampaignRequest request);

  /**
   * 최근 생성한 캠페인부터 페이지 단위로 조회한다.
   *
   * @param page 0부터 시작하는 페이지
   * @param size 1~50의 페이지 크기
   * @return 캠페인과 전체 실행 요약 목록
   */
  @Operation(summary = "푸시 캠페인 목록 조회")
  ApiResponse<List<AdminPushCampaignView>> list(int page, int size);

  /**
   * 원문과 전체 및 테스트 실행을 조회한다.
   *
   * @param campaignId 캠페인 ID
   * @return 불변 원문, 상태와 실행 집계
   */
  @Operation(
      summary = "푸시 캠페인 상세 조회",
      description = "전체 실행이 없으면 DRAFT입니다. COMPLETED는 전원 성공이나 기기 수신 완료를 의미하지 않습니다.")
  ApiResponse<AdminPushCampaignView> detail(UUID campaignId);

  /**
   * 조회 시점의 활성 사용자와 토큰 수를 예상한다.
   *
   * @param campaignId 캠페인 ID
   * @return 예상 사용자 수, 토큰 수와 조회 시각
   */
  @Operation(
      summary = "전체 발송 예상 대상 조회",
      description = "활성 사용자의 활성 토큰 기준입니다. 준비 단계의 실제 확정 수와 달라질 수 있습니다.")
  ApiResponse<AdminPushAudiencePreview> preview(UUID campaignId);

  /**
   * 인증 관리자의 활성 토큰에 원문 그대로 테스트 실행을 요청한다.
   *
   * @param campaignId 캠페인 ID
   * @param principal 수신 대상이 되는 인증 관리자
   * @param key 동일 테스트 실행을 식별하는 멱등성 키
   * @return 별도 집계와 중복 키를 사용하는 테스트 실행
   */
  @Operation(
      summary = "관리자 본인 테스트 발송",
      description = "전체 발송 시작 전만 새 테스트를 생성합니다. 관리자별 새 테스트는 10초에 1회이며 같은 키의 재요청은 기존 실행을 반환합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "202",
        description = "비동기 실행 접수"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "409",
        description = "전체 발송이 이미 시작됨"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "429",
        description = "관리자 테스트 생성 간격 제한")
  })
  ApiResponse<AdminPushRunView> test(
      UUID campaignId, @Parameter(hidden = true) AuthUserPrincipal principal, String key);

  /**
   * 캠페인의 유일한 전체 발송 실행을 요청한다.
   *
   * @param campaignId 캠페인 ID
   * @param principal 요청 관리자
   * @param key 요청 멱등성 키
   * @return 신규 또는 이미 존재하는 전체 발송 실행
   */
  @Operation(
      summary = "캠페인 전체 발송",
      description = "다른 관리자나 다른 키로 다시 요청해도 기존 전체 실행을 반환합니다. 접수 여부가 불명확한 토큰은 자동 재발송하지 않습니다.")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "202",
      description = "비동기 실행 접수")
  ApiResponse<AdminPushRunView> send(
      UUID campaignId, @Parameter(hidden = true) AuthUserPrincipal principal, String key);

  /**
   * 캠페인에 속한 실행의 상태와 토큰 집계를 조회한다.
   *
   * @param campaignId 캠페인 ID
   * @param runId 실행 ID
   * @return 처리 중, 성공, 실패, 제외, 확인 불가와 Ticket 접수 수
   */
  @Operation(
      summary = "푸시 실행 조회",
      description =
          "성공은 Receipt의 APNs/FCM 인계 성공입니다. 확정 대상 수는 준비 전 null이며, 확정 후 "
              + "처리 중+성공+실패+제외+확인 불가의 합입니다. Ticket 접수 수는 별도 참고 지표입니다.")
  ApiResponse<AdminPushRunView> run(UUID campaignId, UUID runId);

  /**
   * 중단된 실행의 기존 대상과 이력을 유지한 채 미처리 작업을 재개한다.
   *
   * @param campaignId 캠페인 ID
   * @param runId 실행 ID
   * @param principal 복구 요청 관리자
   * @return 재개한 실행 또는 이미 진행 중인 실행
   */
  @Operation(
      summary = "중단된 푸시 실행 복구",
      description = "BLOCKED 실행의 미처리 작업을 재개합니다. 실패·확인 불가 알림은 다시 제출하지 않습니다. 진행 중 재요청은 현재 상태를 반환합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "202",
        description = "복구 접수 또는 현재 실행 반환"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "409",
        description = "이미 완료된 실행")
  })
  ApiResponse<AdminPushRunView> resume(
      UUID campaignId, UUID runId, @Parameter(hidden = true) AuthUserPrincipal principal);
}
