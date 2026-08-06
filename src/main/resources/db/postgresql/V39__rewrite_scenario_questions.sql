-- 기존 20개 시나리오의 고정 질문 60건을 40일 커리큘럼 기준으로 재작성한다.
-- =============================================================================
-- 시나리오 질문 개편 UPDATE SQL (최종 확정본)
-- 대상 테이블: scenario_question_language_variant (EN/KR variant 60건)
-- 생성일: 2026-08-06
--
-- 반영 내용:
--   1) 질문 60개 전면 교체 (자연스러운 회화체, 질문 독립성 보장, 화용 함정 유지)
--   2) 캐릭터명 통일: Chloe(tts_voice_id=1), Marco(tts_voice_id=2)
--      - q1: Marco 자기소개 / q16: Chloe 자기소개 / q22: Marco 자기소개
--   3) 시나리오 4는 '기숙사 요금 문제(프론트 전화, 직원 Kate)' 기준으로 재작성
--   4) inner_thought는 전건 기존 내용과 정합 확인 완료 — 변경 없음
--
-- 시나리오 테이블 쪽 변경(노출 순서, 제목, AI 역할)은 파일 하단 주석 참고.
-- =============================================================================


-- ---------------------------------------------------------------------------
-- Day 1 | 시나리오 1 — 입주 첫날, 룸메이트 Marco와 첫 만남 (q 1~3)
-- ---------------------------------------------------------------------------
UPDATE scenario_question_language_variant SET
  question_text = 'Wait, you''re my roommate? No way! I''m Marco, an exchange student from Spain. Nice to meet you! So what''s your name? Tell me about yourself!',
  question_translation = '잠깐, 네가 내 룸메야? 대박! 난 Marco고, 스페인에서 온 교환학생이야. 만나서 반가워! 넌 이름이 뭐야? 네 얘기 좀 해줘!',
  updated_at = now()
WHERE scenario_question_id = 1 AND target_locale = 'EN' AND base_locale = 'KR';

UPDATE scenario_question_language_variant SET
  question_text = 'So what do you do for fun? What got you into it?',
  question_translation = '넌 취미가 뭐야? 어쩌다 그거에 빠졌어?',
  updated_at = now()
WHERE scenario_question_id = 2 AND target_locale = 'EN' AND base_locale = 'KR';

UPDATE scenario_question_language_variant SET
  question_text = 'I''m so into Korea right now — the food, the dramas, everything. If I ever visit, where do you take me first?',
  question_translation = '나 요즘 한국에 완전 빠졌거든 — 음식이며 드라마며 전부 다. 내가 언젠가 한국 가면 어디부터 데려갈 거야?',
  updated_at = now()
WHERE scenario_question_id = 3 AND target_locale = 'EN' AND base_locale = 'KR';

-- ---------------------------------------------------------------------------
-- Day 2 | 시나리오 3 — 주말 약속 잡기 (q 7~9)
-- ---------------------------------------------------------------------------
UPDATE scenario_question_language_variant SET
  question_text = 'Hey, we should hang out this weekend! Does Saturday work, or is Sunday better for you?',
  question_translation = '야, 우리 이번 주말에 놀자! 토요일 괜찮아, 아니면 일요일이 나아?',
  updated_at = now()
WHERE scenario_question_id = 7 AND target_locale = 'EN' AND base_locale = 'KR';

UPDATE scenario_question_language_variant SET
  question_text = 'Is there anything you''ve been wanting to do since you got here? Maybe we can do it this weekend.',
  question_translation = '여기 와서 해보고 싶었던 거 있어? 이번 주말에 같이 하자.',
  updated_at = now()
WHERE scenario_question_id = 8 AND target_locale = 'EN' AND base_locale = 'KR';

UPDATE scenario_question_language_variant SET
  question_text = 'So what do you usually do when you hang out with your friends?',
  question_translation = '넌 평소에 친구들이랑 만나면 뭐하고 놀아?',
  updated_at = now()
WHERE scenario_question_id = 9 AND target_locale = 'EN' AND base_locale = 'KR';

-- ---------------------------------------------------------------------------
-- Day 4 | 시나리오 17 — 카페 주문 (q 49~51)
-- ---------------------------------------------------------------------------
UPDATE scenario_question_language_variant SET
  question_text = 'Hi, what can I get for you?',
  question_translation = '안녕하세요, 뭐 드릴까요?',
  updated_at = now()
