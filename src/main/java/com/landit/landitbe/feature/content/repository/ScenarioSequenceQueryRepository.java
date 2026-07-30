// 사용자별 시나리오 진행 순서 조회를 정의한다.

package com.landit.landitbe.feature.content.repository;

import com.landit.landitbe.feature.content.domain.Scenario;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** 사용자 학습 언어에 맞는 시나리오 진행 순서 조회를 정의한다. */
@Repository
public interface ScenarioSequenceQueryRepository extends JpaRepository<Scenario, Long> {

  /**
   * 사용자 학습 언어에 맞는 시나리오 ID를 노출 순서대로 조회한다.
   *
   * @param userId 사용자 ID
   * @return 카테고리와 시나리오 노출 순서로 정렬한 시나리오 ID 목록
   */
  @Query(
      """
            SELECT s.id
            FROM UserProfile up
            JOIN Scenario s ON 1 = 1
            JOIN Category c ON c.id = s.categoryId
            JOIN ScenarioLanguageVariant slv
              ON slv.scenarioId = s.id
             AND slv.targetLocale = up.targetLocale
             AND slv.baseLocale = up.baseLocale
            WHERE up.id = :userId
            ORDER BY c.displayOrder ASC, s.displayOrder ASC
      """)
  List<Long> findScenarioIdsInDisplayOrder(@Param("userId") long userId);
}
