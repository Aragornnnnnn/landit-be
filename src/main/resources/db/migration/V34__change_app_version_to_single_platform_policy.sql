-- 플랫폼별 단일 앱 버전 정책과 최소 지원 버전명을 적용한다.

ALTER TABLE app_version
    ADD COLUMN minimum_supported_version_name VARCHAR(30);

UPDATE app_version AS current_version
SET minimum_supported_version_name = (
    SELECT legacy_version.version_name
    FROM app_version AS legacy_version
    WHERE legacy_version.platform = current_version.platform
      AND legacy_version.build_number = current_version.minimum_supported_build_number
)
WHERE current_version.active = TRUE;

DELETE FROM app_version
WHERE active = FALSE;

ALTER TABLE app_version
    ALTER COLUMN minimum_supported_version_name SET NOT NULL;

ALTER TABLE app_version
    DROP CONSTRAINT chk_app_version_build;

ALTER TABLE app_version
    DROP COLUMN minimum_supported_build_number;

ALTER TABLE app_version
    ADD CONSTRAINT chk_app_version_build CHECK (build_number >= 1);

ALTER TABLE app_version
    DROP CONSTRAINT uk_app_version_platform_build;

DROP INDEX IF EXISTS uk_app_version_active_platform;

ALTER TABLE app_version
    ADD CONSTRAINT uk_app_version_platform UNIQUE (platform);
