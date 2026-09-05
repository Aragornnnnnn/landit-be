// 유저 프로필에 저장된 목표 억양을 발음 기준으로 제공한다.

package com.landit.landitbe.feature.content.service;

import com.landit.landitbe.feature.profile.domain.UserProfile;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import com.landit.landitbe.shared.domain.AccentLocale;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 유저 프로필에 저장된 목표 억양을 발음 기준으로 제공한다.
 *
 * <p>경로: user_profile.accent_locale. 유저가 온보딩에서 고른 나라가 곧 발음 학습의 기준 억양이다.
 */
@Component
@RequiredArgsConstructor
public class UserAccentLocaleResolver {

  private final UserProfileService userProfileService;

  /**
   * 유저의 목표 억양을 도출한다. 발음 평가처럼 억양이 반드시 필요한 곳에서 쓴다.
   *
   * @param userId 사용자 ID
   * @return 목표 억양
   * @throws ApiException 억양이 설정되지 않았을 때
   */
  public AccentLocale require(Long userId) {
    UserProfile userProfile = userProfileService.requireActive(userId);
    if (userProfile.getAccentLocale() == null) {
      throw new ApiException(ErrorCode.INVALID_REQUEST, "영어 억양이 설정되지 않았습니다.");
    }
    return userProfile.getAccentLocale();
  }

  /**
   * 유저의 목표 억양을 도출한다. 억양이 부가 정보인 곳(learning-start의 음성 URL)에서 사용한다.
   *
   * @param userId 사용자 ID
   * @return 목표 억양. 도출할 수 없으면 빈 Optional
   */
  public Optional<AccentLocale> tryResolve(Long userId) {
    UserProfile userProfile = userProfileService.requireActive(userId);
    return Optional.ofNullable(userProfile.getAccentLocale());
  }
}
