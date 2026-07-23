// GitHub Actions에서 애플리케이션 배포 전 Flyway 마이그레이션만 실행한다.

package com.landit.landitbe;

import org.flywaydb.core.Flyway;

/** GitHub Actions에서 애플리케이션 배포 전 Flyway 마이그레이션만 실행한다. */
public final class FlywayMigrationRunner {

  private FlywayMigrationRunner() {}

  /**
   * 환경 변수의 데이터베이스 접속 정보로 Flyway 마이그레이션을 실행한다.
   *
   * @param args 실행 인자
   * @throws IllegalStateException 필수 DB 환경 변수가 없거나 비어 있을 때
   */
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
