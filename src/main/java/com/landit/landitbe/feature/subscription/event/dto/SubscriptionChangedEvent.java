// 반영된 구독 변경을 같은 트랜잭션의 후속 처리에 전달한다.

package com.landit.landitbe.feature.subscription.event.dto;

/**
 * 구독 갱신 또는 이전이 반영된 사용자와 결제 환경이다.
 *
 * @param userId 구독 변경이 반영된 사용자 ID
 * @param environment 결제 제공자 환경
 */
public record SubscriptionChangedEvent(long userId, String environment) {}
