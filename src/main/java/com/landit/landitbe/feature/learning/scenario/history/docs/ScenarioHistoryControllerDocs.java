// 시나리오 히스토리 조회의 인증과 정렬·피드백 반환 계약을 문서화한다.

package com.landit.landitbe.feature.learning.scenario.history.docs;

import com.landit.landitbe.feature.learning.scenario.history.dto.ScenarioHistoryResponse;
import com.landit.landitbe.shared.response.ApiResponse;
import com.landit.landitbe.shared.security.AuthUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/** 시나리오 히스토리 조회의 OpenAPI 문서다. */
@Tag(name = "Scenario", description = "시나리오 API")
public interface ScenarioHistoryControllerDocs {

  /**
   * 로그인한 사용자의 시나리오 전체 완료 회차를 조회한다.
   *
   * @param principal 인증 사용자 정보
   * @param scenarioId 시나리오 ID
   * @return 완료 회차별 대화와 저장된 피드백
   */
  @Operation(
      summary = "시나리오 전체 완료 회차 히스토리 조회",
      description =
          "사용자 ID는 로그인 토큰에서 식별한다. 완료한 모든 회차를 endedAt 내림차순, 동률이면 sessionId 내림차순으로 반환한다."
              + " 진행 중·중도 종료 회차는 제외하며, 비활성 콘텐츠의 과거 기록도 조회한다."
              + " 각 회차의 messages는 messageSequence 오름차순이다."
              + " 기록이 없거나 존재하지 않는 시나리오 ID이면 sessions는 빈 배열이다."
              + " 저장된 완료 피드백이 없으면 feedback은 null이며 AI 생성은 실행하지 않는다."
              + " 기존 정책으로 잠긴 상세 피드백은 messageFeedbacks가 빈 배열이고 detailFeedbackLocked가 true다."
              + " 과거 평가 문맥을 복원할 메시지나 시작 안내가 없으면 evaluationContext는 null이다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "시나리오 ID 형식 오류"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 실패")
  })
  ApiResponse<ScenarioHistoryResponse> getHistory(
      @Parameter(hidden = true) AuthUserPrincipal principal,
      @Parameter(description = "조회할 시나리오 ID", example = "1") Long scenarioId);
}
