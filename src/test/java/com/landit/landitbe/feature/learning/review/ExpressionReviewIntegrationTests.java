// 복습의 실제 저장·HTTP 권한·시간 경계·동시성·푸시 예약을 검증한다.

package com.landit.landitbe.feature.learning.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.landitbe.feature.learning.review.dto.ReviewAnswerRequest;
import com.landit.landitbe.feature.learning.review.dto.ReviewOffer;
import com.landit.landitbe.feature.learning.review.dto.ReviewQuestion;
import com.landit.landitbe.feature.learning.review.dto.ReviewResponse;
import com.landit.landitbe.feature.learning.review.service.ExpressionReviewService;
import com.landit.landitbe.feature.notification.delivery.dto.SendPushNotificationCommand;
import com.landit.landitbe.feature.notification.delivery.service.NotificationDispatchService;
import com.landit.landitbe.feature.notification.domain.NotificationType;
import com.landit.landitbe.feature.notification.scheduled.service.LearningNotificationFrequencyService;
import com.landit.landitbe.feature.notification.scheduled.service.ReviewNotificationService;
import com.landit.landitbe.feature.notification.token.service.UserPushTokenDeliveryService;
import com.landit.landitbe.shared.domain.Locale;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.support.ExpressionPracticeFixture;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;

/** 전용 H2 DB와 고유 사용자로 격리하고 외부 푸시만 mock으로 대체한다. */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
    properties = {
      "landit.subscription.launched-at=2026-09-16T12:00:00+09:00",
      "spring.datasource.url=jdbc:h2:mem:expression-review;MODE=PostgreSQL;"
          + "DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH"
    })
class ExpressionReviewIntegrationTests {
  private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
  private static final Instant LAUNCH = Instant.parse("2026-09-16T03:00:00Z");
  @Autowired private MockMvc mvc;
  @Autowired private JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper = new ObjectMapper();
  @Autowired private ExpressionReviewService reviews;
  @Autowired private LearningNotificationFrequencyService frequency;
  @Autowired private PlatformTransactionManager transactionManager;
  @MockitoBean private Clock clock;
  private Instant now;

  @BeforeEach
  void setClockBeforeLaunch() {
    now = LAUNCH.minusSeconds(60);
    when(clock.instant()).thenAnswer(invocation -> now);
    when(clock.getZone()).thenReturn(ZONE);
  }

  @DisplayName("일일 알림을 재처리해도 복습 알림의 발송 가능 시각을 늦추지 않는다.")
  @Test
  void reviewDailyReplayMustNotPostponeTheReviewNotificationWindow() throws Exception {
    User user = user(0);
    var daily = command(user, "daily", NotificationType.DAILY_SCENARIO_REMINDER);
    final var review = command(user, "review", NotificationType.EXPRESSION_REVIEW);
    assertThat(frequency.reserveAll(List.of(daily))).containsExactly(daily);
    // 첫 daily 예약·발송 후 두 시간 뒤 동일 이벤트를 다시 처리한다.
    now = now.plusSeconds(2 * 3600);
    assertThat(frequency.reserveAll(List.of(daily))).containsExactly(daily);
    // ScheduledNotificationService.processPage가 재시도 때 markSent(now)로 기록하는 상태다.
    jdbcTemplate.update(
        """
        insert into user_notification_state
        (user_profile_id, notification_type, status, last_sent_at, created_at, updated_at)
        values (?, 'DAILY_SCENARIO_REMINDER', 'SENT', ?, ?, ?)
        """,
        user.id(),
        local(),
        local(),
        local());
    now = now.plusSeconds(3600);
    assertThat(frequency.reserveAll(List.of(review)))
        .as("최초 daily 예약으로부터 정확히 세 시간이 지난 복습은 허용해야 한다")
        .containsExactly(review);
  }

