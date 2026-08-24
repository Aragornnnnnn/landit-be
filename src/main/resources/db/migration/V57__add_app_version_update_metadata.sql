-- 앱 버전 정책의 마지막 수정 시각과 수정자 프로필을 저장한다.

ALTER TABLE app_version
    ADD COLUMN updated_at TIMESTAMP(6);

ALTER TABLE app_version
    ADD COLUMN updated_by_user_profile_id BIGINT;

UPDATE app_version AS app_version
SET updated_at = COALESCE(
        (
            SELECT audit_log.created_at
            FROM admin_audit_log AS audit_log
            WHERE audit_log.action = 'APP_VERSION_UPDATED'
              AND audit_log.target_type = 'APP_VERSION'
              AND audit_log.target_id = app_version.platform
            ORDER BY audit_log.created_at DESC, audit_log.id DESC
            LIMIT 1
        ),
        app_version.created_at
    ),
    updated_by_user_profile_id = (
        SELECT audit_log.admin_user_profile_id
        FROM admin_audit_log AS audit_log
        WHERE audit_log.action = 'APP_VERSION_UPDATED'
          AND audit_log.target_type = 'APP_VERSION'
          AND audit_log.target_id = app_version.platform
        ORDER BY audit_log.created_at DESC, audit_log.id DESC
        LIMIT 1
    );

ALTER TABLE app_version
    ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE app_version
    ADD CONSTRAINT fk_app_version_updated_by_user_profile_id
        FOREIGN KEY (updated_by_user_profile_id) REFERENCES user_profile (id);
