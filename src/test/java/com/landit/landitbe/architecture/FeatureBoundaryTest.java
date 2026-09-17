// 컴파일된 타입 의존을 기준으로 업무 경계와 순환 의존의 회귀를 검사한다.

package com.landit.landitbe.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Entity;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** JDK의 바이트코드 분석으로 실제 필드·호출·상속·시그니처 의존을 검사한다. SQL JOIN은 문서로 관리한다. */
class FeatureBoundaryTest {
  private static final String ROOT = "com.landit.landitbe.";
  private static final String FEATURE = ROOT + "feature.";
  private static final Path CLASSES = Path.of("build/classes/java/main");
  private static final Pattern DEPENDENCY =
      Pattern.compile("(?m)^\\s+(com\\.landit\\.\\S+)\\s+->\\s+(com\\.landit\\.\\S+)");
  private static List<Dependency> dependencies;
  private static Set<String> entities;
  private static List<Class<?>> types;

  @BeforeAll
  static void readCompiledDependencies() throws Exception {
    dependencies = analyze(CLASSES);
    assertThat(dependencies).as("운영 클래스의 의존을 실제로 읽어야 한다").isNotEmpty();
    types = new ArrayList<>();
    entities = new HashSet<>();
    try (var files = Files.walk(CLASSES)) {
      for (Path file : files.filter(path -> path.toString().endsWith(".class")).toList()) {
        String name = CLASSES.relativize(file).toString().replace('/', '.').replace(".class", "");
        Class<?> type = Class.forName(name, false, FeatureBoundaryTest.class.getClassLoader());
        types.add(type);
        if (type.isAnnotationPresent(Entity.class)) {
          entities.add(name);
        }
      }
    }
  }

  @Test
  void businessModulesOwnTheirRepositoriesAndEntities() {
    List<Dependency> violations =
        dependencies.stream()
            .filter(edge -> !moduleOf(edge.source()).isEmpty())
            .filter(edge -> !moduleOf(edge.target()).isEmpty())
            .filter(edge -> !moduleOf(edge.source()).equals(moduleOf(edge.target())))
            .filter(edge -> isPersistenceType(edge.target()))
            .toList();
    assertThat(violations).isEmpty();
  }

  @Test
  void controllersDoNotAccessEvenTheirOwnPersistenceTypes() {
    assertThat(
            dependencies.stream()
                .filter(edge -> outerType(edge.source()).endsWith("Controller"))
                .filter(edge -> isPersistenceType(edge.target()))
                .toList())
        .isEmpty();
  }

  @Test
  void administrativeHttpEntrypointsStayInAdminPackages() {
    for (Class<?> type : types) {
      for (var method : type.getDeclaredMethods()) {
        var mapping =
            org.springframework.core.annotation.AnnotatedElementUtils.findMergedAnnotation(
                method, org.springframework.web.bind.annotation.RequestMapping.class);
        if (mapping != null
            && java.util.Arrays.stream(mapping.path())
                .anyMatch(path -> path.startsWith("/api/v1/admin/"))) {
          assertThat(type.getPackageName()).as(type.getName()).contains(".admin");
        }
      }
    }
  }

  @Test
  void userControllersDoNotDependOnAdministrativeContracts() {
    assertThat(
            dependencies.stream()
                .filter(edge -> outerType(edge.source()).endsWith("Controller"))
                .filter(edge -> !edge.source().contains(".admin."))
                .filter(edge -> edge.target().contains(".admin."))
                .toList())
        .isEmpty();
  }

  @Test
  void infrastructureAndCatalogRespectExplicitDependencyDirections() {
    assertThat(
            dependencies.stream()
                .filter(
                    edge -> {
                      String source = moduleOf(edge.source());
                      String target = moduleOf(edge.target());
                      return (edge.source().startsWith(ROOT + "shared.") && !target.isEmpty())
                          || (source.equals("content")
                              && (target.startsWith("learning.")
                                  || Set.of("session", "subscription").contains(target)))
                          || (source.equals("memory") && target.equals("session"))
                          || (source.equals("subscription")
                              && Set.of("session", "auth").contains(target));
                    })
                .toList())
        .isEmpty();
  }

  @Test
  void businessModuleDependenciesHaveNoCycles() {
    assertThat(cycleNodes(dependencies)).isEmpty();
  }

  @Test
  void servicesExposeRecordsFromValuePackages() {
    List<String> violations = new ArrayList<>();
    for (Class<?> type : types) {
      if (!type.getSimpleName().endsWith("Service")) {
        continue;
      }
      for (Class<?> nested : type.getDeclaredClasses()) {
        if (nested.isRecord() && Modifier.isPublic(nested.getModifiers())) {
          violations.add(nested.getName());
        }
      }
    }
    assertThat(violations).isEmpty();
  }

