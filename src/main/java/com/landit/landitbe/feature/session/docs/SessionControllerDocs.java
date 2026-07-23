// 학습 세션 조작 API의 OpenAPI 문서를 정의한다.

package com.landit.landitbe.feature.session.docs;

import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.feature.session.dto.SessionFeedbackResponse;
import com.landit.landitbe.feature.session.dto.SessionInnerThoughtResponse;
import com.landit.landitbe.feature.session.dto.SessionMessageSubmitRequest;
import com.landit.landitbe.feature.session.dto.SessionMessageSubmitResponse;
import com.landit.landitbe.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

/** 학습 세션 조작 API의 OpenAPI 문서를 정의한다. */
@Tag(name = "Session", description = "학습 세션 API")
public interface SessionControllerDocs {

  /** 사용자 발화를 저장하고 다음 AI 메시지를 생성한다. */
  @Operation(
      summary = "사용자 발화 제출",
      description = "사용자 메시지를 저장하고 다음 AI 메시지 또는 종료 메시지를 생성한다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "제출 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "잘못된 요청"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 실패"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "403",
        description = "권한 없음"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "세션 없음"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "409",
        description = "이미 완료됨"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "503",
        description = "AI 생성 실패")
  })
  ResponseEntity<ApiResponse<SessionMessageSubmitResponse>> submitMessage(
      AuthUserPrincipal principal, Long sessionId, SessionMessageSubmitRequest request);

  /** 사용자 메시지의 상대 역할 속마음 처리 상태를 조회한다. */
  @Operation(
      summary = "사용자 메시지 속마음 조회",
      description = "속마음 생성 상태와 완료된 속마음 결과를 조회한다.",
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
        description = "권한 없음"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "세션 또는 메시지 없음")
  })
  ResponseEntity<ApiResponse<SessionInnerThoughtResponse>> getInnerThought(
      AuthUserPrincipal principal, Long sessionId, Long messageId);

  /** 완료된 세션의 최종 피드백을 생성하거나 저장된 결과를 조회한다. */
  @Operation(
      summary = "대화 최종 피드백 생성 및 조회",
      description = "완료된 세션의 요약 피드백과 메시지별 피드백을 생성하거나 조회한다.",
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
        description = "권한 없음"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "세션 없음"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "409",
        description = "완료되지 않음 또는 피드백 미준비"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "502",
        description = "AI 응답 형식 오류"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "503",
        description = "최종 피드백 생성 실패")
  })
  ResponseEntity<ApiResponse<SessionFeedbackResponse>> getOrCreateFeedback(
      AuthUserPrincipal principal, Long sessionId);

  /** 진행 중인 학습 세션을 사용자가 중도 종료한다. */
  @Operation(
      summary = "세션 중도 종료",
      description = "진행 중인 학습 세션을 INTERRUPTED 상태로 종료한다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "종료 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 실패"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "403",
        description = "권한 없음"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "세션 없음"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "409",
        description = "이미 완료됨")
  })
  ApiResponse<Void> endSession(AuthUserPrincipal principal, Long sessionId);
}
