// 관리자 푸시 캠페인의 생성, 테스트와 전체 발송 요청을 처리한다.

package com.landit.landitbe.feature.notification;

import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.feature.notification.docs.AdminPushCampaignControllerDocs;
import com.landit.landitbe.feature.notification.dto.AdminPushAudiencePreview;
import com.landit.landitbe.feature.notification.dto.AdminPushAudienceQueryRequest;
import com.landit.landitbe.feature.notification.dto.AdminPushCampaignRequest;
import com.landit.landitbe.feature.notification.dto.AdminPushCampaignView;
import com.landit.landitbe.feature.notification.dto.AdminPushScheduleRequest;
import com.landit.landitbe.feature.notification.service.AdminPushCampaignService;
import com.landit.landitbe.shared.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 권한 필터 아래에서 일괄 푸시 API를 제공한다. */
@RestController
@RequestMapping("/api/v1/admin/push-campaigns")
@RequiredArgsConstructor
public class AdminPushCampaignController implements AdminPushCampaignControllerDocs {

  private final AdminPushCampaignService campaigns;

  /** {@inheritDoc} */
  @Override
  @PostMapping("/audience-query")
  public ApiResponse<List<Long>> queryAudience(
      @AuthenticationPrincipal AuthUserPrincipal principal,
      @Valid @RequestBody AdminPushAudienceQueryRequest request) {
    return ApiResponse.success(campaigns.queryAudience(principal.userId(), request.sql()));
  }

  /** {@inheritDoc} */
  @Override
  @PostMapping("/{campaignId}/schedule")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public ApiResponse<AdminPushCampaignView> schedule(
      @PathVariable UUID campaignId,
      @AuthenticationPrincipal AuthUserPrincipal principal,
      @RequestHeader("Idempotency-Key") String key,
      @Valid @RequestBody AdminPushScheduleRequest request) {
    return ApiResponse.success(
        campaigns.schedule(campaignId, principal.userId(), key, request.scheduledAt()));
  }

  /** {@inheritDoc} */
  @Override
  @PostMapping("/{campaignId}/cancel-schedule")
  public ApiResponse<AdminPushCampaignView> cancelSchedule(
      @PathVariable UUID campaignId, @AuthenticationPrincipal AuthUserPrincipal principal) {
    return ApiResponse.success(campaigns.cancelSchedule(campaignId, principal.userId()));
  }

  /** {@inheritDoc} */
  @Override
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<AdminPushCampaignView> create(
      @AuthenticationPrincipal AuthUserPrincipal principal,
      @RequestHeader("Idempotency-Key") String key,
      @Valid @RequestBody AdminPushCampaignRequest request) {
    return ApiResponse.success(campaigns.create(principal.userId(), key, request));
  }

  /** {@inheritDoc} */
  @Override
  @GetMapping
  public ApiResponse<List<AdminPushCampaignView>> list(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return ApiResponse.success(campaigns.list(page, size));
  }

  /** {@inheritDoc} */
  @Override
  @GetMapping("/{campaignId}")
  public ApiResponse<AdminPushCampaignView> detail(@PathVariable UUID campaignId) {
    return ApiResponse.success(campaigns.detail(campaignId));
  }

  /** {@inheritDoc} */
  @Override
  @GetMapping("/{campaignId}/audience-preview")
  public ApiResponse<AdminPushAudiencePreview> preview(@PathVariable UUID campaignId) {
    return ApiResponse.success(campaigns.preview(campaignId));
  }

  /** {@inheritDoc} */
  @Override
  @PostMapping("/{campaignId}/test")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public ApiResponse<AdminPushCampaignView> test(
      @PathVariable UUID campaignId,
      @AuthenticationPrincipal AuthUserPrincipal principal,
      @RequestHeader("Idempotency-Key") String key) {
    return ApiResponse.success(campaigns.test(campaignId, principal.userId(), key));
  }

  /** {@inheritDoc} */
  @Override
  @PostMapping("/{campaignId}/send")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public ApiResponse<AdminPushCampaignView> send(
      @PathVariable UUID campaignId,
      @AuthenticationPrincipal AuthUserPrincipal principal,
      @RequestHeader("Idempotency-Key") String key) {
    return ApiResponse.success(campaigns.send(campaignId, principal.userId(), key));
  }
}
