-- 기존 20개 시나리오의 ai_role을 정리하고 display_order를 전체(글로벌) Day 번호로 전환한다.
-- =============================================================================
-- scenario 테이블 UPDATE SQL
-- 생성일: 2026-08-06
--
-- 반영 내용:
--   1) ai_role 20건 — 캐릭터명 제거, 직무/성격만 유지 (검수표 기준)
--      * 시나리오 1은 새 질문(q1)에서 Marco가 "스페인에서 온 교환학생"으로
--        자기소개하므로 '미국인 대학생' -> '스페인에서 온 교환학생'으로 정합화.
--   2) display_order — 전체(글로벌) 노출 순서로 부여.
--      40일 최종 플랜의 Day 번호를 그대로 사용 (신규 20개가 빈 번호를 채움).
--
-- 이 테이블에 없는 것 (별도 처리 필요):
--   - 제목(title): 다른 테이블에 있음. Charlie->Marco(1), Hailey->Chloe(6),
--     Jordan->Marco(8) 3건 반영 필요.
--   - tts_voice_id: 다른 테이블에 있음. 1=Chloe, 2=Marco 기준으로
--     시나리오 1·8은 Marco(2), 6은 Chloe(1) 확인 필요.
--
-- =============================================================================


-- ---------------------------------------------------------------------------
-- 1) ai_role — 캐릭터명 제거
-- ---------------------------------------------------------------------------
UPDATE scenario SET ai_role = '스페인에서 온 교환학생 룸메이트, 외향적이고 친화적인 성격', updated_at = now() WHERE id = 1;
UPDATE scenario SET ai_role = '룸메이트, 실용적이고 솔직하게 생활 규칙을 정하고 싶어함', updated_at = now() WHERE id = 2;
UPDATE scenario SET ai_role = '룸메이트, 주말에 같이 놀고 싶어하는 밝은 성격', updated_at = now() WHERE id = 3;
UPDATE scenario SET ai_role = '기숙사 프론트 데스크 직원, 매뉴얼에 따라 사무적이면서도 친절하게 응대', updated_at = now() WHERE id = 4;
UPDATE scenario SET ai_role = '룸메이트, 친구를 자주 초대하고 음악을 틀고 공부하는 편이라 경계를 미리 정하고 싶어함', updated_at = now() WHERE id = 5;
UPDATE scenario SET ai_role = '기숙사 파티에서 처음 만난 다른 유학생, 밝고 사교적인 성격', updated_at = now() WHERE id = 6;
UPDATE scenario SET ai_role = '룸메이트, 이제 꽤 친해져서 깊은 이야기도 나누고 싶어함', updated_at = now() WHERE id = 7;
UPDATE scenario SET ai_role = '같은 수업을 듣는 학생, 낯가림 없이 먼저 다가오는 성격', updated_at = now() WHERE id = 8;
UPDATE scenario SET ai_role = '조별과제 팀원, 발표를 앞두고 긴장한 성격', updated_at = now() WHERE id = 9;
UPDATE scenario SET ai_role = '담당 교수, 공정하지만 격식 있고 신중한 태도', updated_at = now() WHERE id = 10;
UPDATE scenario SET ai_role = '같은 수업 친구, 편하게 시험 스트레스를 나누는 성격', updated_at = now() WHERE id = 11;
UPDATE scenario SET ai_role = '조별 토론 팀원, 논리적이고 적극적으로 의견을 주고받는 성격', updated_at = now() WHERE id = 12;
UPDATE scenario SET ai_role = '비행기 옆자리에 앉은 여행자, 여행을 좋아하고 대화하기 편한 성격', updated_at = now() WHERE id = 13;
UPDATE scenario SET ai_role = '항공사 카운터 직원, 매뉴얼대로 정중하게 보상 절차를 안내', updated_at = now() WHERE id = 14;
UPDATE scenario SET ai_role = '호텔 프론트 데스크 직원, 친절하고 정중한 서비스직 태도', updated_at = now() WHERE id = 15;
UPDATE scenario SET ai_role = '낯선 사람, 호감을 표현하며 적극적으로 다가오는 성격', updated_at = now() WHERE id = 16;
UPDATE scenario SET ai_role = '카페/식당 직원, 친절하고 캐주얼한 서비스 태도', updated_at = now() WHERE id = 17;
UPDATE scenario SET ai_role = '약사, 차분하고 전문적으로 증상을 확인하는 태도', updated_at = now() WHERE id = 18;
UPDATE scenario SET ai_role = '길에서 만난 현지인 행인, 친절하게 길을 안내해주는 성격', updated_at = now() WHERE id = 19;
UPDATE scenario SET ai_role = '친구, 여행 이야기에 호기심 많고 수다스러운 성격', updated_at = now() WHERE id = 20;

