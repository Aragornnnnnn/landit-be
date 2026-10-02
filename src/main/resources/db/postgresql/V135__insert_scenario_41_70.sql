-- 시나리오 41~70(30개)과 언어 변형, 레벨별 질문 270개, 질문 언어 변형 270개를 추가한다.
-- LAN-391. id = display_order = 41~70 고정 (썸네일 경로가 scenario id를 사용).
-- 캐릭터: chloe(기획서 41~47) / marco(51·52·53·55·57·58·59) / teddy(나머지 16개).
-- 레벨→응답 요구량: LEVEL_1=LOW, LEVEL_2_TO_3=MEDIUM, LEVEL_4_TO_5=HIGH.

-- scenario : 시나리오 41~70 (30행). id와 display_order를 41~70으로 동일하게 고정하고,
-- character_id로 대화 캐릭터를 지정한다.
DO $$
BEGIN
  IF EXISTS (
      SELECT 1
      FROM scenario
      WHERE id BETWEEN 41 AND 70
         OR display_order BETWEEN 41 AND 70
  ) THEN
    RAISE EXCEPTION 'LAN-603 requires unused scenario ids and display orders 41 through 70';
  END IF;
END $$;

INSERT INTO scenario
  (id, category_id, ai_role, character_id, difficulty, first_speaker, thumbnail_url,
   display_order, status, created_at, updated_at, total_question_count)
