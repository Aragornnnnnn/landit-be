// 사용자 프로필을 소유하며 다른 기능에 조회와 상태 변경 계약을 제공한다.

package com.landit.landitbe.feature.profile.service;

import com.landit.landitbe.feature.profile.domain.UserProfile;
import com.landit.landitbe.feature.profile.domain.UserProfileStatus;
import com.landit.landitbe.feature.profile.dto.AuthProfile;
import com.landit.landitbe.feature.profile.dto.UserLocale;
import com.landit.landitbe.feature.profile.exception.UserProfileErrorCode;
import com.landit.landitbe.feature.profile.exception.UserProfileException;
import com.landit.landitbe.feature.profile.repository.UserProfileRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 사용자 프로필을 소유하며 다른 기능에 조회와 상태 변경 계약을 제공한다. */
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
   * 기본 학습 설정을 가진 인증 사용자 프로필을 생성한다.
   *
   * @param email 사용자 이메일
   * @param nickname 사용자 닉네임
   * @param aiTutorId 기본 AI 튜터 ID
   * @return 생성된 인증 사용자 프로필
   */
  @Transactional
  public AuthProfile createAuthenticationProfile(String email, String nickname, Long aiTutorId) {
    UserProfile userProfile =
        userProfileRepository.save(new UserProfile(email, nickname, aiTutorId));
    return AuthProfile.from(userProfile);
  }

  /**
   * 활성 사용자 프로필을 인증 기능 공개 계약으로 조회한다.
   *
   * @param userId 조회할 사용자 ID
   * @return 인증 기능용 사용자 프로필. 활성 프로필이 없으면 빈 값
   */
  @Transactional(readOnly = true)
  public Optional<AuthProfile> findAuthenticationProfile(Long userId) {
    return userProfileRepository
        .findByIdAndStatus(userId, UserProfileStatus.ACTIVE)
        .map(AuthProfile::from);
  }

  /**
   * 소셜 로그인에서 받은 최신 프로필 정보를 반영한다.
   *
   * @param userId 갱신할 사용자 ID
   * @param email 사용자 이메일
   * @param nickname 사용자 닉네임
   * @return 갱신된 인증 사용자 프로필. 활성 프로필이 없으면 빈 값
   */
  @Transactional
  public Optional<AuthProfile> updateAuthenticationProfile(
      Long userId, String email, String nickname) {
    return userProfileRepository
        .findByIdAndStatus(userId, UserProfileStatus.ACTIVE)
        .map(
            userProfile -> {
              userProfile.updateProfile(email, nickname);
              return AuthProfile.from(userProfile);
            });
  }

  /**
   * 활성 사용자 프로필을 탈퇴 상태로 전환한다.
   *
   * @param userId 탈퇴할 사용자 ID
   * @return 탈퇴 처리 여부
   */
  @Transactional
  public boolean withdrawIfActive(Long userId) {
    return userProfileRepository
        .findByIdAndStatus(userId, UserProfileStatus.ACTIVE)
        .map(
            userProfile -> {
              userProfile.withdraw();
              return true;
            })
        .orElse(false);
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