-- ---------------------------------------------------------------------------
-- 2) display_order — 전체(글로벌) 노출 순서
--    40일 최종 플랜의 Day 번호를 그대로 부여합니다.
--    비어 있는 번호(3, 6, 9, 11, 13, 15, 17, 19, 20, 21, 23, 24, 25, 26,
--    29, 30, 32, 34, 35, 40)는 신규 시나리오 20개 몫 — INSERT 시 해당
--    번호로 넣으면 기존 행 재조정 없이 40일 순서가 완성됩니다.
--
--    display_order 유니크 제약이 있어도 안전하도록 먼저 +100 오프셋.
-- ---------------------------------------------------------------------------
UPDATE scenario SET display_order = display_order + 100 WHERE id BETWEEN 1 AND 20;

UPDATE scenario SET display_order = 1,  updated_at = now() WHERE id = 1;  -- Day 1  룸메 첫만남
UPDATE scenario SET display_order = 2,  updated_at = now() WHERE id = 3;  -- Day 2  주말 약속
UPDATE scenario SET display_order = 4,  updated_at = now() WHERE id = 17; -- Day 4  카페 주문
UPDATE scenario SET display_order = 5,  updated_at = now() WHERE id = 6;  -- Day 5  파티 (Chloe)
UPDATE scenario SET display_order = 7,  updated_at = now() WHERE id = 16; -- Day 7  헌팅 거절
UPDATE scenario SET display_order = 8,  updated_at = now() WHERE id = 8;  -- Day 8  첫 수업 (Marco)
UPDATE scenario SET display_order = 10, updated_at = now() WHERE id = 20; -- Day 10 여행 수다
UPDATE scenario SET display_order = 12, updated_at = now() WHERE id = 11; -- Day 12 시험 기간
UPDATE scenario SET display_order = 14, updated_at = now() WHERE id = 5;  -- Day 14 룰 정하기
UPDATE scenario SET display_order = 16, updated_at = now() WHERE id = 2;  -- Day 16 생활 습관
UPDATE scenario SET display_order = 18, updated_at = now() WHERE id = 9;  -- Day 18 발표 준비
UPDATE scenario SET display_order = 22, updated_at = now() WHERE id = 12; -- Day 22 토론 (HARD)
UPDATE scenario SET display_order = 27, updated_at = now() WHERE id = 13; -- Day 27 비행기 옆자리
UPDATE scenario SET display_order = 28, updated_at = now() WHERE id = 15; -- Day 28 호텔 체크인
UPDATE scenario SET display_order = 31, updated_at = now() WHERE id = 19; -- Day 31 런던 현지인
UPDATE scenario SET display_order = 33, updated_at = now() WHERE id = 18; -- Day 33 약국
UPDATE scenario SET display_order = 36, updated_at = now() WHERE id = 14; -- Day 36 항공 보상 (HARD)
UPDATE scenario SET display_order = 37, updated_at = now() WHERE id = 10; -- Day 37 교수 면담 (HARD)
UPDATE scenario SET display_order = 38, updated_at = now() WHERE id = 4;  -- Day 38 기숙사 요금 (HARD)
UPDATE scenario SET display_order = 39, updated_at = now() WHERE id = 7;  -- Day 39 깊은 대화


-- ---------------------------------------------------------------------------
-- [참고] 신규 시나리오 20개 INSERT 시 부여할 display_order (40일 플랜):
--   Day 3  유심 구매        | Day 6  방 키 분실     | Day 9  식료품 대체 재료
--   Day 11 세탁물           | Day 13 결석 노트(Marco)| Day 15 냉장고
--   Day 17 세면대 누수      | Day 19 과제 제출      | Day 20 지도교수 상담(HARD)
--   Day 21 옷 추천          | Day 23 헤드폰         | Day 24 불량 교환
--   Day 25 중고 자전거(HARD)| Day 26 기차표         | Day 29 박물관 패스
--   Day 30 식당 메뉴        | Day 32 투어 변경(HARD)| Day 34 선물 고르기
--   Day 35 공항 좌석        | Day 40 퇴실 점검(HARD, 피날레)
-- ---------------------------------------------------------------------------

-- =============================================================================
-- [확인 쿼리] 반영 결과 점검용
-- SELECT id, category_id, display_order, difficulty, first_speaker, ai_role
-- FROM scenario WHERE id BETWEEN 1 AND 20
-- ORDER BY display_order;
-- =============================================================================
