// 적용된 WebP URL 매핑을 역전해 신규 Flyway 롤백 SQL 후보를 생성한다.
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** V48의 고정 URL 매핑을 역전해 검토 가능한 신규 Flyway migration SQL을 출력한다. */
public final class GenerateContentImageWebpRollback {

  private static final Pattern URL_MAPPING_PATTERN =
      Pattern.compile("\\('([^']+\\.png)', '([^']+\\.webp)'\\)");
  private static final String MIGRATION_TAIL_MARKER = "CREATE TEMP VIEW current_content_image_urls AS";

  private GenerateContentImageWebpRollback() {}

  /**
   * V48 경로를 읽어 표준 출력으로 역방향 SQL을 생성한다.
   *
   * @param args V48 migration 파일 경로
   * @throws Exception 파일을 읽지 못하거나 고정 매핑이 692개가 아닐 때
   */
  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      throw new IllegalArgumentException("V48 migration path is required");
    }

    String migrationSql = Files.readString(Path.of(args[0]), StandardCharsets.UTF_8);
    List<UrlMapping> mappings = extractMappings(migrationSql);
    int tailIndex = migrationSql.indexOf(MIGRATION_TAIL_MARKER);
    if (tailIndex < 0) {
      throw new IllegalStateException("V48 migration tail marker is missing");
    }

    printHeaderAndMappings(mappings);
    System.out.print(migrationSql.substring(tailIndex));
  }

  private static List<UrlMapping> extractMappings(String migrationSql) {
    Matcher matcher = URL_MAPPING_PATTERN.matcher(migrationSql);
    List<UrlMapping> mappings = new ArrayList<>();
    while (matcher.find()) {
      mappings.add(new UrlMapping(matcher.group(1), matcher.group(2)));
    }
    if (mappings.size() != 692) {
      throw new IllegalStateException("Expected 692 V48 mappings but found " + mappings.size());
    }
    return mappings;
  }

  private static void printHeaderAndMappings(List<UrlMapping> mappings) {
    System.out.print(
        """
        -- 공용 콘텐츠 이미지 URL을 기존 PNG 객체로 되돌린다.
        SELECT pg_advisory_xact_lock(hashtext('landit-content-image-webp-rollback'));

        CREATE TEMP TABLE image_url_remap (
            old_url TEXT PRIMARY KEY,
            new_url TEXT NOT NULL UNIQUE
        ) ON COMMIT DROP;

        INSERT INTO image_url_remap (old_url, new_url) VALUES
        """);
    for (int index = 0; index < mappings.size(); index++) {
      UrlMapping mapping = mappings.get(index);
      System.out.printf(
          "    ('%s', '%s')%s%n",
          mapping.webpUrl(), mapping.pngUrl(), index == mappings.size() - 1 ? ";" : ",");
    }
    System.out.println();
  }

  private record UrlMapping(String pngUrl, String webpUrl) {}
}
