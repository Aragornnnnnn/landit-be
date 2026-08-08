// 관리자 NPS 목록 요청을 처리한다.

package com.landit.landitbe.feature.nps;

import com.landit.landitbe.feature.nps.docs.AdminNpsControllerDocs;
import com.landit.landitbe.feature.nps.dto.AdminNpsResponsePage;
import com.landit.landitbe.feature.nps.service.NpsService;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import com.landit.landitbe.shared.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 NPS 목록 요청을 처리한다. */
@RequiredArgsConstructor
@RestController
public class AdminNpsController implements AdminNpsControllerDocs {

  private final NpsService npsService;

  /** {@inheritDoc} */
  @Override
  @GetMapping("/api/v1/admin/nps-responses")
  public ApiResponse<AdminNpsResponsePage> list(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    if (page < 0 || size < 1 || size > 50) {
      throw new ApiException(ErrorCode.INVALID_REQUEST);
    }

    return ApiResponse.success(npsService.getAdminResponses(page, size));
  }
}
