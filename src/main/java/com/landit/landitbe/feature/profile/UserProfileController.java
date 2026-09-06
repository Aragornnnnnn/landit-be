// 인증된 사용자의 프로필 설정 조회 및 변경 요청을 처리한다.

package com.landit.landitbe.feature.profile;

import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.feature.profile.docs.UserProfileControllerDocs;
import com.landit.landitbe.feature.profile.dto.AccentLocaleOptionResponse;
import com.landit.landitbe.feature.profile.dto.UserAccentLocaleResponse;
import com.landit.landitbe.feature.profile.dto.UserAccentLocaleUpdateRequest;
import com.landit.landitbe.feature.profile.dto.UserLearningLevelResponse;
import com.landit.landitbe.feature.profile.dto.UserLearningLevelUpdateRequest;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import com.landit.landitbe.shared.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 인증된 사용자의 프로필 설정 API 요청을 처리한다. */
@RestController
@RequiredArgsConstructor
public class UserProfileController implements UserProfileControllerDocs {

  private final UserProfileService userProfileService;

  /** {@inheritDoc} */
  @Override
  @GetMapping("/api/v1/me/learning-level")
  public ApiResponse<UserLearningLevelResponse> getLearningLevel(
      @AuthenticationPrincipal AuthUserPrincipal principal) {
    return ApiResponse.success(userProfileService.getLearningLevel(principal.userId()));
  }

  /** {@inheritDoc} */
  @Override
  @PutMapping("/api/v1/me/learning-level")
  public ApiResponse<Void> updateLearningLevel(
      @AuthenticationPrincipal AuthUserPrincipal principal,
      @Valid @RequestBody UserLearningLevelUpdateRequest request) {
    userProfileService.updateLearningLevel(principal.userId(), request.learningLevel());
    return ApiResponse.success(null);
  }

  /** {@inheritDoc} */
  @Override
  @GetMapping("/api/v1/accent-locales")
  public ApiResponse<List<AccentLocaleOptionResponse>> getAccentLocales() {
    return ApiResponse.success(userProfileService.getAccentLocales());
  }

  /** {@inheritDoc} */
  @Override
  @GetMapping("/api/v1/me/accent-locale")
  public ApiResponse<UserAccentLocaleResponse> getAccentLocale(
      @AuthenticationPrincipal AuthUserPrincipal principal) {
    return ApiResponse.success(userProfileService.getAccentLocale(principal.userId()));
  }

  /** {@inheritDoc} */
  @Override
  @PutMapping("/api/v1/me/accent-locale")
  public ApiResponse<Void> updateAccentLocale(
      @AuthenticationPrincipal AuthUserPrincipal principal,
      @Valid @RequestBody UserAccentLocaleUpdateRequest request) {
    userProfileService.updateAccentLocale(principal.userId(), request.accentLocale());
    return ApiResponse.success(null);
  }
}
