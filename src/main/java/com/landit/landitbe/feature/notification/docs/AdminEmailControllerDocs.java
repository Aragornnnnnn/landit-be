// 관리자 이메일 테스트와 체험 알림 설정 계약을 문서화한다.

package com.landit.landitbe.feature.notification.docs;

import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.feature.notification.dto.AdminEmailTestRequest;
import com.landit.landitbe.feature.notification.dto.NotificationJobView;
import com.landit.landitbe.feature.notification.dto.TrialReminderSettings;
import com.landit.landitbe.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;

/** 관리자 전용 알림 API 계약이다. */
@Tag(name = "관리자 이메일 및 체험 알림")
@SecurityRequirement(name = "bearerAuth")
public interface AdminEmailControllerDocs {
  /**
   * 고정된 테스트 문구를 입력한 주소로 비동기 발송한다.
   *
   * @param principal 인증 관리자
   * @param key 중복 접수 방지 UUID. 같은 키에 다른 수신 주소는 409
   * @param request 수신 주소
   * @return 접수 작업. 202는 발송 완료가 아니며 SES 샌드박스 제한은 그대로 적용된다
   */
  @Operation(
      summary = "관리자 이메일 테스트 접수",
      description = "수신자 허용 목록 없이 입력 주소로 고정 문구를 발송합니다. SES 접수 여부는 작업 조회 API에서 확인합니다.")
  ApiResponse<NotificationJobView> test(
      AuthUserPrincipal principal, UUID key, AdminEmailTestRequest request);

  /**
   * 이메일 주소를 제외한 작업 상태를 조회한다.
   *
   * @param id 작업 ID
   * @return 현재 처리 상태. ACCEPTED는 SES 접수이고 UNKNOWN은 재발송 전 확인 필요
   */
  @Operation(summary = "알림 작업 상태 조회")
  ApiResponse<NotificationJobView> job(UUID id);

  /**
   * 런타임 체험 알림 설정을 조회한다.
   *
   * @return 기본값은 두 채널 모두 OFF
   */
  @Operation(summary = "무료 체험 알림 채널 설정 조회")
  ApiResponse<TrialReminderSettings> settings();

  /**
   * 발송 직전 적용하는 채널 설정을 변경한다.
   *
   * @param principal 인증 관리자
   * @param settings 푸시와 이메일의 독립 설정
   * @return 저장된 설정. OFF에서 이미 제외된 작업은 ON으로 바꿔도 재발송하지 않는다
   */
  @Operation(
      summary = "무료 체험 알림 채널 설정 변경",
      description = "서버 재시작 없이 발송 직전 설정을 확인합니다. 이미 외부에 접수된 알림은 취소되지 않습니다.")
  ApiResponse<TrialReminderSettings> update(
      AuthUserPrincipal principal, TrialReminderSettings settings);
}
