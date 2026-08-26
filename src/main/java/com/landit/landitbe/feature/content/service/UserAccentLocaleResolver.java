// 유저의 목표 억양을 AI 튜터 설정에서 도출한다.

package com.landit.landitbe.feature.content.service;

import com.landit.landitbe.feature.content.domain.AiTutor;
import com.landit.landitbe.feature.content.repository.AiTutorRepository;
import com.landit.landitbe.feature.profile.domain.UserProfile;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import com.landit.landitbe.shared.domain.AccentLocale;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 유저의 목표 억양을 AI 튜터 설정에서 도출한다.
 *
 * <p>경로: user_profile.ai_tutor_id → ai_tutor.accent_locale. 유저가 온보딩에서 고른 튜터의 국적이 곧 발음 학습의 기준 억양이다.
 */
@Component
@RequiredArgsConstructor
public class UserAccentLocaleResolver {

  private final UserProfileService userProfileService;
  private final AiTutorRepository aiTutorRepository;

  /**
   * 유저의 목표 억양을 도출한다. 튜터가 없으면 예외를 던진다 — 발음 평가처럼 억양이 반드시 필요한 곳에서 쓴다.
   *
   * @param userId 사용자 ID
   * @return 목표 억양
   * @throws ApiException 튜터가 설정되지 않았거나 튜터 데이터가 없을 때
   */
  public AccentLocale require(Long userId) {
    UserProfile userProfile = userProfileService.requireActive(userId);
    if (userProfile.getAiTutorId() == null) {
      throw new ApiException(ErrorCode.INVALID_REQUEST, "AI 튜터가 설정되지 않았습니다.");
    }
    AiTutor tutor =
        aiTutorRepository
            .findById(userProfile.getAiTutorId())
            .orElseThrow(() -> new ApiException(ErrorCode.DEFAULT_AI_TUTOR_NOT_CONFIGURED));
    return tutor.getAccentLocale();
  }

  /**
   * 유저의 목표 억양을 도출한다. 튜터가 없으면 빈 값을 반환한다 — 억양이 부가 정보인 곳(learning-start의 음성 URL)에서 화면 전체를 깨지 않기 위해
   * 쓴다.
   *
   * @param userId 사용자 ID
   * @return 목표 억양. 도출할 수 없으면 빈 Optional
   */
  public Optional<AccentLocale> tryResolve(Long userId) {
    UserProfile userProfile = userProfileService.requireActive(userId);
    if (userProfile.getAiTutorId() == null) {
      return Optional.empty();
    }
    return aiTutorRepository.findById(userProfile.getAiTutorId()).map(AiTutor::getAccentLocale);
  }
}
