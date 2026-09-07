// 최초 완료 시나리오의 질문 수준과 평가 후 적용 수준을 조회한다.

package com.landit.landitbe.feature.session.repository.projection;

import java.time.LocalDateTime;

/** 최초 완료 세션의 콘텐츠 수준 조회 결과다. */
public interface CompletedScenarioLevelProjection {
  /**
   * 질문 그룹을 반환한다.
   *
   * @return 최초 완료 당시 질문 그룹
   */
  String getQuestionLevelGroup();

  /**
   * 평가 후 적용 수준을 반환한다.
   *
   * @return 최초 완료 평가 이후 적용 수준. 평가가 없으면 null
   */
  Integer getCurrentLevel();

  /**
   * 완료 시각을 반환한다.
   *
   * @return 최초 완료 시각
   */
  LocalDateTime getEndedAt();
}
