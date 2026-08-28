// 시나리오 고정 질문 조회 Repository의 locale, 순서, 활성 상태 조건을 검증한다.

package com.landit.landitbe.feature.content;

import static org.assertj.core.api.Assertions.assertThat;

import com.landit.landitbe.feature.content.repository.ScenarioQuestionQueryRepository;
import com.landit.landitbe.feature.content.repository.projection.ScenarioQuestionProjection;
import com.landit.landitbe.shared.domain.Locale;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** 시나리오 고정 질문 조회 Repository의 locale, 순서, 활성 상태 조건을 검증한다. */
@ActiveProfiles("test")
@SpringBootTest
@Transactional
class ScenarioQuestionQueryRepositoryIntegrationTests {

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private ScenarioQuestionQueryRepository scenarioQuestionQueryRepository;

  @Test
  void findsActiveQuestionByScenarioDisplayOrderAndLocale() {
    seedScenario(991101L);
    seedQuestion(991201L, 991101L, 1, "ACTIVE");
    seedQuestion(991202L, 991101L, 2, "ACTIVE");
    seedQuestionLanguageVariant(
        991301L, 991201L, "EN", "KR", "What food do you like?", "좋아하는 음식이 뭐야?", "ACTIVE");
    seedQuestionLanguageVariant(
        991302L, 991202L, "EN", "KR", "Do you cook often?", "요리를 자주 해?", "ACTIVE");

    Optional<ScenarioQuestionProjection> question =
        scenarioQuestionQueryRepository.findActiveQuestion(991101L, 2, Locale.EN, Locale.KR);

    assertThat(question).isPresent();
    assertThat(question.get().questionId()).isEqualTo(991202L);
    assertThat(question.get().sequence()).isEqualTo(2);
    assertThat(question.get().questionText()).isEqualTo("Do you cook often?");
    assertThat(question.get().questionTranslation()).isEqualTo("요리를 자주 해?");
    assertThat(question.get().questionAudioUrl())
        .isEqualTo("https://cdn.example.com/questions/991302.mp3");
  }

  @Test
  void doesNotReturnInactiveQuestionOrInactiveVariant() {
    seedScenario(991102L);
    seedQuestion(991203L, 991102L, 1, "INACTIVE");
    seedQuestion(991204L, 991102L, 2, "ACTIVE");
    seedQuestionLanguageVariant(
        991303L, 991203L, "EN", "KR", "Inactive question", "비활성 질문", "ACTIVE");
    seedQuestionLanguageVariant(
        991304L, 991204L, "EN", "KR", "Inactive variant", "비활성 번역", "INACTIVE");

    assertThat(scenarioQuestionQueryRepository.findActiveQuestion(991102L, 1, Locale.EN, Locale.KR))
        .isEmpty();
    assertThat(scenarioQuestionQueryRepository.findActiveQuestion(991102L, 2, Locale.EN, Locale.KR))
        .isEmpty();
  }

  private void seedScenario(long scenarioId) {
    jdbcTemplate.update(
        """
                        insert into category (
                            id,
                            display_order,
                            status,
                            created_at,
                            updated_at
                        )
                        values (?, ?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        scenarioId,
        scenarioId);
    jdbcTemplate.update(
        """
                        insert into scenario (
                            id,
                            category_id,
                            ai_role,
                            difficulty,
                            first_speaker,
                            total_question_count,
                            display_order,
                            status,
                            created_at,
                            updated_at
                        )
                        values (?, ?, 'friend', 'EASY', 'AI', 2, ?, 'ACTIVE',
                            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        scenarioId,
        scenarioId,
        scenarioId);
  }

  private void seedQuestion(long questionId, long scenarioId, int displayOrder, String status) {
    jdbcTemplate.update(
        """
                        insert into scenario_question (
                            id,
                            scenario_id,
                            display_order,
                            status,
                            created_at,
                            updated_at
                        )
                        values (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        questionId,
        scenarioId,
        displayOrder,
        status);
  }

  private void seedQuestionLanguageVariant(
      long variantId,
      long questionId,
      String targetLocale,
      String baseLocale,
      String questionText,
      String questionTranslation,
      String status) {
    jdbcTemplate.update(
        """
                        insert into scenario_question_language_variant (
                            id,
                            scenario_question_id,
                            target_locale,
                            base_locale,
                            question_text,
                            question_translation,
                            audio_url,
                            status,
                            created_at,
                            updated_at
                        )
                        values (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        variantId,
        questionId,
        targetLocale,
        baseLocale,
        questionText,
        questionTranslation,
        "https://cdn.example.com/questions/%d.mp3".formatted(variantId),
        status);
  }
}
