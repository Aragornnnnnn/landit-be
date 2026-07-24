// 복습 날짜와 상태를 기준으로 복습 문항과 대상 사용자를 조회한다.

package com.landit.landitbe.feature.learning.repository;

import com.landit.landitbe.feature.learning.domain.ReviewItem;
import com.landit.landitbe.feature.learning.domain.ReviewItemStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 복습 날짜와 상태를 기준으로 복습 문항과 대상 사용자를 조회한다. */
public interface ReviewItemRepository extends JpaRepository<ReviewItem, Long> {

  /**
   * 지정한 날짜와 상태의 복습 문항을 가진 사용자 ID를 중복 없이 조회한다.
   *
   * @param reviewDate 복습 기준 날짜
   * @param status 복습 문항 상태
   * @return 조건을 만족한 사용자 ID 목록
   */
  @Query(
      """
      select distinct reviewItem.userProfileId
      from ReviewItem reviewItem
      where reviewItem.reviewDate = :reviewDate
        and reviewItem.status = :status
      order by reviewItem.userProfileId
      """)
  List<Long> findDistinctUserProfileIds(
      @Param("reviewDate") LocalDate reviewDate, @Param("status") ReviewItemStatus status);
}
