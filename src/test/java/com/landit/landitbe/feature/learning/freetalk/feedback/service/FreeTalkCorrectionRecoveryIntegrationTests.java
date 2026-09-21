// 끝나지 못한 턴 교정이 실제 DB에서 다시 시도되고 반드시 끝 상태로 확정되는지 검증한다.

package com.landit.landitbe.feature.learning.freetalk.feedback.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.landit.landitbe.config.session.FreeTalkCorrectionRetryProperties;
import com.landit.landitbe.feature.learning.freetalk.client.ai.AiFreeTalkClient;
import com.landit.landitbe.feature.learning.freetalk.feedback.domain.FreeTalkMistakePattern;
import com.landit.landitbe.feature.learning.freetalk.feedback.dto.FreeTalkCorrectionAttempt;
import com.landit.landitbe.feature.learning.freetalk.feedback.dto.FreeTalkTurnCorrection;
import com.landit.landitbe.feature.learning.freetalk.innerthought.client.ai.AiFreeTalkInnerThoughtRequest;
import com.landit.landitbe.feature.learning.freetalk.innerthought.client.ai.AiFreeTalkInnerThoughtResult;
import com.landit.landitbe.shared.domain.InnerThoughtType;
import io.micrometer.core.instrument.MeterRegistry;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** 끝나지 못한 턴 교정이 실제 DB에서 다시 시도되고 반드시 끝 상태로 확정되는지 검증한다. */
@ActiveProfiles("test")
@SpringBootTest
class FreeTalkCorrectionRecoveryIntegrationTests {

  private static final long USER_ID = 999001L;
  private static final long LEARNING_SESSION_ID = 999002L;
  private static final long SESSION_HISTORY_ID = 999003L;
  private static final long MESSAGE_ID = 999004L;
  private static final FreeTalkTurnCorrection CORRECTION =
      FreeTalkTurnCorrection.completed(
          new FreeTalkTurnCorrection.Sentence(
              "I go to a gym.",
              "I went to the gym.",
              "어제 일이라 과거형이에요.",
              FreeTalkMistakePattern.TENSE),
          true);

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private FreeTalkMessageFeedbackService feedbackService;

  @Autowired private FreeTalkCorrectionRetryProperties retryProperties;

  @Autowired private MeterRegistry meterRegistry;

  @Autowired private Clock clock;

  private final AiFreeTalkClient aiClient = mock(AiFreeTalkClient.class);
  private final FreeTalkCorrectionRequestService requestService =
      mock(FreeTalkCorrectionRequestService.class);
  private FreeTalkCorrectionRecoveryService recoveryService;
  private List<Map<String, Object>> foreignLeases = List.of();

  @BeforeEach
  void setUp() {
    // 복구 전용 스레드 대신 호출한 스레드에서 바로 도는 실행기를 써서, 복구 한 바퀴가 끝난 뒤에 결과를 확인한다.
    recoveryService =
        new FreeTalkCorrectionRecoveryService(
            feedbackService, requestService, aiClient, retryProperties, new SyncTaskExecutor());
    when(requestService.rebuild(anyLong()))
        .thenReturn(Optional.of(mock(AiFreeTalkInnerThoughtRequest.class)));
    // 같은 DB를 쓰는 다른 테스트가 남긴 준비 상태 교정이 한 번에 넘겨받는 자리를 차지하지 않게 복구 대상에서 잠시 뺀다.
    // 남의 행을 바꾼 채로 두지 않도록 원래 임대를 기억했다가 끝나면 되돌린다.
    foreignLeases =
        jdbcTemplate.queryForList(
            "select id, lease_until from free_talk_message_feedback"
                + " where processing_status = 'PREPARING'");
    jdbcTemplate.update(
        "update free_talk_message_feedback set lease_until = ? where processing_status ="
            + " 'PREPARING'",
        Timestamp.valueOf(LocalDateTime.now(clock).plusYears(1)));
    seedMessage();
  }

