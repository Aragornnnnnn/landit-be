// 프리톡 신규 표현의 AI-5 학습 콘텐츠 JSON 저장을 검증한다.

package com.landit.landitbe.feature.session.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.landitbe.feature.session.repository.FreeTalkExpressionRepository;
import com.landit.landitbe.shared.domain.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** 프리톡 신규 표현의 AI-5 학습 콘텐츠 JSON 저장을 검증한다. */
@ActiveProfiles("test")
@SpringBootTest
@Transactional
class FreeTalkExpressionPersistenceIntegrationTest {

  @Autowired private FreeTalkExpressionRepository repository;
  @Autowired private JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper = new ObjectMapper();

  /** AI-5가 생성한 신규 표현의 전체 학습 콘텐츠를 저장하고 다시 읽는다. */
  @Test
  void persistsAndLoadsAllGeneratedLearningContent() throws Exception {
    FreeTalkExpression saved =
        repository.saveAndFlush(
            FreeTalkExpression.newExpression(
                Locale.EN,
                Locale.KR,
                "hit it off",
                "죽이 잘 맞다",
                "quick summary",
                "detailed usage",
                "How was your first date?",
                "첫 데이트는 어땠어?",
                "We really hit it off.",
                "우리는 정말 죽이 잘 맞았어.",
                objectMapper.readTree("[\"We\", \"really\", \"hit\", \"it\", \"off\"]"),
                objectMapper.readTree("[\"hit\", \"miss\", \"it\", \"off\"]"),
                null,
                objectMapper.readTree("[{\"sentenceText\":\"They hit it off.\"}]")));

    FreeTalkExpression loaded = repository.findById(saved.getId()).orElseThrow();

    assertThat(loaded.getSourceType()).isEqualTo(FreeTalkExpressionSourceType.NEW);
    assertThat(loaded.getRepresentativeQuestionText()).isEqualTo("How was your first date?");
    assertThat(loaded.getRepresentativeSentenceTranslation()).isEqualTo("우리는 정말 죽이 잘 맞았어.");
    assertThat(loaded.getRepresentativeSentenceWords())
        .isEqualTo(objectMapper.readTree("[\"We\", \"really\", \"hit\", \"it\", \"off\"]"));
    assertThat(loaded.getPracticeExamplesPayload())
        .isEqualTo(objectMapper.readTree("[{\"sentenceText\":\"They hit it off.\"}]"));
  }

  /** 기존 Writing 표현을 참조하는 프리톡 표현을 저장하고 다시 읽는다. */
  @Test
  void persistsAndLoadsExistingWritingExpressionReference() {
    long writingExpressionId = insertWritingExpressionRow();

    FreeTalkExpression saved =
        repository.saveAndFlush(
            FreeTalkExpression.existingExpression(
                writingExpressionId,
                Locale.EN,
                Locale.KR,
                "make up for",
                "만회하다",
                "부족했던 부분을 보완한다."));

    FreeTalkExpression loaded = repository.findById(saved.getId()).orElseThrow();

    assertThat(loaded.getSourceType()).isEqualTo(FreeTalkExpressionSourceType.EXISTING);
    assertThat(loaded.getWritingExpressionId()).isEqualTo(writingExpressionId);
    assertThat(loaded.getUsageSummary()).isEqualTo("부족했던 부분을 보완한다.");
  }

  private long insertWritingExpressionRow() {
    jdbcTemplate.update(
        """
        insert into category (id, display_order, status, created_at, updated_at)
        values (993001, 993001, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """);
    jdbcTemplate.update(
        """
        insert into scenario (id, category_id, ai_role, difficulty, first_speaker,
            total_question_count, display_order, status, created_at, updated_at)
        values (993002, 993001, 'friend', 'NORMAL', 'AI', 1, 993002, 'ACTIVE',
            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """);
    jdbcTemplate.update(
        """
        insert into writing_expression (
            id, scenario_id, expression_type, usage_frequency_level, target_locale, base_locale,
            display_order, target_expression_text, base_expression_meaning_text, usage_summary,
            usage_description, representative_sentence_text, representative_sentence_translation,
            representative_sentence_words, representative_sentence_word_choices,
            practice_examples_payload, status, created_at, updated_at
        ) values (
            993003, 993002, 'DAILY_ROUTINE', 'BASIC', 'EN', 'KR', 1,
            'make up for', '만회하다', '부족했던 부분을 보완한다.', 'description', 'sentence', '문장',
            ARRAY['sentence'], ARRAY['sentence', 'choice'], CAST('[]' AS jsonb), 'ACTIVE',
            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        )
        """);
    return 993003L;
  }
}
