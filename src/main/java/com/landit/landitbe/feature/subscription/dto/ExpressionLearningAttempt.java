// 표현 학습 시도의 식별자와 유효 시각을 전달한다.

package com.landit.landitbe.feature.subscription.dto;

import com.landit.landitbe.feature.subscription.domain.LearningAccessGrant;
import java.time.LocalDateTime;

/**
 * 표현 학습 시도의 식별자와 유효 시각을 전달한다.
 *
 * @param id 학습 시도 ID
 * @param expiresAt 완료 권한 만료 시각
 */
public record ExpressionLearningAttempt(String id, LocalDateTime expiresAt) {
  /**
   * 저장된 권한에서 공개할 학습 시도 값을 복사한다.
   *
   * @param grant 저장된 학습 권한
   * @return 학습 시도 정보
   */
  public static ExpressionLearningAttempt from(LearningAccessGrant grant) {
    return new ExpressionLearningAttempt(grant.getId(), grant.getExpiresAt());
  }
}