  @DisplayName("날짜별 예약이 없는 기존 일일 알림도 자정을 넘어 복습 알림 간격을 제한한다.")
  @Test
  void legacyDailyWithoutMatchingDateSlotStillBlocksReviewAcrossMidnight() throws Exception {
    User user = user(0);
    now = LAUNCH.minusSeconds(60);
    frequency.reserveAll(
        List.of(command(user, "yesterday", NotificationType.DAILY_SCENARIO_REMINDER)));
    now = now.plusSeconds(13 * 3600);
    jdbcTemplate.update(
        """
        insert into user_notification_state
        (user_profile_id, notification_type, status, last_sent_at, created_at, updated_at)
        values (?, 'DAILY_SCENARIO_REMINDER', 'SENT', ?, ?, ?)
        """,
        user.id(),
        local(),
        local(),
        local());
    var review = command(user, "review", NotificationType.EXPRESSION_REVIEW);
    now = now.plusSeconds(3 * 3600 - 1);
    assertThat(frequency.reserveAll(List.of(review))).isEmpty();
    now = now.plusSeconds(1);
    assertThat(frequency.reserveAll(List.of(review))).containsExactly(review);
  }

  @DisplayName("유효 문제 3개를 모으거나 후보가 소진될 때까지 불량 후보를 건너뛴다.")
  @ParameterizedTest
  @CsvSource({"30, 0", "30, 1", "30, 3", "29, 3"})
  void continuesPastInvalidCandidatesUntilThreeValidQuestionsOrExhaustion(
      int invalidCount, int validCount) throws Exception {
    User user = user(invalidCount + validCount);
    for (long expressionId : user.expressions().subList(0, invalidCount)) {
      jdbcTemplate.update(
          "update writing_expression set practice_examples_payload = '[]' format json where id = ?",
          expressionId);
    }
    var offered = reviews.offer(user.id(), date());
    if (validCount == 0) {
      assertThat(offered).isEmpty();
      assertThat(countReviews(user)).isZero();
      return;
    }
    assertThat(offered).isPresent();
    var questions = reviews.start(user.id(), offered.orElseThrow().reviewId()).questions();
    assertThat(questions)
        .extracting(ReviewQuestion::expressionId)
        .containsExactlyInAnyOrderElementsOf(
            user.expressions().subList(invalidCount, invalidCount + validCount));
    if (validCount == 3) {
      assertThat(questions)
          .extracting(question -> question.quiz().quizLanguage())
          .containsExactlyInAnyOrder(Locale.EN, Locale.EN, Locale.KR);
    }
  }

  @DisplayName("복습 조회와 시작 및 답안 제출은 인증이 필요하고 다른 사용자의 시작은 거부한다.")
  @Test
  void requiresAuthenticationAndReviewOwnership() throws Exception {
    User user = user(3);
    ReviewOffer offer = offer(user);
    String path = "/api/v1/reviews/" + offer.reviewId();
    mvc.perform(get(path)).andExpect(status().isUnauthorized());
    mvc.perform(post(path + "/start")).andExpect(status().isUnauthorized());
    mvc.perform(post(path + "/answers").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isUnauthorized());
    User other = user(0);
    mvc.perform(post(path + "/start").header("Authorization", "Bearer " + other.token()))
        .andExpect(status().isNotFound());
  }

