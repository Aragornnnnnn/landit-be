// 사용자 프로필의 조회성 보조 기능(학습 locale 등)을 다른 모듈에 제공한다.

package com.landit.landitbe.feature.profile.service;

import com.landit.landitbe.feature.profile.domain.UserProfile;
import com.landit.landitbe.feature.profile.domain.UserProfileStatus;
import com.landit.landitbe.feature.profile.dto.UserLocale;
import com.landit.landitbe.feature.profile.repository.UserProfileRepository;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 사용자 프로필의 조회성 보조 기능(학습 locale 등)을 다른 모듈에 제공한다. */
@Service
@RequiredArgsConstructor
public class UserProfileService {

  private final UserProfileRepository userProfileRepository;

  /** 활성 사용자 프로필을 조회한다. */
  @Transactional(readOnly = true)
  public UserProfile requireActive(Long userId) {
    return userProfileRepository
        .findByIdAndStatus(userId, UserProfileStatus.ACTIVE)
        .orElseThrow(() -> new ApiException(ErrorCode.INVALID_TOKEN));
  }

  /** 세션 시작을 직렬화하기 위해 활성 사용자 프로필을 쓰기 잠금으로 조회한다. */
  @Transactional
  public UserProfile requireActiveForUpdate(Long userId) {
    return userProfileRepository
        .findActiveByIdForUpdate(userId)
        .orElseThrow(() -> new ApiException(ErrorCode.INVALID_TOKEN));
  }

  /** 사용자 프로필을 저장한다. */
  @Transactional
  public UserProfile save(UserProfile userProfile) {
    return userProfileRepository.save(userProfile);
  }

  /** 활성 사용자 프로필이 존재하는지 확인한다. */
  @Transactional(readOnly = true)
  public boolean existsActive(Long userId) {
    return userProfileRepository.existsByIdAndStatus(userId, UserProfileStatus.ACTIVE);
  }

  /** 활성 사용자의 학습 locale(target/base)을 조회한다. 활성 사용자가 아니면 INVALID_TOKEN 예외를 던진다. */
  @Transactional(readOnly = true)
  public UserLocale getUserLocale(Long userId) {
    UserProfile userProfile = requireActive(userId);

    return new UserLocale(userProfile.getTargetLocale(), userProfile.getBaseLocale());
  }
}
