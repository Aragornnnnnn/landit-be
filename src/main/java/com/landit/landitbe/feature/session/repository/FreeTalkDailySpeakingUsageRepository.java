// 프리톡 일일 발화 사용량을 잠금 조회한다.

package com.landit.landitbe.feature.session.repository;

import com.landit.landitbe.feature.session.domain.FreeTalkDailySpeakingUsage;
import com.landit.landitbe.feature.session.domain.FreeTalkDailySpeakingUsage.DailyUsageId;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 프리톡 일일 발화 사용량을 잠금 조회한다. */
public interface FreeTalkDailySpeakingUsageRepository
    extends JpaRepository<FreeTalkDailySpeakingUsage, DailyUsageId> {

  /** 사용자와 날짜의 사용량을 발화 예약용으로 잠금 조회한다. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select usage from FreeTalkDailySpeakingUsage usage "
          + "where usage.id.userProfileId = :userProfileId and usage.id.usageDate = :usageDate")
  Optional<FreeTalkDailySpeakingUsage> findByUserProfileIdAndUsageDateForUpdate(
      @Param("userProfileId") long userProfileId, @Param("usageDate") LocalDate usageDate);
}