WHERE scenario_question_id = 49 AND target_locale = 'EN' AND base_locale = 'KR';

UPDATE scenario_question_language_variant SET
  question_text = 'For here or to go? And would you like anything else with that?',
  question_translation = '드시고 가세요, 포장이세요? 더 필요하신 건 없으세요?',
  updated_at = now()
WHERE scenario_question_id = 50 AND target_locale = 'EN' AND base_locale = 'KR';

UPDATE scenario_question_language_variant SET
  question_text = 'By the way, our chocolate cookies just came out of the oven. Wanna add one?',
  question_translation = '아 참, 초코쿠키가 방금 나왔어요. 하나 추가하실래요?',
  updated_at = now()
WHERE scenario_question_id = 51 AND target_locale = 'EN' AND base_locale = 'KR';

-- ---------------------------------------------------------------------------
-- Day 5 | 시나리오 6 — 인터내셔널 파티, 처음 만난 Chloe (q 16~18)
-- ---------------------------------------------------------------------------
UPDATE scenario_question_language_variant SET
  question_text = 'Hey, I don''t think we''ve met — I''m Chloe. How do you know the host?',
  question_translation = '안녕, 우리 초면이지? 난 Chloe야. 파티 주최한 애랑은 어떻게 아는 사이야?',
  updated_at = now()
WHERE scenario_question_id = 16 AND target_locale = 'EN' AND base_locale = 'KR';

UPDATE scenario_question_language_variant SET
  question_text = 'By the way, your English is really good! Did you study abroad before?',
  question_translation = '근데 너 영어 진짜 잘한다! 전에 유학한 적 있어?',
  updated_at = now()
WHERE scenario_question_id = 17 AND target_locale = 'EN' AND base_locale = 'KR';

UPDATE scenario_question_language_variant SET
  question_text = 'My friend''s having a potluck next week — you should come! Everyone brings something. What would you make?',
  question_translation = '다음 주에 내 친구가 포틀럭 파티 하는데, 너도 와! 다들 음식 하나씩 가져오거든. 넌 뭐 만들어 올래?',
  updated_at = now()
WHERE scenario_question_id = 18 AND target_locale = 'EN' AND base_locale = 'KR';

-- ---------------------------------------------------------------------------
-- Day 7 | 시나리오 16 — 길거리 데이트 신청 (q 46~48)
-- ---------------------------------------------------------------------------
UPDATE scenario_question_language_variant SET
  question_text = 'Hi, sorry to just come up to you like this — I just thought you were really cute. Are you free tonight? Maybe we could grab a coffee?',
  question_translation = '안녕하세요, 이렇게 갑자기 말 걸어서 미안한데 — 너무 제 스타일이라서요. 오늘 저녁에 시간 있어요? 커피 한잔 어때요?',
  updated_at = now()
WHERE scenario_question_id = 46 AND target_locale = 'EN' AND base_locale = 'KR';

UPDATE scenario_question_language_variant SET
  question_text = 'By the way, are you from around here, or just visiting?',
  question_translation = '그런데 이 근처 사세요, 아니면 여행 중이세요?',
  updated_at = now()
WHERE scenario_question_id = 47 AND target_locale = 'EN' AND base_locale = 'KR';

UPDATE scenario_question_language_variant SET
  question_text = 'So… could I get your Instagram? I''d love to keep in touch.',
  question_translation = '저기… 인스타 알려줄 수 있어요? 계속 연락하고 싶어서요.',
  updated_at = now()
WHERE scenario_question_id = 48 AND target_locale = 'EN' AND base_locale = 'KR';

-- ---------------------------------------------------------------------------
-- Day 10 | 시나리오 20 — 여행 수다 (q 58~60)
-- ---------------------------------------------------------------------------
UPDATE scenario_question_language_variant SET
  question_text = 'I just got back from Portugal and it was unreal. What about you — what''s the best place you''ve ever been?',
  question_translation = '나 포르투갈 갔다 왔는데 진짜 미쳤었어. 넌 어때 — 여태 가본 곳 중에 어디가 최고였어?',
  updated_at = now()
WHERE scenario_question_id = 58 AND target_locale = 'EN' AND base_locale = 'KR';

UPDATE scenario_question_language_variant SET
  question_text = 'Tell me about a trip that went wrong — anything counts, even a school trip. Those stories are the best.',
  question_translation = '망했던 여행 얘기 해줘 — 수학여행도 좋아, 뭐든. 그런 얘기가 제일 재밌잖아.',
  updated_at = now()
