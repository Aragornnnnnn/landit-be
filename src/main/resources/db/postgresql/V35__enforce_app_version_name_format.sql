-- PostgreSQL에서 앱 버전명과 최소 지원 버전명의 Major.Minor.Patch 형식을 강제한다.

ALTER TABLE app_version
    ADD CONSTRAINT chk_app_version_version_name
        CHECK (version_name ~ '^[0-9]+\.[0-9]+\.[0-9]+$');

ALTER TABLE app_version
    ADD CONSTRAINT chk_app_version_minimum_supported_version_name
        CHECK (minimum_supported_version_name ~ '^[0-9]+\.[0-9]+\.[0-9]+$');