  @DisplayName("복습은 언어별 문제 3개를 고정하고 콘텐츠 변경 후에도 같은 문제를 재사용한다.")
  @Test
  void fixesThreeQuestionsAndReusesSnapshotAfterContentChanges() throws Exception {
    User user = user(3);
    ReviewOffer offer = offer(user);
    String path = "/api/v1/reviews/" + offer.reviewId();
    mvc.perform(get(path).header("Authorization", "Bearer " + user.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("READY"))
        .andExpect(jsonPath("$.data.questions").isEmpty());
    mvc.perform(post(path + "/start").header("Authorization", "Bearer " + user.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.questions.length()").value(3));
    ReviewResponse initial = reviews.get(user.id(), offer.reviewId());
    assertThat(initial.questions())
        .extracting(q -> q.quiz().quizLanguage())
        .containsExactlyInAnyOrder(Locale.EN, Locale.EN, Locale.KR);
    assertThat(initial.questions())
        .extracting(ReviewQuestion::expressionId)
        .containsExactlyInAnyOrderElementsOf(user.expressions());
    assertThat(
            initial.questions().stream()
                .filter(q -> q.quiz().quizLanguage() == Locale.KR)
                .findFirst()
                .orElseThrow()
                .quiz()
                .writingSentenceAcceptedAnswers())
        .hasSize(2);
    jdbcTemplate.update(
        "update writing_expression set practice_examples_payload = '[]' format json where id = ?",
        user.expressions().getFirst());
    now = now.plusSeconds(300);
    assertThat(reviews.start(user.id(), offer.reviewId())).isEqualTo(initial);
    assertThat(reviews.offer(user.id(), date())).isEmpty();
  }

  @DisplayName("오답은 문제를 뒤로 이동하고 같은 제출을 재전송해도 오답 횟수를 중복 증가시키지 않는다.")
  @Test
  void wrongAnswerMovesQuestionToEndAndReplaysSubmission() throws Exception {
    User user = user(3);
    UUID id = offer(user).reviewId();
    ReviewResponse state = reviews.start(user.id(), id);
    ReviewQuestion first = current(state);
    ReviewAnswerRequest wrong =
        new ReviewAnswerRequest(UUID.randomUUID(), first.questionId(), List.of("wrong"));
    var failed = reviews.answer(user.id(), id, wrong);
    assertThat(failed.correct()).isFalse();
    assertThat(failed.review().currentQuestionId()).isNotEqualTo(first.questionId());
    assertThat(reviews.answer(user.id(), id, wrong)).isEqualTo(failed);
    assertThat(failed.review().questions().getFirst().wrongCount()).isEqualTo(1);
  }

  @DisplayName("정답과 두 번째 오답을 모두 종료로 처리하며 완료 후 재진입과 재전송도 일관된다.")
  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void completesReviewWithExhaustedQuestionsAndPreservesGrading(boolean includeCorrect)
      throws Exception {
    User user = user(3);
    UUID id = offer(user).reviewId();
    ReviewResponse state = reviews.start(user.id(), id);
    if (includeCorrect) {
      var first = current(state);
      state =
          reviews
              .answer(
                  user.id(),
                  id,
                  new ReviewAnswerRequest(
                      UUID.randomUUID(),
                      first.questionId(),
                      first.quiz().writingSentenceAcceptedAnswers().getFirst()))
              .review();
    }
    ReviewAnswerRequest last = null;
    int wrongAttempts = includeCorrect ? 4 : 6;
    for (int attempt = 0; attempt < wrongAttempts; attempt++) {
      last =
          new ReviewAnswerRequest(UUID.randomUUID(), state.currentQuestionId(), List.of("wrong"));
      var answer = reviews.answer(user.id(), id, last);
      assertThat(answer.correct()).isFalse();
      state = answer.review();
      if (attempt < wrongAttempts - 1) {
        assertThat(state.status()).isEqualTo("IN_PROGRESS");
        assertThat(current(state).completedAt()).isNull();
        assertThat(current(state).wrongCount()).isLessThan(2);
      }
    }
    assertThat(state.status()).isEqualTo("COMPLETED");
    assertThat(state.currentQuestionId()).isNull();
    assertThat(state.questions()).allSatisfy(q -> assertThat(q.completedAt()).isNotNull());
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from expression_review_submission"
                    + " where review_id = ? and correct = false",
                Integer.class,
                id))
        .isEqualTo(wrongAttempts);
    var replay = reviews.answer(user.id(), id, last);
    assertThat(replay.correct()).isFalse();
    assertThat(replay.review()).isEqualTo(state);
    UUID finalQuestionId = last.questionId();
    assertThatThrownBy(
            () ->
                reviews.answer(
                    user.id(),
                    id,
                    new ReviewAnswerRequest(UUID.randomUUID(), finalQuestionId, List.of("wrong"))))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("현재 풀 문제");
    now = now.plusSeconds(2 * 86400);
    assertThat(reviews.start(user.id(), id)).isEqualTo(state);
    mvc.perform(get("/api/v1/reviews/{id}", id).header("Authorization", "Bearer " + user.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("COMPLETED"))
        .andExpect(jsonPath("$.data.currentQuestionId").doesNotExist());
  }

  @DisplayName("두 번째 오답으로 종료한 표현도 실제 종료 시각부터 최근 복습 제외 기간을 적용한다.")
  @Test
  void exhaustedExpressionUsesCompletionTimeForNextReview() throws Exception {
    User user = user(1);
    UUID id = offer(user).reviewId();
    UUID questionId = reviews.start(user.id(), id).currentQuestionId();
    now = now.plusSeconds(23 * 3600);
    for (int attempt = 0; attempt < 2; attempt++) {
      reviews.answer(
          user.id(), id, new ReviewAnswerRequest(UUID.randomUUID(), questionId, List.of("wrong")));
    }
    subscribe(user, "ACTIVE", local().plusDays(30));
    now = LAUNCH.minusSeconds(60).plusSeconds(3 * 86400);
    assertThat(reviews.offer(user.id(), date())).isEmpty();
    now = now.plusSeconds(23 * 3600);
    assertThat(offer(user).reviewId()).isNotEqualTo(id);
  }

  @DisplayName("기존 오답 이력의 두 번째 시각으로 종료를 보정하고 미진행 문제와 원본 채점은 보존한다.")
  @ParameterizedTest
  @ValueSource(ints = {1, 2})
  void backfillsLegacyExhaustedQuestionsWithoutCompletingUnansweredOnes(int questionCount)
      throws Exception {
    User user = user(questionCount);
    UUID id = offer(user).reviewId();
    ReviewResponse initial = reviews.start(user.id(), id);
    UUID questionId = initial.currentQuestionId();
    LocalDateTime startedAt = local();
    for (int attempt = 1; attempt <= 3; attempt++) {
      jdbcTemplate.update(
          """
          insert into expression_review_submission
          (review_id, submission_id, question_id, answer_json, correct, created_at)
          values (?, ?, ?, '[]', false, ?)
          """,
          id,
          UUID.randomUUID(),
          questionId,
          startedAt.plusSeconds(attempt));
    }
    jdbcTemplate.update(
        "update expression_review_question set wrong_count = 3 where id = ?", questionId);
    var migration =
        new ResourceDatabasePopulator(
            new ClassPathResource("db/migration/V125__complete_exhausted_expression_reviews.sql"));
    migration.execute(jdbcTemplate.getDataSource());
    var result = reviews.get(user.id(), id);
    assertThat(result.questions().getFirst().completedAt()).isEqualTo(startedAt.plusSeconds(2));
    assertThat(result.questions().getFirst().wrongCount()).isEqualTo(3);
    if (questionCount == 1) {
      assertThat(result.status()).isEqualTo("COMPLETED");
      assertThat(result.completedAt()).isEqualTo(startedAt.plusSeconds(2));
      assertThat(result.currentQuestionId()).isNull();
    } else {
      assertThat(result.status()).isEqualTo("IN_PROGRESS");
      assertThat(result.completedAt()).isNull();
      assertThat(result.currentQuestionId()).isEqualTo(initial.questions().get(1).questionId());
      assertThat(result.questions().get(1).completedAt()).isNull();
    }
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from expression_review_submission"
                    + " where review_id = ? and correct = false",
                Integer.class,
                id))
        .isEqualTo(3);
    migration.execute(jdbcTemplate.getDataSource());
    assertThat(reviews.get(user.id(), id)).isEqualTo(result);
  }

