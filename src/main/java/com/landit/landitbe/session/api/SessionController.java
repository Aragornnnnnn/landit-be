// 기존 학습 세션을 조작하는 API 요청을 처리한다.
package com.landit.landitbe.session.api;

import com.landit.landitbe.auth.security.AuthUserPrincipal;
import com.landit.landitbe.common.response.ApiResponse;
import com.landit.landitbe.session.application.SessionEndUseCase;
import com.landit.landitbe.session.application.SessionMessageSubmitUseCase;
import com.landit.landitbe.session.api.dto.SessionMessageSubmitRequest;
import com.landit.landitbe.session.api.dto.SessionMessageSubmitResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
@RestController
@Tag(name = "Session", description = "학습 세션 API")
public class SessionController {

    private final SessionEndUseCase sessionEndUseCase;
    private final SessionMessageSubmitUseCase sessionMessageSubmitUseCase;

    /** 사용자 발화를 저장하고 다음 AI 메시지를 생성한다. */
    @Operation(
            summary = "사용자 발화 제출",
            description = "사용자 메시지를 저장하고 다음 AI 메시지 또는 종료 메시지를 생성한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "제출 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "세션 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 완료됨"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "AI 생성 실패")
    })
    @PostMapping("/{sessionId}/messages")
    public ResponseEntity<ApiResponse<SessionMessageSubmitResponse>> submitMessage(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable Long sessionId,
            @RequestBody SessionMessageSubmitRequest request
    ) {
        return ApiResponse.success(
                HttpStatus.OK,
                sessionMessageSubmitUseCase.submitMessage(principal.userId(), sessionId, request)
        );
    }

    /** 진행 중인 학습 세션을 사용자가 중도 종료한다. */
    @Operation(
            summary = "세션 중도 종료",
            description = "진행 중인 학습 세션을 INTERRUPTED 상태로 종료한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "종료 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "세션 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 완료됨")
    })
    @PatchMapping("/{sessionId}/end")
    public ApiResponse<Void> endSession(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable Long sessionId
    ) {
        sessionEndUseCase.endSession(principal.userId(), sessionId);
        return ApiResponse.success(null);
    }
}
