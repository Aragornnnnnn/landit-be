// Flyway가 생성한 실제 DB 스키마가 DBML 핵심 구조를 따르는지 검증한다.
package com.landit.landitbe;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StreamUtils;

@ActiveProfiles("test")
@SpringBootTest
class DatabaseSchemaIntegrationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void dbmlCoreTablesExist() {
        List<String> tableNames = List.of(
                "user_profile",
                "oauth_identity",
                "refresh_token",
                "ai_tutor",
                "scenario",
                "learning_session",
                "session_history",
                "session_history_message_feedback",
                "user_learning_expression",
                "writing_expression"
        );

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

        assertThat(migrationSql).contains(
                "CREATE UNIQUE INDEX uk_oauth_identity_active_provider_user",
                "WHERE status = 'ACTIVE'",
                "CREATE UNIQUE INDEX uk_oauth_identity_active_user_provider",
                "CREATE UNIQUE INDEX uk_user_quest_active_user"
        );
    }

    @DisplayName("NPS 테이블 교체는 이미 적용된 V4가 아니라 V6 migration에서 처리한다.")
    @Test
    void npsTableReplacementIsSeparatedFromAppliedV4Migration() throws Exception {
        String v4MigrationSql = readMigrationSql("db/migration/V4__apply_dbml_schema.sql");
        String v6MigrationSql = readMigrationSql("db/migration/V6__replace_session_nps_response.sql");

        assertThat(v4MigrationSql)
                .contains("CREATE TABLE session_nps_response")
                .doesNotContain("CREATE TABLE nps_response");
        assertThat(v6MigrationSql).contains(
                "CREATE TABLE nps_response",
                "DROP TABLE session_nps_response"
        );
    }

    @Test
    void sessionHistoryMessageFeedbackDoesNotKeepLearningExpressionBackReference() {
        Integer columnCount = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from information_schema.columns
                        where lower(table_name) = 'session_history_message_feedback'
                          and lower(column_name) = 'user_learning_expression_id'
                        """,
                Integer.class
        );

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
                        """
        );

        jdbcTemplate.update(
                """
                        insert into nps_response (user_profile_id, score, opinion_text, created_at)
                        values (990001, 3, 'first', CURRENT_TIMESTAMP)
                        """
        );
        jdbcTemplate.update(
                """
                        insert into nps_response (user_profile_id, score, opinion_text, created_at)
                        values (990001, 4, 'second', CURRENT_TIMESTAMP)
                        """
        );

        Integer responseCount = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from nps_response
                        where user_profile_id = 990001
                        """,
                Integer.class
        );

        assertThat(responseCount).isEqualTo(2);
    }

    @DisplayName("ERD v2 컬럼 차이를 최신 migration으로 반영한다.")
    @Test
    void erdV2ColumnChangesAreAppliedByLatestMigration() {
        assertColumnExists("scenario", "total_question_count");
        assertColumnDoesNotExist("scenario", "min_turns_to_goal");
        assertColumnDoesNotExist("scenario", "max_turns_to_goal");
        assertColumnDoesNotExist("scenario", "completion_criteria");

        assertColumnExists("writing_expression", "representative_sentence_translation_highlight_text");
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
        assertTableConstraintExists(
                "scenario_question_language_variant",
                "uk_scenario_question_lang"
        );
    }

    private void assertTableExists(String tableName) {
        Integer tableCount = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from information_schema.tables
                        where lower(table_name) = ?
                        """,
                Integer.class,
                tableName
        );

        assertThat(tableCount).as("table %s", tableName).isEqualTo(1);
    }

    private void assertTableDoesNotExist(String tableName) {
        Integer tableCount = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from information_schema.tables
                        where lower(table_name) = ?
                        """,
                Integer.class,
                tableName
        );

        assertThat(tableCount).as("table %s", tableName).isZero();
    }

    private void assertColumnExists(String tableName, String columnName) {
        Integer columnCount = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from information_schema.columns
                        where lower(table_name) = ?
                          and lower(column_name) = ?
                        """,
                Integer.class,
                tableName,
                columnName
        );

        assertThat(columnCount).as("column %s.%s", tableName, columnName).isEqualTo(1);
    }

    private void assertColumnDoesNotExist(String tableName, String columnName) {
        Integer columnCount = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from information_schema.columns
                        where lower(table_name) = ?
                          and lower(column_name) = ?
                        """,
                Integer.class,
                tableName,
                columnName
        );

        assertThat(columnCount).as("column %s.%s", tableName, columnName).isZero();
    }

    private void assertIndexExists(String indexName) {
        Integer indexCount = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from information_schema.indexes
                        where lower(index_name) = ?
                        """,
                Integer.class,
                indexName
        );

        assertThat(indexCount).as("index %s", indexName).isEqualTo(1);
    }

    private void assertTableConstraintExists(String tableName, String constraintName) {
        Integer constraintCount = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from information_schema.table_constraints
                        where lower(table_name) = ?
                          and lower(constraint_name) = ?
                        """,
                Integer.class,
                tableName,
                constraintName
        );

        assertThat(constraintCount)
                .as("constraint %s.%s", tableName, constraintName)
                .isEqualTo(1);
    }

    private String readMigrationSql(String path) throws Exception {
        return StreamUtils.copyToString(
                new ClassPathResource(path).getInputStream(),
                java.nio.charset.StandardCharsets.UTF_8
        );
    }
}
