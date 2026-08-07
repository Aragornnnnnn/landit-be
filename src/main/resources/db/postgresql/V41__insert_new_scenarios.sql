-- 신규 시나리오 20개(쇼핑 카테고리 포함)와 고정 질문 60건을 추가한다.
-- =============================================================================
-- 신규 시나리오 20개 INSERT SQL (scenario id 21~40)
-- 생성일: 2026-08-06
--
-- 구성:
--   1) scenario 20건 (id 21~40) — display_order는 40일 글로벌 플랜의 Day 번호
--   2) scenario_question 60건 (id 61~120, 시나리오당 3개)
--   3) scenario_question_language_variant 60건 (EN/KR)
--
-- 전제/주의:
--   - 카테고리: 1=기숙사, 2=여행, 3=수업, 4=쇼핑(신규 — 이 파일 0)섹션에서
--     category + category_language_variant INSERT 포함).
--   - thumbnail_url은 NULL — 썸네일 제작 후 UPDATE 필요.
--   - total_question_count: USER 먼저(유저 시작 발화 포함) = 4, AI 먼저 = 3.
--   - inner_thought는 신규분 미정이라 NULL — 확정되면 Q1 variant에 UPDATE.
--   - id를 명시했으므로 auto-increment 시퀀스 사용 시 반영 후 시퀀스 재조정 필요
--     (Postgres 예: SELECT setval('scenario_id_seq', 40); 등 실제 시퀀스명에 맞게)
--   - 제목(title)은 scenario 테이블에 없음 — 제목 테이블에 별도 INSERT 필요
--     (파일 하단 제목 목록 참고)
-- =============================================================================


-- ---------------------------------------------------------------------------
-- 0) category '쇼핑' (id 4) — 신규 시나리오 34~40이 참조하므로 최우선 실행
-- ---------------------------------------------------------------------------
INSERT INTO category (id, display_order, status, created_at, updated_at) VALUES
(4, 4, 'ACTIVE', now(), now());

INSERT INTO category_language_variant (id, category_id, base_locale, name, created_at, updated_at) VALUES
(4, 4, 'KR', '쇼핑', now(), now());

-- ---------------------------------------------------------------------------
-- 1) scenario (id 21~40)
-- ---------------------------------------------------------------------------
INSERT INTO scenario (id, category_id, ai_role, difficulty, first_speaker, thumbnail_url, display_order, status, created_at, updated_at, total_question_count) VALUES
(21, 1, '차분하고 친절한 기숙사 생활지원실 직원',                 'EASY',   'USER', NULL,  6, 'ACTIVE', now(), now(), 4),  -- Day 6  방 키 분실
(22, 1, '룸메이트, 공용 냉장고 정리 규칙을 정하고 싶어함',          'NORMAL', 'AI',   NULL, 15, 'ACTIVE', now(), now(), 3),  -- Day 15 공용 냉장고 (Marco)
(23, 1, '세탁실에서 옷 한 벌을 찾고 있는 친근한 기숙사 이웃',      'EASY',   'AI',   NULL, 11, 'ACTIVE', now(), now(), 3),  -- Day 11 세탁물 섞임
(24, 1, '기숙사 시설 관리 접수 직원',                              'NORMAL', 'USER', NULL, 17, 'ACTIVE', now(), now(), 4),  -- Day 17 세면대 누수
(25, 1, '기숙사 퇴실 점검을 담당하는 하우징 직원',                 'HARD',   'AI',   NULL, 40, 'ACTIVE', now(), now(), 3),  -- Day 40 퇴실 점검 (피날레)
(26, 2, '여행자의 일정을 함께 확인해 주는 기차역 매표소 직원',     'EASY',   'USER', NULL, 26, 'ACTIVE', now(), now(), 4),  -- Day 26 기차표
(27, 2, '메뉴를 잘 설명해 주는 현지 식당 서버',                    'NORMAL', 'AI',   NULL, 30, 'ACTIVE', now(), now(), 3),  -- Day 30 식당 메뉴
(28, 2, '박물관과 도시 관광권을 판매하는 관광 안내소 직원',        'EASY',   'AI',   NULL, 29, 'ACTIVE', now(), now(), 3),  -- Day 29 박물관 패스
(29, 2, '야외 투어 일정 변경을 담당하는 여행사 직원',              'HARD',   'USER', NULL, 32, 'ACTIVE', now(), now(), 4),  -- Day 32 투어 변경
(30, 2, '가능한 좌석을 차분하게 안내하는 공항 탑승구 직원',        'NORMAL', 'AI',   NULL, 35, 'ACTIVE', now(), now(), 3),  -- Day 35 공항 좌석
(31, 3, '학생의 목표와 현실적인 학업 부담을 함께 고려하는 지도교수','HARD',  'AI',   NULL, 20, 'ACTIVE', now(), now(), 3),  -- Day 20 지도교수 상담
(32, 3, '같은 수업을 듣는 친절한 동급생',                          'EASY',   'AI',   NULL, 13, 'ACTIVE', now(), now(), 3),  -- Day 13 결석 노트 (Marco)
(33, 3, '과제 선택지를 쉽게 비교해 주는 수업 조교',                'NORMAL', 'AI',   NULL, 19, 'ACTIVE', now(), now(), 3),  -- Day 19 과제 제출
(34, 4, '요리 재료의 차이를 쉽게 설명해 주는 식료품점 직원',       'EASY',   'AI',   NULL,  9, 'ACTIVE', now(), now(), 3),  -- Day 9  대체 재료
(35, 4, '부담스럽지 않게 취향을 물어보는 의류 매장 직원',          'EASY',   'AI',   NULL, 21, 'ACTIVE', now(), now(), 3),  -- Day 21 옷 추천
(36, 4, '받는 사람의 취향을 바탕으로 선택지를 좁혀 주는 기념품점 직원','EASY','AI',  NULL, 34, 'ACTIVE', now(), now(), 3),  -- Day 34 선물 고르기
(37, 4, '제품 차이를 사용 목적 중심으로 설명하는 전자제품 매장 직원','NORMAL','AI', NULL, 23, 'ACTIVE', now(), now(), 3),  -- Day 23 헤드폰
(38, 4, '직접 만나 중고 자전거를 보여 주는 개인 판매자',           'HARD',   'AI',   NULL, 25, 'ACTIVE', now(), now(), 3),  -- Day 25 중고 자전거
(39, 4, '문제 상황과 구매 기록을 확인하는 매장 반품 창구 직원',    'NORMAL', 'USER', NULL, 24, 'ACTIVE', now(), now(), 4),  -- Day 24 불량 교환
(40, 4, '통신사 매장에서 유심과 요금제를 안내하는 직원',           'NORMAL', 'USER', NULL,  3, 'ACTIVE', now(), now(), 4);  -- Day 3  유심 구매

