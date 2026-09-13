// 인증용 프로필 생성·갱신과 탈퇴를 처리한다.

package com.landit.landitbe.feature.profile.authentication.service;

import com.landit.landitbe.feature.profile.domain.UserProfile;
import com.landit.landitbe.feature.profile.dto.AuthProfile;
import com.landit.landitbe.feature.profile.repository.UserProfileRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 인증용 프로필 생성·갱신과 탈퇴를 처리한다. */
@Service
@RequiredArgsConstructor
public class ProfileAuthenticationService {
  private final UserProfileRepository userProfileRepository;

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
   * 활성 사용자 프로필을 쓰기 잠금으로 조회해 인증 기능 공개 계약으로 반환한다.
   *
   * <p>잠금은 현재 트랜잭션이 끝날 때까지 유지된다.
   *
   * @param userId 조회할 사용자 ID
   * @return 인증 기능용 사용자 프로필. 활성 프로필이 없으면 빈 값
   */
  @Transactional
  public Optional<AuthProfile> findAuthenticationProfileForUpdate(Long userId) {
    return userProfileRepository.findActiveByIdForUpdate(userId).map(AuthProfile::from);
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
  public Optional<AuthProfile> updateAuthenticationProfileForUpdate(
      Long userId, String email, String nickname) {
    return userProfileRepository
        .findActiveByIdForUpdate(userId)
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
  public boolean withdrawIfActiveForUpdate(Long userId) {
    return userProfileRepository
        .findActiveByIdForUpdate(userId)
        .map(
            userProfile -> {
              userProfile.withdraw();
              return true;
            })
        .orElse(false);
  }
}