WHERE scenario_question_id = 59 AND target_locale = 'EN' AND base_locale = 'KR';

UPDATE scenario_question_language_variant SET
  question_text = 'If money wasn''t an issue, where would you go next?',
  question_translation = '돈 걱정 없으면 다음엔 어디 가고 싶어?',
  updated_at = now()
WHERE scenario_question_id = 60 AND target_locale = 'EN' AND base_locale = 'KR';

-- ---------------------------------------------------------------------------
-- Day 8 | 시나리오 8 — 첫 수업, 옆자리 Marco (q 22~24)
-- ---------------------------------------------------------------------------
UPDATE scenario_question_language_variant SET
  question_text = 'Hey, is anyone sitting here? Mind if I sit down? I''m Marco, by the way.',
  question_translation = '안녕, 여기 자리 있어? 앉아도 될까? 아, 난 Marco야.',
  updated_at = now()
WHERE scenario_question_id = 22 AND target_locale = 'EN' AND base_locale = 'KR';

UPDATE scenario_question_language_variant SET
  question_text = 'So why''d you take this class? I''m kind of regretting it already, haha.',
  question_translation = '근데 이 수업 왜 신청했어? 난 벌써 좀 후회 중이야 ㅋㅋ',
  updated_at = now()
WHERE scenario_question_id = 23 AND target_locale = 'EN' AND base_locale = 'KR';

UPDATE scenario_question_language_variant SET
  question_text = 'What''s school like in Korea? I''ve heard it''s pretty intense.',
  question_translation = '한국 학교는 어때? 엄청 빡세다고 들었는데.',
  updated_at = now()
WHERE scenario_question_id = 24 AND target_locale = 'EN' AND base_locale = 'KR';

-- ---------------------------------------------------------------------------
-- Day 12 | 시나리오 11 — 시험 기간 (q 31~33)
-- ---------------------------------------------------------------------------
UPDATE scenario_question_language_variant SET
  question_text = 'I can''t believe finals are next week. Do you cram everything the night before, or are you one of those people who plans ahead?',
  question_translation = '다음 주가 기말이라니 말도 안 돼. 넌 전날 밤에 몰아서 하는 타입이야, 미리미리 하는 타입이야?',
  updated_at = now()
WHERE scenario_question_id = 31 AND target_locale = 'EN' AND base_locale = 'KR';

UPDATE scenario_question_language_variant SET
  question_text = 'I''m so lost on chapter five. Could you help me after class? Are you good at explaining stuff?',
  question_translation = '나 5챕터 완전 모르겠어. 수업 끝나고 나 좀 도와줄 수 있어? 너 설명 잘해주는 편이야?',
  updated_at = now()
WHERE scenario_question_id = 32 AND target_locale = 'EN' AND base_locale = 'KR';

UPDATE scenario_question_language_variant SET
  question_text = 'We should study together. Where do you usually study, and when are you free?',
  question_translation = '우리 같이 공부하자. 넌 보통 어디서 공부해? 언제 시간 돼?',
  updated_at = now()
WHERE scenario_question_id = 33 AND target_locale = 'EN' AND base_locale = 'KR';

-- ---------------------------------------------------------------------------
-- Day 14 | 시나리오 5 — 룸메이트 룰 정하기 (q 13~15)
-- ---------------------------------------------------------------------------
UPDATE scenario_question_language_variant SET
  question_text = 'Hey, heads up — I was gonna have a couple friends over at our room on Friday. That''s cool with you, right?',
  question_translation = '아 맞다, 미리 말해두는데 — 금요일에 우리 기숙사 방에 친구 몇 명 부르려고. 너 괜찮지?',
  updated_at = now()
WHERE scenario_question_id = 13 AND target_locale = 'EN' AND base_locale = 'KR';

UPDATE scenario_question_language_variant SET
  question_text = 'Oh, and I used your charger yesterday — mine died and it was kind of an emergency. Is it okay if I use your stuff like that sometimes, or would you rather I ask every time?',
  question_translation = '아, 그리고 어제 네 충전기 썼어 — 내 게 방전됐는데 너무 급했거든. 가끔 그렇게 네 물건 써도 돼? 아니면 매번 물어보는 게 좋아?',
  updated_at = now()
WHERE scenario_question_id = 14 AND target_locale = 'EN' AND base_locale = 'KR';

