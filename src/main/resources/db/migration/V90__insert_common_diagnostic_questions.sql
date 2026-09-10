-- 검증된 공통 진단 4문항을 추가하고 과거 수준별 질문과 음원은 보존한다.
-- 음원: docs/tasks/LAN-438/audio-verification.json (Gemini 블라인드 전사·음질 검증).
INSERT INTO scenario_question (
    scenario_id, display_order, question_level_group, response_demand,
    status, created_at, updated_at
)
SELECT s.id, q.question_order, 'DIAGNOSTIC', q.demand, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM scenario s
CROSS JOIN (VALUES (1, 'MEDIUM'), (2, 'MEDIUM'), (3, 'HIGH'), (4, 'HIGH'))
    AS q(question_order, demand)
WHERE s.id = 1;

INSERT INTO scenario_question_language_variant (
    scenario_question_id, target_locale, base_locale, question_text, question_translation,
    audio_url, required_response_element, inner_thought, inner_thought_type,
    status, created_at, updated_at
)
SELECT q.id, 'EN', 'KR',
    'Hey! I''m Marco, an exchange student from Spain. Nice to meet you! Can you introduce yourself a little? — What do you do, and what do you like to do in your free time?',
    '안녕! 난 마르코고, 스페인에서 온 교환학생이야. 만나서 반가워! 자기소개를 간단히 해줄래? — 어떤 일을 하고, 여가 시간에는 뭘 즐겨 해?',
    'https://d19azau1un4t7r.cloudfront.net/content/scenarios/1/diagnostic-questions/95a682253d94b307ed64fad30155f844a5d584990e1e938d8f08d54a99c916b8.mp3',
    'Describe what you do.
State what you like to do in your free time.',
    (SELECT v.inner_thought FROM scenario_question original
        JOIN scenario_question_language_variant v ON v.scenario_question_id = original.id
        WHERE original.scenario_id = 1 AND original.question_level_group = 'LEVEL_4_TO_5'
            AND original.display_order = 1 AND v.target_locale = 'EN' AND v.base_locale = 'KR'),
    (SELECT v.inner_thought_type FROM scenario_question original
        JOIN scenario_question_language_variant v ON v.scenario_question_id = original.id
        WHERE original.scenario_id = 1 AND original.question_level_group = 'LEVEL_4_TO_5'
            AND original.display_order = 1 AND v.target_locale = 'EN' AND v.base_locale = 'KR'),
    'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM scenario_question q
WHERE q.scenario_id = 1 AND q.question_level_group = 'DIAGNOSTIC' AND q.display_order = 1;

INSERT INTO scenario_question_language_variant (
    scenario_question_id, target_locale, base_locale, question_text, question_translation,
    audio_url, required_response_element, inner_thought, inner_thought_type,
    status, created_at, updated_at
)
SELECT q.id, 'EN', 'KR',
    'So, have you done anything fun or memorable lately?',
    '그럼, 최근에 했던 즐겁거나 기억에 남는 일 있어?',
    'https://d19azau1un4t7r.cloudfront.net/content/scenarios/1/diagnostic-questions/a581134b53756adf5fc5c1b8d767bc958f588eddb6ae44d07935daa1c5d1e562.mp3',
    'Share a recent fun or memorable experience, or state that you have not had one.',
    NULL, NULL,
    'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM scenario_question q
WHERE q.scenario_id = 1 AND q.question_level_group = 'DIAGNOSTIC' AND q.display_order = 2;

INSERT INTO scenario_question_language_variant (
    scenario_question_id, target_locale, base_locale, question_text, question_translation,
    audio_url, required_response_element, inner_thought, inner_thought_type,
    status, created_at, updated_at
)
SELECT q.id, 'EN', 'KR',
    'If you could travel anywhere in the world next year, where would you go? And why there?',
    '내년에 세계 어디든 여행할 수 있다면, 어디로 가고 싶어? 그 이유는 뭐야?',
    'https://d19azau1un4t7r.cloudfront.net/content/scenarios/1/diagnostic-questions/364e90ce95079065cb9547fae895b57287fb77a23e28f370a395fbeb6d68ebee.mp3',
    'Name a destination you would choose for next year.
Explain why you would choose that destination.',
    NULL, NULL,
    'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM scenario_question q
WHERE q.scenario_id = 1 AND q.question_level_group = 'DIAGNOSTIC' AND q.display_order = 3;

INSERT INTO scenario_question_language_variant (
    scenario_question_id, target_locale, base_locale, question_text, question_translation,
    audio_url, required_response_element, inner_thought, inner_thought_type,
    status, created_at, updated_at
)
SELECT q.id, 'EN', 'KR',
    'Some of my friends say traveling alone is way better than traveling with friends. I''m not so sure though. What do you think?',
    '내 친구들 중엔 혼자 여행하는 게 친구랑 가는 것보다 훨씬 낫다는 애들이 있거든. 난 잘 모르겠던데. 넌 어떻게 생각해?',
    'https://d19azau1un4t7r.cloudfront.net/content/scenarios/1/diagnostic-questions/84c1c7d74ccc01888c12fb6ba6fbd2269392ea2ec28cb55e6d49c973635399bb.mp3',
    'Express your view on traveling alone compared with traveling with friends.',
    NULL, NULL,
    'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM scenario_question q
WHERE q.scenario_id = 1 AND q.question_level_group = 'DIAGNOSTIC' AND q.display_order = 4;

UPDATE scenario SET total_question_count = 4 WHERE id = 1;
