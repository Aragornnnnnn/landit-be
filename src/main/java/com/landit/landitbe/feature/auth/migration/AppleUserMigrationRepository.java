// Apple 사용자 이전 상태와 OAuth identity를 JDBC로 일관되게 갱신한다.

package com.landit.landitbe.feature.auth.migration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/** Apple 사용자 이전 상태와 OAuth identity를 JDBC로 일관되게 갱신한다. */
public class AppleUserMigrationRepository {

  private static final String DATABASE_ERROR = "DATABASE_ERROR";

  private final String databaseUrl;
  private final String username;
  private final String password;

  /**
   * 데이터베이스 접속 정보로 저장소를 생성한다.
   *
   * @param databaseUrl JDBC URL
   * @param username 데이터베이스 사용자명
   * @param password 데이터베이스 비밀번호
   */
  public AppleUserMigrationRepository(String databaseUrl, String username, String password) {
    this.databaseUrl = databaseUrl;
    this.username = username;
    this.password = password;
  }

  /**
   * 아직 등록되지 않은 활성 Apple OAuth identity를 이전 대상으로 등록한다.
   *
   * @return 새로 등록한 대상 수
   * @throws AppleUserMigrationException 데이터베이스 작업에 실패할 때
   */
  public int initializePending() {
    String sql =
        """
        INSERT INTO apple_user_migration (oauth_identity_id, status)
        SELECT identity.id, 'PENDING'
        FROM oauth_identity identity
        WHERE identity.provider = 'APPLE'
          AND identity.status = 'ACTIVE'
          AND NOT EXISTS (
              SELECT 1
              FROM apple_user_migration migration
              WHERE migration.oauth_identity_id = identity.id
          )
        """;
    try (Connection connection = openConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      return statement.executeUpdate();
    } catch (SQLException exception) {
      throw new AppleUserMigrationException(DATABASE_ERROR, exception);
    }
  }

  /**
   * 실행 단계에서 재시도할 수 있는 대상을 ID 순서로 조회한다.
   *
   * @param phase 실행 단계
   * @return 이전 대상 목록
   * @throws AppleUserMigrationException 데이터베이스 작업에 실패할 때
   */
  public List<AppleUserMigrationCandidate> findCandidates(AppleUserMigrationPhase phase) {
    String statuses =
        phase == AppleUserMigrationPhase.PREPARE
            ? "('PENDING', 'PREPARE_FAILED')"
            : "('PREPARED', 'COMPLETE_FAILED')";
    String sql =
        """
        SELECT migration.id AS migration_id,
               identity.id AS oauth_identity_id,
               identity.provider_user_id,
               migration.transfer_sub
        FROM apple_user_migration migration
        JOIN oauth_identity identity ON identity.id = migration.oauth_identity_id
        WHERE migration.status IN %s
        ORDER BY identity.id
        """
            .formatted(statuses);
    try (Connection connection = openConnection();
        PreparedStatement statement = connection.prepareStatement(sql);
        ResultSet resultSet = statement.executeQuery()) {
      List<AppleUserMigrationCandidate> candidates = new ArrayList<>();
      while (resultSet.next()) {
        candidates.add(
            new AppleUserMigrationCandidate(
                resultSet.getLong("migration_id"),
                resultSet.getLong("oauth_identity_id"),
                resultSet.getString("provider_user_id"),
                resultSet.getString("transfer_sub")));
      }
      return candidates;
    } catch (SQLException exception) {
      throw new AppleUserMigrationException(DATABASE_ERROR, exception);
    }
  }

  /**
   * Apple이 발급한 이전 식별자를 저장하고 준비 완료로 표시한다.
   *
   * @param migrationId 사용자 이전 상태 ID
   * @param transferSub Apple 이전 식별자
   * @throws AppleUserMigrationException 상태가 바뀌었거나 데이터베이스 작업에 실패할 때
   */
  public void markPrepared(long migrationId, String transferSub) {
    String sql =
        """
        UPDATE apple_user_migration
        SET transfer_sub = ?,
            status = 'PREPARED',
            failure_code = NULL,
            attempt_count = attempt_count + 1,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = ?
          AND status IN ('PENDING', 'PREPARE_FAILED')
        """;
    executeConditionalUpdate(sql, transferSub, migrationId, "MIGRATION_STATE_INVALID");
  }

