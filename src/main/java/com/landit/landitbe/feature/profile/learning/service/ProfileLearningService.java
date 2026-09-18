// 사용자의 학습 언어·수준과 평가 상태를 관리한다.

package com.landit.landitbe.feature.profile.learning.service;

import com.landit.landitbe.feature.profile.domain.UserProfile;
import com.landit.landitbe.feature.profile.domain.UserProfileStatus;
import com.landit.landitbe.feature.profile.exception.UserProfileErrorCode;
import com.landit.landitbe.feature.profile.exception.UserProfileException;
import com.landit.landitbe.feature.profile.learning.dto.UserLearningAssessmentState;
import com.landit.landitbe.feature.profile.learning.dto.UserLearningLevelResponse;
import com.landit.landitbe.feature.profile.learning.dto.UserLearningProfile;
import com.landit.landitbe.feature.profile.learning.dto.UserLocale;
import com.landit.landitbe.feature.profile.repository.UserProfileRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 사용자의 학습 언어·수준과 평가 상태를 관리한다. */
@Service
@RequiredArgsConstructor
public class ProfileLearningService {
  private final UserProfileRepository userProfileRepository;
  private final java.time.Clock clock;

  private UserProfile requireActiveEntity(Long userId) {
    return userProfileRepository
        .findByIdAndStatus(userId, UserProfileStatus.ACTIVE)
        .orElseThrow(() -> new UserProfileException(UserProfileErrorCode.INVALID_TOKEN));
  }

  /**
   * 활성 사용자 잠금을 획득하고 학습 설정을 반환한다. 잠금은 호출 트랜잭션 종료까지 유지된다.
   *
   * @param userId 잠글 사용자 ID
   * @return 활성 학습 설정. 활성 사용자가 없으면 빈 값
   */
  @Transactional
  public Optional<UserLearningProfile> findActiveLearningProfileForUpdate(Long userId) {
    return userProfileRepository.findActiveByIdForUpdate(userId).map(UserLearningProfile::from);
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
    UserProfile userProfile = requireActiveEntity(userId);

    return new UserLocale(userProfile.getTargetLocale(), userProfile.getBaseLocale());
  }

  /**
   * 활성 사용자의 학습 수준을 조회한다.
   *
   * @param userId 조회할 사용자 ID
   * @return 현재 적용 학습 수준. 미설정 값도 기본 수준 3으로 반환
   * @throws UserProfileException 활성 프로필이 없을 때
   */
  @Transactional(readOnly = true)
  public UserLearningLevelResponse getLearningLevel(Long userId) {
    return new UserLearningLevelResponse(requireActiveEntity(userId).getLearningLevel());
  }

  /**
   * 프로필 상태와 무관하게 학습 수준을 조회한다.
   *
   * <p>사용자 요청이 아니라 백그라운드 콘텐츠 추천에서 쓰는 조회다. 프로필이 비활성이라는 이유로 추천 작업을 실패시키지 않도록 {@link
   * com.landit.landitbe.feature.profile.service.UserProfileService#requireActive(Long)}와 달리 예외를 던지지
   * 않는다. 프로필은 있으나 수준이 미설정이면 기본 수준 3을 반환한다.
   *
   * @param userProfileId 조회할 사용자 ID
   * @return 현재 적용 학습 수준. 프로필 자체가 없으면 빈 값
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
    userProfileRepository
        .findActiveByIdForUpdate(userId)
        .orElseThrow(() -> new UserProfileException(UserProfileErrorCode.INVALID_TOKEN))
        .updateLearningLevel(learningLevel, java.time.LocalDateTime.now(clock));
  }

  /**
   * 활성 사용자를 잠그고 수준 평가에 필요한 상태를 반환한다.
   *
   * @param userId 평가 대상 사용자
   * @return 잠근 프로필의 수준 상태. 비활성이거나 없으면 빈 값
   */
  @Transactional
  public Optional<UserLearningAssessmentState> findLearningAssessmentStateForUpdate(long userId) {
    return userProfileRepository
        .findActiveByIdForUpdate(userId)
        .map(
            profile ->
                new UserLearningAssessmentState(
                    profile.getLearningLevel(),
                    profile.getPromotionStreak(),
                    profile.getLearningLevelUpdatedAt()));
  }

  /**
   * 같은 트랜잭션에서 잠근 사용자의 평가 결과를 적용한다.
   *
   * @param userId findLearningAssessmentStateForUpdate로 잠근 사용자
   * @param learningLevel 적용할 수준
   * @param promotionStreak 연속 승급 근거 횟수
   * @param assessedAt 적용 시각
   */
  @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
  public void applyAssessedLearningLevel(
      long userId, Integer learningLevel, int promotionStreak, LocalDateTime assessedAt) {
    userProfileRepository
        .getReferenceById(userId)
        .applyAssessedLearningLevel(learningLevel, promotionStreak, assessedAt);
  }
}
