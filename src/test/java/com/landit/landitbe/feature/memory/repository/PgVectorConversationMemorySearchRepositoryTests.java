// PostgreSQL 장기기억 exact 검색 SQL 계약을 검증한다.

package com.landit.landitbe.feature.memory.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** PostgreSQL 장기기억 exact 검색 SQL 계약을 검증한다. */
class PgVectorConversationMemorySearchRepositoryTests {

  @Test
  void searchSqlKeepsHardFiltersAndExactCosineOrdering() {
    String sql = PgVectorConversationMemorySearchRepository.SEARCH_SQL;

    assertThat(sql)
        .contains("cm.user_profile_id = :userProfileId")
        .contains("cm.status = 'ACTIVE'")
        .contains("cm.character_id IS NULL OR cm.character_id = :characterId")
        .contains("cm.embedding <=> CAST(:embedding AS extensions.vector)")
        .contains("ORDER BY cm.embedding <=> CAST(:embedding AS extensions.vector), cm.id")
        .contains("LIMIT :limit")
        .doesNotContain("NOT IN")
        .doesNotContain("hnsw")
        .doesNotContain("ivfflat");
  }
}