  @DisplayName("같은 제출 ID에 다른 답을 보내면 거부한다.")
  @Test
  void repeatedSubmissionRejectsDifferentAnswer() throws Exception {
    User user = user(3);
    UUID id = offer(user).reviewId();
    ReviewResponse state = reviews.start(user.id(), id);
    ReviewQuestion first = current(state);
    ReviewAnswerRequest wrong =
        new ReviewAnswerRequest(UUID.randomUUID(), first.questionId(), List.of("wrong"));
    var failed = reviews.answer(user.id(), id, wrong);
    assertThatThrownBy(
            () ->
                reviews.answer(
                    user.id(),
                    id,
                    new ReviewAnswerRequest(
                        wrong.submissionId(), first.questionId(), List.of("different"))))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("제출 키");
  }

  @DisplayName("복수 정답으로 복습을 완료하고 마지막 답안 재전송과 기한 이후 조회도 완료 결과를 유지한다.")
  @Test
  void acceptedAnswersCompleteReviewAndReplayFinalSubmission() throws Exception {
    User user = user(3);
    UUID id = offer(user).reviewId();
    ReviewResponse state = reviews.start(user.id(), id);
    ReviewQuestion first = current(state);
    ReviewAnswerRequest wrong =
        new ReviewAnswerRequest(UUID.randomUUID(), first.questionId(), List.of("wrong"));
    var failed = reviews.answer(user.id(), id, wrong);
    state = failed.review();
    ReviewAnswerRequest last = null;
    while (state.currentQuestionId() != null) {
      ReviewQuestion question = current(state);
      last =
          new ReviewAnswerRequest(
              UUID.randomUUID(),
              question.questionId(),
              question.quiz().writingSentenceAcceptedAnswers().getLast());
      var result = reviews.answer(user.id(), id, last);
      assertThat(result.correct()).isTrue();
      state = result.review();
    }
    assertThat(state.status()).isEqualTo("COMPLETED");
    assertThat(state.questions()).allMatch(q -> q.completedAt() != null);
    assertThat(state.questions()).extracting(ReviewQuestion::targetExpressionText).hasSize(3);
    assertThat(reviews.answer(user.id(), id, last).review()).isEqualTo(state);
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from expression_review_submission where review_id = ?",
                Long.class,
                id))
        .isEqualTo(4);
    assertThat(
            jdbcTemplate.queryForObject(
                "select max(last_completed_at) from user_writing_expression_completion"
                    + " where user_profile_id = ?",
                LocalDateTime.class,
                user.id()))
        .isEqualTo(local().minusDays(5));
    now = LAUNCH.plusSeconds(10 * 86400);
    assertThat(reviews.start(user.id(), id).status()).isEqualTo("COMPLETED");
  }

  @DisplayName("유효 콘텐츠가 0~2개이면 가능한 수만 출제하고 불량 콘텐츠로 문제를 만들지 않는다.")
  @Test
  void handlesZeroOneTwoAndInvalidContentWithoutInventingQuestions() throws Exception {
    User empty = user(0);
    assertThat(reviews.offer(empty.id(), date())).isEmpty();
    for (int count : List.of(1, 2)) {
      User user = user(count);
      var state = reviews.start(user.id(), offer(user).reviewId());
      assertThat(state.questions()).hasSize(count);
      if (count == 2) {
        assertThat(state.questions())
            .extracting(q -> q.quiz().quizLanguage())
            .containsExactlyInAnyOrder(Locale.EN, Locale.KR);
      }
    }
    User invalid = user(1);
    jdbcTemplate.update(
        "update writing_expression set practice_examples_payload = '[]' format json where id = ?",
        invalid.expressions().getFirst());
    assertThat(reviews.offer(invalid.id(), date())).isEmpty();
    assertThat(countReviews(invalid)).isZero();
  }

  @DisplayName("최근 출제하거나 비활성인 표현과 다른 언어 및 미완료 표현을 복습에서 제외한다.")
  @Test
  void excludesRecentInactiveOtherLanguageAndNeverCompletedExpressions() throws Exception {
    User user = user(3);
    jdbcTemplate.update(
        "update user_writing_expression_completion set last_completed_at = ?"
            + " where user_profile_id = ? and writing_expression_id = ?",
        local(),
        user.id(),
        user.expressions().get(0));
    jdbcTemplate.update(
        "update writing_expression set status = 'INACTIVE' where id = ?",
        user.expressions().get(1));
    jdbcTemplate.update(
        "update writing_expression set base_locale = 'EN' where id = ?", user.expressions().get(2));
    seedExpressionWithPracticeExamples("ACTIVE", practiceExamplesPayloadJson());
    assertThat(reviews.offer(user.id(), date())).isEmpty();
  }

  @DisplayName("다른 학습 경로에서 최근 완료한 표현도 복습에서 제외한다.")
  @Test
  void excludesExpressionCompletedRecentlyThroughAnotherLearningSource() throws Exception {
    User user = user(1);
    jdbcTemplate.update(
        """
        insert into user_writing_expression_completion
        (user_profile_id, writing_expression_id, learning_source, completed_at, last_completed_at)
        values (?, ?, 'FREE_TALK', ?, ?)
        """,
        user.id(),
        user.expressions().getFirst(),
        local(),
        local());
    assertThat(reviews.offer(user.id(), date())).isEmpty();
    subscribe(user, "ACTIVE", local().plusDays(30));
    now = now.plusSeconds(3 * 86400);
    assertThat(reviews.start(user.id(), offer(user).reviewId()).questions()).hasSize(1);
  }

  @DisplayName("실제 구독 도입 시각부터 발송과 시작을 제한하되 이미 시작한 복습은 유예한다.")
  @Test
  void usesActualLaunchTimeForSendingAndStartingButPreservesStartedSession() throws Exception {
    User started = user(1);
    UUID startedId = offer(started).reviewId();
    reviews.start(started.id(), startedId);
    User unopened = user(1);
    final UUID unopenedId = offer(unopened).reviewId();
    now = LAUNCH;
    User unpaid = user(1);
    assertThat(reviews.offer(unpaid.id(), date())).isEmpty();
    assertThat(reviews.offer(unopened.id(), date())).isEmpty();
    mvc.perform(
            post("/api/v1/reviews/{id}/start", unopenedId)
                .header("Authorization", "Bearer " + unopened.token()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("PREMIUM_REQUIRED"));
    assertThat(reviews.start(started.id(), startedId).status()).isEqualTo("IN_PROGRESS");
    for (String status : List.of("ACTIVE", "CANCELED")) {
      User paid = user(1);
      subscribe(paid, status, local().plusDays(1));
      assertThat(reviews.offer(paid.id(), date())).isPresent();
    }
    User expired = user(1);
    subscribe(expired, "ACTIVE", local());
    assertThat(reviews.offer(expired.id(), date())).isEmpty();
    subscribe(expired, "EXPIRED", local().plusDays(1));
    assertThat(reviews.offer(expired.id(), date())).isEmpty();
    now = LAUNCH.minusSeconds(60).plusSeconds(86400);
    mvc.perform(
            post("/api/v1/reviews/{id}/start", startedId)
                .header("Authorization", "Bearer " + started.token()))
        .andExpect(status().isGone())
        .andExpect(jsonPath("$.error.code").value("REVIEW_EXPIRED"));
    assertThat(reviews.get(started.id(), startedId).questions()).isEmpty();
  }

  @DisplayName("복습 제공 간격과 생성 후 7일의 시작 기한을 지킨다.")
  @Test
  void respectsOfferIntervalAndSevenDayStartDeadline() throws Exception {
    User user = user(1);
    final UUID first = offer(user).reviewId();
    subscribe(user, "ACTIVE", local().plusDays(30));
    now = now.plusSeconds(3 * 86400 - 1);
    assertThat(reviews.offer(user.id(), date())).isEmpty();
    now = now.plusSeconds(1);
    assertThat(offer(user).reviewId()).isNotEqualTo(first);
    now = LAUNCH.minusSeconds(60).plusSeconds(7 * 86400);
    assertThatThrownBy(() -> reviews.start(user.id(), first))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("유효기간");
    assertThat(reviews.get(user.id(), first).status()).isEqualTo("EXPIRED");
  }

  @DisplayName("동시 복습 생성과 시작 및 중복 오답 제출을 직렬화한다.")
  @Test
  void serializesConcurrentOffersStartsAndDuplicateWrongAnswer() throws Exception {
    User user = user(3);
    try (var executor = Executors.newFixedThreadPool(2)) {
      Callable<ReviewOffer> create = () -> offer(user);
      var offers = executor.invokeAll(List.of(create, create));
      UUID id = offers.getFirst().get().reviewId();
      assertThat(offers.getLast().get().reviewId()).isEqualTo(id);
      assertThat(countReviews(user)).isEqualTo(1);
      Callable<ReviewResponse> start = () -> reviews.start(user.id(), id);
      var starts = executor.invokeAll(List.of(start, start));
      assertThat(starts.getFirst().get()).isEqualTo(starts.getLast().get());
      UUID questionId = starts.getFirst().get().currentQuestionId();
      var request = new ReviewAnswerRequest(UUID.randomUUID(), questionId, List.of("wrong"));
      Callable<Boolean> submit = () -> reviews.answer(user.id(), id, request).correct();
      for (var result : executor.invokeAll(List.of(submit, submit))) {
        assertThat(result.get()).isFalse();
      }
      assertThat(reviews.get(user.id(), id).questions().getFirst().wrongCount()).isEqualTo(1);
    }
  }

  @DisplayName("정확한 알림 간격부터 종류별 발송을 예약하고 기기 수를 중복 계산하지 않는다.")
  @Test
  void reservesSeparateLearningSlotsAtExactGapAndNeverCountsDevicesTwice() throws Exception {
    User user = user(0);
    var daily = command(user, "daily", NotificationType.DAILY_SCENARIO_REMINDER);
    final var review = command(user, "review", NotificationType.EXPRESSION_REVIEW);
    assertThat(frequency.reserveAll(List.of(daily))).containsExactly(daily);
    assertThat(frequency.reserveAll(List.of(daily))).containsExactly(daily);
    now = now.plusSeconds(3 * 3600 - 1);
    assertThat(frequency.reserveAll(List.of(review))).isEmpty();
    now = now.plusSeconds(1);
    assertThat(frequency.reserveAll(List.of(review))).containsExactly(review);
    assertThat(frequency.reserveAll(List.of(daily))).isEmpty();
    assertThat(frequency.reserveAll(List.of(review))).containsExactly(review);
    now = now.plusSeconds(3 * 3600);
    assertThat(
            frequency.reserveAll(
                List.of(command(user, "third", NotificationType.EXPRESSION_REVIEW))))
        .isEmpty();
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from learning_notification_slot where user_profile_id = ?",
                Long.class,
                user.id()))
        .isEqualTo(2);
  }

  @DisplayName("일일 알림 때문에 발송이 막히면 복습 생성을 롤백하고 재시도는 같은 이벤트를 사용한다.")
  @Test
  void rollsBackOfferWhenDailyPushBlocksItAndReusesEventOnDispatchRetry() throws Exception {
    User user = user(3);
    var tokens = mock(UserPushTokenDeliveryService.class);
    when(tokens.findSendableTokenIdsByUserProfileIds(any()))
        .thenReturn(Map.of(user.id(), List.of(1L)));
    var dispatch = mock(NotificationDispatchService.class);
    var batch =
        new ReviewNotificationService(
            reviews, frequency, tokens, dispatch, clock, transactionManager);
    frequency.reserveAll(
        List.of(command(user, "first-daily", NotificationType.DAILY_SCENARIO_REMINDER)));
    batch.process("batch", now, () -> {});
    assertThat(countReviews(user)).isZero();
    now = now.plusSeconds(3 * 3600);
    subscribe(user, "ACTIVE", local().plusDays(1));
    batch.process("batch", now, () -> {});
    final UUID id = offer(user).reviewId();
    batch.process("retry-message", now, () -> {});
    assertThat(countReviews(user)).isEqualTo(1);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<SendPushNotificationCommand>> captor = ArgumentCaptor.forClass(List.class);
    verify(dispatch, org.mockito.Mockito.atLeast(3)).sendAll(captor.capture());
    var delivered = captor.getAllValues().stream().flatMap(List::stream).toList();
    assertThat(delivered).hasSize(2);
    assertThat(delivered.getFirst()).isEqualTo(delivered.getLast());
    assertThat(delivered.getFirst().deepLink())
        .isEqualTo(
            "/reviews/"
                + id
                + "?utm_source=push&utm_medium=notification&utm_campaign=expression_review"
                + "&utm_content=expression_review_quiz");
    assertThat(delivered.getFirst().notificationType())
        .isEqualTo(NotificationType.EXPRESSION_REVIEW);
  }

  @DisplayName("순서를 벗어나거나 잘못된 답안을 거부하고 허용 정답 계약을 문서화한다.")
  @Test
  void rejectsOutOfOrderAndInvalidAnswersAndDocumentsAcceptedAnswers() throws Exception {
    User user = user(3);
    UUID id = offer(user).reviewId();
    var state = reviews.start(user.id(), id);
    var request =
        new ReviewAnswerRequest(
            UUID.randomUUID(), state.questions().getLast().questionId(), List.of("wrong"));
    mvc.perform(
            post("/api/v1/reviews/{id}/answers", id)
                .header("Authorization", "Bearer " + user.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict());
    mvc.perform(
            post("/api/v1/reviews/{id}/answers", id)
                .header("Authorization", "Bearer " + user.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/v1/reviews/{reviewId}/start'].post").exists())
        .andExpect(
            jsonPath(
                    "$.components.schemas.WritingSentenceResponse.properties"
                        + ".writingSentenceAcceptedAnswers.items.type")
                .value("array"));
  }

  private SendPushNotificationCommand command(User user, String suffix, NotificationType type) {
    return new SendPushNotificationCommand(
        user.id() + ":" + suffix, user.id(), type, "title", "body", "/scenario");
  }

  private ReviewQuestion current(ReviewResponse state) {
    return state.questions().stream()
        .filter(q -> q.questionId().equals(state.currentQuestionId()))
        .findFirst()
        .orElseThrow();
  }

  private ReviewOffer offer(User user) {
    return reviews.offer(user.id(), date()).orElseThrow();
  }

  private long countReviews(User user) {
    return jdbcTemplate.queryForObject(
        "select count(*) from expression_review where user_profile_id = ?", Long.class, user.id());
  }

  private void subscribe(User user, String status, LocalDateTime expiresAt) {
    jdbcTemplate.update(
        "update user_profile set subscription_status = ?, subscription_expires_at = ? where id = ?",
        status,
        expiresAt,
        user.id());
  }

  private LocalDate date() {
    return local().toLocalDate();
  }

  private LocalDateTime local() {
    return LocalDateTime.ofInstant(now, ZONE);
  }

  private User user(int count) throws Exception {
    String key = "review-" + UUID.randomUUID();
    String nonce = UUID.randomUUID().toString();
    var result =
        mvc.perform(
                post("/api/v1/auth/social-login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"provider":"GOOGLE","idToken":"%s|%s@example.com|review|%s","nonce":"%s"}
                        """
                            .formatted(key, key, nonce, nonce)))
            .andExpect(status().isOk())
            .andReturn();
    String token =
        objectMapper
            .readTree(result.getResponse().getContentAsByteArray())
            .path("data")
            .path("accessToken")
            .asText();
    long id =
        jdbcTemplate.queryForObject(
            "select id from user_profile where email = ?", Long.class, key + "@example.com");
    List<Long> expressions = new ArrayList<>();
    for (int index = 0; index < count; index++) {
      long expression = seedExpressionWithPracticeExamples("ACTIVE", practiceExamplesPayloadJson());
      Long scenario =
          jdbcTemplate.queryForObject(
              "select scenario_id from writing_expression where id = ?", Long.class, expression);
      jdbcTemplate.update(
          """
          insert into user_writing_expression_completion
          (user_profile_id, scenario_id, writing_expression_id, learning_source, completed_at, last_completed_at)
          values (?, ?, ?, 'SCENARIO', ?, ?)
          """,
          id,
          scenario,
          expression,
          local().minusDays(5),
          local().minusDays(5));
      expressions.add(expression);
    }
    return new User(id, token, expressions);
  }

  private record User(long id, String token, List<Long> expressions) {}

  private Long seedExpressionWithPracticeExamples(String status, String payloadJson) {
    return new ExpressionPracticeFixture(jdbcTemplate).seed(status, payloadJson);
  }

  private String practiceExamplesPayloadJson() {
    String example =
        """
        {"sentenceText":"I like apples", "highlightingPart":"like", "sentenceTranslation":"나는 사과를 좋아해",
         "practiceQuestion":"What do you like?", "practiceQuestionTranslation":"무엇을 좋아해?",
         "sentenceWords":["I","like","apples"], "sentenceWordChoices":["apples","like","I","not"],
         "sentenceTranslateWords":["나는","사과를","좋아해"],
         "sentenceTranslateWordChoices":["사과를","나는","좋아해","싫어해"],
         "sentenceTranslateAcceptedAnswers":[["나는","사과를","좋아해"],["사과를","나는","좋아해"]]}
        """;
    return "[" + String.join(",", java.util.Collections.nCopies(4, example)) + "]";
  }
}
