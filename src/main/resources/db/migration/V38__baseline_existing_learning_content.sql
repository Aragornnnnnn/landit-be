-- 기존 시나리오와 Writing 표현을 재구성하고 서비스 캐릭터명과 썸네일을 최신화한다.
-- LAN-207 기준 데이터는 ID 1~83이 비어 있는 환경에서만 누락 행을 보완한다.

INSERT INTO category (
    id, display_order, status, created_at, updated_at
)
SELECT
        1,
        1,
        'ACTIVE',
        '2026-07-07T16:53:27',
        '2026-07-07T16:53:28'
WHERE NOT EXISTS (
    SELECT 1 FROM category WHERE id = 1
);
INSERT INTO category (
    id, display_order, status, created_at, updated_at
)
SELECT
        2,
        2,
        'ACTIVE',
        '2026-07-07T16:53:39',
        '2026-07-07T16:53:41'
WHERE NOT EXISTS (
    SELECT 1 FROM category WHERE id = 2
);
INSERT INTO category (
    id, display_order, status, created_at, updated_at
)
SELECT
        3,
        3,
        'ACTIVE',
        '2026-07-07T16:53:50',
        '2026-07-07T16:53:51'
WHERE NOT EXISTS (
    SELECT 1 FROM category WHERE id = 3
);
INSERT INTO category_language_variant (
    id, category_id, base_locale, name, created_at, updated_at
)
SELECT
        1,
        1,
        'KR',
        '기숙사',
        '2026-07-07T16:54:31',
        '2026-07-07T16:54:32'
WHERE NOT EXISTS (
    SELECT 1 FROM category_language_variant WHERE id = 1
);
INSERT INTO category_language_variant (
    id, category_id, base_locale, name, created_at, updated_at
)
SELECT
        2,
        2,
        'KR',
        '여행',
        '2026-07-07T16:54:50',
        '2026-07-07T16:54:51'
WHERE NOT EXISTS (
    SELECT 1 FROM category_language_variant WHERE id = 2
);
INSERT INTO category_language_variant (
    id, category_id, base_locale, name, created_at, updated_at
)
SELECT
        3,
        3,
        'KR',
        '수업',
        '2026-07-07T16:55:08',
        '2026-07-07T16:55:10'
WHERE NOT EXISTS (
    SELECT 1 FROM category_language_variant WHERE id = 3
);
INSERT INTO scenario (
    id, category_id, ai_role, difficulty, first_speaker, thumbnail_url, display_order, status, total_question_count, created_at, updated_at
)
SELECT
        1,
        1,
        '미국인 대학생 룸메이트, 외향적이고 친화적인 성격',
        'EASY',
        'AI',
        'https://d19azau1un4t7r.cloudfront.net/content/scenarios/1/thumbnail/41e15ef2-3923-4749-9ac5-4e2824fed54e.png',
        1,
        'ACTIVE',
        3,
        '2026-07-07T00:00:00',
        '2026-07-16T02:21:40.057033'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario WHERE id = 1
);
INSERT INTO scenario (
    id, category_id, ai_role, difficulty, first_speaker, thumbnail_url, display_order, status, total_question_count, created_at, updated_at
)
SELECT
        2,
        1,
        '룸메이트, 실용적이고 솔직하게 생활 규칙을 정하고 싶어함',
        'NORMAL',
        'AI',
        'https://d19azau1un4t7r.cloudfront.net/content/scenarios/2/thumbnail/2da49db2-946a-49c9-b526-6c979063cca7.png',
        2,
        'ACTIVE',
        3,
        '2026-07-07T00:00:00',
        '2026-07-16T02:21:40.057033'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario WHERE id = 2
);
INSERT INTO scenario (
    id, category_id, ai_role, difficulty, first_speaker, thumbnail_url, display_order, status, total_question_count, created_at, updated_at
)
SELECT
        3,
        1,
        '룸메이트, 주말에 같이 놀고 싶어하는 밝은 성격',
        'EASY',
        'AI',
        'https://d19azau1un4t7r.cloudfront.net/content/scenarios/3/thumbnail/b8821003-0f19-4153-a0c7-b119a46dcbab.png',
        3,
        'ACTIVE',
        3,
        '2026-07-07T00:00:00',
        '2026-07-16T02:21:40.057033'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario WHERE id = 3
);
INSERT INTO scenario (
    id, category_id, ai_role, difficulty, first_speaker, thumbnail_url, display_order, status, total_question_count, created_at, updated_at
)
SELECT
        4,
        1,
        '기숙사 프론트 데스크 직원, 매뉴얼에 따라 사무적이면서도 친절하게 응대',
        'HARD',
        'USER',
        'https://d19azau1un4t7r.cloudfront.net/content/scenarios/4/thumbnail/b6929083-b21b-44df-a90a-ee010201f56d.png',
        4,
        'ACTIVE',
        4,
        '2026-07-07T00:00:00',
        '2026-07-16T02:21:40.057033'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario WHERE id = 4
);
INSERT INTO scenario (
    id, category_id, ai_role, difficulty, first_speaker, thumbnail_url, display_order, status, total_question_count, created_at, updated_at
)
SELECT
        5,
        1,
        '룸메이트, 친구를 자주 초대하고 음악을 틀고 공부하는 편이라 경계를 미리 정하고 싶어함',
        'NORMAL',
        'AI',
        'https://d19azau1un4t7r.cloudfront.net/content/scenarios/5/thumbnail/86a20939-09e8-427d-9736-1019226eae5a.png',
        5,
        'ACTIVE',
        3,
        '2026-07-07T00:00:00',
        '2026-07-16T02:21:40.057033'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario WHERE id = 5
);
INSERT INTO scenario (
    id, category_id, ai_role, difficulty, first_speaker, thumbnail_url, display_order, status, total_question_count, created_at, updated_at
)
SELECT
        6,
        1,
        '기숙사 파티에서 처음 만난 다른 유학생, 밝고 사교적인 성격',
        'EASY',
        'AI',
        'https://d19azau1un4t7r.cloudfront.net/content/scenarios/6/thumbnail/f8c0d2fe-9b01-440c-829b-96459946982b.png',
        6,
        'ACTIVE',
        3,
        '2026-07-07T00:00:00',
        '2026-07-16T02:21:40.057033'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario WHERE id = 6
);
INSERT INTO scenario (
    id, category_id, ai_role, difficulty, first_speaker, thumbnail_url, display_order, status, total_question_count, created_at, updated_at
)
SELECT
        7,
        1,
        '룸메이트, 이제 꽤 친해져서 깊은 이야기도 나누고 싶어함',
        'NORMAL',
        'AI',
        'https://d19azau1un4t7r.cloudfront.net/content/scenarios/7/thumbnail/97e5dd23-b895-47f0-9f4a-9ba6fb12ef68.png',
        7,
        'ACTIVE',
        3,
        '2026-07-07T00:00:00',
        '2026-07-16T02:21:40.057033'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario WHERE id = 7
);
INSERT INTO scenario (
    id, category_id, ai_role, difficulty, first_speaker, thumbnail_url, display_order, status, total_question_count, created_at, updated_at
)
SELECT
        8,
        3,
        '같은 수업을 듣는 학생, 낯가림 없이 먼저 다가오는 성격',
        'EASY',
        'AI',
        'https://d19azau1un4t7r.cloudfront.net/content/scenarios/8/thumbnail/0ee6fc4a-0c91-4c50-88a8-c6b0b9fdb8fb.png',
        1,
        'ACTIVE',
        3,
        '2026-07-07T00:00:00',
        '2026-07-16T02:21:40.057033'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario WHERE id = 8
);
INSERT INTO scenario (
    id, category_id, ai_role, difficulty, first_speaker, thumbnail_url, display_order, status, total_question_count, created_at, updated_at
)
SELECT
        9,
        3,
        '조별과제 팀원, 발표를 앞두고 긴장한 성격',
        'NORMAL',
        'AI',
        'https://d19azau1un4t7r.cloudfront.net/content/scenarios/9/thumbnail/ac6f40ec-3e95-421d-b63e-d59255a735dd.png',
        2,
        'ACTIVE',
        3,
        '2026-07-07T00:00:00',
        '2026-07-16T02:21:40.057033'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario WHERE id = 9
);
INSERT INTO scenario (
    id, category_id, ai_role, difficulty, first_speaker, thumbnail_url, display_order, status, total_question_count, created_at, updated_at
)
SELECT
        10,
        3,
        '담당 교수, 공정하지만 격식 있고 신중한 태도',
        'HARD',
        'USER',
        'https://d19azau1un4t7r.cloudfront.net/content/scenarios/10/thumbnail/f87f5c2c-02a1-4290-95f5-7b5497c280b1.png',
        3,
        'ACTIVE',
        4,
        '2026-07-07T00:00:00',
        '2026-07-16T02:21:40.057033'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario WHERE id = 10
);
INSERT INTO scenario (
    id, category_id, ai_role, difficulty, first_speaker, thumbnail_url, display_order, status, total_question_count, created_at, updated_at
)
SELECT
        11,
        3,
        '같은 수업 친구, 편하게 시험 스트레스를 나누는 성격',
        'EASY',
        'AI',
        'https://d19azau1un4t7r.cloudfront.net/content/scenarios/11/thumbnail/9699f877-b78f-4a2f-b350-53e494cc53ed.png',
        4,
        'ACTIVE',
        3,
        '2026-07-07T00:00:00',
        '2026-07-16T02:21:40.057033'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario WHERE id = 11
);
INSERT INTO scenario (
    id, category_id, ai_role, difficulty, first_speaker, thumbnail_url, display_order, status, total_question_count, created_at, updated_at
)
SELECT
        12,
        3,
        '조별 토론 팀원, 논리적이고 적극적으로 의견을 주고받는 성격',
        'HARD',
        'AI',
        'https://d19azau1un4t7r.cloudfront.net/content/scenarios/12/thumbnail/de8d779e-59d8-480b-9a55-11246629e752.png',
        5,
        'ACTIVE',
        3,
        '2026-07-07T00:00:00',
        '2026-07-16T02:21:40.057033'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario WHERE id = 12
);
INSERT INTO scenario (
    id, category_id, ai_role, difficulty, first_speaker, thumbnail_url, display_order, status, total_question_count, created_at, updated_at
)
SELECT
        13,
        2,
        '비행기 옆자리에 앉은 여행자, 여행을 좋아하고 대화하기 편한 성격',
        'EASY',
        'AI',
        'https://d19azau1un4t7r.cloudfront.net/content/scenarios/13/thumbnail/ed93dc13-2594-4360-b4af-284b2af9b397.png',
        1,
        'ACTIVE',
        3,
        '2026-07-07T00:00:00',
        '2026-07-16T02:21:40.057033'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario WHERE id = 13
);
INSERT INTO scenario (
    id, category_id, ai_role, difficulty, first_speaker, thumbnail_url, display_order, status, total_question_count, created_at, updated_at
)
SELECT
        14,
        2,
        '항공사 카운터 직원, 매뉴얼대로 정중하게 보상 절차를 안내',
        'HARD',
        'USER',
        'https://d19azau1un4t7r.cloudfront.net/content/scenarios/14/thumbnail/8ab5c295-b8e4-4e6f-8d98-16e0ef575362.png',
        2,
        'ACTIVE',
        4,
        '2026-07-07T00:00:00',
        '2026-07-16T02:21:40.057033'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario WHERE id = 14
);
INSERT INTO scenario (
    id, category_id, ai_role, difficulty, first_speaker, thumbnail_url, display_order, status, total_question_count, created_at, updated_at
)
SELECT
        15,
        2,
        '호텔 프론트 데스크 직원, 친절하고 정중한 서비스직 태도',
        'EASY',
        'AI',
        'https://d19azau1un4t7r.cloudfront.net/content/scenarios/15/thumbnail/f8e7a303-bc68-46e4-91e7-09dc95ee4ef6.png',
        3,
        'ACTIVE',
        3,
        '2026-07-07T00:00:00',
        '2026-07-16T02:21:40.057033'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario WHERE id = 15
);
INSERT INTO scenario (
    id, category_id, ai_role, difficulty, first_speaker, thumbnail_url, display_order, status, total_question_count, created_at, updated_at
)
SELECT
        16,
        2,
        '낯선 사람, 호감을 표현하며 적극적으로 다가오는 성격',
        'NORMAL',
        'AI',
        'https://d19azau1un4t7r.cloudfront.net/content/scenarios/16/thumbnail/253c75d2-f39f-486c-b8fb-0d0e59696275.png',
        4,
        'ACTIVE',
        3,
        '2026-07-07T00:00:00',
        '2026-07-16T02:21:40.057033'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario WHERE id = 16
);
INSERT INTO scenario (
    id, category_id, ai_role, difficulty, first_speaker, thumbnail_url, display_order, status, total_question_count, created_at, updated_at
)
SELECT
        17,
        2,
        '카페/식당 직원, 친절하고 캐주얼한 서비스 태도',
        'EASY',
        'AI',
        'https://d19azau1un4t7r.cloudfront.net/content/scenarios/17/thumbnail/b82d0860-8fcb-44ea-8116-c2f924302757.png',
        5,
        'ACTIVE',
        3,
        '2026-07-07T00:00:00',
        '2026-07-16T02:21:40.057033'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario WHERE id = 17
);
INSERT INTO scenario (
    id, category_id, ai_role, difficulty, first_speaker, thumbnail_url, display_order, status, total_question_count, created_at, updated_at
)
SELECT
        18,
        2,
        '약사, 차분하고 전문적으로 증상을 확인하는 태도',
        'NORMAL',
        'AI',
        'https://d19azau1un4t7r.cloudfront.net/content/scenarios/18/thumbnail/0681abf4-76b7-46a1-a776-9973015ace04.png',
        6,
        'ACTIVE',
        3,
        '2026-07-07T00:00:00',
        '2026-07-16T02:21:40.057033'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario WHERE id = 18
);
INSERT INTO scenario (
    id, category_id, ai_role, difficulty, first_speaker, thumbnail_url, display_order, status, total_question_count, created_at, updated_at
)
SELECT
        19,
        2,
        '길에서 만난 현지인 행인, 친절하게 길을 안내해주는 성격',
        'NORMAL',
        'USER',
        'https://d19azau1un4t7r.cloudfront.net/content/scenarios/19/thumbnail/26ec489b-4d45-4ebf-a7e2-8b7a944113b0.png',
        7,
        'ACTIVE',
        4,
        '2026-07-07T00:00:00',
        '2026-07-16T02:21:40.057033'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario WHERE id = 19
);
INSERT INTO scenario (
    id, category_id, ai_role, difficulty, first_speaker, thumbnail_url, display_order, status, total_question_count, created_at, updated_at
)
SELECT
        20,
        2,
        '친구, 여행 이야기에 호기심 많고 수다스러운 성격',
        'NORMAL',
        'AI',
        'https://d19azau1un4t7r.cloudfront.net/content/scenarios/20/thumbnail/495ab179-eefb-4126-9407-a2910add9c4b.png',
        8,
        'ACTIVE',
        3,
        '2026-07-07T00:00:00',
        '2026-07-16T02:21:40.057033'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario WHERE id = 20
);
INSERT INTO scenario_language_variant (
    id, scenario_id, target_locale, base_locale, title, briefing, user_opening_instruction, conversation_goal, status, tts_voice_id, created_at, updated_at
)
SELECT
        1,
        1,
        'EN',
        'KR',
        '입주 첫날, 룸메이트 Marco와 첫 만남',
        '기숙사 입주 첫날, 방문을 열자 이미 짐을 풀고 있던 룸메이트 Marco가 반갑게 인사를 건넨다.',
        NULL,
        'Marco에게 자기소개를 하고, 취미와 한국 여행지를 자연스럽게 소개하기',
        'ACTIVE',
        2,
        '2026-07-07T00:00:00',
        '2026-07-07T00:00:00'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_language_variant WHERE id = 1
);
INSERT INTO scenario_language_variant (
    id, scenario_id, target_locale, base_locale, title, briefing, user_opening_instruction, conversation_goal, status, tts_voice_id, created_at, updated_at
)
SELECT
        2,
        2,
        'EN',
        'KR',
        '집안일과 생활 규칙 정하기',
        '입주 며칠 후, Marco가 편하게 지내기 위해 청소·생활 패턴·규칙에 대해 미리 얘기해보자고 한다.',
        NULL,
        '청소 분담, 생활 리듬, 룸메이트로서의 마지노선을 Marco와 솔직하게 조율하기',
        'ACTIVE',
        2,
        '2026-07-07T00:00:00',
        '2026-07-07T00:00:00'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_language_variant WHERE id = 2
);
INSERT INTO scenario_language_variant (
    id, scenario_id, target_locale, base_locale, title, briefing, user_opening_instruction, conversation_goal, status, tts_voice_id, created_at, updated_at
)
SELECT
        3,
        3,
        'EN',
        'KR',
        '카페 수다 — 주말 약속 잡기',
        '카페에서 Marco와 커피를 마시다가 자연스럽게 주말 계획 얘기가 나온다.',
        NULL,
        'Marco와 주말 약속(요일, 할 일)을 정하고 서로의 취향 알아가기',
        'ACTIVE',
        2,
        '2026-07-07T00:00:00',
        '2026-07-07T00:00:00'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_language_variant WHERE id = 3
);
INSERT INTO scenario_language_variant (
    id, scenario_id, target_locale, base_locale, title, briefing, user_opening_instruction, conversation_goal, status, tts_voice_id, created_at, updated_at
)
SELECT
        4,
        4,
        'EN',
        'KR',
        '기숙사 에어컨 요금 문제 — 프론트에 전화하기',
        '7월 한 달 내내 여행으로 기숙사를 비웠는데 에어컨 요금이 100달러나 청구되었다. 기숙사 프론트에 전화해 Chloe에게 상황을 설명해야 한다.',
        '7월 한 달 내내 여행을 다니느라 기숙사를 비웠는데 에어컨 요금이 100달러나 나왔다. 기숙사 프론트에 전화해서 Chloe에게 상황을 설명하자.',
        'Chloe에게 부당 청구 상황을 설명하고, 증빙 제출과 환불 방식을 정하기',
        'ACTIVE',
        1,
        '2026-07-07T00:00:00',
        '2026-07-07T00:00:00'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_language_variant WHERE id = 4
);
INSERT INTO scenario_language_variant (
    id, scenario_id, target_locale, base_locale, title, briefing, user_opening_instruction, conversation_goal, status, tts_voice_id, created_at, updated_at
)
SELECT
        5,
        5,
        'EN',
        'KR',
        '소음, 손님, 경계 정하기',
        '같이 지낸 지 좀 됐지만, Marco가 손님 초대·소음·물건 공유에 대한 경계를 확실히 정해두고 싶어한다.',
        NULL,
        '손님, 소음, 물건 공유에 대한 서로의 기준을 명확히 정하기',
        'ACTIVE',
        2,
        '2026-07-07T00:00:00',
        '2026-07-07T00:00:00'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_language_variant WHERE id = 5
);
INSERT INTO scenario_language_variant (
    id, scenario_id, target_locale, base_locale, title, briefing, user_opening_instruction, conversation_goal, status, tts_voice_id, created_at, updated_at
)
SELECT
        6,
        6,
        'EN',
        'KR',
        '인터내셔널 파티 — 처음 만난 Chloe',
        '기숙사에서 열린 인터내셔널 파티. 처음 보는 Chloe가 먼저 다가와 말을 건다.',
        NULL,
        '처음 만난 Chloe와 자연스럽게 스몰토크하고 다음 모임 초대에 응답하기',
        'ACTIVE',
        1,
        '2026-07-07T00:00:00',
        '2026-07-07T00:00:00'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_language_variant WHERE id = 6
);
INSERT INTO scenario_language_variant (
    id, scenario_id, target_locale, base_locale, title, briefing, user_opening_instruction, conversation_goal, status, tts_voice_id, created_at, updated_at
)
SELECT
        7,
        7,
        'EN',
        'KR',
        '서로 더 알아가는 밤',
        '밤에 방에서 Marco와 둘이 있는데, Marco가 좀 더 깊은 이야기를 나누고 싶어한다.',
        NULL,
        'Marco와 가족, 꿈, 요즘 고민 등 개인적인 이야기를 진솔하게 나누기',
        'ACTIVE',
        2,
        '2026-07-07T00:00:00',
        '2026-07-07T00:00:00'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_language_variant WHERE id = 7
);
INSERT INTO scenario_language_variant (
    id, scenario_id, target_locale, base_locale, title, briefing, user_opening_instruction, conversation_goal, status, tts_voice_id, created_at, updated_at
)
SELECT
        8,
        8,
        'EN',
        'KR',
        '첫 수업, 옆자리 Marco',
        '첫 수업 시간, 옆자리에 앉아도 되는지 묻는 Marco과 대화가 시작된다.',
        NULL,
        'Marco과 자연스럽게 첫 대화를 나누고 한국에서의 학교생활을 소개하기',
        'ACTIVE',
        2,
        '2026-07-07T00:00:00',
        '2026-07-07T00:00:00'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_language_variant WHERE id = 8
);
INSERT INTO scenario_language_variant (
    id, scenario_id, target_locale, base_locale, title, briefing, user_opening_instruction, conversation_goal, status, tts_voice_id, created_at, updated_at
)
SELECT
        9,
        9,
        'EN',
        'KR',
        '조별 발표 준비하기',
        '다음 주 조별 발표를 앞두고, 팀원 Chloe와 역할 분담과 준비 상황을 논의한다.',
        NULL,
        'Chloe와 발표 경험을 공유하고 역할을 분담하기',
        'ACTIVE',
        1,
        '2026-07-07T00:00:00',
        '2026-07-07T00:00:00'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_language_variant WHERE id = 9
);
INSERT INTO scenario_language_variant (
    id, scenario_id, target_locale, base_locale, title, briefing, user_opening_instruction, conversation_goal, status, tts_voice_id, created_at, updated_at
)
SELECT
        10,
        10,
        'EN',
        'KR',
        '교수님 오피스아워 방문',
        '지난 과제 성적이 생각보다 낮게 나와, 교수님을 찾아가 정중하게 이유를 여쭙고 개선 방법을 물어봐야 한다.',
        '지난 과제 성적이 생각보다 낮게 나왔다. 교수님을 찾아가 정중하게 이유를 여쭙고 어떻게 개선할 수 있을지 물어보자.',
        '교수님께 정중하게 감점 사유를 여쭙고 개선 방법과 후속 조치를 확인하기',
        'ACTIVE',
        2,
        '2026-07-07T00:00:00',
        '2026-07-07T00:00:00'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_language_variant WHERE id = 10
);
INSERT INTO scenario_language_variant (
    id, scenario_id, target_locale, base_locale, title, briefing, user_opening_instruction, conversation_goal, status, tts_voice_id, created_at, updated_at
)
SELECT
        11,
        11,
        'EN',
        'KR',
        '시험 공부 수다',
        '기말고사를 앞두고 친구 Chloe와 공부 습관에 대해 수다를 떤다.',
        NULL,
        'Chloe와 공부 습관과 경험을 나누고 같이 공부할 시간을 정하기',
        'ACTIVE',
        1,
        '2026-07-07T00:00:00',
        '2026-07-07T00:00:00'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_language_variant WHERE id = 11
);
INSERT INTO scenario_language_variant (
    id, scenario_id, target_locale, base_locale, title, briefing, user_opening_instruction, conversation_goal, status, tts_voice_id, created_at, updated_at
)
SELECT
        12,
        12,
        'EN',
        'KR',
        '토론 수업 — 돈과 행복',
        '오늘 토론 수업 주제는 "돈으로 행복을 살 수 있는가". 조원 Marco과 의견을 나눈다.',
        NULL,
        '자신의 입장을 근거와 함께 논리적으로 표현하고 Marco과 토론하기',
        'ACTIVE',
        2,
        '2026-07-07T00:00:00',
        '2026-07-07T00:00:00'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_language_variant WHERE id = 12
);
INSERT INTO scenario_language_variant (
    id, scenario_id, target_locale, base_locale, title, briefing, user_opening_instruction, conversation_goal, status, tts_voice_id, created_at, updated_at
)
SELECT
        13,
        13,
        'EN',
        'KR',
        '비행기 옆자리 승객과의 대화',
        '장거리 비행 중, 옆자리에 앉은 Marco이 먼저 말을 건다.',
        NULL,
        'Marco과 여행 목적과 취향에 대해 자연스럽게 대화 나누기',
        'ACTIVE',
        2,
        '2026-07-07T00:00:00',
        '2026-07-07T00:00:00'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_language_variant WHERE id = 13
);
INSERT INTO scenario_language_variant (
    id, scenario_id, target_locale, base_locale, title, briefing, user_opening_instruction, conversation_goal, status, tts_voice_id, created_at, updated_at
)
SELECT
        14,
        14,
        'EN',
        'KR',
        '수하물 파손 — 카운터에 항의하기',
        '캐리어가 파손된 채로 도착했다. 카운터 직원 Marco에게 상황을 설명하고 보상책을 물어봐야 한다.',
        '당신은 캐리어가 부서졌다. 카운터 직원 Marco에게 상황을 설명하고 보상책을 물어봐야한다.',
        'Marco에게 수하물 파손 상황을 설명하고 보상 방식을 정하기',
        'ACTIVE',
        2,
        '2026-07-07T00:00:00',
        '2026-07-07T00:00:00'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_language_variant WHERE id = 14
);
INSERT INTO scenario_language_variant (
    id, scenario_id, target_locale, base_locale, title, briefing, user_opening_instruction, conversation_goal, status, tts_voice_id, created_at, updated_at
)
SELECT
        15,
        15,
        'EN',
        'KR',
        '호텔 체크인 하기',
        '호텔에 도착해 체크인을 하며 프론트 직원 Chloe와 대화한다.',
        NULL,
        'Chloe에게 예약 정보를 전달하고 객실 선호사항을 요청하기',
        'ACTIVE',
        1,
        '2026-07-07T00:00:00',
        '2026-07-07T00:00:00'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_language_variant WHERE id = 15
);
INSERT INTO scenario_language_variant (
    id, scenario_id, target_locale, base_locale, title, briefing, user_opening_instruction, conversation_goal, status, tts_voice_id, created_at, updated_at
)
SELECT
        16,
        16,
        'EN',
        'KR',
        '마음에 안 드는 사람 정중히 거절하기',
        '길에서 만난 Marco가 호감을 표현하며 데이트를 제안한다. 정중하게 거절해야 하는 상황.',
        NULL,
        'Marco의 제안을 무례하지 않으면서도 명확하게 거절하기',
        'ACTIVE',
        2,
        '2026-07-07T00:00:00',
        '2026-07-07T00:00:00'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_language_variant WHERE id = 16
);
INSERT INTO scenario_language_variant (
    id, scenario_id, target_locale, base_locale, title, briefing, user_opening_instruction, conversation_goal, status, tts_voice_id, created_at, updated_at
)
SELECT
        17,
        17,
        'EN',
        'KR',
        '카페에서 주문하기',
        '카페에 들어가 직원 Marco에게 주문을 한다.',
        NULL,
        'Marco에게 원하는 메뉴를 주문하고 매장/포장 여부를 전달하기',
        'ACTIVE',
        2,
        '2026-07-07T00:00:00',
        '2026-07-07T00:00:00'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_language_variant WHERE id = 17
);
INSERT INTO scenario_language_variant (
    id, scenario_id, target_locale, base_locale, title, briefing, user_opening_instruction, conversation_goal, status, tts_voice_id, created_at, updated_at
)
SELECT
        18,
        18,
        'EN',
        'KR',
        '약국에서 증상 설명하고 약 사기',
        '두통이 심해 약국에 방문했다. 약사 Chloe에게 증상을 설명하고 약을 사야 한다.',
        NULL,
        'Chloe에게 증상을 정확히 설명하고 필요한 정보를 전달하기',
        'ACTIVE',
        1,
        '2026-07-07T00:00:00',
        '2026-07-07T00:00:00'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_language_variant WHERE id = 18
);
INSERT INTO scenario_language_variant (
    id, scenario_id, target_locale, base_locale, title, briefing, user_opening_instruction, conversation_goal, status, tts_voice_id, created_at, updated_at
)
SELECT
        19,
        19,
        'EN',
        'KR',
        '길 잃고 현지인에게 길 묻기',
        '백화점에서 길을 잃었다. 지나가는 행인 Chloe에게 Tower Bridge로 가는 방향을 물어봐야 한다.',
        '당신은 백화점에서 길을 잃었다. 지나가는 행인 Chloe에게 어디로 나가야 Tower bridge가 가까운지 물어봐야한다.',
        'Chloe에게 길을 묻고 자연스럽게 대화를 이어가기',
        'ACTIVE',
        1,
        '2026-07-07T00:00:00',
        '2026-07-07T00:00:00'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_language_variant WHERE id = 19
);
INSERT INTO scenario_language_variant (
    id, scenario_id, target_locale, base_locale, title, briefing, user_opening_instruction, conversation_goal, status, tts_voice_id, created_at, updated_at
)
SELECT
        20,
        20,
        'EN',
        'KR',
        '친구와 여행 수다 떨기',
        '친구 Chloe와 카페에서 그동안의 여행 이야기를 나눈다.',
        NULL,
        'Chloe와 여행 경험과 앞으로의 버킷리스트를 자유롭게 이야기하기',
        'ACTIVE',
        1,
        '2026-07-07T00:00:00',
        '2026-07-07T00:00:00'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_language_variant WHERE id = 20
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        1,
        1,
        1,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 1
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        2,
        1,
        2,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 2
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        3,
        1,
        3,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 3
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        4,
        2,
        1,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 4
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        5,
        2,
        2,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 5
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        6,
        2,
        3,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 6
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        7,
        3,
        1,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 7
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        8,
        3,
        2,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 8
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        9,
        3,
        3,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 9
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        10,
        4,
        1,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 10
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        11,
        4,
        2,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 11
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        12,
        4,
        3,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 12
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        13,
        5,
        1,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 13
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        14,
        5,
        2,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 14
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        15,
        5,
        3,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 15
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        16,
        6,
        1,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 16
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        17,
        6,
        2,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 17
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        18,
        6,
        3,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 18
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        19,
        7,
        1,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 19
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        20,
        7,
        2,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 20
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        21,
        7,
        3,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 21
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        22,
        8,
        1,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 22
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        23,
        8,
        2,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 23
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        24,
        8,
        3,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 24
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        25,
        9,
        1,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 25
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        26,
        9,
        2,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 26
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        27,
        9,
        3,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 27
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        28,
        10,
        1,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 28
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        29,
        10,
        2,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 29
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        30,
        10,
        3,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 30
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        31,
        11,
        1,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 31
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        32,
        11,
        2,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 32
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        33,
        11,
        3,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 33
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        34,
        12,
        1,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 34
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        35,
        12,
        2,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 35
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        36,
        12,
        3,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 36
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        37,
        13,
        1,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 37
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        38,
        13,
        2,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 38
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        39,
        13,
        3,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 39
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        40,
        14,
        1,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 40
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        41,
        14,
        2,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 41
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        42,
        14,
        3,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 42
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        43,
        15,
        1,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 43
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        44,
        15,
        2,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 44
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        45,
        15,
        3,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 45
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        46,
        16,
        1,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 46
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        47,
        16,
        2,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 47
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        48,
        16,
        3,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 48
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        49,
        17,
        1,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 49
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        50,
        17,
        2,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 50
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        51,
        17,
        3,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 51
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        52,
        18,
        1,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 52
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        53,
        18,
        2,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 53
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        54,
        18,
        3,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 54
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        55,
        19,
        1,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 55
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        56,
        19,
        2,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 56
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        57,
        19,
        3,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 57
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        58,
        20,
        1,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 58
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        59,
        20,
        2,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 59
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
SELECT
        60,
        20,
        3,
        'ACTIVE',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question WHERE id = 60
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        1,
        1,
        'EN',
        'KR',
        'Hey, you''re my roommate, right?! I''m Marco, nice to meet you! What''s your name? Tell me a little about yourself!',
        '안녕 너 내 룸메이트지?! 난 Marco야. 만나서 반가워. 넌 이름이 뭐야? 너에 대해 소개해주라.',
        'ACTIVE',
        '드디어 룸메이트를 만났다! 좋은 친구가 되면 좋겠는데, 어떤 애일지 궁금하다.',
        'GOOD',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T14:11:15.12539'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 1
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        2,
        2,
        'EN',
        'KR',
        'What are you into? What do you love about it?',
        '취미는 뭐야? 그게 어떤 매력이 있어?',
        'ACTIVE',
        NULL,
        NULL,
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 2
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        3,
        3,
        'EN',
        'KR',
        'I''m obsessed with Korea! Tell me your must-visit spots and why I should go!',
        '나 한국 엄청 좋아하는데, 추천할 만한 관광지와 그 이유를 알려줘!',
        'ACTIVE',
        NULL,
        NULL,
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 3
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        4,
        4,
        'EN',
        'KR',
        'Okay, before this place turns into a disaster — how do you usually like to split cleaning stuff? I''m curious what worked for you before.',
        '자, 좋아. 방 난장판 되기 전에 — 청소 같은 거 보통 어떻게 나누는 거 좋아해? 전에는 어떻게 했었는지 궁금해.',
        'ACTIVE',
        '같이 살 사람이랑 이런 얘기 미리 해두는 게 편한데, 너무 깐깐해 보이진 않았으면 좋겠다.',
        'NORMAL',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T14:11:15.12539'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 4
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        5,
        5,
        'EN',
        'KR',
        'I''m such a night owl, it''s a problem. What''s your whole daily rhythm like — when are you up, when do you crash?',
        '나 완전 야행성이라 큰일이야. 넌 하루 리듬이 어때? 언제 일어나고 언제 뻗어?',
        'ACTIVE',
        NULL,
        NULL,
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 5
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        6,
        6,
        'EN',
        'KR',
        'Have you ever had a roommate situation that drove you crazy? I wanna know what your dealbreakers are so I don''t accidentally become that person.',
        '전에 룸메 때문에 진짜 미칠 것 같았던 적 있어? 네가 뭘 못 참는지 알고 싶어, 나도 모르게 그런 룸메 되기 싫어서.',
        'ACTIVE',
        NULL,
        NULL,
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 6
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        7,
        7,
        'EN',
        'KR',
        'We should totally hang out this weekend! Does Saturday or Sunday work better for you?',
        '우리 이번 주말에 꼭 놀자! 토요일이랑 일요일 중에 언제가 더 좋아?',
        'ACTIVE',
        '이번 주말에 같이 놀면 재밌겠다 — 얘가 뭐 하고 싶어할지 궁금하네.',
        'GOOD',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T14:11:15.12539'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 7
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        8,
        8,
        'EN',
        'KR',
        'So what do you usually do for fun? Or is there anything you''ve been dying to try ever since you got here?',
        '넌 보통 뭐하고 놀아? 아님 여기 와서 꼭 해보고 싶었던 거 있어?',
        'ACTIVE',
        NULL,
        NULL,
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 8
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        9,
        9,
        'EN',
        'KR',
        'Are you more of a go-explore-the-city person or a stay-in-and-chill person? What actually sounds fun to you?',
        '넌 도시 탐험파야, 집콕파야? 넌 뭐가 진짜 재밌을 것 같아?',
        'ACTIVE',
        NULL,
        NULL,
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 9
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        10,
        10,
        'EN',
        'KR',
        'So just to sort this out, I''ll need some proof that you were traveling during July — do you have anything like that? Let me know what you''ve got.',
        '이거 처리하려면 7월에 여행 다니셨다는 증빙 자료가 좀 필요해요 — 그런 거 갖고 계세요? 어떤 게 있는지 알려주세요.',
        'ACTIVE',
        NULL,
        NULL,
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 10
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        11,
        11,
        'EN',
        'KR',
        'The thing is, this month''s charge already came out of your account. Would you rather get a cash refund, or have it taken off next month''s bill?',
        '근데 이번 달 요금이 이미 계좌에서 빠져나갔거든요. 현금으로 환불받으시겠어요, 아니면 다음 달 요금에서 차감해 드릴까요?',
        'ACTIVE',
        NULL,
        NULL,
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 11
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        12,
        12,
        'EN',
        'KR',
        'It''ll take about two weeks to look into it and get it sorted — is that okay with you? Anything else you wanted to ask?',
        '확인하고 처리하는 데 2주 정도 걸릴 것 같은데 — 괜찮으세요? 더 물어보실 거 있으세요?',
        'ACTIVE',
        NULL,
        NULL,
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 12
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        13,
        13,
        'EN',
        'KR',
        'Sometimes I like having a friend or two over — how do you feel about guests in the room? I wanna get on the same page early.',
        '나 가끔 친구 한두 명 부르는 거 좋아하는데 — 넌 방에 손님 오는 거 어떻게 생각해? 미리 맞춰두고 싶어서.',
        'ACTIVE',
        '이런 거 미리 확실히 안 해두면 나중에 서운해질 수도 있으니까 지금 짚고 넘어가야지.',
        'NORMAL',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T14:11:15.12539'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 13
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        14,
        14,
        'EN',
        'KR',
        'I always study with music on — what kind of setup helps you focus? Dead silence, some background noise, music?',
        '난 항상 음악 틀고 공부하는데 — 넌 뭐가 있어야 집중돼? 완전 조용, 약간의 소음, 음악?',
        'ACTIVE',
        NULL,
        NULL,
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 14
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        15,
        15,
        'EN',
        'KR',
        'Where do you draw the line on sharing stuff? Like, if I run out of milk, can I just grab yours, or would you rather I always ask first?',
        '넌 물건 공유 어디까지 오케이야? 이를테면 나 우유 떨어지면 네 거 그냥 써도 돼, 아니면 매번 물어보는 게 나아?',
        'ACTIVE',
        NULL,
        NULL,
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 15
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        16,
        16,
        'EN',
        'KR',
        'Hey, hi! Nice to meet you! I don''t think we''ve met — how long have you been here? Have you made a lot of friends yet?',
        '안녕, 반가워! 우리 처음 보는 것 같은데 — 여기 온 지 얼마나 됐어? 친구는 많이 사귀었어?',
        'ACTIVE',
        '새로운 사람 만나는 거 항상 재밌어 — 이 친구랑도 잘 통했으면 좋겠다.',
        'GOOD',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T14:11:15.12539'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 16
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        17,
        17,
        'EN',
        'KR',
        'Are you into parties and stuff usually? Do people back in Korea throw parties like this too?',
        '넌 평소에 파티 같은 거 좋아해? 한국에서도 이런 파티 많이 해?',
        'ACTIVE',
        NULL,
        NULL,
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 17
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        18,
        18,
        'EN',
        'KR',
        'Oh, my friend''s having a going-away party soon — you should come! It''s potluck though, so everyone brings a dish. If you come, what would you make?',
        '아, 내 친구 곧 송별 파티 하는데 — 너도 와! 근데 포틀럭이라 각자 음식 하나씩 해와야 돼. 오면 넌 뭐 만들어 올 거야?',
        'ACTIVE',
        NULL,
        NULL,
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 18
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        19,
        19,
        'EN',
        'KR',
        'Do you have any brothers or sisters? Being an only child, I sometimes felt a little lonely growing up — I always wondered if having siblings feels different. How was it for you?',
        '너 형제자매 있어? 난 외동이라 크면서 가끔 좀 외롭더라고 — 형제자매 있으면 다를까 늘 궁금했어. 넌 어땠어?',
        'ACTIVE',
        '이제 좀 친해진 것 같은데, 더 깊은 얘기도 나눌 수 있을까?',
        'NORMAL',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T14:11:15.12539'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 19
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        20,
        20,
        'EN',
        'KR',
        'I''m curious about you — what''s your big dream? And what made you pick your major?',
        '나 궁금한 거 있어 — 너는 꿈이 뭐야? 그리고 왜 그 전공을 선택했어?',
        'ACTIVE',
        NULL,
        NULL,
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 20
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        21,
        21,
        'EN',
        'KR',
        'You''ve seemed kinda off lately — everything okay? You know you can talk to me, right? Come on, tell me everything!',
        '너 요즘 좀 기운없어 보여 — 다 괜찮아? 나한테 얘기해도 되는 거 알지? 자, 나한테 다 털어놔봐!',
        'ACTIVE',
        NULL,
        NULL,
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 21
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        22,
        22,
        'EN',
        'KR',
        'Hey, I''m Marco. Is this seat taken? Do you mind if I sit here?',
        '안녕. 나 Marco이라고 해. 여기 자리 있어? 나 여기 앉아도 돼?',
        'ACTIVE',
        '이 수업 아는 사람이 없어서 좀 긴장되네 — 옆에 앉는 사람이랑 친해지면 좋겠다.',
        'NORMAL',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T14:11:15.12539'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 22
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        23,
        23,
        'EN',
        'KR',
        'You''re Korean, right? Do you know anyone else in this class? I''m taking it alone and I''m kinda worried — you know, about assignments and group projects and stuff.',
        '너 한국인 맞지? 이 수업 같이 듣는 친구 있어? 난 혼자 듣게 돼서 걱정이 좀 많거든. 과제라든지 조별 과제 같은 거 때문에.',
        'ACTIVE',
        NULL,
        NULL,
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 23
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        24,
        24,
        'EN',
        'KR',
        'What was your school back in Korea like? Was there a class you actually really enjoyed?',
        '너 한국에서 다니던 학교는 어땠어? 재밌게 들었던 수업 있어?',
        'ACTIVE',
        NULL,
        NULL,
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 24
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        25,
        25,
        'EN',
        'KR',
        'Ugh, we have to present next week — I''m so nervous. Have you ever presented in English back in Korea? Or taken any classes in English?',
        '아 우리 다음 주에 발표해야 되잖아. 너무 긴장된다. 너 한국에서 영어로 발표 해본 적 있어? 아니면 영어 수업을 들어봤다든지.',
        'ACTIVE',
        '발표 진짜 떨리는데 팀원이랑 잘 맞춰서 준비하면 좀 나아지겠지.',
        'NORMAL',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T14:11:15.12539'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 25
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        26,
        26,
        'EN',
        'KR',
        'We should split up the parts — what are you most confident about, presenting or research? Or making the slides? I''ll let you pick first.',
        '우리 파트 나눠야 되는데, 넌 뭐 제일 자신 있어? 발표, 자료조사, 아님 PPT 만들기? 네가 먼저 골라.',
        'ACTIVE',
        NULL,
        NULL,
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 26
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        27,
        27,
        'EN',
        'KR',
        'What''s the hardest part of presenting in English for you? I feel like everyone struggles with something different.',
        '넌 영어로 발표할 때 제일 힘든 게 뭐야? 다들 힘든 포인트가 다른 것 같더라고.',
        'ACTIVE',
        NULL,
        NULL,
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 27
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        28,
        28,
        'EN',
        'KR',
        'I can''t show you the grading rubric directly, but it may have been marked down for a late submission, or because parts of it read as AI-generated — or possibly because the argument wasn''t fully developed. Does any of that ring a bell?',
        '채점 기준표를 직접 보여줄 순 없지만, 제출 기한을 넘겼거나, AI를 쓴 티가 나거나, 아니면 논지가 충분히 전개되지 않아서 감점됐을 수 있어요. 짐작 가는 이유가 있나요?',
        'ACTIVE',
        NULL,
        NULL,
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 28
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        29,
        29,
        'EN',
        'KR',
        'How about this — I''ll reread your assignment later, write up what was lacking, and get back to you tomorrow. Could you drop by my office tomorrow, or would email work better for you?',
        '그럼 이렇게 하죠 — 제가 이따가 학생 과제를 다시 읽어보고 부족했던 점을 정리해서 내일 알려줄게요. 내일 오피스로 올 수 있어요, 아니면 이메일이 더 편해요?',
        'ACTIVE',
        NULL,
        NULL,
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 29
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        30,
        30,
        'EN',
        'KR',
        'You''re doing just fine, really. Come by anytime if anything''s tough or you''ve got questions — my door''s always open.',
        '지금도 잘하고 있어요, 정말로. 앞으로도 힘든 점이나 궁금한 거 있으면 언제든 찾아와요.',
        'ACTIVE',
        NULL,
        NULL,
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 30
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        31,
        31,
        'EN',
        'KR',
        'Ugh, finals are next week already — where does the time even go? Are you a cram-the-night-before type, or do you actually study ahead?',
        '하 다음 주가 시험이라니 시간이 왜 이렇게 빠르지? 너는 벼락치기 타입이야, 아님 미리미리 해두는 타입이야?',
        'ACTIVE',
        '시험 기간만 되면 늘 마음이 붕 뜨는 기분이야 — 같이 공부하면 좀 나을 것 같은데.',
        'NORMAL',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T14:11:15.12539'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 31
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        32,
        32,
        'EN',
        'KR',
        'What did you do when you bombed a test and knew your parents would kill you? Me, I just straight up never showed them my report card, lol.',
        '시험 망쳐서 부모님한테 혼날 것 같으면 어떻게 했어? 난 그냥 성적표 절대 안 보여줬어 ㅋㅋ.',
        'ACTIVE',
        NULL,
        NULL,
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 32
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        33,
        33,
        'EN',
        'KR',
        'Where do you usually study — library, home, a café? I kinda wanna study together — are you free sometime?',
        '너 도서관에서 공부하는 편이야, 아님 집, 카페? 너랑 같이 공부하고 싶은데 시간 괜찮아?',
        'ACTIVE',
        NULL,
        NULL,
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 33
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        34,
        34,
        'EN',
        'KR',
        'Okay, today''s debate is whether money can actually buy happiness. Where do you stand — can it, or is that a myth? And why do you think so?',
        '자, 오늘 토론 주제가 돈으로 행복을 살 수 있는가잖아. 넌 어느 쪽이야 — 살 수 있다고 봐, 아님 그냥 환상이라고 봐? 왜 그렇게 생각해?',
        'ACTIVE',
        '오늘 주제 흥미로운데, 다른 사람들은 어떻게 생각하는지 궁금하다.',
        'NORMAL',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T14:11:15.12539'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 34
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        35,
        35,
        'EN',
        'KR',
        'Think about the last time buying something actually made you happy — what was it, and did that feeling last? I''m curious if it really works.',
        '마지막으로 뭔가 사서 진짜 행복했던 때 떠올려봐 — 뭐였고, 그 기분이 오래갔어? 그게 진짜 효과가 있는 건지 궁금해서.',
        'ACTIVE',
        NULL,
        NULL,
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 35
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        36,
        36,
        'EN',
        'KR',
        'Okay, imagine you suddenly won a million dollars tomorrow — do you think you''d actually be happier a year later, or would you just get used to it? What would really change?',
        '자, 내일 갑자기 백만 달러가 생겼다고 쳐 — 1년 뒤에 진짜 더 행복할 것 같아, 아님 그냥 익숙해질 것 같아? 진짜로 뭐가 달라질까?',
        'ACTIVE',
        NULL,
        NULL,
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 36
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        37,
        37,
        'EN',
        'KR',
        'Looks like we''re gonna be sitting together for a while — so where are you headed, and what''s taking you there?',
        '안녕하세요! 우리 한동안 같이 앉아 가겠네요 — 어디 가는 길이에요, 무슨 일로 가요?',
        'ACTIVE',
        '장거리 비행이라 심심했는데, 옆자리 사람이랑 얘기하면 시간 잘 갈 것 같다.',
        'GOOD',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T14:11:15.12539'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 37
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        38,
        38,
        'EN',
        'KR',
        'I''m always trying to figure out my next trip — when you travel, are you more into big cities or getting out into nature? What''s your usual vibe?',
        '난 여행을 좋아해서, 늘 다음 여행 궁리 중이거든요 — 여행할 때 대도시파예요, 자연으로 나가는 파예요? 보통 어떤 스타일이에요?',
        'ACTIVE',
        NULL,
        NULL,
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 38
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        39,
        39,
        'EN',
        'KR',
        'Okay, best trip you''ve ever taken — where was it, and what made it so special?',
        '여태 다녀온 여행 중 최고는 어디였고, 뭐가 그렇게 특별했어요?',
        'ACTIVE',
        NULL,
        NULL,
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 39
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        40,
        40,
        'EN',
        'KR',
        'We offer a few types of compensation — a cash refund, mileage points, or a travel voucher for your next flight. Which one would work best for you?',
        '저희가 제공해 드리는 보상 몇 가지가 있습니다 — 현금 환불, 마일리지 포인트, 아니면 다음 비행에 쓰실 수 있는 여행 바우처요. 어떤 게 제일 나으실까요?',
        'ACTIVE',
        NULL,
        NULL,
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 40
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        41,
        41,
        'EN',
        'KR',
        'Could I get your name, nationality, and a contact number? I''ll follow up with the full details of the process.',
        '성함과 국적, 연락처를 알려주시겠어요? 자세한 처리 과정은 제가 다시 안내해 드릴게요.',
        'ACTIVE',
        NULL,
        NULL,
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 41
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        42,
        42,
        'EN',
        'KR',
        'Is there anything else I can help you with?',
        '제가 더 도와드릴 일 있으신가요?',
        'ACTIVE',
        NULL,
        NULL,
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 42
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        43,
        43,
        'EN',
        'KR',
        'Welcome! Will you be checking in with us today? May I have your name and how you booked your stay?',
        '어서 오세요! 오늘 체크인 도와드릴까요? 성함과 예약하신 방법을 알려주시겠어요?',
        'ACTIVE',
        '오늘 손님도 편하게 체크인할 수 있게 잘 안내해드려야지.',
        'NORMAL',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T14:11:15.12539'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 43
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        44,
        44,
        'EN',
        'KR',
        'You''re all set. Do you have any preferences for your room — a higher floor, a quieter side? I''ll do my best to arrange it.',
        '다 됐습니다. 방 선호하시는 거 있으신가요? 높은 층, 조용한 쪽, 제가 최대한 맞춰드리겠습니다.',
        'ACTIVE',
        NULL,
        NULL,
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 44
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        45,
        45,
        'EN',
        'KR',
        'Here''s your key card. Is there anything else you''d like to know?',
        '카드키 드리겠습니다. 더 궁금한 사항 있으신가요?',
        'ACTIVE',
        NULL,
        NULL,
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 45
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        46,
        46,
        'EN',
        'KR',
        'Hi! Sorry to just come up like this — I saw you and thought I''d regret it if I didn''t say hi. You''re totally my type. Are you free tonight, by any chance? Would you like to grab a coffee with me?',
        '안녕! 이렇게 갑자기 다가와서 미안. 너 보고 인사 안 하면 후회할 것 같아서. 너무 내 스타일이거든. 혹시 오늘 저녁에 시간 돼? 같이 커피라도 한잔할 수 있을까?',
        'ACTIVE',
        '오늘따라 왠지 말 걸고 싶은 사람이 보이네 — 용기 내서 한번 다가가볼까.',
        'NORMAL',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T14:11:15.12539'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 46
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        47,
        47,
        'EN',
        'KR',
        'Oh… can I ask why? Is it something I did, or…?',
        '아… 이유 물어봐도 돼? 내가 뭐 잘못했나, 아니면…?',
        'ACTIVE',
        NULL,
        NULL,
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 47
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        48,
        48,
        'EN',
        'KR',
        'Alright, I understand. Could I at least get your Instagram, then?',
        '알겠어. 그럼 인스타그램이라도 받을 수 있을까?',
        'ACTIVE',
        NULL,
        NULL,
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 48
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        49,
        49,
        'EN',
        'KR',
        'Hi there! What can I get started for you today?',
        '안녕하세요! 오늘 뭐로 준비해 드릴까요?',
        'ACTIVE',
        '오늘 손님은 뭐 주문할지 궁금하네, 친절하게 안내해드려야지.',
        'NORMAL',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T14:11:15.12539'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 49
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        50,
        50,
        'EN',
        'KR',
        'Good choice! Is that for here or to go? And anything to go with it?',
        '좋은 선택이에요! 여기서 드시나요 아니면 포장인가요? 곁들일 것도 원하시나요?',
        'ACTIVE',
        NULL,
        NULL,
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 50
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        51,
        51,
        'EN',
        'KR',
        'We''ve got a couple of specials today — want me to walk you through them, or do you already know what you''re after?',
        '오늘 스페셜 메뉴가 몇 개 있는데 — 설명해 드릴까요, 아님 이미 뭐 드실지 정하셨어요?',
        'ACTIVE',
        NULL,
        NULL,
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 51
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        52,
        52,
        'EN',
        'KR',
        'Hi, what can I help you with today? You''re not looking so great — what''s been going on?',
        '안녕하세요, 뭘 도와드릴까요? 안색이 안 좋아 보이는데 — 어디가 안 좋으세요?',
        'ACTIVE',
        '안색이 안 좋아 보이는데 어디가 안 좋은지 잘 들어봐야겠다.',
        'NORMAL',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T14:11:15.12539'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 52
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        53,
        53,
        'EN',
        'KR',
        'Okay, and how long have you been feeling like this? Any fever, or is it more of an achy, tired kind of thing?',
        '언제부터 그랬어요? 열도 있어요, 아니면 그냥 몸살처럼 쑤시고 피곤한 거예요?',
        'ACTIVE',
        NULL,
        NULL,
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 53
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        54,
        54,
        'EN',
        'KR',
        'I''ll give you something for that. Are you allergic to anything, or taking any other medication right now?',
        '그거에 맞는 약 드릴게요. 알레르기 있는 거나 지금 먹고 있는 다른 약 있어요?',
        'ACTIVE',
        NULL,
        NULL,
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 54
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        55,
        55,
        'EN',
        'KR',
        'Is this your first time in London? How are you finding it so far?',
        '런던 처음이에요? 여행 다녀보니 어때요?',
        'ACTIVE',
        NULL,
        NULL,
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 55
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        56,
        56,
        'EN',
        'KR',
        'Oh, if you haven''t had dinner yet, I can point you to some great spots nearby — what kind of food are you into?',
        '아, 아직 저녁 안 드셨으면 근처 맛집 추천해 드릴게요 — 어떤 음식 좋아해요?',
        'ACTIVE',
        NULL,
        NULL,
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 56
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        57,
        57,
        'EN',
        'KR',
        'Let me give you my number — if you have any questions while you''re in London, just text me!',
        '제 연락처 알려드릴게요 — 런던 계시는 동안 궁금한 거 있으면 연락해요!',
        'ACTIVE',
        NULL,
        NULL,
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 57
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        58,
        58,
        'EN',
        'KR',
        'Okay I''m dying to know — of everywhere you''ve ever been, what''s the one place that just blew you away? And why that one?',
        '나 진짜 궁금한 거 있어 — 여태 가본 데 중에 딱 하나, 완전 반해버린 곳 어디야? 왜 하필 거기야?',
        'ACTIVE',
        '여행 얘기는 언제 들어도 재밌어 — 이번엔 또 어떤 얘기가 나올까.',
        'GOOD',
        '2026-07-12T12:49:25.059973',
        '2026-07-12T14:11:15.12539'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 58
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        59,
        59,
        'EN',
        'KR',
        'What about a trip that went totally wrong? Those are always the funniest to hear about later.',
        '완전 망한 여행은 없었어? 그런 게 나중에 들으면 제일 웃기잖아.',
        'ACTIVE',
        NULL,
        NULL,
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 59
);
INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, inner_thought, inner_thought_type, created_at, updated_at
)
SELECT
        60,
        60,
        'EN',
        'KR',
        'If money and time didn''t matter at all — where would you go next, and what would you do there?',
        '돈이랑 시간이 무제한이면 — 다음에 어디 갈 거야, 거기서 뭐 할 거야?',
        'ACTIVE',
        NULL,
        NULL,
        '2026-07-12T12:49:25.059973',
        '2026-07-12T12:49:25.059973'
WHERE NOT EXISTS (
    SELECT 1 FROM scenario_question_language_variant WHERE id = 60
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        1,
        1,
        'SCENARIO',
        'EMOTION_EMPATHY',
        'BASIC',
        'EN',
        'KR',
        1,
        'There is nothing like',
        '~만 한 게 없다',
        '최고라고 강조하는 There is nothing like',
        '''~만 한 게 없다'', ''~이 최고다''라고 강조할 때 쓰는 표현입니다. 직역하면 ''~같은 것은 없다''인데, 그만큼 독보적으로 좋다는 뜻이에요.',
        'What do you love about hiking?',
        '하이킹의 어떤 점이 좋아?',
        'There''s nothing like hiking to clear my head.',
        '머리 식히는 데는 하이킹만 한 게 없어.',
        ARRAY['There''s', 'nothing', 'like', 'hiking', 'to', 'clear', 'my', 'head'],
        ARRAY['my', 'hiking', 'like', 'head', 'anything', 'There''s', 'his', 'nothing', 'to', 'cleared', 'clear'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/1/expressions/1/practice-examples/25e16edf-e702-4141-b303-b9bc1a73b891.png","sentenceText":"There''s nothing like a cold beer on a hot day.","sentenceWords":["There''s","nothing","like","a","cold","beer","on","a","hot","day"],"highlightingPart":"There''s nothing like","practiceQuestion":"Want a cold beer? It''s so hot.","sentenceTranslation":"더운 날엔 시원한 맥주만 한 게 없지.","sentenceWordChoices":["beats","a","cold","nothing","better","beer","than","day","like","There''s","hot","a","on"],"practiceQuestionTranslation":"시원한 맥주 마실래? 너무 덥다."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/1/expressions/1/practice-examples/d72773a0-98d3-4781-9c1a-65ff7b3e1e15.png","sentenceText":"There''s nothing like mom''s cooking.","sentenceWords":["There''s","nothing","like","mom''s","cooking"],"highlightingPart":"There''s nothing like","practiceQuestion":"What do you miss most about being away from home?","sentenceTranslation":"엄마 밥만 한 게 없어.","sentenceWordChoices":["than","like","nothing","There''s","beats","cooking","mom''s","better"],"practiceQuestionTranslation":"집 떠나 있으니까 뭐가 제일 그리워?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/1/expressions/1/practice-examples/7b80223d-4373-413e-8675-68d809e60e14.png","sentenceText":"There''s nothing like a good night''s sleep.","sentenceWords":["There''s","nothing","like","a","good","night''s","sleep"],"highlightingPart":"There''s nothing like","practiceQuestion":"I''m so exhausted today.","sentenceTranslation":"푹 자는 것만큼 좋은 게 없어.","sentenceWordChoices":["a","good","than","like","night''s","better","beats","sleep","There''s","nothing"],"practiceQuestionTranslation":"나 오늘 완전 피곤해."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/1/expressions/1/practice-examples/84a42e0b-cad0-484c-b71b-028a44fdb9b0.png","sentenceText":"There''s nothing like traveling with friends.","sentenceWords":["There''s","nothing","like","traveling","with","friends"],"highlightingPart":"There''s nothing like","practiceQuestion":"What''s your favorite way to spend a vacation?","sentenceTranslation":"친구들이랑 여행하는 것만 한 게 없지.","sentenceWordChoices":["traveling","There''s","beats","nothing","better","than","like","friends","with"],"practiceQuestionTranslation":"휴가 때 뭐 하는 게 제일 좋아?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 1
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        2,
        1,
        'SCENARIO',
        'EMOTION_EMPATHY',
        'BASIC',
        'EN',
        'KR',
        2,
        'blow my mind',
        '끝내주게 놀랍다',
        '감탄을 표현하는 blow my mind',
        '''정신이 날아갈 만큼 놀랍다'', 즉 ''끝내준다'', ''충격적으로 대단하다''는 뜻입니다. 경치, 공연, 반전 등 강렬한 인상을 받았을 때 최고의 리액션이에요.',
        'What should I definitely see in Korea?',
        '한국에서 뭘 꼭 봐야 해?',
        'Gyeongbokgung Palace will blow your mind.',
        '경복궁은 널 완전 놀라게 할 거야.',
        ARRAY['Gyeongbokgung', 'Palace', 'will', 'blow', 'your', 'mind'],
        ARRAY['Gyeongbokgung', 'mind', 'would', 'Palace', 'blow', 'will', 'brain', 'your', 'blew'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/1/expressions/2/practice-examples/f0073aae-6782-403b-8098-d50d932e891e.png","sentenceText":"Her voice blows my mind every time.","sentenceWords":["Her","voice","blows","my","mind","every","time"],"highlightingPart":"blows my mind","practiceQuestion":"What do you think of her singing?","sentenceTranslation":"그녀 목소리는 들을 때마다 소름 돋아.","sentenceWordChoices":["amazing","blows","every","voice","my","shocked","brain","Her","mind","time"],"practiceQuestionTranslation":"걔 노래 어때?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/1/expressions/2/practice-examples/c51f1018-f533-484d-984a-f7839a2826c9.png","sentenceText":"The special effects blew my mind.","sentenceWords":["The","special","effects","blew","my","mind"],"highlightingPart":"blew my mind","practiceQuestion":"How was the movie?","sentenceTranslation":"특수효과가 끝내줬어.","sentenceWordChoices":["mind","effects","amazing","brain","special","The","blew","shocked","my"],"practiceQuestionTranslation":"영화 어땠어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/1/expressions/2/practice-examples/ac3ffcdd-12ae-4b6b-bc84-bb6b5971ee90.png","sentenceText":"This fact will blow your mind.","sentenceWords":["This","fact","will","blow","your","mind"],"highlightingPart":"blow your mind","practiceQuestion":"Want to hear a crazy fact?","sentenceTranslation":"이 사실 들으면 깜짝 놀랄걸.","sentenceWordChoices":["This","mind","amazing","blow","your","brain","fact","will","shocked"],"practiceQuestionTranslation":"놀라운 사실 하나 듣고 싶어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/1/expressions/2/practice-examples/9f9aa865-e9e8-43b7-9862-743612d0df57.png","sentenceText":"The concert was mind-blowing.","sentenceWords":["The","concert","was","mind-blowing"],"highlightingPart":"mind-blowing","practiceQuestion":"How was the concert last night?","sentenceTranslation":"콘서트가 진짜 압도적이었어.","sentenceWordChoices":["shocked","mind-blowing","was","brain","concert","The","amazing"],"practiceQuestionTranslation":"어젯밤 콘서트 어땠어?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 2
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        3,
        1,
        'SCENARIO',
        'RELATIONSHIP_SOCIAL',
        'BASIC',
        'EN',
        'KR',
        3,
        'grow up',
        '자라다, 성장하다',
        '성장 배경을 묻는 grow up',
        '''자라다, 성장하다''라는 뜻으로, 어린 시절과 성장 배경을 이야기할 때 필수인 구동사입니다. Where did you grow up?은 첫 만남 스몰토크 단골 질문이에요.',
        'So, where did you grow up?',
        '그래서 넌 어디서 자랐어?',
        'I grew up in Busan, near the beach.',
        '난 바닷가 근처 부산에서 자랐어.',
        ARRAY['I', 'grew', 'up', 'in', 'Busan', 'near', 'the', 'beach'],
        ARRAY['at', 'grow', 'up', 'in', 'beach', 'the', 'near', 'grew', 'on', 'I', 'Busan'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/1/expressions/3/practice-examples/0fe9e360-e257-4692-92a0-c2ef978bc9a0.png","sentenceText":"She grew up speaking two languages.","sentenceWords":["She","grew","up","speaking","two","languages"],"highlightingPart":"grew up","practiceQuestion":"Is she bilingual?","sentenceTranslation":"걔는 두 개 언어를 쓰면서 자랐어.","sentenceWordChoices":["grown","speaking","languages","up","older","grew","She","raised","two"],"practiceQuestionTranslation":"걔 이중언어 하는 애야?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/1/expressions/3/practice-examples/d649360c-7938-40e3-baaa-688174af2f3a.png","sentenceText":"What do you want to be when you grow up?","sentenceWords":["What","do","you","want","to","be","when","you","grow","up"],"highlightingPart":"grow up","practiceQuestion":"You''re getting so big!","sentenceTranslation":"커서 뭐가 되고 싶어?","sentenceWordChoices":["grown","you","What","do","you","up","to","be","want","older","when","grow","raised"],"practiceQuestionTranslation":"너 진짜 많이 컸다!"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/1/expressions/3/practice-examples/98007d3b-d301-4201-8bb1-39d43dded18c.png","sentenceText":"We grew up together.","sentenceWords":["We","grew","up","together"],"highlightingPart":"grew up","practiceQuestion":"How do you guys know each other?","sentenceTranslation":"우리는 같이 자란 사이야.","sentenceWordChoices":["raised","grew","up","grown","together","We","older"],"practiceQuestionTranslation":"너네 어떻게 알게 된 사이야?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/1/expressions/3/practice-examples/0eaedbbd-a2df-4e63-a6d9-ff603d025d51.png","sentenceText":"He grew up watching baseball with his dad.","sentenceWords":["He","grew","up","watching","baseball","with","his","dad"],"highlightingPart":"grew up","practiceQuestion":"Were he and his dad close?","sentenceTranslation":"걔는 아빠랑 야구 보면서 컸어.","sentenceWordChoices":["with","baseball","older","dad","his","grown","up","raised","watching","He","grew"],"practiceQuestionTranslation":"걔랑 아빠랑 친했어?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 3
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        4,
        1,
        'SCENARIO',
        'TRAVEL_MOVEMENT',
        'BASIC',
        'EN',
        'KR',
        4,
        'check out',
        '살펴보다; 체크아웃하다',
        '둘러보고 확인하는 check out',
        '''살펴보다, 확인해 보다''라는 뜻과 호텔에서 ''체크아웃하다''라는 뜻을 모두 가진 구동사입니다. Check it out!(이것 좀 봐!)은 추천 멘트의 대명사예요.',
        'Any must-visit spots you''d recommend?',
        '추천해 줄 꼭 가볼 곳 있어?',
        'You should check out Bukchon Hanok Village.',
        '북촌 한옥마을 한번 가봐.',
        ARRAY['You', 'should', 'check', 'out', 'Bukchon', 'Hanok', 'Village'],
        ARRAY['You', 'Bukchon', 'out', 'would', 'checked', 'in', 'Hanok', 'Village', 'should', 'check'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/1/expressions/4/practice-examples/8c25059b-9b96-48be-b481-9c2b3e074d25.png","sentenceText":"Check out this video!","sentenceWords":["Check","out","this","video"],"highlightingPart":"Check out","practiceQuestion":"You have to see this.","sentenceTranslation":"이 영상 좀 봐!","sentenceWordChoices":["see","out","video","look","Check","this","browse"],"practiceQuestionTranslation":"이거 꼭 봐야 해."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/1/expressions/4/practice-examples/c137d199-4cb4-4148-ab5f-12a390b5a366.png","sentenceText":"We need to check out by 11.","sentenceWords":["We","need","to","check","out","by","11"],"highlightingPart":"check out","practiceQuestion":"What time do we leave the hotel?","sentenceTranslation":"11시까지 체크아웃해야 해.","sentenceWordChoices":["out","need","11","see","by","to","look","check","We","browse"],"practiceQuestionTranslation":"호텔에서 몇 시에 나가야 해?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/1/expressions/4/practice-examples/c5e27b1f-4eef-490f-98e2-6cf51d3514b3.png","sentenceText":"I checked out a few stores, but nothing caught my eye.","sentenceWords":["I","checked","out","a","few","stores","but","nothing","caught","my","eye"],"highlightingPart":"checked out","practiceQuestion":"Did you find anything at the mall?","sentenceTranslation":"가게 몇 군데 둘러봤는데 눈에 띄는 게 없더라.","sentenceWordChoices":["I","a","nothing","stores","few","but","browse","out","see","caught","look","eye","my","checked"],"practiceQuestionTranslation":"몰에서 뭐 좀 건졌어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/1/expressions/4/practice-examples/ad27d94e-d736-4d7d-b25a-f6c3623000d8.png","sentenceText":"Check out the view from here!","sentenceWords":["Check","out","the","view","from","here"],"highlightingPart":"Check out","practiceQuestion":"Wow, what a spot!","sentenceTranslation":"여기서 보이는 경치 좀 봐!","sentenceWordChoices":["here","browse","view","Check","from","look","the","out","see"],"practiceQuestionTranslation":"와, 여기 자리 좋다!"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 4
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        5,
        1,
        'SCENARIO',
        'OPINION_JUDGMENT',
        'BASIC',
        'EN',
        'KR',
        5,
        'be into',
        '~에 푹 빠져 있다, ~을 정말 좋아하다',
        '푹 빠져 있다고 말하는 be into',
        '''~에 푹 빠져 있다'', ''~을 정말 좋아하다''라는 뜻의 캐주얼한 표현입니다. 그냥 like보다 훨씬 열정적으로 좋아한다는 뉘앙스로, into 뒤에 명사나 동명사를 붙여요.',
        'What are you into these days?',
        '너 요즘 뭐에 빠져 있어?',
        'I''m really into hiking these days.',
        '나 요즘 하이킹에 푹 빠져 있어.',
        ARRAY['I''m', 'really', 'into', 'hiking', 'these', 'days'],
        ARRAY['these', 'really', 'days', 'into', 'was', 'onto', 'those', 'I''m', 'hiking'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/1/expressions/5/practice-examples/140e37d4-db77-4e98-982e-8142cf6053f5.png","sentenceText":"She''s into hiking and camping.","sentenceWords":["She''s","into","hiking","and","camping"],"highlightingPart":"''s into","practiceQuestion":"What does your sister do for fun?","sentenceTranslation":"걔는 등산이랑 캠핑을 좋아해.","sentenceWordChoices":["She''s","and","enjoy","camping","love","hiking","into","like"],"practiceQuestionTranslation":"네 여동생은 취미가 뭐야?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/1/expressions/5/practice-examples/311bf7c9-a565-4d1a-b62b-ec071205ab54.png","sentenceText":"What kind of music are you into?","sentenceWords":["What","kind","of","music","are","you","into"],"highlightingPart":"are you into","practiceQuestion":"I love listening to music.","sentenceTranslation":"넌 어떤 음악 좋아해?","sentenceWordChoices":["are","of","kind","What","music","enjoy","into","love","you","like"],"practiceQuestionTranslation":"나 음악 듣는 거 완전 좋아해."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/1/expressions/5/practice-examples/9aba59b8-e5c5-45d6-8d2f-24a2b34ac0f7.png","sentenceText":"I''m not really into horror movies.","sentenceWords":["I''m","not","really","into","horror","movies"],"highlightingPart":"''m not really into","practiceQuestion":"Want to watch a horror movie tonight?","sentenceTranslation":"난 공포 영화는 별로 안 좋아해.","sentenceWordChoices":["horror","I''m","not","really","into","like","love","enjoy","movies"],"practiceQuestionTranslation":"오늘 밤 공포 영화 볼래?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/1/expressions/5/practice-examples/b9a455c0-b2d9-495a-98a7-7b1c93854661.png","sentenceText":"He''s been into cooking lately.","sentenceWords":["He''s","been","into","cooking","lately"],"highlightingPart":"''s been into","practiceQuestion":"What''s he been up to lately?","sentenceTranslation":"걔 요즘 요리에 빠져 있어.","sentenceWordChoices":["lately","enjoy","into","love","He''s","been","cooking","like"],"practiceQuestionTranslation":"걔 요즘 뭐 하고 지내?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 5
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        6,
        2,
        'SCENARIO',
        'WORK_STUDY',
        'BASIC',
        'EN',
        'KR',
        1,
        'work it out',
        '해결하다, 풀어내다',
        '함께 해결하는 work it out',
        '''(문제를) 해결하다, 풀어내다''라는 뜻으로, 특히 갈등이나 문제를 노력해서 풀 때 씁니다. 관계 문제부터 계산까지 폭넓게 쓰이는 만능 표현이에요.',
        'How do you wanna split the cleaning?',
        '청소 어떻게 나누고 싶어?',
        'We can work out a cleaning schedule together.',
        '청소 일정은 같이 정하면 돼.',
        ARRAY['We', 'can', 'work', 'out', 'a', 'cleaning', 'schedule', 'together'],
        ARRAY['the', 'in', 'can', 'work', 'schedule', 'We', 'cleaning', 'works', 'together', 'out', 'a'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/2/expressions/6/practice-examples/638e1055-3dd6-45ee-b40d-c89426f3c3fb.png","sentenceText":"They worked out their differences.","sentenceWords":["They","worked","out","their","differences"],"highlightingPart":"worked out","practiceQuestion":"How are things between them now?","sentenceTranslation":"걔네는 의견 차이를 잘 풀었어.","sentenceWordChoices":["out","worked","differences","figure","solve","fix","their","They"],"practiceQuestionTranslation":"걔네 사이 요즘 어때?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/2/expressions/6/practice-examples/21b84a1f-f262-4b59-aad1-0897edcc07f4.png","sentenceText":"Let''s work out a schedule that works for both of us.","sentenceWords":["Let''s","work","out","a","schedule","that","works","for","both","of","us"],"highlightingPart":"work out","practiceQuestion":"When should we clean the bathroom?","sentenceTranslation":"둘 다 괜찮은 일정으로 조율해 보자.","sentenceWordChoices":["out","for","work","figure","fix","solve","of","Let''s","a","us","both","schedule","works","that"],"practiceQuestionTranslation":"화장실 청소는 언제 할까?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/2/expressions/6/practice-examples/f8c4afc5-76cb-44a1-9d86-27a2a4dd9915.png","sentenceText":"Things will work out in the end.","sentenceWords":["Things","will","work","out","in","the","end"],"highlightingPart":"work out","practiceQuestion":"I''m scared everything will fall apart.","sentenceTranslation":"결국엔 다 잘 풀릴 거야.","sentenceWordChoices":["out","fix","in","work","solve","will","figure","the","Things","end"],"practiceQuestionTranslation":"다 틀어질까 봐 무서워."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/2/expressions/6/practice-examples/572b29c9-a7f8-4964-a1f6-6ac576037976.png","sentenceText":"Can you work out how much we each owe?","sentenceWords":["Can","you","work","out","how","much","we","each","owe"],"highlightingPart":"work out","practiceQuestion":"How much do we owe for dinner?","sentenceTranslation":"우리 각자 얼마씩 내야 하는지 계산해 줄래?","sentenceWordChoices":["we","how","you","owe","fix","Can","each","figure","much","work","out","solve"],"practiceQuestionTranslation":"저녁값 얼마씩 내야 해?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 6
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        7,
        2,
        'SCENARIO',
        'DAILY_ROUTINE',
        'BASIC',
        'EN',
        'KR',
        2,
        'stay up',
        '안 자고 깨어 있다, 밤새다',
        '밤새우는 stay up',
        '''안 자고 깨어 있다, 밤새다''라는 뜻입니다. stay up late(늦게까지 깨어 있다), stay up all night(밤을 새우다)로 활용하면 돼요.',
        'When do you usually go to bed?',
        '넌 보통 언제 자?',
        'I''m a night owl, so I usually stay up late.',
        '난 야행성이라 보통 늦게까지 안 자.',
        ARRAY['I''m', 'a', 'night', 'owl', 'so', 'I', 'usually', 'stay', 'up', 'late'],
        ARRAY['usually', 'up', 'owl', 'stay', 'I', 'a', 'down', 'stays', 'I''m', 'night', 'so', 'lately', 'late'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/2/expressions/7/practice-examples/a37a5e9c-c6ec-4af8-9b78-3df08aa7eb79.png","sentenceText":"Don''t stay up too late.","sentenceWords":["Don''t","stay","up","too","late"],"highlightingPart":"stay up","practiceQuestion":"I''ve got an early flight tomorrow.","sentenceTranslation":"너무 늦게까지 깨어 있지 마.","sentenceWordChoices":["too","nap","asleep","slept","late","up","stay","Don''t"],"practiceQuestionTranslation":"나 내일 아침 일찍 비행기 타야 해."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/2/expressions/7/practice-examples/508d6bdb-a6ae-4ec4-a1cb-466753366203.png","sentenceText":"She stayed up to finish the assignment.","sentenceWords":["She","stayed","up","to","finish","the","assignment"],"highlightingPart":"stayed up","practiceQuestion":"Why was her light on so late?","sentenceTranslation":"걔는 과제 끝내느라 안 자고 있었어.","sentenceWordChoices":["slept","the","assignment","asleep","finish","up","stayed","She","nap","to"],"practiceQuestionTranslation":"걔 방 불이 왜 그렇게 늦게까지 켜져 있었어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/2/expressions/7/practice-examples/45aacbca-6e7a-4ffd-a27a-060754fcd3ee.png","sentenceText":"We stayed up talking until 3 a.m.","sentenceWords":["We","stayed","up","talking","until","3","a.m."],"highlightingPart":"stayed up","practiceQuestion":"How was your night with Charlie?","sentenceTranslation":"새벽 3시까지 수다 떨었어.","sentenceWordChoices":["3","slept","stayed","talking","a.m.","asleep","nap","until","We","up"],"practiceQuestionTranslation":"Charlie랑 밤에 뭐 했어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/2/expressions/7/practice-examples/3469509c-234c-4743-988b-efa427887ba2.png","sentenceText":"I can''t stay up past midnight anymore.","sentenceWords":["I","can''t","stay","up","past","midnight","anymore"],"highlightingPart":"stay up","practiceQuestion":"Want to pull an all-nighter this weekend?","sentenceTranslation":"이제 자정 넘어서까지 못 버티겠어.","sentenceWordChoices":["asleep","past","can''t","slept","I","stay","nap","up","midnight","anymore"],"practiceQuestionTranslation":"이번 주말에 밤샐래?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 7
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        8,
        2,
        'SCENARIO',
        'EMOTION_EMPATHY',
        'BASIC',
        'EN',
        'KR',
        3,
        'be sick of',
        '~이 지긋지긋하다',
        '지긋지긋함을 표현하는 be sick of',
        '''~에 신물이 나다, 지긋지긋하다''라는 뜻입니다. tired of보다 강한 짜증이 담겨 있고, sick and tired of라고 하면 더 강조돼요.',
        'What can''t you put up with in a roommate?',
        '룸메한테 뭘 못 참아?',
        'I get sick of dishes piling up in the sink.',
        '싱크대에 설거지 쌓이는 게 지긋지긋해.',
        ARRAY['I', 'get', 'sick', 'of', 'dishes', 'piling', 'up', 'in', 'the', 'sink'],
        ARRAY['sick', 'get', 'on', 'gets', 'tired', 'in', 'piling', 'I', 'sink', 'of', 'dishes', 'the', 'up'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/2/expressions/8/practice-examples/6a215905-aeea-4191-88fe-1942dcabaff2.png","sentenceText":"I''m sick of eating the same lunch every day.","sentenceWords":["I''m","sick","of","eating","the","same","lunch","every","day"],"highlightingPart":"''m sick of","practiceQuestion":"Want to grab lunch here again?","sentenceTranslation":"매일 똑같은 점심 먹는 거 질렸어.","sentenceWordChoices":["every","same","annoyed","fed","lunch","sick","eating","the","day","hate","I''m","of"],"practiceQuestionTranslation":"또 여기서 점심 먹을래?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/2/expressions/8/practice-examples/3a54904a-c3fd-4361-ab16-6f6337c934b4.png","sentenceText":"She''s sick of his excuses.","sentenceWords":["She''s","sick","of","his","excuses"],"highlightingPart":"''s sick of","practiceQuestion":"Is she still dating him?","sentenceTranslation":"걔는 그의 변명에 신물이 났어.","sentenceWordChoices":["his","of","She''s","hate","excuses","sick","fed","annoyed"],"practiceQuestionTranslation":"걔 아직도 걔랑 사귀어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/2/expressions/8/practice-examples/3bd6d27d-8acf-4ffc-a7f0-ac3b9e68dfce.png","sentenceText":"Aren''t you sick of this song yet?","sentenceWords":["Aren''t","you","sick","of","this","song","yet"],"highlightingPart":"sick of","practiceQuestion":"This song has been playing all day.","sentenceTranslation":"이 노래 아직도 안 질렸어?","sentenceWordChoices":["hate","yet","fed","song","this","you","of","annoyed","sick","Aren''t"],"practiceQuestionTranslation":"이 노래 하루 종일 나오네."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/2/expressions/8/practice-examples/3ea77aac-a5b2-4ca2-9aec-ed89cc288df2.png","sentenceText":"I''m sick and tired of waiting.","sentenceWords":["I''m","sick","and","tired","of","waiting"],"highlightingPart":"sick and tired of","practiceQuestion":"The bus is taking forever.","sentenceTranslation":"기다리는 거 정말 지긋지긋해.","sentenceWordChoices":["tired","of","and","sick","annoyed","hate","I''m","waiting","fed"],"practiceQuestionTranslation":"버스가 진짜 안 온다."}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 8
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        9,
        3,
        'SCENARIO',
        'TIME_PLANNING',
        'BASIC',
        'EN',
        'KR',
        1,
        'work for',
        '(일정이) ~에게 괜찮다',
        '일정을 조율하는 work for',
        '''그 시간 괜찮아?''처럼 일정이나 조건이 맞는지 물을 때 동사 work를 씁니다. ''작동하다''가 아니라 ''(나에게) 통하다, 괜찮다''라는 의미로 쓰이는 활용도 높은 표현이에요.',
        'Does Saturday or Sunday work better for you?',
        '토요일이랑 일요일 중에 언제가 더 좋아?',
        'Saturday works better for me.',
        '난 토요일이 더 좋아.',
        ARRAY['Saturday', 'works', 'better', 'for', 'me'],
        ARRAY['Saturday', 'works', 'to', 'better', 'good', 'me', 'work', 'for'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/3/expressions/9/practice-examples/e066540e-e744-4393-8fc8-6dc7871f288a.png","sentenceText":"Monday works for me.","sentenceWords":["Monday","works","for","me"],"highlightingPart":"works for","practiceQuestion":"Can we meet up sometime next week?","sentenceTranslation":"난 월요일 괜찮아.","sentenceWordChoices":["me","suits","works","for","good","Monday","fit"],"practiceQuestionTranslation":"다음 주 중에 만날까?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/3/expressions/9/practice-examples/b4856be5-390c-46df-9426-71e3f41c88a6.png","sentenceText":"Does this weekend work for everyone?","sentenceWords":["Does","this","weekend","work","for","everyone"],"highlightingPart":"work for","practiceQuestion":"Should we plan the trip for this weekend?","sentenceTranslation":"이번 주말 다들 괜찮아?","sentenceWordChoices":["everyone","Does","this","good","work","weekend","suits","fit","for"],"practiceQuestionTranslation":"이번 주말로 여행 잡을까?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/3/expressions/9/practice-examples/a6c6d18a-a877-4ff7-94f5-d13b76d3c003.png","sentenceText":"That time doesn''t work for me.","sentenceWords":["That","time","doesn''t","work","for","me"],"highlightingPart":"doesn''t work for","practiceQuestion":"Is 5 o''clock okay for you?","sentenceTranslation":"그 시간은 나 안 돼.","sentenceWordChoices":["That","good","work","doesn''t","me","time","for","fit","suits"],"practiceQuestionTranslation":"5시 괜찮아?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/3/expressions/9/practice-examples/7a138446-75c1-4ec6-9085-9534d226f581.png","sentenceText":"If Thursday doesn''t work, how about Friday?","sentenceWords":["If","Thursday","doesn''t","work","how","about","Friday"],"highlightingPart":"doesn''t work","practiceQuestion":"Are you free Thursday?","sentenceTranslation":"목요일이 안 되면 금요일은 어때?","sentenceWordChoices":["good","how","If","about","work","doesn''t","Friday","Thursday","suits","fit"],"practiceQuestionTranslation":"목요일에 시간 돼?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 9
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        10,
        3,
        'SCENARIO',
        'EMOTION_EMPATHY',
        'BASIC',
        'EN',
        'KR',
        2,
        'can''t wait to',
        '빨리 ~하고 싶다',
        '기대감을 표현하는 can''t wait to',
        '''너무 기대돼서 못 기다리겠어'', 즉 ''빨리 ~하고 싶어''라고 말할 때 가장 흔하게 쓰이는 표현입니다. can''t wait to 뒤에 동사원형, can''t wait for 뒤에 명사를 붙여요.',
        'Anything you''ve been dying to try since you got here?',
        '여기 온 뒤로 꼭 해보고 싶었던 거 있어?',
        'I can''t wait to try a real American diner.',
        '진짜 미국식 다이너 꼭 가보고 싶어.',
        ARRAY['I', 'can''t', 'wait', 'to', 'try', 'a', 'real', 'American', 'diner'],
        ARRAY['American', 'waiting', 'wait', 'diner', 'a', 'real', 'can''t', 'to', 'for', 'try', 'I', 'tried'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/3/expressions/10/practice-examples/0cac7d57-2799-49bb-a539-82d437c3a4d5.png","sentenceText":"I can''t wait to try that new restaurant.","sentenceWords":["I","can''t","wait","to","try","that","new","restaurant"],"highlightingPart":"can''t wait to","practiceQuestion":"Have you been to that new place?","sentenceTranslation":"그 새로 생긴 식당 빨리 가보고 싶어.","sentenceWordChoices":["waiting","that","dying","wait","excited","try","new","restaurant","can''t","to","I"],"practiceQuestionTranslation":"그 새로 생긴 데 가봤어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/3/expressions/10/practice-examples/d9473f04-74a9-4cae-8855-442476479019.png","sentenceText":"She can''t wait to start her new job.","sentenceWords":["She","can''t","wait","to","start","her","new","job"],"highlightingPart":"can''t wait to","practiceQuestion":"How does she feel about the new job?","sentenceTranslation":"걔는 새 직장 시작하는 걸 엄청 기대하고 있어.","sentenceWordChoices":["excited","new","can''t","start","her","job","She","to","wait","waiting","dying"],"practiceQuestionTranslation":"걔 새 직장에 대해 어떻게 생각해?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/3/expressions/10/practice-examples/6b90aeda-7efe-472d-be37-af151b1e925b.png","sentenceText":"I can''t wait for the weekend.","sentenceWords":["I","can''t","wait","for","the","weekend"],"highlightingPart":"can''t wait for","practiceQuestion":"This week has been so long.","sentenceTranslation":"주말이 너무 기다려져.","sentenceWordChoices":["for","excited","wait","I","the","weekend","dying","can''t","waiting"],"practiceQuestionTranslation":"이번 주 진짜 길다."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/3/expressions/10/practice-examples/d1611014-bde8-4995-875d-c34c8668d457.png","sentenceText":"We can''t wait to hear the results.","sentenceWords":["We","can''t","wait","to","hear","the","results"],"highlightingPart":"can''t wait to","practiceQuestion":"When do the results come out?","sentenceTranslation":"결과 듣는 게 너무 기대돼.","sentenceWordChoices":["wait","hear","can''t","waiting","We","dying","to","results","excited","the"],"practiceQuestionTranslation":"결과 언제 나와?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 10
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        11,
        3,
        'SCENARIO',
        'RELATIONSHIP_SOCIAL',
        'BASIC',
        'EN',
        'KR',
        3,
        'be up for / be down for',
        '~할 마음이 있다, 콜이다',
        '제안에 동의할 때 be up for / be down for',
        '''나 그거 할래'', ''콜!''처럼 제안에 긍정적으로 응할 때 쓰는 캐주얼한 표현입니다. 재미있게도 up과 down이 정반대 단어인데 둘 다 같은 의미로 쓰여요.',
        'Are you more of a go-explore person or a stay-in person?',
        '넌 도시 탐험파야, 집콕파야?',
        'Honestly, I''m always up for exploring the city.',
        '솔직히 난 도시 탐험이라면 언제든 콜이야.',
        ARRAY['Honestly', 'I''m', 'always', 'up', 'for', 'exploring', 'the', 'city'],
        ARRAY['explore', 'to', 'for', 'was', 'city', 'the', 'Honestly', 'always', 'up', 'exploring', 'I''m'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/3/expressions/11/practice-examples/2ab72a8f-2081-4831-9816-5df63da9fd85.png","sentenceText":"Are you up for a movie later?","sentenceWords":["Are","you","up","for","a","movie","later"],"highlightingPart":"up for","practiceQuestion":"I''m bored tonight.","sentenceTranslation":"이따 영화 볼래?","sentenceWordChoices":["later","interested","a","for","movie","willing","up","Are","keen","you"],"practiceQuestionTranslation":"오늘 밤 심심하다."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/3/expressions/11/practice-examples/d3d7c52e-3bcd-4c67-9bfc-0171bac591d6.png","sentenceText":"I''m down for whatever you want.","sentenceWords":["I''m","down","for","whatever","you","want"],"highlightingPart":"''m down for","practiceQuestion":"What do you want to eat?","sentenceTranslation":"네가 원하는 거 뭐든 좋아.","sentenceWordChoices":["you","I''m","willing","want","interested","for","keen","whatever","down"],"practiceQuestionTranslation":"뭐 먹고 싶어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/3/expressions/11/practice-examples/fe2d531c-0f35-4831-8fa7-ddcf4c1752dc.png","sentenceText":"She''s always up for a challenge.","sentenceWords":["She''s","always","up","for","a","challenge"],"highlightingPart":"up for","practiceQuestion":"Is she up for trying something hard?","sentenceTranslation":"걔는 항상 도전을 마다하지 않아.","sentenceWordChoices":["interested","for","up","She''s","a","challenge","willing","keen","always"],"practiceQuestionTranslation":"걔 어려운 거 도전할 마음 있어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/3/expressions/11/practice-examples/e5c27bf1-6876-4fd5-819f-0a439a69b45d.png","sentenceText":"Who''s down for a road trip this weekend?","sentenceWords":["Who''s","down","for","a","road","trip","this","weekend"],"highlightingPart":"''s down for","practiceQuestion":"We should do something fun this weekend.","sentenceTranslation":"이번 주말 드라이브 갈 사람?","sentenceWordChoices":["a","willing","weekend","for","down","Who''s","interested","this","trip","keen","road"],"practiceQuestionTranslation":"이번 주말에 뭔가 재밌는 거 하자."}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 11
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        12,
        3,
        'SCENARIO',
        'DAILY_ROUTINE',
        'SLANG_NEOLOGISM',
        'EN',
        'KR',
        4,
        'rot in bed',
        '침대에서 뒹굴거리다',
        '침대에서 뒹굴거리는 rot in bed',
        '''침대에서 썩다'', 즉 ''아무것도 안 하고 침대에서 뒹굴거리다''라는 뜻의 슬랭입니다. bed rotting이라는 명사형으로도 유행하며, 죄책감 반 행복 반의 휴식을 표현해요.',
        'What sounds more fun to you on a weekend?',
        '주말엔 뭐가 더 재밌을 것 같아?',
        'Honestly, I''d rather just rot in bed all day.',
        '솔직히 난 그냥 하루 종일 침대에서 뒹굴고 싶어.',
        ARRAY['Honestly', 'I''d', 'rather', 'just', 'rot', 'in', 'bed', 'all', 'day'],
        ARRAY['Honestly', 'rotting', 'day', 'bed', 'just', 'would', 'all', 'rather', 'rot', 'I''d', 'in', 'on'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/3/expressions/12/practice-examples/404f019a-626e-493a-9bcc-15c79f4d987b.png","sentenceText":"I spent Sunday rotting in bed.","sentenceWords":["I","spent","Sunday","rotting","in","bed"],"highlightingPart":"rotting in bed","practiceQuestion":"How did you spend your Sunday?","sentenceTranslation":"일요일을 침대에서 뒹굴며 보냈어.","sentenceWordChoices":["spent","bed","lazing","in","rotting","Sunday","lounge","I","lying"],"practiceQuestionTranslation":"일요일 어떻게 보냈어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/3/expressions/12/practice-examples/bb079c77-8e12-45ac-a45f-298a94547453.png","sentenceText":"After that week, I deserve to rot in bed.","sentenceWords":["After","that","week","I","deserve","to","rot","in","bed"],"highlightingPart":"rot in bed","practiceQuestion":"That week was so rough.","sentenceTranslation":"그런 한 주를 보냈으니 침대에서 썩을 자격이 있지.","sentenceWordChoices":["rot","in","I","that","After","deserve","lounge","bed","lazing","lying","week","to"],"practiceQuestionTranslation":"그 주는 진짜 힘들었어."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/3/expressions/12/practice-examples/96c04cfb-8ac8-4023-aba8-c8f615fde37d.png","sentenceText":"No plans today — just bed rotting.","sentenceWords":["No","plans","today","just","bed","rotting"],"highlightingPart":"bed rotting","practiceQuestion":"Any plans for today?","sentenceTranslation":"오늘 약속 없어. 그냥 침대에서 뒹굴 거야.","sentenceWordChoices":["today","rotting","bed","lying","No","lazing","just","plans","lounge"],"practiceQuestionTranslation":"오늘 뭐 할 거야?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/3/expressions/12/practice-examples/16f57632-938f-4e21-a217-9b219e591542.png","sentenceText":"Quit rotting in bed and get some sunlight.","sentenceWords":["Quit","rotting","in","bed","and","get","some","sunlight"],"highlightingPart":"rotting in bed","practiceQuestion":"I''ve been in bed all day.","sentenceTranslation":"그만 뒹굴고 나가서 햇볕 좀 쫴.","sentenceWordChoices":["lazing","lying","sunlight","and","lounge","in","rotting","bed","some","Quit","get"],"practiceQuestionTranslation":"나 하루 종일 침대에 누워 있었어."}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 12
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        13,
        4,
        'SCENARIO',
        'CONVERSATION_SKILL',
        'BASIC',
        'EN',
        'KR',
        1,
        'be about',
        '~에 관한 것이다',
        '이유나 용건을 묻는 be about',
        '''~에 관한 것이다''라는 뜻으로, 연락이나 상황의 용건을 물을 때 씁니다. What is this about?은 ''무슨 일이시죠?''라는 뜻의 실전 필수 문장이에요.',
        'Hi, front desk. How can I help you?',
        '네, 프런트 데스크입니다. 무엇을 도와드릴까요?',
        'Hi, I''m calling about my AC bill for July.',
        '안녕하세요, 7월 에어컨 요금 때문에 전화드렸어요.',
        ARRAY['Hi', 'I''m', 'calling', 'about', 'my', 'AC', 'bill', 'for', 'July'],
        ARRAY['I''m', 'to', 'of', 'July', 'my', 'for', 'bill', 'Hi', 'call', 'calling', 'AC', 'about'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/4/expressions/13/practice-examples/3c4b6699-0232-4543-a2a3-e214f83af1bf.png","sentenceText":"What was the call about?","sentenceWords":["What","was","the","call","about"],"highlightingPart":"was the call about","practiceQuestion":"Who called you just now?","sentenceTranslation":"그 전화 무슨 용건이었어?","sentenceWordChoices":["about","regarding","call","on","the","What","was","concerning"],"practiceQuestionTranslation":"방금 누구한테 전화 왔어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/4/expressions/13/practice-examples/f299cac8-6ef8-4130-b675-e82a5b231499.png","sentenceText":"Is this about the schedule change?","sentenceWords":["Is","this","about","the","schedule","change"],"highlightingPart":"Is this about","practiceQuestion":"I got an email from the office.","sentenceTranslation":"이거 일정 변경 건인가요?","sentenceWordChoices":["schedule","change","about","on","the","concerning","Is","this","regarding"],"practiceQuestionTranslation":"사무실에서 메일이 왔어."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/4/expressions/13/practice-examples/466f0b94-161e-4424-b0d5-f602c41d3759.png","sentenceText":"The movie is about two old friends.","sentenceWords":["The","movie","is","about","two","old","friends"],"highlightingPart":"is about","practiceQuestion":"What''s that movie about?","sentenceTranslation":"그 영화는 오랜 친구 둘에 관한 얘기야.","sentenceWordChoices":["two","on","friends","old","is","movie","The","concerning","about","regarding"],"practiceQuestionTranslation":"그 영화 무슨 내용이야?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/4/expressions/13/practice-examples/03a715d0-1afe-4107-b2e7-f4c6450fc9f1.png","sentenceText":"I don''t know what the fuss is about.","sentenceWords":["I","don''t","know","what","the","fuss","is","about"],"highlightingPart":"is about","practiceQuestion":"Everyone''s talking about something in the office.","sentenceTranslation":"뭐 때문에 이렇게 난리인지 모르겠네.","sentenceWordChoices":["don''t","regarding","is","fuss","I","what","know","the","on","about","concerning"],"practiceQuestionTranslation":"사무실에서 다들 뭔가 얘기하고 있던데."}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 13
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        14,
        4,
        'SCENARIO',
        'WORK_STUDY',
        'BASIC',
        'EN',
        'KR',
        2,
        'look into',
        '조사하다, 살펴보다',
        '살펴봐 달라는 look into',
        '''조사하다, 살펴보다''라는 뜻으로, 문제나 사안을 확인해 본다는 의미입니다. 업무 메일에서 I''ll look into it(확인해 보겠습니다)으로 아주 많이 쓰여요.',
        'Let me pull up your account.',
        '계정 좀 확인해 볼게요.',
        'Could you look into why the bill is so high?',
        '요금이 왜 이렇게 많이 나왔는지 확인해 주시겠어요?',
        ARRAY['Could', 'you', 'look', 'into', 'why', 'the', 'bill', 'is', 'so', 'high'],
        ARRAY['so', 'looking', 'high', 'the', 'bill', 'at', 'is', 'Could', 'why', 'you', 'into', 'look', 'much'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/4/expressions/14/practice-examples/8d59335b-07d0-4423-9117-bbef31ef10c4.png","sentenceText":"Can you look into the error?","sentenceWords":["Can","you","look","into","the","error"],"highlightingPart":"look into","practiceQuestion":"The app keeps crashing.","sentenceTranslation":"그 오류 좀 살펴봐 줄래?","sentenceWordChoices":["research","look","examine","Can","error","investigate","into","you","the"],"practiceQuestionTranslation":"앱이 자꾸 꺼져."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/4/expressions/14/practice-examples/ad8722d2-fb66-4bdc-b88b-b32889b3f6c7.png","sentenceText":"We''re looking into new apartment options.","sentenceWords":["We''re","looking","into","new","apartment","options"],"highlightingPart":"looking into","practiceQuestion":"Are you moving soon?","sentenceTranslation":"우리 새 아파트 알아보는 중이야.","sentenceWordChoices":["looking","apartment","options","research","into","investigate","examine","We''re","new"],"practiceQuestionTranslation":"곧 이사 가?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/4/expressions/14/practice-examples/fae074f0-41d0-4df6-9b4f-f8df1eceac8c.png","sentenceText":"The company is looking into the complaint.","sentenceWords":["The","company","is","looking","into","the","complaint"],"highlightingPart":"looking into","practiceQuestion":"Did they respond to the complaint?","sentenceTranslation":"회사에서 그 민원을 조사 중이야.","sentenceWordChoices":["the","examine","The","complaint","company","looking","is","research","investigate","into"],"practiceQuestionTranslation":"그 민원에 답변 왔어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/4/expressions/14/practice-examples/8beceab5-0310-4cf3-846f-fcfee60acdf9.png","sentenceText":"I''ll look into flights for next month.","sentenceWords":["I''ll","look","into","flights","for","next","month"],"highlightingPart":"look into","practiceQuestion":"Are you planning a trip?","sentenceTranslation":"다음 달 항공편 알아볼게.","sentenceWordChoices":["I''ll","flights","examine","next","investigate","for","research","into","month","look"],"practiceQuestionTranslation":"여행 계획 있어?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 14
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        15,
        4,
        'SCENARIO',
        'POLITE_EXPRESSION',
        'BASIC',
        'EN',
        'KR',
        3,
        'Would you like me to ~?',
        '제가 ~해드릴까요?',
        '공손하게 제안하는 Would you like me to ~?',
        '''제가 ~해드릴까요?''라고 정중하게 도움을 제안하는 표현입니다. Do you want me to ~?보다 한층 공손해서 직장이나 처음 보는 사람에게 쓰기 좋아요.',
        'Do you have any proof you were traveling in July?',
        '7월에 여행하셨다는 증빙 자료 있으세요?',
        'Would you like me to email you my flight tickets?',
        '제 항공권을 이메일로 보내드릴까요?',
        ARRAY['Would', 'you', 'like', 'me', 'to', 'email', 'you', 'my', 'flight', 'tickets'],
        ARRAY['my', 'email', 'to', 'want', 'flight', 'sending', 'you', 'your', 'tickets', 'like', 'me', 'Would', 'you'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/4/expressions/15/practice-examples/051f1e34-2376-45e7-9c89-6d208a0f4c6b.png","sentenceText":"Would you like me to call you a taxi?","sentenceWords":["Would","you","like","me","to","call","you","a","taxi"],"highlightingPart":"Would you like me to","practiceQuestion":"There are no taxis around here.","sentenceTranslation":"택시 불러드릴까요?","sentenceWordChoices":["taxi","like","Would","you","Could","to","Shall","you","a","Will","me","call"],"practiceQuestionTranslation":"이 근처에 택시가 없네."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/4/expressions/15/practice-examples/18d82e54-18d8-4b92-8b27-8d533e5445b8.png","sentenceText":"Would you like me to send you the file?","sentenceWords":["Would","you","like","me","to","send","you","the","file"],"highlightingPart":"Would you like me to","practiceQuestion":"I need that document for the meeting.","sentenceTranslation":"파일 보내드릴까요?","sentenceWordChoices":["Shall","me","Could","file","the","like","you","send","Will","Would","to","you"],"practiceQuestionTranslation":"회의 때문에 그 자료가 필요한데요."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/4/expressions/15/practice-examples/d3d18c5d-a3c8-46ae-a240-927593b4a7c6.png","sentenceText":"Would you like me to pick you up at the station?","sentenceWords":["Would","you","like","me","to","pick","you","up","at","the","station"],"highlightingPart":"Would you like me to","practiceQuestion":"I''m not sure how to get from the station.","sentenceTranslation":"역으로 데리러 갈까요?","sentenceWordChoices":["up","at","Shall","me","Would","you","the","like","station","Will","Could","pick","to","you"],"practiceQuestionTranslation":"역에서 어떻게 가야 할지 모르겠어요."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/4/expressions/15/practice-examples/d84f5e47-a20d-4e34-993b-5c6a656fa80c.png","sentenceText":"Would you like me to explain it again?","sentenceWords":["Would","you","like","me","to","explain","it","again"],"highlightingPart":"Would you like me to","practiceQuestion":"I didn''t quite catch that.","sentenceTranslation":"다시 설명해 드릴까요?","sentenceWordChoices":["Will","explain","again","Would","Could","Shall","me","it","you","like","to"],"practiceQuestionTranslation":"잘 이해가 안 갔어요."}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 15
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        16,
        4,
        'SCENARIO',
        'POLITE_EXPRESSION',
        'BASIC',
        'EN',
        'KR',
        4,
        'I''m afraid',
        '유감이지만 ~입니다',
        '미안함을 담아 전하는 I''m afraid',
        '''유감이지만 ~입니다''라며 좋지 않은 소식을 정중하고 부드럽게 전달하는 표현입니다. 특히 서비스 응대나 거절 상황에서 충격을 완화해 주는 쿠션 역할을 해요.',
        'Would you like a cash refund or a discount next month?',
        '현금 환불받으시겠어요, 다음 달 할인받으시겠어요?',
        'I''m afraid I''d prefer a cash refund.',
        '죄송하지만 현금 환불이 좋을 것 같아요.',
        ARRAY['I''m', 'afraid', 'I''d', 'prefer', 'a', 'cash', 'refund'],
        ARRAY['I''d', 'preferred', 'refund', 'I''m', 'would', 'afraid', 'cash', 'a', 'prefer', 'scared'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/4/expressions/16/practice-examples/db1a3af9-6e74-4e3a-8983-2019d82330e8.png","sentenceText":"I''m afraid I can''t make it.","sentenceWords":["I''m","afraid","I","can''t","make","it"],"highlightingPart":"I''m afraid","practiceQuestion":"Can you come to the party tonight?","sentenceTranslation":"미안하지만 못 갈 것 같아.","sentenceWordChoices":["sadly","afraid","I","it","worried","can''t","make","I''m","scared"],"practiceQuestionTranslation":"오늘 밤 파티 올 수 있어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/4/expressions/16/practice-examples/eadcc569-5b84-45b2-9f71-0051753465c7.png","sentenceText":"I''m afraid that seat is taken.","sentenceWords":["I''m","afraid","that","seat","is","taken"],"highlightingPart":"I''m afraid","practiceQuestion":"Excuse me, is this seat free?","sentenceTranslation":"죄송하지만 그 자리는 주인이 있어요.","sentenceWordChoices":["that","sadly","afraid","taken","is","I''m","scared","worried","seat"],"practiceQuestionTranslation":"실례지만 여기 자리 있나요?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/4/expressions/16/practice-examples/e0a8c733-1a43-4c18-b172-bf30bf5847a6.png","sentenceText":"I''m afraid the flight has been delayed.","sentenceWords":["I''m","afraid","the","flight","has","been","delayed"],"highlightingPart":"I''m afraid","practiceQuestion":"What time does the flight leave?","sentenceTranslation":"유감이지만 비행기가 지연됐습니다.","sentenceWordChoices":["has","flight","sadly","I''m","scared","delayed","afraid","worried","been","the"],"practiceQuestionTranslation":"비행기가 몇 시에 출발하나요?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/4/expressions/16/practice-examples/f7b2234c-86a5-4e6e-b91e-7c5bbc9c4504.png","sentenceText":"I''m afraid I have some bad news.","sentenceWords":["I''m","afraid","I","have","some","bad","news"],"highlightingPart":"I''m afraid","practiceQuestion":"Is everything alright?","sentenceTranslation":"안타깝게도 안 좋은 소식이 있어.","sentenceWordChoices":["afraid","worried","sadly","news","I''m","have","bad","some","scared","I"],"practiceQuestionTranslation":"다 괜찮으신가요?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 16
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        17,
        5,
        'SCENARIO',
        'WORK_STUDY',
        'BASIC',
        'EN',
        'KR',
        1,
        'on the same page',
        '같은 생각/이해를 공유하는',
        '같은 이해선상에 있는 on the same page',
        '''같은 생각/이해를 공유하는'' 상태를 뜻하며, 회의나 협업에서 특히 자주 쓰입니다. 서로 오해 없이 같은 그림을 보고 있는지 확인할 때 딱이에요.',
        'How do you feel about having guests over?',
        '방에 손님 오는 거 어떻게 생각해?',
        'I just want us to be on the same page about guests.',
        '손님 문제는 우리 미리 입장 맞춰두고 싶어.',
        ARRAY['I', 'just', 'want', 'us', 'to', 'be', 'on', 'the', 'same', 'page', 'about', 'guests'],
        ARRAY['them', 'guests', 'pages', 'at', 'be', 'just', 'about', 'on', 'same', 'the', 'I', 'page', 'want', 'to', 'us'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/5/expressions/17/practice-examples/d1498553-2216-47a7-81f0-496cce8635fd.png","sentenceText":"Are we on the same page about the budget?","sentenceWords":["Are","we","on","the","same","page","about","the","budget"],"highlightingPart":"on the same page","practiceQuestion":"Did you look at the numbers I sent?","sentenceTranslation":"예산에 대해서 우리 생각이 같은 거지?","sentenceWordChoices":["Are","budget","we","page","aligned","same","the","the","pages","about","on","agree"],"practiceQuestionTranslation":"내가 보낸 숫자 봤어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/5/expressions/17/practice-examples/3d7ed917-2982-4d2c-a338-8cf1b2ec77e7.png","sentenceText":"The whole team is finally on the same page.","sentenceWords":["The","whole","team","is","finally","on","the","same","page"],"highlightingPart":"on the same page","practiceQuestion":"How''s the team doing lately?","sentenceTranslation":"드디어 팀 전체가 같은 방향을 보고 있어.","sentenceWordChoices":["on","aligned","finally","page","pages","agree","is","The","whole","the","same","team"],"practiceQuestionTranslation":"요즘 팀 분위기 어때?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/5/expressions/17/practice-examples/47818c0e-7e79-44ce-b6e7-16a0e6b08edf.png","sentenceText":"I want us to be on the same page before the meeting.","sentenceWords":["I","want","us","to","be","on","the","same","page","before","the","meeting"],"highlightingPart":"on the same page","practiceQuestion":"The meeting is tomorrow morning.","sentenceTranslation":"회의 전에 우리끼리 입장을 맞추고 싶어.","sentenceWordChoices":["aligned","agree","pages","the","want","I","on","before","be","the","same","to","page","meeting","us"],"practiceQuestionTranslation":"회의가 내일 아침이야."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/5/expressions/17/practice-examples/345ebdcc-252a-4d61-946b-a2ba3afb39b9.png","sentenceText":"We''re clearly not on the same page here.","sentenceWords":["We''re","clearly","not","on","the","same","page","here"],"highlightingPart":"on the same page","practiceQuestion":"Wait, that''s not what I meant at all.","sentenceTranslation":"우리 지금 서로 딴 얘기 하고 있는 것 같은데.","sentenceWordChoices":["pages","same","the","on","We''re","agree","not","page","aligned","here","clearly"],"practiceQuestionTranslation":"잠깐, 내가 말한 건 그게 아닌데."}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 17
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        18,
        5,
        'SCENARIO',
        'POLITE_EXPRESSION',
        'BASIC',
        'EN',
        'KR',
        2,
        'Do you mind ~ing?',
        '~해줄래요? (양해 구하기)',
        '조심스럽게 양해를 구하는 Do you mind ~ing?',
        '상대에게 부담스럽지 않게 부탁하거나 양해를 구할 때 쓰는 정중한 표현입니다. mind는 ''꺼리다''라는 뜻이라, 괜찮다고 답할 때는 No, not at all처럼 부정으로 답한다는 점도 포인트예요.',
        'Is it cool if I have a friend over sometimes?',
        '가끔 친구 불러도 괜찮아?',
        'Do you mind giving me a heads-up first?',
        '미리 한마디만 해줄 수 있어?',
        ARRAY['Do', 'you', 'mind', 'giving', 'me', 'a', 'heads-up', 'first'],
        ARRAY['first', 'me', 'you', 'last', 'Do', 'a', 'give', 'giving', 'would', 'mind', 'heads-up'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/5/expressions/18/practice-examples/e206de28-4b64-476e-9872-7184a57e230e.png","sentenceText":"Do you mind turning down the music?","sentenceWords":["Do","you","mind","turning","down","the","music"],"highlightingPart":"Do you mind","practiceQuestion":"The music is really loud.","sentenceTranslation":"음악 소리 좀 줄여줄 수 있어?","sentenceWordChoices":["the","Can","Will","music","down","Do","Could","turning","mind","you"],"practiceQuestionTranslation":"음악 소리가 너무 커."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/5/expressions/18/practice-examples/da3efd9d-cfe7-4b6c-b869-eb7307629b80.png","sentenceText":"Do you mind waiting a few minutes?","sentenceWords":["Do","you","mind","waiting","a","few","minutes"],"highlightingPart":"Do you mind","practiceQuestion":"I need a few more minutes to finish up.","sentenceTranslation":"몇 분만 기다려 주실 수 있나요?","sentenceWordChoices":["Could","mind","Can","you","few","minutes","Will","Do","a","waiting"],"practiceQuestionTranslation":"마무리하는 데 몇 분만 더 필요해요."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/5/expressions/18/practice-examples/90ed72da-0fb2-4e2b-a715-22fcc6f5e30c.png","sentenceText":"Do you mind if I sit here?","sentenceWords":["Do","you","mind","if","I","sit","here"],"highlightingPart":"Do you mind","practiceQuestion":"This café is packed today.","sentenceTranslation":"여기 앉아도 괜찮을까요?","sentenceWordChoices":["you","Can","mind","Will","Could","here","Do","I","if","sit"],"practiceQuestionTranslation":"오늘 카페가 사람 많다."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/5/expressions/18/practice-examples/3d53b7b8-4677-402a-a377-6b7dacf39024.png","sentenceText":"Would you mind repeating that?","sentenceWords":["Would","you","mind","repeating","that"],"highlightingPart":"Would you mind","practiceQuestion":"Sorry, I was talking pretty fast.","sentenceTranslation":"다시 한번 말씀해 주시겠어요?","sentenceWordChoices":["Will","Can","you","Could","that","mind","repeating","Would"],"practiceQuestionTranslation":"미안, 내가 좀 빨리 말했지."}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 18
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        19,
        5,
        'SCENARIO',
        'POLITE_EXPRESSION',
        'BASIC',
        'EN',
        'KR',
        3,
        'I''m good',
        '전 괜찮아요',
        '정중하게 거절하는 I''m good',
        '권유를 받았을 때 ''전 괜찮아요''라고 부드럽게 거절하는 마법의 두 단어입니다. No thanks보다 훨씬 부드럽고, 상황에 따라 ''난 잘 지내'' 같은 안부 답변으로도 쓰여요.',
        'Does music bother you when you study?',
        '공부할 때 음악 거슬려?',
        'I''m good with a little background noise.',
        '약간의 배경 소음은 난 괜찮아.',
        ARRAY['I''m', 'good', 'with', 'a', 'little', 'background', 'noise'],
        ARRAY['a', 'at', 'noises', 'with', 'noise', 'good', 'few', 'background', 'little', 'I''m'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/5/expressions/19/practice-examples/0587742c-b17c-4907-81f6-f05acb3fcdbd.png","sentenceText":"Need any help? — I''m good.","sentenceWords":["Need","any","help","I''m","good"],"highlightingPart":"I''m good","practiceQuestion":"You seem to be struggling with those boxes.","sentenceTranslation":"도움 필요해? — 난 괜찮아.","sentenceWordChoices":["cool","I''m","great","good","thanks","help","any","Need"],"practiceQuestionTranslation":"그 박스들 옮기기 힘들어 보이네."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/5/expressions/19/practice-examples/68609fe7-a6a6-4e40-a034-ca51b6501ac5.png","sentenceText":"Do you want a refill? — I''m good for now.","sentenceWords":["Do","you","want","a","refill","I''m","good","for","now"],"highlightingPart":"I''m good","practiceQuestion":"How''s your coffee?","sentenceTranslation":"리필해 드릴까요? — 지금은 괜찮아요.","sentenceWordChoices":["want","refill","a","good","Do","cool","you","for","now","thanks","great","I''m"],"practiceQuestionTranslation":"커피 어때요?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/5/expressions/19/practice-examples/322edd4f-a3a8-4cf6-ac01-ead6f2003574.png","sentenceText":"Anything else? — No, I''m good.","sentenceWords":["Anything","else","No","I''m","good"],"highlightingPart":"I''m good","practiceQuestion":"Here''s your order.","sentenceTranslation":"더 필요한 거 있으세요? — 아뇨, 괜찮아요.","sentenceWordChoices":["good","great","No","else","Anything","thanks","cool","I''m"],"practiceQuestionTranslation":"주문하신 거 나왔습니다."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/5/expressions/19/practice-examples/0843d37b-0d1f-473c-b671-65fd73e7db05.png","sentenceText":"Are you coming with us? — I''m good. I''ll stay home.","sentenceWords":["Are","you","coming","with","us","I''m","good","I''ll","stay","home"],"highlightingPart":"I''m good","practiceQuestion":"We''re heading to the mall.","sentenceTranslation":"같이 갈래? — 난 됐어. 집에 있을래.","sentenceWordChoices":["stay","thanks","cool","great","coming","I''ll","Are","I''m","with","good","you","home","us"],"practiceQuestionTranslation":"우리 몰 갈 건데."}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 19
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        20,
        5,
        'SCENARIO',
        'POLITE_EXPRESSION',
        'BASIC',
        'EN',
        'KR',
        4,
        'feel free to',
        '편하게 ~하세요',
        '부담 없이 요청하는 feel free to',
        '''편하게 ~하세요''라고 상대의 부담을 덜어주며 권하는 표현입니다. 이메일, 안내, 접객 등 격식 있는 자리에서도 두루 쓸 수 있는 만능 표현이에요.',
        'If I run out of milk, can I grab yours?',
        '나 우유 떨어지면 네 거 써도 돼?',
        'Feel free to grab my milk anytime.',
        '내 우유는 언제든 편하게 써도 돼.',
        ARRAY['Feel', 'free', 'to', 'grab', 'my', 'milk', 'anytime'],
        ARRAY['feels', 'for', 'my', 'grab', 'grabbing', 'Feel', 'free', 'anytime', 'milk', 'to'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/5/expressions/20/practice-examples/a38ae411-64b6-4758-9f26-ff3454160bd5.png","sentenceText":"Feel free to look around.","sentenceWords":["Feel","free","to","look","around"],"highlightingPart":"Feel free to","practiceQuestion":"This is my first time in your store.","sentenceTranslation":"편하게 둘러보세요.","sentenceWordChoices":["Feel","look","freely","feels","welcome","around","to","free"],"practiceQuestionTranslation":"여기 처음 와봤어요."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/5/expressions/20/practice-examples/e5ebfc6c-f5f4-4c82-8553-241be6bb16db.png","sentenceText":"Feel free to grab a snack from the kitchen.","sentenceWords":["Feel","free","to","grab","a","snack","from","the","kitchen"],"highlightingPart":"Feel free to","practiceQuestion":"I''m a little hungry.","sentenceTranslation":"부엌에서 간식 편하게 꺼내 먹어.","sentenceWordChoices":["the","welcome","snack","a","kitchen","free","grab","from","to","freely","feels","Feel"],"practiceQuestionTranslation":"나 좀 배고파."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/5/expressions/20/practice-examples/e6a7804c-bdbf-4fd3-8ca8-0a5c8a6fc129.png","sentenceText":"Feel free to say no.","sentenceWords":["Feel","free","to","say","no"],"highlightingPart":"Feel free to","practiceQuestion":"I have a favor to ask, but no pressure.","sentenceTranslation":"부담 갖지 말고 거절해도 돼.","sentenceWordChoices":["welcome","to","Feel","no","freely","say","free","feels"],"practiceQuestionTranslation":"부탁이 있는데, 부담 갖지 마."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/5/expressions/20/practice-examples/6498b36a-32a1-417e-ab8a-6b9fc713f490.png","sentenceText":"Please feel free to join us anytime.","sentenceWords":["Please","feel","free","to","join","us","anytime"],"highlightingPart":"feel free to","practiceQuestion":"We''re having a get-together this weekend.","sentenceTranslation":"언제든 편하게 함께하세요.","sentenceWordChoices":["us","freely","welcome","anytime","feels","free","to","Please","feel","join"],"practiceQuestionTranslation":"이번 주말에 모임 있어."}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 20
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        21,
        6,
        'SCENARIO',
        'RELATIONSHIP_SOCIAL',
        'BASIC',
        'EN',
        'KR',
        1,
        'hit it off',
        '처음부터 죽이 잘 맞다',
        '죽이 잘 맞을 때 hit it off',
        '''처음 만나자마자 잘 통하다'', ''죽이 잘 맞다''라는 뜻입니다. 첫 만남에서 바로 친해진 케미를 표현하는 데 이만한 표현이 없어요.',
        'Have you made a lot of friends yet?',
        '친구는 많이 사귀었어?',
        'Not many yet, but I hit it off with my roommate.',
        '아직 많진 않은데, 룸메랑은 바로 잘 통했어.',
        ARRAY['Not', 'many', 'yet', 'but', 'I', 'hit', 'it', 'off', 'with', 'my', 'roommate'],
        ARRAY['much', 'Not', 'hits', 'roommate', 'yet', 'off', 'on', 'it', 'but', 'many', 'hit', 'my', 'with', 'I'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/6/expressions/21/practice-examples/fcd7e181-8cca-472d-a9c6-78eb5c809ff7.png","sentenceText":"They hit it off on their first date.","sentenceWords":["They","hit","it","off","on","their","first","date"],"highlightingPart":"hit it off","practiceQuestion":"How did their date go?","sentenceTranslation":"걔네 첫 데이트에서 완전 죽이 잘 맞았대.","sentenceWordChoices":["They","on","clicked","hit","their","along","off","first","date","it","bonded"],"practiceQuestionTranslation":"걔네 데이트 어떻게 됐어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/6/expressions/21/practice-examples/be621062-76e4-4949-a64d-2a9ea30f9683.png","sentenceText":"I didn''t expect to hit it off with my roommate.","sentenceWords":["I","didn''t","expect","to","hit","it","off","with","my","roommate"],"highlightingPart":"hit it off","practiceQuestion":"How do you and your roommate get along?","sentenceTranslation":"룸메이트랑 이렇게 잘 맞을 줄 몰랐어.","sentenceWordChoices":["to","clicked","along","with","it","roommate","off","bonded","didn''t","expect","hit","my","I"],"practiceQuestionTranslation":"룸메이트랑 잘 지내?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/6/expressions/21/practice-examples/2a29c32d-1832-40ec-849c-122e3a86d081.png","sentenceText":"The two kids hit it off immediately.","sentenceWords":["The","two","kids","hit","it","off","immediately"],"highlightingPart":"hit it off","practiceQuestion":"How was the playdate?","sentenceTranslation":"두 애들이 바로 친해졌어.","sentenceWordChoices":["along","clicked","off","kids","hit","two","it","bonded","immediately","The"],"practiceQuestionTranslation":"애들 놀이 약속 어땠어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/6/expressions/21/practice-examples/f6e2c198-0a0f-4027-9541-cbcffce5a004.png","sentenceText":"Did you hit it off with her friends?","sentenceWords":["Did","you","hit","it","off","with","her","friends"],"highlightingPart":"hit it off","practiceQuestion":"How was hanging out with her friends?","sentenceTranslation":"걔 친구들이랑은 잘 맞았어?","sentenceWordChoices":["hit","with","her","along","off","friends","bonded","you","clicked","it","Did"],"practiceQuestionTranslation":"걔 친구들이랑 노는 거 어땠어?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 21
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        22,
        6,
        'SCENARIO',
        'OPINION_JUDGMENT',
        'BASIC',
        'EN',
        'KR',
        2,
        'be (not) for me',
        '~은 내 취향이다/아니다',
        '취향이 아니라고 말할 때 be (not) for me',
        '''~은 내 취향이야/아니야''를 말할 때 원어민들은 전치사 for를 써서 간단하게 표현합니다. It''s not for me 한마디면 무언가가 나와 맞지 않는다는 뉘앙스를 부드럽게 전달할 수 있어요.',
        'Are you into parties and stuff?',
        '넌 파티 같은 거 좋아해?',
        'Honestly, big parties aren''t really for me.',
        '솔직히 큰 파티는 내 취향은 아니야.',
        ARRAY['Honestly', 'big', 'parties', 'aren''t', 'really', 'for', 'me'],
        ARRAY['big', 'isn''t', 'parties', 'to', 'for', 'aren''t', 'Honestly', 'mine', 'me', 'really'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/6/expressions/22/practice-examples/d74b1b31-a402-4c0f-9365-6d8b12c8fdd0.png","sentenceText":"Camping just isn''t for me.","sentenceWords":["Camping","just","isn''t","for","me"],"highlightingPart":"isn''t for me","practiceQuestion":"Want to go camping this weekend?","sentenceTranslation":"캠핑은 그냥 나랑 안 맞아.","sentenceWordChoices":["thing","isn''t","me","just","for","Camping","style","suits"],"practiceQuestionTranslation":"이번 주말에 캠핑 갈래?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/6/expressions/22/practice-examples/3d7525b9-e048-4a57-860e-f69720d18825.png","sentenceText":"I tried yoga, but it wasn''t for me.","sentenceWords":["I","tried","yoga","but","it","wasn''t","for","me"],"highlightingPart":"wasn''t for me","practiceQuestion":"How was the yoga class?","sentenceTranslation":"요가 해봤는데 나랑은 안 맞더라.","sentenceWordChoices":["tried","it","style","thing","me","suits","yoga","wasn''t","for","but","I"],"practiceQuestionTranslation":"요가 수업 어땠어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/6/expressions/22/practice-examples/c31c2bbc-dfa3-4a80-9395-c92ac4bce52c.png","sentenceText":"City life is definitely for me.","sentenceWords":["City","life","is","definitely","for","me"],"highlightingPart":"is definitely for me","practiceQuestion":"Do you like living in the city?","sentenceTranslation":"도시 생활이 확실히 나한테 맞아.","sentenceWordChoices":["for","me","suits","is","life","style","City","definitely","thing"],"practiceQuestionTranslation":"도시에서 사는 거 좋아?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/6/expressions/22/practice-examples/5985a398-9b52-47c8-93a8-86a3223be5ed.png","sentenceText":"This kind of job isn''t for everyone.","sentenceWords":["This","kind","of","job","isn''t","for","everyone"],"highlightingPart":"isn''t for everyone","practiceQuestion":"Would you ever work night shifts?","sentenceTranslation":"이런 일은 아무나 하는 게 아니야.","sentenceWordChoices":["kind","style","for","thing","job","of","everyone","isn''t","suits","This"],"practiceQuestionTranslation":"야간 근무 해볼 생각 있어?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 22
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        23,
        6,
        'SCENARIO',
        'TIME_PLANNING',
        'BASIC',
        'EN',
        'KR',
        3,
        'I was thinking of ~ing',
        '~할까 생각 중이었어',
        '고민 중인 생각을 말하는 I was thinking of ~ing',
        '''~할까 생각 중이었어''라고 아직 확정되지 않은 계획이나 고민을 부드럽게 꺼낼 때 쓰는 표현입니다. 단정적이지 않아서 상대의 의견을 듣고 싶을 때 특히 유용해요.',
        'What would you make for the potluck?',
        '포틀럭에 뭐 만들어 올 거야?',
        'I was thinking of making some Korean fried chicken.',
        '한국식 프라이드치킨 만들까 생각 중이야.',
        ARRAY['I', 'was', 'thinking', 'of', 'making', 'some', 'Korean', 'fried', 'chicken'],
        ARRAY['Korean', 'fried', 'think', 'I', 'making', 'of', 'some', 'made', 'thinking', 'was', 'chicken', 'to'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/6/expressions/23/practice-examples/104c95d7-d0dd-4b23-8bd8-0a4c0624b253.png","sentenceText":"I was thinking of taking a trip next month.","sentenceWords":["I","was","thinking","of","taking","a","trip","next","month"],"highlightingPart":"was thinking of","practiceQuestion":"Any plans for next month?","sentenceTranslation":"다음 달에 여행 갈까 생각 중이야.","sentenceWordChoices":["considering","of","taking","a","next","trip","thinking","planning","I","was","think","month"],"practiceQuestionTranslation":"다음 달에 계획 있어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/6/expressions/23/practice-examples/7c73368c-f143-4e6b-955f-c0c71bc6b888.png","sentenceText":"I was thinking of learning Spanish.","sentenceWords":["I","was","thinking","of","learning","Spanish"],"highlightingPart":"was thinking of","practiceQuestion":"Are you learning any new languages?","sentenceTranslation":"스페인어 배워볼까 생각 중이었어.","sentenceWordChoices":["learning","was","considering","thinking","I","of","think","planning","Spanish"],"practiceQuestionTranslation":"새로운 언어 배우는 거 있어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/6/expressions/23/practice-examples/637581da-24bf-48aa-b5f1-be2045b2e105.png","sentenceText":"We were thinking of moving to a bigger place.","sentenceWords":["We","were","thinking","of","moving","to","a","bigger","place"],"highlightingPart":"were thinking of","practiceQuestion":"Is your apartment getting too small?","sentenceTranslation":"우리 더 큰 집으로 이사 갈까 생각 중이었어.","sentenceWordChoices":["thinking","place","think","to","considering","planning","a","bigger","moving","were","We","of"],"practiceQuestionTranslation":"너희 집 너무 좁아지는 거 같아?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/6/expressions/23/practice-examples/532f3be5-501c-443e-9737-a22fdc09dd72.png","sentenceText":"I''m thinking of quitting my job.","sentenceWords":["I''m","thinking","of","quitting","my","job"],"highlightingPart":"''m thinking of","practiceQuestion":"How''s work going lately?","sentenceTranslation":"일 그만둘까 고민 중이야.","sentenceWordChoices":["think","my","planning","thinking","quitting","I''m","considering","of","job"],"practiceQuestionTranslation":"요즘 일은 어때?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 23
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        24,
        6,
        'SCENARIO',
        'OPINION_JUDGMENT',
        'BASIC',
        'EN',
        'KR',
        4,
        'can''t go wrong with',
        '~을 고르면 실패가 없다',
        '실패 없는 선택 can''t go wrong with',
        '''~을 선택하면 실패할 일이 없다''라며 안전하고 확실한 선택을 추천하는 표현입니다. 메뉴, 선물, 옷 색깔 등 뭘 골라야 할지 모를 때 쓰기 좋아요.',
        'Do you think people will like Korean food?',
        '사람들이 한국 음식 좋아할까?',
        'You can''t go wrong with Korean fried chicken.',
        '한국식 프라이드치킨이면 실패 없지.',
        ARRAY['You', 'can''t', 'go', 'wrong', 'with', 'Korean', 'fried', 'chicken'],
        ARRAY['went', 'You', 'with', 'Korean', 'wrong', 'fried', 'go', 'chicken', 'right', 'for', 'can''t'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/6/expressions/24/practice-examples/2038c4ce-ac35-41f9-9f5d-bab16200c661.png","sentenceText":"You can''t go wrong with a white shirt.","sentenceWords":["You","can''t","go","wrong","with","a","white","shirt"],"highlightingPart":"can''t go wrong with","practiceQuestion":"What color shirt should I get?","sentenceTranslation":"흰 셔츠는 실패가 없어.","sentenceWordChoices":["beat","can''t","wrong","safe","white","with","You","shirt","go","went","a"],"practiceQuestionTranslation":"무슨 색 셔츠 살까?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/6/expressions/24/practice-examples/4acca126-5b11-4651-bbcd-60627a72929b.png","sentenceText":"For a gift, you can''t go wrong with flowers.","sentenceWords":["For","a","gift","you","can''t","go","wrong","with","flowers"],"highlightingPart":"can''t go wrong with","practiceQuestion":"What should I get her for her birthday?","sentenceTranslation":"선물로는 꽃이면 무난하지.","sentenceWordChoices":["For","wrong","gift","you","can''t","go","safe","flowers","with","beat","went","a"],"practiceQuestionTranslation":"걔 생일 선물로 뭐 사줄까?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/6/expressions/24/practice-examples/54b4db11-59ca-4a52-8cfd-545dae99b70b.png","sentenceText":"You can''t go wrong with this brand.","sentenceWords":["You","can''t","go","wrong","with","this","brand"],"highlightingPart":"can''t go wrong with","practiceQuestion":"Which brand of laptop should I buy?","sentenceTranslation":"이 브랜드는 믿고 사도 돼.","sentenceWordChoices":["wrong","can''t","safe","You","beat","with","this","went","brand","go"],"practiceQuestionTranslation":"노트북 어느 브랜드 사야 할까?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/6/expressions/24/practice-examples/584fb7f3-c2cf-43b7-abd6-7d6a80c46035.png","sentenceText":"When in doubt, you can''t go wrong with the classics.","sentenceWords":["When","in","doubt","you","can''t","go","wrong","with","the","classics"],"highlightingPart":"can''t go wrong with","practiceQuestion":"I can''t decide what to wear tonight.","sentenceTranslation":"고민될 땐 클래식한 걸 고르면 실패 없어.","sentenceWordChoices":["you","doubt","with","went","in","go","classics","When","can''t","wrong","the","safe","beat"],"practiceQuestionTranslation":"오늘 뭐 입을지 못 정하겠어."}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 24
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        25,
        7,
        'SCENARIO',
        'RELATIONSHIP_SOCIAL',
        'BASIC',
        'EN',
        'KR',
        1,
        'get along',
        '사이좋게 지내다',
        '사이가 좋은 get along',
        '''사이좋게 지내다'', ''잘 어울리다''라는 뜻으로 인간관계를 표현하는 핵심 구동사입니다. get along with 뒤에 상대를 붙이면 ''~와 잘 지내다''가 돼요.',
        'Do you get along with your siblings?',
        '형제자매랑 사이 좋아?',
        'I get along really well with my older sister.',
        '난 언니랑 진짜 사이좋아.',
        ARRAY['I', 'get', 'along', 'really', 'well', 'with', 'my', 'older', 'sister'],
        ARRAY['on', 'I', 'sister', 'my', 'gets', 'good', 'along', 'with', 'get', 'well', 'older', 'really'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/7/expressions/25/practice-examples/67289a36-b9ba-410d-806d-338d5bb695bb.png","sentenceText":"My kids get along really well.","sentenceWords":["My","kids","get","along","really","well"],"highlightingPart":"get along","practiceQuestion":"Do your kids fight a lot?","sentenceTranslation":"우리 애들은 사이가 진짜 좋아.","sentenceWordChoices":["friendly","get","well","My","click","along","bond","kids","really"],"practiceQuestionTranslation":"너희 애들 많이 싸워?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/7/expressions/25/practice-examples/a6ecf85e-110d-491d-ba9c-3c0b2f34d902.png","sentenceText":"I don''t get along with my roommate.","sentenceWords":["I","don''t","get","along","with","my","roommate"],"highlightingPart":"get along","practiceQuestion":"How''s living with your roommate?","sentenceTranslation":"난 룸메이트랑 잘 안 맞아.","sentenceWordChoices":["my","I","don''t","roommate","get","with","click","along","friendly","bond"],"practiceQuestionTranslation":"룸메이트랑 사는 거 어때?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/7/expressions/25/practice-examples/d6b60610-d9e4-45c4-afaa-20038116dece.png","sentenceText":"She gets along with everyone.","sentenceWords":["She","gets","along","with","everyone"],"highlightingPart":"gets along","practiceQuestion":"Is she popular at school?","sentenceTranslation":"걔는 누구하고나 잘 지내.","sentenceWordChoices":["with","friendly","along","click","She","everyone","gets","bond"],"practiceQuestionTranslation":"걔 학교에서 인기 많아?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/7/expressions/25/practice-examples/fbdafeb0-53b5-436c-8cf4-857cb1805ce4.png","sentenceText":"We didn''t get along at first, but now we''re close.","sentenceWords":["We","didn''t","get","along","at","first","but","now","we''re","close"],"highlightingPart":"get along","practiceQuestion":"Were you and Charlie close from the start?","sentenceTranslation":"처음엔 안 맞았는데 지금은 친해.","sentenceWordChoices":["click","close","get","we''re","didn''t","but","We","bond","now","at","along","first","friendly"],"practiceQuestionTranslation":"너랑 Charlie 처음부터 친했어?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 25
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        26,
        7,
        'SCENARIO',
        'EMOTION_EMPATHY',
        'BASIC',
        'EN',
        'KR',
        2,
        'look forward to',
        '~을 기대하다',
        '기대하는 일을 말하는 look forward to',
        '''~을 기대하다, 고대하다''라는 뜻으로, 특히 이메일 마무리 인사로도 유명한 표현입니다. to 뒤에 동사원형이 아니라 명사/동명사가 온다는 게 최대 함정 포인트예요.',
        'What''s your big dream?',
        '넌 꿈이 뭐야?',
        'I''m really looking forward to studying abroad someday.',
        '언젠가 유학 가는 걸 정말 기대하고 있어.',
        ARRAY['I''m', 'really', 'looking', 'forward', 'to', 'studying', 'abroad', 'someday'],
        ARRAY['I''m', 'look', 'looking', 'really', 'abroad', 'for', 'someday', 'studying', 'study', 'to', 'forward'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/7/expressions/26/practice-examples/c466061e-6541-45e4-808e-ea1da64dc36c.png","sentenceText":"I''m looking forward to the concert.","sentenceWords":["I''m","looking","forward","to","the","concert"],"highlightingPart":"looking forward to","practiceQuestion":"Excited for anything coming up?","sentenceTranslation":"콘서트 너무 기대돼.","sentenceWordChoices":["excited","concert","to","forward","looking","the","expect","waiting","I''m"],"practiceQuestionTranslation":"다가오는 것 중에 기대되는 거 있어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/7/expressions/26/practice-examples/8360be25-4742-484b-bd27-fd4eeb782b70.png","sentenceText":"She''s looking forward to her vacation.","sentenceWords":["She''s","looking","forward","to","her","vacation"],"highlightingPart":"looking forward to","practiceQuestion":"When is her vacation?","sentenceTranslation":"걔 휴가를 엄청 기대하고 있어.","sentenceWordChoices":["forward","She''s","excited","her","waiting","to","expect","vacation","looking"],"practiceQuestionTranslation":"걔 휴가 언제야?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/7/expressions/26/practice-examples/d6fd178b-10d3-4519-bc8b-0b8d32b935e9.png","sentenceText":"We look forward to working with you.","sentenceWords":["We","look","forward","to","working","with","you"],"highlightingPart":"look forward to","practiceQuestion":"We''re excited to start this project together.","sentenceTranslation":"함께 일하게 되기를 기대합니다.","sentenceWordChoices":["excited","forward","with","waiting","to","working","look","We","you","expect"],"practiceQuestionTranslation":"같이 이 프로젝트 시작하게 돼서 기뻐요."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/7/expressions/26/practice-examples/d57ff307-937f-413d-8ef2-7bf5198847b7.png","sentenceText":"I look forward to your reply.","sentenceWords":["I","look","forward","to","your","reply"],"highlightingPart":"look forward to","practiceQuestion":"I''ll send you my answer by email.","sentenceTranslation":"답장 기다리겠습니다.","sentenceWordChoices":["waiting","I","excited","look","forward","to","your","expect","reply"],"practiceQuestionTranslation":"이메일로 답변 보내드릴게요."}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 26
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        27,
        7,
        'SCENARIO',
        'OPINION_JUDGMENT',
        'BASIC',
        'EN',
        'KR',
        3,
        'be interested in',
        '~에 관심이 있다',
        '관심 여부를 말하는 be interested in',
        '''~에 관심이 있다''라는 기본 표현이지만, not really interested in처럼 부정형으로 정중히 거절할 때도 아주 유용합니다. in 뒤에는 명사나 동명사가 와요.',
        'What made you pick your major?',
        '왜 그 전공을 선택했어?',
        'I''ve always been interested in psychology.',
        '난 항상 심리학에 관심이 있었어.',
        ARRAY['I''ve', 'always', 'been', 'interested', 'in', 'psychology'],
        ARRAY['been', 'in', 'interesting', 'be', 'I''ve', 'always', 'on', 'interested', 'psychology'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/7/expressions/27/practice-examples/78866a28-2ffd-444b-be31-63612cf72be9.png","sentenceText":"Are you interested in joining our club?","sentenceWords":["Are","you","interested","in","joining","our","club"],"highlightingPart":"interested in","practiceQuestion":"We''re starting a new club this semester.","sentenceTranslation":"우리 동아리 들어올 생각 있어?","sentenceWordChoices":["joining","our","interested","keen","curious","club","you","in","Are","like"],"practiceQuestionTranslation":"이번 학기에 새 동아리 만들 거야."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/7/expressions/27/practice-examples/4e383722-9925-4f77-869e-c1025aad9c94.png","sentenceText":"I''m not really interested in sports.","sentenceWords":["I''m","not","really","interested","in","sports"],"highlightingPart":"''m not really interested in","practiceQuestion":"Do you like sports?","sentenceTranslation":"난 스포츠에는 별로 관심 없어.","sentenceWordChoices":["curious","keen","like","in","I''m","interested","not","sports","really"],"practiceQuestionTranslation":"스포츠 좋아해?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/7/expressions/27/practice-examples/e03a98ff-be08-4026-8956-7ccd71b42f98.png","sentenceText":"She''s interested in learning Korean.","sentenceWords":["She''s","interested","in","learning","Korean"],"highlightingPart":"''s interested in","practiceQuestion":"Why is she taking Korean lessons?","sentenceTranslation":"걔 한국어 배우는 데 관심 있어.","sentenceWordChoices":["learning","She''s","Korean","curious","like","interested","in","keen"],"practiceQuestionTranslation":"걔 왜 한국어 수업 들어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/7/expressions/27/practice-examples/006b7308-74f4-432a-9e5b-229b576fd53a.png","sentenceText":"Thanks, but I''m not interested.","sentenceWords":["Thanks","but","I''m","not","interested"],"highlightingPart":"''m not interested","practiceQuestion":"Would you like to buy some cookies?","sentenceTranslation":"감사하지만 관심 없어요.","sentenceWordChoices":["interested","curious","I''m","but","keen","Thanks","like","not"],"practiceQuestionTranslation":"쿠키 좀 사시겠어요?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 27
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        28,
        7,
        'SCENARIO',
        'EMOTION_EMPATHY',
        'BASIC',
        'EN',
        'KR',
        4,
        'have an off day',
        '유난히 안 풀리는 날을 보내다',
        '되는 일이 없는 날 have an off day',
        '''유난히 안 풀리는 날을 보내다''라는 뜻입니다. off가 ''평소 같지 않은, 컨디션이 떨어진'' 상태를 나타내서, 실수가 잦거나 기운이 없는 날에 딱이에요.',
        'You''ve seemed kinda off lately — everything okay?',
        '너 요즘 좀 기운 없어 보여 — 다 괜찮아?',
        'Yeah, I''ve just been having a lot of off days.',
        '응, 그냥 요즘 안 풀리는 날이 많았어.',
        ARRAY['Yeah', 'I''ve', 'just', 'been', 'having', 'a', 'lot', 'of', 'off', 'days'],
        ARRAY['have', 'lot', 'off', 'I''ve', 'on', 'Yeah', 'a', 'just', 'lots', 'been', 'of', 'having', 'days'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/7/expressions/28/practice-examples/048b6849-fdc9-426a-87a0-7453ba3d65c5.png","sentenceText":"Everyone has an off day sometimes.","sentenceWords":["Everyone","has","an","off","day","sometimes"],"highlightingPart":"has an off day","practiceQuestion":"I made so many mistakes today.","sentenceTranslation":"누구나 가끔 안 풀리는 날이 있지.","sentenceWordChoices":["Everyone","has","sometimes","off","slow","an","rough","day","tough"],"practiceQuestionTranslation":"오늘 실수를 너무 많이 했어."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/7/expressions/28/practice-examples/ee30ef07-19e3-4f1f-b695-530fb6bfccc4.png","sentenceText":"She''s usually great, but she had an off day.","sentenceWords":["She''s","usually","great","but","she","had","an","off","day"],"highlightingPart":"had an off day","practiceQuestion":"Why did she miss those easy shots?","sentenceTranslation":"걔 원래 잘하는데 그날따라 컨디션이 안 좋았어.","sentenceWordChoices":["tough","usually","an","off","slow","rough","great","she","had","day","She''s","but"],"practiceQuestionTranslation":"걔 왜 그런 쉬운 슛을 놓쳤대?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/7/expressions/28/practice-examples/7872b012-a3c3-4b31-b2de-af3a5365234b.png","sentenceText":"I''m just having an off day. Don''t mind me.","sentenceWords":["I''m","just","having","an","off","day","Don''t","mind","me"],"highlightingPart":"having an off day","practiceQuestion":"You seem a little off today.","sentenceTranslation":"그냥 오늘 컨디션이 별로야. 신경 쓰지 마.","sentenceWordChoices":["tough","slow","I''m","rough","off","just","an","Don''t","day","mind","having","me"],"practiceQuestionTranslation":"너 오늘 좀 이상해 보여."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/7/expressions/28/practice-examples/b743872d-46a3-4f60-8eeb-bb9d8e50e7e3.png","sentenceText":"Even pros have off days.","sentenceWords":["Even","pros","have","off","days"],"highlightingPart":"have off days","practiceQuestion":"She''s usually great. What happened?","sentenceTranslation":"프로들도 안 되는 날이 있어.","sentenceWordChoices":["off","slow","days","rough","have","tough","Even","pros"],"practiceQuestionTranslation":"걔 원래 잘하잖아. 무슨 일이야?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 28
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        29,
        7,
        'SCENARIO',
        'RELATIONSHIP_SOCIAL',
        'BASIC',
        'EN',
        'KR',
        5,
        'open up',
        '마음을 열다, 털어놓다',
        '속마음을 터놓는 open up',
        '''마음을 열다, 속마음을 털어놓다''라는 뜻입니다. open up to someone은 ''~에게 솔직하게 털어놓다''로, 깊은 대화를 청할 때 쓰는 따뜻한 표현이에요.',
        'You know you can talk to me, right?',
        '나한테 얘기해도 되는 거 알지?',
        'Thanks. It''s just hard for me to open up sometimes.',
        '고마워. 근데 가끔 속마음 털어놓는 게 좀 어려워서.',
        ARRAY['Thanks', 'It''s', 'just', 'hard', 'for', 'me', 'to', 'open', 'up', 'sometimes'],
        ARRAY['up', 'me', 'opens', 'for', 'down', 'sometimes', 'Thanks', 'at', 'open', 'hard', 'It''s', 'to', 'just'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/7/expressions/29/practice-examples/b12b6d35-2e8c-49c1-bfb8-88c801370519.png","sentenceText":"She finally opened up about her worries.","sentenceWords":["She","finally","opened","up","about","her","worries"],"highlightingPart":"opened up","practiceQuestion":"Did she ever tell you what''s wrong?","sentenceTranslation":"걔가 드디어 고민을 털어놨어.","sentenceWordChoices":["her","share","up","finally","worries","confide","opened","about","vent","She"],"practiceQuestionTranslation":"걔가 뭐가 문제인지 말해줬어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/7/expressions/29/practice-examples/9b34a090-877a-4916-a022-34e1e9c0dc5d.png","sentenceText":"You can open up to me anytime.","sentenceWords":["You","can","open","up","to","me","anytime"],"highlightingPart":"open up","practiceQuestion":"I don''t know if I should tell anyone.","sentenceTranslation":"언제든 나한테 털어놔도 돼.","sentenceWordChoices":["open","vent","can","me","anytime","up","confide","to","You","share"],"practiceQuestionTranslation":"누구한테 말해야 할지 모르겠어."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/7/expressions/29/practice-examples/ce60c5e0-feab-44ea-9846-20cfd3b67693.png","sentenceText":"He opened up to his therapist.","sentenceWords":["He","opened","up","to","his","therapist"],"highlightingPart":"opened up","practiceQuestion":"Is he seeing a therapist now?","sentenceTranslation":"걔는 상담사에게 속마음을 털어놨어.","sentenceWordChoices":["confide","share","up","vent","He","his","to","therapist","opened"],"practiceQuestionTranslation":"걔 요즘 상담받고 있어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/7/expressions/29/practice-examples/3c0a0af3-1f7d-4ddf-a70f-973cd5409e1c.png","sentenceText":"It takes time to open up to new people.","sentenceWords":["It","takes","time","to","open","up","to","new","people"],"highlightingPart":"open up","practiceQuestion":"I''m still kind of shy around new people.","sentenceTranslation":"새로운 사람에게 마음을 여는 덴 시간이 걸려.","sentenceWordChoices":["It","takes","share","open","to","confide","vent","new","time","people","up","to"],"practiceQuestionTranslation":"난 아직 새로운 사람들 앞에서 좀 낯가려."}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 29
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        62,
        8,
        'SCENARIO',
        'EMOTION_EMPATHY',
        'CLASSIC_COMMON',
        'EN',
        'KR',
        1,
        'in the same boat',
        '같은 처지다',
        '같은 처지임을 말하는 in the same boat',
        '''같은 배를 탔다'', 즉 ''같은 처지다''라는 관용 표현입니다. 어려운 상황을 함께 겪고 있다는 동질감과 위로를 전할 때 딱이에요.',
        'Do you know anyone else in this class? I''m kinda worried.',
        '이 수업 같이 듣는 친구 있어? 나 걱정이 좀 돼.',
        'We''re in the same boat — I don''t know anyone here either.',
        '우리 같은 처지네. 나도 여기 아는 사람 없어.',
        ARRAY['We''re', 'in', 'the', 'same', 'boat', 'I', 'don''t', 'know', 'anyone', 'here', 'either'],
        ARRAY['someone', 'same', 'don''t', 'anyone', 'here', 'I', 'know', 'in', 'the', 'boat', 'boats', 'either', 'We''re', 'too'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/8/expressions/62/practice-examples/0dba7b9f-9ba2-45c1-9488-0b2f3a0605cb.png","sentenceText":"I failed too. We''re in the same boat.","sentenceWords":["I","failed","too","We''re","in","the","same","boat"],"highlightingPart":"in the same boat","practiceQuestion":"I can''t believe I failed.","sentenceTranslation":"나도 떨어졌어. 우리 같은 신세네.","sentenceWordChoices":["the","same","seat","ship","too","boat","boats","in","We''re","I","failed"],"practiceQuestionTranslation":"내가 떨어졌다니 믿기지가 않아."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/8/expressions/62/practice-examples/7b7dae3e-0ab7-4bf3-8102-6af9b21be6f1.png","sentenceText":"Everyone at work is in the same boat with the new system.","sentenceWords":["Everyone","at","work","is","in","the","same","boat","with","the","new","system"],"highlightingPart":"in the same boat","practiceQuestion":"Ugh, this new system is so confusing.","sentenceTranslation":"새 시스템 때문에 회사 사람들 다 같은 처지야.","sentenceWordChoices":["at","seat","Everyone","in","with","the","is","the","same","work","new","boat","boats","ship","system"],"practiceQuestionTranslation":"아 이 새 시스템 진짜 헷갈려."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/8/expressions/62/practice-examples/463d4c89-67a9-4713-9b69-53b06f6217ad.png","sentenceText":"You can''t sleep either? We''re in the same boat.","sentenceWords":["You","can''t","sleep","either","We''re","in","the","same","boat"],"highlightingPart":"in the same boat","practiceQuestion":"I can''t sleep at all these days.","sentenceTranslation":"너도 잠이 안 와? 우리 똑같네.","sentenceWordChoices":["boat","You","ship","sleep","the","seat","same","either","We''re","can''t","in","boats"],"practiceQuestionTranslation":"나 요즘 통 잠을 못 자."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/8/expressions/62/practice-examples/1c6342ee-cfd9-4fa8-a22c-659520b6f6bd.png","sentenceText":"As new parents, we''re all in the same boat.","sentenceWords":["As","new","parents","we''re","all","in","the","same","boat"],"highlightingPart":"in the same boat","practiceQuestion":"It''s so hard being a new parent.","sentenceTranslation":"초보 부모로서 우리 다 같은 배를 탄 거지.","sentenceWordChoices":["the","As","same","in","we''re","parents","all","boat","boats","ship","new","seat"],"practiceQuestionTranslation":"초보 부모 노릇이 너무 힘들어."}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 62
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        63,
        8,
        'SCENARIO',
        'WORK_STUDY',
        'BASIC',
        'EN',
        'KR',
        2,
        'figure out',
        '알아내다, 이해하다',
        '만능 구동사 figure out',
        '''알아내다'', ''이해하다'', ''해결책을 찾다''까지 커버하는 활용도 최고의 구동사입니다. 머리를 써서 답이나 방법을 찾아낸다는 뉘앙스가 핵심이에요.',
        'What if the group project gets complicated?',
        '조별 과제가 복잡해지면 어떡해?',
        'Don''t worry, we''ll figure it out together.',
        '걱정 마, 우리가 같이 방법을 찾으면 돼.',
        ARRAY['Don''t', 'worry', 'we''ll', 'figure', 'it', 'out', 'together'],
        ARRAY['out', 'it', 'worried', 'figure', 'Don''t', 'figures', 'worry', 'we''ll', 'together', 'them'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/8/expressions/63/practice-examples/d2cc7ef6-a73d-4d96-9e87-8fc4d82b7143.png","sentenceText":"I can''t figure out how to use this app.","sentenceWords":["I","can''t","figure","out","how","to","use","this","app"],"highlightingPart":"figure out","practiceQuestion":"Why isn''t this app working?","sentenceTranslation":"이 앱 어떻게 쓰는지 모르겠어.","sentenceWordChoices":["how","this","I","to","app","can''t","solve","out","figure","work","use","figured"],"practiceQuestionTranslation":"이 앱 왜 안 되지?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/8/expressions/63/practice-examples/a92bfbde-aa2c-4ccd-8d5d-f0d0a3d165cc.png","sentenceText":"Did you figure out what was wrong?","sentenceWords":["Did","you","figure","out","what","was","wrong"],"highlightingPart":"figure out","practiceQuestion":"The car wouldn''t start yesterday.","sentenceTranslation":"뭐가 문제였는지 알아냈어?","sentenceWordChoices":["wrong","you","was","solve","figure","Did","figured","out","what","work"],"practiceQuestionTranslation":"어제 차가 시동이 안 걸렸어."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/8/expressions/63/practice-examples/976e0b31-dba0-43b9-92de-cf5f5eda3e30.png","sentenceText":"We need to figure out a plan.","sentenceWords":["We","need","to","figure","out","a","plan"],"highlightingPart":"figure out","practiceQuestion":"The trip is only two weeks away and we have nothing booked.","sentenceTranslation":"우리 계획을 짜내야 해.","sentenceWordChoices":["out","to","figured","work","need","a","figure","solve","plan","We"],"practiceQuestionTranslation":"여행이 2주밖에 안 남았는데 예약한 게 하나도 없어."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/8/expressions/63/practice-examples/eab7b237-6c37-4f42-840c-1f12339f81a1.png","sentenceText":"It took me an hour to figure out the directions.","sentenceWords":["It","took","me","an","hour","to","figure","out","the","directions"],"highlightingPart":"figure out","practiceQuestion":"How did you find our place? Did you get lost?","sentenceTranslation":"길 파악하는 데 한 시간 걸렸어.","sentenceWordChoices":["work","the","It","to","solve","hour","out","me","figure","figured","an","directions","took"],"practiceQuestionTranslation":"우리 집 어떻게 찾아왔어? 길 잃었어?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 63
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        64,
        8,
        'SCENARIO',
        'WORK_STUDY',
        'BASIC',
        'EN',
        'KR',
        3,
        'keep track of',
        '~을 계속 파악하다',
        '놓치지 않고 파악하는 keep track of',
        '변하는 정보나 상태를 계속 기록하고 파악한다는 뜻입니다. 단순히 아는(know) 게 아니라 지속적으로 챙겨서 놓치지 않는다는 게 핵심이에요.',
        'I''m worried about all the assignments.',
        '과제들 때문에 걱정돼.',
        'I use a planner to keep track of all the deadlines.',
        '난 마감일 관리하려고 플래너를 써.',
        ARRAY['I', 'use', 'a', 'planner', 'to', 'keep', 'track', 'of', 'all', 'the', 'deadlines'],
        ARRAY['deadlines', 'tracks', 'to', 'track', 'all', 'the', 'keeps', 'a', 'used', 'use', 'of', 'keep', 'I', 'planner'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/8/expressions/64/practice-examples/0f307be5-a723-499d-92a4-91fe7adf1558.png","sentenceText":"It''s hard to keep track of all my passwords.","sentenceWords":["It''s","hard","to","keep","track","of","all","my","passwords"],"highlightingPart":"keep track of","practiceQuestion":"Why do you use a password manager?","sentenceTranslation":"비밀번호를 다 기억하고 관리하기가 힘들어.","sentenceWordChoices":["hard","manage","record","keep","track","to","all","my","of","monitor","passwords","It''s"],"practiceQuestionTranslation":"왜 비밀번호 관리 앱을 써?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/8/expressions/64/practice-examples/6f6dbf45-7f03-465d-878c-9fcfad59ab19.png","sentenceText":"This app keeps track of your spending.","sentenceWords":["This","app","keeps","track","of","your","spending"],"highlightingPart":"keeps track of","practiceQuestion":"How do you manage your budget?","sentenceTranslation":"이 앱이 네 지출을 기록해 줘.","sentenceWordChoices":["monitor","app","keeps","your","manage","spending","of","track","This","record"],"practiceQuestionTranslation":"예산 관리 어떻게 해?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/8/expressions/64/practice-examples/dea358b3-3544-4239-a8b2-24ed8a561331.png","sentenceText":"It''s my job to keep track of the inventory.","sentenceWords":["It''s","my","job","to","keep","track","of","the","inventory"],"highlightingPart":"keep track of","practiceQuestion":"What exactly do you do at the warehouse?","sentenceTranslation":"재고 파악은 내 담당이야.","sentenceWordChoices":["job","to","track","inventory","my","monitor","of","record","manage","It''s","the","keep"],"practiceQuestionTranslation":"창고에서 정확히 무슨 일 해?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/8/expressions/64/practice-examples/d038e52b-a253-4e29-a553-bac67dd0dfa5.png","sentenceText":"Someone needs to keep track of the score.","sentenceWords":["Someone","needs","to","keep","track","of","the","score"],"highlightingPart":"keep track of","practiceQuestion":"Wait, who''s winning right now?","sentenceTranslation":"누군가는 점수를 계속 체크해야 해.","sentenceWordChoices":["of","track","keep","manage","monitor","to","record","needs","score","the","Someone"],"practiceQuestionTranslation":"잠깐, 지금 누가 이기고 있어?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 64
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        65,
        8,
        'SCENARIO',
        'WORK_STUDY',
        'BASIC',
        'EN',
        'KR',
        4,
        'do a good job of',
        '~을 잘 해내다',
        '실력을 인정해주는 do a good job of',
        '''~을 잘 해내다''라는 뜻으로, be good at이 능력 자체라면 이 표현은 실제로 해낸 결과에 초점이 있습니다. of 뒤에 동명사를 붙여 잘하는 일을 구체적으로 말해요.',
        'Was there a class you actually really enjoyed?',
        '재밌게 들었던 수업 있어?',
        'My history teacher did a good job of making class fun.',
        '우리 역사 선생님이 수업을 재밌게 참 잘하셨어.',
        ARRAY['My', 'history', 'teacher', 'did', 'a', 'good', 'job', 'of', 'making', 'class', 'fun'],
        ARRAY['work', 'history', 'did', 'good', 'does', 'class', 'teacher', 'job', 'make', 'a', 'making', 'fun', 'My', 'of'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/8/expressions/65/practice-examples/0e56758b-bf6b-4024-84d5-a10217b0c540.png","sentenceText":"You did a great job of organizing the event.","sentenceWords":["You","did","a","great","job","of","organizing","the","event"],"highlightingPart":"did a great job of","practiceQuestion":"The event went so smoothly today.","sentenceTranslation":"행사 준비 정말 잘했어.","sentenceWordChoices":["the","a","great","did","You","event","nicely","of","organizing","well","job","perfectly"],"practiceQuestionTranslation":"오늘 행사 진짜 매끄럽게 진행됐어."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/8/expressions/65/practice-examples/aeefa46f-0ead-4d21-98b9-a697c112d7f9.png","sentenceText":"He does a good job of staying calm under pressure.","sentenceWords":["He","does","a","good","job","of","staying","calm","under","pressure"],"highlightingPart":"does a good job of","practiceQuestion":"How does he handle stressful situations?","sentenceTranslation":"걔는 압박 속에서도 침착함을 잘 유지해.","sentenceWordChoices":["pressure","job","calm","staying","well","does","a","of","under","He","perfectly","nicely","good"],"practiceQuestionTranslation":"걔는 스트레스 상황을 어떻게 대처해?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/8/expressions/65/practice-examples/c1cf25a7-9773-4563-875b-453ab0eaec2d.png","sentenceText":"The movie does a good job of building tension.","sentenceWords":["The","movie","does","a","good","job","of","building","tension"],"highlightingPart":"does a good job of","practiceQuestion":"What did you think of the thriller?","sentenceTranslation":"그 영화는 긴장감을 잘 쌓아 올려.","sentenceWordChoices":["movie","of","tension","nicely","building","a","well","perfectly","job","The","does","good"],"practiceQuestionTranslation":"그 스릴러 영화 어땠어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/8/expressions/65/practice-examples/f5a8a652-c797-464d-b463-4a08147d777f.png","sentenceText":"They did a poor job of communicating the changes.","sentenceWords":["They","did","a","poor","job","of","communicating","the","changes"],"highlightingPart":"did a poor job of","practiceQuestion":"Did everyone know about the schedule change?","sentenceTranslation":"걔네는 변경 사항 공지를 제대로 못 했어.","sentenceWordChoices":["nicely","a","job","They","well","the","poor","changes","communicating","did","of","perfectly"],"practiceQuestionTranslation":"다들 일정 바뀐 거 알고 있었어?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 65
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        66,
        9,
        'SCENARIO',
        'WORK_STUDY',
        'SLANG_NEOLOGISM',
        'EN',
        'KR',
        1,
        'wing it',
        '준비 없이 즉흥으로 하다',
        '준비 없이 부딪히는 wing it',
        '''준비 없이 즉흥으로 하다'', ''대충 임기응변으로 때우다''라는 뜻의 슬랭입니다. 발표나 시험을 준비 못 했을 때 자조적으로 쓰기 딱 좋아요.',
        'Have you ever presented in English before?',
        '영어로 발표해 본 적 있어?',
        'Once — I didn''t prepare enough, so I just winged it.',
        '한 번. 준비를 충분히 못 해서 그냥 즉흥으로 했어.',
        ARRAY['Once', 'I', 'didn''t', 'prepare', 'enough', 'so', 'I', 'just', 'winged', 'it'],
        ARRAY['don''t', 'winged', 'just', 'it', 'wing', 'Once', 'enough', 'didn''t', 'prepare', 'I', 'so', 'I', 'prepared'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/9/expressions/66/practice-examples/1b5c1d79-a809-4e47-b04c-2fb0c407e1c6.png","sentenceText":"No script — I''m just going to wing it.","sentenceWords":["No","script","I''m","just","going","to","wing","it"],"highlightingPart":"wing it","practiceQuestion":"Do you have a script ready?","sentenceTranslation":"대본 없어. 그냥 즉흥으로 할 거야.","sentenceWordChoices":["fake","wing","I''m","to","just","bluff","it","going","script","improvise","No"],"practiceQuestionTranslation":"대본 준비돼 있어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/9/expressions/66/practice-examples/0dd2bccf-b1e9-4c57-ac7a-4b058e5d25e1.png","sentenceText":"He winged the interview and somehow got the job.","sentenceWords":["He","winged","the","interview","and","somehow","got","the","job"],"highlightingPart":"winged the interview","practiceQuestion":"Did he prepare a lot for the interview?","sentenceTranslation":"걔 면접을 즉흥으로 봤는데 어떻게 붙었어.","sentenceWordChoices":["got","winged","somehow","and","improvise","bluff","He","interview","the","fake","the","job"],"practiceQuestionTranslation":"걔 면접 준비 많이 했대?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/9/expressions/66/practice-examples/4423fa7b-f399-4f62-9082-5e8fafb44949.png","sentenceText":"Don''t wing it this time. Actually study.","sentenceWords":["Don''t","wing","it","this","time","Actually","study"],"highlightingPart":"wing it","practiceQuestion":"I''ll probably just wing the exam again.","sentenceTranslation":"이번엔 벼락치기로 때우지 말고 진짜 공부해.","sentenceWordChoices":["this","Don''t","Actually","bluff","time","it","fake","improvise","wing","study"],"practiceQuestionTranslation":"이번에도 그냥 벼락치기로 때울까 봐."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/9/expressions/66/practice-examples/f54bcb2f-d34c-4223-ab8a-6ed94cd32591.png","sentenceText":"We didn''t book anything. We''re winging the whole trip.","sentenceWords":["We","didn''t","book","anything","We''re","winging","the","whole","trip"],"highlightingPart":"winging the whole trip","practiceQuestion":"What''s the itinerary for your trip?","sentenceTranslation":"아무것도 예약 안 했어. 여행 전체를 즉흥으로 다니는 중이야.","sentenceWordChoices":["fake","whole","improvise","bluff","anything","We''re","book","didn''t","winging","We","trip","the"],"practiceQuestionTranslation":"여행 일정이 어떻게 돼?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 66
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        67,
        9,
        'SCENARIO',
        'WORK_STUDY',
        'BASIC',
        'EN',
        'KR',
        2,
        'be cut out for',
        '~에 적합하다, ~할 재목이다',
        '적성에 안 맞을 때 be cut out for',
        '''~에 적합하다, ~할 재목이다''라는 뜻으로, 주로 부정형으로 ''적성에 안 맞는다''고 말할 때 씁니다. 타고난 기질과 안 맞는다는 뉘앙스가 있어요.',
        'What are you most confident about — presenting, research, or slides?',
        '발표, 자료조사, PPT 중에 뭐가 제일 자신 있어?',
        'I''m not cut out for presenting, so I''ll take the research.',
        '난 발표 체질이 아니라서 자료조사 맡을게.',
        ARRAY['I''m', 'not', 'cut', 'out', 'for', 'presenting', 'so', 'I''ll', 'take', 'the', 'research'],
        ARRAY['take', 'so', 'not', 'cut', 'I''m', 'out', 'research', 'for', 'I''ll', 'presenting', 'in', 'cuts', 'present', 'the'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/9/expressions/67/practice-examples/a5a67fc2-edf7-489c-a132-ddb5881270b6.png","sentenceText":"He''s not cut out for city life.","sentenceWords":["He''s","not","cut","out","for","city","life"],"highlightingPart":"not cut out for","practiceQuestion":"Would he like living downtown?","sentenceTranslation":"걔는 도시 생활이랑 안 맞아.","sentenceWordChoices":["out","fit","cuts","He''s","cutting","for","cut","not","life","city"],"practiceQuestionTranslation":"걔 도심 생활 좋아할까?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/9/expressions/67/practice-examples/f2636885-e96c-49fa-ac5d-1c2e1ed1a2b0.png","sentenceText":"I don''t think I''m cut out to be a teacher.","sentenceWords":["I","don''t","think","I''m","cut","out","to","be","a","teacher"],"highlightingPart":"cut out to be","practiceQuestion":"Have you thought about becoming a teacher?","sentenceTranslation":"난 선생님 할 재목은 아닌 것 같아.","sentenceWordChoices":["cuts","think","fit","be","I","out","to","I''m","a","cut","teacher","cutting","don''t"],"practiceQuestionTranslation":"선생님 되는 거 생각해본 적 있어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/9/expressions/67/practice-examples/cecef66c-3db8-4226-ba34-af49c8bcb81e.png","sentenceText":"She''s definitely cut out for leadership.","sentenceWords":["She''s","definitely","cut","out","for","leadership"],"highlightingPart":"cut out for","practiceQuestion":"Do you think she''d be a good team leader?","sentenceTranslation":"걔는 확실히 리더 체질이야.","sentenceWordChoices":["cut","definitely","cutting","leadership","out","She''s","fit","cuts","for"],"practiceQuestionTranslation":"걔가 팀장 잘할 것 같아?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/9/expressions/67/practice-examples/c4143920-898f-40cf-8619-5792c19ed1d1.png","sentenceText":"Not everyone is cut out for this kind of work.","sentenceWords":["Not","everyone","is","cut","out","for","this","kind","of","work"],"highlightingPart":"cut out for","practiceQuestion":"Why did so many people quit this job?","sentenceTranslation":"이런 일이 아무한테나 맞는 건 아니야.","sentenceWordChoices":["cuts","for","work","of","kind","Not","everyone","is","cut","fit","cutting","out","this"],"practiceQuestionTranslation":"이 일 왜 이렇게 많이들 그만둬?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 67
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        68,
        9,
        'SCENARIO',
        'WORK_STUDY',
        'BASIC',
        'EN',
        'KR',
        3,
        'handle',
        '감당하다, 처리하다',
        '''감당하다''를 말하는 handle',
        'handle은 ''다루다''를 넘어 ''감당하다, 처리하다''라는 의미로 자주 쓰입니다. nothing I can''t handle(내가 감당 못 할 건 없어)처럼 자신감을 표현하기 좋아요.',
        'Can you take the slides too, or is that too much?',
        'PPT까지 맡을 수 있어, 아님 너무 많아?',
        'Don''t worry, I can handle the slides too.',
        '걱정 마, PPT까지는 내가 감당할 수 있어.',
        ARRAY['Don''t', 'worry', 'I', 'can', 'handle', 'the', 'slides', 'too'],
        ARRAY['can', 'could', 'handles', 'I', 'either', 'handle', 'Don''t', 'worry', 'too', 'the', 'slides'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/9/expressions/68/practice-examples/9deeb784-da31-4b37-88c5-290487360bb4.png","sentenceText":"Can you handle the pressure?","sentenceWords":["Can","you","handle","the","pressure"],"highlightingPart":"handle","practiceQuestion":"This job gets really stressful sometimes.","sentenceTranslation":"그 압박감 감당할 수 있겠어?","sentenceWordChoices":["cope","you","control","Can","the","tackle","handle","pressure"],"practiceQuestionTranslation":"이 일이 가끔 진짜 스트레스가 심해."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/9/expressions/68/practice-examples/3666231d-565d-41af-8d1b-90bda2575aca.png","sentenceText":"She handled the situation really well.","sentenceWords":["She","handled","the","situation","really","well"],"highlightingPart":"handled","practiceQuestion":"How did she deal with that mess?","sentenceTranslation":"걔 그 상황을 정말 잘 처리했어.","sentenceWordChoices":["tackle","She","really","control","cope","well","the","situation","handled"],"practiceQuestionTranslation":"걔 그 난리를 어떻게 처리했어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/9/expressions/68/practice-examples/de5b9a60-2b92-4aa8-84cb-ec9507a4cb6f.png","sentenceText":"Don''t worry, I''ll handle the reservations.","sentenceWords":["Don''t","worry","I''ll","handle","the","reservations"],"highlightingPart":"handle","practiceQuestion":"Who''s going to book the restaurant?","sentenceTranslation":"걱정 마, 예약은 내가 처리할게.","sentenceWordChoices":["worry","control","Don''t","the","cope","tackle","handle","reservations","I''ll"],"practiceQuestionTranslation":"식당 예약은 누가 할 거야?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/9/expressions/68/practice-examples/c7093520-2e3a-407b-bdca-2b2e171f1d74.png","sentenceText":"This is too much for one person to handle.","sentenceWords":["This","is","too","much","for","one","person","to","handle"],"highlightingPart":"handle","practiceQuestion":"Can you finish all of this by yourself?","sentenceTranslation":"이건 한 사람이 감당하기엔 너무 많아.","sentenceWordChoices":["to","much","handle","for","This","one","tackle","control","person","cope","too","is"],"practiceQuestionTranslation":"이거 혼자서 다 끝낼 수 있어?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 68
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        69,
        9,
        'SCENARIO',
        'WORK_STUDY',
        'BASIC',
        'EN',
        'KR',
        4,
        'mess up',
        '망치다, 실수하다',
        '망쳐버렸을 때 mess up',
        '''망치다, 실수하다''라는 뜻의 대표 구동사입니다. 요리, 시험, 발표 등 뭔가를 그르쳤을 때 I messed up 한마디로 쿨하게 인정할 수 있어요.',
        'What''s the hardest part of presenting in English for you?',
        '넌 영어로 발표할 때 제일 힘든 게 뭐야?',
        'I''m always scared I''ll mess up my lines.',
        '난 대사 틀릴까 봐 항상 무서워.',
        ARRAY['I''m', 'always', 'scared', 'I''ll', 'mess', 'up', 'my', 'lines'],
        ARRAY['mess', 'down', 'I''m', 'messed', 'scared', 'my', 'up', 'line', 'I''ll', 'always', 'lines'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/9/expressions/69/practice-examples/10ba98d5-e64f-4e6e-91e1-033078e2c669.png","sentenceText":"I really messed up this time.","sentenceWords":["I","really","messed","up","this","time"],"highlightingPart":"messed up","practiceQuestion":"Did the presentation go well?","sentenceTranslation":"이번엔 진짜 내가 망쳤어.","sentenceWordChoices":["really","blow","up","ruin","spoil","this","I","time","messed"],"practiceQuestionTranslation":"발표 잘됐어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/9/expressions/69/practice-examples/49742748-ada3-462e-9baa-7ba672884b28.png","sentenceText":"Don''t worry, everyone messes up sometimes.","sentenceWords":["Don''t","worry","everyone","messes","up","sometimes"],"highlightingPart":"messes up","practiceQuestion":"I feel terrible about the mistake I made.","sentenceTranslation":"걱정 마, 누구나 가끔 실수해.","sentenceWordChoices":["messes","Don''t","worry","blow","everyone","sometimes","spoil","ruin","up"],"practiceQuestionTranslation":"내가 저지른 실수 때문에 기분이 너무 안 좋아."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/9/expressions/69/practice-examples/29fa683e-9637-46bf-885e-3264e86b5ac4.png","sentenceText":"He messed up his lines during the play.","sentenceWords":["He","messed","up","his","lines","during","the","play"],"highlightingPart":"messed up","practiceQuestion":"How did the school play go?","sentenceTranslation":"걔 연극 중에 대사를 틀렸어.","sentenceWordChoices":["lines","his","ruin","during","He","play","the","messed","up","spoil","blow"],"practiceQuestionTranslation":"학교 연극 어떻게 됐어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/9/expressions/69/practice-examples/8487e3e7-b8a5-4736-bd7b-da89d3524ae5.png","sentenceText":"The rain messed up our plans.","sentenceWords":["The","rain","messed","up","our","plans"],"highlightingPart":"messed up","practiceQuestion":"Did you guys go on the picnic?","sentenceTranslation":"비 때문에 우리 계획이 다 틀어졌어.","sentenceWordChoices":["rain","The","blow","our","messed","ruin","spoil","plans","up"],"practiceQuestionTranslation":"너네 소풍 갔다 왔어?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 69
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        70,
        10,
        'SCENARIO',
        'CONVERSATION_SKILL',
        'BASIC',
        'EN',
        'KR',
        1,
        'Have you got a minute?',
        '잠깐 시간 돼?',
        '시간이 있는지 묻는 Have you got a minute?',
        '''잠깐 시간 돼?''라고 상대의 시간을 부담 없이 확보하는 표현입니다. 뭔가 보여주거나 상의할 게 있을 때 대화의 문을 여는 용도로 딱이에요.',
        'Yes? The door''s open.',
        '네? 문 열려 있어요.',
        'Professor, have you got a minute? It''s about my last assignment.',
        '교수님, 잠깐 시간 되세요? 지난 과제 때문에요.',
        ARRAY['Professor', 'have', 'you', 'got', 'a', 'minute', 'It''s', 'about', 'my', 'last', 'assignment'],
        ARRAY['assignment', 'got', 'minute', 'gets', 'have', 'Professor', 'about', 'my', 'you', 'It''s', 'last', 'at', 'minutes', 'a'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/10/expressions/70/practice-examples/a53a313c-3f9c-450a-8523-f718c0600ba0.png","sentenceText":"Do you have a minute to talk?","sentenceWords":["Do","you","have","a","minute","to","talk"],"highlightingPart":"Do you have a minute","practiceQuestion":"Hey, I saw your text. What''s up?","sentenceTranslation":"잠깐 얘기할 시간 있어?","sentenceWordChoices":["minute","minutes","while","a","talk","moment","you","Do","have","to"],"practiceQuestionTranslation":"야, 문자 봤어. 무슨 일이야?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/10/expressions/70/practice-examples/1b8d54f1-b615-47e0-b2f7-bf15328ed1f6.png","sentenceText":"Got a second? I need your opinion.","sentenceWords":["Got","a","second","I","need","your","opinion"],"highlightingPart":"Got a second?","practiceQuestion":"I can''t decide what to wear tonight.","sentenceTranslation":"잠깐 시간 돼? 네 의견이 필요해.","sentenceWordChoices":["a","I","Got","opinion","moment","while","second","need","minutes","your"],"practiceQuestionTranslation":"오늘 밤에 뭐 입을지 못 정하겠어."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/10/expressions/70/practice-examples/709e0718-bf87-4bcc-ac82-a49dcd839e46.png","sentenceText":"If you''ve got a minute, can you check this?","sentenceWords":["If","you''ve","got","a","minute","can","you","check","this"],"highlightingPart":"you''ve got a minute","practiceQuestion":"I finished the draft.","sentenceTranslation":"시간 되면 이것 좀 봐줄래?","sentenceWordChoices":["you''ve","while","this","a","minutes","you","got","moment","If","can","minute","check"],"practiceQuestionTranslation":"초안 다 썼어."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/10/expressions/70/practice-examples/0f03faf5-7ce6-45c2-b393-e86fc174e5c2.png","sentenceText":"Have you got a minute after lunch?","sentenceWords":["Have","you","got","a","minute","after","lunch"],"highlightingPart":"Have you got a minute","practiceQuestion":"Can we talk later today?","sentenceTranslation":"점심 먹고 잠깐 시간 돼?","sentenceWordChoices":["while","after","minute","a","Have","lunch","got","minutes","you","moment"],"practiceQuestionTranslation":"이따 얘기할 수 있어?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 70
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        71,
        10,
        'SCENARIO',
        'OPINION_JUDGMENT',
        'BASIC',
        'EN',
        'KR',
        2,
        'I might be wrong, but',
        '내가 틀렸을 수도 있는데',
        '겸손하게 의견을 내는 I might be wrong, but',
        '''내가 틀렸을 수도 있는데''라며 단정을 피하고 의견을 조심스럽게 내놓는 표현입니다. 반박당할 부담을 줄이면서 할 말은 하는 세련된 화법이에요.',
        'The grade reflects the rubric criteria.',
        '성적은 채점 기준에 따른 거예요.',
        'I might be wrong, but I thought I covered all the requirements.',
        '제가 틀렸을 수도 있지만, 요구 사항은 다 채웠다고 생각했거든요.',
        ARRAY['I', 'might', 'be', 'wrong', 'but', 'I', 'thought', 'I', 'covered', 'all', 'the', 'requirements'],
        ARRAY['I', 'but', 'all', 'cover', 'the', 'think', 'be', 'right', 'I', 'covered', 'I', 'might', 'thought', 'wrong', 'requirements'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/10/expressions/71/practice-examples/897c8871-73df-44b3-bbda-1ca3d4ef4bc6.png","sentenceText":"I might be wrong, but isn''t that Sarah over there?","sentenceWords":["I","might","be","wrong","but","isn''t","that","Sarah","over","there"],"highlightingPart":"I might be wrong, but","practiceQuestion":"Who''s that by the door?","sentenceTranslation":"내가 잘못 봤을 수도 있는데, 저기 사라 아니야?","sentenceWordChoices":["isn''t","Sarah","but","over","there","might","be","I","that","perhaps","maybe","possibly","wrong"],"practiceQuestionTranslation":"문 앞에 저 사람 누구야?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/10/expressions/71/practice-examples/c5b23d17-257b-40e0-b05b-7b55840639c4.png","sentenceText":"I might be wrong, but this data looks off.","sentenceWords":["I","might","be","wrong","but","this","data","looks","off"],"highlightingPart":"I might be wrong, but","practiceQuestion":"Does the report look right to you?","sentenceTranslation":"제가 틀렸을 수도 있지만, 이 데이터 좀 이상해 보여요.","sentenceWordChoices":["off","looks","but","wrong","might","be","I","possibly","this","maybe","perhaps","data"],"practiceQuestionTranslation":"이 보고서 괜찮아 보여?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/10/expressions/71/practice-examples/6b277dd9-7cd0-43f7-9ce2-ea65d98af2d9.png","sentenceText":"Correct me if I''m wrong, but we met before, right?","sentenceWords":["Correct","me","if","I''m","wrong","but","we","met","before","right"],"highlightingPart":"Correct me if I''m wrong, but","practiceQuestion":"Nice to meet you.","sentenceTranslation":"틀렸으면 말해줘, 우리 전에 만난 적 있지?","sentenceWordChoices":["maybe","wrong","possibly","Correct","perhaps","if","before","met","right","we","but","me","I''m"],"practiceQuestionTranslation":"만나서 반가워요."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/10/expressions/71/practice-examples/6bef91ca-c21c-456d-a109-b6b09fe73220.png","sentenceText":"I could be wrong, but I doubt he''ll come.","sentenceWords":["I","could","be","wrong","but","I","doubt","he''ll","come"],"highlightingPart":"I could be wrong, but","practiceQuestion":"Do you think he''ll show up to the party?","sentenceTranslation":"내가 틀릴 수도 있지만, 걔 안 올 것 같아.","sentenceWordChoices":["come","I","perhaps","possibly","could","but","he''ll","wrong","maybe","doubt","be","I"],"practiceQuestionTranslation":"걔 파티에 올 것 같아?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 71
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        72,
        10,
        'SCENARIO',
        'CONVERSATION_SKILL',
        'BASIC',
        'EN',
        'KR',
        3,
        'ring a bell',
        '어렴풋이 들어본 것 같다',
        '들어본 것 같을 때 ring a bell',
        '''종이 울리다'', 즉 ''어렴풋이 기억나다, 들어본 것 같다''라는 뜻입니다. 확실히는 모르지만 낯익을 때, 또는 부정형으로 ''전혀 모르겠다''고 할 때 쓰여요.',
        'It may have been marked down for a late submission — does any of that ring a bell?',
        '기한을 넘겨서 감점됐을 수도 있는데, 짐작 가는 게 있나요?',
        'Honestly, the late submission part rings a bell.',
        '솔직히 늦은 제출 부분은 짚이는 게 있어요.',
        ARRAY['Honestly', 'the', 'late', 'submission', 'part', 'rings', 'a', 'bell'],
        ARRAY['bells', 'bell', 'submission', 'Honestly', 'part', 'ring', 'early', 'the', 'late', 'a', 'rings'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/10/expressions/72/practice-examples/dd5354ed-dfea-4ecb-a552-04947bfd822b.png","sentenceText":"Does this song ring a bell?","sentenceWords":["Does","this","song","ring","a","bell"],"highlightingPart":"ring a bell","practiceQuestion":"I heard this playing at the cafe earlier.","sentenceTranslation":"이 노래 들어본 기억 있어?","sentenceWordChoices":["song","ring","this","familiar","bells","remember","a","bell","Does"],"practiceQuestionTranslation":"아까 카페에서 이 노래 나오던데."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/10/expressions/72/practice-examples/7aa52551-6c75-45d2-8815-e6602b2b715d.png","sentenceText":"The address doesn''t ring a bell at all.","sentenceWords":["The","address","doesn''t","ring","a","bell","at","all"],"highlightingPart":"ring a bell","practiceQuestion":"Do you remember this address?","sentenceTranslation":"그 주소는 전혀 기억에 없는데.","sentenceWordChoices":["all","bell","remember","ring","at","address","familiar","bells","doesn''t","The","a"],"practiceQuestionTranslation":"이 주소 기억나?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/10/expressions/72/practice-examples/becae4a6-3528-43a5-84e9-24fef6c19f55.png","sentenceText":"Her face rings a bell, but I can''t place her.","sentenceWords":["Her","face","rings","a","bell","but","I","can''t","place","her"],"highlightingPart":"rings a bell","practiceQuestion":"Do you know that woman over there?","sentenceTranslation":"얼굴은 낯익은데 어디서 봤는지 모르겠어.","sentenceWordChoices":["remember","face","but","bells","place","a","I","Her","can''t","rings","bell","her","familiar"],"practiceQuestionTranslation":"저기 저 여자 알아?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/10/expressions/72/practice-examples/cd613321-e78c-4f1f-bff5-2fc6953553cf.png","sentenceText":"Hmm, that title vaguely rings a bell.","sentenceWords":["Hmm","that","title","vaguely","rings","a","bell"],"highlightingPart":"rings a bell","practiceQuestion":"Have you read this book before?","sentenceTranslation":"음, 그 제목 어렴풋이 기억나는 것 같기도 해.","sentenceWordChoices":["bells","bell","rings","a","Hmm","familiar","vaguely","that","title","remember"],"practiceQuestionTranslation":"이 책 읽어본 적 있어?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 72
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        73,
        10,
        'SCENARIO',
        'WORK_STUDY',
        'BASIC',
        'EN',
        'KR',
        4,
        'get better at',
        '~을 더 잘하게 되다',
        '잘하고 싶은 마음을 표현하는 get better at',
        '''~을 더 잘하게 되다'', ''~실력을 늘리다''라는 뜻으로 발전 의지를 표현합니다. be good at(잘한다)의 변화 버전이라고 생각하면 쉬워요.',
        'The argument wasn''t fully developed.',
        '논지가 충분히 전개되지 않았어요.',
        'How can I get better at developing my arguments?',
        '논지 전개를 더 잘하려면 어떻게 해야 할까요?',
        ARRAY['How', 'can', 'I', 'get', 'better', 'at', 'developing', 'my', 'arguments'],
        ARRAY['developing', 'at', 'better', 'develop', 'arguments', 'can', 'get', 'good', 'How', 'my', 'gets', 'I'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/10/expressions/73/practice-examples/fdb2aaa3-6f22-4d4d-8d1f-69dbc886b25e.png","sentenceText":"I''m trying to get better at English.","sentenceWords":["I''m","trying","to","get","better","at","English"],"highlightingPart":"get better at","practiceQuestion":"Are you studying anything these days?","sentenceTranslation":"영어 실력을 키우려고 노력 중이야.","sentenceWordChoices":["English","progress","trying","I''m","at","to","master","improve","better","get"],"practiceQuestionTranslation":"요즘 뭐 공부해?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/10/expressions/73/practice-examples/dcc3fa0c-0550-4336-acd6-af83b6192993.png","sentenceText":"He''s getting better at driving.","sentenceWords":["He''s","getting","better","at","driving"],"highlightingPart":"getting better at","practiceQuestion":"How''s your brother doing with driving lessons?","sentenceTranslation":"걔 운전이 점점 늘고 있어.","sentenceWordChoices":["getting","He''s","improve","driving","master","at","progress","better"],"practiceQuestionTranslation":"네 남동생 운전 연수 어때?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/10/expressions/73/practice-examples/dd16f914-84b5-4c25-935c-1da9dae76e1b.png","sentenceText":"How can I get better at public speaking?","sentenceWords":["How","can","I","get","better","at","public","speaking"],"highlightingPart":"get better at","practiceQuestion":"I get so nervous whenever I have to present.","sentenceTranslation":"발표를 어떻게 하면 더 잘할 수 있을까?","sentenceWordChoices":["improve","get","public","I","can","speaking","How","progress","at","better","master"],"practiceQuestionTranslation":"발표할 때마다 너무 떨려."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/10/expressions/73/practice-examples/7c1dfe86-621e-4fa7-84aa-bdcca7d1d692.png","sentenceText":"You''ll get better at it with practice.","sentenceWords":["You''ll","get","better","at","it","with","practice"],"highlightingPart":"get better at","practiceQuestion":"I''m still so bad at cooking.","sentenceTranslation":"연습하면 점점 늘 거야.","sentenceWordChoices":["master","You''ll","it","get","at","practice","with","progress","improve","better"],"practiceQuestionTranslation":"나 아직 요리 진짜 못해."}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 73
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        74,
        10,
        'SCENARIO',
        'TRAVEL_MOVEMENT',
        'BASIC',
        'EN',
        'KR',
        5,
        'stop by',
        '잠깐 들르다',
        '잠깐 들르는 stop by',
        '''잠깐 들르다''라는 뜻으로 swing by, drop by와 함께 쓰이는 대표 구동사입니다. 목적지가 아니라 가는 길에 가볍게 들르는 뉘앙스예요.',
        'Could you drop by my office tomorrow, or would email work better?',
        '내일 오피스로 올 수 있어요, 아니면 이메일이 더 편해요?',
        'I''ll stop by your office tomorrow after class.',
        '내일 수업 끝나고 교수님 오피스에 들를게요.',
        ARRAY['I''ll', 'stop', 'by', 'your', 'office', 'tomorrow', 'after', 'class'],
        ARRAY['I''ll', 'class', 'by', 'stop', 'after', 'tomorrow', 'at', 'before', 'your', 'stops', 'office'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/10/expressions/74/practice-examples/b4a86ecd-00b7-47ea-b258-27b93a540838.png","sentenceText":"Stop by anytime you''re in the area.","sentenceWords":["Stop","by","anytime","you''re","in","the","area"],"highlightingPart":"Stop by","practiceQuestion":"I might be near your neighborhood next week.","sentenceTranslation":"근처 오면 언제든 들러.","sentenceWordChoices":["around","the","visit","Stop","area","stopping","you''re","anytime","in","by"],"practiceQuestionTranslation":"다음 주에 너네 동네 근처 갈 것 같아."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/10/expressions/74/practice-examples/e8cf967e-fb40-4233-9280-2ba4b87296e5.png","sentenceText":"Can we stop by the bakery first?","sentenceWords":["Can","we","stop","by","the","bakery","first"],"highlightingPart":"stop by","practiceQuestion":"We''re heading to the park now, right?","sentenceTranslation":"빵집 먼저 잠깐 들를 수 있어?","sentenceWordChoices":["bakery","we","the","visit","stop","by","Can","first","stopping","around"],"practiceQuestionTranslation":"지금 공원으로 가는 거지?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/10/expressions/74/practice-examples/9aa9d7a6-64c5-46a1-9c6a-d8a8cdad13f3.png","sentenceText":"She stopped by to say hello.","sentenceWords":["She","stopped","by","to","say","hello"],"highlightingPart":"stopped by","practiceQuestion":"Did anyone come by while I was out?","sentenceTranslation":"걔가 인사하러 잠깐 들렀어.","sentenceWordChoices":["hello","say","by","stopped","stopping","visit","around","She","to"],"practiceQuestionTranslation":"나 없는 동안 누가 왔었어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/10/expressions/74/practice-examples/94dce180-c54c-4b47-b831-5faa085c10ff.png","sentenceText":"I need to stop by the bank today.","sentenceWords":["I","need","to","stop","by","the","bank","today"],"highlightingPart":"stop by","practiceQuestion":"What do you have planned for today?","sentenceTranslation":"오늘 은행에 잠깐 들러야 해.","sentenceWordChoices":["by","I","around","stop","stopping","need","to","visit","bank","today","the"],"practiceQuestionTranslation":"오늘 뭐 할 계획이야?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 74
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        75,
        11,
        'SCENARIO',
        'EMOTION_EMPATHY',
        'SLANG_NEOLOGISM',
        'EN',
        'KR',
        1,
        'cooked',
        '망했다, 끝장났다',
        '망했다고 말하는 cooked',
        '''(비유적으로) 다 익어버렸다'', 즉 ''망했다'', ''끝장났다''는 뜻의 최신 슬랭입니다. 시험, 마감, 체력 등 손쓸 수 없는 상황에서 I''m cooked 한마디로 자조할 수 있어요.',
        'Finals are next week already — are you ready?',
        '벌써 다음 주가 기말이야. 준비됐어?',
        'Not even close. If I don''t start today, I''m cooked.',
        '전혀. 오늘 시작 안 하면 나 망했어.',
        ARRAY['Not', 'even', 'close', 'If', 'I', 'don''t', 'start', 'today', 'I''m', 'cooked'],
        ARRAY['Not', 'If', 'close', 'started', 'I', 'today', 'don''t', 'I''m', 'won''t', 'even', 'cook', 'start', 'cooked'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/11/expressions/75/practice-examples/014d342f-f39c-4e9f-896b-d1bcab68a82c.png","sentenceText":"If the boss finds out, we''re cooked.","sentenceWords":["If","the","boss","finds","out","we''re","cooked"],"highlightingPart":"cooked","practiceQuestion":"What if they find out we''re late?","sentenceTranslation":"상사가 알면 우리 끝장이야.","sentenceWordChoices":["the","out","finds","cooking","cook","burnt","we''re","boss","If","cooked"],"practiceQuestionTranslation":"우리 늦은 거 걔네가 알면 어떡해?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/11/expressions/75/practice-examples/d453e959-7825-4530-8124-381e4fc8058c.png","sentenceText":"My phone died and I don''t know the address. I''m cooked.","sentenceWords":["My","phone","died","and","I","don''t","know","the","address","I''m","cooked"],"highlightingPart":"cooked","practiceQuestion":"Where are we supposed to meet again?","sentenceTranslation":"폰은 꺼졌고 주소도 몰라. 나 망했다.","sentenceWordChoices":["phone","I","address","cooking","burnt","I''m","My","know","cook","the","don''t","died","cooked","and"],"practiceQuestionTranslation":"우리 어디서 만나기로 했지?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/11/expressions/75/practice-examples/5ec8c494-6b31-4e3c-8469-2369819d56ad.png","sentenceText":"After that workout, my legs are cooked.","sentenceWords":["After","that","workout","my","legs","are","cooked"],"highlightingPart":"cooked","practiceQuestion":"How was leg day at the gym?","sentenceTranslation":"그 운동 하고 나니 다리가 완전 맛이 갔어.","sentenceWordChoices":["are","After","workout","cooked","burnt","my","that","cook","legs","cooking"],"practiceQuestionTranslation":"헬스장 하체 운동 어땠어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/11/expressions/75/practice-examples/4a235b38-cb96-419b-a396-d653a5389621.png","sentenceText":"They''re up by 20 points. We''re cooked.","sentenceWords":["They''re","up","by","20","points","We''re","cooked"],"highlightingPart":"cooked","practiceQuestion":"What''s the score right now?","sentenceTranslation":"20점 차로 지고 있어. 우린 글렀어.","sentenceWordChoices":["by","20","cooking","They''re","burnt","points","cooked","cook","up","We''re"],"practiceQuestionTranslation":"지금 스코어 어떻게 돼?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 75
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        76,
        11,
        'SCENARIO',
        'GRAMMAR_FUNCTION_WORD',
        'BASIC',
        'EN',
        'KR',
        2,
        'not ~ until',
        '~하고 나서야 비로소 …하다',
        '예상 밖의 시점을 말하는 not ~ until',
        '''~까지는 …하지 않는다'', 뒤집으면 ''~하고 나서야 비로소 …한다''는 뜻입니다. 예상했던 시점과 실제가 다를 때 그 차이를 자연스럽게 드러내 줘요.',
        'Are you a cram-the-night-before type, or do you study ahead?',
        '넌 벼락치기 타입이야, 미리 하는 타입이야?',
        'Honestly, I don''t start studying until the night before.',
        '솔직히 난 전날 밤이 돼서야 공부를 시작해.',
        ARRAY['Honestly', 'I', 'don''t', 'start', 'studying', 'until', 'the', 'night', 'before'],
        ARRAY['night', 'I', 'the', 'Honestly', 'before', 'don''t', 'after', 'study', 'start', 'starts', 'until', 'studying'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/11/expressions/76/practice-examples/ec18eac4-0a0b-44d4-b70e-5c67b18a7921.png","sentenceText":"I didn''t know until this morning.","sentenceWords":["I","didn''t","know","until","this","morning"],"highlightingPart":"didn''t know until","practiceQuestion":"When did you hear the news?","sentenceTranslation":"오늘 아침에야 알았어.","sentenceWordChoices":["when","until","after","morning","this","I","know","didn''t","before"],"practiceQuestionTranslation":"그 소식 언제 들었어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/11/expressions/76/practice-examples/0a20274d-3da8-47c1-b4fb-7be522614fed.png","sentenceText":"The package won''t arrive until next week.","sentenceWords":["The","package","won''t","arrive","until","next","week"],"highlightingPart":"won''t arrive until","practiceQuestion":"When is my package supposed to get here?","sentenceTranslation":"택배는 다음 주나 돼야 도착해.","sentenceWordChoices":["after","arrive","when","The","before","won''t","week","until","next","package"],"practiceQuestionTranslation":"내 택배 언제 도착하는 거야?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/11/expressions/76/practice-examples/0e286600-d02c-43ab-b931-10d2b633afb8.png","sentenceText":"She didn''t start cooking until she was 30.","sentenceWords":["She","didn''t","start","cooking","until","she","was","30"],"highlightingPart":"didn''t start cooking until","practiceQuestion":"Has she always been into cooking?","sentenceTranslation":"걔는 서른이 되어서야 요리를 시작했어.","sentenceWordChoices":["when","until","start","cooking","she","was","after","didn''t","before","She","30"],"practiceQuestionTranslation":"걔 원래부터 요리 좋아했어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/11/expressions/76/practice-examples/9d28fd7d-2cf4-41ed-a8e0-37cf242a29b7.png","sentenceText":"Don''t open it until your birthday.","sentenceWords":["Don''t","open","it","until","your","birthday"],"highlightingPart":"Don''t open it until","practiceQuestion":"Can I open my present now?","sentenceTranslation":"생일 전까진 열어보지 마.","sentenceWordChoices":["until","it","Don''t","birthday","before","after","your","when","open"],"practiceQuestionTranslation":"내 선물 지금 열어봐도 돼?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 76
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        77,
        11,
        'SCENARIO',
        'EMOTION_EMPATHY',
        'BASIC',
        'EN',
        'KR',
        3,
        'let ~ down',
        '~을 실망시키다',
        '실망을 표현하는 let ~ down',
        '''~을 실망시키다''라는 뜻으로, 기대에 못 미쳤을 때의 감정을 표현합니다. Don''t let me down(실망시키지 마)처럼 신뢰와 기대의 맥락에서 자주 쓰여요.',
        'What did you do when you bombed a test?',
        '시험 망쳤을 때 어떻게 했어?',
        'I didn''t want to let my parents down, so I told them first.',
        '부모님을 실망시키고 싶지 않아서 먼저 말씀드렸어.',
        ARRAY['I', 'didn''t', 'want', 'to', 'let', 'my', 'parents', 'down', 'so', 'I', 'told', 'them', 'first'],
        ARRAY['tell', 'lets', 'to', 'my', 'I', 'didn''t', 'I', 'up', 'them', 'down', 'let', 'so', 'first', 'parents', 'want', 'told'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/11/expressions/77/practice-examples/85980bb1-5f5f-464e-9da5-004a5dcdd63f.png","sentenceText":"The movie really let me down.","sentenceWords":["The","movie","really","let","me","down"],"highlightingPart":"let me down","practiceQuestion":"How was that movie everyone loved?","sentenceTranslation":"그 영화 정말 실망이었어.","sentenceWordChoices":["movie","disappoint","fail","down","really","The","let","letting","me"],"practiceQuestionTranslation":"다들 좋다던 그 영화 어땠어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/11/expressions/77/practice-examples/aec14dd5-7a0a-48b7-bb91-f778a563d5cf.png","sentenceText":"He never lets his team down.","sentenceWords":["He","never","lets","his","team","down"],"highlightingPart":"lets his team down","practiceQuestion":"Can we count on him for the finals?","sentenceTranslation":"걔는 절대 팀을 실망시키지 않아.","sentenceWordChoices":["team","He","disappoint","lets","letting","fail","never","his","down"],"practiceQuestionTranslation":"결승전에서 걔 믿을 수 있을까?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/11/expressions/77/practice-examples/9a4dab41-f07a-41da-b6ea-0d2ac985ecd6.png","sentenceText":"Sorry I let you down.","sentenceWords":["Sorry","I","let","you","down"],"highlightingPart":"let you down","practiceQuestion":"I was really counting on you yesterday.","sentenceTranslation":"실망시켜서 미안해.","sentenceWordChoices":["fail","I","you","down","Sorry","let","letting","disappoint"],"practiceQuestionTranslation":"어제 너 진짜 믿었는데."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/11/expressions/77/practice-examples/d2df12d4-c724-4364-84e2-8985e33b1cd7.png","sentenceText":"The restaurant let us down this time.","sentenceWords":["The","restaurant","let","us","down","this","time"],"highlightingPart":"let us down","practiceQuestion":"How was dinner last night?","sentenceTranslation":"그 식당 이번엔 실망스러웠어.","sentenceWordChoices":["letting","fail","us","this","down","time","let","The","disappoint","restaurant"],"practiceQuestionTranslation":"어젯밤 저녁 어땠어?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 77
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        78,
        11,
        'SCENARIO',
        'CONVERSATION_SKILL',
        'BASIC',
        'EN',
        'KR',
        4,
        'How does ~ sound?',
        '~ 어때?',
        '의견을 물어보는 How does ~ sound?',
        '제안한 것에 대해 ''~ 어때?''라고 상대의 의견을 묻는 표현입니다. 특히 시간 약속이나 계획을 조율할 때 캐주얼하면서도 자연스럽게 쓸 수 있어요.',
        'I kinda wanna study together — are you free sometime?',
        '너랑 같이 공부하고 싶은데, 시간 돼?',
        'Sure! How does Saturday at the library sound?',
        '좋아! 토요일에 도서관 어때?',
        ARRAY['Sure', 'How', 'does', 'Saturday', 'at', 'the', 'library', 'sound'],
        ARRAY['do', 'library', 'sound', 'How', 'Sure', 'at', 'does', 'sounds', 'in', 'Saturday', 'the'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/11/expressions/78/practice-examples/f14d2164-5246-4c84-9513-80090e8d1266.png","sentenceText":"How does dinner at 7 sound?","sentenceWords":["How","does","dinner","at","7","sound"],"highlightingPart":"How does dinner at 7 sound?","practiceQuestion":"What time works for dinner?","sentenceTranslation":"7시에 저녁 어때?","sentenceWordChoices":["seems","looks","at","does","How","dinner","7","feels","sound"],"practiceQuestionTranslation":"저녁 몇 시가 좋아?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/11/expressions/78/practice-examples/982c2144-c033-4c5f-9a44-63344a68d2ea.png","sentenceText":"How does Italian food sound tonight?","sentenceWords":["How","does","Italian","food","sound","tonight"],"highlightingPart":"How does Italian food sound","practiceQuestion":"What do you want to eat tonight?","sentenceTranslation":"오늘 저녁 이탈리안 어때?","sentenceWordChoices":["Italian","How","feels","looks","does","food","sound","tonight","seems"],"practiceQuestionTranslation":"오늘 저녁 뭐 먹고 싶어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/11/expressions/78/practice-examples/ce3f5ae9-9eaa-4659-8bcf-26c20e4f58e5.png","sentenceText":"How does that plan sound to you?","sentenceWords":["How","does","that","plan","sound","to","you"],"highlightingPart":"How does that plan sound","practiceQuestion":"I was thinking we could meet early and grab coffee first.","sentenceTranslation":"그 계획 어떤 것 같아?","sentenceWordChoices":["looks","that","How","sound","to","seems","you","does","plan","feels"],"practiceQuestionTranslation":"일찍 만나서 커피 먼저 마시면 어떨까 생각했어."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/11/expressions/78/practice-examples/faa1aa9f-0d43-4f6b-bbf8-e5010dfc7ff3.png","sentenceText":"Pizza and a movie — how does that sound?","sentenceWords":["Pizza","and","a","movie","how","does","that","sound"],"highlightingPart":"how does that sound?","practiceQuestion":"What do you want to do this weekend?","sentenceTranslation":"피자에 영화 한 편, 어때?","sentenceWordChoices":["Pizza","a","how","seems","feels","looks","sound","and","that","does","movie"],"practiceQuestionTranslation":"이번 주말에 뭐 하고 싶어?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 78
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        79,
        12,
        'SCENARIO',
        'OPINION_JUDGMENT',
        'BASIC',
        'EN',
        'KR',
        1,
        'It''s about ~',
        '중요한 건 ~이다',
        '핵심이 무엇인지 짚는 It''s about ~',
        '''중요한 건 ~이다'', ''핵심은 ~이다''라고 본질을 짚어주는 표현입니다. It''s not about A, it''s about B 구조로 쓰면 ''중요한 건 A가 아니라 B야''라는 명언 같은 문장이 완성돼요.',
        'Where do you stand — can money buy happiness?',
        '넌 어느 쪽이야? 돈으로 행복을 살 수 있어?',
        'It''s not about the money itself. It''s about the freedom it gives you.',
        '중요한 건 돈 자체가 아니라, 돈이 주는 자유야.',
        ARRAY['It''s', 'not', 'about', 'the', 'money', 'itself', 'It''s', 'about', 'the', 'freedom', 'it', 'gives', 'you'],
        ARRAY['at', 'freedom', 'It''s', 'about', 'itself', 'myself', 'the', 'the', 'about', 'not', 'It''s', 'give', 'it', 'money', 'you', 'gives'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/12/expressions/79/practice-examples/28d7d3f3-7d23-4db5-8adc-de6859fb8bd5.png","sentenceText":"It''s not about the gift. It''s about the thought.","sentenceWords":["It''s","not","about","the","gift","It''s","about","the","thought"],"highlightingPart":"It''s not about the gift. It''s about the thought.","practiceQuestion":"I''m worried my gift is too cheap.","sentenceTranslation":"중요한 건 선물이 아니라 마음이야.","sentenceWordChoices":["about","It''s","not","about","the","thought","gift","It''s","the","matters","regarding","concerns"],"practiceQuestionTranslation":"선물이 너무 저렴한 것 같아 걱정돼."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/12/expressions/79/practice-examples/0745cb0f-272d-4635-8b62-0c99a2f489f9.png","sentenceText":"Success is about consistency.","sentenceWords":["Success","is","about","consistency"],"highlightingPart":"is about consistency","practiceQuestion":"What do you think is the secret to success?","sentenceTranslation":"성공의 핵심은 꾸준함이야.","sentenceWordChoices":["concerns","consistency","Success","about","is","matters","regarding"],"practiceQuestionTranslation":"성공의 비결이 뭐라고 생각해?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/12/expressions/79/practice-examples/23ce02ea-b382-415f-97b1-799022e0c640.png","sentenceText":"It''s not about how much you have.","sentenceWords":["It''s","not","about","how","much","you","have"],"highlightingPart":"It''s not about","practiceQuestion":"He always says money is everything.","sentenceTranslation":"얼마나 가졌는지가 중요한 게 아니야.","sentenceWordChoices":["It''s","you","how","regarding","not","matters","about","much","concerns","have"],"practiceQuestionTranslation":"걔는 항상 돈이 최고라고 말해."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/12/expressions/79/practice-examples/907f4d9b-0cfd-46dd-a146-d2db40522c4c.png","sentenceText":"For me, it''s all about balance.","sentenceWords":["For","me","it''s","all","about","balance"],"highlightingPart":"it''s all about balance","practiceQuestion":"What matters most to you in life?","sentenceTranslation":"나한테 제일 중요한 건 균형이야.","sentenceWordChoices":["me","balance","about","matters","For","it''s","regarding","concerns","all"],"practiceQuestionTranslation":"인생에서 제일 중요한 게 뭐야?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 79
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        80,
        12,
        'SCENARIO',
        'OPINION_JUDGMENT',
        'BASIC',
        'EN',
        'KR',
        2,
        'not necessarily',
        '반드시 그런 건 아니다',
        '반드시 그런 건 아닌 not necessarily',
        '''꼭 그렇다고는 할 수 없다''라며 일반화에 제동을 거는 표현입니다. 상대 말을 정면으로 부정하지 않으면서 예외의 여지를 열어줘요.',
        'So more money means more happiness, right?',
        '그럼 돈이 많을수록 더 행복한 거 맞지?',
        'Having more doesn''t necessarily mean being happier.',
        '더 많이 가졌다고 꼭 더 행복한 건 아니야.',
        ARRAY['Having', 'more', 'doesn''t', 'necessarily', 'mean', 'being', 'happier'],
        ARRAY['Having', 'necessarily', 'means', 'being', 'more', 'happier', 'necessary', 'mean', 'doesn''t', 'happy'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/12/expressions/80/practice-examples/b3c4740d-31ac-4113-a9d4-224ed9781e48.png","sentenceText":"Not necessarily. It depends on the situation.","sentenceWords":["Not","necessarily","It","depends","on","the","situation"],"highlightingPart":"Not necessarily","practiceQuestion":"So we''re definitely canceling?","sentenceTranslation":"꼭 그렇진 않아. 상황에 따라 달라.","sentenceWordChoices":["situation","always","depends","exactly","on","necessarily","It","the","Not","necessary"],"practiceQuestionTranslation":"그럼 우리 확실히 취소하는 거야?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/12/expressions/80/practice-examples/da04e09e-33d6-4682-bd80-9c16259eb3c8.png","sentenceText":"Being busy doesn''t necessarily mean being productive.","sentenceWords":["Being","busy","doesn''t","necessarily","mean","being","productive"],"highlightingPart":"doesn''t necessarily mean","practiceQuestion":"He''s always so busy, he must get so much done.","sentenceTranslation":"바쁘다고 꼭 생산적인 건 아니야.","sentenceWordChoices":["necessarily","exactly","doesn''t","mean","being","Being","always","productive","necessary","busy"],"practiceQuestionTranslation":"걔는 항상 바쁘니까 일도 엄청 많이 할 거야."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/12/expressions/80/practice-examples/b7344b9c-519f-474c-a62d-00681acc5e46.png","sentenceText":"Older doesn''t necessarily mean wiser.","sentenceWords":["Older","doesn''t","necessarily","mean","wiser"],"highlightingPart":"doesn''t necessarily mean","practiceQuestion":"Shouldn''t we listen to him since he''s older?","sentenceTranslation":"나이가 많다고 꼭 더 지혜로운 건 아니지.","sentenceWordChoices":["mean","always","wiser","necessary","doesn''t","Older","necessarily","exactly"],"practiceQuestionTranslation":"걔가 더 나이 많으니까 말 들어야 하지 않아?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/12/expressions/80/practice-examples/cfae05b8-9445-4924-9d36-faddbf3983a6.png","sentenceText":"You don''t necessarily have to agree with me.","sentenceWords":["You","don''t","necessarily","have","to","agree","with","me"],"highlightingPart":"don''t necessarily have to","practiceQuestion":"I feel like I have to agree with everything you say.","sentenceTranslation":"꼭 내 의견에 동의할 필요는 없어.","sentenceWordChoices":["don''t","You","agree","to","with","me","exactly","have","necessary","always","necessarily"],"practiceQuestionTranslation":"네가 말하는 거에 다 동의해야 할 것 같아."}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 80
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        81,
        12,
        'SCENARIO',
        'OPINION_JUDGMENT',
        'BASIC',
        'EN',
        'KR',
        3,
        'depend on',
        '~에 달려 있다, ~에 따라 다르다',
        '상황에 따라 달라지는 depend on',
        '''~에 달려 있다, ~에 따라 다르다''라는 뜻으로, 조건에 따라 답이 바뀔 때 씁니다. 단독으로 It depends(상황에 따라 달라)라고만 해도 훌륭한 대답이 돼요.',
        'Why do you think money works for some people?',
        '왜 어떤 사람들한텐 돈이 통하는 것 같아?',
        'It depends on how you spend it.',
        '그건 어떻게 쓰느냐에 따라 달라.',
        ARRAY['It', 'depends', 'on', 'how', 'you', 'spend', 'it'],
        ARRAY['at', 'spends', 'spend', 'you', 'on', 'how', 'it', 'depend', 'It', 'depends'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/12/expressions/81/practice-examples/0751abbc-ac04-4ce1-bcf0-fff1e32d7e99.png","sentenceText":"It depends on what time it ends.","sentenceWords":["It","depends","on","what","time","it","ends"],"highlightingPart":"depends on","practiceQuestion":"Are you coming?","sentenceTranslation":"몇 시에 끝나느냐에 따라.","sentenceWordChoices":["depends","ends","it","based","varies","time","It","on","what","rely"],"practiceQuestionTranslation":"올 거야?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/12/expressions/81/practice-examples/2a24d3f9-dc94-4441-8b5f-68d390df18df.png","sentenceText":"The price depends on the size.","sentenceWords":["The","price","depends","on","the","size"],"highlightingPart":"depends on","practiceQuestion":"How much does this cost?","sentenceTranslation":"가격은 사이즈에 따라 달라요.","sentenceWordChoices":["size","The","on","rely","price","depends","varies","the","based"],"practiceQuestionTranslation":"이거 얼마예요?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/12/expressions/81/practice-examples/9e8e28ff-64e8-4459-a27c-d7c6db066009.png","sentenceText":"Everything depends on the test results.","sentenceWords":["Everything","depends","on","the","test","results"],"highlightingPart":"depends on","practiceQuestion":"So what happens next with your treatment?","sentenceTranslation":"모든 게 시험 결과에 달렸어.","sentenceWordChoices":["test","results","the","on","rely","depends","Everything","based","varies"],"practiceQuestionTranslation":"그럼 치료는 다음에 어떻게 되는 거야?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/12/expressions/81/practice-examples/54c91230-6070-4bc4-b045-19aac3e745d0.png","sentenceText":"It depends on who you ask.","sentenceWords":["It","depends","on","who","you","ask"],"highlightingPart":"depends on","practiceQuestion":"Is this the best restaurant in town?","sentenceTranslation":"누구한테 묻느냐에 따라 달라.","sentenceWordChoices":["on","based","ask","who","rely","depends","varies","you","It"],"practiceQuestionTranslation":"이게 이 동네 최고 맛집이야?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 81
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        82,
        12,
        'SCENARIO',
        'OPINION_JUDGMENT',
        'BASIC',
        'EN',
        'KR',
        4,
        'make a difference',
        '변화를 만들다',
        '변화를 만드는 make a difference',
        '''변화를 만들다'', ''영향을 주다''라는 뜻으로, 크든 작든 의미 있는 차이를 만들어낼 때 씁니다. 봉사, 환경, 노력의 가치를 말할 때 단골로 등장해요.',
        'Did that feeling actually last?',
        '그 기분이 진짜 오래갔어?',
        'It did — experiences make a bigger difference than things.',
        '응. 물건보다 경험이 훨씬 큰 차이를 만들더라.',
        ARRAY['It', 'did', 'experiences', 'make', 'a', 'bigger', 'difference', 'than', 'things'],
        ARRAY['makes', 'make', 'difference', 'It', 'did', 'a', 'than', 'then', 'experiences', 'things', 'big', 'bigger'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/12/expressions/82/practice-examples/7f250709-40dc-4b99-83dc-e1cec79d81a8.png","sentenceText":"Your donation will make a real difference.","sentenceWords":["Your","donation","will","make","a","real","difference"],"highlightingPart":"make a real difference","practiceQuestion":"Will my small donation even help?","sentenceTranslation":"당신의 기부가 실질적인 변화를 만들 거예요.","sentenceWordChoices":["matter","a","Your","make","will","change","donation","difference","real","impact"],"practiceQuestionTranslation":"내 작은 기부가 도움이 되긴 할까?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/12/expressions/82/practice-examples/b91d79b7-8afe-4ad8-8914-aa8319db8c04.png","sentenceText":"Does it make a difference which one I choose?","sentenceWords":["Does","it","make","a","difference","which","one","I","choose"],"highlightingPart":"make a difference","practiceQuestion":"Should I get the red one or the blue one?","sentenceTranslation":"어느 걸 고르든 차이가 있어?","sentenceWordChoices":["change","make","which","it","difference","impact","choose","Does","one","matter","a","I"],"practiceQuestionTranslation":"빨간 거 살까 파란 거 살까?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/12/expressions/82/practice-examples/bd1855f2-700c-4a98-b53f-cdb111ec4030.png","sentenceText":"One person can make a difference.","sentenceWords":["One","person","can","make","a","difference"],"highlightingPart":"make a difference","practiceQuestion":"What''s the point of just me recycling?","sentenceTranslation":"한 사람이 세상을 바꿀 수 있어.","sentenceWordChoices":["impact","a","One","person","change","difference","make","can","matter"],"practiceQuestionTranslation":"나 혼자 재활용한다고 무슨 의미가 있겠어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/12/expressions/82/practice-examples/7db226bb-0951-485d-b3c4-08561116701d.png","sentenceText":"Getting enough sleep makes a huge difference.","sentenceWords":["Getting","enough","sleep","makes","a","huge","difference"],"highlightingPart":"makes a huge difference","practiceQuestion":"I''ve been feeling so much better lately.","sentenceTranslation":"잠을 충분히 자는 게 엄청난 차이를 만들어.","sentenceWordChoices":["sleep","makes","a","difference","huge","matter","Getting","impact","change","enough"],"practiceQuestionTranslation":"나 요즘 컨디션이 훨씬 좋아졌어."}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 82
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        83,
        12,
        'SCENARIO',
        'DAILY_ROUTINE',
        'BASIC',
        'EN',
        'KR',
        5,
        'get used to',
        '~에 익숙해지다',
        '적응과 익숙함을 말하는 get used to',
        '''~에 익숙해지다''라는 뜻으로, 새로운 환경이나 상황에 적응하는 과정을 표현합니다. to 뒤에는 명사나 동명사가 온다는 점, 그리고 be used to(이미 익숙한 상태)와의 차이가 포인트예요.',
        'If you won a million dollars, would you actually be happier a year later?',
        '백만 달러가 생기면 1년 뒤에 진짜 더 행복할 것 같아?',
        'Honestly, I think I''d just get used to it.',
        '솔직히 그냥 익숙해질 것 같아.',
        ARRAY['Honestly', 'I', 'think', 'I''d', 'just', 'get', 'used', 'to', 'it'],
        ARRAY['to', 'think', 'I', 'just', 'gets', 'using', 'I''d', 'use', 'it', 'used', 'get', 'Honestly'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/12/expressions/83/practice-examples/3927a250-e392-4f29-aadd-039ff43752d2.png","sentenceText":"It took me a while to get used to the noise.","sentenceWords":["It","took","me","a","while","to","get","used","to","the","noise"],"highlightingPart":"get used to","practiceQuestion":"How did you handle the loud neighbors?","sentenceTranslation":"소음에 익숙해지는 데 시간이 좀 걸렸어.","sentenceWordChoices":["using","It","me","usual","a","took","while","noise","get","use","used","the","to","to"],"practiceQuestionTranslation":"시끄러운 이웃 어떻게 견뎠어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/12/expressions/83/practice-examples/60b84279-6662-4f25-80dc-fb89003cb17b.png","sentenceText":"You''ll get used to it soon.","sentenceWords":["You''ll","get","used","to","it","soon"],"highlightingPart":"get used to","practiceQuestion":"Everything here still feels so unfamiliar.","sentenceTranslation":"금방 익숙해질 거야.","sentenceWordChoices":["used","get","to","usual","using","soon","use","You''ll","it"],"practiceQuestionTranslation":"여기 아직도 모든 게 낯설어."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/12/expressions/83/practice-examples/416ab60d-277f-40d1-a22c-970177783689.png","sentenceText":"I can''t get used to waking up early.","sentenceWords":["I","can''t","get","used","to","waking","up","early"],"highlightingPart":"get used to","practiceQuestion":"How''s the new work schedule going?","sentenceTranslation":"일찍 일어나는 건 도무지 익숙해지지 않아.","sentenceWordChoices":["can''t","using","I","up","get","used","waking","to","use","early","usual"],"practiceQuestionTranslation":"새 근무 시간표 어때?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/12/expressions/83/practice-examples/f7c3aeb9-8082-46dc-ac39-482ca933f385.png","sentenceText":"She got used to living alone.","sentenceWords":["She","got","used","to","living","alone"],"highlightingPart":"got used to","practiceQuestion":"Is she doing okay since moving out on her own?","sentenceTranslation":"걔는 혼자 사는 것에 익숙해졌어.","sentenceWordChoices":["got","to","usual","She","living","use","alone","using","used"],"practiceQuestionTranslation":"걔 독립하고 잘 지내?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 83
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        30,
        13,
        'SCENARIO',
        'GRAMMAR_FUNCTION_WORD',
        'BASIC',
        'EN',
        'KR',
        1,
        'for',
        '~하러, ~을 위해',
        '목적을 나타내는 for',
        '전치사 for는 ''~하러'', ''~을 위해''라며 행동의 목적을 간단히 표현합니다. go out for a walk(산책하러 나가다)처럼 [동사 + for + 명사] 형태로 활용도가 높아요.',
        'So where are you headed, and what''s taking you there?',
        '어디 가는 길이에요, 무슨 일로 가요?',
        'I''m going to London for my best friend''s wedding.',
        '제일 친한 친구 결혼식 때문에 런던에 가요.',
        ARRAY['I''m', 'going', 'to', 'London', 'for', 'my', 'best', 'friend''s', 'wedding'],
        ARRAY['to', 'wedding', 'friend''s', 'for', 'London', 'best', 'friends', 'I''m', 'go', 'going', 'my', 'at'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/13/expressions/30/practice-examples/d5d2d091-ff1f-493c-a341-f315baba6dd6.png","sentenceText":"I went out for a run this morning.","sentenceWords":["I","went","out","for","a","run","this","morning"],"highlightingPart":"for","practiceQuestion":"What did you do this morning?","sentenceTranslation":"아침에 달리기하러 나갔다 왔어.","sentenceWordChoices":["in","went","for","I","a","to","run","this","of","out","morning"],"practiceQuestionTranslation":"오늘 아침에 뭐 했어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/13/expressions/30/practice-examples/d25cd559-3ba7-4a91-9fa3-54e1fdff71e1.png","sentenceText":"She stepped out for some coffee.","sentenceWords":["She","stepped","out","for","some","coffee"],"highlightingPart":"for","practiceQuestion":"Where''s Sarah?","sentenceTranslation":"걔 커피 사러 잠깐 나갔어.","sentenceWordChoices":["in","for","coffee","of","some","stepped","She","out","to"],"practiceQuestionTranslation":"사라 어디 갔어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/13/expressions/30/practice-examples/6941fce8-8a8b-49b5-ba4a-be55ee97b4a1.png","sentenceText":"Let''s meet for lunch next week.","sentenceWords":["Let''s","meet","for","lunch","next","week"],"highlightingPart":"for","practiceQuestion":"Are you free next week?","sentenceTranslation":"다음 주에 점심 먹으러 만나자.","sentenceWordChoices":["week","for","of","in","meet","to","next","Let''s","lunch"],"practiceQuestionTranslation":"다음 주에 시간 돼?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/13/expressions/30/practice-examples/f89b41e6-b674-4032-aac2-2f60fa8b0c6b.png","sentenceText":"We stopped for gas on the way.","sentenceWords":["We","stopped","for","gas","on","the","way"],"highlightingPart":"for","practiceQuestion":"Why did it take so long to get here?","sentenceTranslation":"가는 길에 주유하러 들렀어.","sentenceWordChoices":["of","for","We","in","to","stopped","the","way","gas","on"],"practiceQuestionTranslation":"여기 오는데 왜 이렇게 오래 걸렸어?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 30
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        31,
        13,
        'SCENARIO',
        'OPINION_JUDGMENT',
        'SLANG_NEOLOGISM',
        'EN',
        'KR',
        2,
        'give off a vibe',
        '~한 분위기를 풍기다',
        '느낌과 분위기를 말하는 give off a vibe',
        '''~한 느낌/분위기를 풍기다''라는 뜻입니다. vibe는 말로 설명하기 힘든 기운이나 인상을 뜻해서, 사람이든 장소든 첫인상을 말할 때 딱이에요.',
        'Are you more into big cities or getting out into nature?',
        '대도시파예요, 자연으로 나가는 파예요?',
        'Big cities give off an exciting vibe, but I recharge in nature.',
        '대도시는 신나는 분위기가 있는데, 재충전은 자연에서 해요.',
        ARRAY['Big', 'cities', 'give', 'off', 'an', 'exciting', 'vibe', 'but', 'I', 'recharge', 'in', 'nature'],
        ARRAY['vibe', 'recharge', 'cities', 'excited', 'a', 'exciting', 'give', 'but', 'off', 'I', 'in', 'gives', 'an', 'nature', 'Big'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/13/expressions/31/practice-examples/70dcb423-6a55-4a53-a8d8-4f9d7b6f5b28.png","sentenceText":"He gives off a friendly vibe.","sentenceWords":["He","gives","off","a","friendly","vibe"],"highlightingPart":"gives off a friendly vibe","practiceQuestion":"What''s your first impression of him?","sentenceTranslation":"걔는 친근한 분위기를 풍겨.","sentenceWordChoices":["He","friendly","aura","radiates","vibe","off","feeling","a","gives"],"practiceQuestionTranslation":"걔 첫인상 어때?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/13/expressions/31/practice-examples/adf947f6-37fd-4def-8b3c-ff8d28d3dfb9.png","sentenceText":"The house gave off a creepy vibe at night.","sentenceWords":["The","house","gave","off","a","creepy","vibe","at","night"],"highlightingPart":"gave off a creepy vibe","practiceQuestion":"How was the old house at night?","sentenceTranslation":"그 집은 밤에 으스스한 느낌이 났어.","sentenceWordChoices":["night","at","vibe","off","feeling","gave","The","a","creepy","radiates","house","aura"],"practiceQuestionTranslation":"그 오래된 집 밤에 어땠어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/13/expressions/31/practice-examples/1ad26d65-e837-437b-827e-967c894d2818.png","sentenceText":"Your outfit gives off summer vibes.","sentenceWords":["Your","outfit","gives","off","summer","vibes"],"highlightingPart":"gives off summer vibes","practiceQuestion":"What do you think of my outfit?","sentenceTranslation":"네 옷 완전 여름 느낌이다.","sentenceWordChoices":["outfit","radiates","off","feeling","aura","gives","summer","vibes","Your"],"practiceQuestionTranslation":"내 옷 어때?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/13/expressions/31/practice-examples/3fdf36b9-7fb1-4196-a143-4d216f5f4abe.png","sentenceText":"The new boss gives off a serious vibe.","sentenceWords":["The","new","boss","gives","off","a","serious","vibe"],"highlightingPart":"gives off a serious vibe","practiceQuestion":"What''s the new boss like?","sentenceTranslation":"새 상사는 진지한 분위기를 풍겨.","sentenceWordChoices":["The","off","serious","feeling","gives","boss","radiates","vibe","new","aura","a"],"practiceQuestionTranslation":"새 상사 어때?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 31
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        32,
        13,
        'SCENARIO',
        'OPINION_JUDGMENT',
        'BASIC',
        'EN',
        'KR',
        3,
        'There is something ~ about',
        '뭔가 ~한 게 있다',
        '딱 꼬집어 말 못 할 때 There is something ~ about',
        '정확히 뭔지는 모르겠지만 ''뭔가 ~한 게 있다''라고 말할 때 쓰는 표현입니다. 특별함, 이상함, 매력 등 설명하기 힘든 느낌을 전달하기 좋아요.',
        'What made that trip so special?',
        '그 여행이 뭐가 그렇게 특별했어요?',
        'There was something magical about the whole city.',
        '그 도시 전체에 뭔가 마법 같은 게 있었어요.',
        ARRAY['There', 'was', 'something', 'magical', 'about', 'the', 'whole', 'city'],
        ARRAY['magic', 'anything', 'magical', 'the', 'was', 'is', 'There', 'whole', 'about', 'city', 'something'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/13/expressions/32/practice-examples/2dd9e7e6-2765-4e74-89a8-eb621088fb21.png","sentenceText":"There''s something strange about this house.","sentenceWords":["There''s","something","strange","about","this","house"],"highlightingPart":"There''s something strange about","practiceQuestion":"How''s the new house?","sentenceTranslation":"이 집엔 뭔가 이상한 게 있어.","sentenceWordChoices":["strange","sort","There''s","about","house","this","kind","anything","something"],"practiceQuestionTranslation":"새집 어때?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/13/expressions/32/practice-examples/bba40bb6-e210-4c02-a60c-32f4934a4249.png","sentenceText":"There''s something charming about small towns.","sentenceWords":["There''s","something","charming","about","small","towns"],"highlightingPart":"There''s something charming about","practiceQuestion":"Why do you like small towns so much?","sentenceTranslation":"작은 동네에는 뭔가 매력이 있어.","sentenceWordChoices":["There''s","about","something","anything","towns","charming","sort","small","kind"],"practiceQuestionTranslation":"작은 동네를 왜 그렇게 좋아해?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/13/expressions/32/practice-examples/e961e2d0-0478-47cb-9c5f-6cdc0af95cdb.png","sentenceText":"There was something familiar about his voice.","sentenceWords":["There","was","something","familiar","about","his","voice"],"highlightingPart":"There was something familiar about","practiceQuestion":"Did you recognize him on the phone?","sentenceTranslation":"그의 목소리엔 뭔가 익숙한 게 있었어.","sentenceWordChoices":["kind","There","familiar","something","anything","was","about","voice","sort","his"],"practiceQuestionTranslation":"전화로 걔 알아챘어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/13/expressions/32/practice-examples/51207d24-259e-4b92-af5a-a4e26242ca1e.png","sentenceText":"There''s something off about this milk.","sentenceWords":["There''s","something","off","about","this","milk"],"highlightingPart":"There''s something off about","practiceQuestion":"Can you smell this milk?","sentenceTranslation":"이 우유 뭔가 이상해.","sentenceWordChoices":["this","about","anything","kind","There''s","off","sort","something","milk"],"practiceQuestionTranslation":"이 우유 냄새 맡아봐 줄래?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 32
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        33,
        13,
        'SCENARIO',
        'EMOTION_EMPATHY',
        'BASIC',
        'EN',
        'KR',
        4,
        'never seen anything like',
        '이런 건 처음 봐',
        '처음 겪는 일을 표현하는 never seen anything like',
        '''이런 건 처음 봐''라며 놀라움이나 신기함을 강조하는 표현입니다. 현재완료 have never와 anything like this가 만나 ''전례 없음''을 생생하게 전달해요.',
        'What''s the best trip you''ve ever taken?',
        '여태 다녀온 여행 중 최고는 어디였어요?',
        'Iceland — I''d never seen anything like the northern lights.',
        '아이슬란드요. 오로라 같은 건 처음 봤어요.',
        ARRAY['Iceland', 'I''d', 'never', 'seen', 'anything', 'like', 'the', 'northern', 'lights'],
        ARRAY['saw', 'never', 'I''d', 'northern', 'anything', 'ever', 'lights', 'something', 'Iceland', 'like', 'the', 'seen'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/13/expressions/33/practice-examples/5b858038-1443-455c-9279-bac5ceba0b1d.png","sentenceText":"I''ve never tasted anything like this.","sentenceWords":["I''ve","never","tasted","anything","like","this"],"highlightingPart":"never tasted anything like","practiceQuestion":"How''s the food here?","sentenceTranslation":"이런 맛은 처음이야.","sentenceWordChoices":["something","anything","tasted","I''ve","never","this","ever","like","saw"],"practiceQuestionTranslation":"여기 음식 어때?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/13/expressions/33/practice-examples/bb112966-b354-4ef6-a628-31d4932954f6.png","sentenceText":"The view was incredible. I''d never seen anything like it.","sentenceWords":["The","view","was","incredible","I''d","never","seen","anything","like","it"],"highlightingPart":"never seen anything like it","practiceQuestion":"How was the mountain hike?","sentenceTranslation":"경치가 엄청났어. 그런 건 처음 봤어.","sentenceWordChoices":["like","never","it","was","saw","seen","incredible","anything","The","ever","view","something","I''d"],"practiceQuestionTranslation":"산 등산 어땠어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/13/expressions/33/practice-examples/277ce5e5-721c-4e0b-a564-a5dc29e7f65e.png","sentenceText":"I''ve never heard anything like that before.","sentenceWords":["I''ve","never","heard","anything","like","that","before"],"highlightingPart":"never heard anything like that","practiceQuestion":"Did you hear what happened to him?","sentenceTranslation":"그런 얘긴 처음 들어봐.","sentenceWordChoices":["never","something","saw","like","before","ever","heard","anything","I''ve","that"],"practiceQuestionTranslation":"걔한테 무슨 일 있었는지 들었어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/13/expressions/33/practice-examples/a7e22053-18a3-46ea-8c28-f28bc7e0cf88.png","sentenceText":"I''ve never experienced anything like it.","sentenceWords":["I''ve","never","experienced","anything","like","it"],"highlightingPart":"never experienced anything like it","practiceQuestion":"How was the concert last night?","sentenceTranslation":"그런 경험은 처음이었어.","sentenceWordChoices":["anything","it","something","like","saw","ever","I''ve","experienced","never"],"practiceQuestionTranslation":"어젯밤 콘서트 어땠어?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 33
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        34,
        14,
        'SCENARIO',
        'GRAMMAR_FUNCTION_WORD',
        'BASIC',
        'EN',
        'KR',
        1,
        'get + 목적어 + p.p.',
        '~을 …되게 하다',
        '5형식으로 부탁하는 get + 목적어 + p.p.',
        '''~을 …되게 하다''라는 5형식 구조로, 특히 서비스나 요청 상황에서 유용합니다. get this fixed(이거 고치게 하다)처럼 내가 직접 하는 게 아니라 그렇게 되도록 만든다는 뉘앙스예요.',
        'Hi, how can I help you today?',
        '안녕하세요, 무엇을 도와드릴까요?',
        'My suitcase broke during the flight — I''d like to get it fixed or replaced.',
        '비행 중에 캐리어가 부서졌는데, 수리나 교체를 받고 싶어요.',
        ARRAY['My', 'suitcase', 'broke', 'during', 'the', 'flight', 'I''d', 'like', 'to', 'get', 'it', 'fixed', 'or', 'replaced'],
        ARRAY['suitcase', 'flight', 'get', 'or', 'I''d', 'broken', 'to', 'fixed', 'fix', 'broke', 'the', 'replaced', 'during', 'My', 'it', 'like', 'while'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/14/expressions/34/practice-examples/cc60b595-9afe-4492-b341-100aea65cf18.png","sentenceText":"I got my hair cut yesterday.","sentenceWords":["I","got","my","hair","cut","yesterday"],"highlightingPart":"got my hair cut","practiceQuestion":"You look different today.","sentenceTranslation":"어제 머리 잘랐어.","sentenceWordChoices":["gets","take","cut","I","yesterday","hair","my","making","got"],"practiceQuestionTranslation":"오늘 뭔가 달라 보인다."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/14/expressions/34/practice-examples/bb7e6c1c-7759-4e01-8483-dbae9376cc7d.png","sentenceText":"Can I get this gift wrapped?","sentenceWords":["Can","I","get","this","gift","wrapped"],"highlightingPart":"get this gift wrapped","practiceQuestion":"Can you wrap this as a gift here?","sentenceTranslation":"이거 선물 포장 되나요?","sentenceWordChoices":["making","wrapped","take","gets","I","Can","gift","get","this"],"practiceQuestionTranslation":"여기서 이거 선물 포장 돼요?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/14/expressions/34/practice-examples/2ba2e237-1f56-4af9-acad-c68d93549a2c.png","sentenceText":"I''d like to get this dry-cleaned.","sentenceWords":["I''d","like","to","get","this","dry-cleaned"],"highlightingPart":"get this dry-cleaned","practiceQuestion":"What do you need with that jacket?","sentenceTranslation":"이거 드라이클리닝 맡기고 싶어요.","sentenceWordChoices":["this","gets","making","to","take","dry-cleaned","I''d","like","get"],"practiceQuestionTranslation":"그 재킷 뭐가 필요해요?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/14/expressions/34/practice-examples/895b4475-ea6e-4ff9-9053-7cd66978d043.png","sentenceText":"We need to get the car washed.","sentenceWords":["We","need","to","get","the","car","washed"],"highlightingPart":"get the car washed","practiceQuestion":"The car looks really dirty.","sentenceTranslation":"세차 맡겨야겠어.","sentenceWordChoices":["gets","car","to","need","take","get","We","the","making","washed"],"practiceQuestionTranslation":"차가 진짜 더럽다."}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 34
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        35,
        14,
        'SCENARIO',
        'POLITE_EXPRESSION',
        'BASIC',
        'EN',
        'KR',
        2,
        'by any chance',
        '혹시',
        '혹시나 하고 묻는 by any chance',
        '''혹시''라며 조심스럽게 확인하거나 부탁할 때 붙이는 표현입니다. 문장 앞이나 뒤에 붙이면 질문의 어감이 훨씬 부드러워져요.',
        'Let me check what we can do for you.',
        '어떻게 도와드릴 수 있을지 확인해 볼게요.',
        'Is there any compensation available, by any chance?',
        '혹시 받을 수 있는 보상이 있을까요?',
        ARRAY['Is', 'there', 'any', 'compensation', 'available', 'by', 'any', 'chance'],
        ARRAY['available', 'there', 'chance', 'at', 'some', 'by', 'Is', 'chances', 'any', 'any', 'compensation'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/14/expressions/35/practice-examples/a7681a46-9337-4ca1-843b-6456519d1945.png","sentenceText":"Are you free tonight, by any chance?","sentenceWords":["Are","you","free","tonight","by","any","chance"],"highlightingPart":"by any chance","practiceQuestion":"Are you doing anything tonight?","sentenceTranslation":"혹시 오늘 밤에 시간 돼?","sentenceWordChoices":["perhaps","by","any","you","tonight","chance","Are","maybe","possibly","free"],"practiceQuestionTranslation":"오늘 밤에 뭐 해?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/14/expressions/35/practice-examples/25deca90-a53e-4ab6-9678-7b93496ebf4f.png","sentenceText":"By any chance, did you see my keys?","sentenceWords":["By","any","chance","did","you","see","my","keys"],"highlightingPart":"By any chance","practiceQuestion":"I can''t find anything in this mess.","sentenceTranslation":"혹시 내 열쇠 봤어?","sentenceWordChoices":["keys","perhaps","see","maybe","you","my","chance","did","any","By","possibly"],"practiceQuestionTranslation":"엉망이라 아무것도 못 찾겠어."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/14/expressions/35/practice-examples/0c10760f-fd8e-482b-b5a4-21bdf68cd0fa.png","sentenceText":"Is your name Julia, by any chance?","sentenceWords":["Is","your","name","Julia","by","any","chance"],"highlightingPart":"by any chance","practiceQuestion":"Excuse me, I think we''ve met before.","sentenceTranslation":"혹시 성함이 줄리아세요?","sentenceWordChoices":["maybe","perhaps","Is","name","your","by","possibly","Julia","any","chance"],"practiceQuestionTranslation":"실례지만, 우리 전에 만난 적 있는 것 같아요."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/14/expressions/35/practice-examples/5a0d8b1f-1986-4d5d-a571-2e1c89783e3a.png","sentenceText":"Do you know him, by any chance?","sentenceWords":["Do","you","know","him","by","any","chance"],"highlightingPart":"by any chance","practiceQuestion":"There''s a new guy in our class.","sentenceTranslation":"혹시 걔 알아?","sentenceWordChoices":["him","perhaps","know","by","possibly","you","any","maybe","chance","Do"],"practiceQuestionTranslation":"우리 반에 새로운 애가 왔어."}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 35
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        36,
        14,
        'SCENARIO',
        'TIME_PLANNING',
        'BASIC',
        'EN',
        'KR',
        3,
        'in the long run',
        '장기적으로 보면, 결국에는',
        '장기적 관점의 in the long run',
        '''장기적으로 보면'', ''결국에는''이라는 뜻입니다. 당장의 손해가 미래엔 이득이 된다는 식의 긴 안목을 말할 때 필수인 표현이에요. 반대는 in the short run이에요.',
        'We offer a cash refund, mileage points, or a travel voucher — which one would work best for you?',
        '현금 환불, 마일리지, 여행 바우처 중 어떤 게 제일 나으실까요?',
        'I''ll take the mileage points — they''re more useful in the long run.',
        '마일리지로 할게요. 길게 보면 그게 더 유용하니까요.',
        ARRAY['I''ll', 'take', 'the', 'mileage', 'points', 'they''re', 'more', 'useful', 'in', 'the', 'long', 'run'],
        ARRAY['run', 'I''ll', 'more', 'mileage', 'long', 'most', 'they''re', 'takes', 'short', 'the', 'useful', 'in', 'the', 'take', 'points'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/14/expressions/36/practice-examples/51c34a68-4130-4dad-982a-efebcafc9385.png","sentenceText":"Quality is cheaper in the long run.","sentenceWords":["Quality","is","cheaper","in","the","long","run"],"highlightingPart":"in the long run","practiceQuestion":"Cheap stuff is better, right?","sentenceTranslation":"길게 보면 좋은 물건이 오히려 싸게 먹혀.","sentenceWordChoices":["Quality","run","in","eventually","cheaper","the","is","term","short","long"],"practiceQuestionTranslation":"싼 게 더 낫지 않아?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/14/expressions/36/practice-examples/85c56df1-eb9d-4ee5-a59e-bd6140c639b4.png","sentenceText":"In the long run, honesty is the best policy.","sentenceWords":["In","the","long","run","honesty","is","the","best","policy"],"highlightingPart":"In the long run","practiceQuestion":"Should I just tell them the truth?","sentenceTranslation":"결국에는 정직이 최선이야.","sentenceWordChoices":["long","the","best","is","run","In","eventually","policy","short","the","term","honesty"],"practiceQuestionTranslation":"그냥 사실대로 말해야 할까?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/14/expressions/36/practice-examples/1b89fa3d-da8e-446b-a8eb-7d8a42911968.png","sentenceText":"Exercise saves you money in the long run.","sentenceWords":["Exercise","saves","you","money","in","the","long","run"],"highlightingPart":"in the long run","practiceQuestion":"Is it worth paying for a gym membership?","sentenceTranslation":"운동이 장기적으로는 돈 버는 거야.","sentenceWordChoices":["the","eventually","saves","money","term","run","Exercise","short","in","long","you"],"practiceQuestionTranslation":"헬스장 등록비 낼 가치가 있을까?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/14/expressions/36/practice-examples/7e35e5c2-157a-4e79-96f6-d1fb9162cfa0.png","sentenceText":"It hurts now, but it''s better in the long run.","sentenceWords":["It","hurts","now","but","it''s","better","in","the","long","run"],"highlightingPart":"in the long run","practiceQuestion":"This diet is so hard right now.","sentenceTranslation":"지금은 아파도 길게 보면 나은 선택이야.","sentenceWordChoices":["it''s","but","better","It","short","in","now","term","long","run","hurts","the","eventually"],"practiceQuestionTranslation":"이 다이어트 지금 너무 힘들어."}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 36
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        37,
        15,
        'SCENARIO',
        'TRAVEL_MOVEMENT',
        'BASIC',
        'EN',
        'KR',
        1,
        'check in',
        '체크인하다',
        '체크인하는 check in',
        '호텔이나 공항에서 ''체크인하다''라는 뜻의 필수 여행 표현입니다. check in on someone(~의 안부를 확인하다)으로 응용하면 활용도가 배가 돼요.',
        'Welcome! Will you be checking in with us today?',
        '어서 오세요! 오늘 체크인 도와드릴까요?',
        'Hi, I''d like to check in — I booked online under Kim.',
        '안녕하세요, 체크인하려고요. 김으로 온라인 예약했어요.',
        ARRAY['Hi', 'I''d', 'like', 'to', 'check', 'in', 'I', 'booked', 'online', 'under', 'Kim'],
        ARRAY['I', 'out', 'under', 'to', 'checked', 'online', 'in', 'booked', 'I''d', 'check', 'Kim', 'book', 'like', 'Hi'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/15/expressions/37/practice-examples/3a9d500d-9920-4e72-b450-ae617b12cee0.png","sentenceText":"We checked in online before the flight.","sentenceWords":["We","checked","in","online","before","the","flight"],"highlightingPart":"checked in","practiceQuestion":"Did you check in at the counter?","sentenceTranslation":"비행 전에 온라인 체크인했어.","sentenceWordChoices":["register","checked","flight","out","the","We","online","in","before","arrive"],"practiceQuestionTranslation":"카운터에서 체크인했어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/15/expressions/37/practice-examples/bfeaf059-c31c-4de9-b041-d50a5b2373ca.png","sentenceText":"I''d like to check in, please.","sentenceWords":["I''d","like","to","check","in","please"],"highlightingPart":"check in","practiceQuestion":"Welcome! How can I help you today?","sentenceTranslation":"체크인하고 싶은데요.","sentenceWordChoices":["register","out","like","I''d","in","please","arrive","to","check"],"practiceQuestionTranslation":"어서 오세요! 무엇을 도와드릴까요?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/15/expressions/37/practice-examples/8557e297-4e61-4020-a947-4566dc8dd252.png","sentenceText":"Check-in starts at 3 p.m.","sentenceWords":["Check-in","starts","at","3","p.m."],"highlightingPart":"Check-in","practiceQuestion":"What time can we check in?","sentenceTranslation":"체크인은 오후 3시부터입니다.","sentenceWordChoices":["at","Check-in","p.m.","3","out","starts","register","arrive"],"practiceQuestionTranslation":"몇 시부터 체크인 가능해요?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/15/expressions/37/practice-examples/bc03b37c-2968-4813-821c-ceae0b533cee.png","sentenceText":"I just called to check in on you.","sentenceWords":["I","just","called","to","check","in","on","you"],"highlightingPart":"check in on you","practiceQuestion":"Why are you calling out of nowhere?","sentenceTranslation":"그냥 잘 지내나 해서 전화했어.","sentenceWordChoices":["called","you","check","register","in","out","arrive","just","I","on","to"],"practiceQuestionTranslation":"갑자기 웬 전화야?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 37
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        38,
        15,
        'SCENARIO',
        'POLITE_EXPRESSION',
        'BASIC',
        'EN',
        'KR',
        2,
        'could use',
        '~이 있으면 딱 좋겠다',
        '넌지시 필요성을 말하는 could use',
        '''~이 절실하다'', ''~이 있으면 딱 좋겠다''를 돌려 말하는 표현입니다. I need보다 부드럽게 지금 나에게 필요한 것을 어필할 수 있어요.',
        'Do you have any preferences for your room?',
        '방 선호하시는 거 있으신가요?',
        'I could use a quiet room — it was a long flight.',
        '조용한 방이면 딱 좋겠어요. 비행이 길었거든요.',
        ARRAY['I', 'could', 'use', 'a', 'quiet', 'room', 'it', 'was', 'a', 'long', 'flight'],
        ARRAY['a', 'I', 'it', 'is', 'quietly', 'was', 'a', 'quiet', 'room', 'flight', 'could', 'use', 'can', 'long'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/15/expressions/38/practice-examples/a05a4ce6-20ba-49cc-8ef7-184676a4680c.png","sentenceText":"I could use a break.","sentenceWords":["I","could","use","a","break"],"highlightingPart":"could use","practiceQuestion":"You seem stressed lately.","sentenceTranslation":"좀 쉬어야겠어.","sentenceWordChoices":["break","might","could","a","would","use","can","I"],"practiceQuestionTranslation":"너 요즘 스트레스 받아 보인다."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/15/expressions/38/practice-examples/0c96da01-9368-4e6a-8ada-c10bfaf0aa3c.png","sentenceText":"You look like you could use some sleep.","sentenceWords":["You","look","like","you","could","use","some","sleep"],"highlightingPart":"could use","practiceQuestion":"You''ve been yawning all day.","sentenceTranslation":"너 잠 좀 자야 할 것 같아 보여.","sentenceWordChoices":["some","You","can","could","use","you","sleep","would","like","look","might"],"practiceQuestionTranslation":"너 하루 종일 하품하네."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/15/expressions/38/practice-examples/503bfd55-0c05-430c-a814-6bef2640c188.png","sentenceText":"We could use some good news.","sentenceWords":["We","could","use","some","good","news"],"highlightingPart":"could use","practiceQuestion":"Everything''s been going wrong this week.","sentenceTranslation":"좋은 소식이 좀 필요해.","sentenceWordChoices":["We","might","would","use","some","can","good","news","could"],"practiceQuestionTranslation":"이번 주에 되는 일이 하나도 없어."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/15/expressions/38/practice-examples/4441e46a-1908-49be-9b5c-6a8824cc10a6.png","sentenceText":"This room could use more light.","sentenceWords":["This","room","could","use","more","light"],"highlightingPart":"could use","practiceQuestion":"What do you think of this room?","sentenceTranslation":"이 방은 조명이 더 있으면 좋겠어.","sentenceWordChoices":["This","use","light","more","would","could","room","might","can"],"practiceQuestionTranslation":"이 방 어때?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 38
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        39,
        15,
        'SCENARIO',
        'TIME_PLANNING',
        'BASIC',
        'EN',
        'KR',
        3,
        'the hours',
        '근무 시간, 영업시간',
        '근무·영업시간을 묻는 the hours',
        'hours는 복수형으로 ''근무 시간'' 또는 ''영업시간''을 뜻합니다. What are the hours?라고 하면 시간대가 어떻게 되는지 통째로 묻는 말이 돼요.',
        'Here''s your key card. Is there anything else you''d like to know?',
        '카드키 드리겠습니다. 더 궁금하신 사항 있으신가요?',
        'Yes — what are the hours for breakfast?',
        '네, 조식 시간은 어떻게 되나요?',
        ARRAY['Yes', 'what', 'are', 'the', 'hours', 'for', 'breakfast'],
        ARRAY['Yes', 'at', 'the', 'are', 'what', 'is', 'breakfast', 'for', 'hours', 'hour'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/15/expressions/39/practice-examples/c7082052-c9a1-488e-95e3-8ce334281caa.png","sentenceText":"Do you know the hours for that cafe?","sentenceWords":["Do","you","know","the","hours","for","that","cafe"],"highlightingPart":"the hours","practiceQuestion":"I want to grab coffee later.","sentenceTranslation":"그 카페 영업시간 알아?","sentenceWordChoices":["you","for","hour","cafe","Do","know","the","hours","that","schedule","time"],"practiceQuestionTranslation":"이따가 커피 마시고 싶어."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/15/expressions/39/practice-examples/2b00129e-f509-462f-b30b-1fca4086db05.png","sentenceText":"The hours are 9 to 6, Monday to Friday.","sentenceWords":["The","hours","are","9","to","6","Monday","to","Friday"],"highlightingPart":"The hours","practiceQuestion":"What time do you work?","sentenceTranslation":"근무 시간은 월요일부터 금요일, 9시에서 6시야.","sentenceWordChoices":["9","schedule","time","The","hour","to","hours","are","Monday","6","to","Friday"],"practiceQuestionTranslation":"근무 시간이 어떻게 돼?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/15/expressions/39/practice-examples/37cd8d5b-6e5f-4da9-84ed-1f59b64c1bfc.png","sentenceText":"I work long hours these days.","sentenceWords":["I","work","long","hours","these","days"],"highlightingPart":"long hours","practiceQuestion":"You look exhausted.","sentenceTranslation":"요즘 장시간 근무하고 있어.","sentenceWordChoices":["I","schedule","hour","long","days","hours","these","time","work"],"practiceQuestionTranslation":"너 완전 지쳐 보인다."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/15/expressions/39/practice-examples/e37c5353-e98d-46ec-93c0-54973773c89a.png","sentenceText":"Their hours are posted on the door.","sentenceWords":["Their","hours","are","posted","on","the","door"],"highlightingPart":"Their hours","practiceQuestion":"How do we know when they''re open?","sentenceTranslation":"영업시간은 문에 붙어 있어요.","sentenceWordChoices":["the","door","time","hour","hours","schedule","posted","are","on","Their"],"practiceQuestionTranslation":"여기 언제 여는지 어떻게 알아?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 39
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        40,
        15,
        'SCENARIO',
        'TRAVEL_MOVEMENT',
        'BASIC',
        'EN',
        'KR',
        4,
        'a ~-minute walk from',
        '~에서 걸어서 몇 분 거리',
        '거리를 표현하는 a ~-minute walk from',
        '''~에서 걸어서 몇 분 거리''라고 거리와 소요 시간을 한 번에 전달하는 표현입니다. a five-minute walk처럼 하이픈으로 묶인 형용사 형태가 포인트예요.',
        'Enjoy your stay! Do you need directions anywhere?',
        '편안한 숙박 되세요! 어디 가시는 길 안내가 필요하세요?',
        'Is the subway station a five-minute walk from here?',
        '지하철역이 여기서 걸어서 5분 거리인가요?',
        ARRAY['Is', 'the', 'subway', 'station', 'a', 'five-minute', 'walk', 'from', 'here'],
        ARRAY['walks', 'subway', 'from', 'Is', 'five-minute', 'station', 'minutes', 'a', 'to', 'walk', 'here', 'the'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/15/expressions/40/practice-examples/9e84dc53-eccc-4dea-b4d3-312229300afd.png","sentenceText":"My office is a ten-minute walk from the subway.","sentenceWords":["My","office","is","a","ten-minute","walk","from","the","subway"],"highlightingPart":"a ten-minute walk from","practiceQuestion":"Is your office near the subway?","sentenceTranslation":"내 사무실은 지하철에서 걸어서 10분이야.","sentenceWordChoices":["is","a","My","to","subway","from","office","away","ten-minute","the","walk","minutes"],"practiceQuestionTranslation":"사무실이 지하철에서 가까워?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/15/expressions/40/practice-examples/7dcc5fa5-2ca1-42f7-ab23-dec26b43f711.png","sentenceText":"The beach is just a short walk from the hotel.","sentenceWords":["The","beach","is","just","a","short","walk","from","the","hotel"],"highlightingPart":"a short walk from","practiceQuestion":"Is the beach far from our hotel?","sentenceTranslation":"해변은 호텔에서 걸어서 금방이야.","sentenceWordChoices":["from","The","just","hotel","to","a","short","walk","is","minutes","the","away","beach"],"practiceQuestionTranslation":"해변이 우리 호텔에서 멀어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/15/expressions/40/practice-examples/32f5dabb-4473-4425-9eb7-d6623c79a0da.png","sentenceText":"It''s a twenty-minute drive from downtown.","sentenceWords":["It''s","a","twenty-minute","drive","from","downtown"],"highlightingPart":"a twenty-minute drive from","practiceQuestion":"How far is it from downtown?","sentenceTranslation":"시내에서 차로 20분 거리야.","sentenceWordChoices":["away","from","minutes","a","downtown","to","drive","twenty-minute","It''s"],"practiceQuestionTranslation":"시내에서 얼마나 멀어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/15/expressions/40/practice-examples/cb7c06e5-b0e7-4574-a83c-e62022dfead2.png","sentenceText":"The cafe is within walking distance.","sentenceWords":["The","cafe","is","within","walking","distance"],"highlightingPart":"within walking distance","practiceQuestion":"Do we need to take a taxi to the cafe?","sentenceTranslation":"그 카페는 걸어갈 수 있는 거리에 있어.","sentenceWordChoices":["The","is","away","distance","cafe","walking","minutes","to","within"],"practiceQuestionTranslation":"그 카페 택시 타고 가야 해?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 40
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        41,
        16,
        'SCENARIO',
        'POLITE_EXPRESSION',
        'BASIC',
        'EN',
        'KR',
        1,
        'I don''t think I can make it',
        '못 갈 것 같아',
        '애매하게 거절하는 I don''t think I can make it',
        '''못 갈 것 같아''라며 초대를 부드럽게 거절하는 정석 표현입니다. make it은 ''시간 맞춰 가다, 참석하다''라는 뜻이고, I can''t go보다 훨씬 완곡해요.',
        'Are you free tonight? Would you like to grab a coffee with me?',
        '오늘 저녁에 시간 돼? 같이 커피 한잔할래?',
        'Sorry, I don''t think I can make it tonight.',
        '미안, 오늘 밤엔 안 될 것 같아.',
        ARRAY['Sorry', 'I', 'don''t', 'think', 'I', 'can', 'make', 'it', 'tonight'],
        ARRAY['don''t', 'I', 'tonight', 'make', 'can', 'this', 'it', 'Sorry', 'I', 'makes', 'think', 'won''t'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/16/expressions/41/practice-examples/07273b85-78e0-45cb-a367-d243e3e79b17.png","sentenceText":"I don''t think I can make it to the meeting.","sentenceWords":["I","don''t","think","I","can","make","it","to","the","meeting"],"highlightingPart":"I don''t think I can make it","practiceQuestion":"Will you be at the meeting?","sentenceTranslation":"회의에 못 들어갈 것 같아요.","sentenceWordChoices":["made","come","can","makes","don''t","the","to","it","I","make","meeting","I","think"],"practiceQuestionTranslation":"회의에 들어올 거야?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/16/expressions/41/practice-examples/54950533-613b-43aa-ba82-a31bff5436d5.png","sentenceText":"Can you make it on Saturday?","sentenceWords":["Can","you","make","it","on","Saturday"],"highlightingPart":"make it","practiceQuestion":"We''re having a get-together this weekend.","sentenceTranslation":"토요일에 올 수 있어?","sentenceWordChoices":["make","made","makes","on","Saturday","you","Can","it","come"],"practiceQuestionTranslation":"이번 주말에 모임 있어."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/16/expressions/41/practice-examples/ed860cb0-5576-4515-8740-9806e91b10e5.png","sentenceText":"I''m afraid I can''t make it this time.","sentenceWords":["I''m","afraid","I","can''t","make","it","this","time"],"highlightingPart":"can''t make it","practiceQuestion":"We''d love for you to join us for dinner.","sentenceTranslation":"아쉽지만 이번엔 못 가겠어.","sentenceWordChoices":["make","afraid","I''m","I","it","made","time","come","can''t","this","makes"],"practiceQuestionTranslation":"같이 저녁 먹으러 오면 좋겠어."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/16/expressions/41/practice-examples/244ef9fe-daaf-40f6-8da6-b25fb3618ef4.png","sentenceText":"She couldn''t make it because of work.","sentenceWords":["She","couldn''t","make","it","because","of","work"],"highlightingPart":"couldn''t make it","practiceQuestion":"Where was Emily at the party?","sentenceTranslation":"걔 일 때문에 못 왔어.","sentenceWordChoices":["it","come","made","She","make","of","because","couldn''t","work","makes"],"practiceQuestionTranslation":"파티에 에밀리 어디 있었어?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 41
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        42,
        16,
        'SCENARIO',
        'EMOTION_EMPATHY',
        'BASIC',
        'EN',
        'KR',
        2,
        'feel the same way',
        '나도 같은 마음이다',
        '동감을 나타내는 feel the same way',
        '''나도 같은 마음이야'', ''동감이야''라고 상대의 감정이나 의견에 공감하는 표현입니다. 감정적인 주제에서 agree보다 따뜻한 느낌을 줘요.',
        'You''re totally my type.',
        '너 완전 내 스타일이거든.',
        'I''m flattered, but I don''t feel the same way.',
        '고맙지만, 난 같은 마음이 아니야.',
        ARRAY['I''m', 'flattered', 'but', 'I', 'don''t', 'feel', 'the', 'same', 'way'],
        ARRAY['way', 'I', 'I''m', 'same', 'feels', 'flattered', 'so', 'but', 'feel', 'don''t', 'the', 'ways'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/16/expressions/42/practice-examples/976230df-bd59-4d45-ac0a-12d8870b212b.png","sentenceText":"I feel the same way about him.","sentenceWords":["I","feel","the","same","way","about","him"],"highlightingPart":"feel the same way","practiceQuestion":"I think he''s really talented.","sentenceTranslation":"걔에 대해서 나도 똑같이 느껴.","sentenceWordChoices":["about","I","feel","think","him","feels","the","agree","same","way"],"practiceQuestionTranslation":"걔 진짜 재능 있는 것 같아."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/16/expressions/42/practice-examples/1188dd43-51a6-4c96-a53a-9e4510dc7459.png","sentenceText":"Do you feel the same way?","sentenceWords":["Do","you","feel","the","same","way"],"highlightingPart":"feel the same way","practiceQuestion":"I really think we should move to a bigger city.","sentenceTranslation":"너도 같은 마음이야?","sentenceWordChoices":["Do","the","feels","think","way","you","agree","feel","same"],"practiceQuestionTranslation":"나 진짜 더 큰 도시로 이사 가야 한다고 생각해."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/16/expressions/42/practice-examples/080da4c0-855f-4205-9aca-1f17cfd3056e.png","sentenceText":"I felt the same way when I first moved here.","sentenceWords":["I","felt","the","same","way","when","I","first","moved","here"],"highlightingPart":"felt the same way","practiceQuestion":"I feel so lost since I moved to this city.","sentenceTranslation":"나도 처음 이사 왔을 때 똑같이 느꼈어.","sentenceWordChoices":["feels","same","first","I","when","here","moved","the","agree","I","felt","think","way"],"practiceQuestionTranslation":"이 도시로 이사 온 뒤로 너무 막막해."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/16/expressions/42/practice-examples/12bef346-f28e-428d-baa7-6cf3a4f0b3c0.png","sentenceText":"Glad to hear you feel the same way.","sentenceWords":["Glad","to","hear","you","feel","the","same","way"],"highlightingPart":"feel the same way","practiceQuestion":"Honestly, I think we should just stay friends.","sentenceTranslation":"너도 같은 마음이라니 다행이다.","sentenceWordChoices":["the","agree","hear","think","to","Glad","you","feels","same","way","feel"],"practiceQuestionTranslation":"솔직히 우리 그냥 친구로 지내는 게 좋을 것 같아."}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 42
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        43,
        16,
        'SCENARIO',
        'OPINION_JUDGMENT',
        'BASIC',
        'EN',
        'KR',
        3,
        'have nothing to do with',
        '~과 아무 상관이 없다',
        '무관함을 말하는 have nothing to do with',
        '''~과는 아무 상관이 없다''라는 뜻으로, 관련성을 부정할 때 씁니다. 오해를 풀거나 책임 소재를 분명히 할 때 아주 유용해요.',
        'Can I ask why? Is it something I did?',
        '이유 물어봐도 돼? 내가 뭐 잘못했나?',
        'It has nothing to do with you — I''m just not looking to date right now.',
        '너랑은 아무 상관 없어. 그냥 지금은 연애 생각이 없어.',
        ARRAY['It', 'has', 'nothing', 'to', 'do', 'with', 'you', 'I''m', 'just', 'not', 'looking', 'to', 'date', 'right', 'now'],
        ARRAY['right', 'has', 'not', 'to', 'to', 'something', 'just', 'I''m', 'doing', 'date', 'do', 'you', 'now', 'look', 'with', 'It', 'nothing', 'looking'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/16/expressions/43/practice-examples/0200f43e-8a92-47eb-a49f-33ee2fff4130.png","sentenceText":"My decision has nothing to do with money.","sentenceWords":["My","decision","has","nothing","to","do","with","money"],"highlightingPart":"has nothing to do with","practiceQuestion":"Did you decide this because of money?","sentenceTranslation":"내 결정은 돈이랑은 상관없어.","sentenceWordChoices":["something","nothing","to","concerns","has","do","My","money","decision","related","with"],"practiceQuestionTranslation":"이거 돈 때문에 결정한 거야?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/16/expressions/43/practice-examples/611e3921-06c4-4e91-90a0-eb1b01c4d4d4.png","sentenceText":"It has nothing to do with what happened yesterday.","sentenceWords":["It","has","nothing","to","do","with","what","happened","yesterday"],"highlightingPart":"has nothing to do with","practiceQuestion":"Is this because of what happened yesterday?","sentenceTranslation":"어제 일이랑은 전혀 관계없어.","sentenceWordChoices":["what","happened","do","nothing","yesterday","related","something","with","to","has","It","concerns"],"practiceQuestionTranslation":"이거 어제 일 때문이야?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/16/expressions/43/practice-examples/f3f454b0-83cf-4059-9763-004855028da4.png","sentenceText":"I had nothing to do with the mistake.","sentenceWords":["I","had","nothing","to","do","with","the","mistake"],"highlightingPart":"had nothing to do with","practiceQuestion":"Who''s responsible for this mistake?","sentenceTranslation":"그 실수는 나랑 무관해.","sentenceWordChoices":["do","something","concerns","nothing","to","related","had","with","I","the","mistake"],"practiceQuestionTranslation":"이 실수 누구 책임이야?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/16/expressions/43/practice-examples/df7f4b8b-3b77-4d51-bf8c-8dcf49f06f54.png","sentenceText":"Age has nothing to do with it.","sentenceWords":["Age","has","nothing","to","do","with","it"],"highlightingPart":"has nothing to do with it","practiceQuestion":"Maybe you''re just too old for this job.","sentenceTranslation":"나이는 그거랑 아무 상관 없어.","sentenceWordChoices":["with","do","nothing","related","it","concerns","Age","has","to","something"],"practiceQuestionTranslation":"너 그냥 이 일 하기엔 너무 나이 든 거 아니야?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 43
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        44,
        16,
        'SCENARIO',
        'POLITE_EXPRESSION',
        'BASIC',
        'EN',
        'KR',
        4,
        'No offense, but',
        '기분 나쁘게 듣지 마, 근데',
        '솔직한 말 앞에 붙이는 No offense, but',
        '''기분 나쁘게 듣지 마'', ''악의는 없는데''라며 껄끄러운 말의 충격을 줄여주는 쿠션 표현입니다. 듣는 쪽은 None taken(기분 안 나빠)으로 받아칠 수 있어요.',
        'Could I at least get your Instagram, then?',
        '그럼 인스타그램이라도 받을 수 있을까?',
        'No offense, but I''d rather not share my socials.',
        '기분 나쁘게 듣지 마, 근데 SNS는 공유 안 하고 싶어.',
        ARRAY['No', 'offense', 'but', 'I''d', 'rather', 'not', 'share', 'my', 'socials'],
        ARRAY['offense', 'sharing', 'rather', 'offensive', 'share', 'my', 'not', 'socials', 'would', 'but', 'No', 'I''d'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/16/expressions/44/practice-examples/b36a7ede-80da-4682-b570-456b845c263d.png","sentenceText":"No offense, but your music is too loud.","sentenceWords":["No","offense","but","your","music","is","too","loud"],"highlightingPart":"No offense, but","practiceQuestion":"Is my music bothering you?","sentenceTranslation":"악의는 없는데, 음악 소리가 너무 커.","sentenceWordChoices":["music","but","offense","honestly","No","offend","loud","offensive","too","your","is"],"practiceQuestionTranslation":"내 음악 거슬려?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/16/expressions/44/practice-examples/4789b38a-fb10-453a-97bb-59f87392481a.png","sentenceText":"No offense, but I''d rather go alone.","sentenceWords":["No","offense","but","I''d","rather","go","alone"],"highlightingPart":"No offense, but","practiceQuestion":"Want me to come with you?","sentenceTranslation":"기분 나빠하지 마, 근데 난 혼자 가는 게 좋아.","sentenceWordChoices":["honestly","go","offend","No","but","rather","alone","offense","offensive","I''d"],"practiceQuestionTranslation":"내가 같이 가줄까?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/16/expressions/44/practice-examples/05e78a00-c616-4bea-a83f-9a1e19318a68.png","sentenceText":"No offense taken. You''re right.","sentenceWords":["No","offense","taken","You''re","right"],"highlightingPart":"No offense taken","practiceQuestion":"Sorry, I didn''t mean to sound harsh.","sentenceTranslation":"기분 안 나빠. 네 말이 맞아.","sentenceWordChoices":["taken","offensive","You''re","right","honestly","offense","offend","No"],"practiceQuestionTranslation":"미안, 그렇게 세게 들릴 줄 몰랐어."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/16/expressions/44/practice-examples/64113245-5a9e-47f1-bcac-d72ca4273def.png","sentenceText":"No offense, but that plan sounds risky.","sentenceWords":["No","offense","but","that","plan","sounds","risky"],"highlightingPart":"No offense, but","practiceQuestion":"What do you think of my plan?","sentenceTranslation":"나쁘게 듣지 마, 근데 그 계획 위험해 보여.","sentenceWordChoices":["plan","honestly","but","sounds","that","offensive","risky","offense","offend","No"],"practiceQuestionTranslation":"내 계획 어때?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 44
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        45,
        17,
        'SCENARIO',
        'DAILY_ROUTINE',
        'BASIC',
        'EN',
        'KR',
        1,
        'feel like ~ing',
        '~하고 싶다, ~이 당기다',
        '당기는 것을 말하는 feel like ~ing',
        '''~하고 싶다'', ''~이 당긴다''를 표현하는 대표적인 회화 표현입니다. want to보다 즉흥적인 기분이나 입맛을 말할 때 어울리고, feel like 뒤에 동명사나 명사가 와요.',
        'Hi there! What can I get started for you today?',
        '안녕하세요! 오늘 뭐로 준비해 드릴까요?',
        'I feel like something sweet today.',
        '오늘은 단 게 당기네요.',
        ARRAY['I', 'feel', 'like', 'something', 'sweet', 'today'],
        ARRAY['sweet', 'today', 'sweets', 'anything', 'feels', 'I', 'something', 'feel', 'like'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/17/expressions/45/practice-examples/56b3fdb1-6788-4dcd-aa51-3f96b1bff250.png","sentenceText":"I feel like watching a movie.","sentenceWords":["I","feel","like","watching","a","movie"],"highlightingPart":"feel like watching","practiceQuestion":"What do you feel like doing tonight?","sentenceTranslation":"영화 보고 싶은 기분이야.","sentenceWordChoices":["watching","I","like","fancy","feels","want","movie","a","feel"],"practiceQuestionTranslation":"오늘 밤에 뭐 하고 싶어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/17/expressions/45/practice-examples/789bf0f5-712f-428d-a3c9-1895d0a8a3e2.png","sentenceText":"I don''t feel like cooking today.","sentenceWords":["I","don''t","feel","like","cooking","today"],"highlightingPart":"feel like cooking","practiceQuestion":"Should we cook dinner tonight?","sentenceTranslation":"오늘은 요리하기 싫어.","sentenceWordChoices":["want","fancy","feels","today","I","feel","like","don''t","cooking"],"practiceQuestionTranslation":"오늘 저녁 우리가 해 먹을까?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/17/expressions/45/practice-examples/56b78a5f-7e8c-4e6c-9eb9-5d98fc719c2d.png","sentenceText":"Do you feel like going for a walk?","sentenceWords":["Do","you","feel","like","going","for","a","walk"],"highlightingPart":"feel like going","practiceQuestion":"It''s such a nice day outside.","sentenceTranslation":"산책하고 싶은 기분이야?","sentenceWordChoices":["feels","walk","going","fancy","for","want","a","you","Do","feel","like"],"practiceQuestionTranslation":"밖에 날씨 진짜 좋다."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/17/expressions/45/practice-examples/275e02de-914d-4cba-9c54-08b9d112078b.png","sentenceText":"I feel like something sweet.","sentenceWords":["I","feel","like","something","sweet"],"highlightingPart":"feel like something sweet","practiceQuestion":"What are you in the mood for?","sentenceTranslation":"단 게 당겨.","sentenceWordChoices":["I","feel","feels","something","want","fancy","sweet","like"],"practiceQuestionTranslation":"뭐가 당겨?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 45
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        46,
        17,
        'SCENARIO',
        'TRAVEL_MOVEMENT',
        'BASIC',
        'EN',
        'KR',
        2,
        'for here or to go',
        '드시고 가세요 아니면 포장이세요?',
        '카페·식당 필수 표현 for here or to go',
        '주문할 때 ''드시고 가세요, 아니면 포장이세요?''를 묻는 정형화된 표현입니다. 손님 입장에서는 For here, please 또는 To go, please로 간단히 답하면 돼요.',
        'Good choice! Is that for here or to go?',
        '좋은 선택이에요! 여기서 드시나요, 포장인가요?',
        'Can I get that to go, please?',
        '그거 포장으로 주시겠어요?',
        ARRAY['Can', 'I', 'get', 'that', 'to', 'go', 'please'],
        ARRAY['get', 'that', 'please', 'gets', 'here', 'for', 'to', 'go', 'Can', 'I'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/17/expressions/46/practice-examples/b5136275-3067-4729-b66b-a74f95d0bd4e.png","sentenceText":"Two burgers to go, please.","sentenceWords":["Two","burgers","to","go","please"],"highlightingPart":"to go","practiceQuestion":"What can I get you today?","sentenceTranslation":"버거 두 개 포장해 주세요.","sentenceWordChoices":["burgers","to","Two","go","staying","takeout","please","away"],"practiceQuestionTranslation":"오늘 뭐 드릴까요?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/17/expressions/46/practice-examples/e74912e0-63f0-43fb-8fd2-b9c0fc2a7f62.png","sentenceText":"Is this order for here?","sentenceWords":["Is","this","order","for","here"],"highlightingPart":"for here","practiceQuestion":"Here''s your total, that''ll be $12.","sentenceTranslation":"이 주문은 매장에서 드시는 건가요?","sentenceWordChoices":["Is","away","this","takeout","here","for","staying","order"],"practiceQuestionTranslation":"주문하신 거 12달러입니다."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/17/expressions/46/practice-examples/d82cb4b4-6ca3-4433-bea0-cd8ca87263cf.png","sentenceText":"I''ll have it for here.","sentenceWords":["I''ll","have","it","for","here"],"highlightingPart":"for here","practiceQuestion":"Are you eating here or taking it out?","sentenceTranslation":"여기서 먹고 갈게요.","sentenceWordChoices":["have","for","takeout","I''ll","away","here","it","staying"],"practiceQuestionTranslation":"여기서 드실 거예요, 포장이세요?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/17/expressions/46/practice-examples/9626d054-5331-48ee-9d5d-f87d9e8a0aad.png","sentenceText":"Can you make that to go instead?","sentenceWords":["Can","you","make","that","to","go","instead"],"highlightingPart":"to go","practiceQuestion":"Actually, I''m in a hurry.","sentenceTranslation":"그거 포장으로 바꿔주실 수 있나요?","sentenceWordChoices":["make","go","you","to","that","away","takeout","Can","instead","staying"],"practiceQuestionTranslation":"사실 제가 좀 급해서요."}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 46
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        47,
        17,
        'SCENARIO',
        'MONEY_SPENDING',
        'BASIC',
        'EN',
        'KR',
        3,
        'come in',
        '(사이즈·색상이) 나오다',
        '다른 옵션을 묻는 come in',
        '''(사이즈·색상·종류 등이) 나오다, 출시되다''라는 뜻으로 쇼핑·주문 필수 표현입니다. Does it come in ~?으로 물으면 다른 옵션이 있는지 확인할 수 있어요. 카페에서는 디카페인·우유 종류 같은 옵션을 물을 때 자연스러워요.',
        'Sure, one latte. Will that be all?',
        '네, 라떼 하나요. 그게 다이신가요?',
        'Actually, does it come in decaf?',
        '아, 근데 그거 디카페인으로도 나오나요?',
        ARRAY['Actually', 'does', 'it', 'come', 'in', 'decaf'],
        ARRAY['Actually', 'in', 'come', 'decaf', 'comes', 'does', 'it', 'at', 'do'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/17/expressions/47/practice-examples/09220eaa-45fb-45ed-8720-2d6956b0925d.png","sentenceText":"Does this shirt come in a medium?","sentenceWords":["Does","this","shirt","come","in","a","medium"],"highlightingPart":"come in","practiceQuestion":"I really like this shirt.","sentenceTranslation":"이 셔츠 미디엄 사이즈 있나요?","sentenceWordChoices":["Does","shirt","available","this","in","offered","a","medium","stock","come"],"practiceQuestionTranslation":"이 셔츠 진짜 마음에 든다."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/17/expressions/47/practice-examples/965d2efe-8601-488b-9567-4366cb29b5dd.png","sentenceText":"It comes in three colors.","sentenceWords":["It","comes","in","three","colors"],"highlightingPart":"comes in","practiceQuestion":"What colors does this bag come in?","sentenceTranslation":"그건 세 가지 색상으로 나와요.","sentenceWordChoices":["It","comes","stock","three","in","colors","offered","available"],"practiceQuestionTranslation":"이 가방 무슨 색으로 나와요?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/17/expressions/47/practice-examples/7a9fae1b-fab9-4955-bc71-fdeb90ad9e1d.png","sentenceText":"Do these shoes come in a bigger size?","sentenceWords":["Do","these","shoes","come","in","a","bigger","size"],"highlightingPart":"come in","practiceQuestion":"These shoes feel a bit tight.","sentenceTranslation":"이 신발 더 큰 사이즈 있어요?","sentenceWordChoices":["offered","a","bigger","stock","Do","come","available","shoes","size","these","in"],"practiceQuestionTranslation":"이 신발 좀 낀다."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/17/expressions/47/practice-examples/5bf40151-5098-42be-9f99-0ebe78f933ff.png","sentenceText":"The phone comes in two models.","sentenceWords":["The","phone","comes","in","two","models"],"highlightingPart":"comes in","practiceQuestion":"How many models does it have?","sentenceTranslation":"그 폰은 두 가지 모델로 나와.","sentenceWordChoices":["offered","phone","available","stock","two","models","comes","in","The"],"practiceQuestionTranslation":"모델이 몇 가지 나와?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 47
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        48,
        17,
        'SCENARIO',
        'CONVERSATION_SKILL',
        'BASIC',
        'EN',
        'KR',
        4,
        'sound',
        '~하게 들리다',
        'sound의 다양한 활용',
        'sound는 ''~하게 들리다''라는 뜻으로, 들은 내용에 대한 인상이나 반응을 표현합니다. sounds good, sounds like a plan처럼 리액션 표현의 핵심 동사예요.',
        'We''ve got a couple of specials today — want me to walk you through them?',
        '오늘 스페셜 메뉴가 몇 개 있는데, 설명해 드릴까요?',
        'Sure, that sounds great!',
        '네, 좋아요!',
        ARRAY['Sure', 'that', 'sounds', 'great'],
        ARRAY['greatly', 'this', 'great', 'Sure', 'that', 'sound', 'sounds'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/17/expressions/48/practice-examples/8e51e3d7-a980-4e64-a4ab-bfe438d5acc5.png","sentenceText":"That sounds like fun.","sentenceWords":["That","sounds","like","fun"],"highlightingPart":"sounds like","practiceQuestion":"We''re planning a picnic this weekend.","sentenceTranslation":"재밌겠다.","sentenceWordChoices":["seems","looks","That","like","sounds","heard","fun"],"practiceQuestionTranslation":"우리 이번 주말에 소풍 계획 중이야."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/17/expressions/48/practice-examples/f997437b-a091-4292-8202-be32a6960d1a.png","sentenceText":"You sound tired. Are you OK?","sentenceWords":["You","sound","tired","Are","you","OK"],"highlightingPart":"sound tired","practiceQuestion":"Hey, how''s it going?","sentenceTranslation":"목소리가 피곤해 보이네. 괜찮아?","sentenceWordChoices":["looks","tired","Are","you","heard","seems","You","OK","sound"],"practiceQuestionTranslation":"야, 잘 지내?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/17/expressions/48/practice-examples/692959f1-c427-433c-a78a-f147390405ca.png","sentenceText":"It sounds like you had a rough day.","sentenceWords":["It","sounds","like","you","had","a","rough","day"],"highlightingPart":"sounds like","practiceQuestion":"I had such a rough day.","sentenceTranslation":"힘든 하루 보낸 것 같네.","sentenceWordChoices":["rough","had","sounds","you","seems","day","It","like","a","looks","heard"],"practiceQuestionTranslation":"오늘 진짜 힘든 하루였어."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/17/expressions/48/practice-examples/e3a1cfc6-8fbf-454d-aca9-e1868d098a60.png","sentenceText":"Sounds like a plan!","sentenceWords":["Sounds","like","a","plan"],"highlightingPart":"Sounds like","practiceQuestion":"Let''s meet at 7 and grab dinner.","sentenceTranslation":"그렇게 하자! / 좋은 계획이야!","sentenceWordChoices":["like","plan","a","Sounds","seems","heard","looks"],"practiceQuestionTranslation":"7시에 만나서 저녁 먹자."}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 48
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        49,
        18,
        'SCENARIO',
        'DAILY_ROUTINE',
        'CLASSIC_COMMON',
        'EN',
        'KR',
        1,
        'under the weather',
        '몸이 좀 안 좋은, 컨디션이 처지는',
        '컨디션이 안 좋을 때 under the weather',
        '''몸이 좀 안 좋은'', ''컨디션이 처지는''이라는 뜻의 대표 관용구입니다. 크게 아픈 건 아니지만 감기 기운이나 피로로 상태가 별로일 때 쓰기 딱 좋아요.',
        'You''re not looking so great — what''s been going on?',
        '안색이 안 좋아 보이는데, 어디가 안 좋으세요?',
        'I''ve been feeling under the weather since yesterday.',
        '어제부터 몸이 좀 안 좋아요.',
        ARRAY['I''ve', 'been', 'feeling', 'under', 'the', 'weather', 'since', 'yesterday'],
        ARRAY['over', 'the', 'under', 'weather', 'feeling', 'I''ve', 'yesterday', 'for', 'been', 'felt', 'since'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/18/expressions/49/practice-examples/503a911c-9c53-4c19-8fad-6d96924d8293.png","sentenceText":"He''s under the weather, so he stayed home.","sentenceWords":["He''s","under","the","weather","so","he","stayed","home"],"highlightingPart":"under the weather","practiceQuestion":"Where''s Jake today?","sentenceTranslation":"걔 몸이 안 좋아서 집에 있어.","sentenceWordChoices":["so","weather","stayed","the","under","he","ill","home","over","He''s","sick"],"practiceQuestionTranslation":"오늘 제이크 어디 있어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/18/expressions/49/practice-examples/bd493cd1-35a8-4dab-87a7-e2b4849edf15.png","sentenceText":"You look a little under the weather. Are you OK?","sentenceWords":["You","look","a","little","under","the","weather","Are","you","OK"],"highlightingPart":"under the weather","practiceQuestion":"Hey, sorry I''m late.","sentenceTranslation":"너 컨디션 안 좋아 보인다. 괜찮아?","sentenceWordChoices":["OK","look","over","a","little","Are","under","you","the","ill","You","sick","weather"],"practiceQuestionTranslation":"아 미안, 늦었어."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/18/expressions/49/practice-examples/1d2929f8-5036-4471-8ccc-31d49b2b8410.png","sentenceText":"I''ve been under the weather since the trip.","sentenceWords":["I''ve","been","under","the","weather","since","the","trip"],"highlightingPart":"under the weather","practiceQuestion":"How have you been since you got back?","sentenceTranslation":"여행 다녀온 뒤로 계속 골골대.","sentenceWordChoices":["been","ill","under","over","the","trip","weather","the","since","I''ve","sick"],"practiceQuestionTranslation":"돌아온 뒤로 어떻게 지냈어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/18/expressions/49/practice-examples/dbece0c7-8505-4db0-ab4d-7cf2d6657c56.png","sentenceText":"If you''re under the weather, get some rest.","sentenceWords":["If","you''re","under","the","weather","get","some","rest"],"highlightingPart":"under the weather","practiceQuestion":"I don''t feel so good today.","sentenceTranslation":"몸이 안 좋으면 좀 쉬어.","sentenceWordChoices":["you''re","under","over","rest","sick","the","ill","weather","If","get","some"],"practiceQuestionTranslation":"오늘 몸이 안 좋아."}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 49
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        50,
        18,
        'SCENARIO',
        'EMOTION_EMPATHY',
        'BASIC',
        'EN',
        'KR',
        2,
        'get over',
        '이겨내다, 극복하다',
        '극복을 말하는 get over',
        '''(슬픔·병·충격을) 이겨내다, 극복하다''라는 뜻입니다. 이별, 감기, 실망 등 마음이나 몸의 어려움에서 회복될 때 두루 쓰여요.',
        'How long have you been feeling like this?',
        '언제부터 그랬어요?',
        'A few days now — I just can''t get over this headache.',
        '며칠 됐어요. 이 두통이 도무지 낫질 않네요.',
        ARRAY['A', 'few', 'days', 'now', 'I', 'just', 'can''t', 'get', 'over', 'this', 'headache'],
        ARRAY['A', 'now', 'won''t', 'this', 'just', 'days', 'I', 'over', 'under', 'can''t', 'get', 'few', 'got', 'headache'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/18/expressions/50/practice-examples/3c8b09ca-1f3c-4670-87ac-bfaca02a61e0.png","sentenceText":"I can''t get over how good this is!","sentenceWords":["I","can''t","get","over","how","good","this","is"],"highlightingPart":"get over","practiceQuestion":"How do you like the food here?","sentenceTranslation":"이거 너무 좋아서 믿기지가 않아!","sentenceWordChoices":["this","is","can''t","I","overcome","over","beat","how","good","recover","get"],"practiceQuestionTranslation":"여기 음식 어때?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/18/expressions/50/practice-examples/6bcc8f32-4de4-4575-80ee-78c6a6fd12a0.png","sentenceText":"He still hasn''t gotten over his cold.","sentenceWords":["He","still","hasn''t","gotten","over","his","cold"],"highlightingPart":"gotten over","practiceQuestion":"Is Tom coming back to work soon?","sentenceTranslation":"걔 아직 감기가 다 안 나았어.","sentenceWordChoices":["still","cold","over","recover","beat","his","He","gotten","overcome","hasn''t"],"practiceQuestionTranslation":"탐 곧 복귀해?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/18/expressions/50/practice-examples/df4bac31-b288-4a76-9c5e-72fbb7be1b87.png","sentenceText":"You''ll get over it in no time.","sentenceWords":["You''ll","get","over","it","in","no","time"],"highlightingPart":"get over it","practiceQuestion":"I''m still so upset about the breakup.","sentenceTranslation":"금방 훌훌 털어낼 거야.","sentenceWordChoices":["overcome","recover","get","You''ll","over","in","no","it","beat","time"],"practiceQuestionTranslation":"아직도 이별 때문에 너무 속상해."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/18/expressions/50/practice-examples/7260ae83-d021-4cc6-ac05-cf806cbbb3a4.png","sentenceText":"She got over her fear of flying.","sentenceWords":["She","got","over","her","fear","of","flying"],"highlightingPart":"got over","practiceQuestion":"Didn''t she used to hate flying?","sentenceTranslation":"걔는 비행 공포증을 극복했어.","sentenceWordChoices":["flying","her","of","overcome","fear","got","She","recover","beat","over"],"practiceQuestionTranslation":"걔 원래 비행기 타는 거 싫어하지 않았어?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 50
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        51,
        18,
        'SCENARIO',
        'GRAMMAR_FUNCTION_WORD',
        'BASIC',
        'EN',
        'KR',
        3,
        'from',
        '~ 때문에, ~로 인해',
        '원인을 나타내는 from',
        '전치사 from은 ''~ 때문에'', ''~로 인해''라며 원인을 표현할 수 있습니다. get a headache from(~때문에 두통이 생기다)처럼 몸 상태의 원인을 말할 때 특히 자주 쓰여요.',
        'Any fever, or is it more of an achy, tired kind of thing?',
        '열도 있어요, 아니면 몸살처럼 쑤시고 피곤한 거예요?',
        'No fever — I think it''s from walking around all day in the sun.',
        '열은 없어요. 하루 종일 땡볕에 걸어 다녀서 그런 것 같아요.',
        ARRAY['No', 'fever', 'I', 'think', 'it''s', 'from', 'walking', 'around', 'all', 'day', 'in', 'the', 'sun'],
        ARRAY['No', 'it''s', 'walking', 'think', 'from', 'in', 'the', 'all', 'I', 'by', 'fever', 'sun', 'walk', 'around', 'day', 'of'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/18/expressions/51/practice-examples/4f8b7f55-5266-4fe6-be52-80b2a7aaf935.png","sentenceText":"I''m sore from yesterday''s workout.","sentenceWords":["I''m","sore","from","yesterday''s","workout"],"highlightingPart":"from","practiceQuestion":"Why are you walking so slowly?","sentenceTranslation":"어제 운동 때문에 몸이 뻐근해.","sentenceWordChoices":["from","sore","yesterday''s","by","workout","I''m","of","because"],"practiceQuestionTranslation":"왜 그렇게 천천히 걸어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/18/expressions/51/practice-examples/0ad399a2-ce9a-4af5-a60b-f7e9bca61029.png","sentenceText":"She got a cold from the rain.","sentenceWords":["She","got","a","cold","from","the","rain"],"highlightingPart":"from","practiceQuestion":"Why is she coughing?","sentenceTranslation":"걔 비 맞아서 감기 걸렸어.","sentenceWordChoices":["from","the","a","got","rain","because","cold","She","by","of"],"practiceQuestionTranslation":"걔 왜 기침해?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/18/expressions/51/practice-examples/c1e0fee4-b5e5-4ad7-89d4-d1b8ca777b94.png","sentenceText":"My throat hurts from talking all day.","sentenceWords":["My","throat","hurts","from","talking","all","day"],"highlightingPart":"from","practiceQuestion":"You sound hoarse.","sentenceTranslation":"하루 종일 말했더니 목이 아파.","sentenceWordChoices":["by","talking","from","of","all","day","throat","My","hurts","because"],"practiceQuestionTranslation":"너 목소리 쉬었다."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/18/expressions/51/practice-examples/5c65378b-d50f-47ec-925a-2b9db181c751.png","sentenceText":"He''s exhausted from the long flight.","sentenceWords":["He''s","exhausted","from","the","long","flight"],"highlightingPart":"from","practiceQuestion":"Why does he look so tired?","sentenceTranslation":"걔 장거리 비행 때문에 녹초가 됐어.","sentenceWordChoices":["by","the","of","long","from","He''s","flight","because","exhausted"],"practiceQuestionTranslation":"걔 왜 저렇게 피곤해 보여?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 51
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        52,
        18,
        'SCENARIO',
        'OPINION_JUDGMENT',
        'BASIC',
        'EN',
        'KR',
        4,
        'as far as I know',
        '내가 알기로는',
        '아는 범위를 한정하는 as far as I know',
        '''내가 알기로는''이라며 내 정보의 한계를 밝히고 말하는 표현입니다. 확실하지 않은 정보를 전달할 때 책임을 덜어주는 안전장치 역할을 해요.',
        'Are you allergic to anything, or taking any other medication?',
        '알레르기 있는 거나 지금 먹고 있는 다른 약 있어요?',
        'As far as I know, I''m not allergic to anything.',
        '제가 알기로는 알레르기는 없어요.',
        ARRAY['As', 'far', 'as', 'I', 'know', 'I''m', 'not', 'allergic', 'to', 'anything'],
        ARRAY['long', 'not', 'I', 'allergic', 'know', 'anything', 'to', 'As', 'far', 'as', 'I''m', 'knew', 'something'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/18/expressions/52/practice-examples/a35a0179-7625-414c-bfd2-ed7cef6131ee.png","sentenceText":"As far as I know, she still lives in Busan.","sentenceWords":["As","far","as","I","know","she","still","lives","in","Busan"],"highlightingPart":"As far as I know","practiceQuestion":"Does she still live in Busan?","sentenceTranslation":"내가 알기로 걔 아직 부산 살아.","sentenceWordChoices":["in","knew","lives","still","think","I","far","as","Busan","long","As","know","she"],"practiceQuestionTranslation":"걔 아직 부산 살아?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/18/expressions/52/practice-examples/95b60b19-f6b1-4af2-8215-ac1e5405f732.png","sentenceText":"As far as I know, the tickets are sold out.","sentenceWords":["As","far","as","I","know","the","tickets","are","sold","out"],"highlightingPart":"As far as I know","practiceQuestion":"Can we still get tickets for tonight?","sentenceTranslation":"내가 알기론 표 다 매진됐어.","sentenceWordChoices":["far","sold","out","long","tickets","as","As","think","knew","the","are","know","I"],"practiceQuestionTranslation":"오늘 밤 표 아직 구할 수 있을까?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/18/expressions/52/practice-examples/cb117aa0-db46-4224-bfe0-b4bdbb046941.png","sentenceText":"He''s never been late, as far as I know.","sentenceWords":["He''s","never","been","late","as","far","as","I","know"],"highlightingPart":"as far as I know","practiceQuestion":"Do you think Tom will be late again?","sentenceTranslation":"내가 아는 한 걔가 지각한 적은 없어.","sentenceWordChoices":["knew","as","been","long","I","know","late","think","as","far","He''s","never"],"practiceQuestionTranslation":"탐 이번에도 늦을 것 같아?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/18/expressions/52/practice-examples/057ac299-8fc4-4a12-aade-cf831ac3459e.png","sentenceText":"As far as I know, nothing has changed.","sentenceWords":["As","far","as","I","know","nothing","has","changed"],"highlightingPart":"As far as I know","practiceQuestion":"Has anything changed with the schedule?","sentenceTranslation":"내가 알기로는 바뀐 거 없어.","sentenceWordChoices":["As","nothing","far","I","know","long","think","as","changed","has","knew"],"practiceQuestionTranslation":"일정에 뭐 바뀐 거 있어?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 52
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        53,
        19,
        'SCENARIO',
        'OPINION_JUDGMENT',
        'BASIC',
        'EN',
        'KR',
        1,
        'be familiar with',
        '~을 알고 있다, ~에 익숙하다',
        '알고 있는지 묻는 be familiar with',
        '''~을 알고 있어?'', ''~에 익숙해?''라고 상대의 배경지식을 확인하는 표현입니다. Do you know보다 ''접해본 적 있는지''를 묻는 뉘앙스라 좀 더 세련돼요.',
        'Hi, you look a little lost — do you need some help?',
        '안녕하세요, 길을 잃으신 것 같은데 도와드릴까요?',
        'Yes, please — I''m not familiar with this area. Which exit is closest to Tower Bridge?',
        '네, 제가 이 지역을 잘 몰라서요. 어느 출구가 타워브리지에 제일 가까워요?',
        ARRAY['Yes', 'please', 'I''m', 'not', 'familiar', 'with', 'this', 'area', 'Which', 'exit', 'is', 'closest', 'to', 'Tower', 'Bridge'],
        ARRAY['Which', 'near', 'Yes', 'is', 'Bridge', 'at', 'please', 'exit', 'this', 'with', 'to', 'familiar', 'Tower', 'I''m', 'closest', 'not', 'area', 'knows'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/19/expressions/53/practice-examples/db2d7765-8769-44aa-8780-eed3a932da48.png","sentenceText":"Are you familiar with this neighborhood?","sentenceWords":["Are","you","familiar","with","this","neighborhood"],"highlightingPart":"familiar with","practiceQuestion":"Do you know your way around here?","sentenceTranslation":"이 동네 잘 알아?","sentenceWordChoices":["used","this","you","Are","familiarity","neighborhood","know","familiar","with"],"practiceQuestionTranslation":"여기 지리 좀 알아?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/19/expressions/53/practice-examples/536933ca-829e-41c2-bf1c-6db54b308c28.png","sentenceText":"I''m not familiar with that term.","sentenceWords":["I''m","not","familiar","with","that","term"],"highlightingPart":"not familiar with","practiceQuestion":"Do you know what this term means?","sentenceTranslation":"그 용어는 잘 몰라요.","sentenceWordChoices":["I''m","used","familiarity","know","familiar","that","with","term","not"],"practiceQuestionTranslation":"이 용어 무슨 뜻인지 알아?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/19/expressions/53/practice-examples/e4c01dae-f823-4c78-9b6a-84a85169b899.png","sentenceText":"She''s familiar with the process.","sentenceWords":["She''s","familiar","with","the","process"],"highlightingPart":"familiar with","practiceQuestion":"Who should we ask to help with this?","sentenceTranslation":"걔는 그 절차를 잘 알아.","sentenceWordChoices":["with","the","used","process","She''s","familiar","familiarity","know"],"practiceQuestionTranslation":"이거 도와줄 사람 누구한테 부탁하지?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/19/expressions/53/practice-examples/4ca7f162-98da-42e0-8075-5622b138c1d6.png","sentenceText":"Most people are familiar with his music.","sentenceWords":["Most","people","are","familiar","with","his","music"],"highlightingPart":"familiar with","practiceQuestion":"Is he a well-known singer?","sentenceTranslation":"대부분의 사람들이 그의 음악을 알고 있어.","sentenceWordChoices":["familiarity","with","used","familiar","Most","music","people","know","are","his"],"practiceQuestionTranslation":"걔 유명한 가수야?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 53
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        54,
        19,
        'SCENARIO',
        'GRAMMAR_FUNCTION_WORD',
        'BASIC',
        'EN',
        'KR',
        2,
        'for the most part',
        '대체로, 전반적으로',
        '''대체로 그렇다''고 말하는 for the most part',
        '전부는 아니지만 큰 틀에서는 그렇다고 할 때 쓰는 표현입니다. ''대부분'', ''전반적으로''라는 뜻으로, 예외가 조금 있다는 여지를 남기며 말할 수 있어요.',
        'Is this your first time in London? How are you finding it so far?',
        '런던 처음이에요? 여행 다녀보니 어때요?',
        'It''s been amazing for the most part — except for the weather!',
        '전반적으로 정말 좋았어요. 날씨만 빼면요!',
        ARRAY['It''s', 'been', 'amazing', 'for', 'the', 'most', 'part', 'except', 'for', 'the', 'weather'],
        ARRAY['the', 'except', 'mostly', 'for', 'amazing', 'besides', 'for', 'most', 'It''s', 'part', 'parts', 'weather', 'the', 'been'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/19/expressions/54/practice-examples/f834829d-dfed-47dc-8142-1ef90fbce06e.png","sentenceText":"For the most part, the reviews were positive.","sentenceWords":["For","the","most","part","the","reviews","were","positive"],"highlightingPart":"For the most part","practiceQuestion":"What did people think of the movie?","sentenceTranslation":"후기는 전반적으로 긍정적이었어.","sentenceWordChoices":["most","positive","mainly","the","For","were","generally","the","reviews","part","mostly"],"practiceQuestionTranslation":"사람들이 그 영화 어떻게 봤대?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/19/expressions/54/practice-examples/6a087d34-8d04-4022-87a4-aa0f85650013.png","sentenceText":"I agree with you for the most part.","sentenceWords":["I","agree","with","you","for","the","most","part"],"highlightingPart":"for the most part","practiceQuestion":"I think we should cut the budget for marketing.","sentenceTranslation":"난 네 말에 대체로 동의해.","sentenceWordChoices":["part","agree","most","mainly","for","you","the","generally","mostly","with","I"],"practiceQuestionTranslation":"마케팅 예산을 줄여야 할 것 같아."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/19/expressions/54/practice-examples/a6da555b-bfe2-439d-9c8f-f8c883a0f774.png","sentenceText":"The neighborhood is quiet for the most part.","sentenceWords":["The","neighborhood","is","quiet","for","the","most","part"],"highlightingPart":"for the most part","practiceQuestion":"What''s the neighborhood like?","sentenceTranslation":"그 동네는 대체로 조용해.","sentenceWordChoices":["neighborhood","is","most","for","mostly","quiet","The","generally","the","part","mainly"],"practiceQuestionTranslation":"그 동네 어때?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/19/expressions/54/practice-examples/7d570733-2f0a-4193-9aae-e01efc5b801c.png","sentenceText":"For the most part, the plan went smoothly.","sentenceWords":["For","the","most","part","the","plan","went","smoothly"],"highlightingPart":"For the most part","practiceQuestion":"How did the event go?","sentenceTranslation":"전반적으로는 계획대로 잘 진행됐어.","sentenceWordChoices":["smoothly","mainly","mostly","plan","the","For","part","most","generally","went","the"],"practiceQuestionTranslation":"행사는 어떻게 됐어?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 54
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        55,
        19,
        'SCENARIO',
        'DAILY_ROUTINE',
        'BASIC',
        'EN',
        'KR',
        3,
        'hit the spot',
        '딱이다, 딱 원하던 것이다',
        '딱 원하던 그 맛일 때 hit the spot',
        '음식이나 음료가 ''딱 이거였어'', ''속이 확 풀린다'' 싶을 때 쓰는 표현입니다. 갈증이나 허기를 정확히 해소해 줬다는 만족감을 한 방에 전달해요.',
        'I can point you to some great spots nearby — what kind of food are you into?',
        '근처 맛집 추천해 드릴게요. 어떤 음식 좋아해요?',
        'Something warm like ramen would really hit the spot right now.',
        '지금은 라멘같이 따뜻한 게 딱일 것 같아요.',
        ARRAY['Something', 'warm', 'like', 'ramen', 'would', 'really', 'hit', 'the', 'spot', 'right', 'now'],
        ARRAY['warm', 'spot', 'hits', 'spots', 'ramen', 'warmly', 'now', 'the', 'would', 'hit', 'like', 'Something', 'right', 'really'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/19/expressions/55/practice-examples/931e57a2-ae89-463a-bbca-2a63073a288e.png","sentenceText":"A hot soup would hit the spot right now.","sentenceWords":["A","hot","soup","would","hit","the","spot","right","now"],"highlightingPart":"hit the spot","practiceQuestion":"I''m freezing and hungry.","sentenceTranslation":"지금 뜨끈한 국물이 딱인데.","sentenceWordChoices":["spot","perfect","would","now","A","soup","the","spots","satisfies","hot","right","hit"],"practiceQuestionTranslation":"나 얼어 죽겠고 배고파."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/19/expressions/55/practice-examples/7ff64a09-decb-411f-a9d0-1e7420ad1af5.png","sentenceText":"That nap really hit the spot.","sentenceWords":["That","nap","really","hit","the","spot"],"highlightingPart":"hit the spot","practiceQuestion":"How do you feel after your nap?","sentenceTranslation":"그 낮잠 진짜 개운했어.","sentenceWordChoices":["nap","spot","That","satisfies","really","spots","hit","the","perfect"],"practiceQuestionTranslation":"낮잠 자고 나니 어때?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/19/expressions/55/practice-examples/de8e477d-e07e-495d-8f6d-d7bb4ae68235.png","sentenceText":"Pizza after a long day always hits the spot.","sentenceWords":["Pizza","after","a","long","day","always","hits","the","spot"],"highlightingPart":"hits the spot","practiceQuestion":"What do you want for dinner after this crazy day?","sentenceTranslation":"긴 하루 끝의 피자는 언제나 진리지.","sentenceWordChoices":["hits","always","day","the","perfect","long","spots","a","after","Pizza","satisfies","spot"],"practiceQuestionTranslation":"이 정신없는 하루 끝나고 저녁 뭐 먹고 싶어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/19/expressions/55/practice-examples/6254237a-b712-4e9e-8381-e20f25e78472.png","sentenceText":"Cold watermelon on a summer day? Hits the spot.","sentenceWords":["Cold","watermelon","on","a","summer","day","Hits","the","spot"],"highlightingPart":"Hits the spot","practiceQuestion":"What''s your favorite summer snack?","sentenceTranslation":"여름날 시원한 수박? 그게 딱이지.","sentenceWordChoices":["the","day","spot","satisfies","on","a","Hits","perfect","summer","spots","Cold","watermelon"],"practiceQuestionTranslation":"여름에 제일 좋아하는 간식이 뭐야?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 55
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        56,
        19,
        'SCENARIO',
        'CONVERSATION_SKILL',
        'BASIC',
        'EN',
        'KR',
        4,
        'It''s nice of you to ~',
        '~해주다니 친절하다',
        '칭찬에 감사를 표하는 It''s nice of you to ~',
        '''그렇게 말해주다니/해주다니 친절하다''라며 상대의 호의에 감사하는 표현입니다. of you가 들어가서 ''너라는 사람이 참 좋다''는 뉘앙스까지 전달돼요.',
        'Let me give you my number — just text me if you have any questions!',
        '제 연락처 알려드릴게요. 궁금한 거 있으면 문자해요!',
        'It''s so nice of you to offer!',
        '그렇게 챙겨주시다니 정말 감사해요!',
        ARRAY['It''s', 'so', 'nice', 'of', 'you', 'to', 'offer'],
        ARRAY['so', 'for', 'offer', 'nicely', 'you', 'offering', 'It''s', 'nice', 'of', 'to'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/19/expressions/56/practice-examples/76450c99-522f-4d2d-a0dc-5346a27859d9.png","sentenceText":"It was nice of you to come all this way.","sentenceWords":["It","was","nice","of","you","to","come","all","this","way"],"highlightingPart":"It was nice of you to come","practiceQuestion":"I drove three hours to see you.","sentenceTranslation":"이 먼 길을 와주다니 고마워.","sentenceWordChoices":["generous","of","It","was","thoughtful","you","considerate","way","all","to","come","this","nice"],"practiceQuestionTranslation":"너 보려고 세 시간 운전해서 왔어."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/19/expressions/56/practice-examples/e3366f03-c013-438e-bc35-c3424fa8cdf4.png","sentenceText":"It''s so nice of you to remember my birthday.","sentenceWords":["It''s","so","nice","of","you","to","remember","my","birthday"],"highlightingPart":"It''s so nice of you to remember","practiceQuestion":"You remembered my birthday!","sentenceTranslation":"내 생일 기억해 주다니 정말 고마워.","sentenceWordChoices":["remember","so","of","generous","considerate","you","my","birthday","It''s","to","nice","thoughtful"],"practiceQuestionTranslation":"내 생일을 기억했네!"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/19/expressions/56/practice-examples/90db590e-2867-43b0-b49b-7e35fd28ab99.png","sentenceText":"That''s very kind of you to offer.","sentenceWords":["That''s","very","kind","of","you","to","offer"],"highlightingPart":"That''s very kind of you to offer","practiceQuestion":"I can pick you up from the airport if you want.","sentenceTranslation":"제안해 주시다니 정말 친절하시네요.","sentenceWordChoices":["considerate","very","thoughtful","of","to","kind","That''s","generous","you","offer"],"practiceQuestionTranslation":"원하면 공항에서 픽업해줄 수 있어."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/19/expressions/56/practice-examples/1eecfc1b-ff62-4e52-a778-fe03d9c63af5.png","sentenceText":"It was sweet of you to call.","sentenceWords":["It","was","sweet","of","you","to","call"],"highlightingPart":"It was sweet of you to call","practiceQuestion":"I just wanted to check how you were doing.","sentenceTranslation":"전화해 주다니 다정하네.","sentenceWordChoices":["call","generous","sweet","thoughtful","you","It","to","considerate","was","of"],"practiceQuestionTranslation":"그냥 잘 지내나 궁금해서 전화했어."}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 56
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        57,
        20,
        'SCENARIO',
        'OPINION_JUDGMENT',
        'BASIC',
        'EN',
        'KR',
        1,
        'live up to',
        '(기대·명성에) 부응하다',
        '기대에 부응하는 live up to',
        '''(기대나 명성에) 부응하다''라는 뜻입니다. live up to expectations(기대에 부응하다), live up to the hype(소문값을 하다)처럼 평가의 맥락에서 자주 쓰여요.',
        'What''s the one place that just blew you away?',
        '완전 반해버린 곳 딱 하나가 어디야?',
        'Kyoto — it totally lived up to the hype.',
        '교토. 소문값 제대로 하더라.',
        ARRAY['Kyoto', 'it', 'totally', 'lived', 'up', 'to', 'the', 'hype'],
        ARRAY['up', 'hype', 'to', 'lived', 'Kyoto', 'live', 'totally', 'it', 'down', 'the', 'hyped'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/20/expressions/57/practice-examples/c4f950f8-1da2-4193-b2c5-db4ce765577d.png","sentenceText":"It''s hard to live up to my parents'' expectations.","sentenceWords":["It''s","hard","to","live","up","to","my","parents''","expectations"],"highlightingPart":"live up to","practiceQuestion":"Is it stressful being their kid?","sentenceTranslation":"부모님 기대에 부응하기가 힘들어.","sentenceWordChoices":["hard","match","parents''","up","to","expectations","It''s","my","reach","live","to","meet"],"practiceQuestionTranslation":"걔네 자식으로 사는 거 힘들어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/20/expressions/57/practice-examples/b3e4218c-b989-498a-bc20-07ca5a9825a3.png","sentenceText":"The restaurant didn''t live up to its reviews.","sentenceWords":["The","restaurant","didn''t","live","up","to","its","reviews"],"highlightingPart":"live up to","practiceQuestion":"How was that restaurant everyone''s raving about?","sentenceTranslation":"그 식당 리뷰만큼은 아니었어.","sentenceWordChoices":["its","to","didn''t","up","match","The","restaurant","reviews","meet","live","reach"],"practiceQuestionTranslation":"다들 극찬하던 그 식당 어땠어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/20/expressions/57/practice-examples/be119b0c-c526-4e23-b81c-ea77851aeaa2.png","sentenceText":"He lived up to his reputation.","sentenceWords":["He","lived","up","to","his","reputation"],"highlightingPart":"lived up to","practiceQuestion":"Was he as good as everyone said?","sentenceTranslation":"걔는 명성대로였어.","sentenceWordChoices":["reputation","up","reach","to","match","his","lived","He","meet"],"practiceQuestionTranslation":"걔 소문만큼 잘해?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/20/expressions/57/practice-examples/88aea1ad-139c-4568-9c77-61514b13cafd.png","sentenceText":"The sequel didn''t live up to the original.","sentenceWords":["The","sequel","didn''t","live","up","to","the","original"],"highlightingPart":"live up to","practiceQuestion":"Was the sequel as good as the first movie?","sentenceTranslation":"속편이 원작만 못했어.","sentenceWordChoices":["up","original","The","live","meet","the","didn''t","to","match","sequel","reach"],"practiceQuestionTranslation":"속편이 1편만큼 좋았어?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 57
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        58,
        20,
        'SCENARIO',
        'GRAMMAR_FUNCTION_WORD',
        'BASIC',
        'EN',
        'KR',
        2,
        'unlike',
        '~와는 달리',
        '대조를 나타내는 unlike',
        '''~와는 달리''라며 둘의 차이를 대조하는 전치사입니다. 문장 앞이나 뒤에 unlike + 명사를 붙이면 비교 대상과의 다름이 선명하게 드러나요.',
        'Why that one? What made it so special?',
        '왜 하필 거기야? 뭐가 그렇게 특별했는데?',
        'Unlike other big cities, it felt calm and personal.',
        '다른 대도시들과 달리 차분하고 아늑한 느낌이었어.',
        ARRAY['Unlike', 'other', 'big', 'cities', 'it', 'felt', 'calm', 'and', 'personal'],
        ARRAY['feels', 'big', 'it', 'Like', 'felt', 'cities', 'Unlike', 'other', 'and', 'calm', 'calmly', 'personal'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/20/expressions/58/practice-examples/de30a428-4084-4722-a988-b9d8ad04d8d0.png","sentenceText":"Unlike last year, this winter is mild.","sentenceWords":["Unlike","last","year","this","winter","is","mild"],"highlightingPart":"Unlike","practiceQuestion":"Is this winter cold like last year?","sentenceTranslation":"작년과 달리 이번 겨울은 포근해.","sentenceWordChoices":["this","mild","Unlike","unless","year","is","different","winter","like","last"],"practiceQuestionTranslation":"이번 겨울도 작년처럼 추워?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/20/expressions/58/practice-examples/ce770f22-57d7-4e26-861d-b991f72557e0.png","sentenceText":"He''s very outgoing, unlike his brother.","sentenceWords":["He''s","very","outgoing","unlike","his","brother"],"highlightingPart":"unlike","practiceQuestion":"What''s he like compared to his brother?","sentenceTranslation":"걔는 형이랑 다르게 아주 외향적이야.","sentenceWordChoices":["like","very","brother","He''s","unlike","different","unless","outgoing","his"],"practiceQuestionTranslation":"걔는 형이랑 비교하면 어때?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/20/expressions/58/practice-examples/13398b0b-b873-46fb-8446-13a54fc8e354.png","sentenceText":"Unlike most cafes, this place opens at 6 a.m.","sentenceWords":["Unlike","most","cafes","this","place","opens","at","6","a.m."],"highlightingPart":"Unlike","practiceQuestion":"Is this cafe open this early?","sentenceTranslation":"여느 카페와 달리 여긴 아침 6시에 열어.","sentenceWordChoices":["place","6","Unlike","different","like","most","cafes","at","this","unless","opens","a.m."],"practiceQuestionTranslation":"이 카페 이렇게 일찍 열어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/20/expressions/58/practice-examples/7414be5a-9fd0-460a-b34c-03516a82788d.png","sentenceText":"It''s so unlike you to give up.","sentenceWords":["It''s","so","unlike","you","to","give","up"],"highlightingPart":"unlike","practiceQuestion":"I think I''m just going to quit the project.","sentenceTranslation":"포기하다니 너답지 않아.","sentenceWordChoices":["so","unless","you","to","up","unlike","It''s","different","give","like"],"practiceQuestionTranslation":"나 그냥 이 프로젝트 그만둘까 봐."}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 58
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        59,
        20,
        'SCENARIO',
        'CONVERSATION_SKILL',
        'BASIC',
        'EN',
        'KR',
        3,
        'long story short',
        '결론만 말하면, 요약하면',
        '요약해서 말하는 long story short',
        '''긴 얘기 짧게 줄이면'', 즉 ''결론만 말하면''이라는 뜻입니다. 구구절절한 사연을 건너뛰고 핵심만 전할 때 문장 앞에 붙여요. to make a long story short의 줄임말이에요.',
        'What about a trip that went totally wrong?',
        '완전 망한 여행은 없었어?',
        'Long story short, we missed our flight and slept in the airport.',
        '결론만 말하면, 비행기 놓치고 공항에서 잤어.',
        ARRAY['Long', 'story', 'short', 'we', 'missed', 'our', 'flight', 'and', 'slept', 'in', 'the', 'airport'],
        ARRAY['missing', 'short', 'sleep', 'slept', 'flight', 'Long', 'the', 'we', 'story', 'airport', 'missed', 'our', 'at', 'in', 'and'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/20/expressions/59/practice-examples/749b9740-fbb9-4be5-be2c-41b160765a17.png","sentenceText":"Long story short, I got the job!","sentenceWords":["Long","story","short","I","got","the","job"],"highlightingPart":"Long story short","practiceQuestion":"Did you get the job or not?","sentenceTranslation":"요약하자면, 나 취직했어!","sentenceWordChoices":["basically","stories","the","story","short","Long","I","job","got","briefly"],"practiceQuestionTranslation":"너 취직한 거야 아니야?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/20/expressions/59/practice-examples/f2ed2ab8-a8a7-42dd-b0f6-2584b667b62a.png","sentenceText":"Long story short, they broke up.","sentenceWords":["Long","story","short","they","broke","up"],"highlightingPart":"Long story short","practiceQuestion":"What happened with Mike and Sarah?","sentenceTranslation":"긴말 줄이면, 걔네 헤어졌어.","sentenceWordChoices":["Long","basically","up","briefly","they","stories","short","story","broke"],"practiceQuestionTranslation":"마이크랑 사라 무슨 일 있었어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/20/expressions/59/practice-examples/a5c27329-8070-48ba-b239-9a24722b1332.png","sentenceText":"Long story short, the car is fine now.","sentenceWords":["Long","story","short","the","car","is","fine","now"],"highlightingPart":"Long story short","practiceQuestion":"Is your car okay after the accident?","sentenceTranslation":"결론부터 말하면 차는 이제 괜찮아.","sentenceWordChoices":["story","car","fine","Long","stories","basically","the","is","briefly","short","now"],"practiceQuestionTranslation":"사고 난 뒤에 차는 괜찮아?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/20/expressions/59/practice-examples/2a89caba-b078-4ad0-96bb-70ec77d4302c.png","sentenceText":"Anyway, long story short, we''re moving to Busan.","sentenceWords":["Anyway","long","story","short","we''re","moving","to","Busan"],"highlightingPart":"long story short","practiceQuestion":"So what''s the big news you wanted to tell me?","sentenceTranslation":"아무튼 짧게 말하면, 우리 부산으로 이사 가.","sentenceWordChoices":["briefly","moving","long","story","Anyway","to","stories","basically","we''re","Busan","short"],"practiceQuestionTranslation":"그래서 나한테 하고 싶었던 큰 소식이 뭐야?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 59
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        60,
        20,
        'SCENARIO',
        'TRAVEL_MOVEMENT',
        'BASIC',
        'EN',
        'KR',
        4,
        'get stuck in',
        '~에 갇히다, 꼼짝 못 하다',
        '교통 체증에 갇힌 get stuck in',
        '''~에 갇히다, 꼼짝 못 하다''라는 뜻으로, get stuck in traffic(차가 막혀 갇히다)이 가장 대표적인 활용입니다. 지각 변명의 단골 표현이죠.',
        'Wait, how did you even miss the flight?',
        '잠깐, 비행기를 어떻게 놓친 거야?',
        'We got stuck in traffic on the way to the airport.',
        '공항 가는 길에 차가 막혀서 꼼짝도 못 했어.',
        ARRAY['We', 'got', 'stuck', 'in', 'traffic', 'on', 'the', 'way', 'to', 'the', 'airport'],
        ARRAY['road', 'got', 'to', 'airport', 'the', 'stuck', 'We', 'in', 'get', 'on', 'way', 'at', 'the', 'traffic'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/20/expressions/60/practice-examples/d13a2cd5-9c9f-4bc7-a7df-31a8c2673e5a.png","sentenceText":"We got stuck in the elevator for 20 minutes.","sentenceWords":["We","got","stuck","in","the","elevator","for","20","minutes"],"highlightingPart":"got stuck in","practiceQuestion":"Why are you guys so late?","sentenceTranslation":"엘리베이터에 20분이나 갇혀 있었어.","sentenceWordChoices":["gets","elevator","in","for","minutes","jammed","the","stuck","got","We","trapped","20"],"practiceQuestionTranslation":"너네 왜 이렇게 늦었어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/20/expressions/60/practice-examples/227c0ed3-de3e-4418-ac6b-289bbdf0829d.png","sentenceText":"I got stuck in a long meeting.","sentenceWords":["I","got","stuck","in","a","long","meeting"],"highlightingPart":"got stuck in","practiceQuestion":"Why didn''t you answer your phone earlier?","sentenceTranslation":"긴 회의에 붙잡혀 있었어.","sentenceWordChoices":["I","a","meeting","in","trapped","got","gets","long","stuck","jammed"],"practiceQuestionTranslation":"아까 왜 전화 안 받았어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/20/expressions/60/practice-examples/732aacca-4ae3-4961-9406-e342aee2becd.png","sentenceText":"Don''t leave at 6 or you''ll get stuck in rush hour.","sentenceWords":["Don''t","leave","at","6","or","you''ll","get","stuck","in","rush","hour"],"highlightingPart":"get stuck in","practiceQuestion":"When should I leave to avoid traffic?","sentenceTranslation":"6시에 나가면 퇴근길 정체에 갇힐 거야.","sentenceWordChoices":["leave","get","you''ll","6","rush","jammed","in","at","Don''t","trapped","gets","hour","or","stuck"],"practiceQuestionTranslation":"차 안 막히려면 언제 나가야 해?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/20/expressions/60/practice-examples/fe90c592-003c-4f15-82f2-815c834f26d0.png","sentenceText":"My car got stuck in the snow.","sentenceWords":["My","car","got","stuck","in","the","snow"],"highlightingPart":"got stuck in","practiceQuestion":"Why are you so late?","sentenceTranslation":"차가 눈에 빠져서 꼼짝 못 했어.","sentenceWordChoices":["got","snow","the","My","trapped","jammed","car","in","stuck","gets"],"practiceQuestionTranslation":"너 왜 이렇게 늦었어?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 60
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
SELECT
        61,
        20,
        'SCENARIO',
        'TIME_PLANNING',
        'BASIC',
        'EN',
        'KR',
        5,
        'take ~ off',
        '(휴가를) 내다, 쉬다',
        '휴가 일정을 말하는 take ~ off',
        '''하루 쉬다'', ''휴가를 내다''라고 말할 때 take a day off처럼 표현합니다. take 뒤에 쉬는 기간을 넣으면 되는 간단하면서도 실용적인 표현이에요.',
        'If money and time didn''t matter at all — where would you go next?',
        '돈이랑 시간이 무제한이면 다음에 어디 갈 거야?',
        'I''d take a whole year off and travel the world.',
        '1년 통째로 쉬면서 세계 여행 할 거야.',
        ARRAY['I''d', 'take', 'a', 'whole', 'year', 'off', 'and', 'travel', 'the', 'world'],
        ARRAY['travels', 'I''d', 'a', 'off', 'year', 'whole', 'world', 'take', 'took', 'on', 'the', 'and', 'travel'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/20/expressions/61/practice-examples/e8ceae13-7f94-48cd-be54-f83daf6d4932.png","sentenceText":"I took the day off to rest.","sentenceWords":["I","took","the","day","off","to","rest"],"highlightingPart":"took the day off","practiceQuestion":"Why weren''t you at work yesterday?","sentenceTranslation":"쉬려고 하루 휴가 냈어.","sentenceWordChoices":["vacation","I","off","the","to","day","rest","took","leave","break"],"practiceQuestionTranslation":"어제 회사 왜 안 갔어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/20/expressions/61/practice-examples/d144c5ca-ec88-4ce5-b655-19965a2947eb.png","sentenceText":"Can I take next Monday off?","sentenceWords":["Can","I","take","next","Monday","off"],"highlightingPart":"take next Monday off","practiceQuestion":"Do you need anything before the weekend?","sentenceTranslation":"다음 주 월요일에 쉬어도 될까요?","sentenceWordChoices":["vacation","off","Can","next","Monday","leave","I","break","take"],"practiceQuestionTranslation":"주말 전에 필요한 거 있어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/20/expressions/61/practice-examples/b1f171d5-a53c-480d-8275-513337f92cd3.png","sentenceText":"She''s taking a week off in August.","sentenceWords":["She''s","taking","a","week","off","in","August"],"highlightingPart":"taking a week off","practiceQuestion":"Is Jenny around in August?","sentenceTranslation":"걔 8월에 일주일 휴가 내.","sentenceWordChoices":["off","break","vacation","a","leave","She''s","taking","in","August","week"],"practiceQuestionTranslation":"8월에 제니 있어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/20/expressions/61/practice-examples/46d6464b-50e5-4128-99e2-ce8e6683afa4.png","sentenceText":"You should take some time off.","sentenceWords":["You","should","take","some","time","off"],"highlightingPart":"take some time off","practiceQuestion":"You look exhausted lately.","sentenceTranslation":"너 좀 쉬어야 해.","sentenceWordChoices":["You","should","leave","some","time","vacation","take","off","break"],"practiceQuestionTranslation":"너 요즘 지쳐 보여."}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM writing_expression WHERE id = 61
);

-- 기존 행이 이미 있더라도 승인된 역할명과 이미지 URL은 항상 최신값으로 맞춘다.
UPDATE scenario
SET ai_role = '미국인 대학생 룸메이트, 외향적이고 친화적인 성격',
    thumbnail_url = 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/1/thumbnail/41e15ef2-3923-4749-9ac5-4e2824fed54e.png',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 1;
UPDATE scenario
SET ai_role = '룸메이트, 실용적이고 솔직하게 생활 규칙을 정하고 싶어함',
    thumbnail_url = 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/2/thumbnail/2da49db2-946a-49c9-b526-6c979063cca7.png',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 2;
UPDATE scenario
SET ai_role = '룸메이트, 주말에 같이 놀고 싶어하는 밝은 성격',
    thumbnail_url = 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/3/thumbnail/b8821003-0f19-4153-a0c7-b119a46dcbab.png',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 3;
UPDATE scenario
SET ai_role = '기숙사 프론트 데스크 직원, 매뉴얼에 따라 사무적이면서도 친절하게 응대',
    thumbnail_url = 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/4/thumbnail/b6929083-b21b-44df-a90a-ee010201f56d.png',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 4;
UPDATE scenario
SET ai_role = '룸메이트, 친구를 자주 초대하고 음악을 틀고 공부하는 편이라 경계를 미리 정하고 싶어함',
    thumbnail_url = 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/5/thumbnail/86a20939-09e8-427d-9736-1019226eae5a.png',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 5;
UPDATE scenario
SET ai_role = '기숙사 파티에서 처음 만난 다른 유학생, 밝고 사교적인 성격',
    thumbnail_url = 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/6/thumbnail/f8c0d2fe-9b01-440c-829b-96459946982b.png',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 6;
UPDATE scenario
SET ai_role = '룸메이트, 이제 꽤 친해져서 깊은 이야기도 나누고 싶어함',
    thumbnail_url = 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/7/thumbnail/97e5dd23-b895-47f0-9f4a-9ba6fb12ef68.png',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 7;
UPDATE scenario
SET ai_role = '같은 수업을 듣는 학생, 낯가림 없이 먼저 다가오는 성격',
    thumbnail_url = 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/8/thumbnail/0ee6fc4a-0c91-4c50-88a8-c6b0b9fdb8fb.png',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 8;
UPDATE scenario
SET ai_role = '조별과제 팀원, 발표를 앞두고 긴장한 성격',
    thumbnail_url = 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/9/thumbnail/ac6f40ec-3e95-421d-b63e-d59255a735dd.png',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 9;
UPDATE scenario
SET ai_role = '담당 교수, 공정하지만 격식 있고 신중한 태도',
    thumbnail_url = 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/10/thumbnail/f87f5c2c-02a1-4290-95f5-7b5497c280b1.png',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 10;
UPDATE scenario
SET ai_role = '같은 수업 친구, 편하게 시험 스트레스를 나누는 성격',
    thumbnail_url = 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/11/thumbnail/9699f877-b78f-4a2f-b350-53e494cc53ed.png',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 11;
UPDATE scenario
SET ai_role = '조별 토론 팀원, 논리적이고 적극적으로 의견을 주고받는 성격',
    thumbnail_url = 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/12/thumbnail/de8d779e-59d8-480b-9a55-11246629e752.png',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 12;
UPDATE scenario
SET ai_role = '비행기 옆자리에 앉은 여행자, 여행을 좋아하고 대화하기 편한 성격',
    thumbnail_url = 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/13/thumbnail/ed93dc13-2594-4360-b4af-284b2af9b397.png',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 13;
UPDATE scenario
SET ai_role = '항공사 카운터 직원, 매뉴얼대로 정중하게 보상 절차를 안내',
    thumbnail_url = 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/14/thumbnail/8ab5c295-b8e4-4e6f-8d98-16e0ef575362.png',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 14;
UPDATE scenario
SET ai_role = '호텔 프론트 데스크 직원, 친절하고 정중한 서비스직 태도',
    thumbnail_url = 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/15/thumbnail/f8e7a303-bc68-46e4-91e7-09dc95ee4ef6.png',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 15;
UPDATE scenario
SET ai_role = '낯선 사람, 호감을 표현하며 적극적으로 다가오는 성격',
    thumbnail_url = 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/16/thumbnail/253c75d2-f39f-486c-b8fb-0d0e59696275.png',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 16;
UPDATE scenario
SET ai_role = '카페/식당 직원, 친절하고 캐주얼한 서비스 태도',
    thumbnail_url = 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/17/thumbnail/b82d0860-8fcb-44ea-8116-c2f924302757.png',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 17;
UPDATE scenario
SET ai_role = '약사, 차분하고 전문적으로 증상을 확인하는 태도',
    thumbnail_url = 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/18/thumbnail/0681abf4-76b7-46a1-a776-9973015ace04.png',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 18;
UPDATE scenario
SET ai_role = '길에서 만난 현지인 행인, 친절하게 길을 안내해주는 성격',
    thumbnail_url = 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/19/thumbnail/26ec489b-4d45-4ebf-a7e2-8b7a944113b0.png',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 19;
UPDATE scenario
SET ai_role = '친구, 여행 이야기에 호기심 많고 수다스러운 성격',
    thumbnail_url = 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/20/thumbnail/495ab179-eefb-4126-9407-a2910add9c4b.png',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 20;
UPDATE scenario_language_variant
SET title = '입주 첫날, 룸메이트 Marco와 첫 만남',
    briefing = '기숙사 입주 첫날, 방문을 열자 이미 짐을 풀고 있던 룸메이트 Marco가 반갑게 인사를 건넨다.',
    user_opening_instruction = NULL,
    conversation_goal = 'Marco에게 자기소개를 하고, 취미와 한국 여행지를 자연스럽게 소개하기',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 1
  AND target_locale = 'EN'
  AND base_locale = 'KR';
UPDATE scenario_language_variant
SET title = '집안일과 생활 규칙 정하기',
    briefing = '입주 며칠 후, Marco가 편하게 지내기 위해 청소·생활 패턴·규칙에 대해 미리 얘기해보자고 한다.',
    user_opening_instruction = NULL,
    conversation_goal = '청소 분담, 생활 리듬, 룸메이트로서의 마지노선을 Marco와 솔직하게 조율하기',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 2
  AND target_locale = 'EN'
  AND base_locale = 'KR';
UPDATE scenario_language_variant
SET title = '카페 수다 — 주말 약속 잡기',
    briefing = '카페에서 Marco와 커피를 마시다가 자연스럽게 주말 계획 얘기가 나온다.',
    user_opening_instruction = NULL,
    conversation_goal = 'Marco와 주말 약속(요일, 할 일)을 정하고 서로의 취향 알아가기',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 3
  AND target_locale = 'EN'
  AND base_locale = 'KR';
UPDATE scenario_language_variant
SET title = '기숙사 에어컨 요금 문제 — 프론트에 전화하기',
    briefing = '7월 한 달 내내 여행으로 기숙사를 비웠는데 에어컨 요금이 100달러나 청구되었다. 기숙사 프론트에 전화해 Chloe에게 상황을 설명해야 한다.',
    user_opening_instruction = '7월 한 달 내내 여행을 다니느라 기숙사를 비웠는데 에어컨 요금이 100달러나 나왔다. 기숙사 프론트에 전화해서 Chloe에게 상황을 설명하자.',
    conversation_goal = 'Chloe에게 부당 청구 상황을 설명하고, 증빙 제출과 환불 방식을 정하기',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 4
  AND target_locale = 'EN'
  AND base_locale = 'KR';
UPDATE scenario_language_variant
SET title = '소음, 손님, 경계 정하기',
    briefing = '같이 지낸 지 좀 됐지만, Marco가 손님 초대·소음·물건 공유에 대한 경계를 확실히 정해두고 싶어한다.',
    user_opening_instruction = NULL,
    conversation_goal = '손님, 소음, 물건 공유에 대한 서로의 기준을 명확히 정하기',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 5
  AND target_locale = 'EN'
  AND base_locale = 'KR';
UPDATE scenario_language_variant
SET title = '인터내셔널 파티 — 처음 만난 Chloe',
    briefing = '기숙사에서 열린 인터내셔널 파티. 처음 보는 Chloe가 먼저 다가와 말을 건다.',
    user_opening_instruction = NULL,
    conversation_goal = '처음 만난 Chloe와 자연스럽게 스몰토크하고 다음 모임 초대에 응답하기',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 6
  AND target_locale = 'EN'
  AND base_locale = 'KR';
UPDATE scenario_language_variant
SET title = '서로 더 알아가는 밤',
    briefing = '밤에 방에서 Marco와 둘이 있는데, Marco가 좀 더 깊은 이야기를 나누고 싶어한다.',
    user_opening_instruction = NULL,
    conversation_goal = 'Marco와 가족, 꿈, 요즘 고민 등 개인적인 이야기를 진솔하게 나누기',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 7
  AND target_locale = 'EN'
  AND base_locale = 'KR';
UPDATE scenario_language_variant
SET title = '첫 수업, 옆자리 Marco',
    briefing = '첫 수업 시간, 옆자리에 앉아도 되는지 묻는 Marco과 대화가 시작된다.',
    user_opening_instruction = NULL,
    conversation_goal = 'Marco과 자연스럽게 첫 대화를 나누고 한국에서의 학교생활을 소개하기',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 8
  AND target_locale = 'EN'
  AND base_locale = 'KR';
UPDATE scenario_language_variant
SET title = '조별 발표 준비하기',
    briefing = '다음 주 조별 발표를 앞두고, 팀원 Chloe와 역할 분담과 준비 상황을 논의한다.',
    user_opening_instruction = NULL,
    conversation_goal = 'Chloe와 발표 경험을 공유하고 역할을 분담하기',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 9
  AND target_locale = 'EN'
  AND base_locale = 'KR';
UPDATE scenario_language_variant
SET title = '교수님 오피스아워 방문',
    briefing = '지난 과제 성적이 생각보다 낮게 나와, 교수님을 찾아가 정중하게 이유를 여쭙고 개선 방법을 물어봐야 한다.',
    user_opening_instruction = '지난 과제 성적이 생각보다 낮게 나왔다. 교수님을 찾아가 정중하게 이유를 여쭙고 어떻게 개선할 수 있을지 물어보자.',
    conversation_goal = '교수님께 정중하게 감점 사유를 여쭙고 개선 방법과 후속 조치를 확인하기',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 10
  AND target_locale = 'EN'
  AND base_locale = 'KR';
UPDATE scenario_language_variant
SET title = '시험 공부 수다',
    briefing = '기말고사를 앞두고 친구 Chloe와 공부 습관에 대해 수다를 떤다.',
    user_opening_instruction = NULL,
    conversation_goal = 'Chloe와 공부 습관과 경험을 나누고 같이 공부할 시간을 정하기',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 11
  AND target_locale = 'EN'
  AND base_locale = 'KR';
UPDATE scenario_language_variant
SET title = '토론 수업 — 돈과 행복',
    briefing = '오늘 토론 수업 주제는 "돈으로 행복을 살 수 있는가". 조원 Marco과 의견을 나눈다.',
    user_opening_instruction = NULL,
    conversation_goal = '자신의 입장을 근거와 함께 논리적으로 표현하고 Marco과 토론하기',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 12
  AND target_locale = 'EN'
  AND base_locale = 'KR';
UPDATE scenario_language_variant
SET title = '비행기 옆자리 승객과의 대화',
    briefing = '장거리 비행 중, 옆자리에 앉은 Marco이 먼저 말을 건다.',
    user_opening_instruction = NULL,
    conversation_goal = 'Marco과 여행 목적과 취향에 대해 자연스럽게 대화 나누기',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 13
  AND target_locale = 'EN'
  AND base_locale = 'KR';
UPDATE scenario_language_variant
SET title = '수하물 파손 — 카운터에 항의하기',
    briefing = '캐리어가 파손된 채로 도착했다. 카운터 직원 Marco에게 상황을 설명하고 보상책을 물어봐야 한다.',
    user_opening_instruction = '당신은 캐리어가 부서졌다. 카운터 직원 Marco에게 상황을 설명하고 보상책을 물어봐야한다.',
    conversation_goal = 'Marco에게 수하물 파손 상황을 설명하고 보상 방식을 정하기',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 14
  AND target_locale = 'EN'
  AND base_locale = 'KR';
UPDATE scenario_language_variant
SET title = '호텔 체크인 하기',
    briefing = '호텔에 도착해 체크인을 하며 프론트 직원 Chloe와 대화한다.',
    user_opening_instruction = NULL,
    conversation_goal = 'Chloe에게 예약 정보를 전달하고 객실 선호사항을 요청하기',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 15
  AND target_locale = 'EN'
  AND base_locale = 'KR';
UPDATE scenario_language_variant
SET title = '마음에 안 드는 사람 정중히 거절하기',
    briefing = '길에서 만난 Marco가 호감을 표현하며 데이트를 제안한다. 정중하게 거절해야 하는 상황.',
    user_opening_instruction = NULL,
    conversation_goal = 'Marco의 제안을 무례하지 않으면서도 명확하게 거절하기',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 16
  AND target_locale = 'EN'
  AND base_locale = 'KR';
UPDATE scenario_language_variant
SET title = '카페에서 주문하기',
    briefing = '카페에 들어가 직원 Marco에게 주문을 한다.',
    user_opening_instruction = NULL,
    conversation_goal = 'Marco에게 원하는 메뉴를 주문하고 매장/포장 여부를 전달하기',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 17
  AND target_locale = 'EN'
  AND base_locale = 'KR';
UPDATE scenario_language_variant
SET title = '약국에서 증상 설명하고 약 사기',
    briefing = '두통이 심해 약국에 방문했다. 약사 Chloe에게 증상을 설명하고 약을 사야 한다.',
    user_opening_instruction = NULL,
    conversation_goal = 'Chloe에게 증상을 정확히 설명하고 필요한 정보를 전달하기',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 18
  AND target_locale = 'EN'
  AND base_locale = 'KR';
UPDATE scenario_language_variant
SET title = '길 잃고 현지인에게 길 묻기',
    briefing = '백화점에서 길을 잃었다. 지나가는 행인 Chloe에게 Tower Bridge로 가는 방향을 물어봐야 한다.',
    user_opening_instruction = '당신은 백화점에서 길을 잃었다. 지나가는 행인 Chloe에게 어디로 나가야 Tower bridge가 가까운지 물어봐야한다.',
    conversation_goal = 'Chloe에게 길을 묻고 자연스럽게 대화를 이어가기',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 19
  AND target_locale = 'EN'
  AND base_locale = 'KR';
UPDATE scenario_language_variant
SET title = '친구와 여행 수다 떨기',
    briefing = '친구 Chloe와 카페에서 그동안의 여행 이야기를 나눈다.',
    user_opening_instruction = NULL,
    conversation_goal = 'Chloe와 여행 경험과 앞으로의 버킷리스트를 자유롭게 이야기하기',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 20
  AND target_locale = 'EN'
  AND base_locale = 'KR';
UPDATE scenario_question_language_variant
SET question_text = 'Hey, you''re my roommate, right?! I''m Marco, nice to meet you! What''s your name? Tell me a little about yourself!',
    question_translation = '안녕 너 내 룸메이트지?! 난 Marco야. 만나서 반가워. 넌 이름이 뭐야? 너에 대해 소개해주라.',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 1;
UPDATE scenario_question_language_variant
SET question_text = 'Hey, I''m Marco. Is this seat taken? Do you mind if I sit here?',
    question_translation = '안녕. 나 Marco이라고 해. 여기 자리 있어? 나 여기 앉아도 돼?',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 22;
