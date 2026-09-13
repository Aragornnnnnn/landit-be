-- 기존 푸시 조회 계정에 public 전체 읽기 권한과 전용 RLS 정책을 부여한다.
-- Supabase SQL Editor에서 테이블 소유자 권한으로 전체를 실행한다. 비밀번호는 변경하지 않는다.
BEGIN;
SET LOCAL lock_timeout = '3s';
SET LOCAL statement_timeout = '30s';

DO $grant_push_reader$
DECLARE
    reader_oid OID;
    relation RECORD;
    creator RECORD;
BEGIN
    SELECT oid INTO reader_oid FROM pg_roles
    WHERE rolname = 'landit_push_reader'
      AND rolcanlogin AND NOT rolsuper AND NOT rolcreatedb AND NOT rolcreaterole
      AND NOT rolreplication AND NOT rolbypassrls AND NOT rolinherit;
    IF reader_oid IS NULL OR EXISTS (
        SELECT 1 FROM pg_auth_members WHERE member = reader_oid
    ) THEN
        RAISE EXCEPTION 'landit_push_reader must be an unprivileged login without role memberships';
    END IF;

    -- 제한 정책을 무시하면 대상 집계가 달라질 수 있으므로 자동 변경하지 않고 중단한다.
    IF EXISTS (
        SELECT 1 FROM pg_policy p
        JOIN pg_class c ON c.oid = p.polrelid
        JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE n.nspname = 'public' AND NOT p.polpermissive AND p.polcmd IN ('r', '*')
          AND (0::oid = ANY(p.polroles) OR reader_oid = ANY(p.polroles))
    ) THEN
        RAISE EXCEPTION 'A restrictive public SELECT policy needs review before granting full reads';
    END IF;

    GRANT USAGE ON SCHEMA public TO landit_push_reader;
    GRANT SELECT ON ALL TABLES IN SCHEMA public TO landit_push_reader;

    IF EXISTS (
        SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE n.nspname = 'public' AND c.relkind IN ('r', 'p', 'v', 'm', 'f')
          AND (has_table_privilege(reader_oid, c.oid, 'INSERT,UPDATE,DELETE,TRUNCATE')
               OR has_any_column_privilege(reader_oid, c.oid, 'INSERT,UPDATE'))
    ) THEN
        RAISE EXCEPTION 'landit_push_reader has existing write privileges; review them first';
    END IF;

    -- RLS를 켜거나 끄지 않는다. 향후 RLS 활성화에도 대비해 일반·파티션 테이블에 정책을 둔다.
    FOR relation IN
        SELECT c.oid, c.relname FROM pg_class c
        JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE n.nspname = 'public' AND c.relkind IN ('r', 'p')
    LOOP
        IF EXISTS (
            SELECT 1 FROM pg_policy
            WHERE polrelid = relation.oid AND polname = 'landit_push_reader_select'
        ) THEN
            IF NOT EXISTS (
                SELECT 1 FROM pg_policy
                WHERE polrelid = relation.oid AND polname = 'landit_push_reader_select'
                  AND polcmd = 'r' AND polpermissive AND polroles = ARRAY[reader_oid]
                  AND pg_get_expr(polqual, polrelid) = 'true'
            ) THEN
                RAISE EXCEPTION 'Existing policy on public.% needs review', relation.relname;
            END IF;
        ELSE
            EXECUTE format(
                'CREATE POLICY landit_push_reader_select ON public.%I '
                'FOR SELECT TO landit_push_reader USING (true)', relation.relname
            );
        END IF;
    END LOOP;

    -- 기본 권한은 테이블 생성 역할별 설정이다. 현재 소유자들과 실행 역할에 적용한다.
    FOR creator IN
        SELECT DISTINCT pg_get_userbyid(c.relowner) AS role_name
        FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE n.nspname = 'public' AND c.relkind IN ('r', 'p', 'v', 'm', 'f')
        UNION SELECT current_user
    LOOP
        IF EXISTS (
            SELECT 1 FROM pg_default_acl d
            CROSS JOIN LATERAL aclexplode(d.defaclacl) a
            WHERE d.defaclrole = (SELECT oid FROM pg_roles WHERE rolname = creator.role_name)
              AND d.defaclobjtype = 'r'
              AND d.defaclnamespace IN (0, 'public'::regnamespace::oid)
              AND a.grantee IN (0, reader_oid)
              AND a.privilege_type IN ('INSERT', 'UPDATE', 'DELETE', 'TRUNCATE')
        ) THEN
            RAISE EXCEPTION 'Existing write defaults for creator % need review', creator.role_name;
        END IF;
        EXECUTE format(
            'ALTER DEFAULT PRIVILEGES FOR ROLE %I IN SCHEMA public '
            'GRANT SELECT ON TABLES TO landit_push_reader', creator.role_name
        );
    END LOOP;
END;
$grant_push_reader$;

COMMIT;

-- can_read=true, can_write=false인지 확인한다. 아래 결과는 행별 RLS 가시성 검증과 별개다.
SELECT tablename, tableowner, rowsecurity,
       has_table_privilege('landit_push_reader', format('public.%I', tablename), 'SELECT') AS can_read,
       (has_table_privilege('landit_push_reader', format('public.%I', tablename),
                            'INSERT,UPDATE,DELETE,TRUNCATE')
        OR has_any_column_privilege('landit_push_reader', format('public.%I', tablename),
                                    'INSERT,UPDATE')) AS can_write
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY tablename;