UPDATE scenario_question_language_variant SET
  question_text = 'What do you like the room to be like on weeknights? Music on, totally quiet...?',
  question_translation = '평일 밤엔 방 분위기 어떤 게 좋아? 음악 틀어놓는 거? 완전 조용한 거?',
  updated_at = now()
WHERE scenario_question_id = 15 AND target_locale = 'EN' AND base_locale = 'KR';

-- ---------------------------------------------------------------------------
-- Day 16 | 시나리오 2 — 룸메이트 생활 습관 (q 4~6)
-- ---------------------------------------------------------------------------
UPDATE scenario_question_language_variant SET
  question_text = 'Are you a clean person? Like, how often do you actually clean your room?',
  question_translation = '너 깔끔한 편이야? 방 청소는 실제로 얼마나 자주 해?',
  updated_at = now()
WHERE scenario_question_id = 4 AND target_locale = 'EN' AND base_locale = 'KR';

UPDATE scenario_question_language_variant SET
  question_text = 'How do you wanna split the chores? You don''t mind doing dishes, do you?',
  question_translation = '집안일은 어떻게 나눌까? 너 설거지 하는 거 싫진 않지?',
  updated_at = now()
WHERE scenario_question_id = 5 AND target_locale = 'EN' AND base_locale = 'KR';

UPDATE scenario_question_language_variant SET
  question_text = 'Are you a morning person or a night owl? I stay up way too late, so I''m a little worried, haha.',
  question_translation = '넌 아침형이야, 야행성이야? 난 맨날 엄청 늦게 자서 좀 걱정되네 ㅋㅋ',
  updated_at = now()
WHERE scenario_question_id = 6 AND target_locale = 'EN' AND base_locale = 'KR';

-- ---------------------------------------------------------------------------
-- Day 18 | 시나리오 9 — 조별 발표 준비 (q 25~27)
-- ---------------------------------------------------------------------------
UPDATE scenario_question_language_variant SET
  question_text = 'So we''ve got the presentation next week. Which part do you want to take? Honestly, I''m terrified of public speaking.',
  question_translation = '우리 다음 주에 발표 있잖아. 넌 어떤 파트 하고 싶어? 난 솔직히 발표 공포증이 좀 있어.',
  updated_at = now()
WHERE scenario_question_id = 25 AND target_locale = 'EN' AND base_locale = 'KR';

UPDATE scenario_question_language_variant SET
  question_text = 'Have you ever presented in English before? How did it go?',
  question_translation = '너 영어로 발표해본 적 있어? 어땠어?',
  updated_at = now()
WHERE scenario_question_id = 26 AND target_locale = 'EN' AND base_locale = 'KR';

UPDATE scenario_question_language_variant SET
  question_text = 'When should we meet up to practice? How''s your week looking?',
  question_translation = '연습은 언제 만나서 할까? 이번 주 일정 어때?',
  updated_at = now()
WHERE scenario_question_id = 27 AND target_locale = 'EN' AND base_locale = 'KR';

-- ---------------------------------------------------------------------------
-- Day 22 | 시나리오 12 — 토론 수업 (q 34~36)
-- ---------------------------------------------------------------------------
UPDATE scenario_question_language_variant SET
  question_text = 'So today''s topic is whether money can buy happiness. What do you think — can it?',
  question_translation = '오늘 주제는 ''돈으로 행복을 살 수 있는가''야. 넌 어떻게 생각해 — 살 수 있을 것 같아?',
  updated_at = now()
WHERE scenario_question_id = 34 AND target_locale = 'EN' AND base_locale = 'KR';

UPDATE scenario_question_language_variant SET
  question_text = 'Honestly, I feel like people who say money can''t buy happiness have just never been broke. Am I wrong?',
  question_translation = '솔직히 돈으로 행복 못 산다는 사람들은 진짜 돈 없어본 적이 없는 것 같아. 내 말이 틀렸어?',
  updated_at = now()
WHERE scenario_question_id = 35 AND target_locale = 'EN' AND base_locale = 'KR';

UPDATE scenario_question_language_variant SET
  question_text = 'Okay, say you won a million dollars tomorrow. Do you think you''d actually be happier a year from now?',
  question_translation = '그럼 내일 백만 달러에 당첨됐다고 쳐봐. 1년 뒤에 진짜 더 행복해져 있을 것 같아?',
  updated_at = now()
WHERE scenario_question_id = 36 AND target_locale = 'EN' AND base_locale = 'KR';

