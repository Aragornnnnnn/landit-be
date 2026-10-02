-- V127 PostgreSQL 회귀 검증에 필요한 표현 테이블과 보존 대상 행을 준비한다.
CREATE TABLE writing_expression (
    id BIGSERIAL PRIMARY KEY,
    scenario_id BIGINT,
    expression_type TEXT,
    usage_frequency_level TEXT,
    target_locale TEXT,
    base_locale TEXT,
    display_order INTEGER,
    target_expression_text TEXT,
    base_expression_meaning_text TEXT,
    usage_summary TEXT,
    usage_description TEXT,
    representative_question_text TEXT,
    representative_question_translation TEXT,
    representative_sentence_text TEXT,
    representative_sentence_translation TEXT,
    representative_image_url TEXT,
    practice_examples_payload JSONB,
    status TEXT,
    created_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ,
    representative_sentence_words VARCHAR[],
    representative_sentence_word_choices VARCHAR[],
    expression_source TEXT,
    difficulty_level INTEGER,
    embedding extensions.vector(1536)
);
INSERT INTO writing_expression (id, target_expression_text, difficulty_level)
VALUES (1, 'preserve existing content', 5);
SELECT setval(pg_get_serial_sequence('writing_expression', 'id'), 7000, true);
