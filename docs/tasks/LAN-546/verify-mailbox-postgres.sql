-- 임시 PostgreSQL에서 실제 편지함 migration과 기존 데이터 보존·저장 제약을 검증한다.
\set ON_ERROR_STOP on
BEGIN;
CREATE SCHEMA lan546_verification;
SET search_path TO lan546_verification;

-- 편지함 외부 의존성은 사용자 PK만 제공한다. 애플리케이션 전체 schema 검증은 아니다.
CREATE TABLE user_profile (id BIGINT PRIMARY KEY);
INSERT INTO user_profile VALUES (1), (2), (3);
\ir ../../../src/main/resources/db/migration/V53__add_mailbox.sql
\ir ../../../src/main/resources/db/migration/V54__add_mailbox_admin_indexes.sql

INSERT INTO mailbox_letter (
    id, letter_type, title, content_blocks, body_text, preview_text,
    publication_status, published_at, created_at, updated_at)
VALUES
    (1, 'NOTICE', '기존 공지', '[{"type":"TEXT","text":"공지"}]', NULL,
     '공지', 'PUBLISHED', now(), now(), now()),
    (2, 'UPDATE', '기존 업데이트', '[{"type":"TEXT","text":"업데이트"}]', NULL,
     '업데이트', 'PUBLISHED', now(), now(), now()),
    (3, 'REPLY', '기존 답장', NULL, '답장 본문', '답장 본문',
     'PUBLISHED', now(), now(), now());
INSERT INTO mailbox_feedback (
    id, user_profile_id, feedback_type, content_text, processing_status, created_at, updated_at)
VALUES (1, 1, 'QUESTION', '기존 문의', 'COMPLETED', now(), now());
INSERT INTO mailbox_letter_recipient (
    letter_id, user_profile_id, representative_feedback_id, read_at, created_at)
VALUES (3, 1, 1, now(), now());
INSERT INTO mailbox_letter_read (letter_id, user_profile_id, read_at, created_at)
VALUES (1, 1, now(), now());

CREATE TEMP TABLE before_migration AS
SELECT 'letter' AS kind, to_jsonb(t) AS data FROM mailbox_letter t
UNION ALL SELECT 'feedback', to_jsonb(t) FROM mailbox_feedback t
UNION ALL SELECT 'recipient', to_jsonb(t) FROM mailbox_letter_recipient t
UNION ALL SELECT 'read', to_jsonb(t) FROM mailbox_letter_read t;

COMMIT;
BEGIN;
\ir ../../../src/main/resources/db/postgresql/V119__add_direct_mailbox_letters.sql
DO $$
BEGIN
    IF (SELECT count(*) FROM pg_constraint
        WHERE conrelid = 'mailbox_letter'::regclass
          AND conname IN ('chk_mailbox_letter_type', 'chk_mailbox_letter_payload')
          AND NOT convalidated) <> 2 THEN
        RAISE EXCEPTION 'V119가 기존 행 검증을 지연하지 않았다.';
    END IF;
END $$;
COMMIT;
BEGIN;

DO $$
DECLARE actual JSONB;
DECLARE expected JSONB;
BEGIN
    SELECT jsonb_agg(to_jsonb(t) ORDER BY kind, data) INTO expected FROM before_migration t;
    SELECT jsonb_agg(to_jsonb(t) ORDER BY kind, data) INTO actual FROM (
        SELECT 'letter' AS kind, to_jsonb(t) AS data FROM mailbox_letter t
        UNION ALL SELECT 'feedback', to_jsonb(t) FROM mailbox_feedback t
        UNION ALL SELECT 'recipient', to_jsonb(t) FROM mailbox_letter_recipient t
        UNION ALL SELECT 'read', to_jsonb(t) FROM mailbox_letter_read t
    ) t;
    IF actual IS DISTINCT FROM expected THEN
        RAISE EXCEPTION '기존 편지·문의·수신자·읽음 데이터가 변경됐다.';
    END IF;
    RAISE NOTICE 'PASS: 기존 데이터와 읽음 시각 보존';
END $$;

INSERT INTO mailbox_letter (
    id, letter_type, title, body_text, preview_text,
    publication_status, published_at, created_at, updated_at)
