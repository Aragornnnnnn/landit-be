// 관리자 푸시 캠페인의 생성, 테스트와 비동기 전체 발송 요청을 처리한다.

package com.landit.landitbe.feature.notification;

import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.feature.notification.docs.AdminPushCampaignControllerDocs;
import com.landit.landitbe.feature.notification.dto.AdminPushAudiencePreview;
import com.landit.landitbe.feature.notification.dto.AdminPushCampaignRequest;
import com.landit.landitbe.feature.notification.dto.AdminPushCampaignView;
import com.landit.landitbe.feature.notification.dto.AdminPushRunView;
import com.landit.landitbe.feature.notification.service.AdminPushCampaignService;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
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

/** 관리자 푸시 캠페인 API를 기존 관리자 권한 필터 아래에서 제공한다. */
@RestController
@RequestMapping("/api/v1/admin/push-campaigns")
@RequiredArgsConstructor
public class AdminPushCampaignController implements AdminPushCampaignControllerDocs {

  private final AdminPushCampaignService campaignService;

  /** {@inheritDoc} */
  @Override
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<AdminPushCampaignView> create(
      @AuthenticationPrincipal AuthUserPrincipal principal,
      @RequestHeader("Idempotency-Key") String key,
      @Valid @RequestBody AdminPushCampaignRequest request) {
    return ApiResponse.success(campaignService.create(principal.userId(), key, request));
  }

  /** {@inheritDoc} */
  @Override
  @GetMapping
  public ApiResponse<List<AdminPushCampaignView>> list(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    if (page < 0 || size < 1 || size > 50) {
      throw new ApiException(ErrorCode.INVALID_REQUEST);
    }
    return ApiResponse.success(campaignService.list(page, size));
  }

  /** {@inheritDoc} */
  @Override
  @GetMapping("/{campaignId}")
  public ApiResponse<AdminPushCampaignView> detail(@PathVariable UUID campaignId) {
    return ApiResponse.success(campaignService.detail(campaignId));
  }

  /** {@inheritDoc} */
  @Override
  @GetMapping("/{campaignId}/audience-preview")
  public ApiResponse<AdminPushAudiencePreview> preview(@PathVariable UUID campaignId) {
    return ApiResponse.success(campaignService.preview(campaignId));
  }

  /** {@inheritDoc} */
  @Override
  @PostMapping("/{campaignId}/test-runs")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public ApiResponse<AdminPushRunView> test(
      @PathVariable UUID campaignId,
      @AuthenticationPrincipal AuthUserPrincipal principal,
      @RequestHeader("Idempotency-Key") String key) {
    return ApiResponse.success(campaignService.start(campaignId, principal.userId(), key, true));
  }

  /** {@inheritDoc} */
  @Override
  @PostMapping("/{campaignId}/send")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public ApiResponse<AdminPushRunView> send(
      @PathVariable UUID campaignId,
      @AuthenticationPrincipal AuthUserPrincipal principal,
      @RequestHeader("Idempotency-Key") String key) {
    return ApiResponse.success(campaignService.start(campaignId, principal.userId(), key, false));
  }

  /** {@inheritDoc} */
  @Override
  @GetMapping("/{campaignId}/runs/{runId}")
  public ApiResponse<AdminPushRunView> run(
      @PathVariable UUID campaignId, @PathVariable UUID runId) {
    return ApiResponse.success(campaignService.run(campaignId, runId));
  }

  /** {@inheritDoc} */
  @Override
  @PostMapping("/{campaignId}/runs/{runId}/resume")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public ApiResponse<AdminPushRunView> resume(
      @PathVariable UUID campaignId,
      @PathVariable UUID runId,
      @AuthenticationPrincipal AuthUserPrincipal principal) {
    return ApiResponse.success(campaignService.resume(campaignId, runId, principal.userId()));
  }
}
