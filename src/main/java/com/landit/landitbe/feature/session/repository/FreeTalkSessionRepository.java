// 프리톡 세션 엔티티를 저장하고 조회한다.

package com.landit.landitbe.feature.session.repository;

import com.landit.landitbe.feature.session.domain.ExpressionGenerationStatus;
import com.landit.landitbe.feature.session.domain.FreeTalkSession;
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
   * 애플리케이션 중단으로 표현 생성이 끝나지 않은 세션을 조회한다.
   *
   * @param expressionGenerationStatus 조회할 표현 생성 상태
   * @return 준비 중인 프리톡 세션 목록
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
            and learningSession.status = com.landit.landitbe.feature.session.domain.LearningSessionStatus.COMPLETED
            and freeTalkSession.conversationStatus = com.landit.landitbe.feature.session.domain.FreeTalkConversationStatus.COMPLETED
          order by learningSession.endedAt desc
      """)
  Page<FreeTalkSession> findCompletedByUserProfileId(
      @Param("userProfileId") Long userProfileId, Pageable pageable);
}
