// GitHub Actions에서 Apple 사용자 이전 CLI를 독립 실행한다.

package com.landit.landitbe.feature.auth.migration;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Map;

/** GitHub Actions에서 Apple 사용자 이전 CLI를 독립 실행한다. */
public final class AppleUserMigrationRunner {

  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

  private AppleUserMigrationRunner() {}

  /**
   * 환경 변수의 운영 설정으로 Apple 사용자 이전 단계를 실행한다.
   *
   * @param args 실행 인자
   */
  public static void main(String[] args) {
    int exitCode = run(System.getenv());
    if (exitCode != 0) {
      System.exit(exitCode);
    }
  }

  static int run(Map<String, String> environment) {
    try {
      AppleUserMigrationSettings settings = AppleUserMigrationSettings.from(environment);
      AppleUserMigrationRepository repository =
          new AppleUserMigrationRepository(
              settings.databaseUrl(), settings.databaseUsername(), settings.databasePassword());
      AppleUserMigrationClient client =
          new HttpAppleUserMigrationClient(
              settings.apiBaseUri(),
              settings.clientId(),
              settings.clientSecret(),
              settings.recipientTeamId(),
              REQUEST_TIMEOUT,
              new ObjectMapper());
      AppleUserMigrationSummary summary =
          new AppleUserMigrationService(repository, client).run(settings.phase());
      printSummary(settings.phase(), summary);
      return summary.completed() ? 0 : 1;
    } catch (AppleUserMigrationException exception) {
      System.err.println("Apple user migration failed: " + exception.failureCode());
      return 1;
    } catch (IllegalStateException exception) {
      System.err.println(exception.getMessage());
      return 1;
    } catch (RuntimeException exception) {
      System.err.println("Apple user migration failed: UNEXPECTED_ERROR");
      return 1;
    }
  }

  private static void printSummary(
      AppleUserMigrationPhase phase, AppleUserMigrationSummary summary) {
    System.out.printf(
        "phase=%s target=%d success=%d failure=%d unresolved=%d%n",
        phase,
        summary.targetCount(),
        summary.successCount(),
        summary.failureCount(),
        summary.unresolvedCount());
  }
}
