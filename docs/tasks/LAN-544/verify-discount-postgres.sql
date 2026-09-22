-- 임시 PostgreSQL에서 할인 제약의 단계별 적용과 검증 잠금을 확인한다.
\set ON_ERROR_STOP on
CREATE SCHEMA lan544_verification;
SET search_path TO lan544_verification;
CREATE TABLE user_profile (id BIGINT PRIMARY KEY);
INSERT INTO user_profile VALUES (1), (2);

BEGIN;
\ir ../../../src/main/resources/db/postgresql/V116__add_paywall_discount_offer.sql
COMMIT;

DO $$
DECLARE statement TEXT;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
        WHERE conrelid = 'user_profile'::regclass
          AND conname = 'chk_user_profile_discount_offer' AND NOT convalidated) THEN
        RAISE EXCEPTION 'V116이 기존 행 검증을 지연하지 않았다.';
    END IF;
    IF (SELECT count(*) FROM user_profile
        WHERE discount_offer_expires_at IS NULL AND discount_offer_new_user IS NULL) <> 2 THEN
        RAISE EXCEPTION '기존 사용자 데이터가 보존되지 않았다.';
    END IF;
    FOR statement IN SELECT * FROM (VALUES
        ('UPDATE user_profile SET discount_offer_expires_at = now() WHERE id = 1'),
        ('UPDATE user_profile SET discount_offer_new_user = false WHERE id = 1')
    ) AS cases(statement)
    LOOP
        BEGIN
            EXECUTE statement;
            RAISE EXCEPTION '미검증 제약이 불일치 쓰기를 허용했다.';
        EXCEPTION WHEN check_violation THEN NULL;
        END;
    END LOOP;
    RAISE NOTICE 'PASS: 기존 사용자 보존 및 검증 전 불일치 쓰기 2건 거부';
END $$;
UPDATE user_profile SET discount_offer_expires_at = now(), discount_offer_new_user = false
WHERE id = 1;

BEGIN;
\ir ../../../src/main/resources/db/postgresql/V121__validate_paywall_discount_offer.sql
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
        WHERE conrelid = 'user_profile'::regclass
          AND conname = 'chk_user_profile_discount_offer' AND convalidated) THEN
        RAISE EXCEPTION 'V121이 기존 행 검증을 완료하지 않았다.';
    END IF;
    IF EXISTS (SELECT 1 FROM pg_locks WHERE pid = pg_backend_pid()
        AND relation = 'user_profile'::regclass AND mode = 'AccessExclusiveLock') THEN
        RAISE EXCEPTION '검증 트랜잭션에 강한 잠금이 남았다.';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_locks WHERE pid = pg_backend_pid()
        AND relation = 'user_profile'::regclass AND mode = 'ShareUpdateExclusiveLock') THEN
        RAISE EXCEPTION '검증 잠금 모드를 확인할 수 없다.';
    END IF;
    RAISE NOTICE 'PASS: 별도 V121 트랜잭션의 검증 완료 및 잠금 확인';
END $$;
COMMIT;
SELECT version() AS verified_postgresql_version;
DROP SCHEMA lan544_verification CASCADE;
\echo 'LAN-544 PostgreSQL verification passed; verification schema removed.'
