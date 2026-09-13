// 기능 간 저장소·엔티티 직접 참조와 금지된 역방향 의존의 회귀를 검사한다.

package com.landit.landitbe.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** 명시적인 Java import 경계를 검사한다. SQL JOIN은 아키텍처 문서의 조회 예외로 관리한다. */
class FeatureBoundaryTest {
  private static final Path SOURCE = Path.of("src/main/java/com/landit/landitbe");
  private static final String FEATURE = "com.landit.landitbe.feature.";
  private static final Pattern IMPORT = Pattern.compile("(?m)^import (?:static )?([\\w.*]+);");

  @Test
  void featuresDoNotImportForeignRepositoriesOrEntities() throws IOException {
    List<Path> files = sourceFiles();
    Set<String> entities = new HashSet<>();
    for (Path file : files) {
      if (Files.readString(file).contains("import jakarta.persistence.Entity;")) {
        entities.add(className(file));
      }
    }
    List<String> violations = new ArrayList<>();
    for (Path file : files) {
      String owner = featureOf(className(file));
      var imports = IMPORT.matcher(Files.readString(file));
      while (imports.find()) {
        String target = imports.group(1);
        String dependency = featureOf(target);
        if (owner.isEmpty() || dependency.isEmpty() || owner.equals(dependency)) {
          continue;
        }
        if (target.contains(".repository.") || entities.contains(target)) {
          violations.add(file + " -> " + target);
        }
      }
    }
    assertThat(violations).isEmpty();
  }

  @Test
  void catalogAndMemoryDoNotDependOnSessionsAndSharedHasNoFeatureDependency() throws IOException {
    List<String> violations = new ArrayList<>();
    for (Path file : sourceFiles()) {
      String name = className(file);
      var imports = IMPORT.matcher(Files.readString(file));
      while (imports.find()) {
        String target = imports.group(1);
        boolean sharedDependency = name.contains(".shared.") && target.startsWith(FEATURE);
        boolean sessionDependency =
            Set.of("content", "memory").contains(featureOf(name))
                && featureOf(target).equals("session");
        if (sharedDependency || sessionDependency) {
          violations.add(file + " -> " + target);
        }
      }
    }
    assertThat(violations).isEmpty();
  }

  private static List<Path> sourceFiles() throws IOException {
    try (var files = Files.walk(SOURCE)) {
      return files.filter(path -> path.toString().endsWith(".java")).toList();
    }
  }

  private static String className(Path file) {
    return "com.landit.landitbe."
        + SOURCE.relativize(file).toString().replace('/', '.').replace(".java", "");
  }

  private static String featureOf(String name) {
    return name.startsWith(FEATURE) ? name.substring(FEATURE.length()).split("\\.")[0] : "";
  }
}