  @AfterEach
  void clearFixtures() {
    for (Map<String, Object> foreign : foreignLeases) {
      jdbcTemplate.update(
          "update free_talk_message_feedback set lease_until = ?"
              + " where id = ? and processing_status = 'PREPARING'",
          foreign.get("LEASE_UNTIL"),
          foreign.get("ID"));
    }
    jdbcTemplate.update(
        "delete from free_talk_message_feedback where session_history_id = ?", SESSION_HISTORY_ID);
    jdbcTemplate.update(
        "delete from session_history_message where session_history_id = ?", SESSION_HISTORY_ID);
    jdbcTemplate.update("delete from session_history where id = ?", SESSION_HISTORY_ID);
    jdbcTemplate.update("delete from learning_session where id = ?", LEARNING_SESSION_ID);
    jdbcTemplate.update("delete from user_profile where id = ?", USER_ID);
  }

  @DisplayName("콜백이 사라져 준비 상태로 남은 교정을 넘겨받아 다시 요청하고, 이미 끝난 속마음은 건드리지 않는다.")
  @Test
  void recoversStalePreparingCorrectionWithoutTouchingInnerThought() {
    seedFeedback(1, expired(), null);
    when(aiClient.generateInnerThought(any())).thenReturn(result("다시 만든 속마음", CORRECTION));
    final double recoveredBefore = counter("recovered");

    recoveryService.recover();

    assertThat(feedbackRow())
        .containsEntry("PROCESSING_STATUS", "COMPLETED")
        .containsEntry("BETTER_SENTENCE", "I went to the gym.")
        .containsEntry("MISTAKE_PATTERN", "TENSE")
        .containsEntry("ATTEMPTS", 2)
        .containsEntry("LEASE_UNTIL", null)
        .containsEntry("ATTEMPT_TOKEN", null);
    // 속마음은 대화 중에만 의미가 있어 다시 요청한 응답의 속마음은 버린다.
    assertThat(innerThoughtRow())
        .containsEntry("INNER_THOUGHT", null)
        .containsEntry("INNER_THOUGHT_PROCESSING_STATUS", "FAILED");
    assertThat(counter("recovered") - recoveredBefore).isEqualTo(1.0);
  }

  @DisplayName("AI가 계속 판정을 돌려주지 못하면 간격을 두고 다시 시도하다 마지막 시도 뒤에 실패로 확정하고, 그 뒤로는 바뀌지 않는다.")
  @Test
  void failsForGoodAfterLastAttemptAndStaysFailed() {
    seedFeedback(1, expired(), null);
    when(aiClient.generateInnerThought(any()))
        .thenReturn(result("속마음", FreeTalkTurnCorrection.unavailable()));

    recoveryService.recover();

    // 두 번째 시도가 실패하면 1분 뒤로 미뤄져 그 전에는 다시 집히지 않는다.
    assertThat(feedbackRow())
        .containsEntry("PROCESSING_STATUS", "PREPARING")
        .containsEntry("ATTEMPTS", 2)
        .containsEntry("ATTEMPT_TOKEN", null);
    assertThat(leaseUntil()).isAfter(LocalDateTime.now(clock).plusSeconds(50));
    recoveryService.recover();
    verify(aiClient, times(1)).generateInnerThought(any());

    expireLease();
    recoveryService.recover();

    assertThat(feedbackRow())
        .containsEntry("PROCESSING_STATUS", "FAILED")
        .containsEntry("ATTEMPTS", 3)
        .containsEntry("LEASE_UNTIL", null);
    verify(aiClient, times(2)).generateInnerThought(any());
    // 실패로 확정된 뒤에 늦은 교정이 와도, 복구가 또 돌아도 기록은 그대로다.
    assertThat(feedbackService.completeIfPreparing(MESSAGE_ID, CORRECTION)).isZero();
    recoveryService.recover();
    assertThat(feedbackRow())
        .containsEntry("PROCESSING_STATUS", "FAILED")
        .containsEntry("BETTER_SENTENCE", null);
    verify(aiClient, times(2)).generateInnerThought(any());
  }

