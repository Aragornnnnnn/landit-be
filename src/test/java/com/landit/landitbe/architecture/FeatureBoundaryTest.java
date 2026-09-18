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
import org.junit.jupiter.api.DisplayName;
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

  @DisplayName("각 업무 모듈은 자신의 Repository와 Entity를 소유한다.")
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

  @DisplayName("Controller는 같은 업무의 저장소와 Entity에도 직접 접근하지 않는다.")
  @Test
  void controllersDoNotAccessEvenTheirOwnPersistenceTypes() {
    assertThat(
            dependencies.stream()
                .filter(edge -> outerType(edge.source()).endsWith("Controller"))
                .filter(edge -> isPersistenceType(edge.target()))
                .toList())
        .isEmpty();
  }

  @DisplayName("관리자 HTTP 진입점은 admin 패키지에 둔다.")
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

  @DisplayName("사용자 Controller는 관리자 전용 계약에 의존하지 않는다.")
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

  @DisplayName("인프라와 콘텐츠 카탈로그는 정해진 의존 방향을 지킨다.")
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
                              && (target.startsWith("learning.") || target.equals("subscription")))
                          || (source.equals("learning.conversation")
                              && target.startsWith("learning.")
                              && !target.equals("learning.conversation"))
                          || (source.equals("memory") && target.startsWith("learning."))
                          || (source.equals("subscription")
                              && (target.equals("auth")
                                  || (target.startsWith("learning.")
                                      && !target.equals("learning.scenario.progress"))));
                    })
                .toList())
        .isEmpty();
  }

  @DisplayName("업무 모듈 사이에 순환 의존이 없다.")
  @Test
  void businessModuleDependenciesHaveNoCycles() {
    assertThat(cycleNodes(dependencies)).isEmpty();
  }

  @DisplayName("Service 내부에 공개 record 타입을 선언하지 않는다.")
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

  @DisplayName("기억 업무는 자신의 AI 구현체를 직접 소유한다.")
  @Test
  void memoryOwnsAllItsAiImplementations() {
    Class<?> port = com.landit.landitbe.feature.memory.client.ai.AiMemoryClient.class;
    List<Class<?>> implementations =
        types.stream().filter(type -> !type.isInterface() && port.isAssignableFrom(type)).toList();
    assertThat(implementations).isNotEmpty();
    assertThat(implementations)
        .allSatisfy(type -> assertThat(type.getName()).startsWith(FEATURE + "memory."));
  }

  @DisplayName("컴파일된 타입 검사로 전체 경로를 쓴 필드와 순환 의존도 감지한다.")
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
    // learning 내부에서도 데이터 소유와 요청 조율의 경계를 별도로 검사한다.
    for (String unit :
        List.of(
            "learning.scenario.selection",
            "learning.scenario.access",
            "learning.scenario.progress",
            "learning.scenario.level",
            "learning.expression.progress")) {
      if (name.startsWith(FEATURE + unit + ".")) {
        return unit;
      }
    }
    // 시나리오 세션·피드백·평가는 동일 실행 업무다. 공통 대화 저장소는 별도 소유다.
    return parts[0].equals("learning") ? parts[0] + "." + parts[1] : parts[0];
  }

  @DisplayName("학습 하위 업무의 저장소 침범과 순환 의존도 경계 검사에서 감지한다.")
  @Test
  void learningSubmodulesCannotHidePersistenceAccessOrCycles() {
    String scenario = FEATURE + "learning.scenario.session.service.StartService";
    String conversation = FEATURE + "learning.conversation.service.LearningSessionService";
    String repository = FEATURE + "learning.conversation.repository.LearningSessionRepository";
    assertThat(moduleOf(scenario)).isNotEqualTo(moduleOf(repository));
    assertThat(isPersistenceType(repository)).isTrue();
    assertThat(
            cycleNodes(
                List.of(
                    new Dependency(scenario, conversation),
                    new Dependency(conversation, scenario))))
        .containsExactlyInAnyOrder("learning.scenario", "learning.conversation");
    assertThat(moduleOf(FEATURE + "learning.scenario.progress.service.ScenarioProgressService"))
        .isNotEqualTo(moduleOf(scenario));
    assertThat(moduleOf(FEATURE + "learning.expression.progress.service.ExpressionProgressService"))
        .isNotEqualTo(moduleOf(FEATURE + "learning.expression.service.ExpressionLearningService"));
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
