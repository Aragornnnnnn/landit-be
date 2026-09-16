// 표현 연습과 복습 통합 테스트의 공통 콘텐츠를 생성한다.

package com.landit.landitbe.support;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

/** 부모 콘텐츠와 연습 예문을 함께 저장하는 테스트 준비 도구다. */
public final class ExpressionPracticeFixture {
  private final JdbcTemplate jdbcTemplate;

  /**
   * 호출 테스트의 데이터베이스를 사용한다.
   *
   * @param jdbcTemplate 테스트 데이터베이스 접근 도구
   */
  public ExpressionPracticeFixture(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * 카테고리·시나리오와 지정한 예문을 가진 표현을 생성한다.
   *
   * @param status 표현 활성 상태
   * @param payloadJson 테스트별 연습 예문 JSON 배열
   * @return 생성한 표현 ID
   */
  public Long seed(String status, String payloadJson) {
    LocalDateTime now = LocalDateTime.now();

    Long categoryId =
        insertAndGetId(
            "INSERT INTO category (display_order, status, created_at, updated_at) "
                + "VALUES (?, 'ACTIVE', ?, ?)",
            nextDisplayOrder("category"),
            now,
            now);

    Long scenarioId =
        insertAndGetId(
            "INSERT INTO scenario "
                + "(category_id, ai_role, difficulty, first_speaker, total_question_count, "
                + "display_order, status, created_at, updated_at) "
                + "VALUES (?, 'barista', 'NORMAL', 'AI', 5, ?, 'ACTIVE', ?, ?)",
            categoryId,
            nextDisplayOrder("scenario"),
            now,
            now);

    return insertAndGetId(
        "INSERT INTO writing_expression "
            + "(scenario_id, expression_type, usage_frequency_level, difficulty_level, "
            + "target_locale, base_locale, "
            + "display_order, target_expression_text, base_expression_meaning_text, usage_summary, "
            + "usage_description, representative_sentence_text, "
            + "representative_sentence_translation, "
            + "representative_sentence_words, representative_sentence_word_choices, "
            + "practice_examples_payload, status, created_at, updated_at) "
            // H2에서 CAST(? AS jsonb)는 문자열을 "JSON 문자열 값"으로 저장해버려서(배열로 파싱 안 됨)
            // 진짜 JSON으로 파싱해 저장하는 H2 문법인 "? FORMAT JSON"을 쓴다.
            + "VALUES (?, 'DAILY_ROUTINE', 'BASIC', 4, 'EN', 'KR', 1, 'blow my mind', '끝내주게 놀랍다', "
            + "'usage summary', '강렬한 인상을 받았을 때 최고의 리액션이에요.', "
            + "'representative sentence', '대표 예문 해석', ARRAY['sample'], ARRAY['sample','choice'], "
            + "? FORMAT JSON, ?, ?, ?)",
        scenarioId,
        payloadJson,
        status,
        now,
        now);
  }

  private Long insertAndGetId(String sql, Object... args) {
    KeyHolder keyHolder = new GeneratedKeyHolder();

    jdbcTemplate.update(
        connection -> {
          PreparedStatement statement =
              connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
          for (int i = 0; i < args.length; i++) {
            statement.setObject(i + 1, args[i]);
          }
          return statement;
        },
        keyHolder);

    return keyHolder.getKey().longValue();
  }

  private int nextDisplayOrder(String tableName) {
    Integer maxOrder =
        jdbcTemplate.queryForObject(
            "SELECT COALESCE(MAX(display_order), 0) FROM " + tableName, Integer.class);
    return maxOrder + 1;
  }
}
