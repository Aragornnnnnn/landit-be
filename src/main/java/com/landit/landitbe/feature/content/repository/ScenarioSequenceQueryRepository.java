// 사용자별 시나리오 진행 순서 조회를 정의한다.

package com.landit.landitbe.feature.content.repository;

import com.landit.landitbe.feature.content.domain.Scenario;
import com.landit.landitbe.feature.content.repository.projection.ScenarioThumbnailProjection;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** 사용자 학습 언어에 맞는 시나리오 진행 순서 조회를 정의한다. */
@Repository
public interface ScenarioSequenceQueryRepository extends JpaRepository<Scenario, Long> {

  /**
   * 사용자 학습 언어에 맞는 시나리오 ID를 ID 오름차순으로 조회한다.
   *
   * @param userId 사용자 ID
   * @return 시나리오 ID 오름차순으로 정렬한 시나리오 ID 목록
   */
  @Query(
      """
            SELECT s.id
            FROM UserProfile up
            JOIN CategoryLanguageVariant clv
              ON clv.baseLocale = up.baseLocale
            JOIN Category c
              ON c.id = clv.categoryId
             AND c.status = com.landit.landitbe.shared.domain.ActiveStatus.ACTIVE
            JOIN Scenario s
              ON s.categoryId = c.id
             AND s.status = com.landit.landitbe.shared.domain.ActiveStatus.ACTIVE
            JOIN ScenarioLanguageVariant slv
              ON slv.scenarioId = s.id
             AND slv.targetLocale = up.targetLocale
             AND slv.baseLocale = up.baseLocale
             AND slv.status = com.landit.landitbe.shared.domain.ActiveStatus.ACTIVE
            WHERE up.id = :userId
            ORDER BY s.id ASC
      """)
  List<Long> findScenarioIdsInIdOrder(@Param("userId") long userId);

  /**
   * 시나리오 ID 목록의 썸네일 URL을 조회한다. 완료 이력 표시가 목적이므로 활성 상태는 필터링하지 않는다.
   *
   * @param scenarioIds 시나리오 ID 목록
   * @return 시나리오 ID와 썸네일 URL projection 목록
   */
  @Query(
      """
            SELECT new com.landit.landitbe.feature.content.repository.projection.ScenarioThumbnailProjection(
                s.id,
                s.thumbnailUrl
            )
            FROM Scenario s
            WHERE s.id IN :scenarioIds
      """)
  List<ScenarioThumbnailProjection> findThumbnailsByScenarioIds(
      @Param("scenarioIds") Collection<Long> scenarioIds);
}
