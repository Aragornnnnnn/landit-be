// 사용자 프로필을 소유하며 다른 기능에 조회와 상태 변경 계약을 제공한다.

package com.landit.landitbe.feature.profile.service;

import com.landit.landitbe.feature.profile.domain.UserProfile;
import com.landit.landitbe.feature.profile.domain.UserProfileStatus;
import com.landit.landitbe.feature.profile.domain.UserRole;
import com.landit.landitbe.feature.profile.dto.UserProfileDetails;
import com.landit.landitbe.feature.profile.dto.UserProfileNickname;
import com.landit.landitbe.feature.profile.dto.UserProfilePage;
import com.landit.landitbe.feature.profile.exception.UserProfileErrorCode;
import com.landit.landitbe.feature.profile.exception.UserProfileException;
import com.landit.landitbe.feature.profile.learning.dto.UserLearningProfile;
import com.landit.landitbe.feature.profile.repository.UserProfileRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
  public UserLearningProfile requireActive(Long userId) {
    return UserLearningProfile.from(requireActiveEntity(userId));
  }

  private UserProfile requireActiveEntity(Long userId) {
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
  public UserLearningProfile requireActiveForUpdate(Long userId) {
    return userProfileRepository
        .findActiveByIdForUpdate(userId)
        .map(UserLearningProfile::from)
        .orElseThrow(() -> new UserProfileException(UserProfileErrorCode.INVALID_TOKEN));
  }

  /**
   * 후보 ID 가운데 실제로 존재하는 첫 사용자 프로필 ID를 찾는다.
   *
   * <p>결제 제공자 웹훅이 이력을 저장하기 전에 사용자를 확정하는 용도라, 탈퇴한 사용자도 포함한다.
   *
   * @param candidateUserIds 확인할 사용자 ID 후보. 앞선 후보를 우선한다
   * @return 존재하는 첫 사용자 프로필 ID. 없으면 빈 값
   */
  @Transactional(readOnly = true)
  public Optional<Long> findExistingUserId(List<Long> candidateUserIds) {
    return candidateUserIds.stream().filter(userProfileRepository::existsById).findFirst();
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
   * 일괄 작업 대상 활성 프로필을 잠그고 해당 ID만 반환한다.
   *
   * <p>호출자의 쓰기 트랜잭션 안에서 사용해야 작업이 끝날 때까지 잠금이 유지된다.
   *
   * @param userIds 확인할 사용자 ID 목록
   * @return 존재하는 활성 사용자 ID 목록. 빈 입력은 빈 목록을 반환한다
   */
  @Transactional
  public List<Long> findActiveIdsForUpdate(List<Long> userIds) {
    if (userIds.isEmpty()) {
      return List.of();
    }
    return userProfileRepository.findActiveByIdsForUpdate(userIds).stream()
        .map(UserProfile::getId)
        .toList();
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
   * 관리자 사용자 목록을 가입일 최신순으로 조회한다.
   *
   * @param page 페이지 번호
   * @param size 페이지 크기
   * @param active 활성 여부. 생략하면 모든 상태
   * @param pushConsent 저장된 푸시 동의 여부. 생략하면 모든 권한 상태
   * @return 관리자 사용자 프로필 목록 페이지
   */
  @Transactional(readOnly = true)
  public UserProfilePage getUserProfiles(int page, int size, Boolean active, Boolean pushConsent) {
    Page<UserProfile> profiles =
        userProfileRepository.findAdminUsers(active, pushConsent, PageRequest.of(page, size));

    return UserProfilePage.from(profiles, page, size);
  }

  /**
   * 관리자 사용자 상세 조회에 사용할 프로필을 계정 상태와 관계없이 조회한다.
   *
   * @param userProfileId 사용자 프로필 ID
   * @return 관리자 사용자 프로필
   * @throws UserProfileException 사용자가 없을 때
   */
  @Transactional(readOnly = true)
  public UserProfileDetails getUserProfileDetails(long userProfileId) {
    return userProfileRepository
        .findById(userProfileId)
        .map(UserProfileDetails::from)
        .orElseThrow(() -> new UserProfileException(UserProfileErrorCode.USER_PROFILE_NOT_FOUND));
  }
}