VALUES (4, 'DIRECT', '직접 편지', '직접 본문', '직접 본문', 'PUBLISHED', now(), now(), now());
INSERT INTO mailbox_letter_recipient (letter_id, user_profile_id, created_at)
VALUES (4, 1, now()), (4, 2, now());

DO $$
BEGIN
    IF (SELECT count(*) FROM mailbox_letter_recipient
        WHERE letter_id = 4 AND representative_feedback_id IS NULL AND read_at IS NULL) <> 2 THEN
        RAISE EXCEPTION '문의 없는 직접 편지 수신 정보 저장 실패';
    END IF;
    RAISE NOTICE 'PASS: 직접 편지와 피드백 없는 수신자 2명 저장';
END $$;

-- 각 실패는 하위 트랜잭션으로 롤백된다. 여러 제약을 위반하면 보고 순서를 강제하지 않는다.
DO $$
DECLARE item RECORD;
DECLARE violated_constraint TEXT;
BEGIN
    FOR item IN SELECT * FROM (VALUES
        ('UPDATE mailbox_letter SET letter_type = ''UNKNOWN'' WHERE id = 4',
         'chk_mailbox_letter_type|chk_mailbox_letter_payload'),
        ('UPDATE mailbox_letter SET body_text = NULL WHERE id = 4',
         'chk_mailbox_letter_payload'),
        ('UPDATE mailbox_letter SET content_blocks = ''[]''::jsonb WHERE id = 4',
         'chk_mailbox_letter_payload'),
        ('UPDATE mailbox_letter SET body_text = ''금지'' WHERE id = 1',
         'chk_mailbox_letter_payload'),
        ('UPDATE mailbox_letter SET content_blocks = NULL WHERE id = 2',
         'chk_mailbox_letter_payload'),
        ('UPDATE mailbox_letter SET body_text = NULL WHERE id = 3',
         'chk_mailbox_letter_payload'),
        ('INSERT INTO mailbox_letter_recipient (letter_id,user_profile_id,created_at) VALUES (4,1,now())',
         'uk_mailbox_letter_recipient_letter_user'),
        ('INSERT INTO mailbox_letter_recipient (letter_id,user_profile_id,created_at) VALUES (4,999,now())',
         'fk_mailbox_letter_recipient_user_profile_id'),
        ('INSERT INTO mailbox_letter_recipient (letter_id,user_profile_id,created_at) VALUES (999,3,now())',
         'fk_mailbox_letter_recipient_letter_id'),
        ('INSERT INTO mailbox_letter_recipient (letter_id,user_profile_id,representative_feedback_id,created_at) VALUES (3,3,999,now())',
         'fk_mailbox_letter_recipient_feedback_id')
    ) AS cases(statement, expected_constraint)
    LOOP
        BEGIN
            EXECUTE item.statement;
            RAISE EXCEPTION '거부해야 할 입력이 성공했다: %', item.expected_constraint;
        EXCEPTION WHEN check_violation OR unique_violation OR foreign_key_violation THEN
            GET STACKED DIAGNOSTICS violated_constraint = CONSTRAINT_NAME;
            IF violated_constraint <> ALL(string_to_array(item.expected_constraint, '|')) THEN
                RAISE EXCEPTION '예상 제약: %, 실제 제약: %', item.expected_constraint, violated_constraint;
            END IF;
        END;
    END LOOP;
    RAISE NOTICE 'PASS: 편지 유형·본문·중복 수신자·외래 키 제약 위반 10건 거부';
END $$;

DO $$
DECLARE first_read TIMESTAMP(6);
DECLARE changed_rows INTEGER;
BEGIN
    UPDATE mailbox_letter_recipient SET read_at = clock_timestamp()
    WHERE letter_id = 4 AND user_profile_id = 1 AND read_at IS NULL;
    SELECT read_at INTO first_read FROM mailbox_letter_recipient
    WHERE letter_id = 4 AND user_profile_id = 1;
    UPDATE mailbox_letter_recipient SET read_at = clock_timestamp()
    WHERE letter_id = 4 AND user_profile_id = 1 AND read_at IS NULL;
    GET DIAGNOSTICS changed_rows = ROW_COUNT;
    IF first_read IS NULL OR changed_rows <> 0 THEN
        RAISE EXCEPTION '최초 읽음 시각 유지 실패';
    END IF;
    IF (SELECT count(*) FROM mailbox_letter_recipient
        WHERE letter_id = 4 AND user_profile_id = 2 AND read_at IS NULL) <> 1 THEN
        RAISE EXCEPTION '다른 수신자의 읽음 상태가 변경됐다.';
    END IF;
    RAISE NOTICE 'PASS: 반복 읽음의 무변경 및 수신자별 상태 분리';
