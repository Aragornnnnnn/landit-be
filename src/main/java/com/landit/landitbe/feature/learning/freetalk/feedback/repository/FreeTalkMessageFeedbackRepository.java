// 프리톡 사용자 발화의 턴 교정을 저장하고 조회한다.

package com.landit.landitbe.feature.learning.freetalk.feedback.repository;

import com.landit.landitbe.feature.learning.conversation.domain.ProcessingStatus;
import com.landit.landitbe.feature.learning.freetalk.feedback.domain.FreeTalkMessageFeedback;
import com.landit.landitbe.feature.learning.freetalk.feedback.domain.FreeTalkMistakePattern;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 프리톡 사용자 발화의 턴 교정을 저장하고 조회한다. */
public interface FreeTalkMessageFeedbackRepository
    extends JpaRepository<FreeTalkMessageFeedback, Long> {

  /**
   * 사용자 발화 하나의 교정을 조회한다.
   *
   * @param sessionHistoryMessageId 교정 대상 사용자 발화 ID
   * @return 그 발화의 교정. 아직 준비한 적이 없으면 비어 있다
   */
  Optional<FreeTalkMessageFeedback> findBySessionHistoryMessageId(Long sessionHistoryMessageId);

  /**
   * 대화 기록 하나의 교정을 한 번에 조회한다.
   *
   * @param sessionHistoryId 대화 기록 ID
   * @return 그 대화의 사용자 발화별 교정 목록
   */
  List<FreeTalkMessageFeedback> findBySessionHistoryId(Long sessionHistoryId);

  /**
   * 준비 상태인 교정에만 값을 한 번에 반영한다. 같은 발화에 두 번 호출해도 한 번만 저장된다.
   *
   * @return 갱신된 row 수. 이미 판정이 끝난 교정이면 0
   */
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      """
            update FreeTalkMessageFeedback feedback
            set feedback.processingStatus = :status,
                feedback.reactedToPartner = :reactedToPartner,
                feedback.originalSentence = :originalSentence,
                feedback.betterSentence = :betterSentence,
                feedback.reason = :reason,
                feedback.mistakePattern = :mistakePattern,
                feedback.memoryId = :memoryId,
                feedback.memoryObservedOn = :memoryObservedOn,
                feedback.memoryLabel = :memoryLabel,
                feedback.leaseUntil = null,
                feedback.attemptToken = null,
                feedback.updatedAt = CURRENT_TIMESTAMP
            where feedback.sessionHistoryMessageId = :messageId
              and feedback.processingStatus = :preparingStatus
      """)
  int updateIfPreparing(
      @Param("messageId") long messageId,
      @Param("status") ProcessingStatus status,
      @Param("reactedToPartner") Boolean reactedToPartner,
      @Param("originalSentence") String originalSentence,
      @Param("betterSentence") String betterSentence,
      @Param("reason") String reason,
      @Param("mistakePattern") FreeTalkMistakePattern mistakePattern,
      @Param("memoryId") Long memoryId,
      @Param("memoryObservedOn") LocalDate memoryObservedOn,
      @Param("memoryLabel") String memoryLabel,
      @Param("preparingStatus") ProcessingStatus preparingStatus);

  /**
   * 실패한 시도를 끝내고 다음 시도를 기다리게 한다. 준비 상태는 그대로 두고 다음 시도 시각만 적는다.
   *
   * <p>그 시도를 시작한 쪽만 반영되도록 시도 순번과 선점 식별자가 같을 때만 갱신한다. 늦게 끝난 옛 시도가 이미 넘겨받은 새 시도의 임대를 풀지 못하게 한다.
   *
   * @param attemptToken 그 시도의 선점 식별자. 첫 시도는 빈 문자열
   * @return 갱신된 row 수. 이미 끝났거나 다른 시도가 넘겨받았으면 0
   */
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      """
            update FreeTalkMessageFeedback feedback
            set feedback.leaseUntil = :nextAttemptAt,
                feedback.attemptToken = null,
                feedback.updatedAt = CURRENT_TIMESTAMP
            where feedback.sessionHistoryMessageId = :messageId
              and feedback.processingStatus = :preparingStatus
              and feedback.attempts = :attempts
              and coalesce(feedback.attemptToken, '') = :attemptToken
      """)
  int releaseForRetry(
      @Param("messageId") long messageId,
      @Param("attempts") int attempts,
      @Param("attemptToken") String attemptToken,
      @Param("nextAttemptAt") LocalDateTime nextAttemptAt,
      @Param("preparingStatus") ProcessingStatus preparingStatus);

  /**
   * 더 시도하지 않는 교정을 실패로 확정한다. 그 시도를 시작한 쪽만 확정할 수 있다.
   *
   * @param attemptToken 그 시도의 선점 식별자. 첫 시도는 빈 문자열
   * @return 갱신된 row 수. 이미 끝났거나 다른 시도가 넘겨받았으면 0
   */
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      """
            update FreeTalkMessageFeedback feedback
            set feedback.processingStatus = :failedStatus,
                feedback.leaseUntil = null,
                feedback.attemptToken = null,
                feedback.updatedAt = CURRENT_TIMESTAMP
            where feedback.sessionHistoryMessageId = :messageId
              and feedback.processingStatus = :preparingStatus
              and feedback.attempts = :attempts
              and coalesce(feedback.attemptToken, '') = :attemptToken
      """)
  int failAttempt(
      @Param("messageId") long messageId,
      @Param("attempts") int attempts,
      @Param("attemptToken") String attemptToken,
      @Param("failedStatus") ProcessingStatus failedStatus,
      @Param("preparingStatus") ProcessingStatus preparingStatus);

  /** 복구 대상 교정의 발화 ID와 지금까지의 시도 횟수다. */
  interface RecoverableCorrection {
    /** 교정 대상 사용자 발화 ID. */
    Long getSessionHistoryMessageId();

    /** 지금까지 AI에 교정을 요청한 횟수. */
    int getAttempts();
  }

  /**
   * 준비 상태이면서 임대가 끝난 교정을 임대가 먼저 끝난 순으로 찾는다. 임대가 없는 행은 시도 정보가 생기기 전부터 준비 상태로 남은 것이라 끝난 것으로 보되, 방금 멈춘
   * 교정이 그 뒤로 밀리지 않도록 맨 뒤에 둔다.
   *
   * @param now 애플리케이션 Clock 기준 현재 시각
   * @param pageable 한 번에 넘겨받을 교정 수
   * @return 복구 대상 교정
   */
  @Query(
      """
            select feedback.sessionHistoryMessageId as sessionHistoryMessageId,
                   feedback.attempts as attempts
            from FreeTalkMessageFeedback feedback
            where feedback.processingStatus = :preparingStatus
              and (feedback.leaseUntil is null or feedback.leaseUntil < :now)
            order by feedback.leaseUntil asc nulls last, feedback.id asc
      """)
  List<RecoverableCorrection> findRecoverable(
      @Param("now") LocalDateTime now,
      @Param("preparingStatus") ProcessingStatus preparingStatus,
      Pageable pageable);

  /**
   * 임대가 끝난 교정의 다음 시도를 선점한다. 여러 서버가 같은 교정을 집어도 한 곳만 성공한다.
   *
   * @param attempts 찾았을 때의 시도 횟수. 그사이 다른 쪽이 넘겨받았으면 달라져 있다
   * @param attemptToken 이번 시도의 선점 식별자
   * @param leaseUntil 이번 시도의 응답을 기다려 줄 시각
   * @return 선점했으면 1, 다른 쪽이 먼저 가져갔거나 이미 끝났으면 0
   */
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      """
            update FreeTalkMessageFeedback feedback
            set feedback.attempts = feedback.attempts + 1,
                feedback.attemptToken = :attemptToken,
                feedback.leaseUntil = :leaseUntil,
                feedback.updatedAt = CURRENT_TIMESTAMP
            where feedback.sessionHistoryMessageId = :messageId
              and feedback.processingStatus = :preparingStatus
              and feedback.attempts = :attempts
              and (feedback.leaseUntil is null or feedback.leaseUntil < :now)
      """)
  int claim(
      @Param("messageId") long messageId,
      @Param("attempts") int attempts,
      @Param("attemptToken") String attemptToken,
      @Param("leaseUntil") LocalDateTime leaseUntil,
      @Param("now") LocalDateTime now,
      @Param("preparingStatus") ProcessingStatus preparingStatus);

  /**
   * 시도를 다 쓴 채 임대가 끝난 교정을 실패로 확정한다. 마지막 시도를 하던 서버가 결과를 남기지 못하고 사라진 경우다.
   *
   * <p>그 시도의 선점 식별자를 알 수 없으므로 임대가 끝났다는 사실로 확정한다.
   *
   * @return 확정했으면 1, 그사이 끝났거나 다른 쪽이 넘겨받았으면 0
   */
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      """
            update FreeTalkMessageFeedback feedback
            set feedback.processingStatus = :failedStatus,
                feedback.leaseUntil = null,
                feedback.attemptToken = null,
                feedback.updatedAt = CURRENT_TIMESTAMP
            where feedback.sessionHistoryMessageId = :messageId
              and feedback.processingStatus = :preparingStatus
              and feedback.attempts = :attempts
              and (feedback.leaseUntil is null or feedback.leaseUntil < :now)
      """)
  int failExpired(
      @Param("messageId") long messageId,
      @Param("attempts") int attempts,
      @Param("now") LocalDateTime now,
      @Param("failedStatus") ProcessingStatus failedStatus,
      @Param("preparingStatus") ProcessingStatus preparingStatus);
}
