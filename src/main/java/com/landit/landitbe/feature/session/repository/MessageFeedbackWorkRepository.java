// 메시지 평가 작업의 만료 임대와 완료 결과를 원자적으로 갱신한다.

package com.landit.landitbe.feature.session.repository;

import com.landit.landitbe.feature.session.domain.MessageFeedbackWork;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/** 메시지 평가 작업의 저장소다. */
public interface MessageFeedbackWorkRepository extends JpaRepository<MessageFeedbackWork, Long> {
  /** 재시도 시각과 실행 임대가 지난 미완료 작업만 조회한다. */
  @Query(
      """
      select w from MessageFeedbackWork w where w.resultPayload is null
      and w.legacyCompleted = false and w.terminalFailed = false
      and w.attempts < 3 and w.availableAt <= :now
      and (w.leaseUntil is null or w.leaseUntil <= :now) order by w.availableAt
      """)
  List<MessageFeedbackWork> findRecoverable(LocalDateTime now, Pageable page);

  /** 한 인스턴스만 만료된 작업을 맡도록 실행 토큰을 교체한다. */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
      update MessageFeedbackWork w set w.attemptToken = :token, w.leaseUntil = :until,
      w.attempts = w.attempts + 1 where w.messageId = :id and w.resultPayload is null
      and w.legacyCompleted = false and w.terminalFailed = false
      and w.attempts < 3 and w.availableAt <= :now
      and (w.leaseUntil is null or w.leaseUntil <= :now)
      """)
  int claim(long id, String token, LocalDateTime now, LocalDateTime until);

  /** 현재 시도의 결과만 반영해 늦게 도착한 응답이 덮어쓰지 못하게 한다. */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
      update MessageFeedbackWork w set w.resultPayload = :result, w.leaseUntil = null,
      w.legacyCompleted = :legacy, w.terminalFailed = :failed, w.availableAt = :retryAt where w.messageId = :id and w.attemptToken = :token
      and w.resultPayload is null
      """)
  int finish(
      long id, String token, String result, boolean legacy, boolean failed, LocalDateTime retryAt);

  /** 마지막 시도 도중 프로세스가 종료된 작업을 찾아 무기한 준비 상태를 끝낸다. */
  @Query(
      """
      select w from MessageFeedbackWork w where w.resultPayload is null
      and w.legacyCompleted = false and w.terminalFailed = false and w.attempts >= 3
      and w.leaseUntil <= :now
      """)
  List<MessageFeedbackWork> findExhausted(LocalDateTime now, Pageable page);

  /** 결과가 유실된 기존 요청을 사용자 재조회에서 재예약한다. 활성 시도는 유지한다. */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
      update MessageFeedbackWork w set w.attempts = 0, w.legacyCompleted = false,
      w.terminalFailed = false, w.availableAt = :now where w.sessionId = :sessionId
      and w.resultPayload is null and (w.leaseUntil is null or w.leaseUntil <= :now)
      """)
  int retryMissing(long sessionId, LocalDateTime now);
}