-- ---------------------------------------------------------------------------
-- Day 39 | 시나리오 7 — 친구와 깊은 대화 (q 19~21)
-- ---------------------------------------------------------------------------
UPDATE scenario_question_language_variant SET
  question_text = 'Do you have any siblings? I''m an only child, so I''ve always wondered what that''s like.',
  question_translation = '너 형제자매 있어? 난 외동이라 그게 어떤 건지 항상 궁금했거든.',
  updated_at = now()
WHERE scenario_question_id = 19 AND target_locale = 'EN' AND base_locale = 'KR';

UPDATE scenario_question_language_variant SET
  question_text = 'What do you actually want to do with your life? Like, your real dream — not the one you tell professors.',
  question_translation = '넌 진짜 하고 싶은 게 뭐야? 교수님한테 말하는 거 말고, 진짜 꿈.',
  updated_at = now()
WHERE scenario_question_id = 20 AND target_locale = 'EN' AND base_locale = 'KR';

UPDATE scenario_question_language_variant SET
  question_text = 'Hey, you''ve seemed a little down lately. Is everything okay?',
  question_translation = '야, 너 요즘 좀 기운 없어 보여. 무슨 일 있어?',
  updated_at = now()
WHERE scenario_question_id = 21 AND target_locale = 'EN' AND base_locale = 'KR';

-- ---------------------------------------------------------------------------
-- Day 27 | 시나리오 13 — 비행기 옆자리 (q 37~39)
-- ---------------------------------------------------------------------------
UPDATE scenario_question_language_variant SET
  question_text = 'Hi! I''m in 23B, right next to you. Looks like we''re stuck together for the next eleven hours, haha. Where are you headed?',
  question_translation = '안녕하세요! 저 23B, 바로 옆자리예요. 앞으로 11시간 동안 붙어 가겠네요 ㅎㅎ 어디까지 가세요?',
  updated_at = now()
WHERE scenario_question_id = 37 AND target_locale = 'EN' AND base_locale = 'KR';

UPDATE scenario_question_language_variant SET
  question_text = 'Do you usually go for the window seat or the aisle? I always fight for the window.',
  question_translation = '보통 창가 좌석 좋아하세요, 복도 좌석 좋아하세요? 전 무조건 창가파거든요.',
  updated_at = now()
WHERE scenario_question_id = 38 AND target_locale = 'EN' AND base_locale = 'KR';

UPDATE scenario_question_language_variant SET
  question_text = 'What''s the best trip you''ve ever been on?',
  question_translation = '여태까지 한 여행 중에 뭐가 제일 좋았어요?',
  updated_at = now()
WHERE scenario_question_id = 39 AND target_locale = 'EN' AND base_locale = 'KR';

-- ---------------------------------------------------------------------------
-- Day 28 | 시나리오 15 — 호텔 체크인 (q 43~45)
-- ---------------------------------------------------------------------------
UPDATE scenario_question_language_variant SET
  question_text = 'Hi, welcome! Are you checking in? Can I get your name, please?',
  question_translation = '안녕하세요, 어서 오세요! 체크인이신가요? 성함 말씀해 주시겠어요?',
  updated_at = now()
WHERE scenario_question_id = 43 AND target_locale = 'EN' AND base_locale = 'KR';

UPDATE scenario_question_language_variant SET
  question_text = 'I''m so sorry, but your room won''t be ready for another hour or so. Would that be alright?',
  question_translation = '정말 죄송한데, 객실 준비가 한 시간 정도 더 걸릴 것 같아요. 괜찮으시겠어요?',
  updated_at = now()
WHERE scenario_question_id = 44 AND target_locale = 'EN' AND base_locale = 'KR';

UPDATE scenario_question_language_variant SET
  question_text = 'Do you have any preferences for your room? Higher floor, away from the elevator, anything like that.',
  question_translation = '객실 관련해서 원하시는 거 있으세요? 높은 층이라든지, 엘리베이터에서 먼 방이라든지요.',
  updated_at = now()
WHERE scenario_question_id = 45 AND target_locale = 'EN' AND base_locale = 'KR';

-- ---------------------------------------------------------------------------
-- Day 31 | 시나리오 19 — 런던 현지인 (q 55~57)
-- ---------------------------------------------------------------------------
UPDATE scenario_question_language_variant SET
  question_text = 'Is this your first time in London? How are you finding it?',
  question_translation = '런던은 처음이세요? 지내보니까 어때요?',
  updated_at = now()
