-- 시나리오 언어 콘텐츠의 캐릭터명을 통일하고 신규 20건의 언어 콘텐츠를 추가한다.
-- V38이 등록한 Teddy 보이스(deepgram/aura-2)의 id가 3이 아니면 즉시 실패시킨다.
DO $$
BEGIN
  IF (SELECT id FROM tts_voice
      WHERE model = 'deepgram/aura-2'
        AND provider_voice_id = 'aura-2-orpheus-en') IS DISTINCT FROM 3 THEN
    RAISE EXCEPTION 'Teddy tts_voice id must be 3 before applying scenario language variants';
  END IF;
END $$;

-- =============================================================================
-- scenario_language_variant UPDATE + INSERT SQL
-- 생성일: 2026-08-06
--
-- 구성:
--   A) 기존 20건 — 캐릭터명 치환 (Charlie->Marco, Hailey->Chloe, Jordan->Marco)
--   B) 기존 20건 — 제3자 사람 이름(Kate, Sofia, Peter, Nina 등) -> Teddy 치환
--      * 제3자 캐릭터가 곰(Teddy) 단일 캐릭터로 통일됨에 따른 정합화.
--      * 원치 않으면 이 섹션만 스킵 가능 (다른 섹션과 독립).
--   C) 기존 12건 — tts_voice_id -> 3(Teddy)
--      * 선행 조건: TTS 테이블에 Teddy(3) 등록 (deepgram/aura-2, aura-2-orpheus-en)
--      * TTS 담당자 작업과 중복되지 않는지 확인 후 실행할 것.
--   D) 신규 20건 INSERT (scenario 21~40, id 21~40)
--
-- 주의: id 명시 INSERT — 시퀀스 사용 시 재조정 필요.
-- =============================================================================


-- ---------------------------------------------------------------------------
-- A) 캐릭터명 치환 (Marco / Chloe) — 제목·브리핑·대화목표
-- ---------------------------------------------------------------------------
UPDATE scenario_language_variant SET
  title = '입주 첫날, 룸메이트 Marco와 첫 만남',
  briefing = '기숙사 입주 첫날, 방문을 열자 이미 짐을 풀고 있던 룸메이트 Marco가 반갑게 인사를 건넨다.',
  conversation_goal = 'Marco에게 자기소개를 하고, 취미와 한국 여행지를 자연스럽게 소개하기',
  updated_at = now()
WHERE scenario_id = 1 AND target_locale = 'EN' AND base_locale = 'KR';

UPDATE scenario_language_variant SET
  briefing = '입주 며칠 후, Marco가 편하게 지내기 위해 청소·생활 패턴·규칙에 대해 미리 얘기해보자고 한다.',
  conversation_goal = '청소 분담, 생활 리듬, 룸메이트로서의 마지노선을 Marco와 솔직하게 조율하기',
  updated_at = now()
WHERE scenario_id = 2 AND target_locale = 'EN' AND base_locale = 'KR';

UPDATE scenario_language_variant SET
  briefing = '카페에서 Marco와 커피를 마시다가 자연스럽게 주말 계획 얘기가 나온다.',
  conversation_goal = 'Marco와 주말 약속(요일, 할 일)을 정하고 서로의 취향 알아가기',
  updated_at = now()
WHERE scenario_id = 3 AND target_locale = 'EN' AND base_locale = 'KR';

UPDATE scenario_language_variant SET
  briefing = '같이 지낸 지 좀 됐지만, Marco가 손님 초대·소음·물건 공유에 대한 경계를 확실히 정해두고 싶어한다.',
  updated_at = now()
WHERE scenario_id = 5 AND target_locale = 'EN' AND base_locale = 'KR';

UPDATE scenario_language_variant SET
  title = '인터내셔널 파티 — 처음 만난 Chloe',
  briefing = '기숙사에서 열린 인터내셔널 파티. 처음 보는 Chloe가 먼저 다가와 말을 건다.',
  conversation_goal = '처음 만난 Chloe와 자연스럽게 스몰토크하고 다음 모임 초대에 응답하기',
  updated_at = now()
WHERE scenario_id = 6 AND target_locale = 'EN' AND base_locale = 'KR';

