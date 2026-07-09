// 기존 학습 세션을 조작하는 API 요청을 처리한다.
package com.landit.landitbe.session.api;

import com.landit.landitbe.auth.security.AuthUserPrincipal;
import com.landit.landitbe.common.response.ApiResponse;
import com.landit.landitbe.session.application.SessionEndUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
@RestController
@Tag(name = "Session", description = "학습 세션 API")
public class SessionController {

    private final SessionEndUseCase sessionEndUseCase;

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