WHERE scenario_question_id = 55 AND target_locale = 'EN' AND base_locale = 'KR';

UPDATE scenario_question_language_variant SET
  question_text = 'If you haven''t eaten yet, I can recommend some great places around here — what are you in the mood for?',
  question_translation = '아직 식사 전이면 이 근처 맛집 제가 추천해 드릴게요 — 뭐가 당기세요?',
  updated_at = now()
WHERE scenario_question_id = 56 AND target_locale = 'EN' AND base_locale = 'KR';

UPDATE scenario_question_language_variant SET
  question_text = 'So where are you visiting from? What''s it like there?',
  question_translation = '그런데 어디서 오셨어요? 거긴 어떤 곳이에요?',
  updated_at = now()
WHERE scenario_question_id = 57 AND target_locale = 'EN' AND base_locale = 'KR';

-- ---------------------------------------------------------------------------
-- Day 33 | 시나리오 18 — 약국 (q 52~54)
-- ---------------------------------------------------------------------------
UPDATE scenario_question_language_variant SET
  question_text = 'Hi, what can I help you with? You don''t look so great — what''s going on?',
  question_translation = '안녕하세요, 뭘 도와드릴까요? 안색이 안 좋아 보이는데 — 어디가 안 좋으세요?',
  updated_at = now()
WHERE scenario_question_id = 52 AND target_locale = 'EN' AND base_locale = 'KR';

UPDATE scenario_question_language_variant SET
  question_text = 'When did it start? Do you have a fever, or is it more of an achy, tired feeling?',
  question_translation = '언제부터 그랬어요? 열이 나요, 아니면 몸살처럼 쑤시고 피곤한 느낌이에요?',
  updated_at = now()
WHERE scenario_question_id = 53 AND target_locale = 'EN' AND base_locale = 'KR';

UPDATE scenario_question_language_variant SET
  question_text = 'Take two of these after meals, no more than six a day. Are you allergic to anything?',
  question_translation = '이거 식후에 두 알씩 드시고, 하루 여섯 알은 넘기지 마세요. 알레르기 있는 거 있어요?',
  updated_at = now()
WHERE scenario_question_id = 54 AND target_locale = 'EN' AND base_locale = 'KR';

-- ---------------------------------------------------------------------------
-- Day 37 | 시나리오 10 — 교수님 면담 (q 28~30)
-- ---------------------------------------------------------------------------
UPDATE scenario_question_language_variant SET
  question_text = 'Come on in, have a seat. So, you emailed me about your grade — what''s on your mind?',
  question_translation = '들어와서 앉아요. 성적 때문에 메일 보냈죠 — 어떤 얘기를 하고 싶어요?',
  updated_at = now()
WHERE scenario_question_id = 28 AND target_locale = 'EN' AND base_locale = 'KR';

UPDATE scenario_question_language_variant SET
  question_text = 'Before I say anything — how did you feel about this essay yourself? Be honest.',
  question_translation = '내가 말하기 전에 — 학생은 이 에세이 스스로 어땠던 것 같아요? 솔직하게요.',
  updated_at = now()
WHERE scenario_question_id = 29 AND target_locale = 'EN' AND base_locale = 'KR';

UPDATE scenario_question_language_variant SET
  question_text = 'Tell you what — I''ll reread it tonight and put together some feedback. Do you want to come by tomorrow, or should I just email you?',
  question_translation = '이렇게 하죠 — 오늘 밤에 다시 읽어보고 피드백 정리해 둘게요. 내일 연구실로 올래요, 아니면 그냥 이메일로 보낼까요?',
  updated_at = now()
WHERE scenario_question_id = 30 AND target_locale = 'EN' AND base_locale = 'KR';

-- ---------------------------------------------------------------------------
-- Day 38 | 시나리오 4 — 기숙사 요금 문제, 프론트에 전화 (q 10~12)
-- 행동 지시: 7월 한 달 여행으로 방을 비웠는데 에어컨 요금 $100 청구.
--            유저가 먼저 전화로 상황을 설명하면 직원(Kate)이 응대.
-- ---------------------------------------------------------------------------
UPDATE scenario_question_language_variant SET
  question_text = 'Ah, I see. Let me check your account… yes, I do see the $100 A/C charge for July. Could you tell me a bit more about when you were away?',
  question_translation = '아, 그러시군요. 계정 확인해 볼게요… 네, 7월 에어컨 요금 100달러 있네요. 언제 방을 비우셨는지 좀 더 자세히 말씀해 주시겠어요?',
  updated_at = now()
