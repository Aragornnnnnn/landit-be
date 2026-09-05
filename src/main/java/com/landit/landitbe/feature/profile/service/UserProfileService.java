// 사용자 프로필을 소유하며 다른 기능에 조회와 상태 변경 계약을 제공한다.

package com.landit.landitbe.feature.profile.service;

import com.landit.landitbe.feature.profile.domain.UserProfile;
import com.landit.landitbe.feature.profile.domain.UserProfileStatus;
import com.landit.landitbe.feature.profile.domain.UserRole;
import com.landit.landitbe.feature.profile.dto.AccentLocaleOptionResponse;
import com.landit.landitbe.feature.profile.dto.AdminUserProfile;
import com.landit.landitbe.feature.profile.dto.AdminUserProfilePage;
import com.landit.landitbe.feature.profile.dto.AuthProfile;
import com.landit.landitbe.feature.profile.dto.UserAccentLocaleResponse;
import com.landit.landitbe.feature.profile.dto.UserLearningLevelResponse;
import com.landit.landitbe.feature.profile.dto.UserLocale;
import com.landit.landitbe.feature.profile.dto.UserProfileNickname;
import com.landit.landitbe.feature.profile.exception.UserProfileErrorCode;
import com.landit.landitbe.feature.profile.exception.UserProfileException;
import com.landit.landitbe.feature.profile.repository.UserProfileRepository;
import com.landit.landitbe.shared.domain.AccentLocale;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 사용자 프로필을 소유하며 다른 기능에 조회와 상태 변경 계약을 제공한다. */
@Service
@RequiredArgsConstructor
public class UserProfileService {

  private static final List<AccentLocale> SUPPORTED_ACCENT_LOCALES =
      List.of(AccentLocale.EN_US, AccentLocale.EN_GB, AccentLocale.EN_AU);

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
   * 활성 사용자 프로필이 관리자 역할을 가졌는지 확인한다.
   *
   * @param userId 확인할 사용자 프로필 ID
   * @return 활성 관리자 프로필이면 {@code true}
   */
  @Transactional(readOnly = true)
  public boolean isAdmin(Long userId) {
    return userId != null
        && userProfileRepository.existsByIdAndStatusAndRole(
            userId, UserProfileStatus.ACTIVE, UserRole.ADMIN);
  }

  /**
   * 사용자 프로필 ID로 닉네임을 조회한다.
   *
   * @param userProfileId 조회할 사용자 프로필 ID
   * @return 사용자 닉네임 계약. 프로필이 없으면 빈 값
   */
  @Transactional(readOnly = true)
  public Optional<UserProfileNickname> findNickname(Long userProfileId) {
    if (userProfileId == null) {
      return Optional.empty();
    }
    return userProfileRepository
        .findById(userProfileId)
        .map(userProfile -> new UserProfileNickname(userProfile.getNickname()));
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

  /**
   * 활성 사용자의 학습 수준을 조회한다.
   *
   * @param userId 조회할 사용자 ID
   * @return 사용자가 선택한 학습 수준. 미설정이면 {@code null}
   * @throws UserProfileException 활성 프로필이 없을 때
   */
  @Transactional(readOnly = true)
  public UserLearningLevelResponse getLearningLevel(Long userId) {
    return new UserLearningLevelResponse(requireActive(userId).getLearningLevel());
  }

  /**
   * 프로필 상태와 무관하게 학습 수준을 조회한다.
   *
   * <p>사용자 요청이 아니라 백그라운드 콘텐츠 추천에서 쓰는 조회다. 프로필이 비활성이라는 이유로 추천 작업을 실패시키지 않도록 {@link
   * #requireActive(Long)}와 달리 예외를 던지지 않는다. 값이 없으면 호출부가 학습 수준을 모르는 경우로 처리한다.
   *
   * @param userProfileId 조회할 사용자 ID
   * @return 사용자가 선택한 학습 수준. 프로필이 없거나 학습 수준이 미설정이면 빈 값
   */
  @Transactional(readOnly = true)
  public Optional<Integer> findLearningLevel(Long userProfileId) {
    return userProfileRepository.findById(userProfileId).map(UserProfile::getLearningLevel);
  }

  /**
   * 활성 사용자의 학습 수준을 갱신한다.
   *
   * @param userId 갱신할 사용자 ID
   * @param learningLevel 온보딩에서 선택한 1부터 5까지의 학습 수준
   * @throws UserProfileException 활성 프로필이 없을 때
   */
  @Transactional
  public void updateLearningLevel(Long userId, int learningLevel) {
    requireActive(userId).updateLearningLevel(learningLevel);
  }

  /**
   * 지원하는 영어 억양 목록을 반환한다.
   *
   * @return 미국, 영국, 호주 억양 선택지
   */
  @Transactional(readOnly = true)
  public List<AccentLocaleOptionResponse> getAccentLocales() {
    return SUPPORTED_ACCENT_LOCALES.stream().map(AccentLocaleOptionResponse::from).toList();
  }

  /**
   * 활성 사용자의 현재 영어 억양을 반환한다.
   *
   * @param userId 조회할 사용자 ID
   * @return 사용자의 현재 영어 억양
   * @throws UserProfileException 활성 프로필이 없을 때
   */
  @Transactional(readOnly = true)
  public UserAccentLocaleResponse getAccentLocale(Long userId) {
    return UserAccentLocaleResponse.from(requireActive(userId).getAccentLocale());
  }

  /**
   * 활성 사용자의 영어 억양을 갱신한다.
   *
   * @param userId 갱신할 사용자 ID
   * @param accentLocale 선택한 영어 억양
   * @throws UserProfileException 활성 프로필이 없을 때
   */
  @Transactional
  public void updateAccentLocale(Long userId, AccentLocale accentLocale) {
    requireActive(userId).updateAccentLocale(accentLocale);
  }

  /**
   * 활성 사용자의 푸시 권한을 허용 상태로 갱신한다.
   *
   * @param userId 갱신할 사용자 ID
   * @throws UserProfileException 활성 프로필이 없을 때
   */
  @Transactional
  public void grantPushPermission(Long userId) {
    requireActiveForUpdate(userId).grantPushPermission(LocalDateTime.now());
  }

  /**
   * 관리자 사용자 목록을 가입일 최신순으로 조회한다.
   *
   * @param page 페이지 번호
   * @param size 페이지 크기
   * @return 관리자 사용자 프로필 목록 페이지
   */
  @Transactional(readOnly = true)
  public AdminUserProfilePage getAdminUserProfiles(int page, int size) {
    Slice<UserProfile> profiles =
        userProfileRepository.findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(page, size));

    return AdminUserProfilePage.from(profiles, page, size);
  }

  /**
   * 관리자 사용자 상세 조회에 사용할 프로필을 계정 상태와 관계없이 조회한다.
   *
   * @param userProfileId 사용자 프로필 ID
   * @return 관리자 사용자 프로필
   * @throws UserProfileException 사용자가 없을 때
   */
  @Transactional(readOnly = true)
  public AdminUserProfile getAdminUserProfile(long userProfileId) {
    return userProfileRepository
        .findById(userProfileId)
        .map(AdminUserProfile::from)
        .orElseThrow(() -> new UserProfileException(UserProfileErrorCode.USER_PROFILE_NOT_FOUND));
  }
}
