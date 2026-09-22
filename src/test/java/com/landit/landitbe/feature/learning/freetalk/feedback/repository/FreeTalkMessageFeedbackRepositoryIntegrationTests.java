// 턴 교정의 조건부 갱신이 실제 DB에서 그 시도를 시작한 쪽의 결과만 반영하는지 검증한다.

package com.landit.landitbe.feature.learning.freetalk.feedback.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.landit.landitbe.feature.learning.conversation.domain.ProcessingStatus;
import com.landit.landitbe.feature.learning.freetalk.feedback.domain.FreeTalkMistakePattern;
import com.landit.landitbe.feature.learning.freetalk.feedback.dto.FreeTalkPatternUsageDraft;
import com.landit.landitbe.feature.learning.freetalk.feedback.dto.FreeTalkTurnCorrection;
import com.landit.landitbe.feature.learning.freetalk.feedback.service.FreeTalkMessageFeedbackService;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

/** 턴 교정의 조건부 갱신이 실제 DB에서 그 시도를 시작한 쪽의 결과만 반영하는지 검증한다. */
@ActiveProfiles("test")
@SpringBootTest
class FreeTalkMessageFeedbackRepositoryIntegrationTests {

  private static final long USER_ID = 998001L;
  private static final long LEARNING_SESSION_ID = 998002L;
  private static final long SESSION_HISTORY_ID = 998003L;
  private static final long MESSAGE_ID = 998004L;
  private static final LocalDateTime LEASE = LocalDateTime.of(2026, 9, 21, 21, 31, 30);
  private static final LocalDateTime NEXT_ATTEMPT_AT = LocalDateTime.of(2026, 9, 21, 21, 32, 0);

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private FreeTalkMessageFeedbackRepository repository;

  @Autowired private FreeTalkMessageFeedbackService feedbackService;

  @Autowired private TransactionTemplate transactionTemplate;

