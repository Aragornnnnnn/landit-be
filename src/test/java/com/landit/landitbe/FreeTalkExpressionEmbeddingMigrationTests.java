// 사전 생성한 프리톡 표현과 임베딩 migration의 구조를 검증한다.

package com.landit.landitbe;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** 사전 생성한 프리톡 표현과 임베딩 migration의 구조를 검증한다. */
class FreeTalkExpressionEmbeddingMigrationTests {

  private static final String MIGRATION =
      "db/postgresql/V52__insert_free_talk_expressions_with_embeddings.sql";

  @Test
  void insertsAllFreeTalkExpressionsWithPrecomputedEmbeddings() throws IOException {
    String sql =
        new String(
            Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(MIGRATION))
                .readAllBytes(),
            StandardCharsets.UTF_8);

    assertThat(sql.lines().filter(line -> line.matches("\\(\\d+,NULL,.*")).count()).isEqualTo(818L);
    assertThat(sql).contains("(164,NULL,").contains("(981,NULL,");
    Matcher vectors = Pattern.compile("'\\[([^]]+)]'::extensions\\.vector").matcher(sql);
    int vectorCount = 0;
    while (vectors.find()) {
      assertThat(vectors.group(1).split(",")).hasSize(1536);
      vectorCount++;
    }
    assertThat(vectorCount).isEqualTo(818);
    assertThat(sql.substring(0, sql.indexOf("VALUES")))
        .contains("id")
        .contains("embedding")
        .doesNotContain("owner_user_profile_id");
    assertThat(sql).contains("pg_get_serial_sequence('writing_expression', 'id')");
  }
}
