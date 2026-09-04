// 구독 상태 갱신 요청이 어떻게 처리됐는지 표현한다.

package com.landit.landitbe.feature.profile.dto;

/** 구독 상태 갱신 요청이 어떻게 처리됐는지 표현한다. */
public enum SubscriptionUpdateResult {
  /** 구독 상태를 갱신했다. */
  APPLIED,
  /** 이미 반영된 이벤트보다 오래된 이벤트라 무시했다. */
  STALE_EVENT,
  /** 사용자 프로필이 없어 갱신하지 못했다. */
  USER_NOT_FOUND
}