UPDATE scenario_language_variant SET
  briefing = '밤에 방에서 Marco와 둘이 있는데, Marco가 좀 더 깊은 이야기를 나누고 싶어한다.',
  conversation_goal = 'Marco와 가족, 꿈, 요즘 고민 등 개인적인 이야기를 진솔하게 나누기',
  updated_at = now()
WHERE scenario_id = 7 AND target_locale = 'EN' AND base_locale = 'KR';

UPDATE scenario_language_variant SET
  title = '첫 수업, 옆자리 Marco',
  briefing = '첫 수업 시간, 옆자리에 앉아도 되는지 묻는 Marco와 대화가 시작된다.',
  conversation_goal = 'Marco와 자연스럽게 첫 대화를 나누고 한국에서의 학교생활을 소개하기',
  updated_at = now()
WHERE scenario_id = 8 AND target_locale = 'EN' AND base_locale = 'KR';

-- ---------------------------------------------------------------------------
-- B) 제3자 이름 -> Teddy 치환 (스킵 가능 — 제3자를 Teddy로 통일하지 않으려면 제외)
-- ---------------------------------------------------------------------------
UPDATE scenario_language_variant SET
  briefing = '7월 한 달 내내 여행으로 기숙사를 비웠는데 에어컨 요금이 100달러나 청구되었다. 기숙사 프론트에 전화해 Teddy에게 상황을 설명해야 한다.',
  user_opening_instruction = '7월 한 달 내내 여행을 다니느라 기숙사를 비웠는데 에어컨 요금이 100달러나 나왔다. 기숙사 프론트에 전화해서 Teddy에게 상황을 설명하자.',
  conversation_goal = 'Teddy에게 부당 청구 상황을 설명하고, 증빙 제출과 환불 방식을 정하기',
  updated_at = now()
WHERE scenario_id = 4 AND target_locale = 'EN' AND base_locale = 'KR';

UPDATE scenario_language_variant SET
  briefing = '다음 주 조별 발표를 앞두고, 팀원 Teddy와 역할 분담과 준비 상황을 논의한다.',
  conversation_goal = 'Teddy와 발표 경험을 공유하고 역할을 분담하기',
  updated_at = now()
WHERE scenario_id = 9 AND target_locale = 'EN' AND base_locale = 'KR';

UPDATE scenario_language_variant SET
  briefing = '기말고사를 앞두고 친구 Teddy와 공부 습관에 대해 수다를 떤다.',
  conversation_goal = 'Teddy와 공부 습관과 경험을 나누고 같이 공부할 시간을 정하기',
  updated_at = now()
WHERE scenario_id = 11 AND target_locale = 'EN' AND base_locale = 'KR';

UPDATE scenario_language_variant SET
  briefing = '오늘 토론 수업 주제는 "돈으로 행복을 살 수 있는가". 조원 Teddy와 의견을 나눈다.',
  conversation_goal = '자신의 입장을 근거와 함께 논리적으로 표현하고 Teddy와 토론하기',
  updated_at = now()
WHERE scenario_id = 12 AND target_locale = 'EN' AND base_locale = 'KR';

UPDATE scenario_language_variant SET
  briefing = '장거리 비행 중, 옆자리에 앉은 Teddy가 먼저 말을 건다.',
  conversation_goal = 'Teddy와 여행 목적과 취향에 대해 자연스럽게 대화 나누기',
  updated_at = now()
WHERE scenario_id = 13 AND target_locale = 'EN' AND base_locale = 'KR';

UPDATE scenario_language_variant SET
  briefing = '캐리어가 파손된 채로 도착했다. 카운터 직원 Teddy에게 상황을 설명하고 보상책을 물어봐야 한다.',
  user_opening_instruction = '당신은 캐리어가 부서졌다. 카운터 직원 Teddy에게 상황을 설명하고 보상책을 물어봐야 한다.',
  conversation_goal = 'Teddy에게 수하물 파손 상황을 설명하고 보상 방식을 정하기',
  updated_at = now()
WHERE scenario_id = 14 AND target_locale = 'EN' AND base_locale = 'KR';

UPDATE scenario_language_variant SET
  briefing = '호텔에 도착해 체크인을 하며 프론트 직원 Teddy와 대화한다.',
  conversation_goal = 'Teddy에게 예약 정보를 전달하고 객실 선호사항을 요청하기',
  updated_at = now()
