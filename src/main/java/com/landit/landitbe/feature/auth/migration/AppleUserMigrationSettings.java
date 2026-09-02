// Apple 사용자 이전 CLI 실행에 필요한 환경 설정을 검증해 전달한다.

package com.landit.landitbe.feature.auth.migration;

import java.net.URI;
import java.util.Locale;
import java.util.Map;

/**
 * Apple 사용자 이전 CLI 실행 설정이다.
 *
 * @param databaseUrl JDBC URL
 * @param databaseUsername 데이터베이스 사용자명
 * @param databasePassword 데이터베이스 비밀번호
 * @param phase 실행 단계
 * @param clientId 이전 대상 앱의 App ID 또는 Services ID
 * @param clientSecret 현재 Team의 client secret JWT
 * @param recipientTeamId 수신 Team ID, COMPLETE에서는 null 가능
 * @param apiBaseUri Apple ID API 기준 URI
 */
public record AppleUserMigrationSettings(
    String databaseUrl,
    String databaseUsername,
    String databasePassword,
    AppleUserMigrationPhase phase,
    String clientId,
    String clientSecret,
    String recipientTeamId,
    URI apiBaseUri) {

  private static final URI DEFAULT_API_BASE_URI = URI.create("https://appleid.apple.com");

  /**
   * 환경 변수 값에서 단계별 필수 설정을 읽고 검증한다.
   *
   * @param environment 환경 변수 맵
   * @return 검증된 실행 설정
   * @throws IllegalStateException 필수 설정이 없거나 실행 단계가 올바르지 않을 때
   */
  public static AppleUserMigrationSettings from(Map<String, String> environment) {
    AppleUserMigrationPhase phase = parsePhase(required(environment, "APPLE_MIGRATION_PHASE"));
    String recipientTeamId = environment.get("APPLE_MIGRATION_TARGET_TEAM_ID");
    if (phase == AppleUserMigrationPhase.PREPARE
        && (recipientTeamId == null || recipientTeamId.isBlank())) {
      throw new IllegalStateException("APPLE_MIGRATION_TARGET_TEAM_ID is required");
    }
    URI apiBaseUri =
        optionalUri(environment.get("APPLE_MIGRATION_API_BASE_URL"), DEFAULT_API_BASE_URI);
    return new AppleUserMigrationSettings(
        required(environment, "DB_URL"),
        required(environment, "DB_USERNAME"),
        required(environment, "DB_PASSWORD"),
        phase,
        required(environment, "APPLE_MIGRATION_CLIENT_ID"),
        required(environment, "APPLE_MIGRATION_CLIENT_SECRET"),
        blankToNull(recipientTeamId),
        apiBaseUri);
  }

  private static AppleUserMigrationPhase parsePhase(String value) {
    try {
      return AppleUserMigrationPhase.valueOf(value.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      throw new IllegalStateException(
          "APPLE_MIGRATION_PHASE must be PREPARE or COMPLETE", exception);
    }
  }

  private static URI optionalUri(String value, URI defaultValue) {
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    try {
      URI uri = URI.create(value);
      if (uri.getScheme() == null || uri.getHost() == null) {
        throw new IllegalArgumentException();
      }
      return uri;
    } catch (IllegalArgumentException exception) {
      throw new IllegalStateException("APPLE_MIGRATION_API_BASE_URL is invalid", exception);
    }
  }

  private static String required(Map<String, String> environment, String name) {
    String value = environment.get(name);
    if (value == null || value.isBlank()) {
      throw new IllegalStateException(name + " is required");
    }
    return value;
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }
}
