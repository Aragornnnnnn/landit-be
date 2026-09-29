-- 탈퇴·재가입·기기 소유권 이전과 보존 자료를 포함한 소급 정리 검증 데이터를 만든다.
INSERT INTO user_profile (id, email, nickname, profile_image_url, target_locale, base_locale,
    current_level, push_permission_status, status, created_at, updated_at)
VALUES (90001, 'same@example.com', 'Old member', 'https://example.com/old.png', 'EN', 'KR',
    1, 'GRANTED', 'WITHDRAWN', TIMESTAMP '2026-01-01 00:00:00', TIMESTAMP '2026-02-01 00:00:00'),
    (90002, 'same@example.com', 'Rejoined', 'https://example.com/new.png', 'EN', 'KR',
    1, 'GRANTED', 'ACTIVE', TIMESTAMP '2026-03-01 00:00:00', TIMESTAMP '2026-03-01 00:00:00'),
    (90003, NULL, '탈퇴한 사용자', NULL, 'EN', 'KR',
    1, 'GRANTED', 'WITHDRAWN', TIMESTAMP '2026-01-01 00:00:00', TIMESTAMP '2026-02-01 00:00:00');

UPDATE user_profile SET subscription_status = 'ACTIVE', subscription_product_id = 'product',
    subscription_store = 'APP_STORE', subscription_event_at = TIMESTAMP '2026-01-01 00:00:00',
    subscription_expires_at = TIMESTAMP '2030-01-01 00:00:00' WHERE id = 90001;

INSERT INTO oauth_identity (id, user_profile_id, provider, provider_user_id, provider_email,
    status, created_at, updated_at)
VALUES (90001, 90001, 'GOOGLE', 'same-sub', 'same@example.com', 'UNLINKED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (90002, 90002, 'GOOGLE', 'same-sub', 'same@example.com', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (90003, 90003, 'APPLE', 'old-apple-sub', 'old-apple@example.com', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (90004, 90002, 'APPLE', 'active-apple-sub', 'active-apple@example.com', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO apple_user_migration (oauth_identity_id, transfer_sub, status)
VALUES (90003, 'old-transfer-sub', 'PREPARED'), (90004, 'active-transfer-sub', 'PREPARED');

INSERT INTO refresh_token (id, user_profile_id, token_hash, expires_at, revoked_at, created_at, updated_at)
VALUES (90001, 90001, 'expired-hash', TIMESTAMP '2025-01-01 00:00:00', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (90002, 90001, 'revoked-hash', TIMESTAMP '2030-01-01 00:00:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (90003, 90003, 'valid-hash', TIMESTAMP '2030-01-01 00:00:00', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (90004, 90002, 'active-hash', TIMESTAMP '2030-01-01 00:00:00', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO user_push_token (id, user_profile_id, platform, expo_push_token, status, installation_id, created_at, updated_at)
VALUES (90001, 90001, 'IOS', 'ExpoPushToken[old-installed]', 'ACTIVE', '00000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (90002, 90001, 'IOS', 'ExpoPushToken[old-revoked]', 'REVOKED', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (90003, 90002, 'IOS', 'ExpoPushToken[transferred]', 'ACTIVE', '00000000-0000-0000-0000-000000000002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (90004, 90003, 'IOS', 'ExpoPushToken[old-legacy]', 'ACTIVE', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO push_delivery (user_profile_id, user_push_token_id, sent_expo_push_token,
    notification_type, deduplication_key, title, body, deep_link, status, requested_at, created_at, updated_at)
VALUES (90001, 90003, 'ExpoPushToken[transferred]', 'TEST', 'lan593-old-owner',
    'title', 'body', 'landit://me', 'SENT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO mailbox_feedback (id, user_profile_id, feedback_type, content_text, processing_status, created_at, updated_at)
VALUES (90001, 90001, 'QUESTION', '보존할 문의', 'PENDING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO mailbox_feedback_attachment (feedback_id, object_key, content_type, file_size, display_order, created_at)
VALUES (90001, 'private/lan593/kept.png', 'image/png', 100, 0, CURRENT_TIMESTAMP);
INSERT INTO mailbox_letter (id, letter_type, title, body_text, preview_text, publication_status,
    published_at, created_at, updated_at)
VALUES (90001, 'REPLY', '보존할 답변', '답변 본문', '답변', 'PUBLISHED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO mailbox_letter_recipient (letter_id, user_profile_id, representative_feedback_id, created_at)
VALUES (90001, 90001, 90001, CURRENT_TIMESTAMP);
INSERT INTO subscription_event (event_id, user_profile_id, type, product_id, price, currency, occurred_at, created_at)
VALUES ('lan593-payment', 90001, 'INITIAL_PURCHASE', 'product', 1000, 'KRW', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO learning_session (id, user_profile_id, session_type, ai_tutor_id, target_locale, base_locale,
    input_mode, status, started_at, created_at, updated_at)
SELECT id, id, 'FREE_TALK', (SELECT MIN(id) FROM ai_tutor), 'EN', 'KR', 'TEXT', 'IN_PROGRESS',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM user_profile WHERE id IN (90001, 90002);
INSERT INTO free_talk_session (id, learning_session_id, character_id, created_at, updated_at)
VALUES (90001, 90001, 'chloe', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (90002, 90002, 'chloe', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO session_history (id, learning_session_id, user_profile_id, session_type, target_locale, base_locale,
    started_at, ended_at, duration_seconds, user_message_count, created_at)
SELECT id, id, id, 'FREE_TALK', 'EN', 'KR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, CURRENT_TIMESTAMP
FROM user_profile WHERE id IN (90001, 90002);
INSERT INTO session_history_message (id, session_history_id, message_sequence, turn_number, role,
    content, input_type, created_at, updated_at)
VALUES (90001, 90001, 1, 1, 'USER', '원문은 별도 정리 범위', 'TEXT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (90002, 90002, 1, 1, 'USER', '보존할 활성 회원 원문', 'TEXT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO conversation_memory (id, user_profile_id, memory_type, content, content_locale, confidence,
    status, valid_from, observed_at, recorded_at, extractor_version, embedding_model, embedding)
VALUES (90001, 90001, 'PROFILE', 'old memory', 'en', 1, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'v1', 'model', '[1,2]'),
    (90002, 90001, 'PROFILE', 'replacement memory', 'en', 1, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'v1', 'model', '[1,2]'),
    (90003, 90002, 'PROFILE', 'active memory', 'en', 1, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'v1', 'model', '[1,2]');
UPDATE conversation_memory SET status = 'SUPERSEDED', superseded_by_id = 90002,
    superseded_at = CURRENT_TIMESTAMP, valid_to = CURRENT_TIMESTAMP WHERE id = 90001;
INSERT INTO conversation_memory_source (memory_id, session_history_message_id)
VALUES (90001, 90001), (90002, 90001), (90003, 90002);
INSERT INTO free_talk_memory_retrieval (id, free_talk_session_id, retrieval_stage, memory_id, candidate_rank, policy_version)
VALUES (90001, 90001, 'OPENING', NULL, 0, 'v1'), (90002, 90001, 'FIRST_USER_TURN', 90001, 1, 'v1'),
    (90003, 90002, 'OPENING', 90003, 1, 'v1');
