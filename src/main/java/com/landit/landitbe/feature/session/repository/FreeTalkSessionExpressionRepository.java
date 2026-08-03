// 프리톡 세션별 개인화 표현을 저장하고 조회한다.

package com.landit.landitbe.feature.session.repository;

import com.landit.landitbe.feature.session.domain.FreeTalkSessionExpression;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 프리톡 세션별 개인화 표현을 저장하고 조회한다. */
public interface FreeTalkSessionExpressionRepository
    extends JpaRepository<FreeTalkSessionExpression, Long> {

  /**
   * 세션에 연결된 표현을 노출 순서대로 조회한다.
   *
   * @param freeTalkSessionId 프리톡 세션 ID
   * @return 세션에 연결된 맞춤 표현 목록
   */
  List<FreeTalkSessionExpression> findByFreeTalkSessionIdOrderByDisplayOrderAsc(
      Long freeTalkSessionId);

  /**
   * 여러 세션에 연결된 표현을 세션과 노출 순서대로 일괄 조회한다.
   *
   * @param freeTalkSessionIds 프리톡 세션 ID 목록
   * @return 세션별 맞춤 표현 목록
   */
  List<FreeTalkSessionExpression>
      findByFreeTalkSessionIdInOrderByFreeTalkSessionIdAscDisplayOrderAsc(
          List<Long> freeTalkSessionIds);

  /**
   * 완료할 프리톡 세션 표현을 동시 요청 직렬화용 잠금과 함께 조회한다.
   *
   * @param freeTalkSessionId 프리톡 세션 ID
   * @param writingExpressionId Writing 표현 ID
   * @return 해당 세션에 연결된 표현
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<FreeTalkSessionExpression> findByFreeTalkSessionIdAndWritingExpressionId(
      Long freeTalkSessionId, Long writingExpressionId);

  /**
   * 사용자가 이전 프리톡에서 추천받은 표현 연결을 일괄 조회한다.
   *
   * @param userProfileId 사용자 프로필 ID
   * @param writingExpressionIds 조회할 Writing 표현 ID 목록
   * @return 사용자의 프리톡에 연결된 표현 목록
   */
  @Query(
      """
      select expression
      from FreeTalkSessionExpression expression, FreeTalkSession freeTalkSession, LearningSession learningSession
      where expression.freeTalkSessionId = freeTalkSession.id
        and freeTalkSession.learningSessionId = learningSession.id
        and learningSession.userProfileId = :userProfileId
        and expression.writingExpressionId in :writingExpressionIds
      """)
  List<FreeTalkSessionExpression> findAllByUserProfileIdAndWritingExpressionIdIn(
      @Param("userProfileId") Long userProfileId,
      @Param("writingExpressionIds") List<Long> writingExpressionIds);

  /**
   * 세션에 연결된 모든 맞춤 표현을 삭제한다.
   *
   * @param freeTalkSessionId 프리톡 세션 ID
   */
  void deleteByFreeTalkSessionId(Long freeTalkSessionId);
}