END $$;

INSERT INTO mailbox_feedback_attachment
    (feedback_id, object_key, content_type, file_size, display_order, created_at)
VALUES (1, 'mailbox/feedback/first', 'image/png', 100, 0, now()),
       (1, 'mailbox/feedback/second', 'image/jpeg', 5242880, 1, now());

DO $$
DECLARE statement TEXT;
BEGIN
    FOR statement IN SELECT * FROM (VALUES
        ('UPDATE mailbox_feedback_attachment SET content_type = ''image/svg+xml'''),
        ('UPDATE mailbox_feedback_attachment SET file_size = 0'),
        ('UPDATE mailbox_feedback_attachment SET file_size = 5242881'),
        ('UPDATE mailbox_feedback_attachment SET display_order = 3'),
        ('UPDATE mailbox_feedback_attachment SET display_order = -1'),
        ('UPDATE mailbox_feedback_attachment SET display_order = 0'),
        ('UPDATE mailbox_feedback_attachment SET object_key = ''duplicate'''),
        ('UPDATE mailbox_feedback_attachment SET feedback_id = 999')
    ) AS cases(statement)
    LOOP
        BEGIN
            EXECUTE statement;
            RAISE EXCEPTION '첨부의 잘못된 입력을 거부하지 않았다: %', statement;
        EXCEPTION WHEN check_violation OR unique_violation OR foreign_key_violation THEN
            NULL;
        END;
    END LOOP;
    IF (SELECT count(*) FROM mailbox_feedback_attachment) <> 2 THEN
        RAISE EXCEPTION '기존 첨부가 보존되지 않았다.';
    END IF;
    DELETE FROM mailbox_letter_recipient WHERE representative_feedback_id = 1;
    DELETE FROM mailbox_feedback WHERE id = 1;
    IF EXISTS (SELECT 1 FROM mailbox_feedback_attachment) THEN
        RAISE EXCEPTION '문의 삭제 시 첨부 메타데이터가 남았다.';
    END IF;
    RAISE NOTICE 'PASS: 첨부 저장, 형식·크기·순서·중복·외래 키 위반 8건 거부, 연쇄 삭제';
END $$;

SELECT version() AS verified_postgresql_version;
COMMIT;
BEGIN;
\ir ../../../src/main/resources/db/postgresql/V122__validate_direct_mailbox_letters.sql
DO $$
BEGIN
    IF (SELECT count(*) FROM pg_constraint
        WHERE conrelid = 'mailbox_letter'::regclass
          AND conname IN ('chk_mailbox_letter_type', 'chk_mailbox_letter_payload')
          AND convalidated) <> 2 THEN
        RAISE EXCEPTION 'V122가 기존 행 검증을 완료하지 않았다.';
    END IF;
    IF EXISTS (SELECT 1 FROM pg_locks WHERE pid = pg_backend_pid()
        AND relation = 'mailbox_letter'::regclass AND mode = 'AccessExclusiveLock') THEN
        RAISE EXCEPTION '검증 트랜잭션에 제약 추가의 강한 잠금이 남았다.';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_locks WHERE pid = pg_backend_pid()
        AND relation = 'mailbox_letter'::regclass AND mode = 'ShareUpdateExclusiveLock') THEN
        RAISE EXCEPTION '검증 잠금 모드를 확인할 수 없다.';
    END IF;
    RAISE NOTICE 'PASS: V119 미검증 제약의 쓰기 보호 및 별도 V122 검증 잠금 확인';
END $$;
COMMIT;
DROP SCHEMA lan546_verification CASCADE;
\echo 'LAN-546 PostgreSQL verification passed; verification schema removed.'