WHERE scenario_id = 15 AND target_locale = 'EN' AND base_locale = 'KR';

UPDATE scenario_language_variant SET
  briefing = '길에서 만난 Teddy가 호감을 표현하며 데이트를 제안한다. 정중하게 거절해야 하는 상황.',
  conversation_goal = 'Teddy의 제안을 무례하지 않으면서도 명확하게 거절하기',
  updated_at = now()
WHERE scenario_id = 16 AND target_locale = 'EN' AND base_locale = 'KR';

UPDATE scenario_language_variant SET
  briefing = '카페에 들어가 직원 Teddy에게 주문을 한다.',
  conversation_goal = 'Teddy에게 원하는 메뉴를 주문하고 매장/포장 여부를 전달하기',
  updated_at = now()
WHERE scenario_id = 17 AND target_locale = 'EN' AND base_locale = 'KR';

UPDATE scenario_language_variant SET
  briefing = '두통이 심해 약국에 방문했다. 약사 Teddy에게 증상을 설명하고 약을 사야 한다.',
  conversation_goal = 'Teddy에게 증상을 정확히 설명하고 필요한 정보를 전달하기',
  updated_at = now()
WHERE scenario_id = 18 AND target_locale = 'EN' AND base_locale = 'KR';

UPDATE scenario_language_variant SET
  briefing = '백화점에서 길을 잃었다. 지나가는 행인 Teddy에게 Tower Bridge로 가는 방향을 물어봐야 한다.',
  user_opening_instruction = '당신은 백화점에서 길을 잃었다. 지나가는 행인 Teddy에게 어디로 나가야 Tower Bridge가 가까운지 물어봐야 한다.',
  conversation_goal = 'Teddy에게 길을 묻고 자연스럽게 대화를 이어가기',
  updated_at = now()
WHERE scenario_id = 19 AND target_locale = 'EN' AND base_locale = 'KR';

-- ---------------------------------------------------------------------------
-- C) tts_voice_id -> 3 (Teddy) — 제3자 시나리오 12건
--    * TTS 테이블에 Teddy(3) 등록 후 실행 / TTS 담당자와 중복 확인
-- ---------------------------------------------------------------------------
UPDATE scenario_language_variant SET tts_voice_id = 3, updated_at = now()
WHERE scenario_id IN (4, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19)
  AND target_locale = 'EN' AND base_locale = 'KR';
-- (1, 2, 3, 5, 7, 8 = Marco(2) / 6, 20 = Chloe(1) — 현재 값 그대로 유지)

-- ---------------------------------------------------------------------------
-- D) 신규 20건 INSERT (scenario 21~40)
-- ---------------------------------------------------------------------------
INSERT INTO scenario_language_variant (id, scenario_id, target_locale, base_locale, title, briefing, user_opening_instruction, conversation_goal, status, created_at, updated_at, tts_voice_id) VALUES
(21, 21, 'EN', 'KR', '방 키를 잃어버려 임시 출입 요청하기',
 '외출 후 기숙사로 돌아왔지만 방 키가 보이지 않는다. 생활지원실에 임시 출입 방법과 재발급 절차를 문의해야 한다.',
 '생활지원실 직원 Teddy에게 방 키를 잃어버렸다고 설명하고, 방에 들어갈 수 있는 방법을 먼저 물어보세요.',
 '당장 방에 들어갈 방법과 키 재발급 시점, 안내 수단을 정하기',
 'ACTIVE', now(), now(), 3),
(22, 22, 'EN', 'KR', '공용 냉장고 공간 정리하기',
 '이름 없는 음식이 공용 냉장고를 가득 채워 새 식재료를 넣기 어렵다. 룸메이트 Marco가 정리 규칙을 정하자고 말을 건다.',
 NULL,
 'Marco와 라벨 방식, 이름 없는 음식 처리 기준, 정리 요일을 합의하기',
 'ACTIVE', now(), now(), 2),
(23, 23, 'EN', 'KR', '세탁물이 뒤섞인 문제 해결하기',
 '공용 세탁실에서 빨래를 꺼내는데, Chloe가 서로 옷이 섞인 것 같다며 다가온다.',
 NULL,
 'Chloe와 섞인 옷을 확인해 돌려주고, 건조기 사용과 재발 방지 방법을 정하기',
 'ACTIVE', now(), now(), 1),
