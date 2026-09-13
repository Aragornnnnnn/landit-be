// 수준 평가 시 잠근 사용자 프로필의 적용 상태를 전달한다.

package com.landit.landitbe.feature.profile.dto;

import java.time.LocalDateTime;

/**
 * 평가 결과의 최신성과 연속 승급을 판단하는 불변 상태다.
 *
 * @param learningLevel 현재 수준
 * @param promotionStreak 연속 승급 근거 횟수
 * @param updatedAt 마지막 수준 변경 시각
 */
public record UserLearningAssessmentState(
    Integer learningLevel, int promotionStreak, LocalDateTime updatedAt) {}
