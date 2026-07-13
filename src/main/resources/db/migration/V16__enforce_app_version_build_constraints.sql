-- 앱 버전 정책의 빌드 번호 범위를 데이터베이스에서 보장한다.
ALTER TABLE app_version DROP CONSTRAINT chk_app_version_build;

ALTER TABLE app_version
    ADD CONSTRAINT chk_app_version_build CHECK (
        build_number >= 1
        AND minimum_supported_build_number >= 0
        AND minimum_supported_build_number <= build_number
    );
