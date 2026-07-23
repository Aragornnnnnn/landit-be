// UserProfileService의 사용자 locale 조회를 단위 검증한다.

package com.landit.landitbe.feature.profile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.profile.domain.UserProfile;
import com.landit.landitbe.feature.profile.domain.UserProfileStatus;
import com.landit.landitbe.feature.profile.dto.UserLocale;
import com.landit.landitbe.feature.profile.repository.UserProfileRepository;
import com.landit.landitbe.shared.domain.Locale;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** UserProfileService의 사용자 locale 조회를 단위 검증한다. */
@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

  private static final Long USER_ID = 1L;

  @Mock private UserProfileRepository userProfileRepository;

  @InjectMocks private UserProfileService userProfileService;

  /** 활성 프로필 조회가 Repository 결과를 그대로 반환하는지 검증한다. */
  @Test
  void requireActiveReturnsActiveProfile() {
    UserProfile userProfile = mock(UserProfile.class);
    when(userProfileRepository.findByIdAndStatus(USER_ID, UserProfileStatus.ACTIVE))
        .thenReturn(Optional.of(userProfile));

    assertThat(userProfileService.requireActive(USER_ID)).isSameAs(userProfile);
  }

  /** 활성 사용자의 학습 locale(target/base)을 프로필에서 그대로 반환하는지 검증한다. */
  @Test
  void shouldReturnLocaleForActiveUser() {
    // given: 프로필에 en/ko locale이 저장된 활성 사용자
    UserProfile userProfile = mock(UserProfile.class);
    when(userProfile.getTargetLocale()).thenReturn(Locale.EN);
    when(userProfile.getBaseLocale()).thenReturn(Locale.KR);
    when(userProfileRepository.findByIdAndStatus(USER_ID, UserProfileStatus.ACTIVE))
        .thenReturn(Optional.of(userProfile));

    // when
    UserLocale locale = userProfileService.getUserLocale(USER_ID);

    // then
    assertThat(locale.targetLocale()).isEqualTo(Locale.EN);
    assertThat(locale.baseLocale()).isEqualTo(Locale.KR);
  }

  /** 활성 사용자가 아니면(미존재/탈퇴) INVALID_TOKEN 예외를 던지는지 검증한다. (토큰 관련 오류는 INVALID_TOKEN으로 통일) */
  @Test
  void shouldThrowInvalidTokenForInactiveUser() {
    // given: 해당 ID의 활성 사용자가 없음
    when(userProfileRepository.findByIdAndStatus(USER_ID, UserProfileStatus.ACTIVE))
        .thenReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> userProfileService.getUserLocale(USER_ID))
        .isInstanceOf(ApiException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.INVALID_TOKEN);
  }
}
