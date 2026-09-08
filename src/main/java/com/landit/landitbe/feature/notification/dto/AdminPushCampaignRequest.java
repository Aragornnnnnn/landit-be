// 관리자 푸시 캠페인의 원문 입력을 정규화하고 기본 제약을 정의한다.

package com.landit.landitbe.feature.notification.dto;

import com.landit.landitbe.feature.notification.domain.AdminPushAudienceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 관리자 푸시 캠페인의 원문 입력이다.
 *
 * @param title 알림 제목
 * @param body 알림 본문
 * @param deepLink 앱 내부 경로 또는 외부 HTTPS URL
 * @param audienceType 발송 대상 유형. 생략하면 ALL
 * @param userProfileIds SELECTED 대상 사용자 ID. ALL이면 빈 목록
 */
public record AdminPushCampaignRequest(
    @NotBlank @Size(max = 255) String title,
    @NotBlank @Size(max = 500) String body,
    @NotBlank @Size(max = 1000) String deepLink,
    @Schema(defaultValue = "ALL") AdminPushAudienceType audienceType,
    @Size(max = 1000) List<@NotNull @Positive Long> userProfileIds) {

  /**
   * 입력 문자열의 앞뒤 공백을 제거한다.
   *
   * @param title 알림 제목
   * @param body 알림 본문
   * @param deepLink 이동 경로
   * @param audienceType 발송 대상 유형
   * @param userProfileIds 선택 사용자 목록
   */
  public AdminPushCampaignRequest {
    title = title == null ? null : title.strip();
    body = body == null ? null : body.strip();
    deepLink = deepLink == null ? null : deepLink.strip();
    audienceType = audienceType == null ? AdminPushAudienceType.ALL : audienceType;
    userProfileIds = userProfileIds == null ? List.of() : userProfileIds;
  }

  /**
   * 기존 전체 사용자 발송 입력을 생성한다.
   *
   * @param title 알림 제목
   * @param body 알림 본문
   * @param deepLink 이동 경로
   */
  public AdminPushCampaignRequest(String title, String body, String deepLink) {
    this(title, body, deepLink, AdminPushAudienceType.ALL, List.of());
  }
}
