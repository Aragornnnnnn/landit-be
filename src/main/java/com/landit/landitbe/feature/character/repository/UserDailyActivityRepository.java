// 사용자별 날짜 단위 학습 활동을 조회하고 저장한다.

package com.landit.landitbe.feature.character.repository;

import com.landit.landitbe.feature.character.domain.UserDailyActivity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 사용자별 날짜 단위 학습 활동을 조회하고 저장한다. */
public interface UserDailyActivityRepository extends JpaRepository<UserDailyActivity, Long> {

  /**
   * 사용자의 특정 날짜 활동을 조회한다.
   *
   * @param userProfileId 사용자 프로필 ID
   * @param activityDate 활동 날짜
   * @return 해당 날짜의 활동. 없으면 빈 값
   */
  Optional<UserDailyActivity> findByUserProfileIdAndActivityDate(
      Long userProfileId, LocalDate activityDate);

  /**
   * 사용자의 최초 활성 학습일을 조회한다.
   *
   * @param userProfileId 사용자 프로필 ID
   * @return 최초 활성 학습일. 없으면 빈 값
   */
  Optional<UserDailyActivity> findFirstByUserProfileIdAndActiveDayTrueOrderByActivityDateAsc(
      Long userProfileId);

  /**
   * 사용자의 활성 학습일 수를 조회한다.
   *
   * @param userProfileId 사용자 프로필 ID
   * @return 활성 학습일 수
   */
  long countByUserProfileIdAndActiveDayTrue(Long userProfileId);

  /**
   * 요청한 월 범위의 활동을 날짜 오름차순으로 조회한다.
   *
   * @param userProfileId 사용자 프로필 ID
   * @param startDate 월 시작일
   * @param endDate 다음 달 시작일
   * @return 요청 범위의 활동 목록
   */
  @Query(
      """
      SELECT activity
      FROM UserDailyActivity activity
      WHERE activity.userProfileId = :userProfileId
        AND activity.activeDay = true
        AND activity.activityDate >= :startDate
        AND activity.activityDate < :endDate
      ORDER BY activity.activityDate ASC
      """)
  List<UserDailyActivity> findAllActiveInDateRange(
      @Param("userProfileId") Long userProfileId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);
}
