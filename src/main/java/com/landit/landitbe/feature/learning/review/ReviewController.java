// 인증된 사용자의 푸시 복습 시작·진행·답안 요청을 처리한다.

package com.landit.landitbe.feature.learning.review;

import com.landit.landitbe.feature.learning.review.docs.ReviewControllerDocs;
import com.landit.landitbe.feature.learning.review.dto.ReviewAnswerRequest;
import com.landit.landitbe.feature.learning.review.dto.ReviewAnswerResponse;
import com.landit.landitbe.feature.learning.review.dto.ReviewResponse;
import com.landit.landitbe.feature.learning.review.service.ExpressionReviewService;
import com.landit.landitbe.shared.response.ApiResponse;
import com.landit.landitbe.shared.security.AuthUserPrincipal;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 푸시에 포함된 복습 ID로만 접근하며 생성·목록 API는 제공하지 않는다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reviews")
public class ReviewController implements ReviewControllerDocs {
  private final ExpressionReviewService service;

  /** {@inheritDoc} */
  @Override
  @GetMapping("/{reviewId}")
  public ApiResponse<ReviewResponse> get(
      @AuthenticationPrincipal AuthUserPrincipal principal, @PathVariable UUID reviewId) {
    return ApiResponse.success(service.get(principal.userId(), reviewId));
  }

  /** {@inheritDoc} */
  @Override
  @PostMapping("/{reviewId}/start")
  public ApiResponse<ReviewResponse> start(
      @AuthenticationPrincipal AuthUserPrincipal principal, @PathVariable UUID reviewId) {
    return ApiResponse.success(service.start(principal.userId(), reviewId));
  }

  /** {@inheritDoc} */
  @Override
  @PostMapping("/{reviewId}/answers")
  public ApiResponse<ReviewAnswerResponse> answer(
      @AuthenticationPrincipal AuthUserPrincipal principal,
      @PathVariable UUID reviewId,
      @Valid @RequestBody ReviewAnswerRequest request) {
    return ApiResponse.success(service.answer(principal.userId(), reviewId, request));
  }
}