  @DisplayName("마지막 시도를 하던 서버가 결과를 남기지 못하고 사라진 교정은 AI를 다시 부르지 않고 실패로 확정한다.")
  @Test
  void abandonsCorrectionWhoseLastAttemptNeverReported() {
    seedFeedback(3, expired(), "dead-worker-token");

    recoveryService.recover();

    assertThat(feedbackRow())
        .containsEntry("PROCESSING_STATUS", "FAILED")
        .containsEntry("ATTEMPTS", 3)
        .containsEntry("ATTEMPT_TOKEN", null);
    verify(aiClient, never()).generateInnerThought(any());
  }

  @DisplayName("마지막 시도의 응답을 아직 기다려 주는 중이면 시도를 다 썼어도 실패로 확정하지 않는다.")
  @Test
  void doesNotAbandonLastAttemptThatIsStillInFlight() {
    seedFeedback(3, LocalDateTime.now(clock).plusSeconds(60), "running-token");

    assertThat(feedbackService.claimNextAttempt(MESSAGE_ID, 3)).isEmpty();

    assertThat(feedbackRow())
        .containsEntry("PROCESSING_STATUS", "PREPARING")
        .containsEntry("ATTEMPT_TOKEN", "running-token");
  }

  @DisplayName("실패 확정은 찾았을 때의 시도 횟수가 그대로일 때만 한다. 그사이 시도 횟수가 달라진 교정은 건드리지 않는다.")
  @Test
  void abandonsOnlyWhenAttemptsAreUnchangedSinceFound() {
    // 최대 시도가 더 컸던 때 네 번째 시도까지 간 교정이다. 찾았을 때 3회였다고 믿는 쪽의 확정은 반영되지 않는다.
    seedFeedback(4, expired(), "dead-worker-token");

    assertThat(feedbackService.claimNextAttempt(MESSAGE_ID, 3)).isEmpty();
    assertThat(feedbackRow()).containsEntry("PROCESSING_STATUS", "PREPARING");

    assertThat(feedbackService.claimNextAttempt(MESSAGE_ID, 4)).isEmpty();
    assertThat(feedbackRow())
        .containsEntry("PROCESSING_STATUS", "FAILED")
        .containsEntry("ATTEMPT_TOKEN", null);
  }

  @DisplayName("시도 정보가 생기기 전부터 준비 상태로 남아 있던 교정(시도 0회·임대 없음)도 넘겨받는다.")
  @Test
  void recoversCorrectionLeftPreparingBeforeRetryColumnsExisted() {
    seedFeedback(0, null, null);
    when(aiClient.generateInnerThought(any())).thenReturn(result("속마음", CORRECTION));

    recoveryService.recover();

    assertThat(feedbackRow())
        .containsEntry("PROCESSING_STATUS", "COMPLETED")
        .containsEntry("ATTEMPTS", 1);
  }

  @DisplayName("아직 응답을 기다려 주는 중인 교정은 넘겨받지 않는다.")
  @Test
  void leavesCorrectionWhoseLeaseIsStillValid() {
    seedFeedback(1, LocalDateTime.now(clock).plusSeconds(60), null);
    final double contendedBefore = counter("contended");

    recoveryService.recover();

    assertThat(feedbackRow())
        .containsEntry("PROCESSING_STATUS", "PREPARING")
        .containsEntry("ATTEMPTS", 1);
    verify(aiClient, never()).generateInnerThought(any());
    // 복구 대상으로 집히지도 않아야 한다. 집혔다가 선점에서 막힌 것이라면 주기마다 헛된 선점 시도가 쌓인다.
    assertThat(counter("contended")).isEqualTo(contendedBefore);
  }

  @DisplayName("찾은 뒤 선점하기 전에 다른 쪽이 임대를 새로 잡았으면 선점하지 못한다.")
  @Test
  void cannotClaimCorrectionWhoseLeaseWasRenewedMeanwhile() {
    // 찾을 때는 임대가 끝나 있었지만, 선점 직전에 같은 시도 횟수로 임대만 새로 잡힌 상태다.
    seedFeedback(1, LocalDateTime.now(clock).plusSeconds(60), null);

    assertThat(feedbackService.claimNextAttempt(MESSAGE_ID, 1)).isEmpty();
    assertThat(feedbackRow()).containsEntry("ATTEMPTS", 1).containsEntry("ATTEMPT_TOKEN", null);
  }

