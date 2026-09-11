// 프로필의 구독 상태와 학습 진행도를 합쳐 앱이 쓸 구독 조회 응답을 만든다.

package com.landit.landitbe.feature.subscription.service;

import com.landit.landitbe.config.subscription.SubscriptionProperties;
import com.landit.landitbe.feature.learning.service.LearningProgressService;
import com.landit.landitbe.feature.profile.dto.UserSubscriptionSnapshot;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import com.landit.landitbe.feature.subscription.dto.UserSubscriptionResponse;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 프로필의 구독 상태와 학습 진행도를 합쳐 앱이 쓸 구독 조회 응답을 만든다. */
@Slf4j
@Service
public class UserSubscriptionService {

  private final UserProfileService userProfileService;
  private final LearningProgressService learningProgressService;
  private final Clock clock;
  private final Optional<LocalDateTime> launchedAt;

  /**
   * 협력 Service와 도입 시점 설정을 주입받고, 도입 시점이 비어 있으면 경고를 남긴다.
   *
   * @param userProfileService 구독 상태 스냅샷을 제공하는 프로필 Service
   * @param learningProgressService 시나리오 완료 이력을 제공하는 학습 진행 Service
   * @param subscriptionProperties 유료 구독 도입 시점 설정
   * @param clock 서비스 기준 시간대를 제공하는 시계
   */
  public UserSubscriptionService(
      UserProfileService userProfileService,
      LearningProgressService learningProgressService,
      SubscriptionProperties subscriptionProperties,
      Clock clock) {
    this.userProfileService = userProfileService;
    this.learningProgressService = learningProgressService;
    this.clock = clock;
    this.launchedAt =
        subscriptionProperties
            .launchedAtOrEmpty()
            .map(value -> value.atZoneSameInstant(clock.getZone()).toLocalDateTime());
    if (launchedAt.isEmpty()) {
      log.warn(
          "LANDIT_SUBSCRIPTION_LAUNCHED_AT이 설정되지 않아 conversationCompletedSinceLaunch는 항상 false로"
              + " 응답한다.");
    }
  }

  /**
   * 활성 사용자의 구독 상태와 도입 이후 대화 완료 여부를 조회한다.
   *
   * <p>대화 완료 여부는 유료 구독 도입 시점 이후에 시나리오를 끝까지 완료(CLEARED)한 이력이 있는지로 판단한다. 도입 시점이 설정되지 않았으면 항상 false다.
   *
   * @param userId 조회할 사용자 ID
   * @return 사용자 구독 상태 응답
   * @throws com.landit.landitbe.feature.profile.exception.UserProfileException 활성 프로필이 없을 때
   */
  @Transactional(readOnly = true)
  public UserSubscriptionResponse getSubscription(Long userId) {
    UserSubscriptionSnapshot snapshot = userProfileService.getSubscription(userId);
    boolean conversationCompletedSinceLaunch =
        launchedAt
            .map(since -> learningProgressService.hasClearedScenarioSince(userId, since))
            .orElse(false);
    return UserSubscriptionResponse.of(snapshot, conversationCompletedSinceLaunch);
  }
}
