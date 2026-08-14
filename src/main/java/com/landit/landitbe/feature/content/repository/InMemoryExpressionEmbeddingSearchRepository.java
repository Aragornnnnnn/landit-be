// 자바 코사인 계산으로 공용 프리톡 표현 후보를 검색한다.

package com.landit.landitbe.feature.content.repository;

import com.landit.landitbe.shared.domain.Locale;
import java.util.Comparator;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 자바 코사인 계산으로 공용 프리톡 표현 후보를 검색한다.
 *
 * <p>vector 타입이 없는 H2 테스트 DB와 로컬 개발 환경에서 pgvector 구현과 같은 계약을 제공한다. 후보 전체를 로드해 계산하므로 운영 규모에는
 * pgvector 구현을 사용한다.
 */
@Repository
@ConditionalOnProperty(
    prefix = "landit.expression-search",
    name = "mode",
    havingValue = "in-memory",
    matchIfMissing = true)
public class InMemoryExpressionEmbeddingSearchRepository
    implements ExpressionEmbeddingSearchRepository {

  private static final String CANDIDATE_SQL =
      """
      SELECT we.id, we.embedding
      FROM writing_expression we
      WHERE we.expression_source = 'FREE_TALK'
        AND we.status = 'ACTIVE'
        AND we.target_locale = ?
        AND we.base_locale = ?
        AND we.embedding IS NOT NULL
        AND NOT EXISTS (
          SELECT 1
          FROM user_writing_expression_completion c
          WHERE c.user_profile_id = ?
            AND c.writing_expression_id = we.id
        )
      """;

  private final JdbcTemplate jdbcTemplate;

  /**
   * JDBC 접근자로 in-memory 검색 저장소를 구성한다.
   *
   * @param jdbcTemplate 후보 조회에 사용할 JDBC 접근자
   */
  public InMemoryExpressionEmbeddingSearchRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /** {@inheritDoc} */
  @Override
  public List<ExpressionEmbeddingMatch> searchFreeTalkCandidates(
      List<Float> embedding,
      long userProfileId,
      Locale targetLocale,
      Locale baseLocale,
      int limit) {
    return jdbcTemplate
        .query(
            CANDIDATE_SQL,
            (resultSet, rowNumber) ->
                new ExpressionEmbeddingMatch(
                    resultSet.getLong("id"),
                    cosineDistance(embedding, parseVector(resultSet.getString("embedding")))),
            targetLocale.name(),
            baseLocale.name(),
            userProfileId)
        .stream()
        .sorted(Comparator.comparingDouble(ExpressionEmbeddingMatch::distance))
        .limit(limit)
        .toList();
  }

  // 저장된 벡터 문자열('[0.1,0.2,...]')을 실수 배열로 변환한다.
  private static double[] parseVector(String storedVector) {
    String[] components = storedVector.replace("[", "").replace("]", "").split(",");
    double[] vector = new double[components.length];
    for (int index = 0; index < components.length; index++) {
      vector[index] = Double.parseDouble(components[index].trim());
    }
    return vector;
  }

  // pgvector의 <=> 연산과 같은 코사인 거리(1 - 코사인 유사도)를 계산한다.
  private static double cosineDistance(List<Float> query, double[] stored) {
    if (query.size() != stored.length) {
      throw new IllegalStateException("쿼리와 저장 임베딩의 차원이 일치하지 않습니다.");
    }
    double dotProduct = 0.0;
    double queryNorm = 0.0;
    double storedNorm = 0.0;
    for (int index = 0; index < stored.length; index++) {
      double queryComponent = query.get(index);
      dotProduct += queryComponent * stored[index];
      queryNorm += queryComponent * queryComponent;
      storedNorm += stored[index] * stored[index];
    }
    if (queryNorm == 0.0 || storedNorm == 0.0) {
      throw new IllegalStateException("코사인 거리를 계산할 수 없는 영벡터 임베딩입니다.");
    }
    return 1.0 - dotProduct / (Math.sqrt(queryNorm) * Math.sqrt(storedNorm));
  }
}
