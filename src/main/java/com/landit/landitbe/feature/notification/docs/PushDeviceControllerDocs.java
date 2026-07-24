// 푸시 알림 설치 상태 동기화 API의 OpenAPI 문서를 정의한다.

package com.landit.landitbe.feature.notification.docs;

import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.feature.notification.dto.PushDeviceSyncRequest;
import com.landit.landitbe.feature.notification.dto.PushDeviceSyncResponse;
import com.landit.landitbe.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

/** 푸시 알림 설치 상태 동기화 API의 OpenAPI 문서를 정의한다. */
@Tag(name = "Push Notification", description = "푸시 알림 설치 상태 API")
public interface PushDeviceControllerDocs {

  /**
   * 현재 앱 설치의 Expo Push Token과 알림 수신 여부를 등록하거나 갱신한다.
   *
   * @param principal 현재 인증 사용자
   * @param installationId 앱 설치 ID
   * @param request 현재 설치 상태
   * @return 동기화된 설치 상태
   */
  @Operation(
      summary = "푸시 알림 설치 상태 동기화",
      description =
          "설치 ID를 기준으로 Expo Push Token과 Landit 푸시 알림 수신 여부를 멱등 동기화합니다. "
              + "pushEnabled가 true이면 Expo Push Token이 필수입니다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "동기화 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "VALIDATION_FAILED: 요청 값이 올바르지 않음"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "INVALID_TOKEN 또는 ACCESS_TOKEN_EXPIRED: 인증 실패"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "500",
        description = "INTERNAL_SERVER_ERROR: 설치 상태 저장 중 서버 오류")
  })
  ResponseEntity<ApiResponse<PushDeviceSyncResponse>> synchronize(
      AuthUserPrincipal principal,
      @Parameter(description = "앱 설치 단위 UUID", required = true) UUID installationId,
      PushDeviceSyncRequest request);
}
