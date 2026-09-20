// 프리톡 세션 요약 파생 데이터를 저장하고 선점 조회한다.

package com.landit.landitbe.feature.learning.freetalk.context.repository;

import com.landit.landitbe.feature.learning.freetalk.context.domain.FreeTalkContextSummary;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

/** 세션당 하나의 컨텍스트 요약 상태를 저장한다. */
public interface FreeTalkContextSummaryRepository
    extends JpaRepository<FreeTalkContextSummary, Long> {

  /** 외부 호출 전 상태를 잠그고 선점할 수 있도록 조회한다. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select summary from FreeTalkContextSummary summary "
          + "where summary.freeTalkSessionId = :freeTalkSessionId")
  Optional<FreeTalkContextSummary> findByIdForUpdate(Long freeTalkSessionId);

  /**
   * 잠금 대기를 포함한 트랜잭션 시작 시각 대신 PostgreSQL의 현재 시각을 읽는다.
   *
   * @return DB 시각의 절대 시점
   */
  @Query(value = "select clock_timestamp()", nativeQuery = true)
  java.time.Instant currentTime();
}