-- ---------------------------------------------------------------------------
-- 2) scenario_question (id 61~120, 시나리오당 3개)
-- ---------------------------------------------------------------------------
INSERT INTO scenario_question (id, scenario_id, display_order, status, created_at, updated_at) VALUES
( 61, 21, 1, 'ACTIVE', now(), now()), ( 62, 21, 2, 'ACTIVE', now(), now()), ( 63, 21, 3, 'ACTIVE', now(), now()),
( 64, 22, 1, 'ACTIVE', now(), now()), ( 65, 22, 2, 'ACTIVE', now(), now()), ( 66, 22, 3, 'ACTIVE', now(), now()),
( 67, 23, 1, 'ACTIVE', now(), now()), ( 68, 23, 2, 'ACTIVE', now(), now()), ( 69, 23, 3, 'ACTIVE', now(), now()),
( 70, 24, 1, 'ACTIVE', now(), now()), ( 71, 24, 2, 'ACTIVE', now(), now()), ( 72, 24, 3, 'ACTIVE', now(), now()),
( 73, 25, 1, 'ACTIVE', now(), now()), ( 74, 25, 2, 'ACTIVE', now(), now()), ( 75, 25, 3, 'ACTIVE', now(), now()),
( 76, 26, 1, 'ACTIVE', now(), now()), ( 77, 26, 2, 'ACTIVE', now(), now()), ( 78, 26, 3, 'ACTIVE', now(), now()),
( 79, 27, 1, 'ACTIVE', now(), now()), ( 80, 27, 2, 'ACTIVE', now(), now()), ( 81, 27, 3, 'ACTIVE', now(), now()),
( 82, 28, 1, 'ACTIVE', now(), now()), ( 83, 28, 2, 'ACTIVE', now(), now()), ( 84, 28, 3, 'ACTIVE', now(), now()),
( 85, 29, 1, 'ACTIVE', now(), now()), ( 86, 29, 2, 'ACTIVE', now(), now()), ( 87, 29, 3, 'ACTIVE', now(), now()),
( 88, 30, 1, 'ACTIVE', now(), now()), ( 89, 30, 2, 'ACTIVE', now(), now()), ( 90, 30, 3, 'ACTIVE', now(), now()),
( 91, 31, 1, 'ACTIVE', now(), now()), ( 92, 31, 2, 'ACTIVE', now(), now()), ( 93, 31, 3, 'ACTIVE', now(), now()),
( 94, 32, 1, 'ACTIVE', now(), now()), ( 95, 32, 2, 'ACTIVE', now(), now()), ( 96, 32, 3, 'ACTIVE', now(), now()),
( 97, 33, 1, 'ACTIVE', now(), now()), ( 98, 33, 2, 'ACTIVE', now(), now()), ( 99, 33, 3, 'ACTIVE', now(), now()),
(100, 34, 1, 'ACTIVE', now(), now()), (101, 34, 2, 'ACTIVE', now(), now()), (102, 34, 3, 'ACTIVE', now(), now()),
(103, 35, 1, 'ACTIVE', now(), now()), (104, 35, 2, 'ACTIVE', now(), now()), (105, 35, 3, 'ACTIVE', now(), now()),
(106, 36, 1, 'ACTIVE', now(), now()), (107, 36, 2, 'ACTIVE', now(), now()), (108, 36, 3, 'ACTIVE', now(), now()),
(109, 37, 1, 'ACTIVE', now(), now()), (110, 37, 2, 'ACTIVE', now(), now()), (111, 37, 3, 'ACTIVE', now(), now()),
(112, 38, 1, 'ACTIVE', now(), now()), (113, 38, 2, 'ACTIVE', now(), now()), (114, 38, 3, 'ACTIVE', now(), now()),
(115, 39, 1, 'ACTIVE', now(), now()), (116, 39, 2, 'ACTIVE', now(), now()), (117, 39, 3, 'ACTIVE', now(), now()),
(118, 40, 1, 'ACTIVE', now(), now()), (119, 40, 2, 'ACTIVE', now(), now()), (120, 40, 3, 'ACTIVE', now(), now());

