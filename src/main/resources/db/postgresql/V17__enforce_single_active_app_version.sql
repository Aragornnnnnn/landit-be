-- 플랫폼별 활성 앱 버전 정책을 한 건으로 제한한다.
CREATE UNIQUE INDEX uk_app_version_active_platform
    ON app_version (platform)
    WHERE active = TRUE;
