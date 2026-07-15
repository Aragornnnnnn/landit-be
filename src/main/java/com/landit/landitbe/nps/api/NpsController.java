// 인증된 사용자의 NPS 제출 요청을 처리하는 Controller다.
package com.landit.landitbe.nps.api;

import com.landit.landitbe.auth.security.AuthUserPrincipal;
import com.landit.landitbe.common.response.ApiResponse;
import com.landit.landitbe.nps.api.dto.NpsSubmitRequest;
import com.landit.landitbe.nps.application.NpsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/v1/nps")
@RestController
@Tag(name = "NPS", description = "서비스 만족도 API")
public class NpsController {

  private final NpsService npsService;

  public NpsController(NpsService npsService) {
    this.npsService = npsService;
  }

  /** 인증된 사용자의 NPS 응답을 저장한다. */
  @Operation(
      summary = "NPS 제출",
      description = "서비스 전반 만족도 점수와 선택 의견을 저장한다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "201",
        description = "제출 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "잘못된 요청"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 실패"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "500",
        description = "서버 오류")
  })
  @PostMapping
  public ResponseEntity<ApiResponse<Void>> submit(
      @AuthenticationPrincipal AuthUserPrincipal principal,
      @Valid @RequestBody NpsSubmitRequest request) {
    npsService.submit(principal.userId(), request.score(), request.opinionText());
    return ApiResponse.success(HttpStatus.CREATED, null);
  }
}
