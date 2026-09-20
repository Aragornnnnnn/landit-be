// 관리자 발음 평가 자산 임포트 요청을 처리한다.

package com.landit.landitbe.feature.content.expression.pronunciation.admin;

import com.landit.landitbe.feature.content.expression.pronunciation.admin.docs.AdminPronunciationAssetControllerDocs;
import com.landit.landitbe.feature.content.expression.pronunciation.admin.dto.AdminPronunciationAssetCoverageResponse;
import com.landit.landitbe.feature.content.expression.pronunciation.admin.dto.AdminPronunciationAssetImportResult;
import com.landit.landitbe.feature.content.expression.pronunciation.admin.service.ExpressionPronunciationAssetService;
import com.landit.landitbe.shared.response.ApiResponse;
import com.landit.landitbe.shared.security.AuthUserPrincipal;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 발음 평가 자산 임포트 요청을 처리한다. */
@RequiredArgsConstructor
@RestController
@Validated
public class AdminPronunciationAssetController implements AdminPronunciationAssetControllerDocs {

  private final ExpressionPronunciationAssetService expressionPronunciationAssetService;

  /** {@inheritDoc} */
  @Override
  @PostMapping("/api/v1/admin/expressions/pronunciation-assets/import-reference-from-s3")
  public ApiResponse<AdminPronunciationAssetImportResult> importReference(
      @AuthenticationPrincipal AuthUserPrincipal principal,
      @RequestParam @NotBlank String manifestKey) {
    return ApiResponse.success(
        expressionPronunciationAssetService.importReference(principal.userId(), manifestKey));
  }

  /** {@inheritDoc} */
  @Override
  @PostMapping("/api/v1/admin/expressions/pronunciation-assets/import-tts-from-s3")
  public ApiResponse<AdminPronunciationAssetImportResult> importTts(
      @AuthenticationPrincipal AuthUserPrincipal principal,
      @RequestParam @NotBlank String manifestKey) {
    return ApiResponse.success(
        expressionPronunciationAssetService.importTts(principal.userId(), manifestKey));
  }

  /** {@inheritDoc} */
  @Override
  @GetMapping("/api/v1/admin/expressions/pronunciation-assets/coverage")
  public ApiResponse<AdminPronunciationAssetCoverageResponse> coverage() {
    return ApiResponse.success(expressionPronunciationAssetService.coverage());
  }
}
