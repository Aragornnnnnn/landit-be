// 관리자 푸시 캠페인 API 계약을 문서화한다.

package com.landit.landitbe.feature.notification.docs;

import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.feature.notification.dto.AdminPushAudiencePreview;
import com.landit.landitbe.feature.notification.dto.AdminPushAudienceQueryRequest;
import com.landit.landitbe.feature.notification.dto.AdminPushCampaignPage;
import com.landit.landitbe.feature.notification.dto.AdminPushCampaignRequest;
import com.landit.landitbe.feature.notification.dto.AdminPushCampaignView;
import com.landit.landitbe.feature.notification.dto.AdminPushScheduleRequest;
import com.landit.landitbe.shared.exception.ApiException;
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

  /**
   * SQL 결과의 ID를 조회한다. 서버는 별도 읽기 전용 DB 연결을 사용한다.
   *
   * @param principal 인증 관리자
   * @param request 대상 조회 SQL
   * @return 중복을 제거한 사용자 ID
   * @throws ApiException 설정 누락, SQL 실행 실패 또는 잘못된 결과일 때 발생
   */
  @Operation(
      summary = "대상 SQL 미리보기",
      description =
          "user_profile_id 한 컬럼의 SELECT/읽기 CTE만 허용한다. "
              + "시간 제한 10초, 기본 결과 제한 10만 행을 넘으면 전체 조회 실패다. SQL은 감사 로그에 남기지 않는다.")
  ApiResponse<List<Long>> queryAudience(
      @Parameter(hidden = true) AuthUserPrincipal principal,
      @Valid AdminPushAudienceQueryRequest request);

  /**
   * 발송 시 SQL을 다시 조회하는 한국 시간 일회성 예약이다.
   *
   * @param campaignId 캠페인 ID
   * @param principal 인증 관리자
   * @param key 요청 멱등성 키
   * @param request 한국 시간 예약 시각
   * @return 예약 상태와 캠페인 정보
   * @throws ApiException 잘못된 시각, 없는 캠페인 또는 상태 충돌 시 발생
   */
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

  /**
   * 시작 전 예약을 취소한다. 발송이 시작되었으면 409를 반환한다.
   *
   * @param campaignId 캠페인 ID
   * @param principal 인증 관리자
   * @return 취소된 캠페인 정보
   * @throws ApiException 캠페인이 없거나 발송이 시작된 경우 발생
   */
  @Operation(summary = "캠페인 예약 취소")
  ApiResponse<AdminPushCampaignView> cancelSchedule(
      UUID campaignId, @Parameter(hidden = true) AuthUserPrincipal principal);

  /**
   * 관리자 푸시 캠페인을 생성한다.
   *
   * @param principal 인증 관리자
   * @param key 생성 요청 멱등성 키
   * @param request 캠페인 내용과 대상 조건
   * @return 생성되었거나 같은 요청으로 이미 생성된 캠페인
   * @throws ApiException 입력이 잘못되었거나 같은 키에 다른 내용을 요청한 경우 발생
   */
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

  /**
   * 캠페인 목록과 페이지 정보를 조회한다.
   *
   * @param scheduled true이면 예약, false이면 예약 없는 캠페인. 생략하면 전체
   * @param status 상태 필터. 생략하면 모든 상태
   * @param page 0부터 시작하는 페이지 번호
   * @param size 페이지 크기. 기본 20, 최대 50
   * @return 필터가 적용된 캠페인 페이지
   */
  @Operation(
      summary = "푸시 캠페인 목록 조회",
      description =
          "예약 여부와 상태를 AND 조건으로 필터링하고 "
              + "생성 시각·ID 내림차순으로 조회한다. 응답은 items와 페이지 정보다. "
              + "scheduledAt은 UTC이며 화면에서는 Asia/Seoul로 변환하고 페이지는 page + 1로 표시한다.")
  ApiResponse<AdminPushCampaignPage> list(
      @Parameter(description = "예약 시각 보유 여부. false에는 초안도 포함") Boolean scheduled,
      @Parameter(
              schema =
                  @io.swagger.v3.oas.annotations.media.Schema(
                      allowableValues = {
                        "DRAFT",
                        "PENDING",
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

  /**
   * 캠페인의 원문, 상태와 Token 기준 집계를 조회한다.
   *
   * @param campaignId 캠페인 ID
   * @return 내용, 상태와 토큰 단위 집계
   * @throws ApiException 캠페인이 없는 경우 발생
   */
  @Operation(summary = "푸시 캠페인 상세 조회")
  ApiResponse<AdminPushCampaignView> detail(UUID campaignId);

  /**
   * 캠페인 대상 조건에 맞는 현재 활성 사용자와 Token 수를 조회한다.
   *
   * @param campaignId 캠페인 ID
   * @return 조회 시점의 예상 사용자 수와 토큰 수
   * @throws ApiException 캠페인이 없거나 SQL 대상 조회가 실패한 경우 발생
   */
  @Operation(summary = "캠페인 예상 대상 조회")
  ApiResponse<AdminPushAudiencePreview> preview(UUID campaignId);

  /**
   * 인증 관리자의 활성 Token에 테스트 알림을 보낸다.
   *
   * @param campaignId 캠페인 ID
   * @param principal 인증 관리자
   * @param key 테스트 요청 멱등성 키
   * @return 테스트를 요청한 캠페인 정보
   * @throws ApiException 잘못된 키, 없는 캠페인 또는 DRAFT가 아닌 상태일 때 발생
   */
  @Operation(summary = "관리자 본인 테스트 발송")
  ApiResponse<AdminPushCampaignView> test(
      UUID campaignId, @Parameter(hidden = true) AuthUserPrincipal principal, String key);

  /**
   * 캠페인의 대상 조건에 맞는 발송을 SQS에 요청한다.
   *
   * @param campaignId 캠페인 ID
   * @param principal 인증 관리자
   * @param key 발송 요청 멱등성 키
   * @return 발송을 요청한 캠페인 정보
   * @throws ApiException 잘못된 키, 없는 캠페인 또는 상태 충돌 시 발생
   */
  @Operation(summary = "캠페인 발송", description = "저장된 ALL 또는 SELECTED 범위 중 활성 사용자·활성 Token에 발송한다.")
  ApiResponse<AdminPushCampaignView> send(
      UUID campaignId, @Parameter(hidden = true) AuthUserPrincipal principal, String key);
}