  /**
   * 사용자 이전 실패 상태와 비식별 오류 코드를 저장한다.
   *
   * @param migrationId 사용자 이전 상태 ID
   * @param phase 실패한 실행 단계
   * @param failureCode 비식별 오류 코드
   * @throws AppleUserMigrationException 상태가 바뀌었거나 데이터베이스 작업에 실패할 때
   */
  public void markFailed(long migrationId, AppleUserMigrationPhase phase, String failureCode) {
    String nextStatus =
        phase == AppleUserMigrationPhase.PREPARE ? "PREPARE_FAILED" : "COMPLETE_FAILED";
    String allowedStatuses =
        phase == AppleUserMigrationPhase.PREPARE
            ? "('PENDING', 'PREPARE_FAILED')"
            : "('PREPARED', 'COMPLETE_FAILED')";
    String sql =
        """
        UPDATE apple_user_migration
        SET status = ?,
            failure_code = ?,
            attempt_count = attempt_count + 1,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = ?
          AND status IN %s
        """
            .formatted(allowedStatuses);
    try (Connection connection = openConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, nextStatus);
      statement.setString(2, failureCode);
      statement.setLong(3, migrationId);
      requireOneUpdated(statement.executeUpdate(), "MIGRATION_STATE_INVALID");
    } catch (SQLException exception) {
      throw new AppleUserMigrationException(DATABASE_ERROR, exception);
    }
  }

  /**
   * 같은 OAuth identity를 새 Team 식별자로 바꾸고 이전 완료 상태를 함께 커밋한다.
   *
   * @param migrationId 사용자 이전 상태 ID
   * @param providerUserId 새 Team Apple 사용자 식별자
   * @param providerEmail 새 Team private relay 이메일, Apple이 반환하지 않으면 null
   * @throws AppleUserMigrationException 대상 상태가 올바르지 않거나 식별자가 충돌하거나 DB 작업에 실패할 때
   */
  public void complete(long migrationId, String providerUserId, String providerEmail) {
    try (Connection connection = openConnection()) {
      connection.setAutoCommit(false);
      try {
        CompletionTarget target = findCompletionTarget(connection, migrationId);
        validateCompletionTarget(connection, target, providerUserId);
        updateIdentity(connection, target.oauthIdentityId(), providerUserId, providerEmail);
        markCompleted(connection, migrationId);
        connection.commit();
      } catch (SQLException | RuntimeException exception) {
        rollback(connection, exception);
        if (exception instanceof AppleUserMigrationException migrationException) {
          throw migrationException;
        }
        throw new AppleUserMigrationException(DATABASE_ERROR, exception);
      }
    } catch (SQLException exception) {
      throw new AppleUserMigrationException(DATABASE_ERROR, exception);
    }
  }

  /**
   * 현재 단계의 대상, 성공, 실패, 미완료 수를 조회한다.
   *
   * @param phase 실행 단계
   * @return 단계별 집계
   * @throws AppleUserMigrationException 데이터베이스 작업에 실패할 때
   */
  public AppleUserMigrationSummary summarize(AppleUserMigrationPhase phase) {
    String successStatuses =
        phase == AppleUserMigrationPhase.PREPARE ? "('PREPARED', 'COMPLETED')" : "('COMPLETED')";
    String failureStatus =
        phase == AppleUserMigrationPhase.PREPARE ? "PREPARE_FAILED" : "COMPLETE_FAILED";
    String sql =
        """
        SELECT COUNT(*) AS target_count,
               SUM(CASE WHEN status IN %s THEN 1 ELSE 0 END) AS success_count,
               SUM(CASE WHEN status = ? THEN 1 ELSE 0 END) AS failure_count
        FROM apple_user_migration
        """
            .formatted(successStatuses);
    try (Connection connection = openConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, failureStatus);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        long targetCount = resultSet.getLong("target_count");
        long successCount = resultSet.getLong("success_count");
        long failureCount = resultSet.getLong("failure_count");
        return new AppleUserMigrationSummary(
            targetCount, successCount, failureCount, targetCount - successCount);
      }
    } catch (SQLException exception) {
      throw new AppleUserMigrationException(DATABASE_ERROR, exception);
    }
  }

  private CompletionTarget findCompletionTarget(Connection connection, long migrationId)
      throws SQLException {
    String sql =
        """
        SELECT identity.id AS oauth_identity_id,
               identity.provider,
               identity.status AS identity_status,
               migration.status AS migration_status
        FROM apple_user_migration migration
        JOIN oauth_identity identity ON identity.id = migration.oauth_identity_id
        WHERE migration.id = ?
        FOR UPDATE
        """;
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, migrationId);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          throw new AppleUserMigrationException("MIGRATION_STATE_INVALID");
        }
        return new CompletionTarget(
            resultSet.getLong("oauth_identity_id"),
            resultSet.getString("provider"),
            resultSet.getString("identity_status"),
            resultSet.getString("migration_status"));
      }
    }
  }

  private void validateCompletionTarget(
      Connection connection, CompletionTarget target, String providerUserId) throws SQLException {
    if (!target.provider().equals("APPLE") || !target.identityStatus().equals("ACTIVE")) {
      throw new AppleUserMigrationException("IDENTITY_STATE_INVALID");
    }
    if (!target.migrationStatus().equals("PREPARED")
        && !target.migrationStatus().equals("COMPLETE_FAILED")) {
      throw new AppleUserMigrationException("MIGRATION_STATE_INVALID");
    }

    String sql =
        """
        SELECT COUNT(*)
        FROM oauth_identity
        WHERE provider = 'APPLE'
          AND status = 'ACTIVE'
          AND provider_user_id = ?
          AND id <> ?
        """;
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, providerUserId);
      statement.setLong(2, target.oauthIdentityId());
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        if (resultSet.getLong(1) > 0) {
          throw new AppleUserMigrationException("IDENTITY_CONFLICT");
        }
      }
    }
  }

  private void updateIdentity(
      Connection connection, long identityId, String providerUserId, String providerEmail)
      throws SQLException {
    String sql =
        """
        UPDATE oauth_identity
        SET provider_user_id = ?,
            provider_email = CASE WHEN ? IS NULL THEN provider_email ELSE ? END,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = ?
          AND provider = 'APPLE'
          AND status = 'ACTIVE'
        """;
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, providerUserId);
      if (providerEmail == null) {
        statement.setNull(2, Types.VARCHAR);
        statement.setNull(3, Types.VARCHAR);
      } else {
        statement.setString(2, providerEmail);
        statement.setString(3, providerEmail);
      }
      statement.setLong(4, identityId);
      requireOneUpdated(statement.executeUpdate(), "IDENTITY_STATE_INVALID");
    }
  }

  private void markCompleted(Connection connection, long migrationId) throws SQLException {
    String sql =
        """
        UPDATE apple_user_migration
        SET status = 'COMPLETED',
            failure_code = NULL,
            attempt_count = attempt_count + 1,
            updated_at = CURRENT_TIMESTAMP,
            completed_at = CURRENT_TIMESTAMP
        WHERE id = ?
          AND status IN ('PREPARED', 'COMPLETE_FAILED')
        """;
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, migrationId);
      requireOneUpdated(statement.executeUpdate(), "MIGRATION_STATE_INVALID");
    }
  }

  private void executeConditionalUpdate(
      String sql, String value, long migrationId, String failureCode) {
    try (Connection connection = openConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, value);
      statement.setLong(2, migrationId);
      requireOneUpdated(statement.executeUpdate(), failureCode);
    } catch (SQLException exception) {
      throw new AppleUserMigrationException(DATABASE_ERROR, exception);
    }
  }

  private void requireOneUpdated(int updatedCount, String failureCode) {
    if (updatedCount != 1) {
      throw new AppleUserMigrationException(failureCode);
    }
  }

  private void rollback(Connection connection, Exception original) {
    try {
      connection.rollback();
    } catch (SQLException rollbackException) {
      original.addSuppressed(rollbackException);
    }
  }

  private Connection openConnection() throws SQLException {
    return DriverManager.getConnection(databaseUrl, username, password);
  }

  private record CompletionTarget(
      long oauthIdentityId, String provider, String identityStatus, String migrationStatus) {}
}
