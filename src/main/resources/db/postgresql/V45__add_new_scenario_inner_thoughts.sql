-- =============================================================================
-- 신규 시나리오 Q1 inner_thought 추가 — AI 먼저 시나리오 14건만
-- 생성일: 2026-08-07
--
-- 기존 데이터 규칙 준수: inner_thought는 AI 먼저 시나리오의 1번 질문에만 존재
-- (기존 20개도 AI 먼저 16개에만 있고, USER 먼저 4개는 전부 NULL).
-- 따라서 신규 USER 먼저 6개(21, 24, 26, 29, 39, 40)는 제외.
-- type 분포: GOOD 3 / NORMAL 11.
-- =============================================================================


-- 신2 | 공용 냉장고 (q64, 룸메이트 Marco)
UPDATE scenario_question_language_variant SET
  inner_thought = '냉장고 얘기 꺼내기 좀 눈치 보였는데, 규칙 정해두면 서로 편해질 거야.',
  inner_thought_type = 'NORMAL', updated_at = now()
WHERE scenario_question_id = 64 AND target_locale = 'EN' AND base_locale = 'KR';

-- 신3 | 세탁물 섞임 (q67, 이웃 Chloe)
UPDATE scenario_question_language_variant SET
  inner_thought = '내 파란 셔츠가 안 보이네… 옆방 애 빨래에 섞인 것 같은데, 기분 안 나쁘게 말해봐야지.',
  inner_thought_type = 'NORMAL', updated_at = now()
WHERE scenario_question_id = 67 AND target_locale = 'EN' AND base_locale = 'KR';

-- 신5 | 퇴실 점검 (q73, 하우징 직원)
UPDATE scenario_question_language_variant SET
  inner_thought = '퇴실 시즌이라 정신없네. 이 학생도 문제없이 보증금 잘 돌려받고 가면 좋겠다.',
  inner_thought_type = 'NORMAL', updated_at = now()
WHERE scenario_question_id = 73 AND target_locale = 'EN' AND base_locale = 'KR';

-- 신7 | 식당 메뉴 (q79, 서버)
UPDATE scenario_question_language_variant SET
  inner_thought = '우리 식당은 모든 메뉴가 다 자신 있지. 이 손님은 뭘 시키실까?',
  inner_thought_type = 'GOOD', updated_at = now()
WHERE scenario_question_id = 79 AND target_locale = 'EN' AND base_locale = 'KR';

-- 신8 | 박물관 패스 (q82, 관광 안내소 직원)
UPDATE scenario_question_language_variant SET
  inner_thought = '여행자들은 늘 패스 고민을 하지. 이분한텐 뭐가 좋으려나.',
  inner_thought_type = 'NORMAL', updated_at = now()
WHERE scenario_question_id = 82 AND target_locale = 'EN' AND base_locale = 'KR';

-- 신10 | 공항 좌석 (q88, 탑승구 직원)
UPDATE scenario_question_language_variant SET
  inner_thought = '이분한테 안내할 수 있는 좌석이 뭐가 있으려나. 넓은 좌석 옵션이 남아 있나 봐야겠다.',
  inner_thought_type = 'NORMAL', updated_at = now()
WHERE scenario_question_id = 88 AND target_locale = 'EN' AND base_locale = 'KR';

-- 신11 | 지도교수 상담 (q91, 지도교수)
UPDATE scenario_question_language_variant SET
  inner_thought = '이 학생에겐 어떤 수업 방식이 맞을까. 본인 생각부터 들어봐야겠다.',
  inner_thought_type = 'NORMAL', updated_at = now()
WHERE scenario_question_id = 91 AND target_locale = 'EN' AND base_locale = 'KR';

-- 신12 | 결석 노트 (q94, 동급생 Marco)
UPDATE scenario_question_language_variant SET
  inner_thought = '아파서 결석했다니 걱정되네. 내 필기가 도움이 되면 좋겠다 — 밥 한 끼 얻어먹을 기회일지도? ㅎㅎ',
  inner_thought_type = 'GOOD', updated_at = now()
WHERE scenario_question_id = 94 AND target_locale = 'EN' AND base_locale = 'KR';

-- 신13 | 과제 제출 방식 (q97, 조교)
UPDATE scenario_question_language_variant SET
  inner_thought = '보고서랑 발표 중에 다들 고민하지. 이 학생은 어느 쪽이 맞을까.',
  inner_thought_type = 'NORMAL', updated_at = now()
WHERE scenario_question_id = 97 AND target_locale = 'EN' AND base_locale = 'KR';

-- 신14 | 대체 재료 (q100, 식료품점 직원)
UPDATE scenario_question_language_variant SET
  inner_thought = '하필 생바질이 다 떨어진 날이네. 요리에 맞는 대체 재료를 찾아드려야겠다.',
  inner_thought_type = 'NORMAL', updated_at = now()
WHERE scenario_question_id = 100 AND target_locale = 'EN' AND base_locale = 'KR';

-- 신15 | 옷 추천 (q103, 의류 매장 직원)
UPDATE scenario_question_language_variant SET
  inner_thought = '주말 행사용 옷을 찾으시는 것 같은데 — 행사 분위기부터 물어봐야 실패가 없지.',
  inner_thought_type = 'NORMAL', updated_at = now()
WHERE scenario_question_id = 103 AND target_locale = 'EN' AND base_locale = 'KR';

-- 신16 | 선물 고르기 (q106, 기념품점 직원)
UPDATE scenario_question_language_variant SET
  inner_thought = '친구 선물 고르러 온 마음이 예쁘네. 딱 맞는 선물을 찾아드려야겠다.',
  inner_thought_type = 'GOOD', updated_at = now()
WHERE scenario_question_id = 106 AND target_locale = 'EN' AND base_locale = 'KR';

-- 신17 | 헤드폰 (q109, 전자제품 매장 직원)
UPDATE scenario_question_language_variant SET
  inner_thought = '헤드폰 사러 오셨네. 딱 맞는 모델 찾게 잘 도와드려야겠다.',
  inner_thought_type = 'NORMAL', updated_at = now()
WHERE scenario_question_id = 109 AND target_locale = 'EN' AND base_locale = 'KR';

-- 신18 | 중고 자전거 (q112, 개인 판매자)
UPDATE scenario_question_language_variant SET
  inner_thought = '드디어 내 중고 자전거를 사겠다는 사람이 나타났다. 오늘 꼭 팔아야지.',
  inner_thought_type = 'NORMAL', updated_at = now()
WHERE scenario_question_id = 112 AND target_locale = 'EN' AND base_locale = 'KR';


-- [확인 쿼리]
-- SELECT scenario_question_id, inner_thought, inner_thought_type
-- FROM scenario_question_language_variant
-- WHERE scenario_question_id IN (64,67,73,79,82,88,91,94,97,100,103,106,109,112)
-- ORDER BY scenario_question_id;