  @BeforeEach
  void seedMessage() {
    Long aiTutorId = jdbcTemplate.queryForObject("select min(id) from ai_tutor", Long.class);
    jdbcTemplate.update(
        """
        insert into user_profile (
            id, nickname, target_locale, base_locale, current_level, ai_tutor_id,
            push_permission_status, status, created_at, updated_at)
        values (?, 'correction-retry-user', 'EN', 'KR', 1, ?, 'NOT_DETERMINED',
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
    jdbcTemplate.update(
        """
        insert into session_history_message (
            id, session_history_id, message_sequence, turn_number, role, content,
            input_type, created_at, updated_at)
        values (?, ?, 1, 1, 'USER', 'I go to a gym.', 'TEXT', CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP)
        """,
        MESSAGE_ID,
        SESSION_HISTORY_ID);
  }

  @AfterEach
  void clearFixtures() {
    jdbcTemplate.update(
        "delete from free_talk_pattern_usage where session_history_message_id = ?", MESSAGE_ID);
    jdbcTemplate.update(
        "delete from free_talk_message_feedback where session_history_message_id = ?", MESSAGE_ID);
    jdbcTemplate.update("delete from session_history_message where id = ?", MESSAGE_ID);
    jdbcTemplate.update("delete from session_history where id = ?", SESSION_HISTORY_ID);
    jdbcTemplate.update("delete from learning_session where id = ?", LEARNING_SESSION_ID);
    jdbcTemplate.update("delete from user_profile where id = ?", USER_ID);
  }

  @DisplayName("첫 시도가 다시 해 볼 실패로 끝나면 준비 상태를 유지한 채 다음 시도 시각만 바꾼다.")
  @Test
  void releasesFirstAttemptForRetry() {
    seedFeedback("PREPARING", 1, null);

    assertThat(releaseForRetry(1, "")).isEqualTo(1);

    assertThat(feedbackRow())
        .containsEntry("PROCESSING_STATUS", "PREPARING")
        .containsEntry("ATTEMPTS", 1)
        .containsEntry("LEASE_UNTIL", Timestamp.valueOf(NEXT_ATTEMPT_AT))
        .containsEntry("ATTEMPT_TOKEN", null);
  }

  @DisplayName("복구 워커가 넘겨받은 시도는 그 선점 식별자로만 풀 수 있고, 풀면 식별자를 비운다.")
  @Test
  void releasesClaimedAttemptOnlyWithItsToken() {
    seedFeedback("PREPARING", 2, "token-2");

    assertThat(releaseForRetry(2, "other-token")).isZero();
    assertThat(releaseForRetry(2, "")).isZero();
    assertThat(feedbackRow()).containsEntry("LEASE_UNTIL", Timestamp.valueOf(LEASE));

    assertThat(releaseForRetry(2, "token-2")).isEqualTo(1);
    assertThat(feedbackRow())
        .containsEntry("LEASE_UNTIL", Timestamp.valueOf(NEXT_ATTEMPT_AT))
        .containsEntry("ATTEMPT_TOKEN", null);
  }

  @DisplayName("응답이 늦은 옛 시도는 이미 다음 시도로 넘어간 교정의 임대를 풀거나 실패로 확정하지 못한다.")
  @Test
  void ignoresLateResultOfAnEarlierAttempt() {
    seedFeedback("PREPARING", 2, "token-2");

    assertThat(releaseForRetry(1, "")).isZero();
    assertThat(failAttempt(1, "")).isZero();

    assertThat(feedbackRow())
        .containsEntry("PROCESSING_STATUS", "PREPARING")
        .containsEntry("LEASE_UNTIL", Timestamp.valueOf(LEASE))
        .containsEntry("ATTEMPT_TOKEN", "token-2");
  }

  @DisplayName("두 번째 시도까지 끝나 식별자가 비워진 뒤에 첫 시도의 응답이 늦게 와도, 시도 순번이 달라 반영되지 않는다.")
  @Test
  void ignoresLateFirstAttemptAfterLaterAttemptWasReleased() {
    // 첫 시도도 식별자가 없고, 풀린 두 번째 시도도 식별자가 없다. 둘을 가르는 것은 시도 순번뿐이다.
    seedFeedback("PREPARING", 2, null);

    assertThat(releaseForRetry(1, "")).isZero();
    assertThat(failAttempt(1, "")).isZero();

    assertThat(feedbackRow())
        .containsEntry("PROCESSING_STATUS", "PREPARING")
        .containsEntry("ATTEMPTS", 2)
        .containsEntry("LEASE_UNTIL", Timestamp.valueOf(LEASE));
  }

  @DisplayName("마지막 시도의 실패는 그 시도의 식별자로만 확정되고, 확정된 뒤에는 늦은 교정도 다음 시도도 반영되지 않는다.")
  @Test
  void failsLastAttemptForGood() {
    seedFeedback("PREPARING", 3, "token-3");

    assertThat(failAttempt(3, "other-token")).isZero();
    assertThat(failAttempt(3, "token-3")).isEqualTo(1);

    assertThat(feedbackRow())
        .containsEntry("PROCESSING_STATUS", "FAILED")
        .containsEntry("LEASE_UNTIL", null)
        .containsEntry("ATTEMPT_TOKEN", null);
    assertThat(completeWithCorrection()).isZero();
    assertThat(releaseForRetry(3, "")).isZero();
    assertThat(feedbackRow())
        .containsEntry("PROCESSING_STATUS", "FAILED")
        .containsEntry("BETTER_SENTENCE", null);
  }

  @DisplayName("교정이 확정되면 임대와 선점 식별자를 비우고, 그 뒤의 실패 처리는 교정을 지우지 못한다.")
  @Test
  void clearsLeaseWhenCorrectionCompletesAndKeepsItAgainstLaterFailure() {
    seedFeedback("PREPARING", 2, "token-2");

    assertThat(completeWithCorrection()).isEqualTo(1);

    assertThat(feedbackRow())
        .containsEntry("PROCESSING_STATUS", "COMPLETED")
        .containsEntry("BETTER_SENTENCE", "I went to the gym.")
        .containsEntry("WRONG_SPAN", "go")
        .containsEntry("BETTER_SPAN", "went")
        .containsEntry("LEASE_UNTIL", null)
        .containsEntry("ATTEMPT_TOKEN", null);
    assertThat(failAttempt(2, "token-2")).isZero();
    assertThat(releaseForRetry(2, "token-2")).isZero();
    assertThat(feedbackRow()).containsEntry("PROCESSING_STATUS", "COMPLETED");
  }

  private int releaseForRetry(int attempts, String token) {
    return transactionTemplate.execute(
        status ->
            repository.releaseForRetry(
                MESSAGE_ID, attempts, token, NEXT_ATTEMPT_AT, ProcessingStatus.PREPARING));
  }

  private int failAttempt(int attempts, String token) {
    return transactionTemplate.execute(
        status ->
            repository.failAttempt(
                MESSAGE_ID, attempts, token, ProcessingStatus.FAILED, ProcessingStatus.PREPARING));
  }

  @DisplayName("저장한 강조 구절은 교정을 읽을 때 그대로 돌아온다.")
  @Test
  void roundTripsSpansThroughCorrection() {
    seedFeedback("PREPARING", 1, null);
    assertThat(completeWithCorrection()).isEqualTo(1);

    FreeTalkTurnCorrection.Sentence sentence =
        repository
            .findBySessionHistoryMessageId(MESSAGE_ID)
            .orElseThrow()
            .toCorrection()
            .sentence();
    assertThat(sentence.wrongSpan()).isEqualTo("go");
    assertThat(sentence.betterSpan()).isEqualTo("went");
  }

  @DisplayName("사용례는 교정이 실제로 반영된 트랜잭션에서만 저장되고, 늦게 온 중복 결과는 교정도 사용례도 넣지 않는다.")
  @Test
  void savesUsagesOnlyWithTheCompletionThatTookEffect() {
    seedFeedback("PREPARING", 1, null);
    FreeTalkTurnCorrection correction =
        FreeTalkTurnCorrection.completed(
            new FreeTalkTurnCorrection.Sentence(
                "I go to a gym.", "I went to the gym.", "과거형", FreeTalkMistakePattern.TENSE),
            true,
            List.of(
                new FreeTalkPatternUsageDraft(
                    FreeTalkMistakePattern.TENSE, "I go to a gym.", "go", false),
                new FreeTalkPatternUsageDraft(
                    FreeTalkMistakePattern.ARTICLE, "I go to a gym.", "a gym", false)));

    assertThat(feedbackService.completeIfPreparing(MESSAGE_ID, correction)).isEqualTo(1);
    assertThat(feedbackService.completeIfPreparing(MESSAGE_ID, correction)).isZero();

    assertThat(
            jdbcTemplate.queryForList(
                "select pattern from free_talk_pattern_usage where session_history_message_id = ?"
                    + " order by id",
                String.class,
                MESSAGE_ID))
        .containsExactly("TENSE", "ARTICLE");
    assertThat(
            jdbcTemplate.queryForObject(
                "select session_history_id from free_talk_pattern_usage"
                    + " where session_history_message_id = ? limit 1",
                Long.class,
                MESSAGE_ID))
        .isEqualTo(SESSION_HISTORY_ID);
  }

  private int completeWithCorrection() {
    return transactionTemplate.execute(
        status ->
            repository.updateIfPreparing(
                MESSAGE_ID,
                ProcessingStatus.COMPLETED,
                true,
                "I go to a gym.",
                "I went to the gym.",
                "어제 일이라 과거형이에요.",
                FreeTalkMistakePattern.TENSE,
                null,
                null,
                null,
                "go",
                "went",
                ProcessingStatus.PREPARING));
  }

  private void seedFeedback(String status, int attempts, String token) {
    jdbcTemplate.update(
        "insert into free_talk_message_feedback (session_history_message_id, session_history_id,"
            + " processing_status, attempts, lease_until, attempt_token, created_at, updated_at)"
            + " values (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
        MESSAGE_ID,
        SESSION_HISTORY_ID,
        status,
        attempts,
        Timestamp.valueOf(LEASE),
        token);
  }

  private Map<String, Object> feedbackRow() {
    return jdbcTemplate.queryForMap(
        "select processing_status, attempts, lease_until, attempt_token, better_sentence,"
            + " wrong_span, better_span"
            + " from free_talk_message_feedback where session_history_message_id = ?",
        MESSAGE_ID);
  }
}
