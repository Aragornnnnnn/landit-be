// 푸시 복습 API의 권한·시간·멱등 제출 계약을 문서화한다.

package com.landit.landitbe.feature.learning.review.docs;

import com.landit.landitbe.feature.learning.review.dto.ReviewAnswerRequest;
import com.landit.landitbe.feature.learning.review.dto.ReviewAnswerResponse;
import com.landit.landitbe.feature.learning.review.dto.ReviewResponse;
import com.landit.landitbe.shared.response.ApiResponse;
import com.landit.landitbe.shared.security.AuthUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;

/** 푸시 복습의 HTTP 계약이다. */
@Tag(name = "Review", description = "학습 완료 표현의 푸시 복습")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "403",
      description = "PREMIUM_REQUIRED: 유료 전환 후 새 시작 권한 없음"),
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "404",
      description = "소유한 복습 없음")
})
public interface ReviewControllerDocs {

  /**
   * 복습 진행 또는 완료 결과를 조회한다.
   *
   * @param principal 로그인 사용자
   * @param reviewId 푸시의 복습 ID
   * @return 시작 전·만료 상태는 빈 문제 목록, 시작 후에는 고정 문제와 진행 상태
   */
  @Operation(
      summary = "복습 상태 조회",
      description =
          "READY는 시작 전, IN_PROGRESS는 진행 중, COMPLETED는 완료, EXPIRED는 만료입니다. 완료 결과는 구독 만료 후에도 조회합니다.")
  ApiResponse<ReviewResponse> get(AuthUserPrincipal principal, UUID reviewId);

  /**
   * 복습을 시작하거나 유효기간 내 진행을 재개한다.
   *
   * @param principal 로그인 사용자
   * @param reviewId 푸시의 복습 ID
   * @return 고정 문제와 진행 상태
   */
  @Operation(
      summary = "복습 시작·재개",
      description =
          "최초 시작에서 유료화 시각과 현재 구독을 검사합니다. 재요청으로 시작·만료 시각이 연장되지 않습니다. 완료된 복습은 완료 상태를 반환합니다.")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "410",
      description = "REVIEW_EXPIRED: 복습 만료")
  ApiResponse<ReviewResponse> start(AuthUserPrincipal principal, UUID reviewId);

  /**
   * 현재 문제의 답안을 제출하고 서버 판정으로 진행 상태를 갱신한다.
   *
   * @param principal 로그인 사용자
   * @param reviewId 복습 ID
   * @param request 제출 ID·현재 문제 ID·선택한 토큰
   * @return 정답 여부와 최신 진행 상태
   */
  @Operation(
      summary = "복습 답안 제출",
      description =
          "currentQuestionId에 제출합니다. 허용 정답 배열과 토큰 값·순서·개수가 일치하면 정답입니다."
              + " 오답은 큐 뒤로 이동하며 마지막 정답에서 자동 완료됩니다. 네트워크 재시도는 같은 submissionId와 내용을 사용합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "요청 검증 실패"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "409",
        description = "시작 전, 문제 순서 또는 제출 키 내용 충돌"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "410",
        description = "REVIEW_EXPIRED: 복습 만료")
  })
  ApiResponse<ReviewAnswerResponse> answer(
      AuthUserPrincipal principal, UUID reviewId, ReviewAnswerRequest request);
}
