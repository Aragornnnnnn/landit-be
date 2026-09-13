// 프로필에 저장된 구독 상태의 변경·이전과 조회를 처리한다.

package com.landit.landitbe.feature.profile.subscription.service;

import com.landit.landitbe.feature.profile.domain.UserProfile;
import com.landit.landitbe.feature.profile.domain.UserProfileStatus;
import com.landit.landitbe.feature.profile.exception.UserProfileErrorCode;
import com.landit.landitbe.feature.profile.exception.UserProfileException;
import com.landit.landitbe.feature.profile.repository.UserProfileRepository;
import com.landit.landitbe.feature.profile.subscription.domain.SubscriptionStatus;
import com.landit.landitbe.feature.profile.subscription.dto.SubscriptionTransferResult;
import com.landit.landitbe.feature.profile.subscription.dto.SubscriptionUpdateCommand;
import com.landit.landitbe.feature.profile.subscription.dto.SubscriptionUpdateResult;
import com.landit.landitbe.feature.profile.subscription.dto.UserSubscriptionSnapshot;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 프로필에 저장된 구독 상태의 변경·이전과 조회를 처리한다. */
@Service
@RequiredArgsConstructor
public class ProfileSubscriptionService {
  private final UserProfileRepository userProfileRepository;

  private UserProfile requireActiveEntity(Long userId) {
    return userProfileRepository
        .findByIdAndStatus(userId, UserProfileStatus.ACTIVE)
        .orElseThrow(() -> new UserProfileException(UserProfileErrorCode.INVALID_TOKEN));
  }

  /**
   * 결제 제공자 이벤트로 사용자 구독 상태를 갱신한다.
   *
   * <p>탈퇴한 사용자도 대상에 포함해 환불·만료 이벤트가 유실되지 않게 한다. 이미 반영한 이벤트보다 오래된 이벤트는 무시한다. 같은 사용자의 웹훅이 동시에 들어와도 오래된
   * 이벤트가 최신 상태를 덮어쓰지 않도록 쓰기 잠금으로 조회한다.
   *
   * @param userId 갱신할 사용자 ID
   * @param command 갱신할 구독 정보
   * @return 갱신 처리 결과
   */
  @Transactional
  public SubscriptionUpdateResult updateSubscription(
      Long userId, SubscriptionUpdateCommand command) {
    Optional<UserProfile> found = userProfileRepository.findByIdForUpdate(userId);
    if (found.isEmpty()) {
      return SubscriptionUpdateResult.USER_NOT_FOUND;
    }
    UserProfile userProfile = found.get();
    if (userProfile.isSubscriptionEventStale(command.eventAt())) {
      return SubscriptionUpdateResult.STALE_EVENT;
    }
    userProfile.updateSubscription(
        command.status(),
        command.periodType(),
        command.expiresAt(),
        command.eventAt(),
        command.productId(),
        command.store());
    return SubscriptionUpdateResult.APPLIED;
  }

  /**
   * 결제 제공자가 알린 계정 간 구독 이전을 반영한다.
   *
   * <p>넘겨준 계정의 구독 정보를 넘겨받은 계정에 복사하고 넘겨준 계정은 구독 없음으로 비운다. 두 계정을 ID 오름차순으로 쓰기 잠금해 교착을 막고, 어느 한쪽이라도 이미
   * 반영한 이벤트보다 오래된 이벤트면 아무것도 바꾸지 않는다. 두 계정이 같거나, 넘겨준 계정이 프리미엄이 아니면(구독 없음·만료) 두 계정 모두 건드리지 않는다.
   *
   * @param fromUserId 구독을 넘겨준 사용자 ID
   * @param toUserId 구독을 넘겨받은 사용자 ID
   * @param eventAt 이벤트 발생 시각
   * @return 이전 처리 결과와 복사된 구독 정보
   */
  @Transactional
  public SubscriptionTransferResult transferSubscription(
      Long fromUserId, Long toUserId, LocalDateTime eventAt) {
    if (fromUserId.equals(toUserId)) {
      // 같은 계정으로의 이전은 옮길 것이 없다. 아래 로직을 타면 자기 구독을 비워 버린다.
      return SubscriptionTransferResult.applied(null);
    }
    // 두 계정을 항상 작은 ID부터 잠근다. 웹훅 두 개가 동시에 서로 반대 순서로 잠그면 교착이 생기기 때문이다.
    Long lowerId = Math.min(fromUserId, toUserId);
    Long higherId = Math.max(fromUserId, toUserId);

    Optional<UserProfile> lower = userProfileRepository.findByIdForUpdate(lowerId);
    Optional<UserProfile> higher = userProfileRepository.findByIdForUpdate(higherId);

    if (lower.isEmpty() || higher.isEmpty()) {
      return SubscriptionTransferResult.userNotFound();
    }
    // 잠근 뒤에는 다시 넘겨준 계정(from)과 넘겨받은 계정(to)으로 나눠 쓴다.
    UserProfile from = lowerId.equals(fromUserId) ? lower.get() : higher.get();
    UserProfile to = lowerId.equals(fromUserId) ? higher.get() : lower.get();
    if (from.isSubscriptionEventStale(eventAt) || to.isSubscriptionEventStale(eventAt)) {
      return SubscriptionTransferResult.stale();
    }
    if (!from.isPremium()) {
      // 구독이 없거나 만료된 계정에서는 옮길 것이 없다. 넘겨받은 계정의 살아 있는 구독을 덮어쓰지 않는다.
      return SubscriptionTransferResult.applied(null);
    }
    UserSubscriptionSnapshot moved = UserSubscriptionSnapshot.fromUserProfile(from);
    to.updateSubscription(
        moved.subscriptionStatus(),
        moved.periodType(),
        moved.expiresAt(),
        eventAt,
        moved.productId(),
        moved.store());
    from.updateSubscription(SubscriptionStatus.NONE, null, null, eventAt, null, null);
    return SubscriptionTransferResult.applied(moved);
  }

  /**
   * 활성 사용자의 서버 기준 구독 상태를 다른 기능이 쓸 스냅샷으로 반환한다.
   *
   * @param userId 조회할 사용자 ID
   * @return 사용자 구독 상태 스냅샷
   * @throws UserProfileException 활성 프로필이 없을 때
   */
  @Transactional(readOnly = true)
  public UserSubscriptionSnapshot getSubscription(Long userId) {
    return UserSubscriptionSnapshot.fromUserProfile(requireActiveEntity(userId));
  }
}
