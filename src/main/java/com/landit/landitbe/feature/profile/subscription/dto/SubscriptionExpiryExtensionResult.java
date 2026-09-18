// 결제 유예 종료 시각으로 구독 만료 시각을 늘리는 요청이 어떻게 처리됐는지 표현한다.

package com.landit.landitbe.feature.profile.subscription.dto;

/** 결제 유예 종료 시각으로 구독 만료 시각을 늘리는 요청이 어떻게 처리됐는지 표현한다. */
public enum SubscriptionExpiryExtensionResult {
  /** 구독 만료 시각을 유예 종료 시각으로 늘렸다. */
  EXTENDED,
  /** 프리미엄이 꺼진 사용자라 무시했다. */
  NOT_PREMIUM,
  /** 저장된 만료 시각이 없거나 유예 종료 시각이 그보다 늦지 않아 무시했다. */
  NOT_LATER,
  /** 이미 반영된 구독 이벤트보다 오래된 이벤트라 무시했다. */
  STALE_EVENT,
  /** 사용자 프로필이 없어 갱신하지 못했다. */
  USER_NOT_FOUND
}
