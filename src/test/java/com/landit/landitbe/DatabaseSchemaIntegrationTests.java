// Flyway가 생성한 실제 DB 스키마가 DBML 핵심 구조를 따르는지 검증한다.

package com.landit.landitbe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StreamUtils;

@ActiveProfiles("test")
@SpringBootTest
class DatabaseSchemaIntegrationTests {

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void dbmlCoreTablesExist() {
    List<String> tableNames =
        List.of(
            "user_profile",
            "oauth_identity",
            "refresh_token",
            "ai_tutor",
            "scenario",
            "learning_session",
            "session_history",
            "session_history_message_feedback",
            "user_learning_expression",
            "writing_expression");

    tableNames.forEach(this::assertTableExists);
  }

  @Test
  void oauthIdentityHasLookupIndexes() {
    assertIndexExists("idx_oauth_identity_provider_user");
    assertIndexExists("idx_oauth_identity_user_provider");
  }

  @DisplayName("PostgreSQL 전용 migration에 ACTIVE partial unique index가 정의되어 있다.")
  @Test
  void postgresqlMigrationDefinesActivePartialUniqueIndexes() throws Exception {
    String migrationSql = readMigrationSql("db/postgresql/V5__add_dbml_partial_unique_indexes.sql");

    assertThat(migrationSql)
        .contains(
            "CREATE UNIQUE INDEX uk_oauth_identity_active_provider_user",
            "WHERE status = 'ACTIVE'",
            "CREATE UNIQUE INDEX uk_oauth_identity_active_user_provider",
            "CREATE UNIQUE INDEX uk_user_quest_active_user");
  }

  @DisplayName("앱 버전 정책은 유효한 빌드 번호 범위만 저장한다.")
  @Test
  void appVersionBuildConstraintRejectsInvalidRanges() {
    assertThatThrownBy(() -> insertAppVersionForConstraintTest(0, 0))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> insertAppVersionForConstraintTest(18, 19))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @DisplayName("PostgreSQL 전용 migration에 플랫폼별 활성 정책 partial unique index가 정의되어 있다.")
  @Test
  void postgresqlMigrationDefinesSingleActiveAppVersionIndex() throws Exception {
    String migrationSql =
        readMigrationSql("db/postgresql/V17__enforce_single_active_app_version.sql");

    assertThat(migrationSql)
        .contains(
            "CREATE UNIQUE INDEX uk_app_version_active_platform",
            "ON app_version (platform)",
            "WHERE active = TRUE");
  }

  @DisplayName("NPS 테이블 교체는 이미 적용된 V4가 아니라 V6 migration에서 처리한다.")
  @Test
  void npsTableReplacementIsSeparatedFromAppliedV4Migration() throws Exception {
    String v4MigrationSql = readMigrationSql("db/migration/V4__apply_dbml_schema.sql");
    String v6MigrationSql = readMigrationSql("db/migration/V6__replace_session_nps_response.sql");

    assertThat(v4MigrationSql)
        .contains("CREATE TABLE session_nps_response")
        .doesNotContain("CREATE TABLE nps_response");
    assertThat(v6MigrationSql)
        .contains("CREATE TABLE nps_response", "DROP TABLE session_nps_response");
  }

  @Test
  void sessionHistoryMessageFeedbackDoesNotKeepLearningExpressionBackReference() {
    Integer columnCount =
        jdbcTemplate.queryForObject(
            """
            select count(*)
            from information_schema.columns
            where lower(table_name) = 'session_history_message_feedback'
              and lower(column_name) = 'user_learning_expression_id'
            """,
            Integer.class);

    assertThat(columnCount).isZero();
  }

