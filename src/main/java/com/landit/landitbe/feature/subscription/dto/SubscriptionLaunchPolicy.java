// 업무 간에 전달할 SubscriptionLaunchPolicy 값을 정의한다.

package com.landit.landitbe.feature.subscription.dto;

import java.time.LocalDateTime;

/** 환경변수로 읽은 공개 정책이며 별도 DB 스위치와 신규 시작 중지를 제공하지 않는다. */
public record SubscriptionLaunchPolicy(
    long version, LocalDateTime effectiveAt, boolean newStartsPaused) {}
