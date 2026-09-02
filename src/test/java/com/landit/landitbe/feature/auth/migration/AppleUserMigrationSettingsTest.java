// Apple 사용자 이전 CLI 환경 설정의 단계별 필수값 검증을 확인한다.

package com.landit.landitbe.feature.auth.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AppleUserMigrationSettingsTest {

  @Test
  void prepareRequiresRecipientTeamId() {
    Map<String, String> environment = validEnvironment("PREPARE");
    environment.remove("APPLE_MIGRATION_TARGET_TEAM_ID");

    assertThatThrownBy(() -> AppleUserMigrationSettings.from(environment))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("APPLE_MIGRATION_TARGET_TEAM_ID is required");
  }

  @Test
  void completeDoesNotRequireRecipientTeamId() {
    Map<String, String> environment = validEnvironment("COMPLETE");
    environment.remove("APPLE_MIGRATION_TARGET_TEAM_ID");

    AppleUserMigrationSettings settings = AppleUserMigrationSettings.from(environment);

    assertThat(settings.phase()).isEqualTo(AppleUserMigrationPhase.COMPLETE);
    assertThat(settings.recipientTeamId()).isNull();
    assertThat(settings.apiBaseUri()).isEqualTo(URI.create("https://appleid.apple.com"));
  }

  @Test
  void customApiBaseUriCanBeInjectedForContractTests() {
    Map<String, String> environment = validEnvironment("PREPARE");
    environment.put("APPLE_MIGRATION_API_BASE_URL", "http://127.0.0.1:9999");

    assertThat(AppleUserMigrationSettings.from(environment).apiBaseUri())
        .isEqualTo(URI.create("http://127.0.0.1:9999"));
  }

  @Test
  void missingSecretErrorDoesNotExposeAnotherConfiguredValue() {
    Map<String, String> environment = validEnvironment("PREPARE");
    environment.remove("APPLE_MIGRATION_CLIENT_ID");

    assertThatThrownBy(() -> AppleUserMigrationSettings.from(environment))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("APPLE_MIGRATION_CLIENT_ID is required")
        .message()
        .doesNotContain("super-secret-client-jwt");
  }

  @Test
  void rejectsUnknownPhaseWithoutEchoingCredentials() {
    Map<String, String> environment = validEnvironment("DELETE");

    assertThatThrownBy(() -> AppleUserMigrationSettings.from(environment))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("APPLE_MIGRATION_PHASE must be PREPARE or COMPLETE")
        .message()
        .doesNotContain("super-secret-client-jwt");
  }

  private Map<String, String> validEnvironment(String phase) {
    Map<String, String> environment = new HashMap<>();
    environment.put("DB_URL", "jdbc:postgresql://database/landit");
    environment.put("DB_USERNAME", "landit");
    environment.put("DB_PASSWORD", "database-password");
    environment.put("APPLE_MIGRATION_PHASE", phase);
    environment.put("APPLE_MIGRATION_CLIENT_ID", "app.client");
    environment.put("APPLE_MIGRATION_CLIENT_SECRET", "super-secret-client-jwt");
    environment.put("APPLE_MIGRATION_TARGET_TEAM_ID", "RECIPIENT_TEAM");
    return environment;
  }
}
