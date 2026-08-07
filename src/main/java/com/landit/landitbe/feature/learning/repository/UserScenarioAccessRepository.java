// 사용자별 시나리오 복습 권한을 조회하고 저장한다.

package com.landit.landitbe.feature.learning.repository;

import com.landit.landitbe.feature.learning.domain.UserScenarioAccess;
import com.landit.landitbe.shared.domain.Locale;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 사용자별 시나리오 복습 권한을 조회하고 저장한다. */
public interface UserScenarioAccessRepository extends JpaRepository<UserScenarioAccess, Long> {

  /**
   * 사용자가 대상 언어의 특정 시나리오 복습 권한을 보유하는지 확인한다.
   *
   * @param userProfileId 사용자 프로필 ID
   * @param scenarioId 시나리오 ID
   * @param targetLocale 학습 대상 언어
   * @return 복습 권한 보유 여부
   */
  boolean existsByUserProfileIdAndScenarioIdAndTargetLocale(
      Long userProfileId, Long scenarioId, Locale targetLocale);

  /**
   * 사용자가 학습 대상 언어로 보유한 모든 시나리오 복습 권한을 시나리오 ID 순서로 조회한다.
   *
   * @param userProfileId 사용자 프로필 ID
   * @param targetLocale 학습 대상 언어
   * @return 시나리오 복습 권한 목록
   */
  List<UserScenarioAccess> findAllByUserProfileIdAndTargetLocaleOrderByScenarioIdAsc(
      Long userProfileId, Locale targetLocale);

  /**
   * 사용자가 특정 날짜에 획득한 복습 권한을 오래된 순서로 조회한다.
   *
   * @param userProfileId 사용자 프로필 ID
   * @param targetLocale 학습 대상 언어
   * @param dayStart 날짜 시작 시각
   * @param nextDayStart 다음 날짜 시작 시각
   * @return 해당 날짜의 복습 권한 목록
   */
  @Query(
      """
            SELECT access
            FROM UserScenarioAccess access
            WHERE access.userProfileId = :userProfileId
              AND access.targetLocale = :targetLocale
              AND access.grantedAt >= :dayStart
              AND access.grantedAt < :nextDayStart
            ORDER BY access.grantedAt ASC, access.id ASC
      """)
  List<UserScenarioAccess> findAllGrantedOn(
      @Param("userProfileId") Long userProfileId,
      @Param("targetLocale") Locale targetLocale,
      @Param("dayStart") LocalDateTime dayStart,
      @Param("nextDayStart") LocalDateTime nextDayStart);

  /**
   * 사용자가 해당 날짜에 새 복습 권한을 얻었는지 확인한다.
   *
   * @param userProfileId 사용자 프로필 ID
   * @param targetLocale 학습 대상 언어
   * @param dayStart 날짜 시작 시각
   * @param nextDayStart 다음 날짜 시작 시각
   * @return 해당 날짜에 새 복습 권한을 얻었으면 true
   */
  @Query(
      """
            SELECT COUNT(access) > 0
            FROM UserScenarioAccess access
            WHERE access.userProfileId = :userProfileId
              AND access.targetLocale = :targetLocale
              AND access.grantedAt >= :dayStart
              AND access.grantedAt < :nextDayStart
      """)
  boolean existsGrantedOn(
      @Param("userProfileId") Long userProfileId,
      @Param("targetLocale") Locale targetLocale,
      @Param("dayStart") LocalDateTime dayStart,
      @Param("nextDayStart") LocalDateTime nextDayStart);

  /**
   * 사용자가 구간 안에서 얻은 시나리오 복습 권한을 획득 시각 오름차순으로 조회한다.
   *
   * @param userProfileId 사용자 프로필 ID
   * @param targetLocale 학습 대상 언어
   * @param rangeStart 구간 시작 시각(포함)
   * @param rangeEnd 구간 끝 시각(제외)
   * @return 획득 시각 오름차순으로 정렬한 시나리오 복습 권한 목록
   */
  @Query(
      """
            SELECT access
            FROM UserScenarioAccess access
            WHERE access.userProfileId = :userProfileId
              AND access.targetLocale = :targetLocale
              AND access.grantedAt >= :rangeStart
              AND access.grantedAt < :rangeEnd
            ORDER BY access.grantedAt ASC
      """)
  List<UserScenarioAccess> findAllGrantedBetween(
      @Param("userProfileId") Long userProfileId,
      @Param("targetLocale") Locale targetLocale,
      @Param("rangeStart") LocalDateTime rangeStart,
      @Param("rangeEnd") LocalDateTime rangeEnd);

  /**
   * 사용자가 대상 언어에서 가장 먼저 얻은 시나리오 복습 권한을 조회한다.
   *
   * @param userProfileId 사용자 프로필 ID
   * @param targetLocale 학습 대상 언어
   * @return 획득 시각이 가장 이른 시나리오 복습 권한. 없으면 빈 값
   */
  Optional<UserScenarioAccess> findTopByUserProfileIdAndTargetLocaleOrderByGrantedAtAsc(
      Long userProfileId, Locale targetLocale);

  /**
   * 이전 날짜에 시작했지만 완료하지 못한 시나리오 세션이 있는지 확인한다.
   *
   * @param userProfileId 사용자 프로필 ID
   * @param scenarioId 시나리오 ID
   * @param targetLocale 학습 대상 언어
   * @param todayStart 오늘 시작 시각
   * @return 이전 날짜의 미완료 세션이 있으면 true
   */
  @Query(
      """
            SELECT COUNT(learningSession) > 0
            FROM LearningSession learningSession
            JOIN ScenarioSession scenarioSession
              ON scenarioSession.learningSessionId = learningSession.id
            JOIN ScenarioLanguageVariant variant
              ON variant.id = scenarioSession.scenarioLanguageVariantId
            WHERE learningSession.userProfileId = :userProfileId
              AND variant.scenarioId = :scenarioId
              AND learningSession.targetLocale = :targetLocale
              AND learningSession.status IN (
                  com.landit.landitbe.feature.session.domain.LearningSessionStatus.IN_PROGRESS,
                  com.landit.landitbe.feature.session.domain.LearningSessionStatus.INTERRUPTED
              )
              AND learningSession.startedAt < :todayStart
      """)
  boolean existsUncompletedSessionBefore(
      @Param("userProfileId") Long userProfileId,
      @Param("scenarioId") Long scenarioId,
      @Param("targetLocale") Locale targetLocale,
      @Param("todayStart") LocalDateTime todayStart);
}