  @DisplayName("응답이 늦은 첫 시도의 계약 위반은, 그사이 복구가 넘겨받아 아직 돌고 있는 시도를 끝내지 못하고 그 시도의 교정이 저장된다.")
  @Test
  void lateInvalidFirstAttemptDoesNotKillTheAttemptInFlight() {
    // 첫 시도의 임대가 끝나 복구가 두 번째 시도를 선점했고 AI를 부르는 중이다.
    seedFeedback(1, expired(), null);
    FreeTalkCorrectionAttempt second =
        feedbackService.claimNextAttempt(MESSAGE_ID, 1).orElseThrow();

    feedbackService.completeFirstAttempt(MESSAGE_ID, FreeTalkTurnCorrection.failed());

    assertThat(feedbackRow())
        .containsEntry("PROCESSING_STATUS", "PREPARING")
        .containsEntry("ATTEMPT_TOKEN", second.token());

    feedbackService.completeAttempt(second, CORRECTION);

    assertThat(feedbackRow())
        .containsEntry("PROCESSING_STATUS", "COMPLETED")
        .containsEntry("BETTER_SENTENCE", "I went to the gym.");
  }

  @DisplayName("늦게 도착했어도 판정을 마친 교정은 받는다. 그 뒤에 끝난 새 시도의 결과는 반영되지 않는다.")
  @Test
  void acceptsLateCompletedFirstAttemptAndIgnoresTheLaterOne() {
    seedFeedback(1, expired(), null);
    FreeTalkCorrectionAttempt second =
        feedbackService.claimNextAttempt(MESSAGE_ID, 1).orElseThrow();

    feedbackService.completeFirstAttempt(MESSAGE_ID, CORRECTION);
    feedbackService.completeAttempt(second, FreeTalkTurnCorrection.failed());

    assertThat(feedbackRow())
        .containsEntry("PROCESSING_STATUS", "COMPLETED")
        .containsEntry("BETTER_SENTENCE", "I went to the gym.");
  }

  @DisplayName("AI 호출이 예외로 끝나면 그 시도만 끝내고 다음 시도를 기다리게 한다.")
  @Test
  void releasesAttemptWhenAiCallThrows() {
    seedFeedback(1, expired(), null);
    when(aiClient.generateInnerThought(any())).thenThrow(new IllegalStateException("비밀 발화"));

    recoveryService.recover();

    assertThat(feedbackRow())
        .containsEntry("PROCESSING_STATUS", "PREPARING")
        .containsEntry("ATTEMPTS", 2)
        .containsEntry("ATTEMPT_TOKEN", null);
  }

  @DisplayName("계약과 다른 교정이 돌아오면 다시 해도 같으므로 시도가 남아 있어도 바로 실패로 확정한다.")
  @Test
  void failsImmediatelyOnContractViolation() {
    seedFeedback(1, expired(), null);
    when(aiClient.generateInnerThought(any()))
        .thenReturn(result("속마음", FreeTalkTurnCorrection.failed()));

    recoveryService.recover();

    assertThat(feedbackRow())
        .containsEntry("PROCESSING_STATUS", "FAILED")
        .containsEntry("ATTEMPTS", 2);
  }

  @DisplayName("대화 기록이 없어 요청을 다시 만들 수 없는 교정은 실패로 확정한다.")
  @Test
  void failsCorrectionThatCannotBeRebuilt() {
    seedFeedback(1, expired(), null);
    when(requestService.rebuild(MESSAGE_ID)).thenReturn(Optional.empty());

    recoveryService.recover();

    assertThat(feedbackRow()).containsEntry("PROCESSING_STATUS", "FAILED");
    verify(aiClient, never()).generateInnerThought(any());
  }

