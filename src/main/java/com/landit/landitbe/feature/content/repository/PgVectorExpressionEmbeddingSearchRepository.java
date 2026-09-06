// PostgreSQL pgvector 연산자로 공용 프리톡 표현 후보를 검색한다.

package com.landit.landitbe.feature.content.repository;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * PostgreSQL pgvector 연산자로 공용 프리톡 표현 후보를 검색한다.
 *
 * <p>코사인 거리 계산은 PostgreSQL이 수행한다. H2 테스트 DB에는 vector 타입이 없으므로 운영 환경에서만 활성화하며, 테스트와 로컬은 in-memory
 * 구현을 사용한다.
 */
@Repository
@ConditionalOnProperty(
    prefix = "landit.expression-search",
    name = "mode",
    havingValue = "pgvector",
    matchIfMissing = true)
public class PgVectorExpressionEmbeddingSearchRepository
    implements ExpressionEmbeddingSearchRepository {

  // ORDER BY에 별칭 대신 연산식을 반복해 이후 벡터 인덱스(HNSW 등) 도입 시에도 그대로 활용되게 한다.
  static final String SEARCH_SQL =
      """
      SELECT we.id, we.embedding <=> CAST(? AS extensions.vector) AS distance
      FROM writing_expression we
      WHERE we.expression_source = 'FREE_TALK'
        AND we.status = 'ACTIVE'
        AND we.target_locale = ?
        AND we.base_locale = ?
        AND we.difficulty_level <= ?
        AND we.embedding IS NOT NULL
        AND NOT EXISTS (
          SELECT 1
          FROM user_writing_expression_completion c
          WHERE c.user_profile_id = ?
            AND c.writing_expression_id = we.id
        )
      ORDER BY we.embedding <=> CAST(? AS extensions.vector)
      LIMIT ?
      """;

  private final JdbcTemplate jdbcTemplate;

  /**
   * JDBC 접근자로 pgvector 검색 저장소를 구성한다.
   *
   * @param jdbcTemplate 네이티브 벡터 연산에 사용할 JDBC 접근자
   */
  public PgVectorExpressionEmbeddingSearchRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /** {@inheritDoc} */
  @Override
  public List<ExpressionEmbeddingMatch> searchFreeTalkCandidates(FreeTalkCandidateSearch search) {
    String vectorLiteral = toVectorLiteral(search.embedding());
    return jdbcTemplate.query(
        SEARCH_SQL,
        (resultSet, rowNumber) ->
            new ExpressionEmbeddingMatch(resultSet.getLong("id"), resultSet.getDouble("distance")),
        vectorLiteral,
        search.targetLocale().name(),
        search.baseLocale().name(),
        search.maxDifficultyLevel(),
        search.userProfileId(),
        vectorLiteral,
        search.limit());
  }

  // 임베딩 벡터를 pgvector 리터럴 형식('[0.1,0.2,...]')으로 변환한다.
  private static String toVectorLiteral(List<Float> embedding) {
    return embedding.stream().map(String::valueOf).collect(Collectors.joining(",", "[", "]"));
  }
}