-- ---------------------------------------------------------------------------
-- 3) scenario_question_language_variant (EN/KR, id 61~120)
-- ---------------------------------------------------------------------------

-- 신1 | 방 키 분실 (scenario 21, Day 6)
INSERT INTO scenario_question_language_variant (id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, created_at, updated_at, inner_thought, inner_thought_type) VALUES
(61, 61, 'EN', 'KR', 'No worries, it happens all the time. I can give you a temporary key, or someone can walk up and let you in. Which works better for you?', '걱정 마세요, 자주 있는 일이에요. 임시 키를 드리거나 직원이 올라가서 문을 열어드릴 수 있어요. 어느 쪽이 나으세요?', 'ACTIVE', now(), now(), NULL, NULL),
(62, 62, 'EN', 'KR', 'For the replacement, we can have it ready tomorrow morning, or the day after in the afternoon. When would you like to pick it up?', '새 키는 내일 오전이나 모레 오후에 준비돼요. 언제 찾으러 오시겠어요?', 'ACTIVE', now(), now(), NULL, NULL),
(63, 63, 'EN', 'KR', 'And we''ll let you know when it''s ready — would you rather get a text or an email?', '준비되면 알려드릴게요 — 문자가 나아요, 이메일이 나아요?', 'ACTIVE', now(), now(), NULL, NULL);

-- 신2 | 공용 냉장고 (scenario 22, Day 15)
INSERT INTO scenario_question_language_variant (id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, created_at, updated_at, inner_thought, inner_thought_type) VALUES
(64, 64, 'EN', 'KR', 'So, the fridge has been getting out of hand lately. I was thinking — we could put names on our own stuff, and a green sticker on anything that''s up for grabs. What do you think?', '요즘 냉장고 정리가 좀 어려워져서 말이야. 생각해봤는데 — 각자 음식엔 이름 쓰고, 같이 먹어도 되는 건 초록 스티커 붙이면 어때?', 'ACTIVE', now(), now(), NULL, NULL),
(65, 65, 'EN', 'KR', 'And for stuff with no name on it — how about we post a photo in the group chat and give it 24 hours before tossing it? Is that fair?', '이름 없는 건 단톡방에 사진 올리고 24시간 기다렸다가 버리는 걸로 하면 어때? 그 정도면 괜찮지?', 'ACTIVE', now(), now(), NULL, NULL),
(66, 66, 'EN', 'KR', 'We should probably clean the fridge out regularly too. Would Friday or Sunday work better for you?', '냉장고 정리도 주기적으로 하는 게 좋을 것 같은데. 금요일이랑 일요일 중 언제가 나아?', 'ACTIVE', now(), now(), NULL, NULL);

-- 신3 | 세탁물 섞임 (scenario 23, Day 11)
INSERT INTO scenario_question_language_variant (id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, created_at, updated_at, inner_thought, inner_thought_type) VALUES
(67, 67, 'EN', 'KR', 'Hey, sorry — I think our laundry got mixed up. I''ve got your white T-shirt, and I think my blue shirt''s in your pile. Could you take a look for me?', '저기, 미안한데 — 우리 빨래가 섞인 것 같아. 네 흰 티셔츠가 나한테 있고, 내 파란 셔츠가 네 빨래에 있는 것 같은데. 한번 찾아봐줄 수 있어?', 'ACTIVE', now(), now(), NULL, NULL),
(68, 68, 'EN', 'KR', 'Oh, are you done with your drying? If not, wanna throw our stuff in together? It''s cheaper that way. Totally fine if you''d rather do yours separately, though!', '아, 너 건조기 다 돌렸어? 아직이면 같이 돌릴래? 그게 더 싸거든. 따로 하고 싶으면 편하게 말해도 돼!', 'ACTIVE', now(), now(), NULL, NULL),
(69, 69, 'EN', 'KR', 'And if our stuff gets mixed up again, should we just leave it by each other''s door, or text and meet up?', '다음에 또 섞이면 그냥 서로 방문 앞에 둘까, 아니면 문자하고 만나서 줄까?', 'ACTIVE', now(), now(), NULL, NULL);