WHERE scenario_question_id = 10 AND target_locale = 'EN' AND base_locale = 'KR';

UPDATE scenario_question_language_variant SET
  question_text = 'Got it. We''d just need some proof that you were away — plane tickets, booking confirmations, anything like that. What do you have?',
  question_translation = '알겠습니다. 방을 비우셨다는 증빙이 필요해서요 — 항공권, 예약 확인서 같은 거요. 어떤 게 있으세요?',
  updated_at = now()
WHERE scenario_question_id = 11 AND target_locale = 'EN' AND base_locale = 'KR';

UPDATE scenario_question_language_variant SET
  question_text = 'Perfect. Once we confirm it, we''ll credit it back — it usually takes about two weeks though. Would that be alright?',
  question_translation = '좋아요. 확인되면 환급해 드릴게요 — 다만 보통 2주 정도 걸려요. 괜찮으시겠어요?',
  updated_at = now()
WHERE scenario_question_id = 12 AND target_locale = 'EN' AND base_locale = 'KR';

-- ---------------------------------------------------------------------------
-- Day 36 | 시나리오 14 — 수하물 파손 보상 (q 40~42)
-- ---------------------------------------------------------------------------
UPDATE scenario_question_language_variant SET
  question_text = 'I''m so sorry about that. Could you show me the damage and tell me what happened?',
  question_translation = '정말 죄송합니다. 파손된 부분을 보여주시고 어떤 상황이었는지 말씀해 주시겠어요?',
  updated_at = now()
WHERE scenario_question_id = 40 AND target_locale = 'EN' AND base_locale = 'KR';

UPDATE scenario_question_language_variant SET
  question_text = 'We can offer a cash refund, mileage points, or a travel voucher — the voucher''s worth the most, but it expires in six months. Which would you prefer?',
  question_translation = '보상은 현금 환불, 마일리지, 여행 바우처 중에 선택하실 수 있어요 — 바우처 금액이 제일 크긴 한데 6개월 뒤에 만료돼요. 어떤 걸로 하시겠어요?',
  updated_at = now()
WHERE scenario_question_id = 41 AND target_locale = 'EN' AND base_locale = 'KR';

UPDATE scenario_question_language_variant SET
  question_text = 'Alright, that''s all done. Is there anything else I can help you with?',
  question_translation = '네, 처리 다 됐습니다. 더 도와드릴 건 없으세요?',
  updated_at = now()
WHERE scenario_question_id = 42 AND target_locale = 'EN' AND base_locale = 'KR';

-- 반영 건수 확인 후 커밋하세요 (총 60건이어야 함)

-- =============================================================================
-- [참고] 시나리오 테이블 쪽 변경 사항 — 해당 테이블 덤프가 없어 주석으로 남깁니다.
-- 실제 컬럼명(display_order, title, ai_role 등)에 맞춰 별도 적용하세요.
--
-- 1) 노출 순서 (Day 1~20 순서대로 scenario_id):
--    1, 3, 17, 6, 16, 20, 8, 11, 5, 2, 9, 12, 7, 13, 15, 19, 18, 10, 4, 14
--
-- 2) 캐릭터 통일 (tts_voice_id: 1=Chloe, 2=Marco):
--    - 전 시나리오 AI 역할 설명에서 이름 제거, 직무/성격만 유지
--    - 제목 변경:
--        시나리오 1: "...룸메이트 Charlie와 첫 만남" -> "...룸메이트 Marco와 첫 만남"
--        시나리오 6: "...처음 만난 Hailey"          -> "...처음 만난 Chloe"
--        시나리오 8: "첫 수업, 옆자리 Jordan"        -> "첫 수업, 옆자리 Marco"
--    - 질문 텍스트 내 캐릭터명은 위 UPDATE에 반영됨 (q1: Marco, q16: Chloe, q22: Marco)
--
-- 3) 시나리오 4 재정의: 통신사 환불(X) -> 기숙사 요금 문제, 프론트 전화 (직원 Kate)
--    유저가 먼저 말 걸기 시나리오. 제목/행동지시/역할 설명도 이에 맞게 갱신 필요.
--
-- 4) inner_thought: 최종 질문 기준으로 전건 정합 확인 완료, 변경 불필요.
-- =============================================================================
