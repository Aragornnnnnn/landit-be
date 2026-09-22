// 프리톡 사용자 발화의 턴 교정을 저장하고 조회한다.

package com.landit.landitbe.feature.learning.freetalk.feedback.repository;

import com.landit.landitbe.feature.learning.conversation.domain.ProcessingStatus;
import com.landit.landitbe.feature.learning.freetalk.feedback.domain.FreeTalkMessageFeedback;
import com.landit.landitbe.feature.learning.freetalk.feedback.domain.FreeTalkMistakePattern;
import java.util.List;
import java.util.Optional;
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
      @Param("preparingStatus") ProcessingStatus preparingStatus);
}