  @Test
  void npsResponseIsUserBoundAndAllowsDuplicateSubmissions() {
    assertTableExists("nps_response");
    assertTableDoesNotExist("session_nps_response");
    assertColumnExists("nps_response", "user_profile_id");
    assertColumnDoesNotExist("nps_response", "learning_session_id");

    jdbcTemplate.update(
        """
        insert into user_profile (
            id,
            nickname,
            target_locale,
            base_locale,
            current_level,
            push_permission_status,
            status,
            created_at,
            updated_at
        )
        values (990001, 'nps-test-user', 'EN', 'KR', 1, 'NOT_DETERMINED',
            'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """);

    jdbcTemplate.update(
        """
        insert into nps_response (user_profile_id, score, opinion_text, created_at)
        values (990001, 3, 'first', CURRENT_TIMESTAMP)
        """);
    jdbcTemplate.update(
        """
        insert into nps_response (user_profile_id, score, opinion_text, created_at)
        values (990001, 4, 'second', CURRENT_TIMESTAMP)
        """);

    Integer responseCount =
        jdbcTemplate.queryForObject(
            """
            select count(*)
            from nps_response
            where user_profile_id = 990001
            """,
            Integer.class);

    assertThat(responseCount).isEqualTo(2);
  }

  @DisplayName("ERD v2 컬럼 차이를 최신 migration으로 반영한다.")
  @Test
  void erdV2ColumnChangesAreAppliedByLatestMigration() {
    assertColumnExists("scenario", "total_question_count");
    assertColumnDoesNotExist("scenario", "min_turns_to_goal");
    assertColumnDoesNotExist("scenario", "max_turns_to_goal");
    assertColumnDoesNotExist("scenario", "completion_criteria");

    assertColumnExists("writing_expression", "representative_sentence_words");
    assertColumnExists("writing_expression", "representative_sentence_word_choices");
    assertColumnExists("user_writing_expression_completion", "scenario_id");
  }

  @DisplayName("시나리오 고정 질문 콘텐츠 테이블을 최신 migration으로 추가한다.")
  @Test
  void scenarioQuestionTablesAreAppliedByLatestMigration() {
    assertTableExists("scenario_question");
    assertColumnExists("scenario_question", "scenario_id");
    assertColumnExists("scenario_question", "display_order");
    assertColumnExists("scenario_question", "status");
    assertTableConstraintExists("scenario_question", "uk_scenario_question_scenario_order");

    assertTableExists("scenario_question_language_variant");
    assertColumnExists("scenario_question_language_variant", "scenario_question_id");
    assertColumnExists("scenario_question_language_variant", "target_locale");
    assertColumnExists("scenario_question_language_variant", "base_locale");
    assertColumnExists("scenario_question_language_variant", "question_text");
    assertColumnExists("scenario_question_language_variant", "question_translation");
    assertColumnExists("scenario_question_language_variant", "status");
    assertTableConstraintExists("scenario_question_language_variant", "uk_scenario_question_lang");
  }

  @DisplayName("V18 migration은 첫 질문 속마음을 질문 Variant로 옮긴다.")
  @Test
  void v18MovesOpeningInnerThoughtToQuestionLanguageVariant() {
    assertColumnExists("scenario_question_language_variant", "inner_thought");
    assertColumnExists("scenario_question_language_variant", "inner_thought_type");
    assertTableConstraintExists(
        "scenario_question_language_variant", "chk_scenario_question_lang_inner_thought_pair");

    assertColumnDoesNotExist("scenario_language_variant", "ai_opening_message");
    assertColumnDoesNotExist("scenario_language_variant", "ai_opening_message_translation");
    assertColumnDoesNotExist("scenario_language_variant", "ai_opening_inner_thought");
    assertColumnDoesNotExist("scenario_language_variant", "ai_opening_inner_thought_type");
  }

  @DisplayName("V20 migration은 사용자 메시지 속마음 처리 상태를 추가한다.")
  @Test
  void v20AddsInnerThoughtProcessingStatusToSessionHistoryMessage() {
    assertColumnExists("session_history_message", "inner_thought_processing_status");
    assertTableConstraintExists(
        "session_history_message", "chk_session_message_inner_thought_status");
  }

  @DisplayName("V24 migration은 사용자 메시지 피드백 처리 상태를 추가한다.")
  @Test
  void v24AddsFeedbackProcessingStatusToSessionHistoryMessage() {
    assertColumnExists("session_history_message", "feedback_processing_status");
    assertTableConstraintExists("session_history_message", "chk_session_message_feedback_status");
  }

