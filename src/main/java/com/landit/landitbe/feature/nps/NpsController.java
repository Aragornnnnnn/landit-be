// 인증된 사용자의 NPS 제출 요청을 처리하는 Controller다.

package com.landit.landitbe.feature.nps;

import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.feature.nps.docs.NpsControllerDocs;
import com.landit.landitbe.feature.nps.dto.NpsSubmitRequest;
import com.landit.landitbe.feature.nps.service.NpsService;
import com.landit.landitbe.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 인증된 사용자의 NPS 제출 요청을 처리하는 Controller다. */
@RestController
public class NpsController implements NpsControllerDocs {

  private final NpsService npsService;

  /**
   * NPS 제출 흐름을 처리할 Service를 주입받는다.
   *
   * @param npsService NPS Service
   */
  public NpsController(NpsService npsService) {
    this.npsService = npsService;
  }

  /** 인증된 사용자의 NPS 응답을 저장한다. */
  @Override
  @PostMapping("/api/v1/nps")
  public ResponseEntity<ApiResponse<Void>> submit(
      @AuthenticationPrincipal AuthUserPrincipal principal,
      @Valid @RequestBody NpsSubmitRequest request) {
    npsService.submit(principal.userId(), request);
    return ApiResponse.success(HttpStatus.CREATED, null);
  }
}
