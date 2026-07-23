// 사용자 프로필의 조회성 보조 기능(학습 locale 등)을 다른 모듈에 제공한다.

package com.landit.landitbe.feature.profile.service;

import com.landit.landitbe.feature.profile.domain.UserProfile;
import com.landit.landitbe.feature.profile.domain.UserProfileStatus;
import com.landit.landitbe.feature.profile.dto.UserLocale;
import com.landit.landitbe.feature.profile.exception.UserProfileErrorCode;
import com.landit.landitbe.feature.profile.exception.UserProfileException;
import com.landit.landitbe.feature.profile.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 사용자 프로필의 조회성 보조 기능(학습 locale 등)을 다른 모듈에 제공한다. */
@Service
@RequiredArgsConstructor
public class UserProfileService {

  private final UserProfileRepository userProfileRepository;

  /**
   * 활성 사용자 프로필을 조회한다.
   *
   * @param userId 조회할 사용자 ID
   * @return 활성 사용자 프로필
   * @throws UserProfileException 활성 프로필이 없을 때
   */
  @Transactional(readOnly = true)
  public UserProfile requireActive(Long userId) {
    return userProfileRepository
        .findByIdAndStatus(userId, UserProfileStatus.ACTIVE)
        .orElseThrow(() -> new UserProfileException(UserProfileErrorCode.INVALID_TOKEN));
  }

  /**
   * 세션 시작을 직렬화하기 위해 활성 사용자 프로필을 쓰기 잠금으로 조회한다.
   *
   * @param userId 조회할 사용자 ID
   * @return 쓰기 잠금으로 조회한 활성 사용자 프로필
   * @throws UserProfileException 활성 프로필이 없을 때
   */
  @Transactional
  public UserProfile requireActiveForUpdate(Long userId) {
    return userProfileRepository
        .findActiveByIdForUpdate(userId)
        .orElseThrow(() -> new UserProfileException(UserProfileErrorCode.INVALID_TOKEN));
  }

  /**
   * 사용자 프로필을 저장한다.
   *
   * @param userProfile 저장할 사용자 프로필
   * @return 저장된 사용자 프로필
   */
  @Transactional
  public UserProfile save(UserProfile userProfile) {
    return userProfileRepository.save(userProfile);
  }

  /**
   * 활성 사용자 프로필이 존재하는지 확인한다.
   *
   * @param userId 확인할 사용자 ID
   * @return 활성 프로필 존재 여부
   */
  @Transactional(readOnly = true)
  public boolean existsActive(Long userId) {
    return userProfileRepository.existsByIdAndStatus(userId, UserProfileStatus.ACTIVE);
  }

  /**
   * 활성 사용자의 학습 locale을 조회한다.
   *
   * @param userId 조회할 사용자 ID
   * @return 학습 대상 locale과 기준 locale
   * @throws UserProfileException 활성 프로필이 없을 때
   */
  @Transactional(readOnly = true)
  public UserLocale getUserLocale(Long userId) {
    UserProfile userProfile = requireActive(userId);

    return new UserLocale(userProfile.getTargetLocale(), userProfile.getBaseLocale());
  }
}