-- 신4 | 세면대 누수 (scenario 24, Day 17)
INSERT INTO scenario_question_language_variant (id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, created_at, updated_at, inner_thought, inner_thought_type) VALUES
(70, 70, 'EN', 'KR', 'Okay, please don''t use the sink for now. Do you need anything while you wait — towels, a bucket? I can also walk you through shutting off the water.', '네, 일단 세면대는 쓰지 말아 주세요. 기다리시는 동안 필요한 거 있으세요 — 수건이나 양동이? 물 잠그는 방법도 알려드릴 수 있어요.', 'ACTIVE', now(), now(), NULL, NULL),
(71, 71, 'EN', 'KR', 'We can send someone at six this evening or nine tomorrow morning. Which time works for you?', '오늘 저녁 6시나 내일 오전 9시에 기사님을 보낼 수 있어요. 어느 시간이 괜찮으세요?', 'ACTIVE', now(), now(), NULL, NULL),
(72, 72, 'EN', 'KR', 'Should the technician call you first, or just knock? And if you''re not in, is it okay for them to go in?', '기사님이 미리 전화드릴까요, 아니면 그냥 노크할까요? 그리고 안 계시면 들어가서 작업해도 될까요?', 'ACTIVE', now(), now(), NULL, NULL);

-- 신5 | 퇴실 점검·보증금 (scenario 25, Day 40 피날레)
INSERT INTO scenario_question_language_variant (id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, created_at, updated_at, inner_thought, inner_thought_type) VALUES
(73, 73, 'EN', 'KR', 'Before the inspection, you''ll need to clear out your things and give the room a good clean. Would you like a checklist, or should we do a quick walk-through together first?', '점검 전에 짐을 다 빼고 방을 청소해 두셔야 해요. 체크리스트를 드릴까요, 아니면 먼저 같이 한번 둘러볼까요?', 'ACTIVE', now(), now(), NULL, NULL),
(74, 74, 'EN', 'KR', 'We have slots today at three or tomorrow at ten. Which one should I book you in for?', '오늘 3시랑 내일 10시에 자리가 있어요. 언제로 잡아드릴까요?', 'ACTIVE', now(), now(), NULL, NULL),
(75, 75, 'EN', 'KR', 'One more thing — if anything''s damaged, like the furniture or the walls, it may come out of your deposit. You knew that, right? If there''s any damage that wasn''t your fault, have some proof ready — photos from move-in, anything like that.', '한 가지 더요 — 가구나 벽 같은 게 파손돼 있으면 보증금에서 차감될 수 있어요. 알고 계시죠? 본인이 그런 게 아닌 파손이 있다면 증빙을 미리 준비해 두세요 — 입주 때 찍은 사진 같은 거요.', 'ACTIVE', now(), now(), NULL, NULL);

-- 신6 | 기차표 구매 (scenario 26, Day 26)
INSERT INTO scenario_question_language_variant (id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, created_at, updated_at, inner_thought, inner_thought_type) VALUES
(76, 76, 'EN', 'KR', 'So the direct train gets you there at four and it''s $45. The one with a transfer arrives at five, but it''s only $30. Which one would you like?', '직행은 4시 도착에 45달러고요, 환승편은 5시 도착인데 30달러예요. 어떤 걸로 드릴까요?', 'ACTIVE', now(), now(), NULL, NULL),
(77, 77, 'EN', 'KR', 'Would you like the ticket printed, or sent to your phone?', '표는 출력해 드릴까요, 휴대폰으로 보내 드릴까요?', 'ACTIVE', now(), now(), NULL, NULL),
(78, 78, 'EN', 'KR', 'And any seat preference — window, aisle?', '좌석은 원하시는 거 있으세요 — 창가, 통로?', 'ACTIVE', now(), now(), NULL, NULL);

-- 신7 | 식당 메뉴 (scenario 27, Day 30)
INSERT INTO scenario_question_language_variant (id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, created_at, updated_at, inner_thought, inner_thought_type) VALUES
(79, 79, 'EN', 'KR', 'Honestly, everything here is good. The pasta''s mild and creamy, and the curry''s got a real kick to it. What are you in the mood for?', '솔직히 저희 음식은 다 맛있어요. 파스타는 부드럽고 크리미하고, 커리는 확실히 매콤해요. 어떤 맛이 당기세요?', 'ACTIVE', now(), now(), NULL, NULL),
(80, 80, 'EN', 'KR', 'Just so you know, we can do the pasta without cheese, or the curry without fish sauce. Want it as is, or should we adjust anything?', '참고로 파스타는 치즈 빼고, 커리는 피시소스 빼고도 돼요. 그대로 드릴까요, 아니면 빼드릴까요?', 'ACTIVE', now(), now(), NULL, NULL),
(81, 81, 'EN', 'KR', 'Oh, and we''re doing a little event — leave us a Google review and you get a free side. Wanna do it? Salad or rice?', '아, 그리고 저희 이벤트 하나 하고 있는데 — 구글 리뷰 남겨주시면 사이드가 무료예요. 하시겠어요? 샐러드랑 밥 중에 뭘로 드릴까요?', 'ACTIVE', now(), now(), NULL, NULL);

