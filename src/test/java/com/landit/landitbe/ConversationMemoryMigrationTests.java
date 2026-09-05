// 장기기억 V65 migration의 DB별 구조 차이를 검증한다.

package com.landit.landitbe;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

class ConversationMemoryMigrationTests {

  @Test
  void v65MigrationsUseDatabaseSpecificVectorAndIndexSyntax() throws Exception {
    String postgresqlSql = readMigrationSql("db/postgresql/V65__add_conversation_memory.sql");
    String h2Sql = readMigrationSql("db/h2/V65__add_conversation_memory.sql");

    assertThat(postgresqlSql)
        .contains("embedding extensions.vector(1536) NOT NULL")
        .contains("CREATE TABLE conversation_memory_source")
        .contains("WHERE status = 'ACTIVE'")
        .doesNotContain("USING hnsw")
        .doesNotContain("USING ivfflat");
    assertThat(h2Sql)
        .contains("CREATE DOMAIN IF NOT EXISTS extensions.vector AS VARCHAR(32767)")
        .contains("embedding extensions.vector NOT NULL")
        .contains("CREATE TABLE conversation_memory_source");
  }

  private String readMigrationSql(String path) throws Exception {
    return StreamUtils.copyToString(
        new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8);
  }
}
