// 복습의 실제 저장·HTTP 권한·시간 경계·동시성·푸시 예약을 검증한다.

package com.landit.landitbe.feature.learning.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import com.landit.landitbe.feature.notification.domain.NotificationType;
import com.landit.landitbe.feature.notification.scheduled.service.LearningNotificationFrequencyService;
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
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
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

  @Test
  void createsFixedThreeQuestionsAndRequiresAuthenticatedOwner() throws Exception {
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

  @Test
  void gradesMultipleAnswersMovesWrongToEndAndDeduplicatesSubmissions() throws Exception {
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
    assertThatThrownBy(
            () ->
                reviews.answer(
                    user.id(),
                    id,
                    new ReviewAnswerRequest(
                        wrong.submissionId(), first.questionId(), List.of("different"))))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("제출 키");
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
