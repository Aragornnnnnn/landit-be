// Apple 사용자 이전 JDBC 저장소의 대상 선정과 원자적 갱신을 검증한다.

package com.landit.landitbe.feature.auth.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AppleUserMigrationRepositoryTest {

  private String databaseUrl;
  private Connection databaseKeeper;
  private AppleUserMigrationRepository repository;

  @BeforeEach
  void setUp() throws Exception {
    databaseUrl =
        "jdbc:h2:mem:apple-migration-" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
    databaseKeeper = openConnection();
    createOauthIdentityTable();
    applyMigration();
    repository = new AppleUserMigrationRepository(databaseUrl, "sa", "");
  }

  @AfterEach
  void tearDown() throws SQLException {
    databaseKeeper.close();
  }

  @Test
  void initializePendingTargetsOnlyActiveAppleIdentities() throws Exception {
    insertIdentity(1L, "APPLE", "old-apple-sub", "apple@example.com", "ACTIVE");
    insertIdentity(2L, "APPLE", "unlinked-apple-sub", null, "UNLINKED");
    insertIdentity(3L, "GOOGLE", "google-sub", "google@example.com", "ACTIVE");

    assertThat(repository.initializePending()).isEqualTo(1);

    assertThat(repository.findCandidates(AppleUserMigrationPhase.PREPARE))
        .extracting(AppleUserMigrationCandidate::providerUserId)
        .containsExactly("old-apple-sub");
    assertThat(repository.initializePending()).isZero();
  }

  @Test
  void migrationCanBeAppliedRepeatedly() throws Exception {
    applyMigration();

    assertThat(repository.summarize(AppleUserMigrationPhase.PREPARE))
        .isEqualTo(new AppleUserMigrationSummary(0, 0, 0, 0));
  }

  @Test
  void preparedAndFailedRowsAreSelectedOnlyForTheirRetryablePhase() throws Exception {
    insertIdentity(1L, "APPLE", "old-sub-1", null, "ACTIVE");
    insertIdentity(2L, "APPLE", "old-sub-2", null, "ACTIVE");
    repository.initializePending();
    List<AppleUserMigrationCandidate> candidates =
        repository.findCandidates(AppleUserMigrationPhase.PREPARE);

    repository.markPrepared(candidates.get(0).migrationId(), "transfer-sub-1");
    repository.markFailed(
        candidates.get(1).migrationId(), AppleUserMigrationPhase.PREPARE, "APPLE_HTTP_400");

    assertThat(repository.findCandidates(AppleUserMigrationPhase.PREPARE))
        .extracting(AppleUserMigrationCandidate::providerUserId)
        .containsExactly("old-sub-2");
    assertThat(repository.findCandidates(AppleUserMigrationPhase.COMPLETE))
        .extracting(AppleUserMigrationCandidate::transferSub)
        .containsExactly("transfer-sub-1");
  }

  @Test
  void completeKeepsUserProfileAndUpdatesTheSameIdentityAtomically() throws Exception {
    insertIdentity(7L, "APPLE", "old-apple-sub", "old@privaterelay.appleid.com", "ACTIVE");
    repository.initializePending();
    AppleUserMigrationCandidate candidate =
        repository.findCandidates(AppleUserMigrationPhase.PREPARE).getFirst();
    repository.markPrepared(candidate.migrationId(), "transfer-sub");

    repository.complete(candidate.migrationId(), "new-apple-sub", "new@privaterelay.appleid.com");

    assertThat(readIdentity(candidate.oauthIdentityId()))
        .isEqualTo(
            new IdentityRow(
                7L, "new-apple-sub", "new@privaterelay.appleid.com", "APPLE", "ACTIVE"));
    assertThat(readMigrationStatus(candidate.migrationId())).isEqualTo("COMPLETED");
    assertThat(repository.findCandidates(AppleUserMigrationPhase.COMPLETE)).isEmpty();
  }

  @Test
  void completePreservesExistingEmailWhenAppleDoesNotReturnPrivateRelayEmail() throws Exception {
    insertIdentity(7L, "APPLE", "old-apple-sub", "person@example.com", "ACTIVE");
    repository.initializePending();
    AppleUserMigrationCandidate candidate =
        repository.findCandidates(AppleUserMigrationPhase.PREPARE).getFirst();
    repository.markPrepared(candidate.migrationId(), "transfer-sub");

    repository.complete(candidate.migrationId(), "new-apple-sub", null);

    assertThat(readIdentity(candidate.oauthIdentityId()).providerEmail())
        .isEqualTo("person@example.com");
  }

  @Test
  void duplicateRecipientSubDoesNotOverwriteEitherIdentity() throws Exception {
    insertIdentity(7L, "APPLE", "old-apple-sub", null, "ACTIVE");
    insertIdentity(8L, "APPLE", "new-apple-sub", null, "ACTIVE");
    repository.initializePending();
    AppleUserMigrationCandidate candidate =
        repository.findCandidates(AppleUserMigrationPhase.PREPARE).stream()
            .filter(item -> item.providerUserId().equals("old-apple-sub"))
            .findFirst()
            .orElseThrow();
    repository.markPrepared(candidate.migrationId(), "transfer-sub");

    assertThatThrownBy(() -> repository.complete(candidate.migrationId(), "new-apple-sub", null))
        .isInstanceOf(AppleUserMigrationException.class)
        .extracting("failureCode")
        .isEqualTo("IDENTITY_CONFLICT");

    assertThat(readIdentity(candidate.oauthIdentityId()).providerUserId())
        .isEqualTo("old-apple-sub");
    assertThat(readMigrationStatus(candidate.migrationId())).isEqualTo("PREPARED");
  }

  @Test
  void inactiveIdentityDoesNotBecomeCompleted() throws Exception {
    insertIdentity(7L, "APPLE", "old-apple-sub", null, "ACTIVE");
    repository.initializePending();
    AppleUserMigrationCandidate candidate =
        repository.findCandidates(AppleUserMigrationPhase.PREPARE).getFirst();
    repository.markPrepared(candidate.migrationId(), "transfer-sub");
    updateIdentityStatus(candidate.oauthIdentityId(), "UNLINKED");

    assertThatThrownBy(() -> repository.complete(candidate.migrationId(), "new-apple-sub", null))
        .isInstanceOf(AppleUserMigrationException.class)
        .extracting("failureCode")
        .isEqualTo("IDENTITY_STATE_INVALID");

    assertThat(readIdentity(candidate.oauthIdentityId()).providerUserId())
        .isEqualTo("old-apple-sub");
    assertThat(readMigrationStatus(candidate.migrationId())).isEqualTo("PREPARED");
  }

  @Test
  void summarizeUsesPhaseSpecificSuccessAndFailureCounts() throws Exception {
    insertIdentity(1L, "APPLE", "old-sub-1", null, "ACTIVE");
    insertIdentity(2L, "APPLE", "old-sub-2", null, "ACTIVE");
    insertIdentity(3L, "APPLE", "old-sub-3", null, "ACTIVE");
    repository.initializePending();
    List<AppleUserMigrationCandidate> candidates =
        repository.findCandidates(AppleUserMigrationPhase.PREPARE);
    repository.markPrepared(candidates.get(0).migrationId(), "transfer-sub-1");
    repository.markFailed(
        candidates.get(1).migrationId(), AppleUserMigrationPhase.PREPARE, "APPLE_HTTP_400");

    assertThat(repository.summarize(AppleUserMigrationPhase.PREPARE))
        .isEqualTo(new AppleUserMigrationSummary(3, 1, 1, 2));

    repository.markFailed(
        candidates.get(0).migrationId(), AppleUserMigrationPhase.COMPLETE, "APPLE_HTTP_500");

    assertThat(repository.summarize(AppleUserMigrationPhase.COMPLETE))
        .isEqualTo(new AppleUserMigrationSummary(3, 0, 1, 3));
  }

  private void createOauthIdentityTable() throws SQLException {
    try (Statement statement = databaseKeeper.createStatement()) {
      statement.execute(
          """
          CREATE TABLE oauth_identity (
              id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
              user_profile_id BIGINT NOT NULL,
              provider VARCHAR(20) NOT NULL,
              provider_user_id VARCHAR(255) NOT NULL,
              provider_email VARCHAR(255),
              status VARCHAR(20) NOT NULL,
              created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
              updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
          )
          """);
      statement.execute(
          "CREATE UNIQUE INDEX uk_oauth_identity_active_provider_user "
              + "ON oauth_identity (provider, provider_user_id)");
    }
  }

  private void applyMigration() throws SQLException, IOException {
    String migration;
    try (var input =
        getClass().getResourceAsStream("/db/migration/R__create_apple_user_migration.sql")) {
      if (input == null) {
        throw new IOException("migration resource is missing");
      }
      migration = new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
    try (Statement statement = databaseKeeper.createStatement()) {
      for (String sql : migration.split(";")) {
        if (!sql.isBlank()) {
          statement.execute(sql);
        }
      }
    }
  }

  private void insertIdentity(
      long userProfileId,
      String provider,
      String providerUserId,
      String providerEmail,
      String status)
      throws SQLException {
    try (Connection connection = openConnection();
        PreparedStatement statement =
            connection.prepareStatement(
                """
                INSERT INTO oauth_identity (
                    user_profile_id, provider, provider_user_id, provider_email, status
                ) VALUES (?, ?, ?, ?, ?)
                """)) {
      statement.setLong(1, userProfileId);
      statement.setString(2, provider);
      statement.setString(3, providerUserId);
      statement.setString(4, providerEmail);
      statement.setString(5, status);
      statement.executeUpdate();
    }
  }

  private IdentityRow readIdentity(long identityId) throws SQLException {
    try (Connection connection = openConnection();
        PreparedStatement statement =
            connection.prepareStatement(
                """
                SELECT user_profile_id, provider_user_id, provider_email, provider, status
                FROM oauth_identity
                WHERE id = ?
                """)) {
      statement.setLong(1, identityId);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return new IdentityRow(
            resultSet.getLong("user_profile_id"),
            resultSet.getString("provider_user_id"),
            resultSet.getString("provider_email"),
            resultSet.getString("provider"),
            resultSet.getString("status"));
      }
    }
  }

  private String readMigrationStatus(long migrationId) throws SQLException {
    try (Connection connection = openConnection();
        PreparedStatement statement =
            connection.prepareStatement("SELECT status FROM apple_user_migration WHERE id = ?")) {
      statement.setLong(1, migrationId);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getString("status");
      }
    }
  }

  private void updateIdentityStatus(long identityId, String status) throws SQLException {
    try (Connection connection = openConnection();
        PreparedStatement statement =
            connection.prepareStatement("UPDATE oauth_identity SET status = ? WHERE id = ?")) {
      statement.setString(1, status);
      statement.setLong(2, identityId);
      statement.executeUpdate();
    }
  }

  private Connection openConnection() throws SQLException {
    return DriverManager.getConnection(databaseUrl, "sa", "");
  }

  private record IdentityRow(
      long userProfileId,
      String providerUserId,
      String providerEmail,
      String provider,
      String status) {}
}
