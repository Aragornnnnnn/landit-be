// 알림 예약 의도와 발송 소유권을 원자적으로 저장한다.

package com.landit.landitbe.feature.notification.repository;

import com.landit.landitbe.feature.notification.dto.NotificationJob;
import com.landit.landitbe.feature.notification.dto.TrialReminderSettings;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** NotificationJobService만 사용하는 예약 및 발송 저장소다. */
@Repository
@RequiredArgsConstructor
public class NotificationJobRepository {
  private final JdbcTemplate jdbc;

  /**
   * 새 발송 의도를 저장한다. 호출자는 사용자 잠금으로 동일 구독 생성 요청을 직렬화한다.
   *
   * @param job 새 작업
   * @param now 현재 시각
   */
  public void insert(NotificationJob job, Instant now) {
    jdbc.update(
        """
        INSERT INTO notification_job
          (id, kind, user_profile_id, product_id, store, environment, expires_at, scheduled_at,
           recipient, next_attempt_at, created_at, updated_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        job.id(),
        job.kind(),
        job.userProfileId(),
        job.productId(),
        job.store(),
        job.environment(),
        time(job.expiresAt()),
        time(job.scheduledAt()),
        job.recipient(),
        time(now),
        time(now),
        time(now));
  }

  /**
   * 식별자로 작업을 조회한다.
   *
   * @param id 작업 ID
   * @return 존재하는 작업
   */
  public Optional<NotificationJob> find(UUID id) {
    return jdbc.query("SELECT * FROM notification_job WHERE id = ?", this::map, id).stream()
        .findFirst();
  }

  /**
   * 예약 발행을 재시도할 작업을 제한된 수로 조회한다.
   *
   * @param now 현재 시각
   * @return 예약 미등록 작업
   */
  public List<NotificationJob> pendingReservations(Instant now) {
    return jdbc.query(
        """
        SELECT * FROM notification_job WHERE reservation_state = 'PENDING'
          AND next_attempt_at <= ? AND status = 'PENDING'
          AND (kind = 'TEST_EMAIL' OR EXISTS (
            SELECT 1 FROM trial_reminder_settings WHERE id = 1
              AND ((kind = 'TRIAL_PUSH' AND push_enabled)
                OR (kind = 'TRIAL_EMAIL' AND email_enabled))))
        ORDER BY next_attempt_at, id LIMIT 100
        """,
        this::map,
        time(now));
  }

  /**
   * 여러 서버 가운데 하나가 예약 발행을 선점한다.
   *
   * @param id 작업 ID
   * @param now 현재 시각
   * @return 선점 여부
   */
  public boolean reserve(UUID id, Instant now) {
    return jdbc.update(
            """
            UPDATE notification_job SET next_attempt_at = ?, updated_at = ?
            WHERE id = ? AND reservation_state = 'PENDING' AND next_attempt_at <= ?
            """,
            time(now.plusSeconds(60)),
            time(now),
            id,
            time(now))
        == 1;
  }

  /**
   * 외부 예약 또는 SQS 접수를 기록한다.
   *
   * @param id 작업 ID
   */
  public void registered(UUID id) {
    jdbc.update("UPDATE notification_job SET reservation_state = 'REGISTERED' WHERE id = ?", id);
  }

  /**
   * 발송 소유권을 선점한다. 오래된 푸시는 기존 토큰별 멱등 처리를 통해 복구한다.
   *
   * @param id 작업 ID
   * @param token 새 소유권
   * @param now 현재 시각
   * @return 선점 여부
   */
  public boolean claim(UUID id, UUID token, Instant now) {
    return jdbc.update(
            """
            UPDATE notification_job SET status = 'PROCESSING', claim_token = ?, claimed_at = ?, updated_at = ?
            WHERE id = ? AND (status = 'PENDING' OR (kind = 'TRIAL_PUSH'
              AND status = 'PROCESSING' AND claimed_at < ?))
            """,
            token,
            time(now),
            time(now),
            id,
            time(now.minusSeconds(300)))
        == 1;
  }

  /**
   * 응답을 기록하지 못한 이메일은 불확실 상태로 고정해 자동 중복 발송을 막는다.
   *
   * @param id 작업 ID
   * @param now 현재 시각
   */
  public void recoverUnknownEmail(UUID id, Instant now) {
    jdbc.update(
        """
        UPDATE notification_job SET status = 'UNKNOWN', result_code = 'INTERRUPTED', updated_at = ?
        WHERE id = ? AND kind <> 'TRIAL_PUSH' AND status = 'PROCESSING' AND claimed_at < ?
        """,
        time(now),
        id,
        time(now.minusSeconds(300)));
  }

  /**
   * 현재 처리 소유자만 결과를 저장한다.
   *
   * @param job 처리 소유권이 있는 작업
   * @param status 저장 상태
   * @param code 결과 코드
   * @param messageId 제공자 접수 ID
   */
  public void finish(NotificationJob job, String status, String code, String messageId) {
    jdbc.update(
        """
        UPDATE notification_job SET status = ?, result_code = ?, provider_message_id = ?,
          updated_at = CURRENT_TIMESTAMP WHERE id = ? AND claim_token = ? AND status = 'PROCESSING'
        """,
        status,
        code,
        messageId,
        job.id(),
        job.claimToken());
  }

  /**
   * 현재 채널 설정을 읽는다.
   *
   * @return 채널 설정
   */
  public TrialReminderSettings settings() {
    return jdbc.queryForObject(
        "SELECT * FROM trial_reminder_settings WHERE id = 1",
        (rs, row) ->
            new TrialReminderSettings(
                rs.getBoolean("push_enabled"), rs.getBoolean("email_enabled")));
  }

  /**
   * 채널 설정을 저장한다.
   *
   * @param settings 새 설정
   */
  public void updateSettings(TrialReminderSettings settings) {
    jdbc.update(
        "UPDATE trial_reminder_settings SET push_enabled = ?, email_enabled = ? WHERE id = 1",
        settings.pushEnabled(),
        settings.emailEnabled());
  }

  private NotificationJob map(ResultSet rs, int row) throws SQLException {
    return new NotificationJob(
        rs.getObject("id", UUID.class),
        rs.getString("kind"),
        rs.getLong("user_profile_id"),
        rs.getString("product_id"),
        rs.getString("store"),
        rs.getString("environment"),
        instant(rs, "expires_at"),
        instant(rs, "scheduled_at"),
        rs.getString("recipient"),
        rs.getString("status"),
        instant(rs, "claimed_at"),
        rs.getObject("claim_token", UUID.class),
        rs.getString("result_code"),
        rs.getString("provider_message_id"));
  }

  private Instant instant(ResultSet rs, String column) throws SQLException {
    OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
    return value == null ? null : value.toInstant();
  }

  private OffsetDateTime time(Instant value) {
    return value == null ? null : value.atOffset(ZoneOffset.UTC);
  }
}
