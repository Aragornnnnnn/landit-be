// GitHub Actions에서 애플리케이션 배포 전 Flyway 마이그레이션만 실행한다.

package com.landit.landitbe;

import org.flywaydb.core.Flyway;

/** GitHub Actions에서 애플리케이션 배포 전 Flyway 마이그레이션만 실행한다. */
public final class FlywayMigrationRunner {

  private FlywayMigrationRunner() {}

  /** 동작을 수행한다. */
  public static void main(String[] args) {
    Flyway.configure()
        .dataSource(requiredEnv("DB_URL"), requiredEnv("DB_USERNAME"), requiredEnv("DB_PASSWORD"))
        .locations("classpath:db/migration", "classpath:db/postgresql")
        .load()
        .migrate();
  }

  private static String requiredEnv(String name) {
    String value = System.getenv(name);
    if (value == null || value.isBlank()) {
      throw new IllegalStateException(name + " is required");
    }
    return value;
  }
}