VALUES
  -- 기획서 항목 42 / DB scenario 41 / Day 41
  (41, 2, '한식이 처음이라 전부 물어보는 호기심 많은 친구', 'chloe', 'EASY', 'AI', 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/41/thumbnail/07646a08-6545-4d94-965c-320ccddcd284.webp', 41, 'ACTIVE', now(), now(), 3),
  -- 기획서 항목 51 / DB scenario 42 / Day 42
  (42, 1, '부탁을 흔쾌히 들어주는 자상한 룸메이트', 'marco', 'EASY', 'USER', 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/42/thumbnail/40dd627b-6391-4e01-972b-8550fc23ee3d.webp', 42, 'ACTIVE', now(), now(), 4),
  -- 기획서 항목 62 / DB scenario 43 / Day 43
  (43, 2, '영화·시간·좌석을 차례로 안내하는 매표소 직원', 'teddy', 'EASY', 'USER', 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/43/thumbnail/7739abca-d332-4e47-90d4-c0c9784eca00.webp', 43, 'ACTIVE', now(), now(), 4),
  -- 기획서 항목 41 / DB scenario 44 / Day 44
  (44, 3, '동아리 박람회에서 가입을 권유하는 활발한 테니스 동아리 부원', 'chloe', 'EASY', 'AI', 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/44/thumbnail/3fb574a2-2dc0-44e8-a15d-52158c3b827b.webp', 44, 'ACTIVE', now(), now(), 3),
  -- 기획서 항목 58 / DB scenario 45 / Day 45
  (45, 2, '같이 운동하자고 제안하는 의욕 넘치는 친구', 'marco', 'EASY', 'AI', 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/45/thumbnail/ebf3d122-452e-4f5d-911d-629ad5e4dbe8.webp', 45, 'ACTIVE', now(), now(), 3),
  -- 기획서 항목 54 / DB scenario 46 / Day 46
  (46, 2, '보관 방법을 친절히 안내하는 수하물 보관소 직원', 'teddy', 'EASY', 'USER', 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/46/thumbnail/e8565109-3c12-477b-b881-5d20f0b18bf2.webp', 46, 'ACTIVE', now(), now(), 4),
  -- 기획서 항목 43 / DB scenario 47 / Day 47
  (47, 1, '룸메이트의 서프라이즈 생일 파티를 계획하는 친구', 'chloe', 'EASY', 'AI', 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/47/thumbnail/d116847d-8167-4fdc-9cbb-60fbdf6034e0.webp', 47, 'ACTIVE', now(), now(), 3),
  -- 기획서 항목 57 / DB scenario 48 / Day 48
  (48, 2, '주말 피크닉을 제안하는 활동적인 친구', 'marco', 'EASY', 'AI', 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/48/thumbnail/1f6e3f92-4cef-4c13-8fad-6f7995b68d33.webp', 48, 'ACTIVE', now(), now(), 3),
  -- 기획서 항목 65 / DB scenario 49 / Day 49
  (49, 2, '펍 문화와 메뉴를 알려 주는 친근한 바텐더', 'teddy', 'EASY', 'USER', 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/49/thumbnail/cb58a9f3-ccbb-40ad-a6e4-f209566d086c.webp', 49, 'ACTIVE', now(), now(), 4),
  -- 기획서 항목 44 / DB scenario 50 / Day 50
  (50, 3, '중간고사를 망쳐 위로가 필요한 친구', 'chloe', 'NORMAL', 'AI', 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/50/thumbnail/87b607b1-3ac4-4928-919e-e6e95066dd81.webp', 50, 'ACTIVE', now(), now(), 3),
  -- 기획서 항목 61 / DB scenario 51 / Day 51
  (51, 4, '셀프 계산대 오류를 능숙하게 해결해 주는 마트 직원', 'teddy', 'EASY', 'USER', 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/51/thumbnail/4e82972d-64b6-4000-b49f-64cb4ab7c6c2.webp', 51, 'ACTIVE', now(), now(), 4),
  -- 기획서 항목 53 / DB scenario 52 / Day 52
  (52, 3, '프린터 오류의 대안을 함께 찾는 든든한 친구', 'marco', 'NORMAL', 'AI', 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/52/thumbnail/2e3a5b3a-e45b-4296-a49c-c05a54c739a5.webp', 52, 'ACTIVE', now(), now(), 3),
  -- 기획서 항목 66 / DB scenario 53 / Day 53
  (53, 2, '차를 추천하고 즐기는 법을 알려 주는 티룸 직원', 'teddy', 'EASY', 'USER', 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/53/thumbnail/c13621c5-69bf-4cf7-b9d7-f695cc14ea02.webp', 53, 'ACTIVE', now(), now(), 4),
  -- 기획서 항목 45 / DB scenario 54 / Day 54
  (54, 2, '방학 여행을 함께 계획하고 싶어 들뜬 친구', 'chloe', 'NORMAL', 'AI', 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/54/thumbnail/e55ce8b8-65fd-41b9-be34-509becbbee8b.webp', 54, 'ACTIVE', now(), now(), 3),
  -- 기획서 항목 49 / DB scenario 55 / Day 55
  (55, 2, '증상을 확인하고 예약을 잡아 주는 클리닉 접수 담당자', 'teddy', 'NORMAL', 'USER', 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/55/thumbnail/3e018097-df24-4985-8040-29f7709fb6d5.webp', 55, 'ACTIVE', now(), now(), 4),
  -- 기획서 항목 55 / DB scenario 56 / Day 56
  (56, 2, '잘못 온 배달 주문을 함께 해결하려는 룸메이트', 'marco', 'NORMAL', 'AI', 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/56/thumbnail/4a12f4b8-f6b0-45d9-a764-f3170daf045c.webp', 56, 'ACTIVE', now(), now(), 3),
  -- 기획서 항목 63 / DB scenario 57 / Day 57
  (57, 2, '좌석과 응원 문화를 알려 주는 경기장 안내원', 'teddy', 'NORMAL', 'USER', 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/57/thumbnail/5d2191ec-303a-480f-8aea-4c43ad1da9e3.webp', 57, 'ACTIVE', now(), now(), 4),
  -- 기획서 항목 47 / DB scenario 58 / Day 58
  (58, 3, '빌린 책을 깜빡한 것을 미안해하는 친구', 'chloe', 'HARD', 'USER', 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/58/thumbnail/32af1dfe-1cf8-4f50-ac15-c3c4c7ef1710.webp', 58, 'ACTIVE', now(), now(), 4),
  -- 기획서 항목 60 / DB scenario 59 / Day 59
  (59, 4, '쿠폰 조건을 확인하고 결제를 도와주는 계산대 직원', 'teddy', 'NORMAL', 'USER', 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/59/thumbnail/3e847c01-fe5a-4b46-8b53-77a3a34fe603.webp', 59, 'ACTIVE', now(), now(), 4),
  -- 기획서 항목 59 / DB scenario 60 / Day 60
  (60, 3, '사진 과제 촬영 계획을 세우자는 같은 조 친구', 'marco', 'NORMAL', 'AI', 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/60/thumbnail/d089a966-72d4-4ac7-9fe9-7df07601bc28.webp', 60, 'ACTIVE', now(), now(), 3),
  -- 기획서 항목 67 / DB scenario 61 / Day 61
  (61, 2, '원하는 스타일을 꼼꼼히 물어보는 미용사', 'teddy', 'NORMAL', 'AI', 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/61/thumbnail/79993888-d42a-40a2-8eb3-684159b3e6d6.webp', 61, 'ACTIVE', now(), now(), 3),
  -- 기획서 항목 52 / DB scenario 62 / Day 62
  (62, 2, '휴대폰 찾기를 차분하게 도와주는 침착한 친구', 'marco', 'HARD', 'AI', 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/62/thumbnail/75e0b35c-6564-40c2-a28c-978caa71bbe7.webp', 62, 'ACTIVE', now(), now(), 3),
  -- 기획서 항목 68 / DB scenario 63 / Day 63
  (63, 2, '퀴즈 나이트 참여를 권하는 유쾌한 진행자', 'teddy', 'NORMAL', 'AI', 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/63/thumbnail/b66dc67b-8e8d-40f6-ab01-f031b4a36d14.webp', 63, 'ACTIVE', now(), now(), 3),
  -- 기획서 항목 46 / DB scenario 64 / Day 64
  (64, 2, '페스티벌에 같이 가자고 조르는 신난 친구', 'chloe', 'NORMAL', 'AI', 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/64/thumbnail/dd6b819d-ecc6-4c52-b545-d0c04923d379.webp', 64, 'ACTIVE', now(), now(), 3),
  -- 기획서 항목 48 / DB scenario 65 / Day 65
  (65, 3, '지원자의 경험과 가능 시간을 확인하는 교내 카페 매니저', 'teddy', 'HARD', 'USER', 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/65/thumbnail/26719cc8-c027-431b-84f6-6a757d68b788.webp', 65, 'ACTIVE', now(), now(), 4),
  -- 기획서 항목 70 / DB scenario 66 / Day 66
  (66, 2, '체력과 날씨에 맞는 코스를 추천하는 방문자 센터 직원', 'teddy', 'NORMAL', 'USER', 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/66/thumbnail/52a37ba1-f946-483a-9e81-92d5a0907a0d.webp', 66, 'ACTIVE', now(), now(), 4),
  -- 기획서 항목 50 / DB scenario 67 / Day 67
  (67, 4, '계좌 종류와 필요 서류를 안내하는 은행 창구 직원', 'teddy', 'HARD', 'USER', 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/67/thumbnail/6e24ad93-be2d-46d7-80dd-efec53356641.webp', 67, 'ACTIVE', now(), now(), 4),
  -- 기획서 항목 64 / DB scenario 68 / Day 68
  (68, 4, '배송 방법과 보험을 안내하는 우체국 직원', 'teddy', 'NORMAL', 'USER', 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/68/thumbnail/b2bf79dc-80cd-4eb4-92e0-c535de6f61de.webp', 68, 'ACTIVE', now(), now(), 4),
  -- 기획서 항목 69 / DB scenario 69 / Day 69
  (69, 2, '요금 문제를 확인하고 환불해 주는 지하철 역무원', 'teddy', 'HARD', 'USER', 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/69/thumbnail/150b3110-a6d8-4f35-aa25-e0b738416257.webp', 69, 'ACTIVE', now(), now(), 4),
  -- 기획서 항목 56 / DB scenario 70 / Day 70
  (70, 2, '항공권 정보를 확인하고 수정해 주는 항공사 카운터 직원', 'teddy', 'HARD', 'USER', 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/70/thumbnail/27f52a8b-80bd-4141-add6-540a2a4b0f2f.webp', 70, 'ACTIVE', now(), now(), 4);

DO $$
BEGIN
  IF (
      SELECT COUNT(*)
      FROM scenario
      WHERE id BETWEEN 41 AND 70
        AND id = display_order
  ) <> 30 THEN
    RAISE EXCEPTION 'LAN-603 requires 30 scenarios with id equal to display_order';
  END IF;
END $$;

SELECT setval(
    pg_get_serial_sequence('scenario', 'id'),
    (SELECT MAX(id) FROM scenario)
);

-- scenario_language_variant : 시나리오 41~70 (30행).
-- scenario_id는 id와 display_order가 같은 41~70 값을 직접 사용한다.
-- 음성 캐릭터는 scenario.character_id가 담당한다 (V55에서 tts_voice_id 컬럼 삭제).
INSERT INTO scenario_language_variant
  (scenario_id, target_locale, base_locale, title, briefing,
   user_opening_instruction, conversation_goal, status, created_at, updated_at)
VALUES
  (44,
   'EN', 'KR', '동아리 부스에서 가입 권유 받기',
   '동아리 박람회 부스를 구경하는데 테니스 동아리 부원 Chloe가 다가와 가입을 권한다. 관심 있는 동아리를 말하고 활동 시간과 회비를 물어본 뒤 환영회에 갈지 답해야 한다.',
   NULL,
   '관심 있는 동아리 종류를 말하고, 활동 요일과 회비를 확인해 환영회 참석 여부를 정하기',
   'ACTIVE', now(), now()),
  (41,
   'EN', 'KR', '한식당에서 Chloe 메뉴 골라주기',
   '한식이 처음인 Chloe와 함께 한식당에 왔는데 Chloe가 메뉴판을 보고 어쩔 줄 몰라 한다. Chloe가 메뉴 추천부터 매운 정도, 먹는 방법까지 계속 물어본다.',
   NULL,
   'Chloe에게 메뉴를 추천하고, 맵지 않은 대안을 알려 주며, 먹는 방법까지 설명하기',
   'ACTIVE', now(), now()),
  (47,
   'EN', 'KR', 'Marco 몰래 생일 파티 준비하기',
   '다음 주가 룸메이트 Marco의 생일이다. Chloe가 깜짝 파티를 열자며 준비를 함께 하자고 제안한다.',
   NULL,
   '깜짝 파티를 할지 정하고, 케이크·장식·초대 역할을 나눠 Marco를 방으로 데려올 방법까지 합의하기',
   'ACTIVE', now(), now()),
  (50,
   'EN', 'KR', '시험 망친 Chloe 위로하기',
   '중간고사를 망친 Chloe가 속상한 얼굴로 찾아온다. Chloe의 이야기를 들어 주고 위로하며 기분 전환할 방법을 함께 찾아야 한다.',
   NULL,
   'Chloe의 시험 이야기를 듣고 공감하며, 비슷한 경험과 극복 방법을 나눠 기분 전환할 일을 정하기',
   'ACTIVE', now(), now()),
  (54,
   'EN', 'KR', 'Chloe와 방학 여행 계획 짜기',
   '곧 방학이라 Chloe가 함께 여행을 가자며 들떠서 말을 건다. 가고 싶은 곳과 예산, 일정과 숙소까지 함께 정해야 한다.',
   NULL,
   '가고 싶은 여행지를 정하고, 예산과 여행 일수, 숙소 종류를 합의하기',
   'ACTIVE', now(), now()),
  (64,
   'EN', 'KR', 'Chloe의 코첼라 제안에 답하기',
   '코첼라 티켓 예매가 열렸다며 Chloe가 잔뜩 신나서 같이 가자고 조른다. 갈지 답하고 주말 선택과 티켓값 분담, 숙소 방식까지 정해야 한다.',
   NULL,
   '코첼라에 함께 갈지 답하고, 갈 주말과 티켓값 분담 방식, 숙소나 이동 방법을 정하기',
   'ACTIVE', now(), now()),
  (58,
   'EN', 'KR', '빌려준 책 돌려받기',
   '한 달 전 Chloe에게 빌려준 전공 책이 시험 공부에 당장 필요하다. Chloe는 빌린 사실조차 잊은 눈치라 조심스럽게 먼저 말을 꺼내야 한다.',
   'Chloe에게 한 달 전 빌려준 전공 책이 시험 공부에 필요하다고 조심스럽게 말을 꺼내보세요.',
   '빌려준 책이 필요하다고 말하고, 언제까지 필요한지 알려 돌려받을 날짜와 방법을 정하기',
   'ACTIVE', now(), now()),
  (65,
   'EN', 'KR', '교내 카페 아르바이트 면접 보기',
   '교내 카페 아르바이트 면접을 보러 카페에 도착한다. 매니저 Teddy에게 먼저 인사하며 면접을 시작해야 한다.',
   '카페 매니저 Teddy에게 아르바이트 면접을 보러 왔다고 인사하세요.',
   '자기소개와 관련 경험을 말하고, 바쁜 시간대 대처 방법과 근무 가능 시간·시작 날짜를 정하기',
   'ACTIVE', now(), now()),
  (55,
   'EN', 'KR', '병원 전화 예약하고 증상 말하기',
   '며칠째 기침이 낫지 않아 클리닉에 전화를 건다. 접수 담당자 Teddy에게 진료 예약을 하고 싶다고 먼저 말해야 한다.',
   '클리닉 접수 담당자 Teddy에게 전화로 진료 예약을 하고 싶다고 말하세요.',
   '증상과 시작 시점을 설명하고, 진료 시간을 골라 보험 여부와 진료비를 확인하기',
   'ACTIVE', now(), now()),
  (67,
   'EN', 'KR', '은행 계좌 개설하기',
   '현지 생활에 쓸 은행 계좌가 필요해 은행 창구를 찾아간다. 창구 직원 Teddy에게 계좌를 개설하러 왔다고 먼저 말해야 한다.',
   '은행 창구 직원 Teddy에게 계좌를 개설하러 왔다고 말하세요.',
   '체류 목적과 기간, 계좌 용도를 설명하고, 필요한 서류와 체크카드 수령 방법을 정하기',
   'ACTIVE', now(), now()),
  (42,
   'EN', 'KR', 'Marco에게 택배 대신 받아 달라고 부탁하기',
   '수업 중에 택배가 도착할 예정인데 직접 받을 수가 없다. 룸메이트 Marco에게 상황을 설명하고 대신 받아 달라고 부탁해야 한다.',
   'Marco에게 수업 중 택배가 도착할 예정이라고 설명하고 대신 받아 달라고 부탁하세요.',
   '택배 도착 시간과 주의할 점을 알려 주고, 대리 수령이 되는지와 시간 안에 못 받을 때의 대안을 정하기',
   'ACTIVE', now(), now()),
  (62,
   'EN', 'KR', 'Marco와 잃어버린 휴대폰 찾기',
   '외출하고 돌아오는 길에 휴대폰이 사라진 것을 알아차린다. 침착한 Marco가 같이 찾아보자며 마지막으로 쓴 곳부터 되짚자고 한다.',
   NULL,
   '휴대폰을 마지막으로 쓴 곳과 추적 방법을 확인하고, 못 찾을 때의 대비와 계정·데이터 보호 방법을 정하기',
   'ACTIVE', now(), now()),
  (52,
   'EN', 'KR', 'Marco와 교내 프린터 오류 해결하기',
   '과제 제출 마감을 앞두고 교내 프린터에 용지 오류가 계속 뜬다. 같은 과제를 내야 하는 Marco가 함께 방법을 찾자고 한다.',
   NULL,
   '프린터 오류에 먼저 해볼 조치를 정하고, 옆 전산실 이용과 파일 전달 방법, 고장 신고까지 합의하기',
   'ACTIVE', now(), now()),
  (46,
   'EN', 'KR', '역에서 여행 가방 맡길 방법 찾기',
   '역의 보관함이 모두 차서 여행 가방을 맡길 곳이 없다. 수하물 보관소 직원 Teddy에게 상황을 설명하고 다른 방법을 물어봐야 한다.',
   '수하물 보관소 직원 Teddy에게 남은 보관함이 없다고 설명하고 가방을 맡길 방법을 물어보세요.',
   '가방 크기와 찾을 시간을 알려 주고, 대안 보관소 중 한 곳을 골라 수령 방법을 확인하기',
   'ACTIVE', now(), now()),
  (56,
   'EN', 'KR', 'Marco와 잘못 배달된 음식 주문 해결하기',
   '배달 음식을 열어 보니 주문한 것과 전혀 다른 메뉴가 들어 있다. Marco가 어떻게 할지 물으며 함께 해결하려 한다.',
   NULL,
   '잘못 온 주문을 어떻게 처리할지 정하고, 재배달과 환불 중 하나를 골라 그동안의 저녁까지 결정하기',
   'ACTIVE', now(), now()),
  (70,
   'EN', 'KR', '항공권 영문 이름 오류 수정 요청하기',
   '공항에서 항공권의 영문 이름이 여권과 다르게 적힌 것을 발견한다. 카운터 직원 Teddy에게 상황을 설명하고 수정을 요청해야 한다.',
   '항공사 카운터 직원 Teddy에게 항공권 이름이 여권과 다르다고 설명하고 수정을 요청하세요.',
   '목적지와 여권 이름을 정확히 전달하고, 재구매 안내를 들은 뒤 비즈니스석과 늦은 편 좌석 중 하나를 정하기',
   'ACTIVE', now(), now()),
  (48,
   'EN', 'KR', 'Marco와 피크닉 계획 세우기',
   '주말 날씨가 아주 좋다는 예보가 나왔다. Marco가 피크닉을 가자며 장소부터 정하자고 한다.',
   NULL,
   '피크닉 장소를 정하고, 각자 가져올 것을 나눠 비 올 때의 대안까지 정하기',
   'ACTIVE', now(), now()),
  (45,
   'EN', 'KR', 'Marco와 같이 할 운동 정하기',
   '수업이 끝나고 Marco가 같이 운동하자고 제안한다. 하고 싶은 운동과 목표를 말하고 만날 시간과 장소를 정해야 한다.',
   NULL,
   '같이 할 운동 종목을 정하고, 각자의 운동 목표와 만날 시간·장소를 합의하기',
   'ACTIVE', now(), now()),
  (60,
   'EN', 'KR', 'Marco와 사진 과제 촬영 계획 세우기',
   '사진 수업에서 좋아하는 풍경 사진 세 장을 조별로 제출해야 한다. 같은 조인 Marco가 촬영 계획을 같이 세우자고 한다.',
   NULL,
   '찍을 풍경의 종류를 정하고, 촬영하기 좋은 시간과 장소, 만날 약속까지 정하기',
   'ACTIVE', now(), now()),
  (59,
   'EN', 'KR', '할인 쿠폰이 적용되지 않아 확인 요청하기',
   '계산대에서 준비해 간 할인 쿠폰이 적용되지 않는다. 직원 Teddy에게 쿠폰이 안 된다고 설명하고 확인을 요청해야 한다.',
   '계산대 직원 Teddy에게 할인 쿠폰이 적용되지 않았다고 설명하고 확인을 요청하세요.',
   '쿠폰이 안 되는 이유를 확인하고, 상품 추가와 정가 결제 중 하나를 골라 적립·영수증 여부까지 정하기',
   'ACTIVE', now(), now()),
  (51,
   'EN', 'KR', '마트 셀프 계산대 오류 해결하기',
   '마트 셀프 계산대가 계산 도중에 멈춰 버린다. 지나가는 직원 Teddy를 불러 도움을 요청해야 한다.',
   '직원 Teddy에게 계산대가 멈췄다고 도움을 요청하세요. (예: ''기계가 멈췄어요'')',
   '계산대가 멈춘 상황을 설명하고, 과일 무게 스티커 문제를 해결해 남은 결제를 마무리하기',
   'ACTIVE', now(), now()),
  (43,
   'EN', 'KR', '영화관에서 티켓 사기',
   '영화관 매표소에서 줄을 서다 차례가 된다. 직원 Teddy에게 영화표를 사고 싶다고 먼저 말해야 한다.',
   '매표소 직원 Teddy에게 영화표를 사고 싶다고 말하세요. (예: ''영화표 살게요'')',
   '볼 영화를 고르고, 상영 시간과 좌석을 정해 스낵까지 주문하기',
   'ACTIVE', now(), now()),
  (57,
   'EN', 'KR', '축구 경기 직관하기',
   '처음 찾은 축구 경기장에서 자리를 찾지 못한다. 안내원 Teddy에게 티켓을 보여 주며 좌석 위치를 물어봐야 한다.',
   '안내원 Teddy에게 티켓을 보여주며 좌석 위치를 물어보세요. (예: ''제 자리 어디예요?'')',
   '좌석 위치를 확인하고, 응원할 팀과 현지 응원 방식을 물어 하프타임 간식까지 정하기',
   'ACTIVE', now(), now()),
  (68,
   'EN', 'KR', '우체국에서 한국으로 소포 보내기',
   '한국으로 선물 소포를 보내려고 우체국을 찾는다. 직원 Teddy에게 소포를 보내고 싶다고 먼저 말해야 한다.',
   '우체국 직원 Teddy에게 한국으로 소포를 보내고 싶다고 말하세요. (예: ''한국으로 소포 보내려고요'')',
   '소포 내용물과 주의할 점을 설명하고, 배송 방법과 보험 여부, 세관 신고 금액을 정하기',
   'ACTIVE', now(), now()),
  (49,
   'EN', 'KR', '펍에서 첫 주문하기',
   '현지 펍에 처음 들어와 어디서 주문해야 할지 모른다. 바텐더 Teddy에게 다가가 먼저 말을 걸어야 한다.',
   '펍 바텐더 Teddy에게 다가가 인사하세요. (예: ''안녕하세요, 여기서 주문하는 거 맞아요?'')',
   '평소 취향을 말해 맥주를 추천받고, 잔 크기와 음식 주문, 결제 방식을 정하기',
   'ACTIVE', now(), now()),
  (53,
   'EN', 'KR', '티룸에서 애프터눈 티 즐기기',
   '애프터눈 티를 즐기러 티룸에 자리를 잡는다. 직원 Teddy에게 애프터눈 티를 주문하고 싶다고 먼저 말해야 한다.',
   '티룸 직원 Teddy에게 애프터눈 티를 주문하고 싶다고 말하세요. (예: ''애프터눈 티 주문할게요'')',
   '차 취향을 말해 추천을 받고, 스콘 먹는 방법을 배워 리필할 차를 정하기',
   'ACTIVE', now(), now()),
  (61,
   'EN', 'KR', '미용실에서 머리 자르기',
   '예약해 둔 미용실에 도착해 거울 앞에 앉는다. 미용사 Teddy가 오늘 머리를 어떻게 할지 꼼꼼히 물어본다.',
   NULL,
   '원하는 머리 스타일을 설명하고, 자를 길이와 앞머리 손질 여부를 정해 마무리 수정까지 요청하기',
   'ACTIVE', now(), now()),
  (63,
   'EN', 'KR', '펍 퀴즈에 합류하기',
   '혼자 펍에 앉아 있는데 곧 퀴즈 나이트가 시작된다. 진행자 Teddy가 한 명이 부족한 팀에 합류하지 않겠냐고 권한다.',
   NULL,
   '퀴즈에 참가할지 답하고, 자신 있는 분야를 말해 참가비를 확인하고 팀 이름을 정하기',
   'ACTIVE', now(), now()),
  (69,
   'EN', 'KR', '지하철 개찰구 요금 문제 해결하기',
   '지하철 개찰구에서 카드가 열리지 않고 요금도 잘못 빠져나간 것 같다. 역무원 Teddy에게 상황을 설명하고 확인을 요청해야 한다.',
   '역무원 Teddy에게 카드가 안 되고 요금이 잘못 나온 것 같다고 말하세요. (예: ''카드가 안 돼요'')',
   '오늘 이동한 경로를 설명해 잘못 청구된 요금을 환불받고, 남은 일정에 맞는 패스가 필요한지 정하기',
   'ACTIVE', now(), now()),
  (66,
   'EN', 'KR', '국립공원에서 하이킹 코스 추천받기',
   '국립공원 방문자 센터에 들러 어느 코스로 갈지 정하려 한다. 직원 Teddy에게 걷기 좋은 하이킹 코스를 추천해 달라고 먼저 말해야 한다.',
   '방문자 센터 직원 Teddy에게 하이킹 코스를 추천해 달라고 말하세요. (예: ''걷기 좋은 코스 추천해줄래요?'')',
   '하이킹 경험과 걸을 수 있는 시간을 말하고, 날씨를 고려해 코스를 골라 준비물을 점검하기',
   'ACTIVE', now(), now());

-- scenario_question : 시나리오 30개 × 레벨 3종 × 질문 3개 = 270행.
-- scenario_question id 365~634가 비어 있어야 한다 (LAN-601 음원 경로가 이 id를 사용).
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM scenario_question WHERE id BETWEEN 365 AND 634) THEN
    RAISE EXCEPTION 'LAN-391 requires unused scenario_question ids 365 through 634';
  END IF;
END $$;

INSERT INTO scenario_question
  (id, scenario_id, display_order, question_level_group, response_demand, status, created_at, updated_at)
VALUES
  (365, 44, 1, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (366, 44, 2, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (367, 44, 3, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (368, 44, 1, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (369, 44, 2, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (370, 44, 3, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (371, 44, 1, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (372, 44, 2, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (373, 44, 3, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (374, 41, 1, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (375, 41, 2, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (376, 41, 3, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (377, 41, 1, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (378, 41, 2, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (379, 41, 3, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (380, 41, 1, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (381, 41, 2, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (382, 41, 3, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (383, 47, 1, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (384, 47, 2, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (385, 47, 3, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (386, 47, 1, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (387, 47, 2, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (388, 47, 3, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (389, 47, 1, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (390, 47, 2, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (391, 47, 3, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (392, 50, 1, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (393, 50, 2, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (394, 50, 3, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (395, 50, 1, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (396, 50, 2, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (397, 50, 3, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (398, 50, 1, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (399, 50, 2, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (400, 50, 3, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (401, 54, 1, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (402, 54, 2, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (403, 54, 3, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (404, 54, 1, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (405, 54, 2, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (406, 54, 3, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (407, 54, 1, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (408, 54, 2, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (409, 54, 3, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (410, 64, 1, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (411, 64, 2, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (412, 64, 3, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (413, 64, 1, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (414, 64, 2, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (415, 64, 3, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (416, 64, 1, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (417, 64, 2, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (418, 64, 3, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (419, 58, 1, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (420, 58, 2, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (421, 58, 3, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (422, 58, 1, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (423, 58, 2, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (424, 58, 3, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (425, 58, 1, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (426, 58, 2, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (427, 58, 3, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (428, 65, 1, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (429, 65, 2, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (430, 65, 3, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (431, 65, 1, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (432, 65, 2, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (433, 65, 3, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (434, 65, 1, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (435, 65, 2, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (436, 65, 3, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (437, 55, 1, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (438, 55, 2, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (439, 55, 3, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (440, 55, 1, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (441, 55, 2, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (442, 55, 3, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (443, 55, 1, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (444, 55, 2, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (445, 55, 3, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (446, 67, 1, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (447, 67, 2, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (448, 67, 3, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (449, 67, 1, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (450, 67, 2, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (451, 67, 3, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (452, 67, 1, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (453, 67, 2, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (454, 67, 3, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (455, 42, 1, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (456, 42, 2, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (457, 42, 3, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (458, 42, 1, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (459, 42, 2, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (460, 42, 3, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (461, 42, 1, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (462, 42, 2, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (463, 42, 3, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (464, 62, 1, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (465, 62, 2, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (466, 62, 3, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (467, 62, 1, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (468, 62, 2, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (469, 62, 3, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (470, 62, 1, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (471, 62, 2, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (472, 62, 3, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (473, 52, 1, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (474, 52, 2, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (475, 52, 3, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (476, 52, 1, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (477, 52, 2, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (478, 52, 3, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (479, 52, 1, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (480, 52, 2, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (481, 52, 3, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (482, 46, 1, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (483, 46, 2, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (484, 46, 3, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (485, 46, 1, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (486, 46, 2, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (487, 46, 3, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (488, 46, 1, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (489, 46, 2, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (490, 46, 3, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (491, 56, 1, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (492, 56, 2, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (493, 56, 3, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (494, 56, 1, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (495, 56, 2, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (496, 56, 3, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (497, 56, 1, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (498, 56, 2, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (499, 56, 3, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (500, 70, 1, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (501, 70, 2, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (502, 70, 3, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (503, 70, 1, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (504, 70, 2, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (505, 70, 3, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (506, 70, 1, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (507, 70, 2, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (508, 70, 3, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (509, 48, 1, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (510, 48, 2, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (511, 48, 3, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (512, 48, 1, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (513, 48, 2, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (514, 48, 3, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (515, 48, 1, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (516, 48, 2, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (517, 48, 3, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (518, 45, 1, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (519, 45, 2, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (520, 45, 3, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (521, 45, 1, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (522, 45, 2, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (523, 45, 3, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (524, 45, 1, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (525, 45, 2, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (526, 45, 3, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (527, 60, 1, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (528, 60, 2, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (529, 60, 3, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (530, 60, 1, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (531, 60, 2, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (532, 60, 3, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (533, 60, 1, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (534, 60, 2, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (535, 60, 3, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (536, 59, 1, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (537, 59, 2, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (538, 59, 3, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (539, 59, 1, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (540, 59, 2, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (541, 59, 3, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (542, 59, 1, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (543, 59, 2, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (544, 59, 3, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (545, 51, 1, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (546, 51, 2, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (547, 51, 3, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (548, 51, 1, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (549, 51, 2, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (550, 51, 3, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (551, 51, 1, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (552, 51, 2, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (553, 51, 3, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (554, 43, 1, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (555, 43, 2, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (556, 43, 3, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (557, 43, 1, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (558, 43, 2, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (559, 43, 3, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (560, 43, 1, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (561, 43, 2, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (562, 43, 3, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (563, 57, 1, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (564, 57, 2, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (565, 57, 3, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (566, 57, 1, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (567, 57, 2, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (568, 57, 3, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (569, 57, 1, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (570, 57, 2, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (571, 57, 3, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (572, 68, 1, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (573, 68, 2, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (574, 68, 3, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (575, 68, 1, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (576, 68, 2, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (577, 68, 3, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (578, 68, 1, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (579, 68, 2, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (580, 68, 3, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (581, 49, 1, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (582, 49, 2, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (583, 49, 3, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (584, 49, 1, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (585, 49, 2, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (586, 49, 3, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (587, 49, 1, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (588, 49, 2, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (589, 49, 3, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (590, 53, 1, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (591, 53, 2, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (592, 53, 3, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (593, 53, 1, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (594, 53, 2, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (595, 53, 3, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (596, 53, 1, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (597, 53, 2, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (598, 53, 3, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (599, 61, 1, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (600, 61, 2, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (601, 61, 3, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (602, 61, 1, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (603, 61, 2, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (604, 61, 3, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (605, 61, 1, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (606, 61, 2, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (607, 61, 3, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (608, 63, 1, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (609, 63, 2, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (610, 63, 3, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (611, 63, 1, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (612, 63, 2, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (613, 63, 3, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (614, 63, 1, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (615, 63, 2, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (616, 63, 3, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (617, 69, 1, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (618, 69, 2, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (619, 69, 3, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (620, 69, 1, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (621, 69, 2, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (622, 69, 3, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (623, 69, 1, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (624, 69, 2, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (625, 69, 3, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (626, 66, 1, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (627, 66, 2, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (628, 66, 3, 'LEVEL_1', 'LOW', 'ACTIVE', now(), now()),
  (629, 66, 1, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (630, 66, 2, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (631, 66, 3, 'LEVEL_2_TO_3', 'MEDIUM', 'ACTIVE', now(), now()),
  (632, 66, 1, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (633, 66, 2, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now()),
  (634, 66, 3, 'LEVEL_4_TO_5', 'HIGH', 'ACTIVE', now(), now());

SELECT setval(
    pg_get_serial_sequence('scenario_question', 'id'),
    (SELECT MAX(id) FROM scenario_question)
);

-- scenario_question_language_variant : 270행.
-- audio_url은 LAN-601 음원 manifest 기준이다
--   (docs/handoffs/lan-601-be-audio-urls.md, manifest baf2cb93…json).
-- required_response_element는 레벨 평가용 필수 응답 요소 (V90 컨벤션, 개행 구분).
-- inner_thought는 AI 선발화 시나리오의 Q1에만 넣는다 (기존 규칙).
INSERT INTO scenario_question_language_variant
  (scenario_question_id, target_locale, base_locale, question_text, question_translation,
   required_response_element, audio_url, status, created_at, updated_at, inner_thought, inner_thought_type)
VALUES
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 44 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 1),
   'EN', 'KR', 'Hi! What sport do you like?',
   '안녕! 무슨 운동 좋아해?',
   'State a sport you like.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/365/ba6837982fe2f05dcb22181459326eaecc4521d9d2c7b19ff1011a210baddf35.mp3', 'ACTIVE', now(), now(), '이 친구는 무슨 운동을 좋아할까? 일단 물어봐야지.', 'GOOD'),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 44 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 2),
   'EN', 'KR', 'We are the tennis club. Do you have a question?',
   '우리는 테니스 동아리야. 궁금한 거 있어?',
   'Ask a question about the club, or say you have none.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/366/8344463035d13e5100179d7c142770e9a1ae9df58386c542f28e913e92276832.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 44 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 3),
   'EN', 'KR', 'We have a party this Friday. Do you want to come?',
   '이번 주 금요일에 파티가 있어. 올래?',
   'Say whether you will come to the party.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/367/b6dd0c61e591c82879af433ee20fb64ae88ea16c1af43011e13e5dd3666ea0bf.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 44 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 1),
   'EN', 'KR', 'Hey! Are you planning to join a club this semester? What kind of clubs do you enjoy?',
   '안녕! 이번 학기에 동아리 들어갈 생각 있어? 어떤 종류의 동아리를 좋아해?',
   'Say whether you plan to join a club.
Describe what kind of clubs you enjoy.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/368/f512725f71a119deaabdc608de86c5a2b1a72459aa441f0c8c6f752cf7fa19f2.mp3', 'ACTIVE', now(), now(), '이번 학기 신입 부원을 꼭 채워야 하는데, 어떤 동아리를 좋아하는지부터 물어봐야지.', 'GOOD'),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 44 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 2),
   'EN', 'KR', 'We''re the tennis club. We play every Friday afternoon, and it costs five dollars a month. Is there anything you want to ask?',
   '우리는 테니스 동아리야. 매주 금요일 오후에 치고, 한 달에 5달러야. 궁금한 거 있어?',
   'Ask a question about the club, or say you have nothing to ask.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/369/0b3c5661f401ea7d3ac4577127b82f131fd5b784eb0af1d6e50075e4518da5a6.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 44 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 3),
   'EN', 'KR', 'We''re having a welcome party for new members this Friday. Would you like to join us?',
   '이번 주 금요일에 신입 환영회를 해. 우리랑 같이 갈래?',
   'Say whether you will join the welcome party.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/370/ec2f7a4b42f6d9dcd72b9ffa4ce1a6351bf6a33de10b0e4cfad6a1b2e629894e.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 44 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 1),
   'EN', 'KR', 'Hey! Are you looking to join a club this semester? What kind of clubs are you into?',
   '안녕! 이번 학기에 들고 싶은 동아리 있어? 어떤 쪽 동아리에 관심 있어?',
   'Say whether you are looking to join a club.
Describe what kind of clubs you are into.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/371/3224634dce5918a05e16efd28e400cd125bffb3e377666392387ea51d0b4d9db.mp3', 'ACTIVE', now(), now(), '관심 분야만 알면 우리 동아리가 딱이라고 설득할 수 있어. 어떤 쪽을 좋아하는지 들어보자.', 'GOOD'),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 44 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 2),
   'EN', 'KR', 'We''re the tennis club! We play every Friday afternoon, and the dues are five dollars a month — total beginners are welcome too. Anything you want to ask?',
   '우리는 테니스 동아리야! 매주 금요일 오후에 같이 치고, 회비는 한 달에 5달러야 — 완전 초보도 환영이야. 궁금한 거 있어?',
   'Ask a question about the club, or say you have nothing to ask.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/372/9314b28f0ceb0de5312417236e410013d0678608b55393fde57d3c819bb217ec.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 44 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 3),
   'EN', 'KR', 'We''re having a welcome party for new members this Friday — what do you say, wanna come?',
   '이번 주 금요일에 신입 환영회 하는데 — 어때, 올래?',
   'Say whether you will come to the welcome party.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/373/078e984f7e19b38c83f61432fd2aff8d3b03e92ebcc608d491f9d07bd3257db8.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 41 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 1),
   'EN', 'KR', 'There are so many foods! Which one is good?',
   '음식이 정말 많아! 어떤 게 맛있어?',
   'Name a dish that is good.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/374/6e1d504fa98d3582d8241f2007e8081ab62c71b001d900f01f539da17d0685d3.mp3', 'ACTIVE', now(), now(), '한국 음식은 처음이라 뭘 골라야 할지 모르겠어. 얘한테 물어보면 되겠다.', 'GOOD'),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 41 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 2),
   'EN', 'KR', 'I can''t eat spicy food. Is this one spicy?',
   '나 매운 거 못 먹어. 이건 매워?',
   'Say whether the dish is spicy.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/375/1c9b4d0ec7ab07e6cadce10be11c3cb1d79b66710ab969862ac68913c5f09e8e.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 41 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 3),
   'EN', 'KR', 'How do I eat this? Show me!',
   '이거 어떻게 먹어? 알려줘!',
   'Explain how to eat the dish.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/376/b095158f4c2c985a0df2b552369935cf166cc7ab545b5628ec170a698d86823f.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 41 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 1),
   'EN', 'KR', 'There are so many dishes here. Which one would you pick for me, and why?',
   '여기 메뉴가 정말 많다. 나한테는 어떤 걸 골라줄래? 왜 그거야?',
   'Choose a dish for the friend.
Explain why you picked it.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/377/4eb208c952a7d9bd8e5de0b9e3bbc4245c1f874202e654c1de14e93da4a62997.mp3', 'ACTIVE', now(), now(), '메뉴가 너무 많아서 혼자서는 못 고르겠어. 이 친구가 골라주면 실패는 없겠지.', 'GOOD'),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 41 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 2),
   'EN', 'KR', 'I''m not good with spicy food. Is this dish hot?',
   '나 매운 걸 잘 못 먹어. 이 요리 많이 매워?',
   'Say whether the dish is hot.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/378/f2f627dc9aff426e706eb9a9c916af92ce55facbdfc1f957b7541659c6108609.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 41 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 3),
   'EN', 'KR', 'How am I supposed to eat this? Do I mix everything, or eat each thing on its own?',
   '이건 어떻게 먹는 거야? 다 섞어서 먹어, 아니면 따로따로 먹어?',
   'Explain whether to mix everything or eat each thing separately.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/379/3a2e47508d3af7c58ff6c6facaba6ca068c8749e57863858afc336129b51fe53.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 41 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 1),
   'EN', 'KR', 'There are so many options! What''s the best thing here? You pick for me.',
   '메뉴가 너무 많아! 여기서 뭐가 제일 맛있어? 네가 골라줘.',
   'Recommend the best dish for the friend.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/380/c3a5c48ef8858db98fdfff3b5c944b425ab3b79b8864045de4d9b56c79d10e5c.mp3', 'ACTIVE', now(), now(), '이름만 봐서는 뭐가 뭔지 하나도 모르겠네. 여기서 제일 맛있는 걸로 추천받아야지.', 'GOOD'),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 41 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 2),
   'EN', 'KR', 'I''m not great with spicy food… is this one really hot? Recommend me something mild.',
   '나 매운 거 잘 못 먹는데… 이거 많이 매워? 안 매운 걸로 추천해줘.',
   'Say how spicy the dish is.
Recommend a mild dish.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/381/2c3eff5ac47d789c0cffff1fa0751beb3b2fe5c3ffb1b1a504a952d84a01e3bc.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 41 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 3),
   'EN', 'KR', 'Wait, how do I eat this? Am I supposed to mix everything together?',
   '잠깐, 이거 어떻게 먹는 거야? 다 섞어 먹는 거야?',
   'Explain whether to mix everything together.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/382/266b45cdbc44cd4e8b2cdb29f83cee412c585dead473c256b577f2fcce2fc122.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 47 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 1),
   'EN', 'KR', 'Next week is Marco''s birthday. Do you want a party?',
   '다음 주가 Marco 생일이야. 파티 해줄래?',
   'Say whether you want to have a party for Marco.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/383/9dc936474177cf7cf488d330e15d88d321595bb75437de435b3dcaa38367eb11.mp3', 'ACTIVE', now(), now(), '다음 주가 Marco 생일이야. 파티 해주면 진짜 좋아하겠지!', 'GOOD'),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 47 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 2),
   'EN', 'KR', 'We need a cake and balloons. Which one do you want?',
   '케이크랑 풍선이 필요해. 넌 어떤 걸 맡을래?',
   'Choose the cake or the balloons.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/384/345ff80113f7e8e0ff6da3c3bac049ba2033add13f5d49a50df0c6f2d94c6a10.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 47 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 3),
   'EN', 'KR', 'Marco can''t know! How do we bring him to the room?',
   'Marco가 알면 안 돼! 어떻게 방으로 데려오지?',
   'Suggest a way to bring Marco to the room.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/385/a4c5d9589ac8ce7117cc2508cb92ad67dad8d96d8c42b6e2e5f5a1f394dcfc86.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 47 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 1),
   'EN', 'KR', 'Marco''s birthday is next week. I want to plan a surprise party for him. What do you think?',
   '다음 주가 Marco 생일이야. 깜짝 파티를 준비하고 싶은데, 어떻게 생각해?',
   'Give your opinion on the surprise party plan.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/386/8592a97b007c65fba5f9ec01625a55f83febba707f69a69b5a64086cdfe5c3bd.mp3', 'ACTIVE', now(), now(), 'Marco 몰래 파티를 준비하려면 도와줄 사람이 필요해. 얘한테 먼저 말해봐야지.', 'GOOD'),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 47 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 2),
   'EN', 'KR', 'We need a cake, some decorations, and someone to invite his friends. Which part can you take?',
   '케이크, 장식, 친구 초대가 필요해. 넌 어떤 걸 맡을 수 있어?',
   'Choose a part you can take.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/387/0fc201059ca6b6d7f33e5c807d5e2fafb65f0fa1a24432047df2cd5ae935595c.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 47 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 3),
   'EN', 'KR', 'How can we get Marco to the room without making him suspicious? Do you have any ideas?',
   'Marco가 눈치채지 않게 어떻게 방으로 데려올까? 좋은 생각 있어?',
   'Suggest a way to get Marco to the room without making him suspicious.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/388/2814bc780698d77851750af27a23620ee777ad2abbe44c79eff65d322ecb52ed.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 47 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 1),
   'EN', 'KR', 'It''s Marco''s birthday next week — let''s throw him a surprise party! What do you think?',
   '다음 주 Marco 생일이잖아 — 서프라이즈 파티 해주자! 어때?',
   'Give your opinion on throwing a surprise party.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/389/39eb5fb23c8c805048fa908563296ed082df5935bc24c974a95edd1815b6e443.mp3', 'ACTIVE', now(), now(), 'Marco가 요즘 지쳐 보였으니까 서프라이즈 파티면 진짜 좋아할 거야. 같이 하자고 설득해봐야지.', 'GOOD'),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 47 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 2),
   'EN', 'KR', 'So we need a cake, decorations, and someone to invite his friends. Which one do you want to take?',
   '케이크, 장식, 친구 초대가 필요해. 넌 어떤 거 맡을래?',
   'Choose the part you will take.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/390/9eee13462757900055089dbf3d154dd581c6a5a1d902232502902b9acafbea10.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 47 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 3),
   'EN', 'KR', 'Okay, last thing — how do we get Marco to the room without him noticing anything?',
   '좋아, 마지막으로 — Marco가 눈치 못 채게 어떻게 방으로 데려오지?',
   'Suggest a way to get Marco to the room without him noticing.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/391/8508bb8bd20c28bc777ec612e3414a518ad8114861deba88673e0892cec9fd0c.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 50 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 1),
   'EN', 'KR', 'I did very badly on my test. I feel so sad.',
   '나 시험을 정말 못 봤어. 너무 속상해.',
   'Comfort the friend about the test.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/392/6e5fce578dc7b3a7ef1bfcb2d092c431791ddaf43a96350e858265a28550e166.mp3', 'ACTIVE', now(), now(), '시험 얘기를 꺼내면 더 속상하겠지만, 그래도 누구한테든 말하고 싶어.', 'NORMAL'),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 50 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 2),
   'EN', 'KR', 'Did you ever do badly, too? Tell me more.',
   '너도 그런 적 있어? 더 얘기해 줘.',
   'Say whether you have done badly too.
Tell more about that experience.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/393/13d821a57258670a9e07af14c0a683005e8e72b24db7d480efe98cdc9176f99d.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 50 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 3),
   'EN', 'KR', 'I want to feel happy again. What can we do today?',
   '다시 기분이 좋아지고 싶어. 우리 오늘 뭐 할까?',
   'Suggest something to do today.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/394/4afe8c8607dbdee40bc983140c80848aee21a5b5ee2849b33de4fb1da68eb844.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 50 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 1),
   'EN', 'KR', 'I did terribly on my midterm. Almost nothing I studied was on the test.',
   '나 중간고사 정말 못 봤어. 공부한 게 시험에 거의 안 나왔어.',
   'Comfort the friend about the midterm.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/395/d0e6095c963ff385c4d336b6dbc25204d329b5231fa7b964f89ba1a89f176dfd.mp3', 'ACTIVE', now(), now(), '공부한 게 하나도 안 나왔다니 너무 억울해. 이 친구한테라도 털어놔야 마음이 좀 풀리겠지.', 'NORMAL'),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 50 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 2),
   'EN', 'KR', 'Have you ever done badly on an important test? How did you deal with it?',
   '너도 중요한 시험을 망쳐본 적 있어? 그때 어떻게 했어?',
   'Say whether you have done badly on an important test.
Explain how you dealt with it.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/396/156e8f76a454bd8350a03f1fa8bac8b3ea4d3586d7de7d545494da2e4c0953f2.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 50 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 3),
   'EN', 'KR', 'I want to forget about it for a while. What should we do together this afternoon?',
   '잠깐 잊어버리고 싶어. 오늘 오후에 우리 같이 뭐 할까?',
   'Suggest what to do together this afternoon.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/397/7f84427a6c4db6e68540ec812f3898ad7b5574922be49df2a0450eb3e59f8aa3.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 50 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 1),
   'EN', 'KR', 'I totally bombed my midterm… none of the stuff I studied was on it.',
   '나 중간고사 완전 망쳤어… 공부한 데서 하나도 안 나왔어.',
   'Comfort the friend about the midterm.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/398/7761de0c026ac6e8b89ccd922ccd6afec235e18db467cc9c6f43b34b99747f08.mp3', 'ACTIVE', now(), now(), '혼자 있으면 계속 곱씹기만 할 것 같아. 먼저 얘기를 꺼내서 마음을 좀 덜어내야지.', 'NORMAL'),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 50 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 2),
   'EN', 'KR', 'Have you ever bombed a test? What did you do to get over it?',
   '너도 시험 망쳐본 적 있어? 그럴 땐 어떻게 극복했어?',
   'Say whether you have bombed a test.
Explain what you did to get over it.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/399/66088380c0bb07b7eef5081ff70c0bbd097c6f084f4b02be6b3da3a8b7bdbc9f.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 50 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 3),
   'EN', 'KR', 'Oh man, I need to clear my head. What should we do to cheer me up?',
   '아 진짜, 기분 전환이 필요해. 우리 뭐 하면서 기분 풀까?',
   'Suggest something to cheer the friend up.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/400/0080877a290ea8c2267fdd6cd6b5b9d9fb9e35537b4d843fb29ac885391234da.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 54 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 1),
   'EN', 'KR', 'Let''s travel this vacation! Where do you want to go?',
   '방학에 여행 가자! 어디에 가고 싶어?',
   'State where you want to go.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/401/b111757611fd7fd668f195d0cbdc8bef6c7d0ded45d797cbe49550e15ad3e74f.mp3', 'ACTIVE', now(), now(), '방학이 코앞이야. 같이 여행 가자고 하면 좋아하겠지?', 'GOOD'),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 54 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 2),
   'EN', 'KR', 'I want to save money. How much can we use?',
   '나 돈을 아끼고 싶어. 우리 얼마나 쓸 수 있을까?',
   'Suggest how much money to use.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/402/d5371d8bfa91ef05714a22b3cab31c234da1930e7aabf80e2a8a38165c4ada66.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 54 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 3),
   'EN', 'KR', 'We need a room too. How many days should we go?',
   '방도 잡아야 해. 우리 며칠 갈까?',
   'Suggest how many days to travel.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/403/fc70d600d10d9a6da34c218d8cf26abbb94f8a367749c9eabde18408295a2f9e.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 54 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 1),
   'EN', 'KR', 'Let''s travel together this vacation! Where would you like to go, and why there?',
   '이번 방학에 같이 여행 가자! 어디에 가고 싶어? 왜 그곳이야?',
   'State where you would like to go.
Explain why there.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/404/4a14d316f9d43765588ad3297da810abf5bb82ffa100a4848121187f3fad4b58.mp3', 'ACTIVE', now(), now(), '이번 방학엔 꼭 어디든 떠나고 싶어. 얘가 가고 싶은 데부터 들어봐야지.', 'GOOD'),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 54 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 2),
   'EN', 'KR', 'How much money should we plan for? I''m trying not to use too much this time.',
   '돈은 얼마 정도로 잡을까? 나는 이번엔 너무 많이 쓰지 않으려고 해.',
   'Suggest how much money to plan for.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/405/da939fe28a0e0b5806f3e091152f81a1c1fa54391e432d7173fbf7c87309ac2b.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 54 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 3),
   'EN', 'KR', 'How many days should we stay? And would a hostel be okay for you?',
   '며칠 정도 머물까? 그리고 숙소는 호스텔도 괜찮아?',
   'Suggest how many days to stay.
Say whether a hostel is okay.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/406/c34e1db27eb7c2dba5795b4e2423c58b89ca28fc889de6ea1c841ca9e81bfec8.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 54 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 1),
   'EN', 'KR', 'Let''s go on a trip together this break! Where do you wanna go?',
   '방학에 같이 여행 가자! 어디 가고 싶어?',
   'State where you want to go.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/407/6369f6f2756d5f6c97cc958da972d1a877c4b705d1e183cbd1d356972c45ea62.mp3', 'ACTIVE', now(), now(), '혼자 가는 것보다 같이 가면 훨씬 재밌을 거야. 먼저 어디 가고 싶은지 물어보자.', 'GOOD'),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 54 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 2),
   'EN', 'KR', 'How much are you thinking for the budget? I kind of need to save this time.',
   '예산은 얼마 정도 생각해? 나 이번엔 좀 아껴야 하거든.',
   'Suggest a budget amount.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/408/790d20598c5df9a904c812a294442a0224790f09a87a8f8a19b1c392b7357551.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 54 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 3),
   'EN', 'KR', 'How many days should we go for? And is a hostel okay with you?',
   '며칠 다녀올까? 숙소는 호스텔도 괜찮아?',
   'Suggest how many days to go for.
Say whether a hostel is okay.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/409/4078eddee463a489102fdd04a38e54f099788c2449b8804b7553ff13f8176af3.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 64 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 1),
   'EN', 'KR', 'Coachella tickets are on sale! Do you want to go with me?',
   '코첼라 티켓 예매 시작했대! 나랑 같이 갈래?',
   'Say whether you want to go to Coachella.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/410/b691135ec460a95c06bc6bfd5dd38545a680e5b4ee0ce8e15a73bbeefa6d6e07.mp3', 'ACTIVE', now(), now(), '코첼라 티켓이 드디어 풀렸어! 같이 갈 사람은 얘밖에 없지.', 'GOOD'),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 64 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 2),
   'EN', 'KR', 'Tickets are 400 dollars. Is the weekend in two weeks okay?',
   '티켓은 400달러야. 2주 뒤 주말은 괜찮아?',
   'Say whether the weekend in two weeks is okay.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/411/79e20c261c18d50335f42bc716768ce70fec752574673188e692d48b9492711e.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 64 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 3),
   'EN', 'KR', 'Do you want to camp or stay in a hotel?',
   '캠핑할래, 호텔에서 잘래?',
   'Choose camping or a hotel.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/412/2208d723286241a0d465005759061e7fccfadb2b3d643346f5d6d9af2dad942b.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 64 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 1),
   'EN', 'KR', 'Coachella tickets just went on sale! Do you want to go together?',
   '코첼라 티켓 예매 열렸대! 우리 같이 갈래?',
   'Say whether you want to go together.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/413/b345922a18896d34a627bd30009dbfd523db7d9317979b91ab9dc1b5cd6b5bf4.mp3', 'ACTIVE', now(), now(), '예매가 열렸어, 이건 놓치면 안 돼. 얘도 같이 간다고 해주면 좋겠다.', 'GOOD'),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 64 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 2),
   'EN', 'KR', 'Which weekend works better for you, One or Two? And how should we pay for them?',
   'Weekend 1이랑 2 중에 어느 쪽이 더 괜찮아? 그리고 티켓값은 어떻게 낼까?',
   'Choose Weekend One or Two.
Suggest how to pay for the tickets.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/414/479eceb955c303def41f26c5b80ba105924f2a9c201a912939703aa8b8a6a04d.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 64 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 3),
   'EN', 'KR', 'For a place to stay, would you rather camp or find a hotel nearby?',
   '숙소는 캠핑이 좋아, 아니면 근처 호텔을 알아볼까?',
   'Choose camping or a hotel nearby.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/415/bdb5eb8fa9958a0bd1d22fa0cd4932091651c11e7aa4deb2bd7c57c056a13994.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 64 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 1),
   'EN', 'KR', 'Coachella tickets just went on sale!! We have to go together!',
   '코첼라 티켓 예매 열렸대!! 우리 무조건 같이 가자!',
   'Respond to the invitation to go together.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/416/55904cfcd77f677fc06af726210a98c80e2ec6758b782607f702f37cf11a6909.mp3', 'ACTIVE', now(), now(), '드디어 예매가 열렸어, 올해는 무조건 간다! 얘를 꼭 데려가야지.', 'GOOD'),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 64 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 2),
   'EN', 'KR', 'Which weekend works for you, One or Two? Let''s split the tickets when we book.',
   'Weekend 1이랑 2 중에 언제 돼? 예매할 때 티켓값은 나눠 내자.',
   'Choose the weekend that works for you.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/417/6df495828210b9d8f4c957f6240cb680cf3bab3c80e5d2569b2010f9dcca9e7a.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 64 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 3),
   'EN', 'KR', 'For the stay — camping or a hotel nearby? Oh, or we could just take the early morning bus.',
   '숙소는 캠핑이랑 근처 호텔 중에 뭐가 좋아? 아, 아니면 그냥 새벽 버스 타고 가는 방법도 있어.',
   'Choose camping, a hotel, or the early morning bus.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/418/7dda176e6083d99244ee33d0ca03daf5cb3e69586167d55fd1003cac41d7e5c3.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 58 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 1),
   'EN', 'KR', 'Wait, did I borrow a book from you?',
   '잠깐, 내가 너한테 책을 빌렸었나?',
   'Confirm that you lent the friend a book.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/419/a421f3e7a5cbd7ccc71e4bd2e9d8e68a47dfd09c89dc642535f96b2709a69951.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 58 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 2),
   'EN', 'KR', 'Oh no, I forgot! Can you wait a few days?',
   '헐, 깜빡했어! 며칠만 기다려 줄 수 있어?',
   'Say whether you can wait a few days.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/420/fe8d70d6e5a15f27292a943efc4131f0bf3300eecc05d3b17fcfb5ff0b9a3dea.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 58 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 3),
   'EN', 'KR', 'I can bring it tomorrow. When do you need it?',
   '내일 갖다줄 수 있어. 언제 필요해?',
   'State when you need the book.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/421/2cff5c890dfb7ce8e43a04ef6561263dfcdc9404d241cf37bafb2d70af0ad47c.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 58 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 1),
   'EN', 'KR', 'Wait, did I borrow something from you? Which book was it?',
   '어? 내가 너한테 뭐 빌렸었나? 어떤 책이었어?',
   'Confirm the friend borrowed something.
Name the book.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/422/3d35bd77d7d1bc05f72a2adcd028d88e604742582ac189e596e4fa357f23e52c.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 58 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 2),
   'EN', 'KR', 'I''m really sorry, I completely forgot! But it''s not in my room right now. Can it wait a few days?',
   '진짜 미안해, 완전 잊고 있었어! 근데 지금 방에 없어. 며칠 뒤에 줘도 괜찮아?',
   'Say whether the book can wait a few days.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/423/2fd7cf35f97908a30effeba5988ffd07e99b40125d04250216b92d6ba415dbba.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 58 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 3),
   'EN', 'KR', 'When do you need it by? I''ll buy you a coffee and bring the book after class tomorrow. Is that okay?',
   '언제까지 필요해? 미안하니까 커피 사고, 책은 내일 수업 끝나고 갖다줄게. 괜찮아?',
   'Say when you need the book by.
Respond to the coffee-and-return plan.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/424/4fd1a946ea17a66789a096aa4c8736f51892aa5be67d89af744e7829332f3df0.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 58 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 1),
   'EN', 'KR', 'Wait, did I borrow something from you? Which book was it?',
   '어? 내가 너한테 뭐 빌렸었나? 어떤 책이더라?',
   'Confirm the friend borrowed something.
Name the book.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/425/3d35bd77d7d1bc05f72a2adcd028d88e604742582ac189e596e4fa357f23e52c.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 58 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 2),
   'EN', 'KR', 'Oh no, I''m so sorry — I completely forgot! But I don''t think it''s in my room right now…',
   '헐, 진짜 미안 — 완전 잊고 있었어! 근데 지금 그 책이 방에 없는 것 같은데…',
   'React to the news and say you still need the book.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/426/be9171785917a78e973b8e8a4f21115bc33b64772c57bd9d5f4e4da0d6f500ac.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 58 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 3),
   'EN', 'KR', 'When do you need it by? Tell you what — I''ll buy you a coffee, and I''ll bring the book after class tomorrow. Would that work?',
   '언제까지 필요해? 이렇게 하자 — 미안하니까 커피 살게, 책은 내일 수업 끝나고 갖다줄게. 괜찮아?',
   'Say when you need the book by.
Respond to the proposed plan.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/427/a514154db1793687d7f074c084cd2ed5cf993be7d911bf1fff9226c4f905e823.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 65 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 1),
   'EN', 'KR', 'Hi, can you tell me about yourself?',
   '안녕하세요, 자기소개 좀 해주시겠어요?',
   'Introduce yourself briefly.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/428/b93bb049e217ce38fcf1c348e1ade6b3c00744162c782d8bb65422dec1727ed9.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 65 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 2),
   'EN', 'KR', 'Did you work at a cafe before?',
   '전에 카페에서 일해본 적 있어요?',
   'Say whether you worked at a cafe before.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/429/88d3c9c04714d2c69499b1a4cd7c193d2ac65b08b12fc825cc3119af7c90c7d7.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 65 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 3),
   'EN', 'KR', 'How many days can you work? Tell me your start day.',
   '며칠 일할 수 있어요? 시작할 날짜도 알려주세요.',
   'State how many days you can work.
State your start day.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/430/7897b2a76cc3cf21c5feda931a37434fcfd7dff470e6bacb0673e23416517526.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 65 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 1),
   'EN', 'KR', 'Hi, you must be here for the interview. Could you tell me a little about yourself?',
   '안녕하세요, 면접 보러 오신 분이죠? 간단히 본인 소개를 해주시겠어요?',
   'Introduce yourself briefly.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/431/6d77a0c2b7438d54f8ecffa9f01d9f50022c043a0d92de105117fb0e2b1e65c3.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 65 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 2),
   'EN', 'KR', 'Have you worked in a cafe or in customer service? What would you do when the shop gets really busy?',
   '카페나 서비스직에서 일해본 적 있어요? 가게가 정말 바빠지면 어떻게 하시겠어요?',
   'Say whether you have cafe or customer service experience.
Explain what you would do when the shop gets busy.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/432/b05141d7fc6ad3b20dec4626ce49ed521f0526d8ec76e910333a444de8173a10.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 65 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 3),
   'EN', 'KR', 'How many hours a week can you work? The pay starts at twelve dollars an hour, and when could you start?',
   '일주일에 몇 시간 일할 수 있어요? 시급은 12달러부터인데, 언제부터 일할 수 있어요?',
   'State how many hours a week you can work.
State when you could start.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/433/080d11064f3e5ac957972057d02f2c411d91c47da761ffb861f43012b9fe0e5f.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 65 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 1),
   'EN', 'KR', 'Hi, you must be here for the interview. Could you introduce yourself briefly?',
   '안녕하세요, 면접 보러 오신 분이죠? 간단히 자기소개해 주시겠어요?',
   'Introduce yourself briefly.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/434/12b261e2ddd590f601b089f50670588ab3f49d57820b1dcb1c566ccad1e15069.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 65 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 2),
   'EN', 'KR', 'Have you worked in a cafe or in customer service before? And when it gets really busy during rush hour, how would you deal with it?',
   '카페나 서비스직에서 일해본 경험 있어요? 그리고 러시 시간에 손님이 확 몰리면 어떻게 대처하실 건가요?',
   'Say whether you have cafe or customer service experience.
Explain how you would handle rush hour.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/435/a0ff8b0b019509cb540ca37e0d3cc57e25aa568c4dd5f69239ba4a53a9646919.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 65 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 3),
   'EN', 'KR', 'How many hours a week can you do? The pay starts at twelve dollars an hour — when could you start?',
   '일주일에 몇 시간 가능해요? 시급은 12달러부터인데 — 언제부터 일할 수 있어요?',
   'State how many hours a week you can work.
State when you could start.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/436/b19eccefa3069222a65959c4c252146786e866dd124024acc1a066168a83e063.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 55 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 1),
   'EN', 'KR', 'Hello, this is the clinic. What is the problem?',
   '여보세요, 클리닉입니다. 어디가 안 좋으세요?',
   'Describe your symptom.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/437/3ead5aef621a7d89864b3d2b02cc5f9cc4132c42a3128b3751b835348b6c3936.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 55 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 2),
   'EN', 'KR', 'How many days, and do you have a fever?',
   '며칠 됐고, 열도 있으세요?',
   'State how many days it has lasted.
Say whether you have a fever.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/438/5d564f623307b7f8f06afa6cf841a90ef8e8b2141dad2f5c1d101c425b18d639.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 55 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 3),
   'EN', 'KR', 'We have today or tomorrow. What time is good?',
   '오늘이나 내일 가능해요. 몇 시가 좋으세요?',
   'Choose a time for the visit.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/439/345d3fb58badd9cd2dcaa9f27d47fbe7fa1a6ef40fac60087d84c16f98e63091.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 55 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 1),
   'EN', 'KR', 'Thank you for calling the clinic. What kind of symptoms are you having?',
   '네, 클리닉에 전화 주셔서 감사합니다. 어떤 증상이 있으세요?',
   'Describe your symptoms.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/440/76b69a1be21ffbffa9a92c901b8741a0b781541e7862f3d2fea117fb0e03d39c.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 55 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 2),
   'EN', 'KR', 'When did it start, and has it changed at all since then?',
   '언제부터 그러셨어요? 그때랑 비교해서 달라진 게 있나요?',
   'State when it started.
Say whether it has changed since then.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/441/55733fd97cec7b327de7183ac33d5b6b969185d774a8467c92ca5eacbb67ba4c.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 55 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 3),
   'EN', 'KR', 'I have an opening today at four or tomorrow at ten — which one works better for you? And do you have insurance?',
   '오늘 4시나 내일 10시가 비는데, 어느 쪽이 더 편하세요? 그리고 보험은 있으세요?',
   'Choose one of the offered times.
Say whether you have insurance.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/442/7f2ec1be76fdb44a41badc6d1d3e06a80bc378498e9713f980c62ddc9cbd5b54.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 55 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 1),
   'EN', 'KR', 'Thanks for calling the clinic. What symptoms are you having?',
   '네, 클리닉입니다. 어떤 증상 때문에 전화 주셨어요?',
   'Describe your symptoms.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/443/5323fc3d9fb87d96f94043d16f78172311f2a2254b23a1da54998f23721c92b7.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 55 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 2),
   'EN', 'KR', 'When did it start? Any fever along with the cough?',
   '언제부터 그러셨어요? 기침이랑 같이 열도 있으세요?',
   'State when it started.
Say whether you have a fever.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/444/e498e5d78cc40eff0e5bba8e7ef534ad536f3f205208ae6822c41b949f165099.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 55 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 3),
   'EN', 'KR', 'We have an opening today at four or tomorrow at ten — which works for you? And do you have insurance? Without it, the visit is sixty dollars.',
   '오늘 4시나 내일 오전 10시가 비어요 — 언제로 잡아드릴까요? 그리고 보험 있으세요? 없으시면 진료비는 60달러예요.',
   'Choose one of the offered times.
Say whether you have insurance.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/445/ebde8eca3d2f3a8c33a79e160e481a19bb0969365556d54502f59f18c0a2b20c.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 67 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 1),
   'EN', 'KR', 'Welcome! Do you study or work here?',
   '어서 오세요! 여기서 공부하세요, 아니면 일하세요?',
   'Say whether you study or work here.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/446/f8bf64b2ccbc875cb075736d3c5d7aa0a5c9b9082a695971d3c0aa5851317ef4.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 67 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 2),
   'EN', 'KR', 'What is this account for?',
   '이 계좌는 어디에 쓰실 거예요?',
   'State what the account is for.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/447/f8a8387eb6bc01ecb378e00851f8d740e4b8d7d19c88159f18056144e9365cca.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 67 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 3),
   'EN', 'KR', 'I need your passport. When do you need your card?',
   '여권이 필요해요. 카드는 언제까지 필요하세요?',
   'State when you need your card.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/448/e581fd5e8c528c60602412acc833b7d4a323872d9160f0c509b21e5cc8f0f312.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 67 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 1),
   'EN', 'KR', 'Welcome! Are you here for work or for study, and how long are you planning to stay?',
   '어서 오세요! 일 때문에 오셨어요, 공부 때문에 오셨어요? 그리고 얼마나 머무를 계획이세요?',
   'Say whether you are here for work or study.
State how long you plan to stay.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/449/83fc140feaa0f4b18aada8fb3fe7f2cf9d51e989ca8a6f2b8b6396879aafa701.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 67 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 2),
   'EN', 'KR', 'What will you mainly use the account for? I''ll recommend the one that fits you best.',
   '계좌를 주로 어디에 쓰실 건가요? 거기에 제일 맞는 걸로 추천해드릴게요.',
   'State what you will mainly use the account for.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/450/23ff3fe5ef449d4a154451089e6a186309f70780db892e52c1a8a6784a75deb0.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 67 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 3),
   'EN', 'KR', 'I''ll need your passport and proof of address — do you have those with you? And when do you need your card by?',
   '여권이랑 거주 증명이 필요한데, 지금 갖고 계세요? 그리고 카드는 언제까지 필요하세요?',
   'Say whether you have the passport and proof of address.
State when you need your card by.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/451/87c4f102047e7499f40ae21d67180e63b7225eadb0a126c4c165c53b9e564496.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 67 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 1),
   'EN', 'KR', 'Welcome! Opening an account, I see. May I ask what brings you here — the purpose and length of your stay?',
   '어서 오세요! 계좌 개설이시군요. 어떤 일로 오셨는지 — 체류 목적과 기간을 여쭤봐도 될까요?',
   'State the purpose and length of your stay.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/452/111c862228e912dd8f8687ffe9eb92b3f0daabcf2ef36d57bd62082dbafad2a3.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 67 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 2),
   'EN', 'KR', 'What will you mainly be using the account for? I''ll recommend the one that fits you best.',
   '계좌는 주로 어떤 용도로 쓰실 건가요? 거기에 제일 맞는 걸로 추천해드릴게요.',
   'State what you will mainly use the account for.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/453/fd35bbf6e41460453b3e0a80e5f126d87a790435bfa769afb1ba0bf21b9c58fb.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 67 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 3),
   'EN', 'KR', 'I''ll need your passport, student ID, and proof of address — do you have them with you? Also, when do you need your debit card by? I''ll find the best way to get it to you.',
   '여권, 학생증, 거주 증명이 필요한데 지금 갖고 계세요? 그리고 체크카드는 언제까지 필요하세요? 제일 맞는 수령 방법을 찾아드릴게요.',
   'Say whether you have the required documents.
State when you need your debit card by.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/454/6fe6daf791f5d615764b257d744a552ad3b4d72ccebe1dae7a428c4328ba79fc.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 42 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 1),
   'EN', 'KR', 'Okay, I can carry it. What time will it come?',
   '알았어, 내가 들어다 줄게. 몇 시에 와?',
   'State what time the package will come.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/455/937c9e669ce381d2498db19ea541a4f2da791ca4b654ef2bff73845439bc63f0.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 42 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 2),
   'EN', 'KR', 'Can I take it and sign your name?',
   '내가 받아서 네 이름으로 사인해도 돼?',
   'Say whether Marco can take it and sign your name.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/456/8b0b1c9a8f7401de4db9f40ad07e98fd2e8297f7d329d7aa5d5b0a3a424f7526.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 42 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 3),
   'EN', 'KR', 'I go out at six. It may come after that. Should I ask our neighbor?',
   '나 6시에 나가. 그 뒤에 올 수도 있잖아. 옆집에 받아달라고 할까?',
   'Respond to the idea of asking the neighbor.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/457/1f5c27aa2e9bb1511cbbcfd62e160eac374b65385417b1ed2874f82b38f6bf3c.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 42 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 1),
   'EN', 'KR', 'Sure, I can get it for you. What time is it coming, and is there anything I should know?',
   '그래, 내가 받아줄게. 몇 시쯤 온대? 그리고 내가 알아둬야 할 게 있어?',
   'State what time the package is coming.
Share anything Marco should know, or say there is nothing.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/458/ba3d5fea9371437f60bad5384428e5d311b0d56449ecdc6bdf252e3d6c209c01.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 42 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 2),
   'EN', 'KR', 'Do you need to be there yourself, or can I just take it and sign your name?',
   '네가 직접 받아야 해? 아니면 내가 그냥 받아서 네 이름으로 사인해도 돼?',
   'Say whether you need to be there, or allow Marco to sign for it.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/459/db378d6b0f0f0203ce09678dd956b00d69a0fe3f9034a2d00330f1508fbd6a68.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 42 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 3),
   'EN', 'KR', 'One problem — I have to go out at six. What should we do if it still hasn''t come?',
   '근데 하나 있어 — 나 6시에 나가야 하거든. 그때까지도 안 오면 어떻게 할까?',
   'Suggest what to do if the package has not come by six.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/460/2336c01c03739f47cc7628e1d931a55cd1f344c78d4d4bebc2b6651d747ac55c.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 42 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 1),
   'EN', 'KR', 'Sure, I can grab it for you. What time is it supposed to arrive, and is there anything I should know — like if it''s heavy or fragile?',
   '그래, 내가 받아줄게. 몇 시쯤 온대? 그리고 무겁다거나 깨지기 쉽다거나, 미리 알아둘 거 있어?',
   'State what time the package should arrive.
Share anything Marco should know, or say there is nothing.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/461/55fb8def7d0a2a5a8986a5ee74788037fa0b0d9da11db9ff78d80aee39b5993b.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 42 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 2),
   'EN', 'KR', 'Does the courier need you there in person, or can I just take it and put your name down?',
   '기사님이 너더러 직접 받으라고 해? 아니면 내가 그냥 받아서 네 이름 적어도 돼?',
   'Say whether the courier needs you in person, or allow Marco to sign.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/462/4ed288e6ca7f18b52daedfde97e12ad171ea0e97bad4944da96db4cb0fe108c1.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 42 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 3),
   'EN', 'KR', 'One thing though — I have plans at six and need to head out. What should we do if it hasn''t arrived by then?',
   '근데 하나 걸리는 게 — 나 6시에 약속 있어서 나가야 하거든. 그때까지 안 오면 어떻게 하지?',
   'Suggest what to do if the package has not arrived by six.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/463/c740142154fd2b480b98c8e45574dde46d09b57c874f8fe4a4186a3731346e60.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 62 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 1),
   'EN', 'KR', 'Let''s stay calm. Where did you have your phone last?',
   '침착하자. 네 폰 마지막으로 어디에서 가지고 있었어?',
   'State where you last had your phone.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/464/f4fec1ceb3f87a20013d6e6ee5642426ad4b254639277dd53752169eb1771874.mp3', 'ACTIVE', now(), now(), '얘가 많이 당황했네. 나라도 침착하게 하나씩 물어봐야겠다.', 'NORMAL'),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 62 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 2),
   'EN', 'KR', 'Maybe we can''t find it today. What should we do first?',
   '오늘 못 찾을 수도 있어. 그럼 뭐부터 해야 할까?',
   'Suggest what to do first if the phone is not found today.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/465/8d9df0e43f92e7069cff16094e93f9190fe3626cc5d37773fb7fc5202d405bef.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 62 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 3),
   'EN', 'KR', 'Your phone has important things. How can we keep them safe?',
   '네 폰에 중요한 게 들어 있잖아. 그걸 어떻게 지킬 수 있을까?',
   'Suggest how to keep the phone''s important data safe.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/466/83f33e238b0f28d387f6bb6266dd46cf648f750c58a1851f33a35c5dbe71b841.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 62 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 1),
   'EN', 'KR', 'Let''s stay calm and think about where we went. I''m calling your phone now — when did you use it last?',
   '침착하게 우리가 어디 갔었는지 생각해보자. 내가 지금 전화해볼게 — 마지막으로 언제 썼어?',
   'State when you last used the phone.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/467/f8485bd71c53c02fcd106d6110245114b2c243fd04831e2ee7eb67a5c9f02d1a.mp3', 'ACTIVE', now(), now(), '당황하면 기억이 더 안 나지. 마지막으로 쓴 때부터 차근차근 짚어보자.', 'NORMAL'),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 62 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 2),
   'EN', 'KR', 'What if we can''t find it today? What do you think we should take care of first?',
   '오늘 못 찾으면 어쩌지? 뭐부터 처리해두는 게 좋을 것 같아?',
   'Suggest what to take care of first if the phone is not found.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/468/e1b8a95f16c313e2fd2e9103186ac7e4ef31c0af565076bf65648b5b083f452e.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 62 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 3),
   'EN', 'KR', 'Is there anything important on your phone? How can we protect your accounts?',
   '폰에 중요한 거 있어? 계정은 어떻게 보호하지?',
   'Say whether there is anything important on the phone.
Suggest how to protect your accounts.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/469/68a20265efdeea97941ccd383b47dda57cb27c953f3b79ff3bd410913761add2.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 62 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 1),
   'EN', 'KR', 'Okay, let''s stay calm and retrace our steps. I''ll call your phone right now — and is it connected to any other device we could use to track it?',
   '좋아, 침착하게 왔던 길을 되짚어 보자. 내가 지금 네 폰으로 전화해볼게 — 그리고 위치 추적에 쓸 만한 연결된 다른 기기 있어?',
   'Say whether the phone is connected to another device for tracking.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/470/199c1ae5ca283807642e6c181e575fc76398f7c41567b036eaf907085bd70e0a.mp3', 'ACTIVE', now(), now(), '일단 전화부터 걸어보고, 위치를 추적할 방법이 있는지 확인하는 게 먼저야.', 'NORMAL'),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 62 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 2),
   'EN', 'KR', 'What if we can''t find it today? We should get ahead of the worst case — what do you think we need to do first?',
   '오늘 못 찾으면 어떡하지? 최악의 상황에도 미리 대비해두자 — 뭐부터 해두면 좋을까?',
   'Suggest what to do first to prepare for the worst case.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/471/e90fc5b12b74c40547ada62865218e89d3a73c5c9ab6df7ce69e817dbac69189.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 62 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 3),
   'EN', 'KR', 'I told the cafe to call me if it turns up. By the way, is there anything important on your phone? How can we protect your accounts and data?',
   '카페에는 폰 나오면 나한테 전화해달라고 말해뒀어. 그런데 폰에 중요한 거 있어? 계정이랑 데이터는 어떻게 지키지?',
   'Say whether there is anything important on the phone.
Suggest how to protect your accounts and data.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/472/69a4083951b46a413bc4463f306afc8e8094aa9419e16c2f4ee9fed3387bff6d.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 52 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 1),
   'EN', 'KR', 'Oh no, the printer is not working! What should we do?',
   '어떡해, 프린터가 안 돼! 우리 어떻게 하지?',
   'Suggest what to do about the printer.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/473/af85b293524282d77498b487a838ec257029e9a65d79e68733b5259fe1c71420.mp3', 'ACTIVE', now(), now(), '프린터가 또 말썽이네. 시간 없는데 어떻게든 방법을 찾아야지.', 'NORMAL'),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 52 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 2),
   'EN', 'KR', 'Let''s use another computer. Can you use a USB or email it?',
   '다른 컴퓨터를 쓰자. USB로 줄래, 아니면 이메일로 보낼래?',
   'Choose USB or email for the file.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/474/2c409d0b5b2378afecf417976953dd27e2eae0800bbcde2f5835116404d8021d.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 52 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 3),
   'EN', 'KR', 'It will print in ten minutes. Can you come with me to tell the office?',
   '10분 뒤에 출력돼. 사무실에 고장났다고 말하러 같이 가줄래?',
   'Say whether you will come to tell the office.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/475/6ca2ff2dc5be6c266ec7721cf9919a17693a6b4dff19662226fdb4eb76e48e52.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 52 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 1),
   'EN', 'KR', 'The printer keeps showing a paper error, and our deadline is really close. What should we try first?',
   '프린터에 자꾸 용지 오류가 뜨는데 마감이 코앞이야. 뭐부터 해볼까?',
   'Suggest what to try first.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/476/f37930d90d78a0d2e7b5fdbfc77eadf089e77fcdd628a20591842e656d211670.mp3', 'ACTIVE', now(), now(), '마감이 코앞인데 프린터가 안 되네. 둘이서 뭐부터 해볼지 정하는 게 빠르겠다.', 'NORMAL'),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 52 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 2),
   'EN', 'KR', 'Moving your file to another computer might be faster. What''s the easiest way for me to get it?',
   '파일을 다른 컴퓨터로 옮기는 게 더 빠를 것 같아. 내가 그 파일을 어떻게 받는 게 제일 편해?',
   'State the easiest way to send the file.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/477/fa79b15c7cf0431c3f2a8a69e753b0692afd198bc206b7bd13faaa95b285ede9.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 52 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 3),
   'EN', 'KR', 'The printout will be ready in about ten minutes. Should we go tell the office it''s broken?',
   '출력물은 10분쯤 뒤에 나와. 사무실에 고장났다고 말하러 같이 갈까?',
   'Say whether to go tell the office.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/478/48847f86f491c1dad74e7b847a493588092cc3bb950a007413f0d5c33cc5b05f.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 52 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 1),
   'EN', 'KR', 'Oh no — the printer''s showing a paper error and the deadline''s coming up fast. What should we try first?',
   '큰일났다, 프린터에 용지 오류가 떴는데 제출 시간이 얼마 안 남았어. 뭐부터 해볼까?',
   'Suggest what to try first.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/479/966f0ed9a7d4e91cd8a5ea4d3e4737d1ffc0701124863d671fb6fbfcd713cbf2.mp3', 'ACTIVE', now(), now(), '여기서 시간을 끌면 제출을 못 해. 될 만한 것부터 하나씩 빠르게 시도해보자.', 'NORMAL'),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 52 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 2),
   'EN', 'KR', 'Let''s use the lab computer next door. Can you get me the file — USB, or email it over?',
   '옆 전산실 컴퓨터로 하자. 파일 좀 줄래 — USB로 줄래, 아니면 이메일로 보낼래?',
   'Choose USB or email for sending the file.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/480/8c76005a528d78d36752e1266e78997d01c2d1daff2449367884b5f09f11f5b5.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 52 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 3),
   'EN', 'KR', 'It''ll print in about ten minutes. Want to come with me to report the printer at the office after?',
   '10분쯤이면 출력돼. 끝나고 사무실에 프린터 고장났다고 같이 말하러 갈래?',
   'Say whether you will come to report the printer.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/481/7c431d476f65da7b4dab07e030167577aeebde3cd0869f692cc730f5fca8cf5c.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 46 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 1),
   'EN', 'KR', 'Sorry, the lockers are full. How big is your bag?',
   '죄송해요, 보관함이 다 찼어요. 가방이 얼마나 큰가요?',
   'State how big your bag is.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/482/1a6cf8b00ff117af3333aa28aed40f8022dcb82401211e25b5b10378fa3269db.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 46 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 2),
   'EN', 'KR', 'Do you want the near shop or the cheaper shop?',
   '가까운 가게로 하시겠어요, 더 싼 가게로 하시겠어요?',
   'Choose the near shop or the cheaper shop.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/483/70867fae3abe00430712b0495db915c32a9b8b47b4ef6da4420eda3ce22fc87d.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 46 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 3),
   'EN', 'KR', 'You need this ticket for your bag. Any questions?',
   '가방 찾으실 때 이 표가 필요해요. 궁금한 거 있으세요?',
   'Ask a question about the ticket or storage, or say you have none.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/484/0d4ee1d701fec9cf875bc7670daf233dff3d0f63bdee0e53c5a73bd2c01ddac7.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 46 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 1),
   'EN', 'KR', 'I''m afraid all the lockers are full right now. What time will you need your bag, and what size is it?',
   '죄송하지만 지금 보관함이 다 찼어요. 가방은 몇 시에 필요하시고, 크기는 어느 정도예요?',
   'State what time you will need the bag.
State the bag''s size.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/485/2faa7f2091037445b164afb0f90a170e18347a1dd16d594de8e073b515158067.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 46 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 2),
   'EN', 'KR', 'There''s a storage shop right outside the station for £8, and a cheaper one a short walk away for £4. Which fits your plans better?',
   '역 바로 앞에 8파운드짜리 보관소가 있고, 조금 걸어가면 4파운드짜리 더 저렴한 곳이 있어요. 일정상 어느 쪽이 더 나으세요?',
   'Choose one of the two storage shops.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/486/1899ee6c204ed3350e912b0c654b69d8d5bad806a2ad8d5f94287b8e90d144c9.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 46 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 3),
   'EN', 'KR', 'You''ll need this ticket to get your bag back. Is there anything else you''d like to know?',
   '가방을 찾으실 때 이 보관증이 필요해요. 더 궁금하신 점 있으세요?',
   'Ask anything else you want to know, or say you have no questions.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/487/b347b1cfa40a46f2308b76bcb9ce536cf8266bf62582d976a662c37433ccf9ce.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 46 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 1),
   'EN', 'KR', 'Right, the lockers are all full at the moment. Let me find you an alternative. First — what time will you be back for your bag, and how big is it?',
   '그러네요, 지금 보관함이 다 찼네요. 제가 대안을 찾아봐 드릴게요. 우선 가방은 몇 시쯤 찾으러 오실 예정이고, 크기는 어느 정도예요?',
   'State what time you will be back for the bag.
State how big it is.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/488/68b8f3007137ffc22b253c926fd3662c0975a02235cabbba2ed6213db7944d02.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 46 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 2),
   'EN', 'KR', 'There''s a storage shop right nearby that''s a bit pricey, and a cheaper one about ten minutes away. Which works better with your plans?',
   '바로 근처엔 조금 비싼 보관소가 있고, 걸어서 10분쯤엔 더 저렴한 곳이 있어요. 일정상 어느 쪽이 나으세요?',
   'Choose the storage option that fits your plans.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/489/733323ed1d53cce67639b4e5e3a2a6944b30f2b7e83e52e58ae4d9ace3c4ccc4.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 46 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 3),
   'EN', 'KR', 'You''ll need this claim ticket when you pick it up. Anything else you''d like to know about storing or collecting your bag?',
   '찾으실 때 이 보관증이 필요해요. 보관이나 수령에 대해 더 궁금한 점 있으세요?',
   'Ask about storing or collecting the bag, or say you have no questions.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/490/970f606b28a99500d260fa0d2c33450a6048cbbdf1d8fa4fd4e92e8e8fcd5df9.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 56 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 1),
   'EN', 'KR', 'Wait, this is not our food. What should we do?',
   '잠깐, 이건 우리 음식이 아니야. 어떻게 하지?',
   'Suggest what to do about the wrong food.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/491/46cec1ee68723156927120074580d9fab112823766ac199f17d8ed86821570b5.mp3', 'ACTIVE', now(), now(), '이건 우리가 시킨 음식이 아니잖아. 배는 고픈데 어쩌지.', 'NORMAL'),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 56 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 2),
   'EN', 'KR', 'They can bring our food in one hour. Is that okay?',
   '한 시간 뒤에 우리 음식을 가져다줄 수 있대. 괜찮아?',
   'Say whether waiting one hour is okay.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/492/d98cac2af9190744fc459801bda93e7a7499fbf9278e6988ca4f6414de066db5.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 56 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 3),
   'EN', 'KR', 'We will wait one hour. What can we do now?',
   '한 시간 기다려야 해. 우리 뭐 할까?',
   'Suggest what to do while waiting.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/493/a4f3d2b1b10680b869d8850f962eda129fc259368fde3b661727d8622cbe645d.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 56 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 1),
   'EN', 'KR', 'Wait, this isn''t what we ordered. What should we do about it?',
   '잠깐, 이건 우리가 시킨 게 아니야. 이거 어떻게 하지?',
   'Suggest what to do about the wrong order.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/494/8fe224eccdf492da835f2851c3886e88d3c5439157bb22afc343f752cb220657.mp3', 'ACTIVE', now(), now(), '시킨 거랑 완전히 다른 게 왔네. 혼자 정하기 전에 같이 얘기해봐야겠다.', 'NORMAL'),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 56 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 2),
   'EN', 'KR', 'They can deliver the right food in an hour, or refund us right away. Which one should we pick?',
   '한 시간 안에 제대로 다시 배달해주거나, 바로 환불해준대. 어느 쪽으로 할까?',
   'Choose redelivery or a refund.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/495/a37d6b020a53b306ce45f7c2e334c1bf979d662b0125127e8da16e2226b1f28f.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 56 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 3),
   'EN', 'KR', 'Either way, we''ll be waiting a while. What should we do while we wait?',
   '어느 쪽이든 좀 기다려야 해. 기다리는 동안 뭐 하면 좋을까?',
   'Suggest what to do while waiting.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/496/2fe7837b5406b0086571ceb82fc0c7fbb9cf5c5d86d412910a1af7daf7c0982c.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 56 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 1),
   'EN', 'KR', 'Wait, this isn''t what we ordered. What should we do?',
   '잠깐, 이거 우리가 시킨 게 아니야. 어떻게 하지?',
   'Suggest what to do about the wrong order.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/497/df7c3e94b4736e4cad784977b1a9d323181f998b919db9d0cc0efd950f3ffa22.mp3', 'ACTIVE', now(), now(), '배는 고픈데 음식은 남의 것이고… 일단 어떻게 할지 같이 정하는 게 낫겠다.', 'NORMAL'),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 56 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 2),
   'EN', 'KR', 'Support says they can redeliver within an hour, or give us a full refund right away. I''m starving, but the refund is instant… which should we go with?',
   '고객센터에서 한 시간 안에 재배달해주거나, 바로 전액 환불해줄 수 있대. 나 배고파 죽겠는데 환불은 즉시고… 어느 쪽으로 할까?',
   'Choose redelivery or the instant refund.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/498/46e99cad55f2cda27cbc82aae28ea0d42bb6d84b1df23b93702b9687205ee41f.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 56 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 3),
   'EN', 'KR', 'Either way it''ll take a while. What should we do about dinner in the meantime?',
   '어느 쪽이든 시간이 좀 걸리겠다. 그동안 저녁은 어떻게 할까?',
   'Suggest what to do about dinner in the meantime.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/499/81ecf4bc4a0d0fdc8dfc136967b3d6e509fc38b3bfd408f28138fd581d3629ca.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 70 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 1),
   'EN', 'KR', 'Please spell your passport name. Where are you going?',
   '여권에 있는 성함 철자를 말씀해 주세요. 어디로 가세요?',
   'Spell your passport name.
State where you are going.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/500/79a0c8be418175dbc7336bc69becaf9661db2be95aa96aff60ba8f561f7f63bb.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 70 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 2),
   'EN', 'KR', 'Sorry, the name on your ticket is wrong. You need a new ticket. Is a more expensive seat okay?',
   '죄송해요, 항공권에 이름이 잘못 적혀 있어요. 새로 구매하셔야 해요. 더 비싼 좌석도 괜찮으세요?',
   'Say whether the more expensive seat is okay.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/501/6b5a4572b5350434b07a0766cf29793e57b7a866af7abdea3a989c1291c4734c.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 70 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 3),
   'EN', 'KR', 'There is a seat for half price. But you must wait seven more hours. Which one do you want?',
   '절반 가격인 좌석도 있어요. 대신 7시간을 더 기다리셔야 해요. 어느 걸로 하시겠어요?',
   'Choose the half-price later seat or the current one.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/502/e3431117839cc564e0eb16b184cb817a9ef256778cd120ba69b6b74874538601.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 70 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 1),
   'EN', 'KR', 'Let me find your booking. Where are you flying to, and how is your name written on your passport?',
   '예약을 찾아볼게요. 어디로 가시고, 여권에는 성함이 어떻게 적혀 있나요?',
   'State your destination.
State how your name is written on your passport.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/503/3a037cd89209900992388cc05f705bae59d9ecb8bf63e5dd97c5a67aecaedc01.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 70 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 2),
   'EN', 'KR', 'I''m sorry — our rules don''t allow a name change, so you''ll need to buy the ticket again. Only business class is left. Is that all right?',
   '죄송해요 — 규정상 이름 변경이 안 돼서 항공권을 다시 구매하셔야 해요. 지금 비즈니스석만 남았는데 괜찮으세요?',
   'Say whether rebooking in business class is all right.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/504/6d80762cf3d81b1eb54352fa6adacbf9345e1f3628f8d49187a16dd9e8d6a978.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 70 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 3),
   'EN', 'KR', 'Another airline flies to the same city thirty minutes later for a lower price. Which one would you like?',
   '다른 항공사가 30분 뒤에 같은 도시로 가는데 가격이 더 저렴해요. 어느 걸로 하시겠어요?',
   'Choose one of the two flights.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/505/843d34763b4177d775d902107e38a41553689e5f24a045116b5b7aececbd09de.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 70 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 1),
   'EN', 'KR', 'Let me pull up your booking first. Could you tell me your destination and the exact name printed on your passport?',
   '먼저 예약을 확인해볼게요. 목적지랑 여권에 인쇄된 영문 이름을 정확히 말씀해 주시겠어요?',
   'State your destination.
State the exact name printed on your passport.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/506/49c54cf04c61a6c04f4a7bf24b2ccb1bf15bab038890a3fb9431a5aa182d691c.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 70 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 2),
   'EN', 'KR', 'I''m afraid our fare rules don''t allow a name correction on an issued ticket — you''d have to rebook. The only fare class still open on that route is business. Shall I go ahead?',
   '확인됐어요. 죄송하지만 규정상 저희가 새로 뽑아드릴 수가 없고 티켓을 다시 구매하셔야돼요. 비즈니스석만 남은 상태인데 괜찮으신가요?',
   'Respond to the business-class rebooking offer.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/507/11ce56c6f599a1538ed19d222a873b9170705dcfce392731f58f1fee1126ede8.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 70 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 3),
   'EN', 'KR', 'Oh, one more thing — there''s a seat on a later flight at about half the price, but it leaves seven hours after this one. Which one would you like to go with?',
   '아, 그리고 하나 더 — 같은 목적지로 가는 절반 가격 좌석이 있는데, 이 편보다 7시간 뒤에 출발해요. 어느 걸로 하시겠어요?',
   'Choose between the current flight and the cheaper later one.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/508/b996e773f1eaa4080ccceaafa6a7cc90b882b3f64b526dca73d905d02dec0210.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 48 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 1),
   'EN', 'KR', 'The weather will be nice this weekend. Where should we go?',
   '이번 주말에 날씨가 좋대. 우리 어디로 갈까?',
   'Suggest where to go.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/509/bf398ca5f6ba0958dee5aecdecf9241844e0e30bdf01a773ddbd901b949bd917.mp3', 'ACTIVE', now(), now(), '주말 날씨가 좋대! 어디로 가면 좋을지 물어봐야지.', 'GOOD'),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 48 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 2),
   'EN', 'KR', 'I have sandwiches and a blanket. What will you bring?',
   '난 샌드위치랑 돗자리 있어. 넌 뭐 가져올래?',
   'State what you will bring.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/510/7059bbcb9db5cf5bd9e337b90f30b349395a85881e0fa886ef970ed024d946ba.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 48 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 3),
   'EN', 'KR', 'It may rain that day. What should we do then?',
   '그날 비가 올 수도 있어. 그러면 뭐 할까?',
   'Suggest what to do if it rains.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/511/5854a17dea52b0dddce1f95420068519fa4cb133002df139d94e691b3df67f86.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 48 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 1),
   'EN', 'KR', 'The weather should be great this weekend. Where do you think we should have our picnic?',
   '이번 주말 날씨가 아주 좋대. 피크닉은 어디서 하면 좋을 것 같아?',
   'Suggest where to have the picnic.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/512/2a3e84bc271d85e3cb24c7dbe776de20058bd289a666c00bef133c4c7d2b25e9.mp3', 'ACTIVE', now(), now(), '이번 주말 날씨가 완벽하대. 피크닉 장소만 정하면 되겠다.', 'GOOD'),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 48 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 2),
   'EN', 'KR', 'I''m bringing sandwiches and a blanket. What would you like to bring?',
   '난 샌드위치랑 돗자리 가져갈게. 넌 뭘 가져오고 싶어?',
   'State what you would like to bring.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/513/1ec532cf54bf6ff64afd1b3168cbea09f5a206f85866c5016817fdbaf95f3def.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 48 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 3),
   'EN', 'KR', 'They say it might rain that day, though. What should we do then?',
   '근데 그날 비가 올 수도 있대. 그러면 뭘 하면 좋을까?',
   'Suggest what to do if it rains.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/514/06d2ac36c6efd69d0dd5622d3446bf0d574cda3d0194f5d979a2fa47062f737b.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 48 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 1),
   'EN', 'KR', 'The weather looks perfect this weekend. Where should we go for our picnic? Do you know any good spots?',
   '이번 주말 날씨 완전 좋대. 피크닉 어디로 갈까? 좋은 데 알아?',
   'Suggest a picnic spot.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/515/cc30a8545a862d1499f69fc3037036fac4bb5d91df2e4db9bde3924d359e92e0.mp3', 'ACTIVE', now(), now(), '이런 날씨를 방에서 보내면 아깝지. 좋은 장소 아는 데 있는지 물어보자.', 'GOOD'),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 48 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 2),
   'EN', 'KR', 'I''ll bring sandwiches and a blanket. What are you going to bring?',
   '난 샌드위치랑 돗자리 가져갈게. 넌 뭐 가져올래?',
   'State what you are going to bring.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/516/62eccadb7d3997fdc313802635248fb2f3f78ae37f88399966345dbdd7760c8a.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 48 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 3),
   'EN', 'KR', 'And just in case it rains — what should we do instead?',
   '그리고 혹시 비 오면 — 대신 뭘 할까?',
   'Suggest what to do instead if it rains.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/517/e97721dfd91a37a9275c870a0ffd9700a4aebf7348ef378903ff86a19d451cb4.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 45 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 1),
   'EN', 'KR', 'Let''s exercise together after class! What sport do you like?',
   '수업 끝나고 같이 운동하자! 너는 무슨 운동을 좋아해?',
   'State a sport you like.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/518/bd09ea5e4e7d5794bb27b9fe965c22e5cff2120a3dae39b73e737efbd9374bd8.mp3', 'ACTIVE', now(), now(), '혼자 운동하면 금방 지루해져. 같이 하자고 해봐야지.', 'GOOD'),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 45 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 2),
   'EN', 'KR', 'Why do you want to exercise?',
   '너는 왜 운동을 하고 싶어?',
   'Explain why you want to exercise.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/519/4f98c619c945efc8f7892428230cb2dfce9a4ac73a1fd16f09372f2385f2d6e4.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 45 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 3),
   'EN', 'KR', 'When and where can we meet?',
   '우리 언제, 어디서 만날까?',
   'Suggest when and where to meet.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/520/f40bd50e4508456170ac2acacf8fb85928a9d46fbbdb7ee974bb1e19f5381af0.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 45 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 1),
   'EN', 'KR', 'Let''s work out together after class! What kind of exercise do you enjoy the most?',
   '수업 끝나고 같이 운동하자! 어떤 운동을 제일 즐겨 해?',
   'State the exercise you enjoy most.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/521/364d4ecd0de13f3d172b7146051022ce5e4615f9455b49fb6d0ed7cc86a2c97b.mp3', 'ACTIVE', now(), now(), '같이 운동할 사람이 있으면 훨씬 오래 가지. 얘가 뭘 좋아하는지부터 물어보자.', 'GOOD'),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 45 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 2),
   'EN', 'KR', 'People exercise for different reasons. What do you hope to gain from it?',
   '사람마다 운동하는 이유가 달라. 너는 운동으로 무엇을 얻고 싶어?',
   'State what you hope to gain from exercising.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/522/70e0adff207cbcd0a377d4e2a9cc1831312dafc8fe275266a3544bb7e8dfa0e7.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 45 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 3),
   'EN', 'KR', 'Then let''s plan it. What time and place would be easiest for you?',
   '그럼 계획을 세우자. 너한테 가장 편한 시간과 장소는 어디야?',
   'Suggest a time and place.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/523/c5fc44c9eded5fd6295a16e36bdfa417cd74f6d0f2e1a36e03d871a5331a154b.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 45 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 1),
   'EN', 'KR', 'Let''s work out together after class! What kind of exercise do you want to do?',
   '수업 끝나고 같이 운동하자! 어떤 운동 하고 싶어?',
   'State what kind of exercise you want to do.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/524/fc0ca5cccd63a94b637465ff7e0beab465befcd082ef4cc0100c7c3bc32d11fd.mp3', 'ACTIVE', now(), now(), '같이 할 사람이 생기면 나도 안 빠지고 하게 되지. 어떤 운동을 하고 싶은지 들어보자.', 'GOOD'),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 45 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 2),
   'EN', 'KR', 'My goal is to build muscle. What about you — what do you want to get out of it?',
   '내 목표는 근육 키우기야. 넌 어때 — 운동으로 뭘 얻고 싶어?',
   'State what you want to get out of exercising.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/525/87d4f9f9e5f1c637c70cd454b854636a8a328614a277a34881aa9bb4c37ec15d.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 45 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 3),
   'EN', 'KR', 'Cool, let''s meet up then. Fair warning — I go pretty hard, so you might be walking home on shaky legs. When and where works for you?',
   '좋아, 그럼 만나자. 미리 말해두는데 나 운동 빡세게 해 — 다리 후들거리면서 집에 갈 각오해. 언제 어디서 볼까?',
   'Suggest when and where to meet.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/526/e4653571a3e1c042aa8d8778d22da612943d629a1776f0519b87bf8bae34506b.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 60 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 1),
   'EN', 'KR', 'We need three nature photos. Do you like the sea or mountains?',
   '자연 사진 세 장이 필요해. 넌 바다가 좋아, 산이 좋아?',
   'Choose the sea or the mountains.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/527/9ab34630429cbf7726f1b12c4e9f2db827192935d043ebdb8f21f7a9557db497.mp3', 'ACTIVE', now(), now(), '과제 사진을 찍어야 해. 얘는 바다랑 산 중에 뭘 좋아할까?', 'GOOD'),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 60 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 2),
   'EN', 'KR', 'Nice! When and where should we take them?',
   '좋다! 언제 어디서 찍을까?',
   'Suggest when and where to take the photos.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/528/57c5532385286b8621ed7e2ce39f152c1223c167363b5ba120436f04c8e63c8a.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 60 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 3),
   'EN', 'KR', 'Do you have a camera? You can use mine.',
   '카메라 있어? 없으면 내 거 써도 돼.',
   'Say whether you have a camera.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/529/43040557d7995f21c249d155a14e3bf6d22d0eb877c86693df0549f894463f76.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 60 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 1),
   'EN', 'KR', 'For class we have to turn in three photos of scenery. What kind of scenery do you like best?',
   '수업에서 풍경 사진 세 장을 내야 해. 넌 어떤 풍경을 제일 좋아해?',
   'State the scenery you like best.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/530/e60e8beb9f5068f3def78b293c9b92818f058aeca21dcac078bd4dbd46eaa379.mp3', 'ACTIVE', now(), now(), '같이 찍으려면 얘 취향도 알아야지. 어떤 풍경을 좋아하는지부터 물어봐야겠다.', 'GOOD'),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 60 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 2),
   'EN', 'KR', 'Nice. When and where should we take them so they look their best?',
   '좋다. 제일 잘 나오게 찍으려면 언제, 어디서 찍는 게 좋을까?',
   'Suggest when and where to shoot.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/531/5168616ed5dd4fec67718ebf4d05cd6b35edf06493fc04719d7faf8c47375cff.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 60 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 3),
   'EN', 'KR', 'Do you have a camera? If not, I can lend you mine.',
   '카메라 있어? 없으면 내 걸 빌려줄게.',
   'Say whether you have a camera.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/532/19220ee0c63d32010ebb6e6486a54c51ff803e354e31b26a4d95ddf472be1c28.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 60 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 1),
   'EN', 'KR', 'So we need to submit three photos of our favorite scenery. What kind of scenery do you love most?',
   '좋아하는 풍경 사진 세 장을 내야 해. 넌 어떤 풍경을 제일 좋아해?',
   'State the scenery you love most.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/533/af172256e804815abfb2e59d3116455251586b9ec9a6085bf0684dd52d9b8700.mp3', 'ACTIVE', now(), now(), '좋아하는 풍경을 찍어야 사진도 잘 나오지. 얘가 어떤 걸 제일 좋아하는지 들어보자.', 'GOOD'),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 60 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 2),
   'EN', 'KR', 'Nice. When and where should we shoot to really capture that? I heard the light is best around golden hour.',
   '좋다. 그 풍경을 제대로 담으려면 언제 어디서 찍는 게 좋을까? 골든아워쯤 빛이 제일 좋다던데.',
   'Suggest when and where to shoot.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/534/3a5c8a7c87d6da6d840f1eb166e7a2beca0196006927d025c8bf0394fd8101cf.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 60 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 3),
   'EN', 'KR', 'Then let''s meet at six the day after tomorrow. And for the other spots — I know just the place. Trust me and follow my lead. Sound good?',
   '그럼 내일모레 6시에 만나자. 나머지 장소는 내가 잘 아는 데가 있어 — 나만 믿고 따라와봐. 어때?',
   'Respond to the meetup plan.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/535/2643182df579508d547849589df88629ff8392364090a14a01e02026cfecaf4d.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 59 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 1),
   'EN', 'KR', 'Your coupon needs five more dollars. What would you like to do?',
   '쿠폰을 쓰시려면 5달러가 더 필요해요. 어떻게 하시겠어요?',
   'Say what you would like to do about the coupon.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/536/d88cf8cc6af0e2baea8b6e8ac4d5877e00f41114e03e4dd74ae0f903f0650559.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 59 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 2),
   'EN', 'KR', 'Spend twenty more dollars today. Then you get a fifteen dollar coupon next time. Do you want to buy more?',
   '오늘 20달러어치 더 사시면 다음에 쓸 수 있는 15달러 쿠폰을 드려요. 더 사시겠어요?',
   'Say whether you want to buy more.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/537/854d30f06d281e5574a7903ea7ad85cdda60423785a8ade36551534cae82dfbf.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 59 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 3),
   'EN', 'KR', 'All done! Do you want points today?',
   '결제 다 됐어요! 포인트 적립해 드릴까요?',
   'Say whether you want points.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/538/3f5a2e706abf7435efdb576900c30587dc91de1572e494a258116d18b87a039c.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 59 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 1),
   'EN', 'KR', 'The coupon is valid, but you''re a little under the minimum. Would you rather add a small item or pay without the discount?',
   '쿠폰은 유효한데 최소 결제 금액에 조금 모자라요. 작은 상품을 하나 추가하시겠어요, 아니면 할인 없이 결제하시겠어요?',
   'Choose adding a small item or paying without the discount.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/539/113af2066c133aafc06118592d43fffd2b71fa39ef7bf07c66e87a08e03b280f.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 59 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 2),
   'EN', 'KR', 'One more thing — spend forty dollars today and you''ll get a fifty-dollar coupon next time. Would you like to add anything?',
   '한 가지 더 알려드리면 — 오늘 40달러를 더 구매하시면 다음에 쓸 수 있는 50달러 쿠폰을 드려요. 더 담으시겠어요?',
   'Say whether you would like to add anything.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/540/659459d04306a6c954f9c8975fd533fc54fd365a29537d039dcb9732eaf29cd6.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 59 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 3),
   'EN', 'KR', 'All set! Would you like to use your membership today, and do you need a receipt?',
   '결제 끝났습니다! 오늘 멤버십 쓰시겠어요? 영수증은 필요하세요?',
   'Say whether you will use your membership.
Say whether you need a receipt.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/541/615d8429ea50f906182f88e6d287990173ab44c49684f52717616d2ac126a3e3.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 59 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 1),
   'EN', 'KR', 'Let me check… ah, the coupon''s valid, but you''re four dollars short of the minimum. You could add something small, or you''d have to pay full price without the coupon — what would you like to do?',
   '확인해볼게요… 아, 쿠폰은 유효한데 최소 결제 금액에서 4달러 모자라요. 작은 상품을 추가하셔도 되고, 아니면 쿠폰 없이 그냥 결제하셔야 돼요 — 어떻게 하시겠어요?',
   'Choose adding an item or paying full price without the coupon.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/542/d99280f2ed393c3a6079d7197cfb58dd7ec3fe9772bdc3e3cd1d2dc6c9ec3eea.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 59 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 2),
   'EN', 'KR', 'Oh, and just so you know — if you add forty dollars to your purchase today, you get a fifty-dollar coupon for next time. Would you like to add anything?',
   '아, 그리고 지금 40달러만 더 추가로 구매하시면 다음번에 쓸 수 있는 50달러 쿠폰을 받으실 수 있어요. 추가 구매 하시겠어요?',
   'Say whether you would like to add anything.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/543/f258d3a2367a6d5b0286ebbc6a430122b466159a81ed278c84ae809fc67e533d.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 59 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 3),
   'EN', 'KR', 'All done! Would you like to earn points on this purchase, and do you need your receipt?',
   '결제 다 됐습니다. 포인트 적립해 드릴까요? 영수증은 필요하세요?',
   'Say whether you want to earn points.
Say whether you need your receipt.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/544/602eef42835b1cc92ce7ece6c71d262fc70ae072a88aaf8c2e61618610fbc34a.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 51 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 1),
   'EN', 'KR', 'Okay, I can help you. What happened here?',
   '네, 도와드릴게요. 여기 무슨 일이에요?',
   'Explain what happened at the checkout.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/545/4fe82f1061daa6252dc136f4eae511c230f7b7c6aeb459e1f142b09103973469.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 51 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 2),
   'EN', 'KR', 'You must put a sticker on fruit yourself. Do you have another problem?',
   '과일은 직접 스티커를 붙여 오셔야 해요. 다른 문제도 있으세요?',
   'Say whether you have another problem.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/546/5441fd8081e9ade8c024fbcd1c868e5dda8712d7386f34a96067347c4f564f17.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 51 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 3),
   'EN', 'KR', 'I will watch your things. Can you get the sticker?',
   '제가 손님 물건을 봐드릴게요. 스티커 받아오실 수 있어요?',
   'Say whether you can go get the sticker.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/547/419718a4d692f00b18d9e082611363dbe8ed4efc34f7b8067e7d47113641c561.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 51 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 1),
   'EN', 'KR', 'Of course, let me have a look. What were you doing when the trouble started?',
   '물론이죠, 한번 볼게요. 문제가 생겼을 때 뭘 하고 계셨어요?',
   'Explain what you were doing when the trouble started.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/548/0dfa83c7cd926347c3722105feb548489765b1f1ee72045cc96727593ced07af.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 51 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 2),
   'EN', 'KR', 'Ah, I see. Fruit needs a special sticker before you pay. Is anything else giving you trouble?',
   '아, 그렇군요. 과일은 계산 전에 따로 스티커가 필요해요. 또 불편한 점이 있으세요?',
   'Say whether anything else is giving you trouble.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/549/86de211d2268a0ecc5fcc2da55cf599da3309d00864eefcc9bac39d5bc457c63.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 51 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 3),
   'EN', 'KR', 'Don''t worry, your things are safe with me here. Could you quickly go and get that sticker?',
   '걱정 마세요, 손님 물건은 제가 여기서 지키고 있을게요. 얼른 가서 그 스티커를 받아오시겠어요?',
   'Respond to the request to go get the sticker.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/550/61d4547ffe0f854092a66b58685090dc51454820ddb746d4778852fbbe228c99.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 51 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 1),
   'EN', 'KR', 'Sure, let me take a look. This machine acts up all the time. What were you doing when it froze?',
   '네, 볼게요. 이 기계가 원래 말썽이 잦아요. 뭐 하다가 멈췄어요?',
   'Explain what you were doing when the machine froze.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/551/92b3f8bc01746dbfbd52c6c92a8c4a0bb22023adc7e10026b90268e46d884e54.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 51 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 2),
   'EN', 'KR', 'Ah, I see — for fruit, you need to weigh it and print the sticker back where you picked it up. Is anything else giving you trouble?',
   '아, 과일은 고객님께서 과일 담으신 곳에서 직접 무게 재고 스티커를 붙여오셔야 해요. 다른 문제 또 있나요?',
   'Say whether anything else is giving you trouble.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/552/64ecc4d9fe9ba994a12ccdaded246934b184ddbffafda8c84a187d0d2d24593d.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 51 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 3),
   'EN', 'KR', 'No worries — I''ll watch your things here at the checkout while you go. Could you quickly pop over and grab the sticker?',
   '괜찮아요 — 다녀오시는 동안 제가 계산대에서 고객님 물건 지키고 있을 테니, 얼른 다녀오실래요?',
   'Respond to the request to go grab the sticker.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/553/28df7ef5183064fd64d36013f48a292fc220b50c9d081aa3d8e8020f5dcc4437.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 43 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 1),
   'EN', 'KR', 'We have an action movie and a cartoon tonight. Which one?',
   '오늘 밤엔 액션 영화랑 만화 영화가 있어요. 어떤 거요?',
   'Choose the action movie or the cartoon.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/554/6b069427ac20a3233ed6e9ce78b080fbd5bb155eb4cdecbceb537559952196b2.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 43 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 2),
   'EN', 'KR', 'We have seven or nine o''clock. How many tickets?',
   '7시랑 9시가 있어요. 몇 장 드릴까요?',
   'Choose a showtime.
State how many tickets you want.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/555/a2e91bb73da2528abb411fa3e3a11cd5fe8049426bc69f10156a8972c6b6f20d.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 43 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 3),
   'EN', 'KR', 'Okay! Do you want popcorn or a drink?',
   '알겠습니다! 팝콘 드릴까요, 음료 드릴까요?',
   'Choose popcorn or a drink, or decline.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/556/025786250c2b683cf363b5e21ff332bcbac22e7800025e2ce35b5affb42ade30.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 43 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 1),
   'EN', 'KR', 'We''re showing an action movie and an animated one tonight. Which one sounds better to you?',
   '오늘 밤엔 액션 영화랑 애니메이션 한 편을 상영해요. 어느 쪽이 더 끌리세요?',
   'Choose the movie that sounds better.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/557/4b49660f5de4ea42a817a07965f45ec0f96c4676dec8a213b5d3ea57b62fcfd1.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 43 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 2),
   'EN', 'KR', 'The seven o''clock is almost full, so you''d be sitting up front. The nine o''clock still has plenty of good seats — which works better?',
   '7시는 거의 차서 앞쪽에 앉으셔야 해요. 9시는 좋은 자리가 아직 많은데, 어느 쪽이 더 나으세요?',
   'Choose the seven or nine o''clock show.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/558/b5936c578de8b8bc2caac460b43f91a9dda7360b2ff48180218529798cf06698.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 43 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 3),
   'EN', 'KR', 'One ticket, then. We have all kinds of snacks here — what do you like to have during a movie?',
   '그럼 티켓 한 장이요. 스낵도 여러 가지 있는데, 영화 볼 때 뭘 드시는 걸 좋아하세요?',
   'State what you like to have during a movie.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/559/55e73259e11f5bbd978de5b86c824ed85937fe7b37a021b56ec89e2b34df9cfa.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 43 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 1),
   'EN', 'KR', 'We''ve got an action movie and an animated one showing tonight. What are you in the mood for?',
   '오늘은 액션 하나랑 애니메이션 하나가 상영 중이에요. 어떤 게 당기세요?',
   'Choose the movie you are in the mood for.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/560/6b5e5851acc7cdae89733eeb33cacb951546285548655b10ffb91f0336b55ac8.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 43 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 2),
   'EN', 'KR', 'The seven o''clock is almost full, so you''d be sitting near the front. The nine o''clock still has plenty of good seats. Which works better for you?',
   '7시는 거의 차서 앞쪽 자리만 남았고, 9시는 좋은 자리가 많이 남아 있어요. 어느 쪽이 나으세요?',
   'Choose the showtime that works better.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/561/a290508afb9a181339b582f86d5cf527700db5ebbea1c82c79f29d0e64517714.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 43 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 3),
   'EN', 'KR', 'That''s one ticket, then. We''ve got all kinds of snacks here — what do you usually go for when you watch a movie?',
   '그럼 티켓 한 장이요. 저희 영화관엔 정말 다양한 스낵이 있어요 — 영화 보실 때 주로 어떤 스낵 드세요?',
   'State what you usually have during a movie.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/562/bf2221f29a83b7690f465dbf57aeb5818cc1dcef95d9c5fd410042ab2a406878.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 57 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 1),
   'EN', 'KR', 'Your seat is that way. Which team do you like?',
   '자리는 저쪽이에요. 어느 팀을 좋아하세요?',
   'State the team you like.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/563/f4d1dad05bb9935f3fea414d84cd7ed8ec0570f3555fc54f8d3a6dfbf71597bd.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 57 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 2),
   'EN', 'KR', 'Is this your first game? Ask me anything.',
   '경기장은 처음이세요? 뭐든 물어보세요.',
   'Say whether it is your first game.
Ask a question, or say you have none.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/564/756e8da2ca23c480de79c9cb0452df2efc43fe466d94717385abcaab117b93a8.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 57 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 3),
   'EN', 'KR', 'People here eat meat pies. Do you want one?',
   '여기 사람들은 미트 파이를 먹어요. 하나 드실래요?',
   'Say whether you want a meat pie.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/565/be9309b8bfa82c516ac58c5a08c6d778f372e336bc54ac2dc3914199fa005e75.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 57 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 1),
   'EN', 'KR', 'Here''s your seat — up those stairs in block C. So which team are you here for today?',
   '자리는 여기예요 — 저 계단 위 C구역이요. 그래서 오늘은 어느 팀 때문에 오셨어요?',
   'State the team you are here for.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/566/d8196dd5969fc65fbe1c79e5e8d3afbf145ff653b14dd3e8167d3865133601c3.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 57 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 2),
   'EN', 'KR', 'Is this your first match here? Is there anything you want to know before it starts?',
   '여기 경기장은 처음이세요? 시작하기 전에 알고 싶은 게 있으세요?',
   'Say whether it is your first match.
Ask what you want to know, or say you have no questions.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/567/cc6fc064da1a81895b91cc597fe9e45fbdfb7eef8b43fe02d51b5d42af091a4d.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 57 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 3),
   'EN', 'KR', 'At halftime most fans eat a meat pie here. Would you like one?',
   '하프타임에는 여기 팬들 대부분이 미트 파이를 먹어요. 하나 드시겠어요?',
   'Say whether you would like a meat pie.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/568/80e7ba18e0b5d4a835ac065d35125cc22b614511a4662b8b88fb17ca87d65c14.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 57 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 1),
   'EN', 'KR', 'Let me see your ticket… you''re up those stairs, block C. So, which team are you here to cheer for today?',
   '티켓 볼게요… 저 계단 위 C구역이에요. 그래서, 오늘 어느 팀 응원하러 오셨어요?',
   'State the team you are here to cheer for.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/569/065e6d86ec777118e2a2619795772603fafc7c7d0cd61a8538b00da03a577513.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 57 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 2),
   'EN', 'KR', 'First time at a stadium here? When we score, everyone jumps up and sings — the words are on the big screen, so just join in. Anything you''re curious about before kickoff?',
   '여기 경기장은 처음이세요? 골이 들어가면 다들 일어나서 응원가를 불러요 — 가사가 전광판에 나오니까 그냥 따라 부르면 돼요. 경기 시작 전에 궁금한 거 있어요?',
   'Say whether it is your first time.
Ask a question before kickoff, or say you have none.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/570/849846aa276b347ea6bfd3431512ec2fb4081284d767673100414b00c6125efe.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 57 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 3),
   'EN', 'KR', 'At halftime everyone grabs a meat pie — it''s tradition. Fancy trying one?',
   '하프타임엔 다들 미트 파이를 먹어요 — 전통이거든요. 하나 드셔보실래요?',
   'Say whether you will try a meat pie.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/571/e9cb8ef0de36e62bef73d1df35274551c9e8f2a34d8fdb0bf5a946249912c1fc.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 68 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 1),
   'EN', 'KR', 'What is in the box?',
   '상자 안에 뭐가 들어 있어요?',
   'State what is in the box.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/572/9c039c483c85dbe0cd02bd529c89b02f34817d3e2d0533421c09a57823444b43.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 68 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 2),
   'EN', 'KR', 'Do you want fast mail or slow mail?',
   '빠른 우편으로 보내시겠어요, 느린 우편으로 보내시겠어요?',
   'Choose fast mail or slow mail.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/573/16e86261db1a363a484189a06b508d7e5ef69e1fd74851152f8f0bdacda6f24b.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 68 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 3),
   'EN', 'KR', 'Do you want insurance for four pounds?',
   '4파운드짜리 보험을 넣으시겠어요?',
   'Say whether you want the insurance.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/574/59a7522ce906a3d18ccc77b6a867f6214cb646502a4f2f80db36d44c51576c07.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 68 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 1),
   'EN', 'KR', 'What''s in the package? Is there anything I should be careful with?',
   '소포 안에 뭐가 들어 있어요? 조심해야 할 물건이 있나요?',
   'State what is in the package.
Say whether anything needs care, or say nothing does.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/575/99d7cda2c4c46149c26a75f385b2c112b34e17e1aad8a70e29fd5e614ab6fba1.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 68 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 2),
   'EN', 'KR', 'Express takes about a week for 25 pounds, and standard takes three weeks for 12. How soon do you want it delivered?',
   '특급은 일주일에 25파운드, 일반은 3주에 12파운드예요. 얼마나 빨리 도착하면 좋겠어요?',
   'State how soon it needs to be delivered.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/576/a457f2eef56a13130fd24078329b11b5d118694fea7586b25e526f9168b92a4e.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 68 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 3),
   'EN', 'KR', 'Would you like to add insurance for four pounds? I also need to write down the value of the items.',
   '4파운드에 보험을 추가하시겠어요? 그리고 물건 가치도 적어야 해요.',
   'Say whether you want the insurance.
State the value of the items.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/577/671885d85c6d1fe87f64e5460dd81a4e6a54995a30cff1d7c4488bfcc4e4e620.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 68 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 1),
   'EN', 'KR', 'What''s in the package? Anything fragile I should know about?',
   '안에 뭐가 들어 있어요? 깨지기 쉬워서 조심해야 할 물건 있나요?',
   'State what is in the package.
Say whether anything is fragile.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/578/91de17004e8961017b419d37b8dbc80aae7969a0a1034455fece7697e38b8282.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 68 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 2),
   'EN', 'KR', 'Express takes about a week at twenty-five pounds; standard takes three weeks at twelve. When does it need to get there?',
   '특급은 일주일에 25파운드고, 일반은 3주에 12파운드예요. 언제까지 도착해야 해요?',
   'State when it needs to get there.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/579/729f54054208ca8254bb85daa1e11e348bd6d0289d4ca54db411011085d3de52.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 68 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 3),
   'EN', 'KR', 'Since it''s gifts, would you like to add insurance for four pounds? And I''ll need a rough value for the customs form — about how much is everything worth?',
   '선물이면 4파운드에 보험 추가하시겠어요? 그리고 세관 신고서에 적어야 해서요 — 내용물이 대략 얼마 정도 가치예요?',
   'Say whether you want the insurance.
State the rough value of the contents.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/580/9b4e991f17adadeca95bcf9fa77d4cd159eb0df4faa2560ecb9c97cc2c577d2a.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 49 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 1),
   'EN', 'KR', 'Yes, you order here. Light beer or dark beer?',
   '네, 여기서 주문하시면 돼요. 가벼운 맥주요, 진한 맥주요?',
   'Choose light beer or dark beer.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/581/6872dd8f5298175835909f3572b62a28e6cc6262529d54cd3eaa17b415df3bf2.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 49 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 2),
   'EN', 'KR', 'Okay! A pint is bigger. Do you want a pint or a half?',
   '네! 파인트가 더 커요. 파인트로 드릴까요, 하프로 드릴까요?',
   'Choose a pint or a half.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/582/ad76a5ca61749a8eb41b1a678ae3ee2c1aebbc58c8c8e0673445b6e953cbed20.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 49 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 3),
   'EN', 'KR', 'That is twelve pounds. Do you want to pay now or later?',
   '12파운드예요. 지금 계산하실래요, 나중에 하실래요?',
   'Choose paying now or later.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/583/796346927fed5016884d96f3eddc525d68ae61ade8a89af987ab572eb253dc09.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 49 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 1),
   'EN', 'KR', 'Yes, you order and pay here at the bar first. Tell me what you normally drink and I''ll find you something similar.',
   '네, 펍에서는 여기 바에서 먼저 주문하고 계산해요. 평소에 어떤 걸 드시는지 말씀해 주시면 비슷한 걸로 찾아드릴게요.',
   'State what you normally drink.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/584/01476552ba7efbdf47ca05d6b54d82b8e0ff07db08e3146499a722743a7deb9f.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 49 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 2),
   'EN', 'KR', 'Good choice — pint or half? Our fish and chips is famous, if you''re eating.',
   '잘 고르셨어요. 파인트로 드릴까요, 하프로 드릴까요? 식사도 하신다면 여기 피시앤칩스가 유명해요.',
   'Choose a pint or a half.
Say whether you want food.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/585/8c0e092ec78d4ac7ab80b326f66df2b60f3e199c65b338721fbe744b19de1656.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 49 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 3),
   'EN', 'KR', 'That''s twelve pounds forty. You can pay each time, or leave your card and pay for everything when you leave — which do you prefer?',
   '12파운드 40이에요. 주문할 때마다 계산하시거나, 카드를 맡겨두고 나가실 때 계산하실 수도 있어요. 어느 쪽이 좋으세요?',
   'Choose paying each time or paying when you leave.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/586/b025b46f1abd76c1c005b2b0ebd9d07a707e00bae87a3471fd03de0c020a9ca3.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 49 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 1),
   'EN', 'KR', 'Yes, you order and pay here at the bar first. What do you usually drink back home? We''ve got light, crisp lagers and rich, hoppy ales on tap — I''ll find you something similar.',
   '네, 펍에선 여기 바에서 먼저 주문하고 계산해요. 평소엔 어떤 술 드세요? 가볍고 시원한 라거랑 향이 진한 에일이 생맥주로 있는데 — 비슷한 걸로 찾아드릴게요.',
   'State what you usually drink back home.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/587/4f2113c1ec44a6ab2256c7b60e95e91912b90e4ee27f14a23d2156784886ca7d.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 49 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 2),
   'EN', 'KR', 'Good choice. Pint or half? And are you eating? The fish and chips here is famous.',
   '잘 고르셨어요. 파인트로 드릴까요, 하프로 드릴까요? 식사도 하실 거예요? 여기 피시앤칩스가 유명해요.',
   'Choose a pint or a half.
Say whether you are eating.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/588/aff9a80e6ae8ad9f40baacb2d966699fa286e3bd69f3230dca7671a397ecf222.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 49 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 3),
   'EN', 'KR', 'That''ll be twelve pounds forty. You can pay as you go, or if you''re planning on a few, I can start you a tab — you leave your card and settle up when you leave. How would you like to do it?',
   '12파운드 40이에요. 주문할 때마다 계산을 하시거나, 많이 드실 거면 ''탭''을 터드릴게요 — 카드를 맡겨두고 나가실 때 한 번에 계산하는 방식이에요. 어떻게 하시겠어요?',
   'Choose paying as you go or starting a tab.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/589/708f2768cbc707d3bdb07b87aa03217305f45cbf3b7bd586ba02c084f4814c87.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 53 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 1),
   'EN', 'KR', 'What kind of tea do you like? I can help.',
   '어떤 차 좋아하세요? 제가 도와드릴게요.',
   'State a kind of tea you like.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/590/fa09e1600fae524d0ed1db806649af667dbadaa1418167acd81b3b204232998b.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 53 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 2),
   'EN', 'KR', 'The scones come with cream and jam. Which goes first?',
   '스콘은 크림이랑 잼이 같이 나와요. 어떤 걸 먼저 바르세요?',
   'Choose cream first or jam first.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/591/01304ad64b40dffbd2bb31de8d0af2a5a85a8eaa6ab39792fb0a6266e365dbfd.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 53 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 3),
   'EN', 'KR', 'Your cup is empty. Would you like more tea?',
   '잔이 다 비었네요. 차 더 드릴까요?',
   'Say whether you want more tea.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/592/378a05ba1e9e02a652743b031487f5cb15e0db9da1d3ee01714de44d98c0c2e4.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 53 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 1),
   'EN', 'KR', 'What kind of tea do you usually enjoy — floral, strong, or fruity? If it''s your first time, I can recommend a few.',
   '평소 어떤 차를 즐기세요 — 꽃향, 진한 맛, 과일향? 처음이시면 몇 가지 추천해드릴게요.',
   'State the kind of tea you enjoy, or ask for a recommendation.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/593/b0c7e0d599c3655ce5ce1b6d2ba97e6d0b5896ff3f5a32c12a15a2b298e031e2.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 53 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 2),
   'EN', 'KR', 'The scones come with clotted cream and jam. Do you put jam first or cream first? I can show you how we do it here.',
   '스콘은 클로티드 크림과 잼이 같이 나와요. 잼을 먼저 바르시나요, 크림을 먼저 바르시나요? 저희가 먹는 방법을 알려드릴게요.',
   'Choose jam first or cream first.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/594/af2cec5dd9e0e84a2238df863026cbcc098545615fdf361a20ba3d25335e44ea.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 53 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 3),
   'EN', 'KR', 'How is everything so far? Would you like a refill — the same one, or shall I bring the menu again?',
   '지금까지 어떠세요? 리필해드릴까요 — 같은 걸로 드릴까요, 메뉴를 다시 가져다드릴까요?',
   'Say how everything is.
Choose the same tea, or ask for the menu.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/595/d263f8397bc8cdaa6b1463816b12f5c65cad4ca0f0bbf1e5b86cae919d8c5354.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 53 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 1),
   'EN', 'KR', 'Lovely! What kind of tea do you usually enjoy — floral, strong, fruity? If it''s your first afternoon tea, I can recommend a few.',
   '좋아요! 평소 어떤 차를 즐기세요 — 꽃향, 진한 맛, 과일향? 애프터눈 티가 처음이시면 몇 가지 추천해드릴게요.',
   'State the kind of tea you enjoy, or ask for a recommendation.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/596/108a48320eb54351d6532cd5f68f929ac7e3864a4ccb4a20986424b07e2f2b42.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 53 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 2),
   'EN', 'KR', 'Here''s a tip — the scones come with clotted cream and jam. Would you like me to show you the proper way to eat them?',
   '팁 하나 드리면 — 스콘은 클로티드 크림이랑 잼이 같이 나와요. 제대로 먹는 방법 알려드릴까요? 혹시 고객님은 잼 먼저 바르시나요 크림 먼저 바르시나요?',
   'Say whether you want to be shown how to eat the scones.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/597/0dfbac7ea3e57526d39edc988e20c9e1b857e1752f098bc8024e6e21c24def0d.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 53 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 3),
   'EN', 'KR', 'How is everything? Would you like a refill — same tea, or something different this time?',
   '어떠세요, 입에 맞으세요? 리필해드릴까요 — 같은 차로 드릴까요, 이번엔 다른 걸로 드셔보실래요?',
   'Say how everything is.
Choose the same tea or a different one for the refill.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/598/9bf07e00134345666debbb09e0acfd149837b6e682ec4079c3bb49006a6b945c.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 61 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 1),
   'EN', 'KR', 'Hi, welcome! What do you want for your hair today?',
   '안녕하세요, 어서 오세요! 오늘 머리 어떻게 해드릴까요?',
   'State what you want for your hair.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/599/875c250b416c2e95813a93e79943fd19dc2547d039a5534e89ac53ddafcd4a94.mp3', 'ACTIVE', now(), now(), '새 손님이 오셨네. 오늘 어떻게 해드릴지 여쭤봐야지.', 'GOOD'),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 61 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 2),
   'EN', 'KR', 'How much should I cut, and your bangs too?',
   '얼마나 자르고, 앞머리도 다듬을까요?',
   'State how much to cut.
Say whether to cut the bangs.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/600/7203352ad96c1a774ca6bf041e69c1dd7ec5491990ec631efa1df63469b024b9.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 61 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 3),
   'EN', 'KR', 'All done! Do you like it?',
   '다 됐어요! 마음에 드세요?',
   'Say whether you like the result.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/601/e6b29d97814963d2b9dd0cd729c1997d1c48157ac25ce5b869e3cb18dc00043a.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 61 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 1),
   'EN', 'KR', 'Hi, welcome in! What would you like me to do with your hair today?',
   '안녕하세요, 어서 오세요! 오늘 머리 어떻게 해드릴까요?',
   'Describe what you would like done with your hair.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/602/f8babafc2c386942c57447928518f436a44ec8a47bcc68d3d319c7fca7625afb.mp3', 'ACTIVE', now(), now(), '오늘 첫 손님이네. 원하는 스타일을 먼저 정확히 들어봐야지.', 'GOOD'),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 61 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 2),
   'EN', 'KR', 'How much should I cut off? And do you want me to do the front as well?',
   '길이는 얼마나 자를까요? 그리고 앞머리도 같이 해드릴까요?',
   'State how much to cut off.
Say whether to do the front.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/603/0844b18f0a67874cb027e2681a3e87e02fe84299d25380b49306bde8d35ea0fd.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 61 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 3),
   'EN', 'KR', 'All done — take a look! How does it look to you, and is there anything you''d like me to fix?',
   '다 됐어요, 한번 보세요! 어떠세요? 더 고쳤으면 하는 데 있으세요?',
   'Say how it looks.
Ask for a fix, or say nothing needs fixing.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/604/1bdc9c0e74b740302a606d8bb6a2f9ebd2b989a214b308e36a09b5ed863b029a.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 61 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 1),
   'EN', 'KR', 'Hi, welcome in! So what are we doing with your hair today?',
   '어서 오세요! 오늘 머리 어떻게 해드릴까요?',
   'Describe what you want done with your hair.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/605/d0dec993098ad5e754e279317be9e711711f802a3aad70becb6e25b456661aa0.mp3', 'ACTIVE', now(), now(), '머리는 한번 자르면 되돌릴 수 없으니까, 원하는 걸 확실히 듣고 시작하자.', 'GOOD'),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 61 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 2),
   'EN', 'KR', 'Got it. How much length should I take off, and do you want me to trim your bangs while I''m at it?',
   '알겠어요. 길이는 얼마나 자를까요? 그리고 하는 김에 앞머리도 다듬어드릴까요?',
   'State how much length to take off.
Say whether to trim the bangs.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/606/922f94fde0b47846926939ef7333d645193b5cebe94d1948cb63c258ead28547.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 61 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 3),
   'EN', 'KR', 'All done — take a look! How do you like it? Anything you want me to touch up?',
   '다 됐어요 — 한번 보세요! 마음에 드세요? 더 손봤으면 하는 데 있어요?',
   'Say how you like it.
Ask for a touch-up, or say none is needed.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/607/95fd511295c4e9fcffa5f541d132b1d0173c75e8b839ba23722dee9ec5aaab64.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 63 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 1),
   'EN', 'KR', 'That team needs one more person. Do you want to play?',
   '저 팀에 한 명이 더 필요해요. 같이 하실래요?',
   'Say whether you want to play.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/608/fff0990b616c99a21cb96e9075f0a177a0a9bf869a4a238f38cdaad40ac3d0d6.mp3', 'ACTIVE', now(), now(), '저 팀에 한 명이 비었네. 이 손님한테 물어보면 되겠다.', 'GOOD'),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 63 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 2),
   'EN', 'KR', 'The quiz has music and sports. Which one is easy for you?',
   '퀴즈에 음악이랑 스포츠가 나와요. 어떤 게 쉬우세요?',
   'State the topic that is easy for you.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/609/e731540906c8175375cb690a10ebc76690f25c47435835847dc01544a3ced213.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 63 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 3),
   'EN', 'KR', 'Now we need a team name. What is your idea?',
   '이제 팀 이름이 필요해요. 어떤 생각이 있으세요?',
   'Suggest a team name.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/610/3d44ca314c6a03ecf041bf5548196905b2385e507d7894e0b1fa3727a8480ad8.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 63 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 1),
   'EN', 'KR', 'Quiz night starts soon and that team is one person short. Would you like to play with them?',
   '퀴즈 나이트가 곧 시작하는데 저 팀에 한 명이 부족해요. 같이 하시겠어요?',
   'Say whether you will play with the team.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/611/d95fc4cc0e08f3759df934338e4bd961fa93321fb4e0fcd397239fcd20ea2869.mp3', 'ACTIVE', now(), now(), '퀴즈가 곧 시작인데 저 팀은 한 명이 모자라. 혼자 온 이 손님을 넣어주면 딱이겠다.', 'GOOD'),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 63 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 2),
   'EN', 'KR', 'The topics are music, sports, and general knowledge. Which one suits you best?',
   '주제는 음악, 스포츠, 상식이에요. 어떤 게 제일 잘 맞으세요?',
   'State the topic that suits you best.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/612/db8d89e85f68d97e05ec1706e31f01a680419a3b1d8a4c91396ff4f196b0458f.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 63 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 3),
   'EN', 'KR', 'Entry is two pounds, and the winning team drinks free. So, what name should your team use?',
   '참가비는 2파운드고, 우승한 팀은 음료가 공짜예요. 자, 팀 이름은 뭐로 할까요?',
   'Suggest a name for the team.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/613/8dc817a592d435471df40aa0d4f587b9f318b0be2c75f5997f429dac4e21a957.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 63 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 1),
   'EN', 'KR', 'Hey, are you here on your own? Quiz night''s about to start and that team over there needs one more — want to join in?',
   '저기, 혼자 오셨어요? 퀴즈 나이트가 곧 시작하는데 저쪽 팀에 한 명 모자라요 — 같이 하실래요?',
   'Say whether you will join in.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/614/3ef807d9b38f2f96668900dfc17a9f4b4ba888895bee8aa2129a964eb50251c3.mp3', 'ACTIVE', now(), now(), '혼자 오신 것 같은데, 퀴즈에 끼면 훨씬 재밌을 거야. 자연스럽게 권해봐야지.', 'GOOD'),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 63 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 2),
   'EN', 'KR', 'The rounds are music, sports, and general knowledge. What are you good at? Your new team will want to know.',
   '라운드는 음악, 스포츠, 상식이에요. 어떤 분야에 자신 있어요? 새 팀원들이 궁금해할 거예요.',
   'State the topic you are good at.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/615/352e83e3d390bab8123345e817359107e979a731ce88e85683d2daa402041eb0.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 63 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 3),
   'EN', 'KR', 'It''s two pounds to enter, and the winners drink for free. Now the big question — what should your team be called?',
   '참가비는 2파운드고 우승 팀은 음료가 공짜예요. 자, 제일 중요한 질문 — 팀 이름은 뭐로 할까요?',
   'Suggest a team name.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/616/70edb8f3b6489a3abbb1af7e97a8c876f0657a131250e954c690994e885d6f9e.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 69 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 1),
   'EN', 'KR', 'Let me check your card. How did you travel today?',
   '카드를 확인해 볼게요. 오늘 어떻게 다니셨어요?',
   'Explain how you traveled today.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/617/1c2db4788769c309549a32fd7eb661f778a8710d680d4cb7dd8cc281f25b1258.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 69 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 2),
   'EN', 'KR', 'You paid too much. I will return the extra money. Where are you going now?',
   '요금이 과하게 부과됐네요. 차액은 돌려드릴게요. 지금은 어디로 가세요?',
   'State where you are going now.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/618/ed695f0ebbb251ce16823cd7f5c775ff9f0e4fc6576b45c8cbbd601037551556.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 69 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 3),
   'EN', 'KR', 'You can buy a day pass. How often do you ride?',
   '원데이 패스를 사실 수 있어요. 지하철은 얼마나 자주 타세요?',
   'State how often you ride.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/619/4857eb793257375d59ce25dcc2345ed6ce0ef3143c7c17a34cdbdc2a1b707d19.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 69 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 1),
   'EN', 'KR', 'What''s the problem? Tell me how you traveled today, and I''ll look at your card.',
   '무슨 문제세요? 오늘 어떻게 이동하셨는지 말씀해 주시면 카드를 살펴볼게요.',
   'Explain how you traveled today.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/620/af488cb8a445f432841c7d5ab40add206289c13288868a41e416ee7ac1919753.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 69 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 2),
   'EN', 'KR', 'You paid more than you should have. I''ll return the difference now. Where are you heading?',
   '원래 내셔야 할 금액보다 많이 내셨어요. 차액은 지금 돌려드릴게요. 지금 어디로 가시는 길이에요?',
   'State where you are heading.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/621/293bd473999032c4db886f6308aa31ba0b04db446c147a75e2f6377d88c1e3af.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 69 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 3),
   'EN', 'KR', 'How many days are you staying, and how often will you use the subway? There may be a cheaper option for you.',
   '며칠이나 머무르시고, 지하철은 얼마나 자주 이용하실 건가요? 더 저렴한 방법이 있을 수도 있어요.',
   'State how many days you are staying.
State how often you will use the subway.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/622/df1f768a25a2479ac734f12218eb0f8b4f26eab9d3a24312fbc10f8a4471810c.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 69 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 1),
   'EN', 'KR', 'What seems to be the trouble? Take your time — walk me through how you''ve been traveling today, and I''ll check your card.',
   '무슨 문제세요? 천천히요 — 오늘 어떻게 이동하셨는지 설명해 주시면 카드를 확인해볼게요.',
   'Explain how you have been traveling today.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/623/2b4808fb256f36def914401908e527ab40726785d271aa27832554ce86f7e44c.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 69 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 2),
   'EN', 'KR', 'You''re right — you were overcharged. I''ll refund the difference right away. Where are you trying to get to at the moment?',
   '맞네요 — 요금이 잘못 청구됐어요. 차액은 바로 환불해드릴게요. 지금은 어디로 가시는 길이에요?',
   'State where you are trying to get to.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/624/ebfd066ab01164324997290abe62a4c82aef46746b43c403ba7c2a2f1239ba94.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 69 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 3),
   'EN', 'KR', 'How much longer are you in London, and how often will you be riding? Depending on that, a day pass could save you money — what would you like to do?',
   '런던에 며칠 더 계세요? 지하철은 얼마나 자주 타실 것 같아요? 그에 따라 원데이 패스가 돈을 아껴줄 수도 있어요 — 어떻게 하시겠어요?',
   'State how much longer you are staying and how often you will ride.
Decide whether to get a day pass.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/625/0bb8cfde187cabb4fd3b6221544be048a033713e5acd8d44d19dc2a5037edffb.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 66 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 1),
   'EN', 'KR', 'I can show you a good hiking path! How many hours can you walk today?',
   '좋은 하이킹 코스 추천해드릴게요! 오늘 몇 시간 걸으실 수 있어요?',
   'State how many hours you can walk.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/626/d7db9cb314caea80f215ab68ac7ae4e30f8eecc7d56173c3a45bbf123f39f662.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 66 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 2),
   'EN', 'KR', 'It may rain later. Lake path or hill path?',
   '이따 비가 올 수도 있어요. 호수 길이요, 언덕 길이요?',
   'Choose the lake path or the hill path.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/627/dc985bee520728441665468f525d63f89dd4ad91ca14105e3da5e71e7d0c25c8.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 66 AND sq.question_level_group = 'LEVEL_1' AND sq.display_order = 3),
   'EN', 'KR', 'Good choice! What is in your bag today?',
   '잘 고르셨어요! 오늘 가방에 뭐가 있어요?',
   'State what is in your bag.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/628/146fcc316e3b78eb8ce536700b5fde2b946bfa15a9706d2dc4cdc83b9e432e7b.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 66 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 1),
   'EN', 'KR', 'Great! Do you hike a lot? And how long do you want to be out today?',
   '좋아요! 하이킹 많이 하세요? 그리고 오늘은 얼마나 걸을 생각이세요?',
   'Say whether you hike a lot.
State how long you want to be out.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/629/5854721f2f30df514798336c78667f8a7471c71b3a687aa81a54181491584a86.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 66 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 2),
   'EN', 'KR', 'There''s a flat trail around the lake, and a harder one up a hill with a lookout at the top. Rain is coming this afternoon. Which one would you prefer?',
   '호수를 도는 평평한 길이 있고, 꼭대기에 전망대가 있는 좀 힘든 언덕 길이 있어요. 오후엔 비가 올 거예요. 어느 쪽이 더 좋으세요?',
   'Choose the lake trail or the hill trail.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/630/cca3345a058efaafc1906d4a9b9aeed0777fa582c45df63ddf7cfdfacf290c5e.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 66 AND sq.question_level_group = 'LEVEL_2_TO_3' AND sq.display_order = 3),
   'EN', 'KR', 'Good choice. What did you bring today? I''ll tell you if something is missing.',
   '잘 고르셨어요. 오늘 뭘 챙겨 오셨어요? 빠진 게 있으면 알려드릴게요.',
   'State what you brought today.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/631/df05964d607d7452d7c126b96267848bff2f2f51e9d833a5c97407c27278869f.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 66 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 1),
   'EN', 'KR', 'Sure! Do you hike often, and how many hours are you up for today?',
   '그럼요! 하이킹 자주 하세요? 오늘은 몇 시간 정도 걸을 생각이세요?',
   'Say whether you hike often.
State how many hours you are up for.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/632/2c400de498b87703df0d0fbaee1ff2c63358d88058d3d41dc888cdeabab665e1.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 66 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 2),
   'EN', 'KR', 'There''s an easy lakeside loop, and a steep hill route with an amazing lookout — but rain''s expected this afternoon. Which sounds right for you?',
   '쉬운 호수 둘레길이랑, 전망이 끝내주는 가파른 언덕길이 있어요 — 근데 오후에 비 예보가 있어요. 어느 쪽이 맞을 것 같으세요?',
   'Choose the trail that sounds right for you.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/633/5c75d9c0ab8aa463297254ae234683295ede5fa270778b3f5c90de8c67a4cf1c.mp3', 'ACTIVE', now(), now(), NULL, NULL),
  ((SELECT sq.id FROM scenario_question sq JOIN scenario s ON s.id = sq.scenario_id
    WHERE s.id = 66 AND sq.question_level_group = 'LEVEL_4_TO_5' AND sq.display_order = 3),
   'EN', 'KR', 'Good pick. What have you got with you today — water, snacks, anything else? I''ll tell you if you''re missing anything.',
   '잘 고르셨어요. 오늘 뭐 챙겨 오셨어요 — 물, 간식, 그 밖에 뭐? 빠진 게 있으면 알려드릴게요.',
   'State what you have with you today.',
   'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/634/07c422aa8d47968988ed4d880d305b29f8eaabac8907f639683bbafd9fffd599.mp3', 'ACTIVE', now(), now(), NULL, NULL);

-- 전수 확인: 시나리오 30, 변형 30, 질문 270(레벨×문항 균등), 질문 변형 270이 전부 연결되어야 한다.
DO $$
BEGIN
    IF (SELECT count(*) FROM scenario
        WHERE id BETWEEN 41 AND 70 AND id = display_order) <> 30 THEN
        RAISE EXCEPTION 'scenario 41~70 insert verification failed';
    END IF;
    IF (SELECT count(*) FROM scenario WHERE id BETWEEN 41 AND 70
        AND (character_id IS NULL OR thumbnail_url IS NULL)) <> 0 THEN
        RAISE EXCEPTION 'scenario 41~70 character_id or thumbnail missing';
    END IF;
    IF (SELECT count(*) FROM scenario_language_variant
        WHERE scenario_id BETWEEN 41 AND 70) <> 30 THEN
        RAISE EXCEPTION 'scenario_language_variant 41~70 insert verification failed';
    END IF;
    IF (SELECT count(*) FROM scenario_question
        WHERE scenario_id BETWEEN 41 AND 70
          AND id BETWEEN 365 AND 634) <> 270 THEN
        RAISE EXCEPTION 'scenario_question 41~70 insert verification failed';
    END IF;
    IF (SELECT count(DISTINCT question_level_group || '/' || display_order)
        FROM scenario_question WHERE scenario_id BETWEEN 41 AND 70) <> 9 THEN
        RAISE EXCEPTION 'scenario_question 41~70 level/order combination verification failed';
    END IF;
    IF (SELECT count(*) FROM scenario_question_language_variant v
        JOIN scenario_question q ON q.id = v.scenario_question_id
        WHERE q.scenario_id BETWEEN 41 AND 70
          AND length(trim(v.required_response_element)) > 0) <> 270 THEN
        RAISE EXCEPTION 'scenario_question_language_variant 41~70 insert verification failed';
    END IF;
END
$$;