  @Test
  void memoryOwnsAllItsAiImplementations() {
    Class<?> port = com.landit.landitbe.feature.memory.client.ai.AiMemoryClient.class;
    List<Class<?>> implementations =
        types.stream().filter(type -> !type.isInterface() && port.isAssignableFrom(type)).toList();
    assertThat(implementations).isNotEmpty();
    assertThat(implementations)
        .allSatisfy(type -> assertThat(type.getName()).startsWith(FEATURE + "memory."));
  }

  @Test
  void compiledAnalysisIncludesFullyQualifiedFieldsAndDetectsCycles(@TempDir Path directory)
      throws Exception {
    Path first = directory.resolve("First.java");
    Path second = directory.resolve("Second.java");
    Files.writeString(
        first,
        "package "
            + FEATURE
            + "first; public class First { "
            + FEATURE
            + "second.Second dependency; }");
    Files.writeString(
        second,
        "package "
            + FEATURE
            + "second; public class Second { "
            + FEATURE
            + "first.First dependency; }");
    int result =
        ToolProvider.getSystemJavaCompiler()
            .run(null, null, null, "-d", directory.toString(), first.toString(), second.toString());
    assertThat(result).isZero();
    List<Dependency> fixture = analyze(directory);
    assertThat(fixture)
        .contains(new Dependency(FEATURE + "first.First", FEATURE + "second.Second"));
    assertThat(cycleNodes(fixture)).containsExactlyInAnyOrder("first", "second");
  }

  private static List<Dependency> analyze(Path classes) throws Exception {
    Path output = Files.createTempFile("landit-boundary-", ".txt");
    try {
      Process process =
          new ProcessBuilder(
                  Path.of(System.getProperty("java.home"), "bin", "jdeps").toString(),
                  "--ignore-missing-deps",
                  "-verbose:class",
                  "-filter:none",
                  classes.toString())
              .redirectErrorStream(true)
              .redirectOutput(output.toFile())
              .start();
      if (!process.waitFor(30, TimeUnit.SECONDS)) {
        process.destroyForcibly();
        throw new IOException("jdeps 분석 시간이 초과되었습니다.");
      }
      String text = Files.readString(output);
      assertThat(process.exitValue()).as(text).isZero();
      List<Dependency> result = new ArrayList<>();
      var matcher = DEPENDENCY.matcher(text);
      while (matcher.find()) {
        result.add(new Dependency(matcher.group(1), matcher.group(2)));
      }
      return result;
    } finally {
      Files.deleteIfExists(output);
    }
  }

  private static boolean isPersistenceType(String name) {
    return name.contains(".repository.") || entities.contains(outerType(name));
  }

  private static String outerType(String name) {
    return name.split("\\$", 2)[0];
  }

  private static String moduleOf(String name) {
    if (!name.startsWith(FEATURE)) {
      return "";
    }
    String[] parts = name.substring(FEATURE.length()).split("\\.");
    if (name.startsWith(FEATURE + "learning.scenario.level.")) {
      return "learning.scenario.level";
    }
    // learning의 상태 소유(access/progress/review)와 요청 조율(expression/scenario)은 별도 업무다.
    return parts[0].equals("learning") ? parts[0] + "." + parts[1] : parts[0];
  }

  private static Set<String> cycleNodes(List<Dependency> edges) {
    Map<String, Set<String>> graph = new HashMap<>();
    for (Dependency edge : edges) {
      String source = moduleOf(edge.source());
      String target = moduleOf(edge.target());
      if (!source.isEmpty() && !target.isEmpty() && !source.equals(target)) {
        graph.computeIfAbsent(source, ignored -> new HashSet<>()).add(target);
        graph.computeIfAbsent(target, ignored -> new HashSet<>());
      }
    }
    Set<String> cycles = new HashSet<>();
    for (String node : graph.keySet()) {
      if (reaches(node, node, graph, new HashSet<>())) {
        cycles.add(node);
      }
    }
    return cycles;
  }

  private static boolean reaches(
      String current, String target, Map<String, Set<String>> graph, Set<String> visited) {
    if (!visited.add(current)) {
      return false;
    }
    for (String next : graph.getOrDefault(current, Set.of())) {
      if (next.equals(target) || reaches(next, target, graph, visited)) {
        return true;
      }
    }
    return false;
  }

  private record Dependency(String source, String target) {}
}
