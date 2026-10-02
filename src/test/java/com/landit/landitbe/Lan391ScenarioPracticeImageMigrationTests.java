// 시나리오 표현 예문의 게시 이미지 매핑과 기존 문장 대응을 검증한다.

package com.landit.landitbe;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

/** LAN-391의 328개 표현과 게시된 예문 이미지 656개의 대응을 검증한다. */
class Lan391ScenarioPracticeImageMigrationTests {

  private static final String SOURCE =
      "db/postgresql/V136__insert_scenario_41_70_writing_expressions.sql";
  private static final String UPDATE =
      "db/postgresql/V138__update_scenario_41_70_practice_example_image_urls.sql";
  private static final Pattern SOURCE_ROW = Pattern.compile("(?m)^  \\((\\d+), (\\d+),");
  private static final Pattern SOURCE_ORDER = Pattern.compile("'EN', 'KR',\\s*(\\d+),");
  private static final Pattern IMAGE_ROW =
      Pattern.compile(
          "(?m)^    \\((\\d+), (\\d+), (\\d+), (\\d+), '((?:[^']|'')*)', "
              + "'((?:[^']|'')*)', '([0-9a-f]{64})', '([0-9a-f]{64})'\\)[,;]$");
  private static final String MAPPING_SHA256 =
      "7b27d0c12f37c3eacbb7f0b4111c9fc84cab27ac61770cfd797a4a61b1e31f83";

  @DisplayName("게시 이미지 656개를 V136의 시나리오·표현 순서·영어 예문과 정확히 연결한다.")
  @Test
  void matchesPublishedImageMapToInsertedExpressions() throws Exception {
    String source = readSql(SOURCE);
    Matcher sourceRows = SOURCE_ROW.matcher(source);
    List<Integer> sourcePositions = new ArrayList<>();
    while (sourceRows.find()) {
      sourcePositions.add(sourceRows.start());
    }
    assertThat(sourcePositions).hasSize(328);

    Matcher imageRows = IMAGE_ROW.matcher(readSql(UPDATE));
    ObjectMapper mapper = new ObjectMapper();
    StringBuilder digestInput = new StringBuilder();
    Set<String> imageHashes = new HashSet<>();
    int rowNumber = 0;
    while (imageRows.find()) {
      rowNumber++;
      String sourceRow =
          source.substring(
              sourcePositions.get(rowNumber - 1),
              rowNumber < sourcePositions.size()
                  ? sourcePositions.get(rowNumber)
                  : source.length());
      Matcher sourceHeader = SOURCE_ROW.matcher(sourceRow);
      Matcher sourceOrder = SOURCE_ORDER.matcher(sourceRow);
      assertThat(sourceHeader.find()).isTrue();
      assertThat(sourceOrder.find()).isTrue();

      int expressionId = Integer.parseInt(imageRows.group(1));
      int scenarioId = Integer.parseInt(imageRows.group(2));
      int sqlRow = Integer.parseInt(imageRows.group(4));
      assertThat(expressionId).isEqualTo(4000 + rowNumber);
      assertThat(scenarioId).isEqualTo(Integer.parseInt(sourceHeader.group(2)));
      assertThat(expressionId).isEqualTo(Integer.parseInt(sourceHeader.group(1)));
      assertThat(Integer.parseInt(imageRows.group(3)))
          .isEqualTo(Integer.parseInt(sourceOrder.group(1)));
      assertThat(sqlRow).isEqualTo(rowNumber);

      int examplesStart = sourceRow.indexOf("'[{");
      int examplesEnd = sourceRow.indexOf("]'::jsonb", examplesStart);
      assertThat(examplesStart).isGreaterThanOrEqualTo(0);
      assertThat(examplesEnd).isGreaterThan(examplesStart);
      String examplesJson = sourceRow.substring(examplesStart + 1, examplesEnd + 1);
      JsonNode examples = mapper.readTree(examplesJson.replace("''", "'"));
      assertThat(examples.size()).isEqualTo(4);
      for (int exampleNumber = 3; exampleNumber <= 4; exampleNumber++) {
        JsonNode example = examples.get(exampleNumber - 1);
        String expectedSentence = imageRows.group(exampleNumber == 3 ? 5 : 6).replace("''", "'");
        String imageHash = imageRows.group(exampleNumber == 3 ? 7 : 8);
        assertThat(example.path("sentenceText").asText()).isEqualTo(expectedSentence);
        assertThat(example.path("imageUrl").isNull()).isTrue();
        assertThat(imageHashes.add(imageHash)).isTrue();
        digestInput
            .append(expressionId)
            .append(':')
            .append(scenarioId)
            .append(':')
            .append(sqlRow)
            .append(':')
            .append(exampleNumber)
            .append(':')
            .append(imageHash)
            .append('\n');
      }
    }
    assertThat(rowNumber).isEqualTo(328);
    assertThat(imageHashes).hasSize(656);
    byte[] digest =
        MessageDigest.getInstance("SHA-256")
            .digest(digestInput.toString().getBytes(StandardCharsets.UTF_8));
    assertThat(java.util.HexFormat.of().formatHex(digest)).isEqualTo(MAPPING_SHA256);
  }

  @DisplayName("V138의 예문 블록은 대상 상태를 검사하고 예문 3·4 이미지 URL만 변경한다.")
  @Test
  void guardsStateAndPreservesOtherPayloadFields() throws Exception {
    String migration = readSql(UPDATE);
    String practiceBlock =
        migration.substring(
            0, migration.indexOf("CREATE TEMP TABLE lan391_representative_image_map"));
    assertThat(practiceBlock)
        .contains(
            "LOCK TABLE writing_expression IN SHARE ROW EXCLUSIVE MODE",
            "expression.scenario_id IS DISTINCT FROM asset.scenario_id",
            "expression.display_order IS DISTINCT FROM asset.expression_display_order",
            "expression.practice_examples_payload->2->>'sentenceText' IS DISTINCT FROM "
                + "asset.example3_sentence",
            "expression.practice_examples_payload->3->>'sentenceText' IS DISTINCT FROM "
                + "asset.example4_sentence",
            "'{2,imageUrl}'",
            "'{3,imageUrl}'",
            "IF affected_rows <> 328 THEN")
        .doesNotContain("representative_image_url", "ON CONFLICT", "COMMIT;");
  }

  private String readSql(String path) throws Exception {
    return StreamUtils.copyToString(
        new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8);
  }
}
