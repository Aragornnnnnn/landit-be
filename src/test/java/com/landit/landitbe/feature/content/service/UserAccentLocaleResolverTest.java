// 사용자 프로필에 저장된 억양을 발음 기준으로 사용하는지 검증한다.

package com.landit.landitbe.feature.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.profile.domain.UserProfile;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import com.landit.landitbe.shared.domain.AccentLocale;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 사용자 프로필에 저장된 억양을 발음 기준으로 사용하는지 검증한다. */
@ExtendWith(MockitoExtension.class)
class UserAccentLocaleResolverTest {

  private static final Long USER_ID = 1L;

  @Mock private UserProfileService userProfileService;

  @InjectMocks private UserAccentLocaleResolver resolver;

  /** AI 튜터 조회 없이 프로필에 저장된 억양을 반환한다. */
  @Test
  void returnsAccentLocaleStoredOnUserProfile() {
    UserProfile userProfile = mock(UserProfile.class);
    when(userProfile.getAccentLocale()).thenReturn(AccentLocale.EN_GB);
    when(userProfileService.requireActive(USER_ID)).thenReturn(userProfile);

    assertThat(resolver.require(USER_ID)).isEqualTo(AccentLocale.EN_GB);
  }

  /** 프로필의 억양이 있으면 부가 조회도 해당 억양으로 반환한다. */
  @Test
  void tryResolveReturnsAccentLocaleStoredOnUserProfile() {
    UserProfile userProfile = mock(UserProfile.class);
    when(userProfile.getAccentLocale()).thenReturn(AccentLocale.EN_AU);
    when(userProfileService.requireActive(USER_ID)).thenReturn(userProfile);

    assertThat(resolver.tryResolve(USER_ID)).isEqualTo(Optional.of(AccentLocale.EN_AU));
  }

  /** 프로필의 억양이 비어 있으면 부가 조회는 빈 값으로 반환한다. */
  @Test
  void tryResolveReturnsEmptyWhenAccentLocaleIsMissing() {
    UserProfile userProfile = mock(UserProfile.class);
    when(userProfileService.requireActive(USER_ID)).thenReturn(userProfile);

    assertThat(resolver.tryResolve(USER_ID)).isEmpty();
  }
}