(24, 24, 'EN', 'KR', '세면대 누수 수리 요청하기',
 '방 세면대 아래에서 물이 새기 시작했다. 피해가 커지기 전에 시설 관리실에 수리를 요청해야 한다.',
 '시설 관리실 직원 Teddy에게 방 세면대에서 물이 새고 있다고 설명하고 수리를 요청하세요.',
 '기다리는 동안의 조치와 방문 시간, 연락 방식을 정하기',
 'ACTIVE', now(), now(), 3),
(25, 25, 'EN', 'KR', '퇴실 점검과 보증금 확인하기',
 '학기가 끝나 기숙사를 떠나기 전이다. 퇴실 점검 항목과 보증금에서 차감될 수 있는 부분을 미리 확인해야 한다.',
 NULL,
 '점검 준비 방법과 방문 시간을 정하고, 보증금 차감 기준과 증빙 준비 안내 이해하기',
 'ACTIVE', now(), now(), 3),
(26, 26, 'EN', 'KR', '일정에 맞는 기차표 구매하기',
 '당일 다른 도시로 이동하려 한다. 매표소에서 직행 열차와 환승 열차를 비교해 표를 사야 한다.',
 '매표소 직원 Teddy에게 당일 이동할 기차표를 찾고 있다고 말하고, 직행과 환승 열차를 비교해 달라고 요청하세요.',
 '두 열차의 시간과 가격을 비교해 표를 고르고, 수령 방법과 좌석을 정하기',
 'ACTIVE', now(), now(), 3),
(27, 27, 'EN', 'KR', '식당에서 메뉴 비교하고 주문하기',
 '여행지 식당에서 파스타와 커리 중 하나를 고르려 한다. 서버 Teddy가 두 메뉴를 설명해 준다.',
 NULL,
 '두 메뉴의 맛을 비교해 메뉴와 옵션을 정하고, 리뷰 이벤트 제안에 답하기',
 'ACTIVE', now(), now(), 3),
(28, 28, 'EN', 'KR', '여행 일정에 맞는 박물관 패스 고르기',
 '하루 동안 쓸 관광권을 사려 한다. 관광 안내소에서 박물관 패스와 도시 패스 중 하나를 골라야 한다.',
 NULL,
 '두 패스의 포함 항목을 비교해 고르고, 점심 추가 여부와 수령 형태를 정하기',
 'ACTIVE', now(), now(), 3),
(29, 29, 'EN', 'KR', '날씨 때문에 야외 투어 일정 변경하기',
 '예약한 야외 투어 시간에 폭우가 예보됐다. 여행사에 일정 변경을 요청해야 한다.',
 '여행사 직원 Teddy에게 날씨 때문에 예약한 야외 투어 참여가 어렵다고 설명하고, 일정 변경이 가능한지 물어보세요.',
 '대체 시간을 고르고 추가 요금에 답한 뒤, 다른 투어 제안에 응답하기',
 'ACTIVE', now(), now(), 3),
(30, 30, 'EN', 'KR', '공항에서 더 편한 좌석으로 변경하기',
 '탑승을 기다리다 내 좌석이 가운데 자리인 것을 확인했다. 탑승구 직원 Teddy가 좌석 옵션을 안내한다.',
 NULL,
 '유료 업그레이드와 비상구 좌석 조건을 판단하고, 수하물 위탁 여부를 정하기',
 'ACTIVE', now(), now(), 3),
(31, 31, 'EN', 'KR', '수강 과목을 지도교수와 상담하기',
 '다음 학기 수강 신청을 앞두고 평가 방식이 다른 두 과목 중 하나를 골라야 한다. 지도교수가 차이를 설명해 준다.',
 NULL,
 '두 과목의 평가 방식을 비교해 고르고, 이유를 설명한 뒤 수업 시간을 정하기',
 'ACTIVE', now(), now(), 3),
(32, 32, 'EN', 'KR', '결석한 수업 노트 부탁하기',
 '몸이 아파 수업에 빠져 중요한 설명을 놓쳤다. Marco가 필기해 둔 노트를 보여주겠다며 말을 건다.',
 NULL,
 'Marco에게 필요한 내용과 전달 방법을 정하고, 능청스러운 밥 사달라는 부탁에 재치 있게 답하기',
 'ACTIVE', now(), now(), 2),