-- 신8 | 박물관 패스 (scenario 28, Day 29)
INSERT INTO scenario_question_language_variant (id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, created_at, updated_at, inner_thought, inner_thought_type) VALUES
(82, 82, 'EN', 'KR', 'Sure! The museum pass gets you into two museums, and the city pass includes public transit too. Which sounds more like your day?', '네! 박물관 패스는 박물관 두 곳 입장이고, 도시 패스는 대중교통까지 포함이에요. 오늘 일정엔 어느 쪽이 맞을 것 같으세요?', 'ACTIVE', now(), now(), NULL, NULL),
(83, 83, 'EN', 'KR', 'They''re both $30, and for $15 more you can add the lunch set. Would you like to include it?', '둘 다 30달러고요, 15달러를 추가하시면 점심 세트까지 포함돼요. 추가하시겠어요?', 'ACTIVE', now(), now(), NULL, NULL),
(84, 84, 'EN', 'KR', 'And would you like a paper pass, or the digital one on your phone?', '패스는 종이로 드릴까요, 휴대폰 디지털로 드릴까요?', 'ACTIVE', now(), now(), NULL, NULL);

-- 신9 | 투어 일정 변경 (scenario 29, Day 32)
INSERT INTO scenario_question_language_variant (id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, created_at, updated_at, inner_thought, inner_thought_type) VALUES
(85, 85, 'EN', 'KR', 'Of course, let''s find you another time. The morning tour is a smaller group, and the evening one includes the city lights. Which would you prefer?', '그럼요, 다른 시간으로 잡아드릴게요. 오전 투어는 소규모고, 저녁 투어는 야경까지 볼 수 있어요. 어느 쪽이 좋으세요?', 'ACTIVE', now(), now(), NULL, NULL),
(86, 86, 'EN', 'KR', 'Just so you know, the evening tour is $10 more than your original booking. Is that okay with you?', '참고로 저녁 투어는 원래 예약보다 10달러 더 비싸요. 괜찮으세요?', 'ACTIVE', now(), now(), NULL, NULL),
(87, 87, 'EN', 'KR', 'By the way, we also run a food tour and a night river cruise — people love them. Want to try one while you''re here? I can get you a discount.', '그런데 저희가 푸드 투어랑 야간 리버 크루즈도 하거든요 — 반응 진짜 좋아요. 계시는 동안 하나 해보실래요? 할인해 드릴 수 있어요.', 'ACTIVE', now(), now(), NULL, NULL);

-- 신10 | 공항 좌석 (scenario 30, Day 35)
INSERT INTO scenario_question_language_variant (id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, created_at, updated_at, inner_thought, inner_thought_type) VALUES
(88, 88, 'EN', 'KR', 'For an extra $50, I can move you up to extra-legroom economy — you can actually stretch your legs. Would you like to do that?', '50달러만 추가하시면 레그룸 넓은 이코노미로 올려드릴 수 있어요 — 다리를 쭉 뻗으실 수 있죠. 하시겠어요?', 'ACTIVE', now(), now(), NULL, NULL),
(89, 89, 'EN', 'KR', 'There''s also an exit-row seat available for free, but you''d need to help the crew in an emergency. Would you be comfortable with that?', '비상구 좌석은 무료로 바꿔드릴 수 있는데, 비상시엔 승무원을 도와주셔야 해요. 괜찮으시겠어요?', 'ACTIVE', now(), now(), NULL, NULL),
(90, 90, 'EN', 'KR', 'Boarding starts in about twenty minutes. Overhead space is tight today, so we''re checking carry-ons for free — do you want to check yours?', '탑승은 20분 뒤에 시작해요. 오늘 기내 짐칸이 부족해서 캐리어를 무료로 부쳐드리고 있는데 — 부치시겠어요?', 'ACTIVE', now(), now(), NULL, NULL);

-- 신11 | 지도교수 상담 (scenario 31, Day 20)
INSERT INTO scenario_question_language_variant (id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, created_at, updated_at, inner_thought, inner_thought_type) VALUES
(91, 91, 'EN', 'KR', 'So, one course runs on weekly projects, and the other comes down to two big exams. Which style suits you better?', '자, 한 과목은 매주 프로젝트로 진행되고, 다른 과목은 큰 시험 두 번으로 결정돼요. 어느 방식이 더 잘 맞을 것 같아요?', 'ACTIVE', now(), now(), NULL, NULL),
(92, 92, 'EN', 'KR', 'And why that one? I''d like to hear your thinking.', '왜 그쪽인가요? 학생 생각을 듣고 싶어요.', 'ACTIVE', now(), now(), NULL, NULL),
(93, 93, 'EN', 'KR', 'That course has two sections — Tuesday mornings or Thursday afternoons. Which fits your schedule better?', '그 과목은 분반이 둘이에요 — 화요일 오전과 목요일 오후. 어느 쪽이 시간표에 맞아요?', 'ACTIVE', now(), now(), NULL, NULL);

