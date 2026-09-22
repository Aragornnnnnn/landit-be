// 프리톡 주제 조회와 세션 시작 API의 OpenAPI 문서를 정의한다.

package com.landit.landitbe.feature.learning.freetalk.docs;

import com.landit.landitbe.feature.learning.freetalk.expression.dto.FreeTalkExpressionRetryResponse;
import com.landit.landitbe.feature.learning.freetalk.history.dto.FreeTalkSessionDetailResponse;
import com.landit.landitbe.feature.learning.freetalk.history.dto.FreeTalkSessionListResponse;
import com.landit.landitbe.feature.learning.freetalk.message.dto.FreeTalkExitDecisionRequest;
import com.landit.landitbe.feature.learning.freetalk.message.dto.FreeTalkMessageSubmitRequest;
import com.landit.landitbe.feature.learning.freetalk.message.dto.FreeTalkMessageSubmitResponse;
import com.landit.landitbe.feature.learning.freetalk.start.dto.FreeTalkSessionStartRequest;
import com.landit.landitbe.feature.learning.freetalk.start.dto.FreeTalkSessionStartResponse;
import com.landit.landitbe.feature.learning.freetalk.summary.dto.FreeTalkSessionSummaryResponse;
import com.landit.landitbe.feature.learning.freetalk.topic.dto.FreeTalkMainResponse;
import com.landit.landitbe.shared.response.ApiResponse;
import com.landit.landitbe.shared.security.AuthUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

/** 프리톡 주제 조회, 세션 시작, 발화 제출과 종료 결정 API의 OpenAPI 문서를 정의한다. */
@Tag(name = "Free Talk", description = "프리톡 세션 API")
public interface FreeTalkControllerDocs {

