// 관리자 대상 SQL 미리보기의 입력 계약을 정의한다.

package com.landit.landitbe.feature.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 사용자 ID 한 컬럼을 반환하는 읽기 SQL이다.
 *
 * @param sql user_profile_id 컬럼을 반환하는 SELECT 또는 읽기 CTE. 세미콜론 제외
 */
public record AdminPushAudienceQueryRequest(@NotBlank @Size(max = 20000) String sql) {}
