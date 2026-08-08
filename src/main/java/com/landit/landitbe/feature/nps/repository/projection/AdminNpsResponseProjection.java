// 관리자 NPS 목록과 작성자 정보를 담는 조회 projection이다.

package com.landit.landitbe.feature.nps.repository.projection;

import java.time.LocalDateTime;

/**
 * 관리자 NPS 목록과 작성자 정보를 담는 조회 projection이다.
 *
 * @param npsResponseId NPS 응답 ID
 * @param score 점수
 * @param opinionText 의견
 * @param submittedAt 제출 시각
 * @param userProfileId 사용자 프로필 ID
 * @param userEmail 사용자 이메일
 * @param userNickname 사용자 닉네임
 */
public record AdminNpsResponseProjection(
    Long npsResponseId,
    int score,
    String opinionText,
    LocalDateTime submittedAt,
    Long userProfileId,
    String userEmail,
    String userNickname) {}
