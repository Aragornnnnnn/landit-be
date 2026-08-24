// 관리자 사용자 조회에 필요한 프로필 정보를 전달한다.

package com.landit.landitbe.feature.profile.dto;

import com.landit.landitbe.feature.profile.domain.PushPermissionStatus;
import com.landit.landitbe.feature.profile.domain.UserProfile;
import com.landit.landitbe.feature.profile.domain.UserProfileStatus;
import com.landit.landitbe.feature.profile.domain.UserRole;
import com.landit.landitbe.shared.domain.Locale;
import java.time.LocalDateTime;

/**
 * 관리자 사용자 조회에 필요한 프로필 정보를 전달한다.
 *
 * @param userProfileId 사용자 프로필 ID
 * @param email 이메일
 * @param nickname 닉네임
 * @param role 사용자 역할
 * @param status 계정 상태
 * @param targetLocale 학습 대상 언어
 * @param baseLocale 기준 언어
 * @param learningLevel 학습 레벨
 * @param currentLevel 현재 레벨
 * @param aiTutorId AI 튜터 ID
 * @param pushPermissionStatus 푸시 권한 상태
 * @param createdAt 가입 시각
 * @param updatedAt 수정 시각
 */
public record AdminUserProfile(
    Long userProfileId,
    String email,
    String nickname,
    UserRole role,
    UserProfileStatus status,
    Locale targetLocale,
    Locale baseLocale,
    Integer learningLevel,
    int currentLevel,
    Long aiTutorId,
    PushPermissionStatus pushPermissionStatus,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {

  /**
   * 사용자 프로필 엔티티를 관리자 공개 계약으로 변환한다.
   *
   * @param userProfile 사용자 프로필 엔티티
   * @return 관리자 사용자 프로필
   */
  public static AdminUserProfile from(UserProfile userProfile) {
    return new AdminUserProfile(
        userProfile.getId(),
        userProfile.getEmail(),
        userProfile.getNickname(),
        userProfile.getRole(),
        userProfile.getStatus(),
        userProfile.getTargetLocale(),
        userProfile.getBaseLocale(),
        userProfile.getLearningLevel(),
        userProfile.getCurrentLevel(),
        userProfile.getAiTutorId(),
        userProfile.getPushPermissionStatus(),
        userProfile.getCreatedAt(),
        userProfile.getUpdatedAt());
  }
}