-- 신12 | 결석 노트 (scenario 32, Day 13, Marco)
INSERT INTO scenario_question_language_variant (id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, created_at, updated_at, inner_thought, inner_thought_type) VALUES
(94, 94, 'EN', 'KR', 'Hey, I heard you were out sick today — you okay? I took notes on the example problems and the assignment changes. What do you want to see first?', '야, 오늘 아파서 못 나왔다며 — 괜찮아? 예제 풀이랑 과제 바뀐 거 필기해놨어. 뭐부터 볼래?', 'ACTIVE', now(), now(), NULL, NULL),
(95, 95, 'EN', 'KR', 'I can snap photos and send them now, or just lend you my whole notebook later. What helps more?', '지금 찍어서 보내줄 수도 있고, 나중에 노트 통째로 빌려줄 수도 있어. 뭐가 더 도움 돼?', 'ACTIVE', now(), now(), NULL, NULL),
(96, 96, 'EN', 'KR', 'Hey, I''m basically saving your life here — you''re buying me dinner later, right? Something fancy, obviously. Deal?', '야, 이거 거의 내가 목숨 구해주는 거다? 나중에 밥 사는 거지? 당연히 비싼 걸로. 콜?', 'ACTIVE', now(), now(), NULL, NULL);

-- 신13 | 과제 제출 방식 (scenario 33, Day 19)
INSERT INTO scenario_question_language_variant (id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, created_at, updated_at, inner_thought, inner_thought_type) VALUES
(97, 97, 'EN', 'KR', 'So for this assignment, you''ve got two options — a three-page report, or a five-minute presentation. Which way are you leaning?', '이번 과제는 둘 중에 고르면 돼요 — 3쪽 보고서 아니면 5분 발표. 어느 쪽으로 마음이 기울어요?', 'ACTIVE', now(), now(), NULL, NULL),
(98, 98, 'EN', 'KR', 'What made you go with that one?', '그걸 고른 이유가 있어요?', 'ACTIVE', now(), now(), NULL, NULL),
(99, 99, 'EN', 'KR', 'And this time, the deadline is the deadline. Remember last time you were one minute late and I let it slide? Not this time — I''ll take points off. Okay?', '그리고 이번엔 마감 꼭 지켜야 해요. 저번에 1분 늦게 냈는데 제가 봐준 거 기억하죠? 이번엔 안 봐줘요 — 점수 깎을 거예요. 알겠죠?', 'ACTIVE', now(), now(), NULL, NULL);

-- 신14 | 식료품 대체 재료 (scenario 34, Day 9)
INSERT INTO scenario_question_language_variant (id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, created_at, updated_at, inner_thought, inner_thought_type) VALUES
(100, 100, 'EN', 'KR', 'Ah, we''re actually out of fresh basil today, sorry! What are you cooking? I can help you find a good substitute.', '아, 오늘 생바질이 다 나갔어요, 죄송해요! 어떤 요리에 쓰시려고요? 맞는 대체 재료 찾아드릴게요.', 'ACTIVE', now(), now(), NULL, NULL),
(101, 101, 'EN', 'KR', 'The big pack is cheaper, but it''s a lot — if you live alone, some might go to waste. The small one costs a bit more, but you''ll finish it before it expires. It doesn''t keep long, so — which one would you go with?', '큰 포장은 저렴한데 양이 많아서 혼자 사시면 남을 수 있어요. 작은 포장은 좀 비싸지만 유통기한 안에 다 드실 수 있고요. 유통기한이 짧은 제품이라 — 어떤 걸로 하시겠어요?', 'ACTIVE', now(), now(), NULL, NULL),
(102, 102, 'EN', 'KR', 'Oh, and tomatoes are 20% off if you buy them together. Want to grab some?', '아, 그리고 같이 사시면 토마토가 20% 할인이에요. 좀 담아드릴까요?', 'ACTIVE', now(), now(), NULL, NULL);

-- 신15 | 옷 추천 (scenario 35, Day 21)
INSERT INTO scenario_question_language_variant (id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, created_at, updated_at, inner_thought, inner_thought_type) VALUES
(103, 103, 'EN', 'KR', 'So we''ve got this casual jacket, or a dressier blazer. What''s the vibe for the event — pretty relaxed, or a bit more formal?', '캐주얼한 재킷이랑 좀 더 격식 있는 블레이저가 있어요. 행사 분위기가 어때요 — 편한 자리예요, 좀 갖춰 입는 자리예요?', 'ACTIVE', now(), now(), NULL, NULL),
(104, 104, 'EN', 'KR', 'Both come in navy and beige. Which color do you want to try on first?', '둘 다 네이비랑 베이지가 있어요. 어떤 색부터 입어보실래요?', 'ACTIVE', now(), now(), NULL, NULL),
(105, 105, 'EN', 'KR', 'One thing to know — the jacket''s final sale, but the blazer you can return within three days. Which one are you going with?', '하나만 참고하세요 — 재킷은 반품이 안 되고, 블레이저는 3일 안에 반품돼요. 어떤 걸로 하시겠어요?', 'ACTIVE', now(), now(), NULL, NULL);

