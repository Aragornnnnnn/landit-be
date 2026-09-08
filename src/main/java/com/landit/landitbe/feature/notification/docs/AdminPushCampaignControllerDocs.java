// 관리자 푸시 캠페인 API 계약을 문서화한다.

package com.landit.landitbe.feature.notification.docs;

import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.feature.notification.dto.AdminPushAudiencePreview;
import com.landit.landitbe.feature.notification.dto.AdminPushCampaignRequest;
import com.landit.landitbe.feature.notification.dto.AdminPushCampaignView;
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

  /** 관리자 푸시 캠페인을 생성한다. */
  @Operation(
      summary = "푸시 캠페인 생성",
      description =
          "audienceType은 ALL(기본값) 또는 SELECTED다. SELECTED는 사용자 ID 목록 1~1000개가 필요하며 "
              + "중복 ID는 제거한다. ALL에 ID를 보내거나 존재하지 않는 ID를 보내면 거부한다. 대상 조건은 생성 후 수정할 수 없다.")
  ApiResponse<AdminPushCampaignView> create(
      @Parameter(hidden = true) AuthUserPrincipal principal,
      @Parameter(description = "1~128자 ASCII 영숫자, 대시, 밑줄") String key,
      @Valid AdminPushCampaignRequest request);

  /** 관리자 푸시 캠페인을 최신순으로 조회한다. */
  @Operation(summary = "푸시 캠페인 목록 조회")
  ApiResponse<List<AdminPushCampaignView>> list(int page, int size);

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
