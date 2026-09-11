// 기존 사용자 Push Token을 푸시 발송 대상으로 조회하고 관리한다.

package com.landit.landitbe.feature.notification.service;

import com.landit.landitbe.feature.notification.domain.UserPushToken;
import com.landit.landitbe.feature.notification.domain.UserPushTokenStatus;
import com.landit.landitbe.feature.notification.repository.UserPushTokenRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 기존 사용자 Push Token을 푸시 발송 대상으로 조회하고 관리한다. */
@Service
@RequiredArgsConstructor
public class UserPushTokenDeliveryService {

  private final UserPushTokenRepository userPushTokenRepository;

  /**
   * 묶음 대상 Token을 잠그고 요청한 사용자의 활성 Token만 반환한다.
   *
   * @param owners Token ID별 발송 대상 사용자 ID
   * @return Token ID별 잠금 확인된 발송 대상
   */
  @Transactional
  public Map<Long, UserPushTokenDeliveryTarget> findLockedSendableDeliveryTargets(
      Map<Long, Long> owners) {
    if (owners.isEmpty()) {
      return Map.of();
    }
    return userPushTokenRepository.findAllByIdsForUpdate(List.copyOf(owners.keySet())).stream()
        .filter(token -> token.getStatus() == UserPushTokenStatus.ACTIVE)
        .filter(token -> token.getUserProfileId().equals(owners.get(token.getId())))
        .collect(
            Collectors.toMap(
                UserPushToken::getId,
                token -> new UserPushTokenDeliveryTarget(token.getId(), token.getExpoPushToken())));
  }

  /**
   * 사용할 수 없는 Expo Token들의 현재 소유 행을 잠가 비활성화한다.
   *
   * @param expoPushTokens 무효 Expo Token 값 목록
   */
  @Transactional
  public void revokeCurrentTokenOwners(List<String> expoPushTokens) {
    if (!expoPushTokens.isEmpty()) {
      userPushTokenRepository
          .findAllByValuesForUpdate(expoPushTokens)
          .forEach(UserPushToken::revoke);
    }
  }

  /**
   * Token을 잠그고 현재 사용자 소유의 활성 Token이면 발송 대상으로 반환한다.
   *
   * @param userPushTokenId 사용자 Push Token ID
   * @param userProfileId 발송 대상 사용자 ID
   * @return 잠금 확인된 발송 대상
   */
  @Transactional
  public Optional<UserPushTokenDeliveryTarget> findLockedSendableDeliveryTarget(
      Long userPushTokenId, Long userProfileId) {
    return userPushTokenRepository
        .findByIdForUpdate(userPushTokenId)
        .filter(token -> token.getStatus() == UserPushTokenStatus.ACTIVE)
        .filter(token -> token.getUserProfileId().equals(userProfileId))
        .map(token -> new UserPushTokenDeliveryTarget(token.getId(), token.getExpoPushToken()));
  }

  /**
   * Expo가 사용할 수 없다고 응답한 현재 Token을 비활성화한다.
   *
   * @param expoPushToken Expo Push Token
   */
  @Transactional
  public void revokeCurrentTokenOwner(String expoPushToken) {
    userPushTokenRepository
        .findByExpoPushTokenForUpdate(expoPushToken)
        .ifPresent(UserPushToken::revoke);
  }

  /**
   * 여러 사용자의 활성 Token ID를 사용자별로 조회한다.
   *
   * @param userProfileIds 사용자 프로필 ID 목록
   * @return 사용자 프로필 ID별 활성 Token ID 목록
   */
  @Transactional(readOnly = true)
  public Map<Long, List<Long>> findSendableTokenIdsByUserProfileIds(List<Long> userProfileIds) {
    if (userProfileIds.isEmpty()) {
      return Map.of();
    }
    return userPushTokenRepository
        .findAllByUserProfileIdInAndStatusOrderByUserProfileIdAscIdAsc(
            userProfileIds, UserPushTokenStatus.ACTIVE)
        .stream()
        .collect(
            Collectors.groupingBy(
                UserPushToken::getUserProfileId,
                LinkedHashMap::new,
                Collectors.mapping(UserPushToken::getId, Collectors.toList())));
  }
}
