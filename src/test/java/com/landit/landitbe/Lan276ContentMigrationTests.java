// LAN-276 표현 오답 선택지 교정 마이그레이션의 고정 매핑을 검증한다.

package com.landit.landitbe;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

/** LAN-276 표현 오답 선택지 교정 마이그레이션의 고정 매핑을 검증한다. */
class Lan276ContentMigrationTests {

  private static final String MIGRATION_PATH =
      "db/postgresql/V46__fix_writing_expression_word_choices.sql";

  /** 비실존 오답 단어가 검출되어 교정 대상이 된 표현 48건의 ID. */
  private static final List<Integer> FIXED_EXPRESSION_IDS =
      List.of(
          87, 88, 90, 91, 92, 94, 96, 97, 99, 101, 103, 104, 106, 107, 109, 111, 112, 113, 114, 116,
          117, 123, 124, 125, 126, 127, 129, 132, 134, 135, 136, 137, 138, 142, 143, 145, 148, 151,
          152, 154, 155, 156, 157, 158, 160, 161, 162, 163);

  private static final Pattern UPDATE_TARGET_PATTERN = Pattern.compile("WHERE id = (\\d+);");

  /** 교정 전 데이터에 존재하던 대표 비실존 단어들. 교정 후에는 파일 어디에도 남아 있으면 안 된다. */
  private static final List<String> NON_EXISTENT_WORDS =
      List.of(
          "nevers",
          "mights",
          "giveing",
          "makeing",
          "likeed",
          "justs",
          "throughs",
          "abouts",
          "rathers",
          "beens",
          "reallys",
          "worths",
          "Anyth",
          "submitt",
          "runn",
          "struggl");

  @Test
  void migrationFixesAllDetectedExpressionsOnly() throws Exception {
    String migrationSql = readMigrationSql();
    Matcher matcher = UPDATE_TARGET_PATTERN.matcher(migrationSql);
    List<Integer> expressionIds = new ArrayList<>();

    while (matcher.find()) {
      expressionIds.add(Integer.parseInt(matcher.group(1)));
    }

    assertThat(expressionIds).containsExactlyElementsOf(FIXED_EXPRESSION_IDS);
    assertThat(migrationSql.split("UPDATE writing_expression SET", -1))
        .hasSize(FIXED_EXPRESSION_IDS.size() + 1);
  }

  @Test
  void migrationLeavesNoNonExistentWords() throws Exception {
    String migrationSql = readMigrationSql();

    for (String word : NON_EXISTENT_WORDS) {
      // 오답 토큰은 항상 따옴표로 감싸인 단독 단어로 존재하므로 quoted 형태로만 검사한다.
      assertThat(migrationSql).doesNotContain("\"" + word + "\"", "'" + word + "'");
    }
  }

  @Test
  void migrationUsesArraySyntaxForVarcharArrayColumn() throws Exception {
    String migrationSql = readMigrationSql();

    // representative_sentence_word_choices는 varchar array 컬럼이라 jsonb 캐스트가 섞이면 실행이 실패한다.
    assertThat(migrationSql).doesNotContain("representative_sentence_word_choices = '[");
  }

  private String readMigrationSql() throws Exception {
    return StreamUtils.copyToString(
        new ClassPathResource(MIGRATION_PATH).getInputStream(), StandardCharsets.UTF_8);
  }
}
