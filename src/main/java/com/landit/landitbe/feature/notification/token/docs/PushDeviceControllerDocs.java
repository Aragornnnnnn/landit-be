// 앱 설치별 푸시 동기화 API의 OpenAPI 계약을 정의한다.

package com.landit.landitbe.feature.notification.token.docs;

import com.landit.landitbe.feature.notification.token.dto.PushDeviceUpdateRequest;
import com.landit.landitbe.shared.response.ApiResponse;
import com.landit.landitbe.shared.security.AuthUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;

/** 앱 설치별 푸시 동기화 API의 OpenAPI 계약을 정의한다. */
@Tag(name = "Push Device", description = "앱 설치별 푸시 수신 상태")
public interface PushDeviceControllerDocs {
  /**
   * 현재 인증 계정과 앱 설치의 푸시 수신 상태를 동기화한다.
   *
   * @param principal 인증된 사용자
   * @param installationId 앱 설치별 UUID
   * @param request 현재 설치의 푸시 상태
   * @return 성공 응답
   */
  @Operation(
      summary = "앱 설치 푸시 상태 동기화",
      description =
          "로그인·계정 전환·Token 갱신 시 호출합니다. 활성 등록 시 현재 계정의 설치 ID 없는 다른 활성 Token을"
              + " 비활성화합니다. 아직 전환하지 않은 다른 기기도 새 API로 등록해야 수신을 재개합니다."
              + " 설치 ID가 연결된 다른 기기와 다른 계정의 Token은 유지합니다."
              + " pushEnabled=false이면 해당 설치만 비활성화하고 구형 Token은 정리하지 않습니다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  ApiResponse<Void> update(
      AuthUserPrincipal principal, UUID installationId, @Valid PushDeviceUpdateRequest request);
}