  @DisplayName("끝나지 못한 교정이 몰려 있어도 복구 한 번은 설정한 처리량만큼만 넘겨받아 AI 호출이 그 안에 묶인다.")
  @Test
  void claimsNoMoreThanBatchSizePerRun() {
    seedFeedback(1, expired(), null);
    for (int index = 1; index <= 11; index++) {
      seedExtraMessageWithFeedback(MESSAGE_ID + index, index + 1, 1, expired());
    }
    when(aiClient.generateInnerThought(any())).thenReturn(result("속마음", CORRECTION));

    recoveryService.recover();

    assertThat(retryProperties.batchSize()).isEqualTo(10);
    verify(aiClient, times(10)).generateInnerThought(any());
    assertThat(countByStatus("COMPLETED")).isEqualTo(10);
    assertThat(countByStatus("PREPARING")).isEqualTo(2);

    recoveryService.recover();

    assertThat(countByStatus("COMPLETED")).isEqualTo(12);
  }

  @DisplayName("방금 멈춘 교정은 시도 정보가 생기기 전부터 쌓여 있던 교정들 뒤로 밀리지 않고 첫 번째 복구에서 처리된다.")
  @Test
  void recoversFreshlyStalledCorrectionBeforeLegacyBacklog() {
    // 임대가 없는 옛 교정 11건이 먼저 만들어져 있고(낮은 ID), 방금 멈춘 교정이 가장 늦게 만들어졌다.
    for (int index = 1; index <= 11; index++) {
      seedExtraMessageWithFeedback(MESSAGE_ID + index, index + 1, 0, null);
    }
    seedFeedback(1, expired(), null);
    when(aiClient.generateInnerThought(any())).thenReturn(result("속마음", CORRECTION));

    recoveryService.recover();

    assertThat(feedbackRow()).containsEntry("PROCESSING_STATUS", "COMPLETED");
    assertThat(countByStatus("PREPARING")).isEqualTo(2);
  }

  @DisplayName("재시도 설정은 기본값(최대 3회, 0초·1분·5분, 10건)으로 읽히고 테스트에서는 주기 복구가 꺼져 있다.")
  @Test
  void bindsRetrySettingsFromConfiguration() {
    assertThat(retryProperties.maxAttempts()).isEqualTo(3);
    assertThat(retryProperties.retryDelays())
        .containsExactly(Duration.ZERO, Duration.ofMinutes(1), Duration.ofMinutes(5));
    assertThat(retryProperties.batchSize()).isEqualTo(10);
    assertThat(retryProperties.schedulingEnabled()).isFalse();
  }

  @DisplayName("두 복구 워커가 같은 교정을 동시에 집어도 한 곳만 선점한다.")
  @Test
  void letsOnlyOneWorkerClaimTheSameCorrection() throws Exception {
    seedFeedback(1, expired(), null);
    CountDownLatch start = new CountDownLatch(1);
    Callable<Optional<FreeTalkCorrectionAttempt>> claim =
        () -> {
          start.await();
          return feedbackService.claimNextAttempt(MESSAGE_ID, 1);
        };
    ExecutorService workers = Executors.newFixedThreadPool(2);
    try {
      Future<Optional<FreeTalkCorrectionAttempt>> first = workers.submit(claim);
      Future<Optional<FreeTalkCorrectionAttempt>> second = workers.submit(claim);
      start.countDown();

      assertThat(List.of(first.get(), second.get())).filteredOn(Optional::isPresent).hasSize(1);
    } finally {
      workers.shutdownNow();
    }
    assertThat(feedbackRow()).containsEntry("ATTEMPTS", 2);
  }

  private AiFreeTalkInnerThoughtResult result(
      String innerThought, FreeTalkTurnCorrection correction) {
    return new AiFreeTalkInnerThoughtResult(innerThought, InnerThoughtType.GOOD, correction);
  }

  private double counter(String outcome) {
    return meterRegistry.counter("landit.free_talk.correction.retry", "outcome", outcome).count();
  }

  private int countByStatus(String status) {
    return jdbcTemplate.queryForObject(
        "select count(*) from free_talk_message_feedback"
            + " where session_history_id = ? and processing_status = ?",
        Integer.class,
        SESSION_HISTORY_ID,
        status);
  }

