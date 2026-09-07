// Expo가 HTTP 429로 제출을 거부했음을 구분한다.

package com.landit.landitbe.feature.notification.client;

/** 기존 재시도 계약을 유지하면서 공지의 안전한 429 재시도를 구분한다. */
public class PushRateLimitedException extends RetryablePushNotificationException {
  /** 토큰 원문 없는 제한 오류를 생성한다. */
  public PushRateLimitedException() {
    super("Expo 요청이 처리량 제한으로 거부됐습니다.");
  }
}
