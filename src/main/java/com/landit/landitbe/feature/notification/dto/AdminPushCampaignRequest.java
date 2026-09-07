// 관리자 푸시 캠페인의 원문 입력을 정규화하고 기본 제약을 정의한다.

package com.landit.landitbe.feature.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 관리자 푸시 캠페인의 원문 입력이다.
 *
 * @param title 알림 제목
 * @param body 알림 본문
 * @param deepLink 앱 내부 경로 또는 외부 HTTPS URL
 */
public record AdminPushCampaignRequest(
    @NotBlank @Size(max = 255) String title,
    @NotBlank @Size(max = 500) String body,
    @NotBlank @Size(max = 1000) String deepLink) {

  /**
   * 입력 문자열의 앞뒤 공백을 제거한다.
   *
   * @param title 알림 제목
   * @param body 알림 본문
   * @param deepLink 이동 경로
   */
  public AdminPushCampaignRequest {
    title = title == null ? null : title.strip();
    body = body == null ? null : body.strip();
    deepLink = deepLink == null ? null : deepLink.strip();
  }
}