  /**
   * 활성 프리톡 추천 주제를 노출 순서대로 조회한다.
   *
   * @param principal 인증된 사용자
   * @return 활성 추천 주제 목록
   */
  @Operation(
      summary = "프리톡 추천 주제 조회",
      description = "활성 상태의 프리톡 추천 주제를 노출 순서대로 조회한다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 실패")
  })
  ResponseEntity<ApiResponse<FreeTalkMainResponse>> getTopics(AuthUserPrincipal principal);

  /**
   * AI 또는 사용자가 먼저 발화하는 프리톡 세션을 시작한다.
   *
   * @param principal 인증된 사용자
   * @param request 세션 시작 방식과 선택 주제
   * @return 생성된 프리톡 세션
   */
  @Operation(
      summary = "프리톡 세션 시작",
      description =
          "AI 선시작 또는 사용자 선시작 프리톡 세션을 생성한다. "
              + "발화 한도 기본값은 KST 하루 누적 120분이다. "
              + "세션 시작·발화·종료 결정·표현 재시도는 계정별 요청 한도를 공유한다 "
              + "(기본 일일 1,000회, 고정 1분 구간당 20회).",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "201",
        description = "시작 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "요청 오류"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 실패"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "403",
        description = "프리미엄 구독 필요 (PREMIUM_REQUIRED)"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "주제 없음"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "409",
        description = "일일 발화 한도 초과 (FREE_TALK_DAILY_SPEAKING_LIMIT_EXCEEDED)"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "502",
        description = "AI 응답 오류"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "429",
        description =
            "일일 요청 한도 초과 (FREE_TALK_DAILY_REQUEST_LIMIT_EXCEEDED) 또는 "
                + "분당 요청 한도 초과 (FREE_TALK_REQUEST_RATE_LIMIT_EXCEEDED)"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "503",
        description = "AI 생성 실패")
  })
  ResponseEntity<ApiResponse<FreeTalkSessionStartResponse>> startSession(
      AuthUserPrincipal principal, FreeTalkSessionStartRequest request);

  /**
   * 사용자 발화를 제출해 AI 후속 메시지 또는 종료 확인 상태를 받는다.
   *
   * @param principal 인증된 사용자
   * @param sessionId 프리톡 학습 세션 ID
   * @param request 사용자 발화 제출 요청
   * @return 발화 처리 결과
   */
  @Operation(
      summary = "프리톡 발화 제출",
      description =
          "사용자 발화를 저장하고 AI 후속 메시지, 종료 확인 또는 시간 제한 종료를 반환한다. "
              + "0ms 발화도 요청 한도에 포함하며, 저장된 응답을 반환하는 재전송은 추가 차감하지 않는다. "
              + "AI 호출 실패 시 발화 시간은 반환하지만 요청 횟수는 유지한다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "처리 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 실패"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "403",
        description = "세션 소유자 아님 또는 프리미엄 구독 필요 (PREMIUM_REQUIRED)"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "세션 없음"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "409",
        description = "중복·처리 중인 발화 또는 일일 발화 한도 초과 (FREE_TALK_DAILY_SPEAKING_LIMIT_EXCEEDED)"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "429",
        description =
            "일일 요청 한도 초과 (FREE_TALK_DAILY_REQUEST_LIMIT_EXCEEDED) 또는 "
                + "분당 요청 한도 초과 (FREE_TALK_REQUEST_RATE_LIMIT_EXCEEDED)"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "503",
        description = "AI 생성 실패")
  })
  ResponseEntity<ApiResponse<FreeTalkMessageSubmitResponse>> submitMessage(
      AuthUserPrincipal principal, long sessionId, FreeTalkMessageSubmitRequest request);

  /**
   * 종료 의사 확인 창에서 사용자가 선택한 결과를 처리한다.
   *
   * @param principal 인증된 사용자
   * @param sessionId 프리톡 학습 세션 ID
   * @param request 종료 또는 계속 대화 결정 요청
   * @return 종료 결정 처리 결과
   */
  @Operation(
      summary = "프리톡 종료 의사 결정",
      description = "종료를 확정하면 마무리 메시지와 함께 세션을 완료하고, 취소하면 대화를 계속한다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "처리 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 실패"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "403",
        description = "세션 소유자 아님 또는 프리미엄 구독 필요 (PREMIUM_REQUIRED)"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "세션 없음"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "409",
        description = "종료 확인 상태 불일치"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "429",
        description =
            "일일 요청 한도 초과 (FREE_TALK_DAILY_REQUEST_LIMIT_EXCEEDED) 또는 "
                + "분당 요청 한도 초과 (FREE_TALK_REQUEST_RATE_LIMIT_EXCEEDED)"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "503",
        description = "AI 생성 실패")
  })
  ResponseEntity<ApiResponse<FreeTalkMessageSubmitResponse>> decideExit(
      AuthUserPrincipal principal, long sessionId, FreeTalkExitDecisionRequest request);

  /**
   * 완료된 지난 프리톡 목록을 완료 시각 최신순으로 페이지 조회한다.
   *
   * @param principal 인증된 사용자
   * @param page 0부터 시작하는 페이지 번호
   * @param size 한 페이지에 조회할 프리톡 수 (1~50)
   * @return 완료된 프리톡 목록과 페이지 정보
   */
  @Operation(
      summary = "지난 프리톡 목록 조회",
      description = "인증된 사용자의 완료된 프리톡을 완료 시각 최신순으로 페이지 조회한다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "페이지 번호 또는 크기 오류"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 실패")
  })
  ResponseEntity<ApiResponse<FreeTalkSessionListResponse>> getSessions(
      AuthUserPrincipal principal,
      @Parameter(description = "0부터 시작하는 페이지 번호", example = "0") int page,
      @Parameter(description = "페이지 크기 (1~50)", example = "20") int size);

  /**
   * 완료된 지난 프리톡의 세션 정보와 전체 대화를 조회한다.
   *
   * @param principal 인증된 사용자
   * @param sessionId 조회할 프리톡 학습 세션 ID
   * @return 프리톡 세션 정보와 전체 대화 메시지
   */
  @Operation(
      summary = "지난 프리톡 상세 조회",
      description =
          "인증된 사용자가 완료한 프리톡의 세션 정보와 전체 대화를 조회한다. 사용자 메시지에는 턴 교정(correction)과"
              + " 교정 처리 상태(correctionStatus)가 함께 내려가고, correctionCount는 교정이 있는 사용자 메시지"
              + " 수다. correction이 null이고 correctionStatus가 COMPLETED면 고칠 것이 없는 턴, PREPARING이면"
              + " 생성 중이라 재조회가 필요한 턴, FAILED면 교정을 만들지 못한 턴이다. 생성 중인 교정은 서버가 스스로"
              + " 끝낸다. AI가 판정을 돌려주지 못했거나 서버가 재시작되면 최대 3회까지 다시 시도하고, 그래도 만들지 못하면"
              + " FAILED로 확정하므로 PREPARING이 끝없이 남지 않는다. 한 번 COMPLETED나 FAILED가 된 교정은 다시 바뀌지"
              + " 않는다. AI 메시지는 세 필드가 모두"
              + " null이다. correction.memoryTag는 장기기억을 근거로 한 교정에만 \"9/13 스몰톡에서 말한 헬스장\""
              + " 형식(한국어 고정)으로 내려주고, 라벨을 만들지 못했으면 \"9/13 스몰톡에서 말한 내용\"으로 채운다."
              + " 기억을 근거로 쓰지 않은 교정은 null이다. 태그는 교정과 함께 저장한 값이라 그 기억이 나중에 바뀌어도"
              + " 달라지지 않는다. reusedExpression은 사용자가 이전에 학습을 마친 표현을 그 메시지에서 다시 썼을 때만"
              + " 내려준다. matchedText는 content 안에 대소문자까지 그대로 들어 있는 구절이라 그 위치에 밑줄을 그으면"
              + " 된다. 한 메시지에서 여러 표현을 썼어도 하나만 내려주고, 다시 쓴 표현이 없거나 AI 메시지면 null이다. 세션 종료 후"
              + " expressionGenerationStatus가 PREPARING인 동안은 아직 판정 전이라 null일 수 있다. 교정은"
              + " 진행 중인 대화의 응답에는 포함되지 않는다. 구독이 만료된 사용자도 본인이 완료한 세션은 조회할 수 있다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 실패"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "403",
        description = "세션 소유자 아님"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "완료된 프리톡 세션 없음")
  })
  ResponseEntity<ApiResponse<FreeTalkSessionDetailResponse>> getSession(
      AuthUserPrincipal principal,
      @Parameter(description = "조회할 프리톡 학습 세션 ID", example = "123") long sessionId);

  /**
   * 완료된 프리톡의 종료 후 요약을 조회한다.
   *
   * @param principal 인증된 사용자
   * @param sessionId 프리톡 학습 세션 ID
   * @return 오늘의 스몰톡 요약
   */
  @Operation(
      summary = "오늘의 스몰톡 요약 조회",
      description =
          "완료된 스몰톡의 종료 후 요약(S7b)을 반환한다. 점수·별점은 없다. 총평(headline·comparison·growth·correctionCount)은"
              + " 이 세션의 턴 교정이 모두 끝난 뒤 한 번 계산해 저장하고, 그 뒤에는 조회할 때마다 같은 값을 돌려준다. 계산 전이면 pending이"
              + " true이고 총평 필드는 모두 null이다. 교정이 끝나기를 세션 종료 후 30초까지 기다리고, 그 뒤에는 끝나지 않은 교정을 빼고 확정하므로"
              + " pending이 끝없이 남지 않는다. 그렇게 빠진 교정이 나중에 끝나면 지난 프리톡 상세에는 보이지만 이 요약의"
              + " correctionCount·growth에는 반영되지 않는다. 그렇게 교정을 빼고 확정할 때는 남은 교정이 같은 패턴일 수 있으므로 growth의"
              + " \"오늘은 맞게 썼다\"(succeeded true)는 주장하지 않고, 또 틀린 근거가 있을 때만 growth를 둔다. headline과"
              + " comparison은 확정 뒤 항상 있고(첫 스몰톡은 comparison.previous가 모두 0), growth는 직전 스몰톡에서 교정받은"
              + " 패턴(많이 틀린 순 최대 3개) 중 하나가 이번에 다시 나왔을 때만 있다. growth의"
              + " 구절(previousWrongSpan·currentSpan)은 각 문장에 대소문자까지 그대로 정확히 한 번 들어 있으며 특정하지 못했으면"
              + " null이다. reusedExpressions와 followUp은 종료 후 비동기 작업의 결과라 각자 pending을 가진다. 표현 작업이 실패로"
              + " 끝나면 reusedExpressions는 pending 없이 빈 목록이다. 장기기억 작업이 시작된 지(워커가 아직 집지 않았으면 세션 종료 후)"
              + " 5분이 지나도 끝나지 않으면 서버가 실패로 확정하므로 followUp.pending도 끝없이 남지 않고, 그때 followUp의 질문 세 필드는"
              + " null이다. 미완료 세션은 409(SESSION_NOT_COMPLETED)다(지난 프리톡 상세 조회는 같은 경우 404를 준다). 구독이 만료된"
              + " 사용자도 본인이 완료한 세션은 조회할 수 있다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 실패"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "403",
        description = "세션 소유자 아님"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "프리톡 세션 없음"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "409",
        description = "완료되지 않은 세션 (SESSION_NOT_COMPLETED)")
  })
  ResponseEntity<ApiResponse<FreeTalkSessionSummaryResponse>> getSummary(
      AuthUserPrincipal principal,
      @Parameter(description = "조회할 프리톡 학습 세션 ID", example = "123") long sessionId);

  /**
   * 실패한 맞춤 표현 생성 작업을 다시 시작한다.
   *
   * @param principal 인증된 사용자
   * @param sessionId 프리톡 학습 세션 ID
   * @return 표현 생성 재시도 요청 결과
   */
  @Operation(summary = "맞춤 표현 생성 재시도", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "202",
        description = "재시도 요청 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 실패"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "403",
        description = "세션 소유자 아님 또는 프리미엄 구독 필요 (PREMIUM_REQUIRED)"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "세션 없음"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "409",
        description = "재시도할 수 없는 세션 상태"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "429",
        description =
            "일일 요청 한도 초과 (FREE_TALK_DAILY_REQUEST_LIMIT_EXCEEDED) 또는 "
                + "분당 요청 한도 초과 (FREE_TALK_REQUEST_RATE_LIMIT_EXCEEDED)")
  })
  ResponseEntity<ApiResponse<FreeTalkExpressionRetryResponse>> retryExpressions(
      AuthUserPrincipal principal, long sessionId);
}
