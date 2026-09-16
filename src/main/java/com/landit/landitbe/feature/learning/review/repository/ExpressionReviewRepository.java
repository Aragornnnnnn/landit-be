// 복습 스냅샷과 진행·제출 이력을 JDBC로 저장한다.

package com.landit.landitbe.feature.learning.review.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.landitbe.feature.content.expression.practice.dto.WritingSentenceResponse;
import com.landit.landitbe.feature.learning.review.domain.ExpressionReview;
import com.landit.landitbe.feature.learning.review.domain.ReviewSubmission;
import com.landit.landitbe.feature.learning.review.dto.ReviewAnswerRequest;
import com.landit.landitbe.feature.learning.review.dto.ReviewQuestion;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 복습만 쓰며 다른 업무의 읽기 JOIN은 아키텍처 문서에 명시한다. */
@Repository
@RequiredArgsConstructor
public class ExpressionReviewRepository {
  private final JdbcTemplate jdbc;
  private final ObjectMapper mapper = new ObjectMapper();

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
   * @param offset 이미 확인한 후보 수
   * @return 오래된 순서의 최대 30개 표현 ID
   */
  public List<Long> candidateExpressions(long userId, LocalDateTime cutoff, int offset) {
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
        limit 30 offset ?
        """,
        (rs, row) -> rs.getLong(1),
        userId,
        userId,
        cutoff,
        cutoff,
        cutoff,
        offset);
  }

  /**
   * 같은 날 생성한 복습을 조회한다.
   *
   * @param userId 사용자 ID
   * @param date 배치 날짜
   * @return 기존 복습
   */
  public Optional<ExpressionReview> findScheduled(long userId, LocalDate date) {
    return jdbc
        .query(
            "select * from expression_review where user_profile_id = ? and scheduled_date = ?",
            this::review,
            userId,
            date)
        .stream()
        .findFirst();
  }

  /**
   * 소유자에 묶인 복습을 조회한다. 쓰기는 호출자가 사용자 잠금 후 수행한다.
   *
   * @param userId 사용자 ID
   * @param id 복습 ID
   * @return 소유한 복습
   */
  public Optional<ExpressionReview> findOwned(long userId, UUID id) {
    return jdbc
        .query(
            "select * from expression_review where user_profile_id = ? and id = ?",
            this::review,
            userId,
            id)
        .stream()
        .findFirst();
  }

  /**
   * 최근 알림 생성이 있어 간격을 지켜야 하는지 확인한다.
   *
   * @param userId 사용자 ID
   * @param cutoff 간격 경계
   * @return 최근 복습이 있으면 true
   */
  public boolean recentlyOffered(long userId, LocalDateTime cutoff) {
    return jdbc.queryForObject(
            "select count(*) from expression_review where user_profile_id = ? and created_at > ?",
            Long.class,
            userId,
            cutoff)
        > 0;
  }

  /**
   * 복습과 고정 문제를 함께 저장한다.
   *
   * @param review 새 복습
   * @param questions 고정 문제
   */
  public void insert(ExpressionReview review, List<ReviewQuestion> questions) {
    jdbc.update(
        """
        insert into expression_review(id, user_profile_id, scheduled_date, created_at, available_until)
        values (?, ?, ?, ?, ?)
        """,
        review.id(),
        review.userId(),
        review.scheduledDate(),
        review.createdAt(),
        review.availableUntil());
    for (ReviewQuestion q : questions) {
      jdbc.update(
          """
          insert into expression_review_question
          (id, review_id, expression_id, target_expression_text, base_expression_meaning_text,
           quiz_json, display_order, queue_order, wrong_count)
          values (?, ?, ?, ?, ?, ?, ?, ?, 0)
          """,
          q.questionId(),
          review.id(),
          q.expressionId(),
          q.targetExpressionText(),
          q.baseExpressionMeaningText(),
          json(q.quiz()),
          q.displayOrder(),
          q.queueOrder());
    }
  }

  /**
   * 최초 시작과 서버가 정한 만료 시각을 저장한다.
   *
   * @param id 복습 ID
   * @param now 시작 시각
   * @param expiresAt 진행 기한
   */
  public void start(UUID id, LocalDateTime now, LocalDateTime expiresAt) {
    jdbc.update(
        "update expression_review set started_at = ?, expires_at = ?"
            + " where id = ? and started_at is null",
        now,
        expiresAt,
        id);
  }

  /**
   * 최초 출제 순서로 고정 문제와 현재 상태를 조회한다.
   *
   * @param id 복습 ID
   * @return 문제 목록
   */
  public List<ReviewQuestion> questions(UUID id) {
    return jdbc.query(
        "select * from expression_review_question where review_id = ? order by display_order",
        (rs, row) ->
            new ReviewQuestion(
                rs.getObject("id", UUID.class),
                rs.getLong("expression_id"),
                rs.getString("target_expression_text"),
                rs.getString("base_expression_meaning_text"),
                readQuiz(rs.getString("quiz_json")),
                rs.getInt("display_order"),
                rs.getInt("queue_order"),
                rs.getInt("wrong_count"),
                time(rs, "completed_at")),
        id);
  }

  /**
   * 이미 처리한 제출을 조회한다.
   *
   * @param id 복습 ID
   * @param submissionId 제출 키
   * @return 저장한 제출
   */
  public Optional<ReviewSubmission> submission(UUID id, UUID submissionId) {
    return jdbc
        .query(
            "select * from expression_review_submission where review_id = ? and submission_id = ?",
            (rs, row) ->
                new ReviewSubmission(
                    rs.getObject("question_id", UUID.class),
                    readWords(rs.getString("answer_json")),
                    rs.getBoolean("correct")),
            id,
            submissionId)
        .stream()
        .findFirst();
  }

  /**
   * 정답이면 완료하고 오답이면 현재 큐의 뒤로 보낸다.
   *
   * @param reviewId 복습 ID
   * @param request 제출 원문
   * @param correct 서버 판정
   * @param now 제출 시각
   */
  public void answer(
      UUID reviewId, ReviewAnswerRequest request, boolean correct, LocalDateTime now) {
    jdbc.update(
        """
        insert into expression_review_submission
        (review_id, submission_id, question_id, answer_json, correct, created_at) values (?, ?, ?, ?, ?, ?)
        """,
        reviewId,
        request.submissionId(),
        request.questionId(),
        json(request.words()),
        correct,
        now);
    if (correct) {
      jdbc.update(
          "update expression_review_question set completed_at = ? where id = ? and review_id = ?",
          now,
          request.questionId(),
          reviewId);
    } else {
      Integer next =
          jdbc.queryForObject(
              "select max(queue_order) + 1 from expression_review_question where review_id = ?",
              Integer.class,
              reviewId);
      jdbc.update(
          "update expression_review_question set wrong_count = wrong_count + 1, queue_order = ?"
              + " where id = ? and review_id = ?",
          next,
          request.questionId(),
          reviewId);
    }
    jdbc.update(
        """
        update expression_review set completed_at = ? where id = ? and completed_at is null
          and not exists (select 1 from expression_review_question where review_id = ? and completed_at is null)
        """,
        now,
        reviewId,
        reviewId);
  }

  private ExpressionReview review(ResultSet rs, int row) throws SQLException {
    return new ExpressionReview(
        rs.getObject("id", UUID.class),
        rs.getLong("user_profile_id"),
        rs.getObject("scheduled_date", LocalDate.class),
        time(rs, "created_at"),
        time(rs, "available_until"),
        time(rs, "started_at"),
        time(rs, "expires_at"),
        time(rs, "completed_at"));
  }

  private LocalDateTime time(ResultSet rs, String name) throws SQLException {
    return rs.getObject(name, LocalDateTime.class);
  }

  private String json(Object value) {
    try {
      return mapper.writeValueAsString(value);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("복습 스냅샷을 저장할 수 없습니다.", exception);
    }
  }

  private WritingSentenceResponse readQuiz(String json) {
    try {
      return mapper.readValue(json, WritingSentenceResponse.class);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("저장된 복습 문제를 읽을 수 없습니다.", exception);
    }
  }

  private List<String> readWords(String json) {
    try {
      return mapper.readValue(json, new TypeReference<List<String>>() {});
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("저장된 복습 제출을 읽을 수 없습니다.", exception);
    }
  }
}
