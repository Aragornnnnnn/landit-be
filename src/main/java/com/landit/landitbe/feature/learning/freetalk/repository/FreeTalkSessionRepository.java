// 프리톡 세션 엔티티를 저장하고 조회한다.

package com.landit.landitbe.feature.learning.freetalk.repository;

import com.landit.landitbe.feature.learning.freetalk.domain.FreeTalkSession;
import com.landit.landitbe.feature.learning.freetalk.expression.domain.ExpressionGenerationStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 프리톡 세션 엔티티를 저장하고 조회한다. */
public interface FreeTalkSessionRepository extends JpaRepository<FreeTalkSession, Long> {

  /**
   * 학습 세션에 연결된 프리톡 세션을 조회한다.
   *
   * @param learningSessionId 학습 세션 ID
   * @return 연결된 프리톡 세션, 없으면 빈 Optional
   */
  Optional<FreeTalkSession> findByLearningSessionId(Long learningSessionId);

  /**
   * 학습 세션에 연결된 프리톡 세션을 상태 변경용으로 잠금 조회한다.
   *
   * @param learningSessionId 학습 세션 ID
   * @return 잠금을 획득한 프리톡 세션, 없으면 빈 Optional
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select session from FreeTalkSession session "
          + "where session.learningSessionId = :learningSessionId")
  Optional<FreeTalkSession> findByLearningSessionIdForUpdate(
      @Param("learningSessionId") Long learningSessionId);

  /**
   * 지정한 표현 생성 상태의 프리톡 세션을 조회한다.
   *
   * @param expressionGenerationStatus 조회할 표현 생성 상태
   * @return 지정한 상태의 프리톡 세션 목록
   */
  List<FreeTalkSession> findByExpressionGenerationStatus(
      ExpressionGenerationStatus expressionGenerationStatus);

  /**
   * 사용자가 소유한 완료 프리톡을 완료 시각 내림차순으로 조회한다.
   *
   * @param userProfileId 사용자 프로필 ID
   * @param pageable 페이지 요청 정보
   * @return 완료된 프리톡 세션 페이지
   */
  @Query(
      """
          select freeTalkSession
          from FreeTalkSession freeTalkSession, LearningSession learningSession
          where learningSession.id = freeTalkSession.learningSessionId
            and learningSession.userProfileId = :userProfileId
            and learningSession.status = com.landit.landitbe.feature.learning.conversation.domain.LearningSessionStatus.COMPLETED
            and freeTalkSession.conversationStatus = com.landit.landitbe.feature.learning.freetalk.domain.FreeTalkConversationStatus.COMPLETED
          order by learningSession.endedAt desc
      """)
  Page<FreeTalkSession> findCompletedByUserProfileId(
      @Param("userProfileId") Long userProfileId, Pageable pageable);

  /**
   * 어느 세션의 직전 완료 프리톡을 조회한다. 같은 사용자의 완료 프리톡 중 그 세션이 시작하기 전에 끝난 것을 완료 시각 내림차순으로 돌려준다.
   *
   * <p>기준 세션이 진행 중이든 끝났든 같은 결과가 나오도록 "그 세션의 시작 시각보다 먼저 끝난" 것으로 정한다. 교정을 다시 요청할 때와 요약을 만들 때가 첫 시도와
   * 같은 직전 세션을 보게 하기 위함이다. 사용자는 기준 세션에서 읽으므로 따로 받지 않는다.
   *
   * @param learningSessionId 기준이 되는 프리톡 학습 세션 ID
   * @param pageable 페이지 요청 정보. 직전 하나만 필요하면 크기 1
   * @return 직전 완료 프리톡 세션 페이지. 첫 프리톡이면 비어 있다
   */
  @Query(
      """
          select freeTalkSession
          from FreeTalkSession freeTalkSession, LearningSession learningSession, LearningSession current
          where current.id = :learningSessionId
            and learningSession.id = freeTalkSession.learningSessionId
            and learningSession.id <> current.id
            and learningSession.userProfileId = current.userProfileId
            and learningSession.status = com.landit.landitbe.feature.learning.conversation.domain.LearningSessionStatus.COMPLETED
            and freeTalkSession.conversationStatus = com.landit.landitbe.feature.learning.freetalk.domain.FreeTalkConversationStatus.COMPLETED
            and learningSession.endedAt <= current.startedAt
          order by learningSession.endedAt desc, learningSession.id desc
      """)
  Page<FreeTalkSession> findPreviousCompleted(
      @Param("learningSessionId") Long learningSessionId, Pageable pageable);
}
