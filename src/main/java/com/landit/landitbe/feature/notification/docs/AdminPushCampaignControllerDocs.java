// 관리자 푸시 캠페인 API 계약을 문서화한다.

package com.landit.landitbe.feature.notification.docs;

import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.feature.notification.dto.AdminPushAudiencePreview;
import com.landit.landitbe.feature.notification.dto.AdminPushAudienceQueryRequest;
import com.landit.landitbe.feature.notification.dto.AdminPushCampaignRequest;
import com.landit.landitbe.feature.notification.dto.AdminPushCampaignView;
import com.landit.landitbe.feature.notification.dto.AdminPushSchedulePage;
import com.landit.landitbe.feature.notification.dto.AdminPushScheduleRequest;
import com.landit.landitbe.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

/** 관리자 푸시 캠페인 API의 입력과 응답을 설명한다. */
@Tag(name = "Admin Push Campaign", description = "관리자 일괄 푸시 캠페인")
@SecurityRequirement(name = "bearerAuth")
public interface AdminPushCampaignControllerDocs {

  /** SQL 결과의 ID를 조회한다. 서버는 별도 읽기 전용 DB 연결을 사용한다. */
  @Operation(
      summary = "대상 SQL 미리보기",
      description =
          "user_profile_id 한 컬럼의 SELECT/읽기 CTE만 허용한다. "
              + "시간 제한 10초, 기본 결과 제한 10만 행을 넘으면 전체 조회 실패다. SQL은 감사 로그에 남기지 않는다.")
  ApiResponse<List<Long>> queryAudience(
      @Parameter(hidden = true) AuthUserPrincipal principal,
      @Valid AdminPushAudienceQueryRequest request);

  /** 발송 시 SQL을 다시 조회하는 한국 시간 일회성 예약이다. */
  @Operation(
      summary = "캠페인 예약",
      description =
          "+09:00 오프셋을 명시한다. 최초 예약은 1분 이후여야 한다. "
              + "SCHEDULE_PENDING은 외부 등록 확인 전 상태로 같은 시각으로 재시도한다. 발송은 분 단위 정밀도다.")
  ApiResponse<AdminPushCampaignView> schedule(
      UUID campaignId,
      @Parameter(hidden = true) AuthUserPrincipal principal,
      String key,
      @Valid AdminPushScheduleRequest request);

  /** 시작 전 예약을 취소한다. 발송이 시작되었으면 409를 반환한다. */
  @Operation(summary = "캠페인 예약 취소")
  ApiResponse<AdminPushCampaignView> cancelSchedule(
      UUID campaignId, @Parameter(hidden = true) AuthUserPrincipal principal);

  /** 관리자 푸시 캠페인을 생성한다. */
  @Operation(
      summary = "푸시 캠페인 생성",
      description =
          "ALL(기본값) 또는 SELECTED다. SELECTED는 수동 userProfileIds와 선택적 audienceSql의 합집합에서 "
              + "excludedUserProfileIds를 뺀다. 1000명 캠페인 제한은 없고 내부 DB 처리는 1000개씩 나눈다. "
              + "SQL은 발송 시 다시 실행한다. 개별 클릭과 ID 붙여넣기는 같은 userProfileIds 배열로 전송한다. "
              + "대상 조건과 내용은 생성 후 수정할 수 없다.")
  ApiResponse<AdminPushCampaignView> create(
      @Parameter(hidden = true) AuthUserPrincipal principal,
      @Parameter(description = "1~128자 ASCII 영숫자, 대시, 밑줄") String key,
      @Valid AdminPushCampaignRequest request);

  /** 관리자 푸시 캠페인을 최신순으로 조회한다. */
  @Operation(summary = "푸시 캠페인 목록 조회")
  ApiResponse<List<AdminPushCampaignView>> list(int page, int size);

  /**
   * 예약 캠페인 목록과 페이지 정보를 조회한다.
   *
   * @param status 상태 필터. 생략하면 전체 예약 이력
   * @param page 0부터 시작하는 페이지 번호
   * @param size 페이지 크기. 기본 20, 최대 50
   * @return 상태와 예약 시각을 포함한 캠페인 페이지
   */
  @Operation(
      summary = "예약 푸시 목록 조회",
      description =
          "예약 시각이 있는 캠페인만 예약 시각·ID 내림차순으로 "
              + "조회한다. 상태 생략 시 등록 대기·발송 중·완료·취소를 모두 포함한다. scheduledAt은 UTC이며 "
              + "화면에서는 Asia/Seoul로 변환하고 페이지 번호는 page + 1로 표시한다. AWS 실시간 조회는 하지 않는다.")
  ApiResponse<AdminPushSchedulePage> schedules(
      @Parameter(
              description = "예약 상태",
              schema =
                  @io.swagger.v3.oas.annotations.media.Schema(
                      allowableValues = {
                        "SCHEDULE_PENDING",
                        "SCHEDULED",
                        "QUEUED",
                        "SENDING",
                        "COMPLETED",
                        "CANCELLED"
                      }))
          String status,
      int page,
      int size);

  /** 캠페인의 원문, 상태와 Token 기준 집계를 조회한다. */
  @Operation(summary = "푸시 캠페인 상세 조회")
  ApiResponse<AdminPushCampaignView> detail(UUID campaignId);

  /** 캠페인 대상 조건에 맞는 현재 활성 사용자와 Token 수를 조회한다. */
  @Operation(summary = "캠페인 예상 대상 조회")
  ApiResponse<AdminPushAudiencePreview> preview(UUID campaignId);

  /** 인증 관리자의 활성 Token에 테스트 알림을 보낸다. */
  @Operation(summary = "관리자 본인 테스트 발송")
  ApiResponse<AdminPushCampaignView> test(
      UUID campaignId, @Parameter(hidden = true) AuthUserPrincipal principal, String key);

  /** 캠페인의 대상 조건에 맞는 발송을 SQS에 요청한다. */
  @Operation(summary = "캠페인 발송", description = "저장된 ALL 또는 SELECTED 범위 중 활성 사용자·활성 Token에 발송한다.")
  ApiResponse<AdminPushCampaignView> send(
      UUID campaignId, @Parameter(hidden = true) AuthUserPrincipal principal, String key);
}
