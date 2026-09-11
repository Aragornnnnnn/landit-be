// 단일 구독 공개 정책을 조회하고 저장한다.

package com.landit.landitbe.feature.subscription.repository;

import com.landit.landitbe.feature.subscription.domain.SubscriptionLaunchPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

/** 공개 정책 저장소다. */
public interface SubscriptionLaunchPolicyRepository
    extends JpaRepository<SubscriptionLaunchPolicy, Long> {}
