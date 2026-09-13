// 다른 기능에 사용자의 학습 설정을 불변 값으로 전달한다.

package com.landit.landitbe.feature.profile.dto;

import com.landit.landitbe.feature.profile.domain.UserProfile;
import com.landit.landitbe.shared.domain.AccentLocale;
import com.landit.landitbe.shared.domain.Locale;

/**
 * 사용자 Entity를 노출하지 않는 학습 설정이다.
 *
 * @param id 사용자 ID
 * @param targetLocale 학습 언어
 * @param baseLocale 기준 언어
 * @param learningLevel 학습 레벨
 * @param aiTutorId AI 튜터 ID
 * @param accentLocale 영어 억양
 */
public record UserLearningProfile(
    Long id,
    Locale targetLocale,
    Locale baseLocale,
    Integer learningLevel,
    Long aiTutorId,
    AccentLocale accentLocale) {
  /**
   * 사용자 Entity의 현재 학습 설정을 복사한다.
   *
   * @param profile 원본 사용자
   * @return 불변 학습 설정
   */
  public static UserLearningProfile from(UserProfile profile) {
    return new UserLearningProfile(
        profile.getId(),
        profile.getTargetLocale(),
        profile.getBaseLocale(),
        profile.getLearningLevel(),
        profile.getAiTutorId(),
        profile.getAccentLocale());
  }
}