  @DisplayName("PostgreSQL 전용 V22 migration이 추가 예문 payload 키를 카멜 케이스로 정규화한다.")
  @Test
  void postgresqlMigrationNormalizesPracticeExamplesPayloadKeys() throws Exception {
    String migrationSql =
        readMigrationSql("db/postgresql/V22__normalize_practice_examples_payload_keys.sql");

    // 스네이크 키 → 파서(REQUIRED_PRACTICE_SENTENCE_KEYS)가 읽는 카멜 키로 변환하는지 확인한다.
    assertThat(migrationSql)
        .contains(
            "jsonb_build_object",
            "'sentenceText'",
            "'highlightingPart'",
            "'sentenceTranslation'",
            "'practiceQuestion'",
            "'practiceQuestionTranslation'",
            "'imageUrl'",
            "WITH ORDINALITY");
    // 이미 카멜로 고쳐진 DB에서는 0행으로 지나가도록 멱등 가드가 있어야 한다.
    assertThat(migrationSql).contains("e ? 'sentence_text'");
  }

  @DisplayName("표현 타입·빈도 컬럼은 enum 상수명이 아닌 값의 INSERT를 거부한다.")
  @Test
  void writingExpressionEnumCheckConstraintsRejectNonEnumValues() {
    assertTableConstraintExists("writing_expression", "chk_writing_expression_type");
    assertTableConstraintExists(
        "writing_expression", "chk_writing_expression_usage_frequency_level");

    // 시딩 파이프라인이 또 한글 라벨로 넣으면 500까지 가지 않고 INSERT 시점에 튕겨야 한다.
    jdbcTemplate.update(
        """
        insert into category (id, display_order, status, created_at, updated_at)
        values (990201, 990201, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """);
    jdbcTemplate.update(
        """
        insert into scenario (
            id, category_id, ai_role, difficulty, first_speaker, total_question_count,
            display_order, status, created_at, updated_at
        )
        values (990202, 990201, 'barista', 'NORMAL', 'AI', 5,
            990202, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """);

    assertThatThrownBy(() -> insertWritingExpressionForConstraintTest("일상·루틴", "BASIC"))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> insertWritingExpressionForConstraintTest("DAILY_ROUTINE", "기본"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  private void insertWritingExpressionForConstraintTest(
      String expressionType, String usageFrequencyLevel) {
    jdbcTemplate.update(
        """
        insert into writing_expression (
            scenario_id, expression_type, usage_frequency_level, target_locale, base_locale,
            display_order, target_expression_text, base_expression_meaning_text, usage_summary,
            usage_description, representative_sentence_text, representative_sentence_translation,
            representative_sentence_words, representative_sentence_word_choices,
            practice_examples_payload, status, created_at, updated_at
        )
        values (990202, ?, ?, 'EN', 'KR', 990203, 'sample', '샘플', 'summary',
            'description', 'sentence', '문장', ARRAY['sample'], ARRAY['sample','choice'],
            CAST('[]' AS jsonb), 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        expressionType,
        usageFrequencyLevel);
  }

  @DisplayName("AI 튜터 음성과 시나리오 TTS 음성을 V14 migration으로 분리한다.")
  @Test
  void aiTutorAndScenarioTtsVoiceSchemaIsSeparatedByV14Migration() {
    assertTableExists("tts_voice");
    assertColumnExists("tts_voice", "provider");
    assertColumnExists("tts_voice", "model");
    assertColumnExists("tts_voice", "provider_voice_id");
    assertColumnExists("tts_voice", "gender");
    assertColumnExists("tts_voice", "description");
    assertColumnExists("tts_voice", "accent_locale");
    assertColumnExists("tts_voice", "status");
    assertTableConstraintExists("tts_voice", "uk_tts_voice_provider_model_voice");

    assertColumnDoesNotExist("ai_tutor", "voice_provider");
    assertColumnDoesNotExist("ai_tutor", "voice_id");
    assertColumnDoesNotExist("scenario", "tts_voice_set_id");
    assertColumnExists("scenario_language_variant", "tts_voice_id");
    assertTableConstraintExists("scenario_language_variant", "fk_scenario_lang_tts_voice_id");
  }

  @DisplayName("V14 migration이 기본 튜터와 시나리오 TTS 음성 두 건을 추가한다.")
  @Test
  void v14MigrationSeedsDefaultTutorAndScenarioTtsVoices() throws Exception {
    List<Map<String, Object>> voices =
        jdbcTemplate.queryForList(
            """
            select provider, model, provider_voice_id, gender, description, accent_locale, status
            from tts_voice
            order by provider_voice_id
            """);

    assertThat(voices).hasSize(2);
    assertThat(voices)
        .extracting(row -> row.get("PROVIDER_VOICE_ID"))
        .containsExactly("en-US-Ethan:MAI-Voice-2", "en-US-Harper:MAI-Voice-2");
    assertThat(voices).extracting(row -> row.get("GENDER")).containsExactly("MALE", "FEMALE");
    assertThat(voices)
        .allSatisfy(
            row -> {
              assertThat(row.get("PROVIDER")).isEqualTo("OPENROUTER");
              assertThat(row.get("MODEL")).isEqualTo("microsoft/mai-voice-2");
              assertThat(row.get("ACCENT_LOCALE")).isEqualTo("EN_US");
              assertThat(row.get("STATUS")).isEqualTo("ACTIVE");
            });

    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    """
                    insert into tts_voice (
                        provider, model, provider_voice_id, gender, accent_locale, status,
                        created_at, updated_at
                    )
                    values (
                        'OPENROUTER', 'microsoft/mai-voice-2', 'en-US-Harper:MAI-Voice-2',
                        'FEMALE', 'EN_US', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                    )
                    """))
        .isInstanceOf(DataIntegrityViolationException.class);

    Integer defaultTutorCount =
        jdbcTemplate.queryForObject(
            """
            select count(*)
            from ai_tutor
            where accent_locale = 'EN_US'
              and target_locale = 'EN'
              and status = 'ACTIVE'
            """,
            Integer.class);
    assertThat(defaultTutorCount).isEqualTo(1);

    Integer koreanVariantCount =
        jdbcTemplate.queryForObject(
            """
            select count(*)
            from ai_tutor_language_variant variant
            join ai_tutor tutor on tutor.id = variant.ai_tutor_id
            where tutor.accent_locale = 'EN_US'
              and tutor.target_locale = 'EN'
              and tutor.status = 'ACTIVE'
              and variant.base_locale = 'KR'
              and variant.display_name = '미국 영어 튜터'
            """,
            Integer.class);
    assertThat(koreanVariantCount).isEqualTo(1);

    String migrationSql =
        readMigrationSql("db/migration/V14__separate_ai_tutor_and_scenario_tts_voice.sql");
    assertThat(migrationSql).contains("UPDATE user_profile", "WHERE ai_tutor_id IS NULL");
  }

  @DisplayName("V14 migration은 AI 튜터가 없는 기존 사용자만 기본 튜터로 backfill한다.")
  @Test
  void v14MigrationBackfillsOnlyUsersWithoutAiTutor() {
    String databaseUrl = migrationTestDatabaseUrl();
    JdbcTemplate migrationJdbcTemplate =
        new JdbcTemplate(new DriverManagerDataSource(databaseUrl, "sa", ""));
    migrateToVersion(databaseUrl, "13");
    insertAiTutor(migrationJdbcTemplate, 990101L, "ACTIVE");
    insertAiTutor(migrationJdbcTemplate, 990102L, "INACTIVE");
    insertUserProfile(migrationJdbcTemplate, 990201L, null);
    insertUserProfile(migrationJdbcTemplate, 990202L, 990102L);

    migrateToLatestVersion(databaseUrl);

    assertThat(userAiTutorId(migrationJdbcTemplate, 990201L)).isEqualTo(990101L);
    assertThat(userAiTutorId(migrationJdbcTemplate, 990202L)).isEqualTo(990102L);
  }

  private String migrationTestDatabaseUrl() {
    return "jdbc:h2:mem:lan100-v14-"
        + UUID.randomUUID()
        + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1";
  }

  private void migrateToVersion(String databaseUrl, String targetVersion) {
    Flyway.configure()
        .dataSource(databaseUrl, "sa", "")
        .locations("classpath:db/migration")
        .target(targetVersion)
        .load()
        .migrate();
  }

  private void migrateToLatestVersion(String databaseUrl) {
    Flyway.configure()
        .dataSource(databaseUrl, "sa", "")
        .locations("classpath:db/migration")
        .load()
        .migrate();
  }

  private void insertAiTutor(JdbcTemplate migrationJdbcTemplate, long tutorId, String status) {
    migrationJdbcTemplate.update(
        """
        INSERT INTO ai_tutor (
            id, accent_locale, target_locale, status, created_at, updated_at
        )
        VALUES (?, 'EN_US', 'EN', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        tutorId,
        status);
  }

  private void insertUserProfile(
      JdbcTemplate migrationJdbcTemplate, long userProfileId, Long aiTutorId) {
    migrationJdbcTemplate.update(
        """
        INSERT INTO user_profile (
            id, nickname, target_locale, base_locale, current_level, ai_tutor_id,
            push_permission_status, status, created_at, updated_at
        )
        VALUES (?, 'migration-test-user', 'EN', 'KR', 1, ?, 'NOT_DETERMINED', 'ACTIVE',
                CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        userProfileId,
        aiTutorId);
  }

  private Long userAiTutorId(JdbcTemplate migrationJdbcTemplate, long userProfileId) {
    return migrationJdbcTemplate.queryForObject(
        "SELECT ai_tutor_id FROM user_profile WHERE id = ?", Long.class, userProfileId);
  }

  private void insertAppVersionForConstraintTest(
      long buildNumber, long minimumSupportedBuildNumber) {
    jdbcTemplate.update(
        """
        INSERT INTO app_version (
            platform, version_name, build_number, minimum_supported_build_number,
            force_update_reason, soft_update_reason, release_note, active,
            released_at, created_at
        )
        VALUES (
            'IOS', 'constraint-test', ?, ?,
            '강제 업데이트', '업데이트 권장', NULL, FALSE,
            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        )
        """,
        buildNumber,
        minimumSupportedBuildNumber);
  }

  private void assertTableExists(String tableName) {
    Integer tableCount =
        jdbcTemplate.queryForObject(
            """
            select count(*)
            from information_schema.tables
            where lower(table_name) = ?
            """,
            Integer.class,
            tableName);

    assertThat(tableCount).as("table %s", tableName).isEqualTo(1);
  }

  private void assertTableDoesNotExist(String tableName) {
    Integer tableCount =
        jdbcTemplate.queryForObject(
            """
            select count(*)
            from information_schema.tables
            where lower(table_name) = ?
            """,
            Integer.class,
            tableName);

    assertThat(tableCount).as("table %s", tableName).isZero();
  }

  private void assertColumnExists(String tableName, String columnName) {
    Integer columnCount =
        jdbcTemplate.queryForObject(
            """
            select count(*)
            from information_schema.columns
            where lower(table_name) = ?
              and lower(column_name) = ?
            """,
            Integer.class,
            tableName,
            columnName);

    assertThat(columnCount).as("column %s.%s", tableName, columnName).isEqualTo(1);
  }

  private void assertColumnDoesNotExist(String tableName, String columnName) {
    Integer columnCount =
        jdbcTemplate.queryForObject(
            """
            select count(*)
            from information_schema.columns
            where lower(table_name) = ?
              and lower(column_name) = ?
            """,
            Integer.class,
            tableName,
            columnName);

    assertThat(columnCount).as("column %s.%s", tableName, columnName).isZero();
  }

  private void assertIndexExists(String indexName) {
    Integer indexCount =
        jdbcTemplate.queryForObject(
            """
            select count(*)
            from information_schema.indexes
            where lower(index_name) = ?
            """,
            Integer.class,
            indexName);

    assertThat(indexCount).as("index %s", indexName).isEqualTo(1);
  }

  private void assertTableConstraintExists(String tableName, String constraintName) {
    Integer constraintCount =
        jdbcTemplate.queryForObject(
            """
            select count(*)
            from information_schema.table_constraints
            where lower(table_name) = ?
              and lower(constraint_name) = ?
            """,
            Integer.class,
            tableName,
            constraintName);

    assertThat(constraintCount).as("constraint %s.%s", tableName, constraintName).isEqualTo(1);
  }

  private String readMigrationSql(String path) throws Exception {
    return StreamUtils.copyToString(
        new ClassPathResource(path).getInputStream(), java.nio.charset.StandardCharsets.UTF_8);
  }
}
