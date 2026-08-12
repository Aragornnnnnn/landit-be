// Flyway가 생성한 실제 DB 스키마가 DBML 핵심 구조를 따르는지 검증한다.

package com.landit.landitbe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
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

  /** 관리자 역할과 쓰기 감사 로그에 필요한 스키마를 생성한다. */
  @Test
  void userRoleAndAuditLogSchemaSupportsAdminAuthorizationAndAudit() {
    assertColumnExists("user_profile", "role");
    assertTableConstraintExists("user_profile", "chk_user_profile_role");
    assertTableExists("admin_audit_log");
    assertColumnExists("admin_audit_log", "admin_user_profile_id");
    assertColumnExists("admin_audit_log", "action");
    assertColumnExists("admin_audit_log", "target_type");
    assertColumnExists("admin_audit_log", "target_id");
    assertColumnExists("admin_audit_log", "before_value");
    assertColumnExists("admin_audit_log", "after_value");
    assertTableConstraintExists("admin_audit_log", "fk_admin_audit_log_admin_user_profile_id");
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

  @DisplayName("앱 버전 정책은 플랫폼별 한 건과 유효한 빌드 번호를 유지한다.")
  @Test
  void appVersionPolicySchemaUsesSinglePlatformRecord() {
    assertColumnExists("app_version", "minimum_supported_version_name");
    assertColumnDoesNotExist("app_version", "minimum_supported_build_number");
    assertTableConstraintExists("app_version", "uk_app_version_platform");

    assertThatThrownBy(() -> insertAppVersionForConstraintTest("IOS", "1.0.0", "1.0.0", 0))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> insertAppVersionForConstraintTest("ANDROID", "1.0", "1.0.0", 1))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> insertAppVersionForConstraintTest("ANDROID", "1.0.0", "minimum", 1))
        .isInstanceOf(DataIntegrityViolationException.class);
    insertAppVersionForConstraintTest("IOS", "1.0.0", "1.0.0", 1);
    assertThatThrownBy(() -> insertAppVersionForConstraintTest("IOS", "1.1.0", "1.0.0", 2))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @DisplayName("앱 버전 정책 migration은 활성 정책 partial unique index를 제거한다.")
  @Test
  void appVersionPolicyMigrationUsesPlatformUniqueConstraint() throws Exception {
    String migrationSql =
        readMigrationSql("db/migration/V34__change_app_version_to_single_platform_policy.sql");

    assertThat(migrationSql)
        .contains(
            "DROP INDEX IF EXISTS uk_app_version_active_platform",
            "minimum_supported_version_name",
            "CONSTRAINT uk_app_version_platform UNIQUE (platform)");
  }

  @DisplayName("DB별 migration은 앱 버전명의 Major.Minor.Patch 형식을 강제한다.")
  @Test
  void appVersionNameFormatMigrationsUseDatabaseSpecificRegularExpressions() throws Exception {
    String h2MigrationSql = readMigrationSql("db/h2/V35__enforce_app_version_name_format.sql");
    String postgresqlMigrationSql =
        readMigrationSql("db/postgresql/V35__enforce_app_version_name_format.sql");

    assertThat(h2MigrationSql).contains("REGEXP '^[0-9]+\\.[0-9]+\\.[0-9]+$'");
    assertThat(postgresqlMigrationSql).contains("~ '^[0-9]+\\.[0-9]+\\.[0-9]+$'");
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

  @DisplayName("사용자 시나리오 복습 권한의 고유 제약을 추가한다.")
  @Test
  void userScenarioAccessConstraintsAreAppliedWithoutGlobalSchedule() {
    assertTableExists("user_scenario_access");
    assertColumnExists("user_scenario_access", "user_profile_id");
    assertColumnExists("user_scenario_access", "scenario_id");
    assertColumnExists("user_scenario_access", "target_locale");
    assertColumnExists("user_scenario_access", "granted_at");
    assertTableConstraintExists(
        "user_scenario_access", "uk_user_scenario_access_user_scenario_locale");
    assertTableConstraintExists("user_scenario_access", "fk_user_scenario_access_user_profile_id");
    assertTableConstraintExists("user_scenario_access", "fk_user_scenario_access_scenario_id");

    assertTableDoesNotExist("daily_scenario_schedule");
    assertColumnDoesNotExist("scenario_session", "daily_scenario_schedule_id");
  }

  @DisplayName("V27 migration은 프리톡 저장 구조와 초기 주제를 추가한다.")
  @Test
  void v27AddsFreeTalkStorageStructureAndTopicSeed() {
    assertTableExists("free_talk_topic");
    assertTableDoesNotExist("free_talk_turn_result");
    assertColumnDoesNotExist("ai_tutor", "free_talk_tts_voice_id");
    assertColumnExists("free_talk_session", "topic_id");
    assertColumnExists("free_talk_session", "processing_client_message_id");
    assertColumnExists("session_history_message", "client_message_id");
    assertColumnExists("session_history_message", "utterance_duration_ms");
    assertColumnExists("session_history_message", "emotion");
    assertTableConstraintExists("free_talk_session", "fk_free_talk_session_topic_id");
    assertTableConstraintExists("session_history_message", "uk_session_history_message_client_id");
    assertThat(
            jdbcTemplate.queryForList(
                "select display_name from free_talk_topic order by display_order", String.class))
        .containsExactly("오늘 하루 얘기", "주말 계획", "요즘 빠진 것", "스포츠", "고민 상담");

    Integer defaultTutorLabelCount =
        jdbcTemplate.queryForObject(
            """
            select count(*)
            from ai_tutor tutor
            join ai_tutor_language_variant variant on variant.ai_tutor_id = tutor.id
            where tutor.accent_locale = 'EN_US'
              and tutor.target_locale = 'EN'
              and variant.base_locale = 'KR'
              and variant.display_name = '미국 영어 튜터'
            """,
            Integer.class);
    assertThat(defaultTutorLabelCount).isEqualTo(1);
  }

  @DisplayName("프리톡 표현 연결 구조는 공용 표현만 참조한다.")
  @Test
  void v29AddsFreeTalkExpressionLearningStorage() {
    assertTableExists("free_talk_session_expression");
    assertTableDoesNotExist("free_talk_expression");
    assertTableDoesNotExist("user_free_talk_expression_completion");
    assertColumnExists("free_talk_session", "expression_generation_status");
    assertColumnExists("free_talk_session", "expression_generation_started_at");
    assertColumnExists("free_talk_session_expression", "writing_expression_id");
    assertColumnDoesNotExist("writing_expression", "owner_user_profile_id");
    assertNullableColumn("writing_expression", "scenario_id");
    assertNullableColumn("user_writing_expression_completion", "scenario_id");
    assertColumnDoesNotExist("free_talk_session_expression", "generated_content_payload");
    assertTableConstraintExists(
        "free_talk_session", "chk_free_talk_session_expression_generation_status");
    assertTableConstraintExists("writing_expression", "chk_writing_expression_scenario_source");
  }

  @DisplayName("V37 migration은 프리톡 세션별 표현 완료 시각을 추가한다.")
  @Test
  void v37AddsFreeTalkExpressionCompletionTime() {
    assertColumnExists("free_talk_session_expression", "completed_at");
    assertNullableColumn("free_talk_session_expression", "completed_at");
  }

  @DisplayName("Writing 표현은 시나리오와 프리톡 사용 영역을 소스별로 구분한다.")
  @Test
  void v36AddsWritingExpressionSource() {
    assertColumnExists("writing_expression", "expression_source");
    assertColumnExists("writing_expression", "embedding");
    assertTableConstraintExists("writing_expression", "chk_writing_expression_scenario_source");
    assertTableConstraintDoesNotExist("writing_expression", "chk_writing_expression_source");
    assertTableConstraintDoesNotExist(
        "writing_expression", "fk_writing_expression_owner_user_profile_id");

    Integer mismatchedSourceCount =
        jdbcTemplate.queryForObject(
            """
            select count(*)
            from writing_expression
            where (scenario_id is null and expression_source <> 'FREE_TALK')
               or (scenario_id is not null and expression_source <> 'SCENARIO')
            """,
            Integer.class);
    assertThat(mismatchedSourceCount).isZero();
  }

  @DisplayName("V36 migration은 시나리오에 속하지 않은 공용 프리톡 표현을 허용한다.")
  @Test
  @Transactional
  void v36AllowsPublicFreeTalkExpression() {
    jdbcTemplate.update(
        """
        insert into writing_expression (
            expression_source, expression_type, usage_frequency_level, target_locale, base_locale,
            display_order, target_expression_text, base_expression_meaning_text, usage_summary,
            usage_description, representative_sentence_text, representative_sentence_translation,
            representative_sentence_words, representative_sentence_word_choices,
            practice_examples_payload, status, created_at, updated_at
        )
        values ('FREE_TALK', 'CONVERSATION_SKILL', 'BASIC', 'EN', 'KR', 990204,
            'free-talk sample', '프리톡 샘플', 'summary', 'description', 'sentence', '문장',
            ARRAY['sentence'], ARRAY['sentence','choice'], CAST('[]' AS jsonb), 'ACTIVE',
            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """);

    Integer expressionCount =
        jdbcTemplate.queryForObject(
            """
            select count(*)
            from writing_expression
            where expression_source = 'FREE_TALK'
              and scenario_id is null
            """,
            Integer.class);
    assertThat(expressionCount).isEqualTo(1);
  }

  @DisplayName("V51 PostgreSQL migration은 pgvector 1536차원 컬럼을 사용한다.")
  @Test
  void v51UsesPgvectorAndH2CompatibleEmbeddingMigrations() throws Exception {
    String postgresqlMigrationSql =
        readMigrationSql("db/postgresql/V51__prepare_writing_expression_embeddings.sql");
    String h2MigrationSql =
        readMigrationSql("db/h2/V51__prepare_writing_expression_embeddings.sql");

    assertThat(postgresqlMigrationSql)
        .contains(
            "CREATE EXTENSION IF NOT EXISTS vector WITH SCHEMA extensions",
            "embedding extensions.vector(1536)",
            "chk_writing_expression_scenario_source");
    assertThat(h2MigrationSql).contains("embedding VARCHAR(32767)");
  }

  @DisplayName("V32 migration은 사용자 Push Token을 Expo Push Token 전용 컬럼으로 전환한다.")
  @Test
  void v32ConvertsUserPushTokenToExpoPushToken() {
    assertColumnExists("user_push_token", "expo_push_token");
    assertColumnDoesNotExist("user_push_token", "token");
  }

  @DisplayName("V32 migration은 기존 활성 Push Token을 폐기한다.")
  @Test
  void v32RevokesExistingActivePushTokens() {
    String databaseUrl = migrationTestDatabaseUrl();
    JdbcTemplate migrationJdbcTemplate =
        new JdbcTemplate(new DriverManagerDataSource(databaseUrl, "sa", ""));
    migrateToVersion(databaseUrl, "31");
    insertAiTutor(migrationJdbcTemplate, 990301L, "ACTIVE");
    insertUserProfile(migrationJdbcTemplate, 990302L, 990301L);
    migrationJdbcTemplate.update(
        """
        INSERT INTO user_push_token (
            id, user_profile_id, platform, token, status, created_at, updated_at
        )
        VALUES (990303, 990302, 'IOS', 'existing-push-token', 'ACTIVE',
                CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """);

    migrateToLatestVersion(databaseUrl);

    String status =
        migrationJdbcTemplate.queryForObject(
            "SELECT status FROM user_push_token WHERE id = 990303", String.class);
    assertThat(status).isEqualTo("REVOKED");
  }

  @DisplayName("V27은 pending 메시지 FK와 클라이언트 메시지 멱등 unique를 실제로 강제한다.")
  @Test
  @Transactional
  void v27EnforcesPendingMessageForeignKeyAndClientMessageUniqueness() {
    Long aiTutorId = jdbcTemplate.queryForObject("select min(id) from ai_tutor", Long.class);
    jdbcTemplate.update(
        """
        insert into user_profile (id, nickname, target_locale, base_locale, current_level,
            ai_tutor_id, push_permission_status, status, created_at, updated_at)
        values (992001, 'free-talk-schema-user', 'EN', 'KR', 1, ?, 'NOT_DETERMINED',
            'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        aiTutorId);
    jdbcTemplate.update(
        """
        insert into learning_session (id, user_profile_id, session_type, ai_tutor_id,
            target_locale, base_locale, input_mode, status, started_at, created_at, updated_at)
        values (992002, 992001, 'FREE_TALK', ?, 'EN', 'KR', 'MIXED', 'IN_PROGRESS',
            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        aiTutorId);
    jdbcTemplate.update(
        """
        insert into free_talk_session (id, learning_session_id, start_mode, conversation_status,
            accumulated_speaking_duration_ms, created_at, updated_at)
        values (992003, 992002, 'USER_FIRST', 'IN_PROGRESS', 0,
            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """);
    jdbcTemplate.update(
        """
        insert into session_history (id, learning_session_id, user_profile_id, session_type,
            target_locale, base_locale, started_at, ended_at, duration_seconds, user_message_count,
            created_at)
        values (992004, 992002, 992001, 'FREE_TALK', 'EN', 'KR', CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP, 0, 0, CURRENT_TIMESTAMP)
        """);
    jdbcTemplate.update(
        """
        insert into session_history_message (id, session_history_id, message_sequence, turn_number,
            role, content, input_type, client_message_id, created_at, updated_at)
        values (992005, 992004, 1, 1, 'USER', 'hello', 'TEXT', 'client-1',
            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """);

    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    """
                    update free_talk_session
                    set pending_user_message_id = 992999
                    where id = 992003
                    """))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    """
                    insert into session_history_message (session_history_id, message_sequence, turn_number,
                        role, content, input_type, client_message_id, created_at, updated_at)
                    values (992004, 2, 2, 'USER', 'again', 'TEXT', 'client-1',
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """))
        .isInstanceOf(DataIntegrityViolationException.class);
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
            where provider = 'OPENROUTER'
              and model = 'microsoft/mai-voice-2'
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

  @DisplayName("V38 migration이 Deepgram Aura 2 TTS 음성을 추가한다.")
  @Test
  void v38MigrationSeedsDeepgramAura2Voice() {
    Map<String, Object> voice =
        jdbcTemplate.queryForMap(
            """
            select provider, model, provider_voice_id, gender, description, accent_locale, status
            from tts_voice
            where provider = 'OPENROUTER'
              and model = 'deepgram/aura-2'
              and provider_voice_id = 'aura-2-orpheus-en'
            """);

    assertThat(voice)
        .containsEntry("PROVIDER", "OPENROUTER")
        .containsEntry("MODEL", "deepgram/aura-2")
        .containsEntry("PROVIDER_VOICE_ID", "aura-2-orpheus-en")
        .containsEntry("GENDER", "MALE")
        .containsEntry("DESCRIPTION", "굵은 남성 음성")
        .containsEntry("ACCENT_LOCALE", "EN_US")
        .containsEntry("STATUS", "ACTIVE");
  }

  @DisplayName("V43 migration이 시나리오 노출 순서를 전체 기준 unique로 강제한다.")
  @Test
  void v43MigrationEnforcesGlobalScenarioDisplayOrder() {
    assertTableConstraintExists("scenario", "uk_scenario_display_order");

    Integer legacyConstraintCount =
        jdbcTemplate.queryForObject(
            """
            select count(*)
            from information_schema.table_constraints
            where lower(table_name) = 'scenario'
              and lower(constraint_name) = 'uk_scenario_category_order'
            """,
            Integer.class);

    assertThat(legacyConstraintCount)
        .as("legacy constraint scenario.uk_scenario_category_order")
        .isZero();
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

  /** V34 migration이 활성 정책의 최소 지원 버전명을 보존하는지 검증한다. */
  @Test
  void v34MigrationKeepsSingleActivePolicyAndMapsMinimumSupportedVersionName() {
    String databaseUrl = migrationTestDatabaseUrl();
    JdbcTemplate migrationJdbcTemplate =
        new JdbcTemplate(new DriverManagerDataSource(databaseUrl, "sa", ""));
    migrateToVersion(databaseUrl, "33");
    insertLegacyAppVersionPolicy(migrationJdbcTemplate, "IOS", "1.0.0", 10, 8, true);
    insertLegacyAppVersionPolicy(migrationJdbcTemplate, "IOS", "0.9.0", 8, 8, false);

    migrateToVersion(databaseUrl, "35");

    assertThat(
            migrationJdbcTemplate.queryForObject(
                "select count(*) from app_version where platform = 'IOS'", Integer.class))
        .isEqualTo(1);
    assertThat(
            migrationJdbcTemplate.queryForObject(
                "select minimum_supported_version_name from app_version where platform = 'IOS'",
                String.class))
        .isEqualTo("0.9.0");
    assertThat(
            migrationJdbcTemplate.queryForObject(
                "select version_name from app_version where platform = 'IOS'", String.class))
        .isEqualTo("1.0.0");
  }

  /** V34 migration은 기존 최소 지원 빌드에 대응하는 버전명이 없으면 적용을 중단한다. */
  @Test
  void v34MigrationFailsWhenMinimumSupportedBuildCannotBeMapped() {
    String databaseUrl = migrationTestDatabaseUrl();
    JdbcTemplate migrationJdbcTemplate =
        new JdbcTemplate(new DriverManagerDataSource(databaseUrl, "sa", ""));
    migrateToVersion(databaseUrl, "33");
    insertLegacyAppVersionPolicy(migrationJdbcTemplate, "IOS", "1.0.0", 10, 8, true);

    assertThatThrownBy(() -> migrateToLatestVersion(databaseUrl))
        .isInstanceOf(FlywayException.class);
  }

  /** V48 migration은 두 플랫폼에 1.1.0 강제 업데이트 정책을 적용한다. */
  @Test
  void v48MigrationRequiresVersion110ForBothPlatforms() {
    String databaseUrl = migrationTestDatabaseUrl();
    JdbcTemplate migrationJdbcTemplate =
        new JdbcTemplate(new DriverManagerDataSource(databaseUrl, "sa", ""));
    migrateToVersion(databaseUrl, "43");
    insertCurrentAppVersionPolicy(migrationJdbcTemplate, "IOS", 101);
    insertCurrentAppVersionPolicy(migrationJdbcTemplate, "ANDROID", 202);

    migrateToLatestVersion(databaseUrl);

    Integer forceUpdatePolicyCount =
        migrationJdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM app_version
            WHERE version_name = '1.1.0'
              AND minimum_supported_version_name = '1.1.0'
              AND force_update_reason = '매일 학습을 챙겨주는 알림 기능이 생겼어요!'
              AND soft_update_reason IS NULL
              AND release_note = '서비스 신규 기능 및 정책 변경'
              AND active = TRUE
              AND released_at > TIMESTAMP '2026-07-16 08:18:09.147652'
              AND created_at = TIMESTAMP '2026-07-16 08:18:09.147652'
            """,
            Integer.class);

    assertThat(forceUpdatePolicyCount).isEqualTo(2);
    assertThat(
            migrationJdbcTemplate.queryForObject(
                "SELECT build_number FROM app_version WHERE platform = 'IOS'", Long.class))
        .isEqualTo(101L);
    assertThat(
            migrationJdbcTemplate.queryForObject(
                "SELECT build_number FROM app_version WHERE platform = 'ANDROID'", Long.class))
        .isEqualTo(202L);
  }

  private String migrationTestDatabaseUrl() {
    return "jdbc:h2:mem:lan100-v14-"
        + UUID.randomUUID()
        + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1";
  }

  private void migrateToVersion(String databaseUrl, String targetVersion) {
    Flyway.configure()
        .dataSource(databaseUrl, "sa", "")
        .locations("classpath:db/migration", "classpath:db/h2")
        .target(targetVersion)
        .load()
        .migrate();
  }

  private void migrateToLatestVersion(String databaseUrl) {
    Flyway.configure()
        .dataSource(databaseUrl, "sa", "")
        .locations("classpath:db/migration", "classpath:db/h2")
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

  /** V32 이전 스키마에 활성 여부가 다른 앱 버전 정책을 추가한다. */
  private void insertLegacyAppVersionPolicy(
      JdbcTemplate migrationJdbcTemplate,
      String platform,
      String versionName,
      long buildNumber,
      long minimumSupportedBuildNumber,
      boolean active) {
    migrationJdbcTemplate.update(
        """
        insert into app_version (
            platform, version_name, build_number, minimum_supported_build_number, active,
            released_at, created_at
        )
        values (?, ?, ?, ?, ?, current_timestamp, current_timestamp)
        """,
        platform,
        versionName,
        buildNumber,
        minimumSupportedBuildNumber,
        active);
  }

  private void insertCurrentAppVersionPolicy(
      JdbcTemplate migrationJdbcTemplate, String platform, long buildNumber) {
    migrationJdbcTemplate.update(
        """
        INSERT INTO app_version (
            platform, version_name, build_number, minimum_supported_version_name,
            force_update_reason, soft_update_reason, release_note, active,
            released_at, created_at
        )
        VALUES (
            ?, '1.0.0', ?, '1.0.0',
            NULL, NULL, 'Landit 최초 출시 버전입니다.', TRUE,
            TIMESTAMP '2026-07-16 08:18:09.147652',
            TIMESTAMP '2026-07-16 08:18:09.147652'
        )
        """,
        platform,
        buildNumber);
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
      String platform, String versionName, String minimumSupportedVersionName, long buildNumber) {
    jdbcTemplate.update(
        """
        INSERT INTO app_version (
            platform, version_name, minimum_supported_version_name, build_number,
            force_update_reason, soft_update_reason, release_note, active,
            released_at, created_at
        )
        VALUES (
            ?, ?, ?, ?,
            '강제 업데이트', '업데이트 권장', NULL, FALSE,
            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        )
        """,
        platform,
        versionName,
        minimumSupportedVersionName,
        buildNumber);
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

  private void assertNullableColumn(String tableName, String columnName) {
    String nullable =
        jdbcTemplate.queryForObject(
            """
            select is_nullable
            from information_schema.columns
            where lower(table_name) = ?
              and lower(column_name) = ?
            """,
            String.class,
            tableName,
            columnName);

    assertThat(nullable).as("column %s.%s is nullable", tableName, columnName).isEqualTo("YES");
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

  private void assertTableConstraintDoesNotExist(String tableName, String constraintName) {
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

    assertThat(constraintCount).as("constraint %s.%s", tableName, constraintName).isZero();
  }

  private String readMigrationSql(String path) throws Exception {
    return StreamUtils.copyToString(
        new ClassPathResource(path).getInputStream(), java.nio.charset.StandardCharsets.UTF_8);
  }
}