(33, 33, 'EN', 'KR', '과제 제출 방식 선택하고 준비하기',
 '과제를 보고서나 발표 중 하나로 제출해야 한다. 조교 Teddy가 선택지를 안내한다.',
 NULL,
 '과제 형식을 고르고 이유를 설명한 뒤, 마감 경고에 답하기',
 'ACTIVE', now(), now(), 3),
(34, 34, 'EN', 'KR', '식료품점에서 대체 재료 찾기',
 '파스타를 만들려는데 생바질을 찾을 수 없다. 직원 Teddy가 대체 재료를 추천해 준다.',
 NULL,
 '만들 요리를 설명하고 대체 재료와 포장 크기, 할인 상품 구매 여부를 정하기',
 'ACTIVE', now(), now(), 3),
(35, 35, 'EN', 'KR', '상황에 맞는 옷 추천받기',
 '주말 행사에 입을 옷을 둘러보는 중이다. 직원 Teddy가 재킷과 블레이저를 추천한다.',
 NULL,
 '행사 분위기를 설명하고 색상과 반품 조건을 고려해 입어 볼 옷을 정하기',
 'ACTIVE', now(), now(), 3),
(36, 36, 'EN', 'KR', '가격과 특징을 비교해 선물 고르기',
 '여행을 마치기 전 친구에게 줄 선물을 사려 한다. 직원 Teddy가 두 가지 선물을 제안한다.',
 NULL,
 '두 선물을 비교해 고르고, 카드 문구와 포장 방법을 정하기',
 'ACTIVE', now(), now(), 3),
(37, 37, 'EN', 'KR', '기능과 가격을 비교해 헤드폰 고르기',
 '헤드폰을 사러 매장에 왔다. 직원 Teddy가 두 모델의 차이를 설명해 준다.',
 NULL,
 '자신의 우선순위를 설명하고 모델과 색상을 정하기',
 'ACTIVE', now(), now(), 3),
(38, 38, 'EN', 'KR', '중고 자전거 상태 확인하고 가격 협상하기',
 '온라인에서 본 중고 자전거를 확인하러 왔다. 판매자 Teddy와 상태를 확인하고 가격을 협상해야 한다.',
 NULL,
 '자전거 상태를 확인하고 근거를 들어 가격을 협상한 뒤, 거래 조건을 확정하기',
 'ACTIVE', now(), now(), 3),
(39, 39, 'EN', 'KR', '불량 상품 교환 또는 환불 요청하기',
 '전날 산 전기주전자가 작동 중 자꾸 꺼진다. 반품 창구에 문제를 설명하고 해결 방법을 정해야 한다.',
 '반품 창구 직원 Teddy에게 전날 구매한 전기주전자의 문제를 설명하고, 교환이나 환불이 가능한지 물어보세요.',
 '점검 여부와 교환 또는 환불을 정하고, 사과 선물에 자연스럽게 답하기',
 'ACTIVE', now(), now(), 3),
(40, 40, 'EN', 'KR', '유심과 요금제 구매하기',
 '현지에 도착해 휴대폰을 쓰려면 유심이 필요하다. 통신사 매장에서 요금제를 비교해 골라야 한다.',
 '통신사 매장 직원 Teddy에게 유심을 사러 왔다고 말하고, 어떤 요금제가 있는지 물어보세요.',
 '체류 기간과 데이터 사용량을 설명하고, 요금제와 등록·결제 방식을 정하기',
 'ACTIVE', now(), now(), 3);


-- =============================================================================
-- [확인 쿼리]
-- SELECT scenario_id, title, tts_voice_id FROM scenario_language_variant
-- WHERE target_locale = 'EN' ORDER BY scenario_id;
-- =============================================================================

-- ---------------------------------------------------------------------------
-- id 명시 INSERT 이후 auto-increment 시퀀스를 재조정한다.
-- ---------------------------------------------------------------------------
SELECT setval(pg_get_serial_sequence('scenario_language_variant', 'id'), (SELECT MAX(id) FROM scenario_language_variant));