-- 신16 | 선물 고르기 (scenario 36, Day 34)
INSERT INTO scenario_question_language_variant (id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, created_at, updated_at, inner_thought, inner_thought_type) VALUES
(106, 106, 'EN', 'KR', 'For a friend? Okay — the snack set is $15 and easy to share, or there''s this handmade mug for $25, something they''d keep. Which feels more like your friend?', '친구 선물이요? 그럼 — 과자 세트는 15달러에 나눠 먹기 좋고, 수제 머그잔은 25달러인데 오래 간직할 수 있어요. 친구분한텐 어느 쪽이 어울릴 것 같아요?', 'ACTIVE', now(), now(), NULL, NULL),
(107, 107, 'EN', 'KR', 'We can pop in a message card for free. What would you like it to say?', '메시지 카드는 무료로 넣어드려요. 뭐라고 적어드릴까요?', 'ACTIVE', now(), now(), NULL, NULL),
(108, 108, 'EN', 'KR', 'And wrapping — standard is free, or gift wrapping is $3 extra. How should I wrap it?', '포장은 기본이 무료고, 선물 포장은 3달러 추가예요. 어떻게 포장해 드릴까요?', 'ACTIVE', now(), now(), NULL, NULL);

-- 신17 | 헤드폰 (scenario 37, Day 23)
INSERT INTO scenario_question_language_variant (id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, created_at, updated_at, inner_thought, inner_thought_type) VALUES
(109, 109, 'EN', 'KR', 'So Model A is $120 with stronger noise canceling, and Model B is $80 and a lot lighter. Which one sounds more like what you need?', 'A 모델은 120달러에 노이즈 캔슬링이 더 세고, B 모델은 80달러에 훨씬 가벼워요. 어느 쪽이 더 필요에 맞을 것 같으세요?', 'ACTIVE', now(), now(), NULL, NULL),
(110, 110, 'EN', 'KR', 'What matters most to you in headphones — the sound, the weight, or the price?', '헤드폰 고를 때 뭐가 제일 중요하세요 — 음질, 무게, 가격?', 'ACTIVE', now(), now(), NULL, NULL),
(111, 111, 'EN', 'KR', 'It comes in black, white, and gray. Which color should I grab for you?', '색상은 검정, 흰색, 회색이 있어요. 어떤 색으로 가져다드릴까요?', 'ACTIVE', now(), now(), NULL, NULL);

-- 신18 | 중고 자전거 (scenario 38, Day 25)
INSERT INTO scenario_question_language_variant (id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, created_at, updated_at, inner_thought, inner_thought_type) VALUES
(112, 112, 'EN', 'KR', 'Here it is! Feel free to take it for a spin, or look it over first — there are a couple of scratches I''ll show you. What do you want to do first?', '이게 그 자전거예요! 한번 타보셔도 되고, 먼저 살펴보셔도 돼요 — 긁힌 데 몇 군데는 보여드릴게요. 뭐부터 해보실래요?', 'ACTIVE', now(), now(), NULL, NULL),
(113, 113, 'EN', 'KR', 'I''m asking $200, but I''m open to offers if you''ve got a fair reason. What were you thinking?', '200달러 생각하고 있는데, 합당한 이유가 있으면 조정할 의향 있어요. 얼마 생각하세요?', 'ACTIVE', now(), now(), NULL, NULL),
(114, 114, 'EN', 'KR', 'Alright — you can pay the full amount in cash today, right? Then we''ve got a deal.', '좋아요 — 오늘 전액 현금으로 주실 수 있는 거 맞죠? 그럼 거래하시죠.', 'ACTIVE', now(), now(), NULL, NULL);

-- 신19 | 불량 교환/환불 (scenario 39, Day 24)
INSERT INTO scenario_question_language_variant (id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, created_at, updated_at, inner_thought, inner_thought_type) VALUES
(115, 115, 'EN', 'KR', 'Oh no, sorry about that. Do you mind if we test it real quick to see if it cuts out? It won''t affect your return either way.', '아이고, 죄송해요. 금방 꺼지는지 저희가 잠깐 테스트해봐도 될까요? 어느 쪽이든 반품에는 영향 없어요.', 'ACTIVE', now(), now(), NULL, NULL),
(116, 116, 'EN', 'KR', 'Either way, here are your options — I can swap it for a new one today, or do a refund, which takes up to five days. Which would you prefer?', '어느 쪽이든 선택지는 이래요 — 오늘 새 제품으로 바로 교환하거나, 환불은 최대 5일 걸려요. 어떤 게 나으세요?', 'ACTIVE', now(), now(), NULL, NULL),
(117, 117, 'EN', 'KR', 'Again, I''m so sorry about the trouble. Here — we''d like to give you a box of tea as an apology. I hope that makes up for it a little!', '다시 한번 정말 죄송해요. 이건 사과의 의미로 차 한 박스 드릴게요. 이걸로 마음이 조금 풀리시면 좋겠어요!', 'ACTIVE', now(), now(), NULL, NULL);

