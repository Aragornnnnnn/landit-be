// 예약과 발송 서비스 사이에서 불변 알림 작업을 전달한다.

package com.landit.landitbe.feature.notification.job.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * DB에 저장된 채널별 발송 작업이다.
 *
 * @param id 멱등 식별자
 * @param kind TRIAL_PUSH, TRIAL_EMAIL, TEST_EMAIL 중 하나
 * @param userProfileId 대상 사용자 또는 테스트 요청 관리자
 * @param productId 구독 상품 ID
 * @param store 스토어 이름
 * @param environment RevenueCat 환경
 * @param expiresAt 체험 종료 시각
 * @param scheduledAt 예정 시각
 * @param recipient 관리자 테스트에서 지정한 이메일
 * @param status 처리 상태
 * @param claimedAt 처리 시작 시각
 * @param claimToken 처리 소유권
 * @param resultCode 처리 결과 코드
 * @param providerMessageId SES 접수 식별자
 */
public record NotificationJob(
    UUID id,
    String kind,
    long userProfileId,
    String productId,
    String store,
    String environment,
    Instant expiresAt,
    Instant scheduledAt,
    String recipient,
    String status,
    Instant claimedAt,
    UUID claimToken,
    String resultCode,
    String providerMessageId) {
  /**
   * 이메일 주소를 제외한 관리자 응답으로 변환한다.
   *
   * @return 발송 작업 응답
   */
  public NotificationJobView view() {
    return new NotificationJobView(id, status, resultCode, providerMessageId);
  }

  /**
   * 체험 종료 알림인지 반환한다.
   *
   * @return 관리자 테스트가 아니면 true
   */
  public boolean trial() {
    return !kind.equals("TEST_EMAIL");
  }
}
