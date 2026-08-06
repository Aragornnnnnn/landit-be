-- 앱 버전을 1.1.0으로 올리고 하위 버전에 강제 업데이트를 적용한다.

UPDATE app_version
SET version_name = '1.1.0',
    minimum_supported_version_name = '1.1.0',
    force_update_reason = '매일 학습을 챙겨주는 알림 기능이 생겼어요!',
    soft_update_reason = NULL,
    release_note = '서비스 신규 기능 및 정책 변경',
    released_at = CURRENT_TIMESTAMP
WHERE platform IN ('IOS', 'ANDROID');