  private void seedExtraMessageWithFeedback(
      long messageId, int sequence, int attempts, LocalDateTime leaseUntil) {
    jdbcTemplate.update(
        """
        insert into session_history_message (
            id, session_history_id, message_sequence, turn_number, role, content,
            input_type, inner_thought_processing_status, created_at, updated_at)
        values (?, ?, ?, ?, 'USER', 'I go to a gym.', 'TEXT', 'FAILED', CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP)
        """,
        messageId,
        SESSION_HISTORY_ID,
        sequence,
        sequence);
    jdbcTemplate.update(
        "insert into free_talk_message_feedback (session_history_message_id, session_history_id,"
            + " processing_status, attempts, lease_until, created_at, updated_at)"
            + " values (?, ?, 'PREPARING', ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
        messageId,
        SESSION_HISTORY_ID,
        attempts,
        leaseUntil == null ? null : Timestamp.valueOf(leaseUntil));
  }

  private LocalDateTime expired() {
    return LocalDateTime.now(clock).minusMinutes(5);
  }

  private void expireLease() {
    jdbcTemplate.update(
        "update free_talk_message_feedback set lease_until = ? where session_history_message_id ="
            + " ?",
        Timestamp.valueOf(expired()),
        MESSAGE_ID);
  }

  private LocalDateTime leaseUntil() {
    return ((Timestamp) feedbackRow().get("LEASE_UNTIL")).toLocalDateTime();
  }

  private Map<String, Object> feedbackRow() {
    return jdbcTemplate.queryForMap(
        "select processing_status, attempts, lease_until, attempt_token, better_sentence,"
            + " mistake_pattern from free_talk_message_feedback"
            + " where session_history_message_id = ?",
        MESSAGE_ID);
  }

  private Map<String, Object> innerThoughtRow() {
    return jdbcTemplate.queryForMap(
        "select inner_thought, inner_thought_processing_status from session_history_message"
            + " where id = ?",
        MESSAGE_ID);
  }

  private void seedFeedback(int attempts, LocalDateTime leaseUntil, String token) {
    jdbcTemplate.update(
        "insert into free_talk_message_feedback (session_history_message_id, session_history_id,"
            + " processing_status, attempts, lease_until, attempt_token, created_at, updated_at)"
            + " values (?, ?, 'PREPARING', ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
        MESSAGE_ID,
        SESSION_HISTORY_ID,
        attempts,
        leaseUntil == null ? null : Timestamp.valueOf(leaseUntil),
        token);
  }

  private void seedMessage() {
    Long aiTutorId = jdbcTemplate.queryForObject("select min(id) from ai_tutor", Long.class);
    jdbcTemplate.update(
        """
        insert into user_profile (
            id, nickname, target_locale, base_locale, current_level, ai_tutor_id,
            push_permission_status, status, created_at, updated_at)
        values (?, 'correction-recovery-user', 'EN', 'KR', 1, ?, 'NOT_DETERMINED',
            'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        USER_ID,
        aiTutorId);
    jdbcTemplate.update(
        """
        insert into learning_session (
            id, user_profile_id, session_type, ai_tutor_id, target_locale, base_locale,
            input_mode, status, started_at, created_at, updated_at)
        values (?, ?, 'FREE_TALK', ?, 'EN', 'KR', 'MIXED', 'IN_PROGRESS',
            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        LEARNING_SESSION_ID,
        USER_ID,
        aiTutorId);
    jdbcTemplate.update(
        """
        insert into session_history (
            id, learning_session_id, user_profile_id, session_type, target_locale,
            base_locale, started_at, ended_at, duration_seconds, user_message_count, created_at)
        values (?, ?, ?, 'FREE_TALK', 'EN', 'KR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
            0, 1, CURRENT_TIMESTAMP)
        """,
        SESSION_HISTORY_ID,
        LEARNING_SESSION_ID,
        USER_ID);
    // 속마음은 폴링 시간 초과로 이미 실패 처리된 발화다.
    jdbcTemplate.update(
        """
        insert into session_history_message (
            id, session_history_id, message_sequence, turn_number, role, content,
            input_type, inner_thought_processing_status, created_at, updated_at)
        values (?, ?, 1, 1, 'USER', 'I go to a gym.', 'TEXT', 'FAILED', CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP)
        """,
        MESSAGE_ID,
        SESSION_HISTORY_ID);
  }
}
