// 사용자의 억양과 푸시 수신 설정을 관리한다.

package com.landit.landitbe.feature.profile.preference.service;

import com.landit.landitbe.feature.profile.domain.UserProfile;
import com.landit.landitbe.feature.profile.domain.UserProfileStatus;
import com.landit.landitbe.feature.profile.exception.UserProfileErrorCode;
import com.landit.landitbe.feature.profile.exception.UserProfileException;
import com.landit.landitbe.feature.profile.preference.dto.AccentLocaleOptionResponse;
import com.landit.landitbe.feature.profile.preference.dto.UserAccentLocaleResponse;
import com.landit.landitbe.feature.profile.repository.UserProfileRepository;
import com.landit.landitbe.shared.domain.AccentLocale;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 사용자의 억양과 푸시 수신 설정을 관리한다. */
@Service
@RequiredArgsConstructor
public class ProfilePreferenceService {
  private final UserProfileRepository userProfileRepository;

  private static final List<AccentLocale> SUPPORTED_ACCENT_LOCALES =
      List.of(AccentLocale.EN_US, AccentLocale.EN_GB, AccentLocale.EN_AU);

  private UserProfile requireActiveEntity(Long userId) {
    return userProfileRepository
        .findByIdAndStatus(userId, UserProfileStatus.ACTIVE)
        .orElseThrow(() -> new UserProfileException(UserProfileErrorCode.INVALID_TOKEN));
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
    return UserAccentLocaleResponse.from(requireActiveEntity(userId).getAccentLocale());
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
    requireActiveEntity(userId).updateAccentLocale(accentLocale);
  }

  /**
   * 활성 사용자의 푸시 권한을 허용 상태로 갱신한다.
   *
   * @param userId 갱신할 사용자 ID
   * @throws UserProfileException 활성 프로필이 없을 때
   */
  @Transactional
  public void grantPushPermission(Long userId) {
    userProfileRepository
        .findActiveByIdForUpdate(userId)
        .orElseThrow(() -> new UserProfileException(UserProfileErrorCode.INVALID_TOKEN))
        .grantPushPermission(LocalDateTime.now());
  }
}
