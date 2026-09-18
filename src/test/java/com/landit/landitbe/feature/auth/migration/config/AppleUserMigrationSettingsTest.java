// Apple 사용자 이전 CLI 환경 설정의 단계별 필수값 검증을 확인한다.

package com.landit.landitbe.feature.auth.migration.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.landit.landitbe.feature.auth.migration.domain.AppleUserMigrationPhase;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AppleUserMigrationSettingsTest {

  @DisplayName("Apple 이전 준비 단계에는 수신 팀 ID가 필요하다.")
  @Test
  void prepareRequiresRecipientTeamId() {
    Map<String, String> environment = validEnvironment("PREPARE");
    environment.remove("APPLE_MIGRATION_TARGET_TEAM_ID");

    assertThatThrownBy(() -> AppleUserMigrationSettings.from(environment))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("APPLE_MIGRATION_TARGET_TEAM_ID is required");
  }

  @DisplayName("Apple 이전 완료 단계에는 수신 팀 ID가 필요하지 않다.")
  @Test
  void completeDoesNotRequireRecipientTeamId() {
    Map<String, String> environment = validEnvironment("COMPLETE");
    environment.remove("APPLE_MIGRATION_TARGET_TEAM_ID");

    AppleUserMigrationSettings settings = AppleUserMigrationSettings.from(environment);

    assertThat(settings.phase()).isEqualTo(AppleUserMigrationPhase.COMPLETE);
    assertThat(settings.recipientTeamId()).isNull();
    assertThat(settings.apiBaseUri()).isEqualTo(URI.create("https://appleid.apple.com"));
  }

  @DisplayName("Apple 이전 계약 테스트에서 API 기본 URI를 교체할 수 있다.")
  @Test
  void customApiBaseUriCanBeInjectedForContractTests() {
    Map<String, String> environment = validEnvironment("PREPARE");
    environment.put("APPLE_MIGRATION_API_BASE_URL", "http://127.0.0.1:9999");

    assertThat(AppleUserMigrationSettings.from(environment).apiBaseUri())
        .isEqualTo(URI.create("http://127.0.0.1:9999"));
  }

  @DisplayName("Apple 시크릿 누락 오류는 다른 설정값을 노출하지 않는다.")
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

  @DisplayName("알 수 없는 Apple 이전 단계를 거부하며 자격 증명을 노출하지 않는다.")
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