-- 신20 | 유심 구매 (scenario 40, Day 3)
INSERT INTO scenario_question_language_variant (id, scenario_question_id, target_locale, base_locale, question_text, question_translation, status, created_at, updated_at, inner_thought, inner_thought_type) VALUES
(118, 118, 'EN', 'KR', 'Sure! How long are you staying here, and do you use a lot of data?', '네! 여기 얼마나 계실 예정이에요? 데이터는 많이 쓰시는 편이에요?', 'ACTIVE', now(), now(), NULL, NULL),
(119, 119, 'EN', 'KR', 'So the prepaid plan is $30 for ten gigs, and unlimited is $45 a month. Which one fits you better?', '선불 요금제는 30달러에 10기가고, 무제한은 한 달에 45달러예요. 어느 쪽이 맞을 것 같으세요?', 'ACTIVE', now(), now(), NULL, NULL),
(120, 120, 'EN', 'KR', 'I''ll need your passport to register the SIM. And should I set up auto-pay, or will you top it up yourself each month?', '유심 등록에 여권이 필요해요. 그리고 자동 결제로 해드릴까요, 아니면 매달 직접 충전하시겠어요?', 'ACTIVE', now(), now(), NULL, NULL);


-- =============================================================================
-- [참고 1] 신규 시나리오 제목 (제목 테이블에 별도 INSERT 필요)
--   21 방 키를 잃어버려 임시 출입 요청하기        | 31 수강 과목을 지도교수와 상담하기
--   22 공용 냉장고 공간 정리하기                  | 32 결석한 수업 노트 부탁하기 (Marco)
--   23 세탁물이 뒤섞인 문제 해결하기              | 33 과제 제출 방식 선택하고 준비하기
--   24 세면대 누수 수리 요청하기                  | 34 식료품점에서 대체 재료 찾기
--   25 퇴실 점검과 보증금 확인하기                | 35 상황에 맞는 옷 추천받기
--   26 일정에 맞는 기차표 구매하기                | 36 가격과 특징을 비교해 선물 고르기
--   27 식당에서 메뉴 재료를 비교하고 주문하기     | 37 기능과 가격을 비교해 헤드폰 고르기
--   28 여행 일정에 맞는 박물관 패스 고르기        | 38 중고 자전거 상태 확인하고 가격 협상하기
--   29 날씨 때문에 야외 투어 일정 변경하기        | 39 불량 상품 교환 또는 환불 요청하기
--   30 공항에서 더 편한 좌석으로 변경하기         | 40 유심/요금제 구매하기
--
-- [참고 2] tts_voice_id (해당 테이블에서 지정 필요, 1=Chloe / 2=Marco / 3=Teddy(제3자, 곰)):
--   - scenario 22(공용 냉장고) = Marco(2), 23(세탁물) = Chloe(1),
--     32(결석 노트) = Marco(2) — 확정
--   - 나머지 신규 17개(직원/판매자/교수 역할) = 제3자(3)
--   - 전체 40개 배정은 scenario_q1_mapping.md 참고
--
-- [참고 3] category '쇼핑'(id=4) INSERT는 이 파일 0)섹션에 포함됨 (선행 처리 완료)
--          category id 시퀀스 사용 시 재조정 필요: setval('category_id_seq', 4) 등
-- [참고 4] USER 먼저 시나리오(21, 24, 26, 29, 39, 40)는 '사용자 시작 안내'
--          텍스트가 별도 테이블에 있다면 그쪽에도 INSERT 필요
-- =============================================================================

-- ---------------------------------------------------------------------------
-- id 명시 INSERT 이후 auto-increment 시퀀스를 재조정한다.
-- ---------------------------------------------------------------------------
SELECT setval(pg_get_serial_sequence('category', 'id'), (SELECT MAX(id) FROM category));
SELECT setval(pg_get_serial_sequence('category_language_variant', 'id'), (SELECT MAX(id) FROM category_language_variant));
SELECT setval(pg_get_serial_sequence('scenario', 'id'), (SELECT MAX(id) FROM scenario));
SELECT setval(pg_get_serial_sequence('scenario_question', 'id'), (SELECT MAX(id) FROM scenario_question));
SELECT setval(pg_get_serial_sequence('scenario_question_language_variant', 'id'), (SELECT MAX(id) FROM scenario_question_language_variant));
