// 사용자가 완료한 표현 ID 집합을 다른 기능에 전달한다.

package com.landit.landitbe.feature.learning.dto;

import com.landit.landitbe.feature.learning.domain.UserWritingExpressionCompletion;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 사용자가 완료한 표현 ID 집합을 다른 기능에 전달한다.
 *
 * @param values 완료한 표현 ID 집합
 */
public record CompletedExpressionIds(Set<Long> values) {

  /** 완료한 표현 ID 집합을 불변 값으로 보관한다. */
  public CompletedExpressionIds {
    values = Set.copyOf(values);
  }

  /**
   * 표현 완료 엔티티 목록을 공개 조회 결과로 변환한다.
   *
   * @param completions 표현 완료 엔티티 목록
   * @return 완료한 표현 ID 집합
   */
  public static CompletedExpressionIds from(List<UserWritingExpressionCompletion> completions) {
    return new CompletedExpressionIds(
        completions.stream()
            .map(UserWritingExpressionCompletion::getWritingExpressionId)
            .collect(Collectors.toSet()));
  }
}
