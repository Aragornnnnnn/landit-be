// pgvector 검색 SQL이 필수 조건과 연산자를 유지하는지 검증한다.

package com.landit.landitbe.feature.content.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * pgvector 검색 SQL이 필수 조건과 연산자를 유지하는지 검증한다.
 *
 * <p>H2 테스트 DB에는 vector 타입이 없어 실행 검증이 불가능하므로, 실행 계약(in-memory 구현과 공유)은 통합 테스트가 검증하고 여기서는 PostgreSQL
 * 전용 SQL의 핵심 구성을 고정한다.
 */
class PgVectorExpressionEmbeddingSearchRepositoryTests {

  @Test
  void searchSqlKeepsVectorOperatorAndFilters() {
    String sql = PgVectorExpressionEmbeddingSearchRepository.SEARCH_SQL;

    assertThat(sql)
        .contains("embedding <=> CAST(? AS extensions.vector)")
        .contains("expression_source = 'FREE_TALK'")
        .contains("status = 'ACTIVE'")
        .contains("embedding IS NOT NULL")
        .contains("NOT EXISTS")
        .contains("user_writing_expression_completion")
        .contains("LIMIT ?");

    // 벡터 인덱스 활용을 위해 ORDER BY에도 별칭이 아닌 연산식을 유지한다.
    assertThat(sql).contains("ORDER BY we.embedding <=> CAST(? AS extensions.vector)");
  }
}
