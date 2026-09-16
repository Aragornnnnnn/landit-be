// 복습 스냅샷과 진행·제출 이력을 JDBC로 저장한다.

package com.landit.landitbe.feature.learning.review.repository;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 복습만 쓰며 다른 업무의 읽기 JOIN은 아키텍처 문서에 명시한다. */
@Repository
@RequiredArgsConstructor
public class ExpressionReviewRepository {
  private final JdbcTemplate jdbc;

  /**
   * 완료 이력이 있는 활성 사용자를 커서로 조회한다.
   *
   * @param after 직전 사용자 ID
   * @param size 최대 사용자 수
   * @return 오름차순 사용자 ID
   */
  public List<Long> candidateUsers(long after, int size) {
    return jdbc.query(
        """
        select p.id from user_profile p where p.id > ? and p.status = 'ACTIVE'
          and exists (select 1 from user_writing_expression_completion c
                      where c.user_profile_id = p.id)
        order by p.id limit ?
        """,
        (rs, row) -> rs.getLong(1),
        after,
        size);
  }

  /**
   * 언어·활성 상태와 최근 학습·복습·출제를 확인해 오래된 후보를 읽는다.
   *
   * @param userId 소유 사용자
   * @param cutoff 최근 제외 기간 경계
   * @return 오래된 순서의 최대 30개 표현 ID
   */
  public List<Long> candidateExpressions(long userId, LocalDateTime cutoff) {
    return jdbc.query(
        """
        select c.writing_expression_id
        from user_writing_expression_completion c
        join writing_expression w on w.id = c.writing_expression_id
        join user_profile p on p.id = c.user_profile_id
        left join (
          select q.expression_id, max(r.created_at) as last_offered,
                 max(q.completed_at) as last_reviewed
          from expression_review_question q join expression_review r on r.id = q.review_id
          where r.user_profile_id = ? group by q.expression_id
        ) h on h.expression_id = w.id
        where c.user_profile_id = ? and w.status = 'ACTIVE'
          and w.target_locale = p.target_locale and w.base_locale = p.base_locale
          and (h.last_offered is null or h.last_offered <= ?)
          and (h.last_reviewed is null or h.last_reviewed <= ?)
        group by c.writing_expression_id
        having max(c.last_completed_at) <= ?
        order by max(case when h.last_reviewed > c.last_completed_at
                          then h.last_reviewed else c.last_completed_at end),
                 c.writing_expression_id
        limit 30
        """,
        (rs, row) -> rs.getLong(1),
        userId,
        userId,
        cutoff,
        cutoff,
        cutoff);
  }
}
