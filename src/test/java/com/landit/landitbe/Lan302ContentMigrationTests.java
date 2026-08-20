// LAN-302 예문 이미지 URL 마이그레이션의 PostgreSQL 갱신 구조를 검증한다.

package com.landit.landitbe;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

/** LAN-302 예문 이미지 URL 마이그레이션의 데이터 갱신 구조를 검증한다. */
class Lan302ContentMigrationTests {

  private static final String MIGRATION_PATH =
      "db/postgresql/V56__update_lan302_practice_example_image_urls.sql";

  @DisplayName("V56은 payload를 먼저 재구성한 뒤 writing expression을 갱신한다.")
  @Test
  void rebuildsPayloadBeforeUpdatingWritingExpression() throws Exception {
    String migrationSql = readMigrationSql();

    assertThat(migrationSql)
        .contains(
            "patched_urls AS",
            "FROM writing_expression expression",
            "jsonb_array_elements(expression.practice_examples_payload)",
            "UPDATE writing_expression target",
            "FROM patched_urls patched",
            "WHERE target.id = patched.id")
        .doesNotContain("jsonb_array_elements(w.practice_examples_payload)");
  }

  private String readMigrationSql() throws Exception {
    return StreamUtils.copyToString(
        new ClassPathResource(MIGRATION_PATH).getInputStream(), StandardCharsets.UTF_8);
  }
}
