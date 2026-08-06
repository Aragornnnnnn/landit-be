-- LAN-207 쇼핑 카테고리와 신규 영어 회화 시나리오 20개를 추가한다.

INSERT INTO category (
    id, display_order, status, created_at, updated_at
)
SELECT
        4,
        4,
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM category WHERE id = 4
);
INSERT INTO category_language_variant (
    id, category_id, base_locale, name, created_at, updated_at
)
SELECT
        4,
        4,
        'KR',
        '쇼핑',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM category_language_variant WHERE id = 4
);
INSERT INTO scenario (
    id, category_id, ai_role, difficulty, first_speaker, thumbnail_url,
    display_order, status, total_question_count, created_at, updated_at
)
VALUES (
    21,
    1,
    '차분하고 친절한 기숙사 생활지원실 직원',
    'EASY',
    'USER',
    'https://d19azau1un4t7r.cloudfront.net/content/scenarios/21/thumbnail/1746225e-5b68-4ce6-a26d-5f65e08d2abd.png',
    21,
    'ACTIVE',
    4,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_language_variant (
    id, scenario_id, target_locale, base_locale, title, briefing,
    user_opening_instruction, conversation_goal, status, tts_voice_id,
    created_at, updated_at
)
VALUES (
    21,
    21,
    'EN',
    'KR',
    '방 키를 잃어버려 임시 출입 요청하기',
    '외출 후 기숙사로 돌아왔지만 방 키가 보이지 않는다. 생활지원실에서 임시 출입 방법과 재발급 절차를 문의해야 한다.',
    '생활지원실 직원에게 방 키를 잃어버렸다고 설명하고, 방에 들어갈 수 있는 방법을 먼저 물어보세요.',
    '당장 방에 들어갈 방법과 키 재발급 시점, 분실 키 안내 수단을 선택하기',
    'ACTIVE',
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    61, 21, 1, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    61,
    61,
    'EN',
    'KR',
    'We can issue a temporary key or escort you to your room. Tell me which option would help you right now.',
    '임시 키를 발급해 드리거나 직원이 방까지 동행할 수 있어요. 지금 어떤 도움이 필요한지 말씀해 주세요.',
    'ACTIVE',
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    62, 21, 2, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    62,
    62,
    'EN',
    'KR',
    'A replacement can be ready tomorrow morning or the following afternoon. Tell me which pickup time works better.',
    '새 키는 내일 오전이나 모레 오후에 받을 수 있어요. 언제 찾으러 오시겠어요?',
    'ACTIVE',
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    63, 21, 3, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    63,
    63,
    'EN',
    'KR',
    'We can update you by text or email. Just let us know where to send the message.',
    '진행 상황은 문자나 이메일로 알려 드릴 수 있어요. 어느 쪽으로 받을지 알려 주세요.',
    'ACTIVE',
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario (
    id, category_id, ai_role, difficulty, first_speaker, thumbnail_url,
    display_order, status, total_question_count, created_at, updated_at
)
VALUES (
    22,
    1,
    '같은 공용 주방을 사용하는 기숙사 이웃',
    'NORMAL',
    'AI',
    'https://d19azau1un4t7r.cloudfront.net/content/scenarios/22/thumbnail/bca33207-5aa3-42a0-89e3-79a848a21cb5.png',
    22,
    'ACTIVE',
    3,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_language_variant (
    id, scenario_id, target_locale, base_locale, title, briefing,
    user_opening_instruction, conversation_goal, status, tts_voice_id,
    created_at, updated_at
)
VALUES (
    22,
    22,
    'EN',
    'KR',
    '공용 냉장고 공간 정리하기',
    '이름이 적히지 않은 음식이 공용 냉장고를 가득 채워 새 식재료를 넣기 어렵다. 이웃과 불편하지 않게 정리 기준을 합의해야 한다.',
    NULL,
    '냉장고 라벨 방식과 이름 없는 음식 처리 기준, 정기 점검 일정을 합의하기',
    'ACTIVE',
    2,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    64, 22, 1, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    64,
    64,
    'EN',
    'KR',
    'I suggest names for personal food and green stickers for shared food. How does that system sound to you?',
    '개인 음식에는 이름을 적고, 같이 먹어도 되는 음식에는 초록색 스티커를 붙이면 어떨 것 같아?',
    'ACTIVE',
    '냉장고 공간이 부족하니 서로 지키기 쉬운 기준부터 정해 보자.',
    'GOOD',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    65, 22, 2, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    65,
    65,
    'EN',
    'KR',
    'For unlabeled food, let''s post a photo and wait 24 hours before removing it. Would that be enough time?',
    '이름 없는 음식은 사진을 올리고 24시간 뒤에 치우자. 이 정도면 기다리는 시간이 충분할까?',
    'ACTIVE',
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    66, 22, 3, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    66,
    66,
    'EN',
    'KR',
    'We can do the fridge check on Friday or Sunday. Does Friday or Sunday work better for you?',
    '냉장고 정리는 금요일이나 일요일에 할 수 있어. 어느 날이 더 좋을까?',
    'ACTIVE',
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario (
    id, category_id, ai_role, difficulty, first_speaker, thumbnail_url,
    display_order, status, total_question_count, created_at, updated_at
)
VALUES (
    23,
    1,
    '세탁실에서 옷 한 벌을 찾고 있는 친근한 기숙사 이웃',
    'EASY',
    'AI',
    'https://d19azau1un4t7r.cloudfront.net/content/scenarios/23/thumbnail/7a3bf1a7-2f5a-4da0-8baa-ff56f68a1e18.png',
    23,
    'ACTIVE',
    3,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_language_variant (
    id, scenario_id, target_locale, base_locale, title, briefing,
    user_opening_instruction, conversation_goal, status, tts_voice_id,
    created_at, updated_at
)
VALUES (
    23,
    23,
    'EN',
    'KR',
    '세탁물이 뒤섞인 문제 해결하기',
    '공용 건조기에서 빨래를 꺼내 방으로 가려는데, 이웃이 자신의 셔츠가 섞인 것 같다며 다가온다.',
    NULL,
    '섞인 세탁물을 확인하고 옷을 돌려줄 방법과 재발 방지 방법을 정하기',
    'ACTIVE',
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    67, 23, 1, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    67,
    67,
    'EN',
    'KR',
    'I have your white T-shirt, and my blue shirt is in your pile. Could you pass me mine while I return yours?',
    '내가 네 흰색 티셔츠를 가지고 있고, 내 파란 셔츠는 네 빨래에 있어. 서로 지금 바꿀까?',
    'ACTIVE',
    '건조기에서 셔츠가 안 보여서 당황스럽지만, 먼저 차분하게 확인해 봐야겠다.',
    'GOOD',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    68, 23, 2, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    68,
    68,
    'EN',
    'KR',
    'The rest of the clothes are still damp. We can use separate dryers or finish one load first. What should we do?',
    '나머지 옷은 아직 덜 말랐어. 건조기를 따로 쓰거나 한 사람 빨래부터 마저 돌릴 수 있는데, 어떻게 할까?',
    'ACTIVE',
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    69, 23, 3, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    69,
    69,
    'EN',
    'KR',
    'If we find another mixed item later, we can text each other or leave it by the door. Which is easier?',
    '나중에 또 섞인 옷을 발견하면 서로 문자하거나 방문 앞에 둘 수 있어. 어떤 방법이 더 편할까?',
    'ACTIVE',
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario (
    id, category_id, ai_role, difficulty, first_speaker, thumbnail_url,
    display_order, status, total_question_count, created_at, updated_at
)
VALUES (
    24,
    1,
    '기숙사 시설 관리 접수 직원',
    'NORMAL',
    'USER',
    'https://d19azau1un4t7r.cloudfront.net/content/scenarios/24/thumbnail/07141ace-c288-4f4b-9b86-017515ca2b15.png',
    24,
    'ACTIVE',
    4,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_language_variant (
    id, scenario_id, target_locale, base_locale, title, briefing,
    user_opening_instruction, conversation_goal, status, tts_voice_id,
    created_at, updated_at
)
VALUES (
    24,
    24,
    'EN',
    'KR',
    '세면대 누수 수리 요청하기',
    '방 세면대 아래에서 물이 새기 시작했다. 피해가 커지기 전에 문제를 설명하고 수리 방문 시간을 정해야 한다.',
    '시설 관리실에 연락해 방 세면대에서 물이 새고 있다고 설명하고 수리를 요청하세요.',
    '즉시 취할 조치와 관리 직원의 방문 시간, 방문 전 연락 방법을 선택하기',
    'ACTIVE',
    2,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    70, 24, 1, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    70,
    70,
    'EN',
    'KR',
    'Please stop using the sink. We can bring towels, a bucket, or help shut off the water. Tell me what you need while waiting.',
    '세면대 사용을 멈춰 주세요. 수건이나 양동이를 가져다드리거나 물을 잠그는 걸 도와드릴 수 있어요. 기다리는 동안 필요한 도움을 말씀해 주세요.',
    'ACTIVE',
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    71, 24, 2, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    71,
    71,
    'EN',
    'KR',
    'The available appointments are six this evening and nine tomorrow morning. Tell me the time you want me to reserve.',
    '방문 가능한 시간은 오늘 저녁 6시와 내일 오전 9시예요. 언제로 예약해 드릴까요?',
    'ACTIVE',
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    72, 24, 3, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    72,
    72,
    'EN',
    'KR',
    'The technician can call or knock. Tell me how we should contact you and what to do if you do not answer.',
    '관리 직원은 전화하거나 문을 두드릴 수 있어요. 어떤 방식으로 연락드리고, 응답이 없으면 어떻게 할지 말씀해 주세요.',
    'ACTIVE',
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario (
    id, category_id, ai_role, difficulty, first_speaker, thumbnail_url,
    display_order, status, total_question_count, created_at, updated_at
)
VALUES (
    25,
    1,
    '기숙사 퇴실 점검을 담당하는 하우징 직원',
    'HARD',
    'AI',
    'https://d19azau1un4t7r.cloudfront.net/content/scenarios/25/thumbnail/49fc7c99-b709-4246-a3e7-3ba7e666a8bd.png',
    25,
    'ACTIVE',
    3,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_language_variant (
    id, scenario_id, target_locale, base_locale, title, briefing,
    user_opening_instruction, conversation_goal, status, tts_voice_id,
    created_at, updated_at
)
VALUES (
    25,
    25,
    'EN',
    'KR',
    '퇴실 점검과 보증금 확인하기',
    '학기가 끝나 기숙사를 떠나기 전이다. 퇴실 점검 항목을 확인하고 보증금에서 비용이 빠질 수 있는 부분을 미리 정리해야 한다.',
    NULL,
    '퇴실 전에 준비할 방법과 점검 방문 시간, 결과 확인 방법을 정하기',
    'ACTIVE',
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    73, 25, 1, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    73,
    73,
    'EN',
    'KR',
    'Before inspection, remove your belongings and clean the room. Would you like a checklist or a quick walk-through with us?',
    '점검 전에는 개인 물건을 치우고 방을 청소해 주세요. 체크리스트를 받으시겠어요, 아니면 직원과 함께 방을 둘러보시겠어요?',
    'ACTIVE',
    '퇴실 전에 필요한 준비와 결과 확인 방법을 구체적으로 안내해야겠다.',
    'GOOD',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    74, 25, 2, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    74,
    74,
    'EN',
    'KR',
    'Appointments are available today at three or tomorrow at ten. Tell me which time to reserve.',
    '점검은 오늘 3시나 내일 10시에 받을 수 있어요. 언제로 예약해 드릴까요?',
    'ACTIVE',
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    75, 25, 3, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    75,
    75,
    'EN',
    'KR',
    'After inspection, we can email a photo report or review any charges with you in person. How would you like the results?',
    '점검 후에는 사진이 포함된 보고서를 이메일로 보내 드리거나 현장에서 비용 항목을 함께 확인할 수 있어요. 결과를 어떻게 확인하시겠어요?',
    'ACTIVE',
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario (
    id, category_id, ai_role, difficulty, first_speaker, thumbnail_url,
    display_order, status, total_question_count, created_at, updated_at
)
VALUES (
    26,
    2,
    '여행자의 일정을 함께 확인해 주는 기차역 매표소 직원',
    'EASY',
    'USER',
    'https://d19azau1un4t7r.cloudfront.net/content/scenarios/26/thumbnail/6cbf6c93-ddac-486e-9eff-99d0f1524c30.png',
    26,
    'ACTIVE',
    4,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_language_variant (
    id, scenario_id, target_locale, base_locale, title, briefing,
    user_opening_instruction, conversation_goal, status, tts_voice_id,
    created_at, updated_at
)
VALUES (
    26,
    26,
    'EN',
    'KR',
    '일정에 맞는 기차표 구매하기',
    '당일 다른 도시로 이동하기 위해 직행 열차와 환승 열차를 비교하려 한다. 매표소 직원이 두 열차의 시간과 가격, 좌석 선택지를 안내한다.',
    '매표소 직원에게 당일 이동할 기차표를 찾고 있다고 말하고, 직행 열차와 환승 열차를 비교해 달라고 요청하세요.',
    '두 열차의 도착 시간과 가격을 비교하고 구매할 표, 수령 방법, 좌석을 선택하기',
    'ACTIVE',
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    76, 26, 1, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    76,
    76,
    'EN',
    'KR',
    'The direct train arrives at four for $45; the transfer arrives at five for $30. Which ticket works better, and what made you choose it?',
    '직행 열차는 4시에 도착하며 요금은 45달러이고, 환승 열차는 5시에 도착하며 30달러예요. 어떤 표가 더 잘 맞나요? 시간과 가격 중 어떤 점을 보고 고르셨는지도 말씀해 주세요.',
    'ACTIVE',
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    77, 26, 2, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    77,
    77,
    'EN',
    'KR',
    'I can print the ticket or send it to your phone. How would you like to receive it?',
    '표를 출력해 드리거나 휴대폰으로 보내 드릴 수 있어요. 어떻게 받으시겠어요?',
    'ACTIVE',
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    78, 26, 3, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    78,
    78,
    'EN',
    'KR',
    'Window and aisle seats are both available. What seating requests do you have?',
    '창가와 통로 좌석이 모두 남아 있어요. 원하시는 좌석 조건을 말씀해 주세요.',
    'ACTIVE',
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario (
    id, category_id, ai_role, difficulty, first_speaker, thumbnail_url,
    display_order, status, total_question_count, created_at, updated_at
)
VALUES (
    27,
    2,
    '메뉴를 잘 설명해 주는 현지 식당 서버',
    'NORMAL',
    'AI',
    'https://d19azau1un4t7r.cloudfront.net/content/scenarios/27/thumbnail/9eb15645-6338-49a8-bbf4-7e9407cd3a74.png',
    27,
    'ACTIVE',
    3,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_language_variant (
    id, scenario_id, target_locale, base_locale, title, briefing,
    user_opening_instruction, conversation_goal, status, tts_voice_id,
    created_at, updated_at
)
VALUES (
    27,
    27,
    'EN',
    'KR',
    '식당에서 메뉴 재료를 비교하고 주문하기',
    '여행지 식당에서 채소 파스타와 코코넛 커리 중 하나를 고르려 한다. 서버가 두 메뉴의 재료와 맛, 곁들임 선택지를 설명한다.',
    NULL,
    '두 메뉴의 재료와 맛을 비교하고 원하는 메뉴와 곁들임을 선택하기',
    'ACTIVE',
    2,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    79, 27, 1, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    79,
    79,
    'EN',
    'KR',
    'The pasta is mild and creamy; the curry is spicy and rich. Describe the flavor you''re in the mood for.',
    '파스타는 부드럽고 고소하고 커리는 맵고 맛이 진해요. 지금은 어떤 맛이 당기세요?',
    'ACTIVE',
    '손님이 쉽게 고를 수 있도록 두 메뉴의 맛 차이를 먼저 알려 줘야겠다.',
    'GOOD',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    80, 27, 2, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    80,
    80,
    'EN',
    'KR',
    'We can remove cheese from the pasta or fish sauce from the curry. Tell me your menu choice and any adjustment.',
    '파스타에서는 치즈를, 커리에서는 피시소스를 뺄 수 있어요. 원하는 메뉴와 옵션을 선택해 주세요.',
    'ACTIVE',
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    81, 27, 3, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    81,
    81,
    'EN',
    'KR',
    'Your meal includes a free side of salad or rice. Which would you like?',
    '사이드로는 샐러드 또는 밥 중 하나를 무료로 드리고 있어요. 어떤 걸로 드릴까요?',
    'ACTIVE',
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario (
    id, category_id, ai_role, difficulty, first_speaker, thumbnail_url,
    display_order, status, total_question_count, created_at, updated_at
)
VALUES (
    28,
    2,
    '박물관과 도시 관광권을 판매하는 관광 안내소 직원',
    'EASY',
    'AI',
    'https://d19azau1un4t7r.cloudfront.net/content/scenarios/28/thumbnail/b5c9d73b-63ec-4550-bd8d-876a16f08858.png',
    28,
    'ACTIVE',
    3,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_language_variant (
    id, scenario_id, target_locale, base_locale, title, briefing,
    user_opening_instruction, conversation_goal, status, tts_voice_id,
    created_at, updated_at
)
VALUES (
    28,
    28,
    'EN',
    'KR',
    '여행 일정에 맞는 박물관 패스 고르기',
    '하루 동안 사용할 관광권을 사려 하지만 박물관 패스와 도시 패스 중 무엇이 나은지 모르겠다. 관광 안내소 직원이 포함 항목과 점심 추가 옵션, 가격을 안내한다.',
    NULL,
    '두 패스의 포함 항목을 비교하고 구매할 패스와 점심 추가 여부, 수령 방법을 선택하기',
    'ACTIVE',
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    82, 28, 1, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    82,
    82,
    'EN',
    'KR',
    'The museum pass covers two museums, while the city pass also includes transit. What can I get you for today?',
    '박물관 패스는 박물관 두 곳을 이용할 수 있고 도시 패스에는 대중교통도 포함돼요. 어떤 걸로 드릴까요?',
    'ACTIVE',
    '오늘 쓸 관광권을 고르기 쉽게 포함 항목을 먼저 설명해야겠다.',
    'GOOD',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    83, 28, 2, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    83,
    83,
    'EN',
    'KR',
    'Both passes cost $30, and lunch is an extra $15. Would you like to add lunch?',
    '두 패스 모두 30달러이고 점심 식사는 15달러에 추가할 수 있어요. 점심도 추가하시겠어요?',
    'ACTIVE',
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    84, 28, 3, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    84,
    84,
    'EN',
    'KR',
    'Printed and digital passes are both ready. Choose the format you want to carry.',
    '종이 패스와 디지털 패스 중에서 고를 수 있어요. 어떤 형태가 편하세요?',
    'ACTIVE',
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario (
    id, category_id, ai_role, difficulty, first_speaker, thumbnail_url,
    display_order, status, total_question_count, created_at, updated_at
)
VALUES (
    29,
    2,
    '야외 투어 일정 변경을 담당하는 여행사 직원',
    'HARD',
    'USER',
    'https://d19azau1un4t7r.cloudfront.net/content/scenarios/29/thumbnail/848801bd-d9ae-46fc-8b49-7e5b54c873cb.png',
    29,
    'ACTIVE',
    4,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_language_variant (
    id, scenario_id, target_locale, base_locale, title, briefing,
    user_opening_instruction, conversation_goal, status, tts_voice_id,
    created_at, updated_at
)
VALUES (
    29,
    29,
    'EN',
    'KR',
    '날씨 때문에 야외 투어 일정 변경하기',
    '예약한 야외 투어 시간에 폭우가 예보됐다. 여행사 직원이 대체 시간의 특징과 예약 확정 방법을 안내한다.',
    '여행사 직원에게 날씨 때문에 예약한 야외 투어 참여가 어렵다고 설명하고, 일정 변경이 가능한지 물어보세요.',
    '대체 투어의 특징을 비교하고 변경할 시간과 예약 확정 시점, 안내 수단을 정하기',
    'ACTIVE',
    2,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    85, 29, 1, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    85,
    85,
    'EN',
    'KR',
    'The morning tour has a smaller group, while the evening tour includes city lights. Would you like the morning or evening replacement?',
    '오전 투어는 인원이 적고 저녁 투어에서는 야경을 볼 수 있어요. 어느 시간으로 변경해 드릴까요?',
    'ACTIVE',
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    86, 29, 2, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    86,
    86,
    'EN',
    'KR',
    'I can hold the new time for one hour or confirm it now. Tell me what you''d like me to do.',
    '변경할 시간은 한 시간 동안 보류하거나 지금 바로 확정할 수 있어요. 어떻게 처리해 드릴까요?',
    'ACTIVE',
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    87, 29, 3, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    87,
    87,
    'EN',
    'KR',
    'I can send the confirmation by text or email. Tell me where to send it.',
    '처리 결과는 문자나 이메일로 보내 드릴 수 있어요. 어디로 보내 드릴까요?',
    'ACTIVE',
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario (
    id, category_id, ai_role, difficulty, first_speaker, thumbnail_url,
    display_order, status, total_question_count, created_at, updated_at
)
VALUES (
    30,
    2,
    '가능한 좌석을 차분하게 안내하는 공항 탑승구 직원',
    'NORMAL',
    'AI',
    'https://d19azau1un4t7r.cloudfront.net/content/scenarios/30/thumbnail/13b2b272-8013-4b49-a3ab-368819ecdb92.png',
    30,
    'ACTIVE',
    3,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_language_variant (
    id, scenario_id, target_locale, base_locale, title, briefing,
    user_opening_instruction, conversation_goal, status, tts_voice_id,
    created_at, updated_at
)
VALUES (
    30,
    30,
    'EN',
    'KR',
    '공항에서 더 편한 좌석으로 변경하기',
    '탑승을 기다리던 중 현재 좌석이 가운데 좌석인 것을 확인했다. 탑승구 직원이 이용 가능한 좌석 변경 선택지를 안내한다.',
    NULL,
    '좌석의 위치와 변경 시점을 비교하고 원하는 좌석과 새 탑승권 수령 방법을 선택하기',
    'ACTIVE',
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    88, 30, 1, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    88,
    88,
    'EN',
    'KR',
    'You''re currently in a middle seat. Would an aisle seat or a window seat be more comfortable for you?',
    '현재 가운데 좌석이에요. 통로석과 창가석 중 어느 쪽이 더 편하세요?',
    'ACTIVE',
    '승객이 원하는 좌석 유형을 먼저 확인한 뒤 가능한 좌석을 안내해야겠다.',
    'GOOD',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    89, 30, 2, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    89,
    89,
    'EN',
    'KR',
    'A back aisle is available now, while a front window may open during boarding. Would you like to switch now or wait?',
    '뒤쪽 통로석은 지금 바꿀 수 있고 앞쪽 창가석은 탑승할 때 자리가 날 수도 있어요. 지금 바꾸시겠어요, 기다리시겠어요?',
    'ACTIVE',
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    90, 30, 3, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    90,
    90,
    'EN',
    'KR',
    'I can print your new boarding pass or send it to your phone. Tell me how you''d like to receive it.',
    '새 탑승권을 출력해 드리거나 휴대폰으로 보내 드릴 수 있어요. 어떻게 받으실지 말씀해 주세요.',
    'ACTIVE',
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario (
    id, category_id, ai_role, difficulty, first_speaker, thumbnail_url,
    display_order, status, total_question_count, created_at, updated_at
)
VALUES (
    31,
    3,
    '학생의 목표와 현실적인 학업 부담을 함께 고려하는 지도교수',
    'HARD',
    'AI',
    'https://d19azau1un4t7r.cloudfront.net/content/scenarios/31/thumbnail/ab6610f2-49a7-46b1-869f-6374f375daea.png',
    31,
    'ACTIVE',
    3,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_language_variant (
    id, scenario_id, target_locale, base_locale, title, briefing,
    user_opening_instruction, conversation_goal, status, tts_voice_id,
    created_at, updated_at
)
VALUES (
    31,
    31,
    'EN',
    'KR',
    '수강 과목을 지도교수와 상담하기',
    '다음 학기 수강 신청을 앞두고 평가 방식이 다른 두 과목 중 하나를 선택해야 한다. 지도교수가 평가 방식과 수업 시간의 차이를 안내한다.',
    NULL,
    '두 과목의 평가 방식을 비교하고 수강할 과목을 고른 뒤 선택 이유를 설명하고 수업 시간을 정하기',
    'ACTIVE',
    2,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    91, 31, 1, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    91,
    91,
    'EN',
    'KR',
    'One course has weekly projects, and the other has two major exams. Tell me the course that feels more comfortable.',
    '한 과목은 매주 프로젝트가 있고 다른 과목은 큰 시험이 두 번 있어요. 더 편한 과목을 말해 주세요.',
    'ACTIVE',
    '학생이 평가 방식과 시간을 한꺼번에 비교해 선택할 수 있게 안내해야겠다.',
    'GOOD',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    92, 31, 2, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    92,
    92,
    'EN',
    'KR',
    'What makes that course a better fit for you?',
    '그 과목이 본인에게 더 잘 맞을 것 같은 이유가 있나요?',
    'ACTIVE',
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    93, 31, 3, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    93,
    93,
    'EN',
    'KR',
    'The course has Tuesday morning and Thursday afternoon sections. Which time works better for you?',
    '수업은 화요일 오전과 목요일 오후 분반이 있어요. 언제가 더 편하세요?',
    'ACTIVE',
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario (
    id, category_id, ai_role, difficulty, first_speaker, thumbnail_url,
    display_order, status, total_question_count, created_at, updated_at
)
VALUES (
    32,
    3,
    '같은 수업을 듣는 친절한 동급생',
    'EASY',
    'AI',
    'https://d19azau1un4t7r.cloudfront.net/content/scenarios/32/thumbnail/b3e67287-43ec-4eaa-a971-ca4e492d3d74.png',
    32,
    'ACTIVE',
    3,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_language_variant (
    id, scenario_id, target_locale, base_locale, title, briefing,
    user_opening_instruction, conversation_goal, status, tts_voice_id,
    created_at, updated_at
)
VALUES (
    32,
    32,
    'EN',
    'KR',
    '결석한 수업 노트 부탁하기',
    '몸이 좋지 않아 수업에 빠졌고 중요한 설명을 놓쳤다. 동급생에게 노트를 부탁하고 함께 확인할 시간을 정해야 한다.',
    NULL,
    '먼저 확인할 수업 내용과 노트를 전달받을 방법, 함께 복습할 방식을 선택하기',
    'ACTIVE',
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    94, 32, 1, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    94,
    94,
    'EN',
    'KR',
    'I heard you missed class today. I have notes on the worked example and the assignment update. What do you need first?',
    '오늘 수업에 못 왔다고 들었어. 풀이 예시와 과제 변경 사항을 정리해 뒀는데, 뭐부터 필요해?',
    'ACTIVE',
    '수업에 빠진 친구가 필요한 내용을 바로 받을 수 있게 먼저 물어봐야겠다.',
    'GOOD',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    95, 32, 2, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    95,
    95,
    'EN',
    'KR',
    'I can send photos now or lend you my notebook later. Say what would help you most.',
    '지금 사진을 보내 줄 수도 있고 나중에 내 노트를 빌려줄 수도 있어. 어떤 게 더 도움이 될까?',
    'ACTIVE',
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    96, 32, 3, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    96,
    96,
    'EN',
    'KR',
    'We can review for ten minutes after class or meet online tonight. Which would you prefer?',
    '수업이 끝난 뒤 10분 정도 같이 복습하거나 저녁에 온라인으로 복습할 수 있어. 어떤 걸 선호해?',
    'ACTIVE',
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario (
    id, category_id, ai_role, difficulty, first_speaker, thumbnail_url,
    display_order, status, total_question_count, created_at, updated_at
)
VALUES (
    33,
    3,
    '과제 선택지를 쉽게 비교해 주는 수업 조교',
    'NORMAL',
    'AI',
    'https://d19azau1un4t7r.cloudfront.net/content/scenarios/33/thumbnail/a7b42eeb-b60b-4287-9eab-92a07f2a2f95.png',
    33,
    'ACTIVE',
    3,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_language_variant (
    id, scenario_id, target_locale, base_locale, title, briefing,
    user_opening_instruction, conversation_goal, status, tts_voice_id,
    created_at, updated_at
)
VALUES (
    33,
    33,
    'EN',
    'KR',
    '과제 제출 방식 선택하고 준비하기',
    '과제를 보고서나 발표 중 하나로 제출해야 한다. 조교가 먼저 다가와 제출 형식과 피드백 방법을 안내한다.',
    NULL,
    '과제 형식을 고르고 선택 이유를 설명한 뒤 초안 피드백 방법을 정하기',
    'ACTIVE',
    2,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    97, 33, 1, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    97,
    97,
    'EN',
    'KR',
    'You can write a three-page report or give a five-minute presentation. Which would you choose?',
    '과제는 3쪽짜리 보고서를 작성하거나 5분 발표를 하는 것 중에서 선택할 수 있어요. 어떤 걸로 하시겠어요?',
    'ACTIVE',
    '학생이 막연하게 고민하지 않도록 두 제출 형식과 분량을 먼저 알려 줘야겠다.',
    'GOOD',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    98, 33, 2, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    98,
    98,
    'EN',
    'KR',
    'What made you choose that format?',
    '그 형식을 고른 이유가 무엇인가요?',
    'ACTIVE',
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    99, 33, 3, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    99,
    99,
    'EN',
    'KR',
    'You can send me a draft for feedback or submit the final version directly. Which would you like to do?',
    '초안을 보내면 피드백을 받을 수 있지만 최종본을 바로 제출해도 돼요. 어떤 방법으로 하시겠어요?',
    'ACTIVE',
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario (
    id, category_id, ai_role, difficulty, first_speaker, thumbnail_url,
    display_order, status, total_question_count, created_at, updated_at
)
VALUES (
    34,
    4,
    '요리 재료의 차이를 쉽게 설명해 주는 식료품점 직원',
    'EASY',
    'AI',
    'https://d19azau1un4t7r.cloudfront.net/content/scenarios/34/thumbnail/21b1937d-9857-4401-acc5-bfc17a218267.png',
    34,
    'ACTIVE',
    3,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_language_variant (
    id, scenario_id, target_locale, base_locale, title, briefing,
    user_opening_instruction, conversation_goal, status, tts_voice_id,
    created_at, updated_at
)
VALUES (
    34,
    34,
    'EN',
    'KR',
    '식료품점에서 대체 재료 찾기',
    '파스타를 만들려고 하지만 필요한 생바질을 찾을 수 없다. 직원이 대체 재료의 맛과 포장 크기, 함께 살 수 있는 할인 상품을 설명한다.',
    NULL,
    '대체 재료의 맛과 포장 크기를 비교하고 선택한 재료와 할인 토마토 구매 여부를 정하기',
    'ACTIVE',
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    100, 34, 1, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    100,
    100,
    'EN',
    'KR',
    'We''re out of fresh basil. Dried basil tastes similar, while parsley is lighter. What flavor do you want in your pasta?',
    '오늘은 생바질이 없어요. 말린 바질은 생바질과 맛이 비슷하고 파슬리는 더 산뜻해요. 파스타에 어떤 맛을 더하고 싶으세요?',
    'ACTIVE',
    '생바질이 없다는 점과 대체 재료의 맛 차이를 먼저 알려야겠다.',
    'GOOD',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    101, 34, 2, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    101,
    101,
    'EN',
    'KR',
    'The small packet covers one meal; the large one covers several. Choose the amount that makes sense for you.',
    '작은 포장은 한 끼 분량이고 큰 포장은 여러 번 쓸 수 있어요. 어느 크기로 드릴까요?',
    'ACTIVE',
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    102, 34, 3, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    102,
    102,
    'EN',
    'KR',
    'Tomatoes are twenty percent off when you buy them with either substitute. Would you like to add some?',
    '대체 재료와 토마토를 함께 구매하면 토마토를 20% 할인해 드려요. 같이 구매하시겠어요?',
    'ACTIVE',
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario (
    id, category_id, ai_role, difficulty, first_speaker, thumbnail_url,
    display_order, status, total_question_count, created_at, updated_at
)
VALUES (
    35,
    4,
    '부담스럽지 않게 취향을 물어보는 의류 매장 직원',
    'EASY',
    'AI',
    'https://d19azau1un4t7r.cloudfront.net/content/scenarios/35/thumbnail/d53a2cfc-1784-40a3-842e-ee154b1be7b4.png',
    35,
    'ACTIVE',
    3,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_language_variant (
    id, scenario_id, target_locale, base_locale, title, briefing,
    user_opening_instruction, conversation_goal, status, tts_voice_id,
    created_at, updated_at
)
VALUES (
    35,
    35,
    'EN',
    'KR',
    '상황에 맞는 옷 추천받기',
    '주말 행사에 입을 옷을 둘러보는 중이다. 직원이 캐주얼 재킷과 격식 있는 블레이저의 색상과 구매 조건을 설명한다.',
    NULL,
    '두 옷의 스타일과 색상, 반품 조건을 비교하고 입어 볼 옷을 선택하기',
    'ACTIVE',
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    103, 35, 1, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    103,
    103,
    'EN',
    'KR',
    'A casual jacket and a dressier blazer are ready to try. What look are you going for at the weekend event?',
    '캐주얼 재킷과 조금 더 격식 있는 블레이저를 입어 볼 수 있어요. 주말 행사에는 어떤 느낌으로 입고 싶으세요?',
    'ACTIVE',
    '손님이 편하게 고를 수 있도록 행사 분위기와 평소 스타일을 먼저 물어봐야겠다.',
    'GOOD',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    104, 35, 2, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    104,
    104,
    'EN',
    'KR',
    'Both come in navy and beige. Let me know the color you want to see in the mirror.',
    '두 옷 모두 네이비와 베이지가 있어요. 어떤 색부터 입어 보실래요?',
    'ACTIVE',
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    105, 35, 3, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    105,
    105,
    'EN',
    'KR',
    'The jacket is final sale, but the blazer can be returned within three days. Which one would you like?',
    '재킷은 반품할 수 없지만 블레이저는 3일 이내에 반품할 수 있어요. 어떤 옷으로 드릴까요?',
    'ACTIVE',
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario (
    id, category_id, ai_role, difficulty, first_speaker, thumbnail_url,
    display_order, status, total_question_count, created_at, updated_at
)
VALUES (
    36,
    4,
    '받는 사람의 취향을 바탕으로 선택지를 좁혀 주는 기념품점 직원',
    'EASY',
    'AI',
    'https://d19azau1un4t7r.cloudfront.net/content/scenarios/36/thumbnail/0389fabf-a6b5-4d4d-9293-b81f18d3d893.png',
    36,
    'ACTIVE',
    3,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_language_variant (
    id, scenario_id, target_locale, base_locale, title, briefing,
    user_opening_instruction, conversation_goal, status, tts_voice_id,
    created_at, updated_at
)
VALUES (
    36,
    36,
    'EN',
    'KR',
    '가격과 특징을 비교해 선물 고르기',
    '여행을 마치기 전에 친구에게 줄 선물을 사려 한다. 직원이 가격과 특징이 다른 과자 세트와 수제 머그잔을 제안한다.',
    NULL,
    '두 선물의 가격과 특징을 비교하고 구매할 선물과 메시지 카드, 포장 방법을 정하기',
    'ACTIVE',
    2,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    106, 36, 1, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    106,
    106,
    'EN',
    'KR',
    'The $15 snack set is easy to share; the $25 handmade mug is a lasting keepsake. Which gift would you like?',
    '15달러짜리 과자 세트는 함께 나눠 먹기 좋고, 25달러짜리 수제 머그잔은 오래 남는 선물이에요. 어떤 걸로 드릴까요?',
    'ACTIVE',
    '두 선물의 차이를 부담 없이 먼저 살펴볼 수 있게 안내해야겠다.',
    'GOOD',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    107, 36, 2, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    107,
    107,
    'EN',
    'KR',
    'I can include a free message card with either gift. Tell me what you''d like the card to say.',
    '어떤 선물을 고르셔도 메시지 카드는 무료로 넣어 드릴 수 있어요. 카드에 어떤 말을 적어 드릴까요?',
    'ACTIVE',
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    108, 36, 3, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    108,
    108,
    'EN',
    'KR',
    'Standard wrapping is free, and gift wrapping costs an extra $3. How would you like it wrapped?',
    '기본 포장은 무료이고 선물용 포장을 추가하면 3달러가 들어요. 어떻게 포장해 드릴까요?',
    'ACTIVE',
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario (
    id, category_id, ai_role, difficulty, first_speaker, thumbnail_url,
    display_order, status, total_question_count, created_at, updated_at
)
VALUES (
    37,
    4,
    '제품 차이를 사용 목적 중심으로 설명하는 전자제품 매장 직원',
    'NORMAL',
    'AI',
    'https://d19azau1un4t7r.cloudfront.net/content/scenarios/37/thumbnail/8538e5a3-a615-43e9-837a-4b980924fe7d.png',
    37,
    'ACTIVE',
    3,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_language_variant (
    id, scenario_id, target_locale, base_locale, title, briefing,
    user_opening_instruction, conversation_goal, status, tts_voice_id,
    created_at, updated_at
)
VALUES (
    37,
    37,
    'EN',
    'KR',
    '기능과 가격을 비교해 헤드폰 고르기',
    '헤드폰을 사기 위해 두 모델을 비교하고 있다. 직원이 가격, 소음 차단, 무게와 색상 차이를 설명한다.',
    NULL,
    '두 모델의 가격과 기능을 비교하고 구매할 모델과 색상을 선택하기',
    'ACTIVE',
    2,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    109, 37, 1, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    109,
    109,
    'EN',
    'KR',
    'Model A blocks more noise and costs $120; Model B is lighter and costs $80. Which model would you like?',
    '헤드폰은 두 모델이 있어요. A 모델은 120달러이고 소음 차단이 더 강하며, B 모델은 80달러이고 더 가벼워요. 어떤 모델로 드릴까요?',
    'ACTIVE',
    '두 모델의 가격과 핵심 차이를 먼저 알려 주고 구매할 모델을 고르게 해야겠다.',
    'GOOD',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    110, 37, 2, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    110,
    110,
    'EN',
    'KR',
    'What stood out to you about that model?',
    '그 모델의 어떤 점이 가장 마음에 드셨나요?',
    'ACTIVE',
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    111, 37, 3, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    111,
    111,
    'EN',
    'KR',
    'Black, white, and gray are available. Which color should I bring for the model you prefer?',
    '검은색, 흰색, 회색이 있어요. 마음에 드는 모델은 어떤 색으로 가져다드릴까요?',
    'ACTIVE',
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario (
    id, category_id, ai_role, difficulty, first_speaker, thumbnail_url,
    display_order, status, total_question_count, created_at, updated_at
)
VALUES (
    38,
    4,
    '직접 만나 중고 자전거를 보여 주는 개인 판매자',
    'HARD',
    'AI',
    'https://d19azau1un4t7r.cloudfront.net/content/scenarios/38/thumbnail/ccb7e991-cb72-4416-9989-e8a9f5de5a5f.png',
    38,
    'ACTIVE',
    3,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_language_variant (
    id, scenario_id, target_locale, base_locale, title, briefing,
    user_opening_instruction, conversation_goal, status, tts_voice_id,
    created_at, updated_at
)
VALUES (
    38,
    38,
    'EN',
    'KR',
    '중고 자전거 상태 확인하고 가격 협상하기',
    '온라인에서 본 중고 자전거를 확인하러 왔다. 판매자가 제품 상태와 판매 가격, 인수 방법을 설명한다.',
    NULL,
    '자전거 상태를 확인하고 판매자가 제시한 가격을 바탕으로 구매 가격과 인수 방법을 정하기',
    'ACTIVE',
    2,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    112, 38, 1, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    112,
    112,
    'EN',
    'KR',
    'You can take it for a test ride or inspect the scratches first. Which would you like to do?',
    '직접 타 보거나 긁힌 부분부터 살펴볼 수 있어요. 어떤 것부터 해 보실래요?',
    'ACTIVE',
    '구매자가 안심하고 상태를 확인할 수 있도록 시승과 흠집 확인부터 제안해야겠다.',
    'GOOD',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    113, 38, 2, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    113,
    113,
    'EN',
    'KR',
    'I''m asking $200, but I''m open to negotiating if you have a good reason. Tell me your price and why.',
    '판매가는 200달러지만 적절한 이유가 있다면 가격을 조정할 수 있어요. 원하는 가격과 이유를 말씀해 주세요.',
    'ACTIVE',
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    114, 38, 3, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    114,
    114,
    'EN',
    'KR',
    'You can take it today or leave a deposit for tomorrow. State the pickup plan you can commit to.',
    '오늘 바로 가져가거나 예약금을 내고 내일 가져갈 수 있어요. 언제 가져가시겠어요?',
    'ACTIVE',
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario (
    id, category_id, ai_role, difficulty, first_speaker, thumbnail_url,
    display_order, status, total_question_count, created_at, updated_at
)
VALUES (
    39,
    4,
    '문제 상황과 구매 기록을 확인하는 매장 반품 창구 직원',
    'NORMAL',
    'USER',
    'https://d19azau1un4t7r.cloudfront.net/content/scenarios/39/thumbnail/72c05afb-11c3-4f49-924a-31d31c46865d.png',
    39,
    'ACTIVE',
    4,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_language_variant (
    id, scenario_id, target_locale, base_locale, title, briefing,
    user_opening_instruction, conversation_goal, status, tts_voice_id,
    created_at, updated_at
)
VALUES (
    39,
    39,
    'EN',
    'KR',
    '불량 상품 교환 또는 환불 요청하기',
    '전날 산 전기주전자가 작동 중 자꾸 꺼진다. 문제를 설명하고 교환과 환불 중 원하는 해결 방법을 요청해야 한다.',
    '반품 창구 직원에게 전날 구매한 전기주전자의 문제를 설명하고, 교환이나 환불이 가능한지 물어보세요.',
    '제품 확인 여부와 교환 또는 환불 방법, 처리 확인서 수령 방법을 선택하기',
    'ACTIVE',
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    115, 39, 1, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    115,
    115,
    'EN',
    'KR',
    'We can test whether it stays on for two minutes, and it won''t affect your return. Would you like us to test it first?',
    '2분 동안 전기주전자가 정상적으로 작동하는지 확인해 볼 수 있어요. 점검을 받아도 반품에는 영향이 없는데, 먼저 확인해 볼까요?',
    'ACTIVE',
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    116, 39, 2, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    116,
    116,
    'EN',
    'KR',
    'Whether we test it or not, a replacement is available today and a refund takes up to five days. Tell me which result you want.',
    '검사 여부와 관계없이 오늘 바로 교환할 수 있고 환불은 최대 5일이 걸려요. 교환과 환불 중 어떤 처리를 원하시나요?',
    'ACTIVE',
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    117, 39, 3, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    117,
    117,
    'EN',
    'KR',
    'I can print the confirmation or email it. Choose where you want the record kept.',
    '처리 확인서는 출력해 드리거나 이메일로 보내 드릴 수 있어요. 어떻게 받아 보시겠어요?',
    'ACTIVE',
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario (
    id, category_id, ai_role, difficulty, first_speaker, thumbnail_url,
    display_order, status, total_question_count, created_at, updated_at
)
VALUES (
    40,
    4,
    '온라인 주문 문제를 해결하는 고객지원 상담원',
    'NORMAL',
    'USER',
    'https://d19azau1un4t7r.cloudfront.net/content/scenarios/40/thumbnail/a2c5b6e6-1f40-4046-a539-7291006e29a1.png',
    40,
    'ACTIVE',
    4,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_language_variant (
    id, scenario_id, target_locale, base_locale, title, briefing,
    user_opening_instruction, conversation_goal, status, tts_voice_id,
    created_at, updated_at
)
VALUES (
    40,
    40,
    'EN',
    'KR',
    '온라인 주문과 다른 상품 문의하기',
    '온라인으로 주문한 가방과 다른 색상의 상품이 배송됐다. 주문 내용과 받은 상품을 비교해 설명하고 반품 또는 재배송 방법을 정해야 한다.',
    '고객지원 상담원에게 주문한 것과 다른 상품이 도착했다고 설명하고, 해결 방법을 문의하세요.',
    '재배송 또는 환불 중 해결 방법을 고르고 반품 방법과 진행 날짜를 선택하기',
    'ACTIVE',
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    118, 40, 1, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    118,
    118,
    'EN',
    'KR',
    'We can resend the correct color or issue a refund. Tell me how you''d like us to fix the order.',
    '주문하신 색상으로 다시 보내 드리거나 환불해 드릴 수 있어요. 어떻게 처리해 드리면 좋을지 말씀해 주세요.',
    'ACTIVE',
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    119, 40, 2, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    119,
    119,
    'EN',
    'KR',
    'Drop-off and pickup are both available for the wrong item. Give me the return method that fits best.',
    '잘못 배송된 상품은 직접 가져다주시거나 수거를 요청하실 수 있어요. 어떤 반품 방법이 편하세요?',
    'ACTIVE',
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
INSERT INTO scenario_question (
    id, scenario_id, display_order, status, created_at, updated_at
)
VALUES (
    120, 40, 3, 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO scenario_question_language_variant (
    id, scenario_question_id, target_locale, base_locale, question_text,
    question_translation, status, inner_thought, inner_thought_type,
    created_at, updated_at
)
VALUES (
    120,
    120,
    'EN',
    'KR',
    'We can schedule the return for today or tomorrow. Which day works better for your drop-off or pickup?',
    '반품은 오늘이나 내일 진행할 수 있어요. 방문 접수나 수거를 언제 진행할까요?',
    'ACTIVE',
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

ALTER TABLE category ALTER COLUMN id RESTART WITH 5;
ALTER TABLE category_language_variant ALTER COLUMN id RESTART WITH 5;
ALTER TABLE scenario ALTER COLUMN id RESTART WITH 41;
ALTER TABLE scenario_language_variant ALTER COLUMN id RESTART WITH 41;
ALTER TABLE scenario_question ALTER COLUMN id RESTART WITH 121;
ALTER TABLE scenario_question_language_variant ALTER COLUMN id RESTART WITH 121;
