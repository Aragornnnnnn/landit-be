-- LAN-269 시나리오 썸네일 40개와 신규 Writing 표현 80개 및 연습 예문 320개를 반영한다.

LOCK TABLE writing_expression IN SHARE ROW EXCLUSIVE MODE;

DO $$
BEGIN
  IF (SELECT COUNT(*) FROM scenario WHERE id BETWEEN 1 AND 40) <> 40 THEN
    RAISE EXCEPTION 'LAN-269 requires scenario ids 1 through 40';
  END IF;

  IF EXISTS (SELECT 1 FROM writing_expression WHERE id BETWEEN 84 AND 163) THEN
    RAISE EXCEPTION 'LAN-269 writing expression ids 84 through 163 must be unused';
  END IF;

  IF EXISTS (
      SELECT 1
      FROM writing_expression
      WHERE scenario_id BETWEEN 21 AND 40
        AND target_locale = 'EN'
        AND base_locale = 'KR'
        AND display_order BETWEEN 1 AND 4
  ) THEN
    RAISE EXCEPTION 'LAN-269 scenarios 21 through 40 must not already have EN/KR writing expressions';
  END IF;
END $$;

UPDATE scenario
SET thumbnail_url = CASE id
    WHEN 1 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/1/thumbnail/36298fa4-cf4d-4593-baa3-0cface4d5487.png'
    WHEN 2 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/2/thumbnail/7d94721e-5176-4d3c-bae5-0dc09a6daa06.png'
    WHEN 3 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/3/thumbnail/5bafb0cc-4601-4aa6-be59-94031d36577f.png'
    WHEN 4 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/4/thumbnail/203a811e-b7c2-4d43-bb87-49930789c1a3.png'
    WHEN 5 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/5/thumbnail/96e97266-6fec-4fdc-b7f5-fbfc866f8d77.png'
    WHEN 6 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/6/thumbnail/f696c4e3-dd9e-454c-b314-4a2279858cb0.png'
    WHEN 7 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/7/thumbnail/8aecccfc-7c7a-4c35-a2c5-fb5455df2336.png'
    WHEN 8 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/8/thumbnail/a52e5cca-ffad-4ada-9875-8b21e16f0d59.png'
    WHEN 9 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/9/thumbnail/06911aca-7e10-4cbb-8382-9a78fa7d5bf6.png'
    WHEN 10 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/10/thumbnail/b4241b3d-376a-4304-a6e6-46f399d1d7e1.png'
    WHEN 11 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/11/thumbnail/b616012b-4cd8-4bfb-a639-e4e2169b9e5f.png'
    WHEN 12 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/12/thumbnail/345dd261-a873-4c71-bb60-e149dfbc2f98.png'
    WHEN 13 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/13/thumbnail/fe412149-231d-44ed-bbc1-fbfd6e1d6ff0.png'
    WHEN 14 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/14/thumbnail/5cee88ea-0b51-4d0b-8db8-5bea066be890.png'
    WHEN 15 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/15/thumbnail/f84f6219-d127-4ad1-a7ee-0aeead49a956.png'
    WHEN 16 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/16/thumbnail/b4b0a8b8-ee4f-406d-93f6-0d091a3c2e26.png'
    WHEN 17 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/17/thumbnail/ff734ab8-ce38-4091-a8bb-0f4e6e1427aa.png'
    WHEN 18 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/18/thumbnail/c05f665e-f20c-471a-add9-40874fc836a9.png'
    WHEN 19 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/19/thumbnail/398d3f09-ff0c-4790-afed-71fa36f2d623.png'
    WHEN 20 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/20/thumbnail/c0e367ca-d765-4a31-b214-f5476ef5aa22.png'
    WHEN 21 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/21/thumbnail/4b0002fd-d247-458f-b891-bf650809d9db.png'
    WHEN 22 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/22/thumbnail/122f0504-4276-47ef-b303-76bd32a49470.png'
    WHEN 23 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/23/thumbnail/b7507615-1e10-4e46-9648-8830b04e951f.png'
    WHEN 24 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/24/thumbnail/8edf240f-9f2c-40ba-9bcf-c3d8332a2345.png'
    WHEN 25 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/25/thumbnail/5e6271bc-4f33-4e61-9fb1-f318f422e4c3.png'
    WHEN 26 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/26/thumbnail/07156a13-417a-497a-bf5f-2b075827b67d.png'
    WHEN 27 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/27/thumbnail/d81b51d3-cd25-4aec-89b6-479ef5f2fef9.png'
    WHEN 28 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/28/thumbnail/d8acab09-b1a4-45ab-ad07-b747d2bd505a.png'
    WHEN 29 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/29/thumbnail/670da0c3-bf53-4f56-bb6c-39743c322908.png'
    WHEN 30 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/30/thumbnail/3559cc1f-dd19-40fc-9094-879e08096825.png'
    WHEN 31 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/31/thumbnail/1690bcbd-fdd5-4424-859a-e5e719d6c8d2.png'
    WHEN 32 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/32/thumbnail/3e57a3e6-b3ac-4daf-b1a3-0aafbd8db878.png'
    WHEN 33 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/33/thumbnail/db0c9636-38a6-482c-bcf2-c9c1b5d6444c.png'
    WHEN 34 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/34/thumbnail/7374337d-3dc4-4db3-88e2-d2a9be151ad2.png'
    WHEN 35 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/35/thumbnail/caad1e9f-d97c-45cb-bee4-ff5f1f007a70.png'
    WHEN 36 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/36/thumbnail/0f20e65f-25d4-4bbd-9cb6-d0cd0c0737dc.png'
    WHEN 37 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/37/thumbnail/0ab5afdd-8611-4bdf-b2d1-73bed2192ac4.png'
    WHEN 38 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/38/thumbnail/4f420c09-892a-4247-a668-aab74e6a5fc6.png'
    WHEN 39 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/39/thumbnail/01e49e83-6283-4d1d-bb72-ca3917b861d1.png'
    WHEN 40 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/40/thumbnail/438835a6-1d43-4ae1-82d5-6e4acc0fa93c.png'
    ELSE thumbnail_url
  END,
  updated_at = CURRENT_TIMESTAMP
WHERE id BETWEEN 1 AND 40;

-- 표현 ID는 DB 시나리오 21~40에 대해 84~163을 고정 사용하며 대표 이미지는 NULL로 유지한다.

INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        84,
        21,
        'SCENARIO',
        'TRAVEL_MOVEMENT',
        'BASIC',
        'EN',
        'KR',
        1,
        'be locked out of',
        '(열쇠 없이) ~밖에 잠겨버리다',
        '문이 잠겨버렸을 때 be locked out of',
        '''(열쇠 없이) ~밖에 잠겨버리다''라는 뜻입니다. 열쇠를 안에 두고 문이 잠긴 난감한 상황을 한 방에 설명해 주는 표현이에요.',
        'Why are you waiting outside?',
        '왜 밖에서 기다려?',
        'I''m locked out of my house.',
        '집 문이 잠겨서 못 들어가고 있어.',
        ARRAY['I''m', 'locked', 'out', 'of', 'my', 'house'],
        ARRAY['I''m', 'locking', 'of', 'out', 'lock', 'house', 'locks', 'my', 'locked'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/21/expressions/84/practice-examples/18190625-893f-40c0-bd68-2322f725eb04.png","sentenceText":"She locked herself out of her apartment.","sentenceWords":["She","locked","herself","out","of","her","apartment"],"highlightingPart":"locked herself out of","practiceQuestion":"Why is she waiting outside her apartment?","sentenceTranslation":"걔 열쇠를 안에 두고 나와서 못 들어가.","sentenceWordChoices":["lock","of","locked","out","herself","locks","apartment","She","locking","her"],"practiceQuestionTranslation":"걔는 왜 아파트 밖에서 기다리고 있어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/21/expressions/84/practice-examples/f47539a7-41ec-489f-a97c-464c54e46584.png","sentenceText":"I got locked out of my email account.","sentenceWords":["I","got","locked","out","of","my","email","account"],"highlightingPart":"got locked out of","practiceQuestion":"Why can''t you get into your email?","sentenceTranslation":"이메일 계정이 잠겨버렸어.","sentenceWordChoices":["out","account","email","locked","you","got","of","he","I","my","we"],"practiceQuestionTranslation":"왜 이메일에 들어가지 못해?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/21/expressions/84/practice-examples/5a9eaf33-6cc3-4968-aa53-8d3eaf0a5cea.png","sentenceText":"Don''t lock yourself out again!","sentenceWords":["Don''t","lock","yourself","out","again"],"highlightingPart":"lock yourself out","practiceQuestion":"What should I be careful not to do again?","sentenceTranslation":"또 열쇠 두고 나오지 마!","sentenceWordChoices":["again","Don''t","out","lock","locking","yourself","locked","locks"],"practiceQuestionTranslation":"다시는 어떤 실수를 하지 않게 조심해야 해?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/21/expressions/84/practice-examples/a7989cf1-1319-47f7-9900-c305a0ed8042.png","sentenceText":"We were locked out of the office all morning.","sentenceWords":["We","were","locked","out","of","the","office","all","morning"],"highlightingPart":"were locked out of","practiceQuestion":"Why couldn''t you get into the office?","sentenceTranslation":"오전 내내 사무실에 못 들어갔어.","sentenceWordChoices":["the","I","all","they","We","were","out","locked","you","office","morning","of"],"practiceQuestionTranslation":"왜 사무실에 들어가지 못했어?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        85,
        21,
        'SCENARIO',
        'DAILY_ROUTINE',
        'BASIC',
        'EN',
        'KR',
        2,
        'forget ~ at',
        '~을 …에 두고 오다',
        '물건을 두고 왔을 때 forget ~ at',
        '''~을 …에 두고 왔다''라고 할 때 원어민은 forget을 씁니다. leave도 가능하지만, forgot my wallet at home처럼 forget + 장소로 말하는 게 아주 자연스러워요.',
        'Everything''s on sale today!',
        '오늘 전부 세일이야!',
        'I forgot my wallet at home.',
        '지갑을 집에 두고 왔어.',
        ARRAY['I', 'forgot', 'my', 'wallet', 'at', 'home'],
        ARRAY['he', 'we', 'you', 'wallet', 'my', 'forgot', 'at', 'home', 'I'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/21/expressions/85/practice-examples/bdec9d26-356e-4616-b34d-3a520107c83c.png","sentenceText":"I forgot my umbrella at the cafe.","sentenceWords":["I","forgot","my","umbrella","at","the","cafe"],"highlightingPart":"forgot my umbrella at","practiceQuestion":"Where''s your umbrella?","sentenceTranslation":"우산을 카페에 두고 왔어.","sentenceWordChoices":["my","umbrella","the","I","he","you","forgot","we","cafe","at"],"practiceQuestionTranslation":"우산 어디 있어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/21/expressions/85/practice-examples/819d28f4-3df2-4abe-ab44-1887be034758.png","sentenceText":"She forgot her keys at the office.","sentenceWords":["She","forgot","her","keys","at","the","office"],"highlightingPart":"forgot her keys at","practiceQuestion":"Why doesn''t she have her keys?","sentenceTranslation":"걔 열쇠를 사무실에 두고 왔대.","sentenceWordChoices":["forgets","at","forget","left","forgot","office","her","She","the","keys"],"practiceQuestionTranslation":"걔는 왜 열쇠가 없어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/21/expressions/85/practice-examples/278d7bab-7e85-4e12-b54e-68a22e90c8d8.png","sentenceText":"Did you forget your gym bag at home again?","sentenceWords":["Did","you","forget","your","gym","bag","at","home","again"],"highlightingPart":"forget your gym bag at","practiceQuestion":"Your gym bag isn''t here.","sentenceTranslation":"운동 가방 또 집에 두고 왔어?","sentenceWordChoices":["Did","home","gym","does","you","do","forget","done","bag","again","at","your"],"practiceQuestionTranslation":"네 운동 가방이 여기 없네."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/21/expressions/85/practice-examples/4a07578e-2966-41bb-bf1b-552055485f1f.png","sentenceText":"I almost forgot my phone at the restaurant.","sentenceWords":["I","almost","forgot","my","phone","at","the","restaurant"],"highlightingPart":"forgot my phone at","practiceQuestion":"Did you leave anything at the restaurant?","sentenceTranslation":"하마터면 폰을 식당에 두고 올 뻔했어.","sentenceWordChoices":["he","you","I","the","restaurant","my","at","phone","we","forgot","almost"],"practiceQuestionTranslation":"식당에 두고 온 물건은 없어?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        86,
        21,
        'SCENARIO',
        'POLITE_EXPRESSION',
        'BASIC',
        'EN',
        'KR',
        3,
        'Would it be possible to ~?',
        '혹시 ~가 가능할까요?',
        '가능 여부를 묻는 Would it be possible to ~?',
        '''혹시 ~가 가능할까요?''라고 요청 자체의 가능 여부를 묻는 형태라, 상대가 거절해도 서로 부담이 없는 정중한 표현입니다. 호텔, 식당, 회사 등 서비스·업무 요청에서 특히 많이 쓰여요.',
        'Front desk, how can I help you?',
        '프런트입니다, 무엇을 도와드릴까요?',
        'Would it be possible to get a late checkout?',
        '혹시 레이트 체크아웃이 가능할까요?',
        ARRAY['Would', 'it', 'be', 'possible', 'to', 'get', 'a', 'late', 'checkout'],
        ARRAY['get', 'should', 'to', 'it', 'will', 'a', 'possible', 'could', 'be', 'late', 'Would', 'checkout'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/21/expressions/86/practice-examples/b9809a63-5725-4e96-b8fe-4d533e840b8e.png","sentenceText":"Would it be possible to change my reservation?","sentenceWords":["Would","it","be","possible","to","change","my","reservation"],"highlightingPart":"possible","practiceQuestion":"I need to move my booking to another day.","sentenceTranslation":"예약을 변경할 수 있을까요?","sentenceWordChoices":["change","Would","to","be","could","it","should","reservation","possible","will","my"],"practiceQuestionTranslation":"예약을 다른 날로 옮겨야 해요."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/21/expressions/86/practice-examples/906355fa-a391-4a48-a51b-1a7ebb549164.png","sentenceText":"Would it be possible to get a window seat?","sentenceWords":["Would","it","be","possible","to","get","a","window","seat"],"highlightingPart":"possible","practiceQuestion":"Do you have any seating requests?","sentenceTranslation":"창가 자리로 가능할까요?","sentenceWordChoices":["possible","seat","get","could","be","to","a","it","should","Would","window","will"],"practiceQuestionTranslation":"원하는 좌석이 있으세요?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/21/expressions/86/practice-examples/4cdaa211-ad48-45fb-bb22-79fbc342a321.png","sentenceText":"Would it be possible to reschedule the meeting?","sentenceWords":["Would","it","be","possible","to","reschedule","the","meeting"],"highlightingPart":"possible","practiceQuestion":"The current meeting time no longer works for me.","sentenceTranslation":"회의 일정을 다시 잡을 수 있을까요?","sentenceWordChoices":["meeting","Would","reschedule","to","should","could","will","be","possible","the","it"],"practiceQuestionTranslation":"지금 회의 시간에는 참석하기 어려워요."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/21/expressions/86/practice-examples/8fbd30ad-5477-4053-9107-d18d22a5dcff.png","sentenceText":"Would it be possible to pay in cash?","sentenceWords":["Would","it","be","possible","to","pay","in","cash"],"highlightingPart":"possible","practiceQuestion":"How would you like to pay?","sentenceTranslation":"현금으로 계산해도 될까요?","sentenceWordChoices":["to","pay","cash","could","will","be","Would","it","in","should","possible"],"practiceQuestionTranslation":"어떻게 결제하시겠어요?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        87,
        21,
        'SCENARIO',
        'TIME_PLANNING',
        'BASIC',
        'EN',
        'KR',
        4,
        'for now',
        '일단은, 당분간은',
        '''일단은''이라고 말할 때 쓰는 for now',
        '당장 완벽한 답이 아니어도 우선은 이렇게 하자고 할 때 쓰는 표현입니다. ''지금으로서는'', ''당분간은''이라는 뉘앙스로, 나중에 바뀔 수 있음을 자연스럽게 깔아줘요.',
        'Is that everything?',
        '그게 다야?',
        'That''s all for now.',
        '일단은 여기까지야.',
        ARRAY['That''s', 'all', 'for', 'now'],
        ARRAY['That''s', 'for', 'to', 'of', 'all', 'with', 'now'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/21/expressions/87/practice-examples/378dd6b2-d5e7-42d1-bcc4-407319265c1b.png","sentenceText":"Let''s leave it here for now.","sentenceWords":["Let''s","leave","it","here","for","now"],"highlightingPart":"for now","practiceQuestion":"Where should we put this?","sentenceTranslation":"일단은 여기 놔두자.","sentenceWordChoices":["it","leaveed","for","Let''s","now","leave","here","leaveing","leaves"],"practiceQuestionTranslation":"이걸 어디에 둘까?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/21/expressions/87/practice-examples/a0ff0d84-4faa-4f56-9948-d06cca160c29.png","sentenceText":"For now, just focus on resting.","sentenceWords":["For","now","just","focus","on","resting"],"highlightingPart":"For now","practiceQuestion":"What should I focus on while I recover?","sentenceTranslation":"지금은 그냥 쉬는 데만 집중해.","sentenceWordChoices":["just","to","resting","with","of","on","now","For","focus"],"practiceQuestionTranslation":"회복하는 동안 무엇에 집중해야 할까?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/21/expressions/87/practice-examples/c2874737-d2a8-4566-af8f-e13bfb792bf4.png","sentenceText":"I''m staying at my parents'' place for now.","sentenceWords":["I''m","staying","at","my","parents","place","for","now"],"highlightingPart":"for now","practiceQuestion":"Where are you staying these days?","sentenceTranslation":"당분간은 부모님 집에 지내고 있어.","sentenceWordChoices":["stays","stay","parents","I''m","now","for","staying","at","my","stayed","place"],"practiceQuestionTranslation":"요즘 어디에서 지내고 있어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/21/expressions/87/practice-examples/88309afa-02fc-4e12-b224-fc619524052d.png","sentenceText":"We''ll skip the details for now.","sentenceWords":["We''ll","skip","the","details","for","now"],"highlightingPart":"for now","practiceQuestion":"Do we need to discuss every detail now?","sentenceTranslation":"세부적인 건 일단 넘어갈게요.","sentenceWordChoices":["skiped","now","skips","details","We''ll","skip","skiping","for","the"],"practiceQuestionTranslation":"지금 세부 사항까지 전부 얘기해야 해요?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        88,
        22,
        'SCENARIO',
        'GRAMMAR_FUNCTION_WORD',
        'BASIC',
        'EN',
        'KR',
        1,
        'room for',
        '~을 위한 여지/자리',
        '여유 공간을 말하는 room for',
        'room은 ''방''뿐 아니라 ''여유, 공간''을 뜻해서, room for는 ''~을 위한 여지/자리''가 됩니다. 디저트 배부터 개선의 여지까지 폭넓게 쓰여요.',
        'Can this table fit another person?',
        '이 테이블에 한 명 더 앉을 수 있어?',
        'Is there room for one more?',
        '한 명 더 낄 자리 있어?',
        ARRAY['Is', 'there', 'room', 'for', 'one', 'more'],
        ARRAY['room', 'be', 'more', 'Is', 'for', 'there', 'one', 'was', 'are'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/22/expressions/88/practice-examples/16b49210-9025-438b-bc14-1bce1e2459d9.png","sentenceText":"Save room for dessert!","sentenceWords":["Save","room","for","dessert"],"highlightingPart":"room for","practiceQuestion":"I''m full, but the cake looks good.","sentenceTranslation":"디저트 먹을 배는 남겨놔!","sentenceWordChoices":["for","room","Save","Saves","Saveed","Saveing","dessert"],"practiceQuestionTranslation":"배부른데 케이크가 맛있어 보여."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/22/expressions/88/practice-examples/0ed15343-ff53-4297-bb12-71a5e39449c3.png","sentenceText":"There''s no room for error.","sentenceWords":["There''s","no","room","for","error"],"highlightingPart":"room for","practiceQuestion":"Can we afford to make a mistake?","sentenceTranslation":"실수하면 안 돼.","sentenceWordChoices":["room","There''s","error","for","no","rooming","rooms","roomed"],"practiceQuestionTranslation":"우리 실수해도 괜찮아?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/22/expressions/88/practice-examples/9b369786-016b-44de-950a-ac6d7abcb228.png","sentenceText":"There''s still room for improvement.","sentenceWords":["There''s","still","room","for","improvement"],"highlightingPart":"room for","practiceQuestion":"Is this already as good as it can be?","sentenceTranslation":"아직 개선할 여지가 있어.","sentenceWordChoices":["stilled","stilling","for","improvement","There''s","stills","room","still"],"practiceQuestionTranslation":"이게 이미 최선인 걸까?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/22/expressions/88/practice-examples/6683ad4c-ff5e-48f1-ba01-f8a31837bef7.png","sentenceText":"Do we have room for another suitcase?","sentenceWords":["Do","we","have","room","for","another","suitcase"],"highlightingPart":"room for","practiceQuestion":"I need to bring one more suitcase.","sentenceTranslation":"캐리어 하나 더 들어갈 공간 있어?","sentenceWordChoices":["does","did","another","we","room","have","for","suitcase","Do","doing"],"practiceQuestionTranslation":"캐리어를 하나 더 가져가야 해."}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        89,
        22,
        'SCENARIO',
        'DAILY_ROUTINE',
        'BASIC',
        'EN',
        'KR',
        2,
        'throw away',
        '내다 버리다',
        '버리는 것을 말하는 throw away',
        '''내다 버리다''라는 뜻의 기본 구동사입니다. throw it away처럼 대명사는 throw와 away 사이에 들어간다는 어순이 포인트예요.',
        'Should I keep this receipt?',
        '이 영수증 보관해야 해?',
        'Don''t throw away the receipt.',
        '영수증 버리지 마.',
        ARRAY['Don''t', 'throw', 'away', 'the', 'receipt'],
        ARRAY['the', 'throwed', 'Don''t', 'throws', 'throwing', 'throw', 'receipt', 'away'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/22/expressions/89/practice-examples/b02c93d8-c66b-4a2d-9b16-e0977745dbee.png","sentenceText":"I threw away my old sneakers.","sentenceWords":["I","threw","away","my","old","sneakers"],"highlightingPart":"away","practiceQuestion":"What did you do with your old sneakers?","sentenceTranslation":"낡은 운동화를 버렸어.","sentenceWordChoices":["I","you","my","old","away","we","he","threw","sneakers"],"practiceQuestionTranslation":"낡은 운동화는 어떻게 했어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/22/expressions/89/practice-examples/29919034-8e18-4fc6-b37d-b3bdffa658df.png","sentenceText":"Can you throw this away for me?","sentenceWords":["Can","you","throw","this","away","for","me"],"highlightingPart":"throw this away","practiceQuestion":"I''m taking the trash out now.","sentenceTranslation":"이것 좀 버려줄래?","sentenceWordChoices":["me","you","should","Can","would","could","for","away","throw","this"],"practiceQuestionTranslation":"나 지금 쓰레기 버리러 가."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/22/expressions/89/practice-examples/62153467-54b6-4b91-8203-50fb941afe33.png","sentenceText":"She threw away all the expired food.","sentenceWords":["She","threw","away","all","the","expired","food"],"highlightingPart":"away","practiceQuestion":"What did she do with the expired food?","sentenceTranslation":"걔는 유통기한 지난 음식을 다 버렸어.","sentenceWordChoices":["threw","She","away","all","the","threwed","expired","threws","food","threwing"],"practiceQuestionTranslation":"걔는 유통기한 지난 음식을 어떻게 했어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/22/expressions/89/practice-examples/d6822ece-abb7-4b8d-8c8b-e632aa33a4ae.png","sentenceText":"Don''t throw away this opportunity.","sentenceWords":["Don''t","throw","away","this","opportunity"],"highlightingPart":"throw away","practiceQuestion":"Should I give up this chance?","sentenceTranslation":"이 기회를 날려버리지 마.","sentenceWordChoices":["Don''t","this","throw","away","opportunity","throwing","throwed","throws"],"practiceQuestionTranslation":"이 기회를 포기해야 할까?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        90,
        22,
        'SCENARIO',
        'DAILY_ROUTINE',
        'BASIC',
        'EN',
        'KR',
        3,
        'put away',
        '제자리에 넣다, 치우다',
        '제자리에 정리하는 put away',
        '''(물건을) 제자리에 넣다, 치우다''라는 뜻입니다. 단순히 옮기는 게 아니라 원래 있어야 할 곳에 정리한다는 뉘앙스가 핵심이에요.',
        'Can you clean up before dinner?',
        '저녁 전에 정리 좀 할래?',
        'Put away your toys before dinner.',
        '저녁 먹기 전에 장난감 정리해.',
        ARRAY['Put', 'away', 'your', 'toys', 'before', 'dinner'],
        ARRAY['your', 'dinner', 'awayed', 'before', 'awaying', 'toys', 'Put', 'away', 'aways'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/22/expressions/90/practice-examples/827f7a12-32e2-4dcb-aa99-bd90bf7dcead.png","sentenceText":"I put away the winter clothes.","sentenceWords":["I","put","away","the","winter","clothes"],"highlightingPart":"put away","practiceQuestion":"What did you do with your winter clothes?","sentenceTranslation":"겨울옷을 정리해 넣었어.","sentenceWordChoices":["away","we","I","you","the","put","clothes","he","winter"],"practiceQuestionTranslation":"겨울옷은 어떻게 했어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/22/expressions/90/practice-examples/09860b43-a63c-4586-9c9c-679a4ef6e073.png","sentenceText":"Can you put away the dishes?","sentenceWords":["Can","you","put","away","the","dishes"],"highlightingPart":"put away","practiceQuestion":"The dishes are dry now.","sentenceTranslation":"설거지한 그릇 좀 제자리에 넣어줄래?","sentenceWordChoices":["dishes","could","away","the","should","would","Can","you","put"],"practiceQuestionTranslation":"그릇이 이제 다 말랐어."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/22/expressions/90/practice-examples/e6991238-77b9-4c70-9189-06297b92bff9.png","sentenceText":"He put his phone away and focused.","sentenceWords":["He","put","his","phone","away","and","focused"],"highlightingPart":"put his phone away","practiceQuestion":"How did he stop himself from checking his phone?","sentenceTranslation":"걔는 폰을 치우고 집중했어.","sentenceWordChoices":["phoneing","away","put","his","He","phoneed","phone","focused","phones","and"],"practiceQuestionTranslation":"걔는 어떻게 휴대폰을 안 보고 집중했어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/22/expressions/90/practice-examples/429b963c-c521-4991-bf98-24ee5f2d7020.png","sentenceText":"Put away the groceries first.","sentenceWords":["Put","away","the","groceries","first"],"highlightingPart":"Put away","practiceQuestion":"What should we do with the groceries first?","sentenceTranslation":"장 본 것 먼저 정리해 놔.","sentenceWordChoices":["aways","awaying","away","groceries","the","awayed","Put","first"],"practiceQuestionTranslation":"장 본 물건부터 어떻게 해야 해?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        91,
        22,
        'SCENARIO',
        'WORK_STUDY',
        'BASIC',
        'EN',
        'KR',
        4,
        'make sure',
        '반드시 ~하도록 하다, 확실히 하다',
        '꼭 확인하라는 make sure',
        '''반드시 ~하도록 하다, 확실히 하다''라는 뜻의 필수 표현입니다. 당부하거나 스스로 챙길 때 make sure to / make sure (that) 형태로 써요.',
        'I''m heading out now.',
        '나 이제 나가.',
        'Make sure to lock the door.',
        '문 꼭 잠가.',
        ARRAY['Make', 'sure', 'to', 'lock', 'the', 'door'],
        ARRAY['door', 'sure', 'Makeed', 'Makes', 'lock', 'Makeing', 'to', 'Make', 'the'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/22/expressions/91/practice-examples/0af5e5ef-1734-424e-b18e-7b528b8a40d6.png","sentenceText":"Make sure you bring your passport.","sentenceWords":["Make","sure","you","bring","your","passport"],"highlightingPart":"Make sure","practiceQuestion":"What should I remember before my flight?","sentenceTranslation":"여권 꼭 챙겨.","sentenceWordChoices":["Make","passport","sure","Makeing","you","Makeed","bring","your","Makes"],"practiceQuestionTranslation":"비행 전에 뭘 꼭 기억해야 해?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/22/expressions/91/practice-examples/895e0656-92e0-4299-b701-052f9728acf2.png","sentenceText":"I''ll make sure everything is ready.","sentenceWords":["I''ll","make","sure","everything","is","ready"],"highlightingPart":"make sure","practiceQuestion":"Will everything be ready in time?","sentenceTranslation":"다 준비되도록 확실히 챙길게.","sentenceWordChoices":["makeed","I''ll","makeing","sure","make","makes","everything","is","ready"],"practiceQuestionTranslation":"제시간에 준비가 다 될까?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/22/expressions/91/practice-examples/81f5efc2-aa42-4890-a1c2-cfc85bd8855a.png","sentenceText":"Make sure the oven is off before you leave.","sentenceWords":["Make","sure","the","oven","is","off","before","you","leave"],"highlightingPart":"Make sure","practiceQuestion":"What should I check before leaving the house?","sentenceTranslation":"나가기 전에 오븐 꺼졌는지 꼭 확인해.","sentenceWordChoices":["Makeed","off","Make","you","leave","Makes","Makeing","is","the","before","oven","sure"],"practiceQuestionTranslation":"집을 나가기 전에 뭘 확인해야 해?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/22/expressions/91/practice-examples/7ee4f792-83d7-44c3-9b27-d64b8c83dfce.png","sentenceText":"Let me make sure I understood correctly.","sentenceWords":["Let","me","make","sure","I","understood","correctly"],"highlightingPart":"make sure","practiceQuestion":"Did you understand what I meant?","sentenceTranslation":"제가 제대로 이해했는지 확인할게요.","sentenceWordChoices":["Let","makeed","me","I","makes","make","makeing","sure","correctly","understood"],"practiceQuestionTranslation":"제가 말한 내용을 이해하셨나요?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        92,
        23,
        'SCENARIO',
        'WORK_STUDY',
        'BASIC',
        'EN',
        'KR',
        1,
        'go through',
        '훑어보다, 검토하다',
        '꼼꼼히 살펴보는 go through',
        '''처음부터 끝까지 훑어보다, 검토하다''라는 뜻의 구동사입니다. 서류, 짐, 자료 등을 하나하나 살펴본다는 뉘앙스이고, ''힘든 일을 겪다''라는 의미로도 확장돼요.',
        'What should we do before signing?',
        '사인하기 전에 뭘 해야 해?',
        'Let me go through the documents first.',
        '먼저 서류를 쭉 검토해 볼게요.',
        ARRAY['Let', 'me', 'go', 'through', 'the', 'documents', 'first'],
        ARRAY['me', 'documents', 'throughs', 'first', 'go', 'Let', 'throughed', 'the', 'throughing', 'through'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/23/expressions/92/practice-examples/615467c8-aeb9-4286-8709-3c0f6a4d4a6c.png","sentenceText":"I went through my closet and donated old clothes.","sentenceWords":["I","went","through","my","closet","and","donated","old","clothes"],"highlightingPart":"went through","practiceQuestion":"How did you decide which clothes to donate?","sentenceTranslation":"옷장을 싹 정리해서 안 입는 옷을 기부했어.","sentenceWordChoices":["donated","clothes","and","closet","through","we","you","he","went","I","old","my"],"practiceQuestionTranslation":"어떤 옷을 기부할지 어떻게 정했어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/23/expressions/92/practice-examples/d2e82841-5899-497b-9357-a8531a8e8825.png","sentenceText":"We need to go through the checklist.","sentenceWords":["We","need","to","go","through","the","checklist"],"highlightingPart":"go through","practiceQuestion":"What should we do before we finish?","sentenceTranslation":"체크리스트를 하나하나 확인해야 해.","sentenceWordChoices":["need","We","the","checklist","they","go","you","I","through","to"],"practiceQuestionTranslation":"마치기 전에 뭘 해야 해?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/23/expressions/92/practice-examples/a948439d-9239-4333-bd94-0dbbe9093ed0.png","sentenceText":"She''s going through a hard time.","sentenceWords":["She''s","going","through","a","hard","time"],"highlightingPart":"through","practiceQuestion":"How is she doing these days?","sentenceTranslation":"걔 지금 힘든 시기를 겪고 있어.","sentenceWordChoices":["goinged","through","goings","goinging","time","She''s","hard","a","going"],"practiceQuestionTranslation":"걔 요즘 어떻게 지내?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/23/expressions/92/practice-examples/4bcfd4f6-fc01-43d0-83d9-096922abdf73.png","sentenceText":"I went through all my emails this morning.","sentenceWords":["I","went","through","all","my","emails","this","morning"],"highlightingPart":"went through","practiceQuestion":"What did you do with all those emails?","sentenceTranslation":"아침에 이메일을 전부 훑어봤어.","sentenceWordChoices":["you","my","all","we","I","through","went","he","morning","emails","this"],"practiceQuestionTranslation":"그 많은 이메일은 어떻게 했어?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        93,
        23,
        'SCENARIO',
        'OPINION_JUDGMENT',
        'BASIC',
        'EN',
        'KR',
        2,
        'not exactly',
        '딱히 그렇진 않다',
        '딱 그렇진 않은 not exactly',
        '''딱히 ~인 건 아니다'', ''정확히 그렇진 않다''라며 단정을 피하는 표현입니다. 완곡하게 부정하거나 반어적으로 ''전혀 아니다''를 표현할 때도 쓰여요.',
        'Is this exactly what you ordered?',
        '이거 네가 주문한 거 맞아?',
        'It''s not exactly what I wanted.',
        '내가 원했던 게 딱 이건 아니야.',
        ARRAY['It''s', 'not', 'exactly', 'what', 'I', 'wanted'],
        ARRAY['what', 'It''s', 'exactly', 'I', 'exactlys', 'exactlyed', 'not', 'wanted', 'exactlying'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/23/expressions/93/practice-examples/63fe5c4e-c539-415a-bf1c-7fb7a0d1cfa5.png","sentenceText":"He''s not exactly a morning person.","sentenceWords":["He''s","not","exactly","a","morning","person"],"highlightingPart":"exactly","practiceQuestion":"Is he good at waking up early?","sentenceTranslation":"걔가 아침형 인간이라고 하긴 좀 그래.","sentenceWordChoices":["a","exactly","exactlyed","exactlys","He''s","person","not","exactlying","morning"],"practiceQuestionTranslation":"걔는 아침에 잘 일어나?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/23/expressions/93/practice-examples/8747ac18-aadf-4005-bfb7-9967f8834fa2.png","sentenceText":"That''s not exactly true.","sentenceWords":["That''s","not","exactly","true"],"highlightingPart":"exactly","practiceQuestion":"So everything he said was true?","sentenceTranslation":"그게 딱 사실인 건 아니야.","sentenceWordChoices":["exactlyed","true","That''s","exactlying","exactly","exactlys","not"],"practiceQuestionTranslation":"그럼 걔 말이 전부 사실이었어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/23/expressions/93/practice-examples/c5958d11-c16d-438f-9889-dfc330eef1f0.png","sentenceText":"It wasn''t exactly a surprise.","sentenceWords":["It","wasn''t","exactly","a","surprise"],"highlightingPart":"exactly","practiceQuestion":"Were you surprised by the news?","sentenceTranslation":"딱히 놀랄 일도 아니었어.","sentenceWordChoices":["exactlyed","a","exactlys","exactlying","exactly","wasn''t","It","surprise"],"practiceQuestionTranslation":"그 소식에 놀랐어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/23/expressions/93/practice-examples/34dc2143-5b4d-48b0-8aa0-ee50b62607b7.png","sentenceText":"I''m not exactly sure how it works.","sentenceWords":["I''m","not","exactly","sure","how","it","works"],"highlightingPart":"exactly","practiceQuestion":"Do you know how it works?","sentenceTranslation":"그게 어떻게 되는 건지 정확히는 몰라.","sentenceWordChoices":["exactlying","sure","it","exactlyed","exactlys","works","not","how","I''m","exactly"],"practiceQuestionTranslation":"그게 어떻게 작동하는지 알아?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        94,
        23,
        'SCENARIO',
        'CONVERSATION_SKILL',
        'BASIC',
        'EN',
        'KR',
        3,
        'No worries',
        '괜찮아요, 신경 쓰지 마세요',
        '만능 응답 No worries',
        '''괜찮아요'', ''신경 쓰지 마세요''라는 뜻의 만능 응답입니다. 사과에도, 감사에도, 부탁 수락에도 쓸 수 있어서 원어민 일상 대화에서 빈도 최상위권이에요.',
        'Sorry I''m late — traffic was crazy.',
        '늦어서 미안해. 차가 너무 막혔어.',
        'No worries. We just got here too.',
        '괜찮아. 우리도 방금 왔어.',
        ARRAY['No', 'worries', 'We', 'just', 'got', 'here', 'too'],
        ARRAY['No', 'worrieed', 'worries', 'just', 'We', 'worrieing', 'here', 'too', 'worrie', 'got'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/23/expressions/94/practice-examples/f0e934a0-7768-42a8-8a9c-ded7242637dd.png","sentenceText":"No worries, it happens.","sentenceWords":["No","worries","it","happens"],"highlightingPart":"No worries","practiceQuestion":"I''m sorry I made a mistake.","sentenceTranslation":"괜찮아, 그럴 수 있지.","sentenceWordChoices":["worrieing","it","worrie","happens","worries","worrieed","No"],"practiceQuestionTranslation":"내가 실수해서 미안해."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/23/expressions/94/practice-examples/ceb65faa-fa2a-4d4d-9ee9-5708de8b9dfd.png","sentenceText":"No worries at all — glad I could help.","sentenceWords":["No","worries","at","all","glad","I","could","help"],"highlightingPart":"No worries at all","practiceQuestion":"Thanks so much for helping me.","sentenceTranslation":"전혀요. 도움이 됐다니 다행이에요.","sentenceWordChoices":["could","worries","No","worrieed","help","all","I","worrieing","glad","at","worrie"],"practiceQuestionTranslation":"도와줘서 정말 고마워."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/23/expressions/94/practice-examples/747813fa-9e29-4b4b-ac1e-d7dbbfccfb9c.png","sentenceText":"No worries if you can''t make it.","sentenceWords":["No","worries","if","you","can''t","make","it"],"highlightingPart":"No worries","practiceQuestion":"I don''t think I can come.","sentenceTranslation":"못 와도 괜찮아.","sentenceWordChoices":["worrie","No","it","can''t","worries","worrieing","worrieed","if","make","you"],"practiceQuestionTranslation":"나 못 갈 것 같아."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/23/expressions/94/practice-examples/d413fa16-5d7b-4540-be57-6c526e8da1a9.png","sentenceText":"Don''t worry about the bill — it''s on me.","sentenceWords":["Don''t","worry","about","the","bill","it''s","on","me"],"highlightingPart":"Don''t worry","practiceQuestion":"Should I pay for dinner?","sentenceTranslation":"계산은 신경 쓰지 마. 내가 낼게.","sentenceWordChoices":["worrys","bill","the","on","worryed","Don''t","worrying","it''s","about","worry","me"],"practiceQuestionTranslation":"저녁값은 내가 낼까?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        95,
        23,
        'SCENARIO',
        'CONVERSATION_SKILL',
        'BASIC',
        'EN',
        'KR',
        4,
        'That explains ~',
        '그래서 그랬구나',
        '이해됐음을 표현하는 That explains ~',
        '''그래서 그랬구나'', ''그게 이유였구나''라며 궁금했던 것이 풀렸을 때 쓰는 표현입니다. 새로 들은 정보가 기존의 의문을 해소해 줄 때 리액션으로 딱이에요.',
        'Wait, so that''s why he left early?',
        '잠깐, 그래서 걔가 일찍 갔구나?',
        'That explains everything.',
        '이제 다 이해가 되네.',
        ARRAY['That', 'explains', 'everything'],
        ARRAY['this', 'explains', 'everything', 'those', 'That', 'these'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/23/expressions/95/practice-examples/3805321c-c1bb-4e62-a0fd-6d16d09a7b5e.png","sentenceText":"That explains why he was so tired.","sentenceWords":["That","explains","why","he","was","so","tired"],"highlightingPart":"That explains why","practiceQuestion":"He was up all night with the baby.","sentenceTranslation":"그래서 걔가 그렇게 피곤했구나.","sentenceWordChoices":["was","these","so","he","explains","why","those","this","That","tired"],"practiceQuestionTranslation":"걔 밤새 아기 보느라 못 잤대."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/23/expressions/95/practice-examples/61ea9561-ba59-43c7-b1a2-0e96bb156f33.png","sentenceText":"Oh, that explains the traffic.","sentenceWords":["Oh","that","explains","the","traffic"],"highlightingPart":"that explains","practiceQuestion":"There was an accident on the highway.","sentenceTranslation":"아, 그래서 차가 막혔구나.","sentenceWordChoices":["those","the","Oh","that","explains","traffic","these","this"],"practiceQuestionTranslation":"고속도로에서 사고가 났대."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/23/expressions/95/practice-examples/f194b6cb-fffc-4d95-a2b4-3108ebbaf6bf.png","sentenceText":"That explains why the office was empty.","sentenceWords":["That","explains","why","the","office","was","empty"],"highlightingPart":"That explains why","practiceQuestion":"Everyone is working from home today.","sentenceTranslation":"그래서 사무실이 텅 비었던 거구나.","sentenceWordChoices":["those","empty","these","office","That","was","why","explains","this","the"],"practiceQuestionTranslation":"오늘은 모두 재택근무한대."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/23/expressions/95/practice-examples/8732b6b2-db9d-4146-994b-5d507ecd7d14.png","sentenceText":"She grew up in Paris? That explains her French.","sentenceWords":["She","grew","up","in","Paris","That","explains","her","French"],"highlightingPart":"That explains","practiceQuestion":"Her French is excellent.","sentenceTranslation":"걔 파리에서 자랐어? 그래서 프랑스어를 잘하는구나.","sentenceWordChoices":["grew","grewing","her","in","up","She","explains","That","grews","grewed","French","Paris"],"practiceQuestionTranslation":"걔 프랑스어 진짜 잘한다."}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        96,
        24,
        'SCENARIO',
        'DAILY_ROUTINE',
        'BASIC',
        'EN',
        'KR',
        1,
        'wipe up',
        '흘린 것을 닦아내다',
        '닦아낼 때 쓰는 wipe up',
        '''흘린 것을 닦아내다''라는 뜻으로, up이 붙어 깨끗이 마무리한다는 느낌을 줍니다. wipe up the mess처럼 액체나 얼룩을 치울 때 딱이에요.',
        'I spilled water on the floor.',
        '바닥에 물 쏟았어.',
        'Can you wipe up the water on the floor?',
        '바닥에 물 좀 닦아줄래?',
        ARRAY['Can', 'you', 'wipe', 'up', 'the', 'water', 'on', 'the', 'floor'],
        ARRAY['you', 'would', 'wipe', 'Can', 'on', 'water', 'the', 'should', 'the', 'floor', 'could', 'up'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/24/expressions/96/practice-examples/c5ffbb7e-c7c7-401b-80d1-6169ae294a66.png","sentenceText":"I wiped up the coffee I spilled.","sentenceWords":["I","wiped","up","the","coffee","I","spilled"],"highlightingPart":"wiped up","practiceQuestion":"What did you do after spilling the coffee?","sentenceTranslation":"흘린 커피를 닦아냈어.","sentenceWordChoices":["up","coffee","wiped","the","spilled","you","I","we","I","he"],"practiceQuestionTranslation":"커피를 쏟고 나서 어떻게 했어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/24/expressions/96/practice-examples/18998bc8-510f-4d77-89cd-2f7dfc3a2846.png","sentenceText":"Use this towel to wipe up the mess.","sentenceWords":["Use","this","towel","to","wipe","up","the","mess"],"highlightingPart":"wipe up","practiceQuestion":"How should I clean this mess?","sentenceTranslation":"이 수건으로 어질러진 거 닦아.","sentenceWordChoices":["to","those","these","that","the","Use","wipe","mess","this","towel","up"],"practiceQuestionTranslation":"이걸 어떻게 치우면 돼?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/24/expressions/96/practice-examples/3e9f431e-edf8-48c6-aab4-dc4b8a44291f.png","sentenceText":"Someone needs to wipe up the juice on the table.","sentenceWords":["Someone","needs","to","wipe","up","the","juice","on","the","table"],"highlightingPart":"wipe up","practiceQuestion":"There''s juice all over the table.","sentenceTranslation":"누가 테이블에 흘린 주스 좀 닦아야 해.","sentenceWordChoices":["table","wipe","needs","Someone","Someoneing","on","juice","up","the","to","the","Someones","Someoneed"],"practiceQuestionTranslation":"테이블에 주스가 다 쏟아졌어."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/24/expressions/96/practice-examples/11970dd7-c39b-4aa5-a184-440e48f05999.png","sentenceText":"She wiped up the crumbs after breakfast.","sentenceWords":["She","wiped","up","the","crumbs","after","breakfast"],"highlightingPart":"wiped up","practiceQuestion":"What did she do after breakfast?","sentenceTranslation":"걔는 아침 먹고 부스러기를 닦아냈어.","sentenceWordChoices":["breakfast","wiping","wiped","the","wips","wip","after","crumbs","She","up"],"practiceQuestionTranslation":"걔는 아침 식사 후에 뭘 했어?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        97,
        24,
        'SCENARIO',
        'POLITE_EXPRESSION',
        'BASIC',
        'EN',
        'KR',
        2,
        'Is there any chance ~?',
        '혹시 ~ 안 될까?',
        '어려운 부탁을 꺼내는 Is there any chance ~?',
        '''혹시 ~할 가능성 있어?'', 즉 ''혹시 ~ 안 될까?''라고 부탁을 한층 부드럽게 만드는 표현입니다. 어려운 부탁일수록 진가를 발휘하고, 캐주얼하게는 Any chance ~?로 줄여서도 써요.',
        'What''s up? You look like you need something.',
        '왜? 뭐 부탁할 게 있는 얼굴인데.',
        'Is there any chance you could cover my shift tomorrow?',
        '혹시 내일 내 근무 좀 대신해 줄 수 있어?',
        ARRAY['Is', 'there', 'any', 'chance', 'you', 'could', 'cover', 'my', 'shift', 'tomorrow'],
        ARRAY['could', 'chance', 'Is', 'are', 'was', 'cover', 'shift', 'you', 'be', 'tomorrow', 'there', 'any', 'my'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/24/expressions/97/practice-examples/4c42f311-62a7-4c68-a804-febecca8e978.png","sentenceText":"Is there any chance you could give me a ride?","sentenceWords":["Is","there","any","chance","you","could","give","me","a","ride"],"highlightingPart":"Is there any chance","practiceQuestion":"I don''t have a way to get there.","sentenceTranslation":"혹시 나 좀 태워줄 수 있어?","sentenceWordChoices":["a","could","be","Is","chance","give","you","are","ride","was","there","me","any"],"practiceQuestionTranslation":"거기 갈 방법이 없어."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/24/expressions/97/practice-examples/6ac2b5db-642b-497f-b0c6-d5cbda09335f.png","sentenceText":"Is there any chance the deadline could be extended?","sentenceWords":["Is","there","any","chance","the","deadline","could","be","extended"],"highlightingPart":"Is there any chance","practiceQuestion":"The deadline is too soon for me.","sentenceTranslation":"혹시 마감이 연장될 가능성 있나요?","sentenceWordChoices":["was","deadline","extended","chance","there","any","are","theres","Is","be","the","could"],"practiceQuestionTranslation":"마감이 나한테는 너무 촉박해."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/24/expressions/97/practice-examples/ea4d56b2-30de-4ed3-b25a-9659f725bb02.png","sentenceText":"Is there any chance you''re free tonight?","sentenceWords":["Is","there","any","chance","you''re","free","tonight"],"highlightingPart":"Is there any chance","practiceQuestion":"Do you have any plans tonight?","sentenceTranslation":"혹시 오늘 밤에 시간 돼?","sentenceWordChoices":["free","are","there","be","chance","any","Is","tonight","was","you''re"],"practiceQuestionTranslation":"오늘 밤에 약속 있어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/24/expressions/97/practice-examples/db684867-3295-4095-a99b-1ba8ceb97c42.png","sentenceText":"Any chance I could borrow your charger?","sentenceWords":["Any","chance","I","could","borrow","your","charger"],"highlightingPart":"Any chance","practiceQuestion":"My phone is about to die.","sentenceTranslation":"혹시 충전기 좀 빌릴 수 있을까?","sentenceWordChoices":["chanceed","borrow","chanceing","your","charger","Any","chance","I","chances","could"],"practiceQuestionTranslation":"휴대폰 배터리가 거의 다 됐어."}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        98,
        24,
        'SCENARIO',
        'POLITE_EXPRESSION',
        'BASIC',
        'EN',
        'KR',
        3,
        'I was wondering if ~',
        '혹시 ~해주실 수 있을까 해서요',
        '조심스럽게 부탁을 꺼내는 I was wondering if ~',
        '부탁을 곧장 던지지 않고 ''혹시 ~해주실 수 있을까 해서요''라고 조심스럽게 꺼내는 정중한 표현입니다. 과거진행형이 심리적 거리를 만들어 부담을 줄여주며, 대면·이메일 어디서나 쓰는 원어민 부탁의 정석이에요.',
        'Hi, what brings you in today?',
        '안녕하세요, 어쩐 일로 오셨어요?',
        'I was wondering if you could help me with this form.',
        '혹시 이 서류 작성하는 것 좀 도와주실 수 있을까 해서요.',
        ARRAY['I', 'was', 'wondering', 'if', 'you', 'could', 'help', 'me', 'with', 'this', 'form'],
        ARRAY['this', 'me', 'he', 'we', 'if', 'was', 'with', 'I', 'help', 'you', 'could', 'wondering', 'form', 'is'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/24/expressions/98/practice-examples/94b83107-72f8-4df5-9140-3d65bb429974.png","sentenceText":"I was wondering if you could give me some feedback.","sentenceWords":["I","was","wondering","if","you","could","give","me","some","feedback"],"highlightingPart":"I was wondering if","practiceQuestion":"What did you want to ask me?","sentenceTranslation":"혹시 피드백 좀 주실 수 있을까 해서요.","sentenceWordChoices":["I","you","was","is","some","we","wondering","if","give","he","feedback","could","me"],"practiceQuestionTranslation":"나한테 뭘 물어보려고 했어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/24/expressions/98/practice-examples/a66d1f15-c5ca-48af-90fb-0f21bec7e6a8.png","sentenceText":"I was wondering if the position is still open.","sentenceWords":["I","was","wondering","if","the","position","is","still","open"],"highlightingPart":"I was wondering if","practiceQuestion":"Why did you call about the job?","sentenceTranslation":"혹시 그 자리가 아직 비어 있는지 궁금해서요.","sentenceWordChoices":["we","position","still","is","open","if","was","I","wondering","the","he","you"],"practiceQuestionTranslation":"그 일자리 때문에 왜 전화했어요?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/24/expressions/98/practice-examples/417e5991-aaf4-4f5b-b093-ac2d5f041416.png","sentenceText":"I was wondering if you''re free this weekend.","sentenceWords":["I","was","wondering","if","you''re","free","this","weekend"],"highlightingPart":"I was wondering if","practiceQuestion":"Did you need something this weekend?","sentenceTranslation":"혹시 이번 주말에 시간 되는지 해서.","sentenceWordChoices":["wondering","we","was","you","free","you''re","if","I","this","he","weekend"],"practiceQuestionTranslation":"이번 주말에 무슨 일 있어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/24/expressions/98/practice-examples/23870433-4ac5-4d81-96af-1686d08ad044.png","sentenceText":"I was wondering if I could leave a bit early today.","sentenceWords":["I","was","wondering","if","I","could","leave","a","bit","early","today"],"highlightingPart":"I was wondering if","practiceQuestion":"Why are you talking to your manager?","sentenceTranslation":"혹시 오늘 조금 일찍 가도 될까 해서요.","sentenceWordChoices":["could","we","I","bit","was","he","early","I","wondering","a","if","today","you","leave"],"practiceQuestionTranslation":"매니저에게 왜 얘기하려고 해?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        99,
        24,
        'SCENARIO',
        'POLITE_EXPRESSION',
        'BASIC',
        'EN',
        'KR',
        4,
        'when you get a chance',
        '시간 날 때, 여유 될 때',
        '시간 압박을 빼주는 when you get a chance',
        '''시간 날 때'', ''여유 될 때''라며 부탁에서 시간 압박을 빼주는 표현입니다. No rush(급할 것 없어요)와 세트로 쓰면 상대를 배려하는 태도가 그대로 전달돼요.',
        'I''m swamped right now — is it urgent?',
        '지금 정신없는데, 급한 거예요?',
        'No rush — just look it over when you get a chance.',
        '급한 거 아니에요. 시간 될 때 한번 봐주세요.',
        ARRAY['No', 'rush', 'just', 'look', 'it', 'over', 'when', 'you', 'get', 'a', 'chance'],
        ARRAY['a', 'it', 'rushed', 'rushs', 'look', 'when', 'just', 'chance', 'No', 'you', 'rush', 'rushing', 'over', 'get'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/24/expressions/99/practice-examples/a162a1fc-965d-41bd-97d5-2ca4dda63a33.png","sentenceText":"Call me back when you get a chance.","sentenceWords":["Call","me","back","when","you","get","a","chance"],"highlightingPart":"when you get a chance","practiceQuestion":"When should I call you back?","sentenceTranslation":"시간 될 때 다시 전화 줘.","sentenceWordChoices":["Calling","when","you","get","a","me","Called","back","Calls","Call","chance"],"practiceQuestionTranslation":"언제 다시 전화하면 돼?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/24/expressions/99/practice-examples/95cb0936-2f68-4234-94b2-5dd45aa705d3.png","sentenceText":"When you get a chance, could you sign this?","sentenceWords":["When","you","get","a","chance","could","you","sign","this"],"highlightingPart":"When you get a chance","practiceQuestion":"Does this need to be signed right now?","sentenceTranslation":"시간 되실 때 여기 서명 좀 해주시겠어요?","sentenceWordChoices":["Whening","When","this","chance","Whened","you","Whens","could","sign","get","a","you"],"practiceQuestionTranslation":"이거 지금 바로 서명해야 하나요?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/24/expressions/99/practice-examples/d948e75f-a209-43a2-8d21-2a7496d25f9e.png","sentenceText":"Check out that cafe when you get the chance.","sentenceWords":["Check","out","that","cafe","when","you","get","the","chance"],"highlightingPart":"when you get the chance","practiceQuestion":"Should I visit that cafe someday?","sentenceTranslation":"기회 되면 그 카페 한번 가봐.","sentenceWordChoices":["Check","out","you","that","Checked","get","cafe","Checks","Checking","the","when","chance"],"practiceQuestionTranslation":"그 카페에 언제 한번 가볼까?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/24/expressions/99/practice-examples/ca5e09c0-2da5-4464-9c8b-657f742f2520.png","sentenceText":"I''ll clean the garage when I get a chance.","sentenceWords":["I''ll","clean","the","garage","when","I","get","a","chance"],"highlightingPart":"when I get a chance","practiceQuestion":"When are you going to clean the garage?","sentenceTranslation":"시간 나면 차고 정리할게.","sentenceWordChoices":["a","I''ll","I","cleans","clean","chance","cleaned","garage","cleaning","get","the","when"],"practiceQuestionTranslation":"차고는 언제 정리할 거야?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        100,
        25,
        'SCENARIO',
        'WORK_STUDY',
        'BASIC',
        'EN',
        'KR',
        1,
        'double-check',
        '한 번 더 확인하다',
        '한 번 더 확인하는 double-check',
        '''한 번 더 확인하다''라는 뜻으로, 실수를 방지하려고 재차 확인할 때 씁니다. Let me double-check(다시 확인해 볼게요)은 서비스와 업무 현장의 단골 멘트예요.',
        'Are you sure the flight is at 8?',
        '비행기 8시인 거 확실해?',
        'Let me double-check the ticket.',
        '표 다시 한번 확인해 볼게.',
        ARRAY['Let', 'me', 'double-check', 'the', 'ticket'],
        ARRAY['me', 'some', 'a', 'double-check', 'Let', 'an', 'ticket', 'the'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/25/expressions/100/practice-examples/939ebb75-7c8e-4ed4-85ee-2a0a4eabdb2b.png","sentenceText":"I double-checked the address before sending.","sentenceWords":["I","double-checked","the","address","before","sending"],"highlightingPart":"double-checked","practiceQuestion":"How did you avoid sending it to the wrong place?","sentenceTranslation":"보내기 전에 주소를 두 번 확인했어.","sentenceWordChoices":["address","before","the","you","double-checked","he","I","sending","we"],"practiceQuestionTranslation":"잘못된 곳으로 보내지 않으려고 어떻게 했어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/25/expressions/100/practice-examples/9025ff92-5fb5-4bae-9d30-da383c70815e.png","sentenceText":"Can you double-check the reservation?","sentenceWords":["Can","you","double-check","the","reservation"],"highlightingPart":"double-check","practiceQuestion":"I found your booking, but the date looks unusual.","sentenceTranslation":"예약 다시 한번 확인해 주시겠어요?","sentenceWordChoices":["double-check","reservation","you","would","the","could","Can","should"],"practiceQuestionTranslation":"예약은 찾았는데 날짜가 좀 이상해 보여요."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/25/expressions/100/practice-examples/5f731920-647c-4f8c-af3f-2009f50fc64e.png","sentenceText":"Always double-check before you hit send.","sentenceWords":["Always","double-check","before","you","hit","send"],"highlightingPart":"double-check","practiceQuestion":"What should I do before sending the message?","sentenceTranslation":"보내기 누르기 전에 꼭 다시 확인해.","sentenceWordChoices":["hit","send","you","Alwaying","before","Always","Alwayed","double-check","Alway"],"practiceQuestionTranslation":"메시지를 보내기 전에 뭘 해야 해?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/25/expressions/100/practice-examples/ba7a9a4e-0ea4-4193-aca2-f15351be4707.png","sentenceText":"I double-checked, and we''re good to go.","sentenceWords":["I","double-checked","and","we''re","good","to","go"],"highlightingPart":"double-checked","practiceQuestion":"Are there any problems left?","sentenceTranslation":"다시 확인했는데 문제없어.","sentenceWordChoices":["double-checked","good","to","we''re","you","he","and","I","we","go"],"practiceQuestionTranslation":"남은 문제는 없어?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        101,
        25,
        'SCENARIO',
        'OPINION_JUDGMENT',
        'BASIC',
        'EN',
        'KR',
        2,
        'be aware of',
        '~을 알고 있다, 인지하다',
        '인지 여부를 말하는 be aware of',
        '''~을 알고 있다, 인지하고 있다''라는 뜻으로, know보다 격식 있고 ''의식하고 있음''에 초점이 있는 표현입니다. wasn''t aware of는 ''미처 몰랐다''는 뉘앙스로 자주 쓰여요.',
        'Did you know the schedule changed?',
        '일정 바뀐 거 알았어?',
        'I wasn''t aware of the change.',
        '그 변경 사항을 미처 몰랐어요.',
        ARRAY['I', 'wasn''t', 'aware', 'of', 'the', 'change'],
        ARRAY['of', 'you', 'the', 'he', 'aware', 'change', 'I', 'wasn''t', 'we'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/25/expressions/101/practice-examples/015a4db5-9489-4e6d-a64c-cbe9e1f27934.png","sentenceText":"Are you aware of the risks?","sentenceWords":["Are","you","aware","of","the","risks"],"highlightingPart":"aware of","practiceQuestion":"I''m ready to move forward with the plan.","sentenceTranslation":"그 위험성을 알고 계신가요?","sentenceWordChoices":["you","the","were","is","of","aware","risks","Are","be"],"practiceQuestionTranslation":"이 계획대로 진행할 준비가 됐어요."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/25/expressions/101/practice-examples/736e67f8-5696-4966-850b-f0c8cc53187d.png","sentenceText":"I''m well aware of the problem.","sentenceWords":["I''m","well","aware","of","the","problem"],"highlightingPart":"aware of","practiceQuestion":"Do you understand the problem?","sentenceTranslation":"그 문제는 잘 알고 있어요.","sentenceWordChoices":["welled","wells","welling","well","of","I''m","problem","aware","the"],"practiceQuestionTranslation":"그 문제를 알고 있어요?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/25/expressions/101/practice-examples/970f7ec3-00b0-405b-8e7d-9280d176521a.png","sentenceText":"She wasn''t aware that the store had closed.","sentenceWords":["She","wasn''t","aware","that","the","store","had","closed"],"highlightingPart":"aware that","practiceQuestion":"Why did she go to the store after it closed?","sentenceTranslation":"걔는 가게가 문 닫은 걸 몰랐어.","sentenceWordChoices":["that","the","She","wasn''t","had","aware","closed","awareed","awares","store","awareing"],"practiceQuestionTranslation":"걔는 왜 문 닫은 뒤에 가게에 갔어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/25/expressions/101/practice-examples/3a90495e-07c7-4f11-87e5-81235c8e3b77.png","sentenceText":"I wasn''t aware of how late it was.","sentenceWords":["I","wasn''t","aware","of","how","late","it","was"],"highlightingPart":"aware of","practiceQuestion":"Did you know it was already so late?","sentenceTranslation":"시간이 그렇게 늦은 줄 몰랐어.","sentenceWordChoices":["late","was","it","he","aware","of","wasn''t","how","we","you","I"],"practiceQuestionTranslation":"벌써 이렇게 늦은 줄 알았어?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        102,
        25,
        'SCENARIO',
        'CONVERSATION_SKILL',
        'BASIC',
        'EN',
        'KR',
        3,
        'point out',
        '지적하다, 짚어주다',
        '지적하는 point out',
        '''지적하다, 짚어주다''라는 뜻으로, 상대가 놓친 것을 알려줄 때 씁니다. 비난보다는 ''알려준다''는 중립적 뉘앙스에 가까워요.',
        'How did you find out about the mistake?',
        '그 실수 어떻게 알게 됐어?',
        'He pointed out a mistake in my report.',
        '걔가 내 보고서에서 실수 하나를 짚어줬어.',
        ARRAY['He', 'pointed', 'out', 'a', 'mistake', 'in', 'my', 'report'],
        ARRAY['mistake', 'point', 'report', 'pointed', 'points', 'out', 'pointing', 'in', 'a', 'He', 'my'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/25/expressions/102/practice-examples/8bf6f568-b533-42c2-90ad-a6da43d8ca3e.png","sentenceText":"Thanks for pointing that out.","sentenceWords":["Thanks","for","pointing","that","out"],"highlightingPart":"pointing that out","practiceQuestion":"There''s a typo in the first paragraph.","sentenceTranslation":"그 부분 짚어줘서 고마워.","sentenceWordChoices":["pointing","that","for","Thanked","Thank","Thanks","out","Thanking"],"practiceQuestionTranslation":"첫 문단에 오타가 있어."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/25/expressions/102/practice-examples/7d75de92-b750-4ee2-a3b4-be28f69c81b8.png","sentenceText":"She pointed out that we were running late.","sentenceWords":["She","pointed","out","that","we","were","running","late"],"highlightingPart":"pointed out","practiceQuestion":"Who noticed that we were late?","sentenceTranslation":"걔가 우리가 늦고 있다고 알려줬어.","sentenceWordChoices":["pointing","points","point","late","we","She","were","running","out","that","pointed"],"practiceQuestionTranslation":"우리가 늦었다는 걸 누가 알아챘어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/25/expressions/102/practice-examples/53e10059-c8f7-4310-8232-74cafad7fa9e.png","sentenceText":"Let me point out a few things.","sentenceWords":["Let","me","point","out","a","few","things"],"highlightingPart":"point out","practiceQuestion":"What would you like to explain first?","sentenceTranslation":"몇 가지 짚고 넘어갈게요.","sentenceWordChoices":["things","points","a","me","point","few","Let","pointing","out","pointed"],"practiceQuestionTranslation":"먼저 무엇을 짚어 주시겠어요?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/25/expressions/102/practice-examples/8fa97d94-067e-428f-aacc-94d6484e27c2.png","sentenceText":"The guide pointed out the famous landmarks.","sentenceWords":["The","guide","pointed","out","the","famous","landmarks"],"highlightingPart":"pointed out","practiceQuestion":"How did you know which buildings were famous?","sentenceTranslation":"가이드가 유명한 명소들을 알려줬어.","sentenceWordChoices":["landmarks","out","famous","a","guide","The","pointed","some","an","the"],"practiceQuestionTranslation":"어떤 건물이 유명한지 어떻게 알았어?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        103,
        25,
        'SCENARIO',
        'CONVERSATION_SKILL',
        'BASIC',
        'EN',
        'KR',
        4,
        'Just to clarify',
        '확실히 짚자면, 확인차 말하자면',
        '확실히 짚고 가는 Just to clarify',
        '들은 내용을 확인하거나 오해를 방지할 때 문장 앞에 붙이는 확인용 쿠션입니다. Just to make sure와 함께, 회의나 약속에서 되물어도 어색하지 않게 만들어주는 실전 필수 표현이에요.',
        '...so we''ll meet at the station at nine.',
        '…그래서 9시에 역에서 만나는 거야.',
        'Just to make sure — nine in the morning, right?',
        '확실히 해두려고 하는 말인데, 아침 9시 맞지?',
        ARRAY['Just', 'to', 'make', 'sure', 'nine', 'in', 'the', 'morning', 'right'],
        ARRAY['Justed', 'the', 'nine', 'Just', 'Justs', 'in', 'make', 'right', 'Justing', 'morning', 'to', 'sure'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/25/expressions/103/practice-examples/1dfe964b-996b-4168-81ae-3aa55ee5d5e3.png","sentenceText":"Just to clarify, the deadline is this Friday?","sentenceWords":["Just","to","clarify","the","deadline","is","this","Friday"],"highlightingPart":"Just to clarify","practiceQuestion":"The deadline is this Friday.","sentenceTranslation":"확실히 짚자면, 마감이 이번 주 금요일이죠?","sentenceWordChoices":["to","this","clarify","Justing","Just","deadline","is","the","Justs","Friday","Justed"],"practiceQuestionTranslation":"마감은 이번 주 금요일이에요."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/25/expressions/103/practice-examples/24b2604c-6715-4004-ae86-c8fd9466047c.png","sentenceText":"Just to make sure I understood — you want two copies?","sentenceWords":["Just","to","make","sure","I","understood","you","want","two","copies"],"highlightingPart":"Just to make sure","practiceQuestion":"I need two copies, please.","sentenceTranslation":"제가 맞게 이해했는지 확인할게요. 두 부 필요하신 거죠?","sentenceWordChoices":["you","make","Justs","copies","two","to","Justed","Justing","understood","want","Just","I","sure"],"practiceQuestionTranslation":"두 부 부탁드려요."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/25/expressions/103/practice-examples/00522d5c-1717-40fb-911b-6242c7f42b17.png","sentenceText":"Just to be clear, I''m not blaming anyone.","sentenceWords":["Just","to","be","clear","I''m","not","blaming","anyone"],"highlightingPart":"Just to be clear","practiceQuestion":"Are you saying this is someone''s fault?","sentenceTranslation":"분명히 해두자면, 누굴 탓하는 게 아니야.","sentenceWordChoices":["be","Just","to","Justed","Justs","not","I''m","clear","blaming","anyone","Justing"],"practiceQuestionTranslation":"이게 누군가의 잘못이라는 말이야?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/25/expressions/103/practice-examples/34e818ce-c932-4b5e-a9c0-ade40d5fbf19.png","sentenceText":"Let me just confirm the address.","sentenceWords":["Let","me","just","confirm","the","address"],"highlightingPart":"just confirm","practiceQuestion":"What do you need to verify?","sentenceTranslation":"주소만 다시 확인할게요.","sentenceWordChoices":["justs","confirm","the","me","justing","address","justed","Let","just"],"practiceQuestionTranslation":"무엇을 확인해야 하나요?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        104,
        26,
        'SCENARIO',
        'TIME_PLANNING',
        'BASIC',
        'EN',
        'KR',
        1,
        'just in time',
        '딱 맞춰서',
        '타이밍을 말하는 just in time',
        '''딱 맞춰서'', ''아슬아슬하게 제때''라는 뜻으로, 늦지 않고 딱 좋은 타이밍이었음을 표현합니다. catch someone just in time처럼 쓰면 ''딱 맞게 붙잡았다''는 의미가 돼요.',
        'Did you make the train?',
        '기차 탔어?',
        'We arrived just in time.',
        '우리 딱 맞춰서 도착했어.',
        ARRAY['We', 'arrived', 'just', 'in', 'time'],
        ARRAY['arrived', 'in', 'I', 'they', 'time', 'We', 'just', 'you'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/26/expressions/104/practice-examples/f16f0022-ce3e-4ac1-8d08-739f3b376773.png","sentenceText":"You''re just in time for dinner.","sentenceWords":["You''re","just","in","time","for","dinner"],"highlightingPart":"just in time","practiceQuestion":"Did I miss dinner?","sentenceTranslation":"딱 저녁 시간에 맞춰 왔네.","sentenceWordChoices":["time","just","justs","justed","justing","in","You''re","for","dinner"],"practiceQuestionTranslation":"나 저녁 식사 놓쳤어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/26/expressions/104/practice-examples/1f5ba057-1c42-45d5-8f8e-f35138889403.png","sentenceText":"I got to the station just in time.","sentenceWords":["I","got","to","the","station","just","in","time"],"highlightingPart":"just in time","practiceQuestion":"Did you make it to the station before the train left?","sentenceTranslation":"역에 아슬아슬하게 제때 도착했어.","sentenceWordChoices":["you","got","the","we","in","just","station","he","to","I","time"],"practiceQuestionTranslation":"기차가 떠나기 전에 역에 도착했어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/26/expressions/104/practice-examples/43b92e9c-ccc5-4652-9318-ba55b5efc423.png","sentenceText":"The rain stopped just in time for the picnic.","sentenceWords":["The","rain","stopped","just","in","time","for","the","picnic"],"highlightingPart":"just in time","practiceQuestion":"Did the rain ruin the picnic?","sentenceTranslation":"소풍 가기 딱 맞춰 비가 그쳤어.","sentenceWordChoices":["some","in","time","rain","for","the","picnic","just","The","a","an","stopped"],"practiceQuestionTranslation":"비 때문에 소풍을 망쳤어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/26/expressions/104/practice-examples/318da397-3e36-4cb9-b187-0bf2c4f1e3fb.png","sentenceText":"He submitted the report just in time.","sentenceWords":["He","submitted","the","report","just","in","time"],"highlightingPart":"just in time","practiceQuestion":"Did he miss the deadline?","sentenceTranslation":"걔 보고서를 딱 마감에 맞춰 냈어.","sentenceWordChoices":["submitt","the","in","report","submitts","He","submitting","submitted","time","just"],"practiceQuestionTranslation":"걔 마감을 놓쳤어?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        105,
        26,
        'SCENARIO',
        'OPINION_JUDGMENT',
        'BASIC',
        'EN',
        'KR',
        2,
        'I''d prefer ~',
        '~쪽이 더 좋다',
        '선호를 밝히는 I''d prefer ~',
        'would rather와 같은 ''~쪽이 더 좋다''는 뜻인데, 뒤에 명사를 바로 붙일 수 있어 선택지를 고를 때 특히 편한 표현입니다. I''d prefer it if ~ 형태로 문장도 받을 수 있어요.',
        'We can do a morning or afternoon appointment.',
        '오전이나 오후 예약이 가능합니다.',
        'I''d prefer the morning, if that''s okay.',
        '괜찮다면 오전이 더 좋아요.',
        ARRAY['I''d', 'prefer', 'the', 'morning', 'if', 'that''s', 'okay'],
        ARRAY['prefered', 'I''d', 'prefers', 'prefering', 'morning', 'the', 'prefer', 'okay', 'that''s', 'if'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/26/expressions/105/practice-examples/1aaa7491-0cb1-43b5-9575-5dc58cda0996.png","sentenceText":"I''d prefer a window seat, please.","sentenceWords":["I''d","prefer","a","window","seat","please"],"highlightingPart":"I''d prefer","practiceQuestion":"Would you like an aisle or window seat?","sentenceTranslation":"창가 자리가 더 좋아요.","sentenceWordChoices":["seat","window","prefering","prefered","I''d","prefer","prefers","please","a"],"practiceQuestionTranslation":"통로와 창가 중 어느 좌석을 원하세요?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/26/expressions/105/practice-examples/2da612ea-c938-4d8b-9d5e-5e7663b740ca.png","sentenceText":"I''d prefer to pay by card.","sentenceWords":["I''d","prefer","to","pay","by","card"],"highlightingPart":"I''d prefer","practiceQuestion":"Would you like to pay in cash?","sentenceTranslation":"카드로 계산하는 게 좋겠어요.","sentenceWordChoices":["prefering","I''d","prefers","card","by","prefer","to","prefered","pay"],"practiceQuestionTranslation":"현금으로 결제하시겠어요?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/26/expressions/105/practice-examples/a17e0dd2-8b84-4fb2-910f-f518ac7ec6d9.png","sentenceText":"She''d prefer something less spicy.","sentenceWords":["She''d","prefer","something","less","spicy"],"highlightingPart":"She''d prefer","practiceQuestion":"What kind of food would she like?","sentenceTranslation":"걔는 덜 매운 게 나을 거야.","sentenceWordChoices":["prefered","prefer","something","less","She''d","prefering","spicy","prefers"],"practiceQuestionTranslation":"걔는 어떤 음식을 좋아할까?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/26/expressions/105/practice-examples/13034291-d3de-461d-a776-a2fe16eb4fc7.png","sentenceText":"I''d prefer it if we kept this casual.","sentenceWords":["I''d","prefer","it","if","we","kept","this","casual"],"highlightingPart":"I''d prefer","practiceQuestion":"Should we make this formal?","sentenceTranslation":"이건 편하게 가는 게 더 좋을 것 같아.","sentenceWordChoices":["prefer","prefered","we","if","I''d","casual","prefering","kept","it","this","prefers"],"practiceQuestionTranslation":"이걸 격식 있게 진행할까요?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        106,
        26,
        'SCENARIO',
        'POLITE_EXPRESSION',
        'BASIC',
        'EN',
        'KR',
        3,
        'I''d like to ~',
        '~하고 싶은데요 (정중한 요청)',
        '정중한 요청의 기본 I''d like to ~',
        'I want to의 정중한 버전으로, 주문·예약·요청의 시작을 여는 가장 기본적인 문형입니다. 식당, 은행, 전화 문의까지 격식 있는 요청은 대부분 이걸로 시작해요.',
        'Good evening! How can I help you?',
        '안녕하세요! 무엇을 도와드릴까요?',
        'I''d like to book a table for two, please.',
        '2인 테이블 예약하고 싶은데요.',
        ARRAY['I''d', 'like', 'to', 'book', 'a', 'table', 'for', 'two', 'please'],
        ARRAY['please', 'likeing', 'I''d', 'likeed', 'book', 'like', 'table', 'two', 'likes', 'for', 'a', 'to'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/26/expressions/106/practice-examples/1d736e68-6f59-4c36-9941-2035b049b0a6.png","sentenceText":"I''d like to check out, please.","sentenceWords":["I''d","like","to","check","out","please"],"highlightingPart":"I''d like","practiceQuestion":"How can I help you at the front desk?","sentenceTranslation":"체크아웃하고 싶습니다.","sentenceWordChoices":["to","like","please","likes","check","likeed","likeing","I''d","out"],"practiceQuestionTranslation":"프런트에서 무엇을 도와드릴까요?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/26/expressions/106/practice-examples/00fc5a5f-e009-42b7-8fb7-fcb12c5281e8.png","sentenceText":"I''d like a refund, if possible.","sentenceWords":["I''d","like","a","refund","if","possible"],"highlightingPart":"I''d like","practiceQuestion":"Would you like an exchange?","sentenceTranslation":"가능하면 환불받고 싶어요.","sentenceWordChoices":["a","possible","if","like","refund","I''d","likes","likeing","likeed"],"practiceQuestionTranslation":"교환하시겠어요?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/26/expressions/106/practice-examples/d6b232d1-2251-43f5-a80f-448c4386e131.png","sentenceText":"I''d like to make an appointment for Tuesday.","sentenceWords":["I''d","like","to","make","an","appointment","for","Tuesday"],"highlightingPart":"I''d like","practiceQuestion":"What day would you like to come in?","sentenceTranslation":"화요일로 예약을 잡고 싶은데요.","sentenceWordChoices":["make","I''d","like","likeed","Tuesday","to","likes","an","likeing","appointment","for"],"practiceQuestionTranslation":"어느 날 방문하고 싶으세요?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/26/expressions/106/practice-examples/d3336794-7f03-440b-a5b2-e9dda9bf6cb2.png","sentenceText":"We''d like to order now.","sentenceWords":["We''d","like","to","order","now"],"highlightingPart":"We''d like","practiceQuestion":"Are you ready to order?","sentenceTranslation":"지금 주문할게요.","sentenceWordChoices":["likeing","order","like","to","likeed","We''d","likes","now"],"practiceQuestionTranslation":"주문하시겠어요?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        107,
        26,
        'SCENARIO',
        'CONVERSATION_SKILL',
        'BASIC',
        'EN',
        'KR',
        4,
        'How about ~?',
        '~은 어때?',
        '의견을 묻는 How about ~?',
        '''~은 어때?''라고 제안하거나 상대 의견을 물을 때 가장 만만하게 쓸 수 있는 표현입니다. 뒤에 명사, 동명사, 심지어 문장까지 자유롭게 붙일 수 있어요.',
        'When should we meet?',
        '우리 언제 만날까?',
        'How about lunch tomorrow?',
        '내일 점심 어때?',
        ARRAY['How', 'about', 'lunch', 'tomorrow'],
        ARRAY['about', 'lunch', 'abouts', 'abouting', 'How', 'abouted', 'tomorrow'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/26/expressions/107/practice-examples/13a77f2b-b4ab-423e-a833-1dab7d656781.png","sentenceText":"How about we split the bill?","sentenceWords":["How","about","we","split","the","bill"],"highlightingPart":"How about","practiceQuestion":"The bill is a little high.","sentenceTranslation":"계산 나눠서 하는 거 어때?","sentenceWordChoices":["abouting","we","How","the","bill","abouts","about","abouted","split"],"practiceQuestionTranslation":"계산서가 조금 많이 나왔네."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/26/expressions/107/practice-examples/2e395966-5c59-4af5-84c0-c34775e8a9e5.png","sentenceText":"How about going for a walk?","sentenceWords":["How","about","going","for","a","walk"],"highlightingPart":"How about","practiceQuestion":"What should we do after dinner?","sentenceTranslation":"산책하러 가는 거 어때?","sentenceWordChoices":["abouting","How","abouted","about","a","going","walk","for","abouts"],"practiceQuestionTranslation":"저녁 먹고 뭐 할까?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/26/expressions/107/practice-examples/0d05460a-f2b9-45d0-8fcc-199e8a39a5e9.png","sentenceText":"I''m free on Sunday. How about you?","sentenceWords":["I''m","free","on","Sunday","How","about","you"],"highlightingPart":"How about","practiceQuestion":"Which day works for you?","sentenceTranslation":"난 일요일에 시간 돼. 넌 어때?","sentenceWordChoices":["How","I''m","freeing","you","freeed","about","free","on","Sunday","frees"],"practiceQuestionTranslation":"어느 날에 시간 돼?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/26/expressions/107/practice-examples/269c88d1-16ec-466d-9916-bad7878e993c.png","sentenceText":"How about this one instead?","sentenceWords":["How","about","this","one","instead"],"highlightingPart":"How about","practiceQuestion":"I don''t think this one will work.","sentenceTranslation":"대신 이건 어때?","sentenceWordChoices":["instead","How","about","this","one","abouted","abouting","abouts"],"practiceQuestionTranslation":"이건 잘 안 맞을 것 같아."}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        108,
        27,
        'SCENARIO',
        'OPINION_JUDGMENT',
        'BASIC',
        'EN',
        'KR',
        1,
        'I''d go with ~',
        '나라면 ~로 하겠다',
        '나라면 이걸 고르는 I''d go with ~',
        '선택지 중 하나를 고를 때 ''나라면 ~로 하겠다''라고 추천하는 원어민 단골 표현입니다. choose보다 훨씬 가볍고 자연스러워서 메뉴, 옵션, 계획 어디에나 쓰여요.',
        'Should I get the blue one or the gray one?',
        '파란 거 살까, 회색 거 살까?',
        'I''d go with the gray one.',
        '나라면 회색으로 하겠어.',
        ARRAY['I''d', 'go', 'with', 'the', 'gray', 'one'],
        ARRAY['go', 'without', 'with', 'I''d', 'for', 'the', 'gray', 'one', 'to'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/27/expressions/108/practice-examples/76d2cc7c-27b3-4ddb-867a-2d5a64eb486c.png","sentenceText":"For a first car, I''d go with something used.","sentenceWords":["For","a","first","car","I''d","go","with","something","used"],"highlightingPart":"I''d go with","practiceQuestion":"What kind of car is good for a first-time buyer?","sentenceTranslation":"첫 차라면 난 중고로 하겠어.","sentenceWordChoices":["the","with","to","go","For","a","I''d","used","car","of","first","something"],"practiceQuestionTranslation":"첫 차로는 어떤 차가 좋아?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/27/expressions/108/practice-examples/8e5ab613-2299-495f-81fb-bce8f22c2b68.png","sentenceText":"I''ll go with the pasta.","sentenceWords":["I''ll","go","with","the","pasta"],"highlightingPart":"I''ll go with","practiceQuestion":"What would you like to order?","sentenceTranslation":"전 파스타로 할게요.","sentenceWordChoices":["pasta","with","go","to","for","the","I''ll","without"],"practiceQuestionTranslation":"무엇을 주문하시겠어요?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/27/expressions/108/practice-examples/24fd9fd7-cd35-432e-bda6-4929b13e901d.png","sentenceText":"If I were you, I''d go with the cheaper plan.","sentenceWords":["If","I","were","you","I''d","go","with","the","cheaper","plan"],"highlightingPart":"I''d go with","practiceQuestion":"Which plan should I choose?","sentenceTranslation":"나라면 더 싼 요금제로 하겠어.","sentenceWordChoices":["with","I''d","If","I","are","we","cheaper","were","go","plan","you","he","the"],"practiceQuestionTranslation":"어느 요금제를 골라야 할까?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/27/expressions/108/practice-examples/95028080-8310-43f9-bd03-9f4d7d08650f.png","sentenceText":"Let''s go with your idea.","sentenceWords":["Let''s","go","with","your","idea"],"highlightingPart":"go with","practiceQuestion":"Whose idea should we use?","sentenceTranslation":"네 아이디어로 가자.","sentenceWordChoices":["for","without","idea","to","your","with","Let''s","go"],"practiceQuestionTranslation":"누구 아이디어로 할까?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        109,
        27,
        'SCENARIO',
        'POLITE_EXPRESSION',
        'BASIC',
        'EN',
        'KR',
        2,
        'would rather',
        '(차라리) ~하는 게 낫다, ~을 더 선호하다',
        '차라리 이게 낫다고 말하는 would rather',
        '''(차라리) ~하는 게 낫다'', ''~을 더 선호한다''라고 두 선택지 중 하나를 고를 때 쓰는 표현입니다. would rather A than B로 비교하고 뒤에 동사원형이 온다는 게 포인트이며, 정중하게 다른 걸 원한다고 말하거나 거절할 때도 부드럽게 쓰여요.',
        'Do you want to eat out or cook tonight?',
        '오늘 밤 나가서 먹을래, 해 먹을래?',
        'I''d rather just cook at home tonight.',
        '오늘 밤엔 그냥 집에서 해 먹는 게 좋겠어.',
        ARRAY['I''d', 'rather', 'just', 'cook', 'at', 'home', 'tonight'],
        ARRAY['at', 'rathered', 'home', 'rathering', 'cook', 'rather', 'tonight', 'I''d', 'just', 'rathers'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/27/expressions/109/practice-examples/374b2130-63e4-427c-875a-81f368813705.png","sentenceText":"I''d rather have coffee than tea.","sentenceWords":["I''d","rather","have","coffee","than","tea"],"highlightingPart":"I''d rather","practiceQuestion":"Would you like coffee or tea?","sentenceTranslation":"차보다 커피가 더 좋아.","sentenceWordChoices":["coffee","rathered","rather","tea","rathering","rathers","I''d","have","than"],"practiceQuestionTranslation":"커피와 차 중 뭐가 좋아?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/27/expressions/109/practice-examples/5ec50d1b-313d-4f53-a422-cf135e38d26d.png","sentenceText":"I''d rather not talk about it.","sentenceWords":["I''d","rather","not","talk","about","it"],"highlightingPart":"I''d rather","practiceQuestion":"Do you want to talk about what happened?","sentenceTranslation":"그 얘긴 안 하는 게 좋겠어.","sentenceWordChoices":["not","about","rathered","rather","it","talk","I''d","rathering","rathers"],"practiceQuestionTranslation":"무슨 일이 있었는지 얘기하고 싶어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/27/expressions/109/practice-examples/cdd8f9e9-6ef4-42f4-a41d-dfc06d109fd2.png","sentenceText":"Would you rather meet on Friday or Saturday?","sentenceWords":["Would","you","rather","meet","on","Friday","or","Saturday"],"highlightingPart":"Would you rather","practiceQuestion":"Which day should we meet?","sentenceTranslation":"금요일이 나아, 토요일이 나아?","sentenceWordChoices":["Saturday","will","on","meet","could","should","Would","rather","you","or","Friday"],"practiceQuestionTranslation":"어느 날 만날까?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/27/expressions/109/practice-examples/bafad5f8-def9-428b-9863-781fd3cd65f5.png","sentenceText":"She''d rather walk than take the bus.","sentenceWords":["She''d","rather","walk","than","take","the","bus"],"highlightingPart":"She''d rather","practiceQuestion":"Does she want to take the bus?","sentenceTranslation":"걔는 버스 타느니 걷는 걸 더 좋아해.","sentenceWordChoices":["than","rathers","She''d","rather","take","rathered","bus","the","rathering","walk"],"practiceQuestionTranslation":"걔는 버스 타고 싶어 해?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        110,
        27,
        'SCENARIO',
        'POLITE_EXPRESSION',
        'BASIC',
        'EN',
        'KR',
        3,
        'Do you happen to know ~?',
        '혹시 ~ 알아?',
        '혹시 아는지 묻는 Do you happen to know ~?',
        'know 앞에 happen to를 넣으면 ''혹시 아세요?''라는 조심스러운 질문이 됩니다. 상대가 모를 수도 있다는 전제를 깔아줘서, 모른다고 답해도 서로 어색하지 않아요.',
        'You''ve lived here a while, right?',
        '너 여기 오래 살았지?',
        'Do you happen to know a good dentist around here?',
        '혹시 이 근처 괜찮은 치과 알아?',
        ARRAY['Do', 'you', 'happen', 'to', 'know', 'a', 'good', 'dentist', 'around', 'here'],
        ARRAY['a', 'know', 'dentist', 'did', 'you', 'Do', 'around', 'doing', 'to', 'happen', 'here', 'good', 'does'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/27/expressions/110/practice-examples/a31a8c7d-45d9-401f-9d91-dd4dc0fe7c64.png","sentenceText":"Do you happen to know what time it closes?","sentenceWords":["Do","you","happen","to","know","what","time","it","closes"],"highlightingPart":"happen to know","practiceQuestion":"I wonder when that bakery closes.","sentenceTranslation":"혹시 몇 시에 닫는지 알아?","sentenceWordChoices":["happen","know","time","it","did","you","closes","to","what","does","Do","doing"],"practiceQuestionTranslation":"그 빵집이 몇 시에 닫는지 궁금해."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/27/expressions/110/practice-examples/de39e85a-2a77-4ccf-b5d6-7e4c217ab883.png","sentenceText":"Do you happen to know her number?","sentenceWords":["Do","you","happen","to","know","her","number"],"highlightingPart":"happen to know","practiceQuestion":"How can I contact her?","sentenceTranslation":"혹시 걔 번호 알아?","sentenceWordChoices":["you","number","did","to","Do","her","doing","happen","does","know"],"practiceQuestionTranslation":"걔한테 어떻게 연락하지?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/27/expressions/110/practice-examples/96384643-f2a5-4c12-99b3-f514231ec84b.png","sentenceText":"Would you happen to know where Gate 3 is?","sentenceWords":["Would","you","happen","to","know","where","Gate","3","is"],"highlightingPart":"happen to know","practiceQuestion":"Excuse me, I''m looking for Gate 3.","sentenceTranslation":"혹시 3번 게이트가 어딘지 아세요?","sentenceWordChoices":["you","Would","where","to","is","should","could","happen","will","3","know","Gate"],"practiceQuestionTranslation":"실례합니다. 3번 게이트를 찾고 있어요."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/27/expressions/110/practice-examples/1b0bbb1c-0c65-4da9-899e-47e4cd8eb2f9.png","sentenceText":"Do you happen to have a pen on you?","sentenceWords":["Do","you","happen","to","have","a","pen","on","you"],"highlightingPart":"happen","practiceQuestion":"I need to write something down.","sentenceTranslation":"혹시 펜 갖고 있어?","sentenceWordChoices":["doing","pen","does","to","you","on","a","did","you","Do","have","happen"],"practiceQuestionTranslation":"적을 게 있는데."}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        111,
        27,
        'SCENARIO',
        'POLITE_EXPRESSION',
        'BASIC',
        'EN',
        'KR',
        4,
        'If it''s not too much trouble',
        '번거롭지 않으시다면',
        '부탁의 무게를 덜어주는 If it''s not too much trouble',
        '''많이 번거롭지 않으시다면''이라고 부탁의 무게를 덜어주는 표현입니다. 상대가 들일 수고를 인정해 주는 말이라, 사소한 부탁도 훨씬 공손해져요.',
        'Can I get you anything else?',
        '더 필요하신 거 있으세요?',
        'If it''s not too much trouble, could I get some more water?',
        '번거롭지 않으시다면 물 좀 더 주시겠어요?',
        ARRAY['If', 'it''s', 'not', 'too', 'much', 'trouble', 'could', 'I', 'get', 'some', 'more', 'water'],
        ARRAY['If', 'some', 'it''s', 'more', 'too', 'trouble', 'muching', 'much', 'could', 'muched', 'water', 'muchs', 'not', 'I', 'get'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/27/expressions/111/practice-examples/48d14d77-ed5f-4d51-9ad3-8e7e1ca88def.png","sentenceText":"If it''s not too much trouble, could you drop this off on your way?","sentenceWords":["If","it''s","not","too","much","trouble","could","you","drop","this","off","on","your","way"],"highlightingPart":"If it''s not too much trouble","practiceQuestion":"I''m going past the post office on my way home.","sentenceTranslation":"번거롭지 않으면 가는 길에 이것 좀 갖다줄래?","sentenceWordChoices":["muchs","off","it''s","much","on","this","not","you","drop","could","muching","If","trouble","too","your","way","muched"],"practiceQuestionTranslation":"집에 가는 길에 우체국 앞을 지나가."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/27/expressions/111/practice-examples/866033b9-3308-4c67-adbd-dcf87a056cee.png","sentenceText":"I''d like a receipt, if it''s not too much trouble.","sentenceWords":["I''d","like","a","receipt","if","it''s","not","too","much","trouble"],"highlightingPart":"if it''s not too much trouble","practiceQuestion":"Do you need anything else?","sentenceTranslation":"번거롭지 않으시다면 영수증 부탁드려요.","sentenceWordChoices":["likeing","a","like","too","I''d","receipt","trouble","likeed","much","likes","not","it''s","if"],"practiceQuestionTranslation":"더 필요한 게 있으세요?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/27/expressions/111/practice-examples/8edc9206-19e5-4e76-964f-67fc4ababcee.png","sentenceText":"If it''s not too much trouble, would you mind taking a photo of us?","sentenceWords":["If","it''s","not","too","much","trouble","would","you","mind","taking","a","photo","of","us"],"highlightingPart":"If it''s not too much trouble","practiceQuestion":"Would you like me to take your photo?","sentenceTranslation":"괜찮으시다면 저희 사진 좀 찍어주시겠어요?","sentenceWordChoices":["would","much","muchs","you","not","it''s","trouble","photo","mind","of","muched","too","us","muching","If","a","taking"],"practiceQuestionTranslation":"사진을 찍어 드릴까요?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/27/expressions/111/practice-examples/dbf6c4f6-53e0-4ff2-a547-fa707a808a5e.png","sentenceText":"It''s no trouble at all.","sentenceWords":["It''s","no","trouble","at","all"],"highlightingPart":"no trouble at all","practiceQuestion":"Sorry for making extra work for you.","sentenceTranslation":"전혀 번거롭지 않아요.","sentenceWordChoices":["at","all","no","troubleing","troubles","It''s","troubleed","trouble"],"practiceQuestionTranslation":"번거롭게 해서 미안해요."}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        112,
        28,
        'SCENARIO',
        'TIME_PLANNING',
        'BASIC',
        'EN',
        'KR',
        1,
        'have in mind',
        '염두에 두다',
        '생각해 둔 것을 묻는 have in mind',
        '''염두에 두다'', ''생각해 둔 게 있다''라는 뜻으로, 상대가 마음속으로 그리고 있는 것을 물을 때 자주 씁니다. 특히 쇼핑, 계획, 아이디어 관련 대화에서 빛을 발해요.',
        'Where should we go for dinner?',
        '저녁 어디로 갈까?',
        'What do you have in mind?',
        '생각해 둔 거 있어?',
        ARRAY['What', 'do', 'you', 'have', 'in', 'mind'],
        ARRAY['Whating', 'mind', 'in', 'What', 'Whats', 'do', 'have', 'Whated', 'you'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/28/expressions/112/practice-examples/2f956c8e-5929-45ec-851c-815f38be320b.png","sentenceText":"Do you have a specific date in mind?","sentenceWords":["Do","you","have","a","specific","date","in","mind"],"highlightingPart":"have a specific date in mind","practiceQuestion":"Let''s choose a date for the trip.","sentenceTranslation":"특별히 생각해 둔 날짜 있어?","sentenceWordChoices":["doing","have","does","you","in","a","mind","Do","did","specific","date"],"practiceQuestionTranslation":"여행 날짜를 정해 보자."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/28/expressions/112/practice-examples/f123dc1e-691e-451a-ae3f-ed2421ff849e.png","sentenceText":"I have a restaurant in mind for dinner.","sentenceWords":["I","have","a","restaurant","in","mind","for","dinner"],"highlightingPart":"have a restaurant in mind","practiceQuestion":"Do you know where we should eat?","sentenceTranslation":"저녁 먹을 식당 생각해 둔 데가 있어.","sentenceWordChoices":["in","dinner","you","mind","I","we","for","restaurant","he","a","have"],"practiceQuestionTranslation":"어디에서 먹을지 생각해 둔 곳 있어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/28/expressions/112/practice-examples/9851cbf7-033e-4345-bca8-f5d2c0021ba8.png","sentenceText":"What kind of design do you have in mind?","sentenceWords":["What","kind","of","design","do","you","have","in","mind"],"highlightingPart":"have in mind","practiceQuestion":"I''ll start on the final design today.","sentenceTranslation":"어떤 디자인을 생각하고 계세요?","sentenceWordChoices":["have","design","mind","you","of","in","Whats","What","do","Whating","kind","Whated"],"practiceQuestionTranslation":"오늘 최종 디자인 작업을 시작할게요."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/28/expressions/112/practice-examples/bf468a5d-93ce-44be-8041-0163350de629.png","sentenceText":"I had someone in mind for the position.","sentenceWords":["I","had","someone","in","mind","for","the","position"],"highlightingPart":"in mind","practiceQuestion":"Who did you want to hire?","sentenceTranslation":"그 자리에 염두에 둔 사람이 있었어.","sentenceWordChoices":["position","in","for","mind","he","someone","we","had","you","I","the"],"practiceQuestionTranslation":"누구를 채용하려고 했어?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        113,
        28,
        'SCENARIO',
        'WORK_STUDY',
        'BASIC',
        'EN',
        'KR',
        2,
        'make use of',
        '~을 잘 활용하다',
        '알뜰하게 활용하는 make use of',
        '그냥 쓰는(use) 것을 넘어 ''가치 있게 활용하다''라는 뉘앙스의 표현입니다. 남는 자원, 시간, 기회를 허투루 버리지 않고 써먹을 때 어울려요.',
        'We have an hour until the next meeting.',
        '다음 회의까지 한 시간 남았어.',
        'Let''s make use of this free time.',
        '이 남는 시간을 잘 활용하자.',
        ARRAY['Let''s', 'make', 'use', 'of', 'this', 'free', 'time'],
        ARRAY['free', 'of', 'use', 'makeed', 'makes', 'Let''s', 'make', 'makeing', 'time', 'this'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/28/expressions/113/practice-examples/cab43a7c-1b70-4d8d-85fa-2375ae135cbd.png","sentenceText":"You should make use of the hotel gym.","sentenceWords":["You","should","make","use","of","the","hotel","gym"],"highlightingPart":"make use of","practiceQuestion":"How can I exercise while traveling?","sentenceTranslation":"호텔 헬스장을 활용해 봐.","sentenceWordChoices":["the","make","use","of","I","You","we","hotel","gym","should","they"],"practiceQuestionTranslation":"여행 중에 어떻게 운동할 수 있을까?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/28/expressions/113/practice-examples/813ef284-c08c-4ae1-a961-c0d6eac3affd.png","sentenceText":"We made good use of the extra space.","sentenceWords":["We","made","good","use","of","the","extra","space"],"highlightingPart":"made good use of","practiceQuestion":"What did you do with the extra room?","sentenceTranslation":"남는 공간을 알차게 활용했어.","sentenceWordChoices":["space","the","you","extra","We","of","I","good","use","they","made"],"practiceQuestionTranslation":"남는 공간을 어떻게 활용했어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/28/expressions/113/practice-examples/d45e6587-e479-4e48-941a-1f57d45e7623.png","sentenceText":"Make use of every opportunity you get.","sentenceWords":["Make","use","of","every","opportunity","you","get"],"highlightingPart":"Make use of","practiceQuestion":"How should I make the most of new opportunities?","sentenceTranslation":"주어지는 기회는 다 활용해.","sentenceWordChoices":["opportunity","you","use","Makeing","get","every","of","Make","Makes","Makeed"],"practiceQuestionTranslation":"새로운 기회를 어떻게 잘 활용하면 좋을까?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/28/expressions/113/practice-examples/a7ce390f-1092-41f2-b119-5d66c35fbc58.png","sentenceText":"I made use of my commute to listen to podcasts.","sentenceWords":["I","made","use","of","my","commute","to","listen","to","podcasts"],"highlightingPart":"made use of","practiceQuestion":"How do you spend your commute?","sentenceTranslation":"출퇴근 시간을 팟캐스트 듣는 데 활용했어.","sentenceWordChoices":["to","I","we","commute","use","listen","of","you","made","he","podcasts","to","my"],"practiceQuestionTranslation":"출퇴근 시간을 어떻게 보내?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        114,
        28,
        'SCENARIO',
        'OPINION_JUDGMENT',
        'BASIC',
        'EN',
        'KR',
        3,
        'on the fence',
        '결정 못 하고 고민 중인',
        '결정을 못 내릴 때 on the fence',
        '''울타리 위에 걸터앉은'', 즉 ''이쪽도 저쪽도 결정 못 하고 고민 중인'' 상태를 뜻합니다. 두 선택지 사이에서 갈팡질팡할 때 딱 맞는 표현이에요.',
        'Have you booked the trip yet?',
        '여행 예약했어?',
        'I''m still on the fence about the trip.',
        '여행 갈지 말지 아직 고민 중이야.',
        ARRAY['I''m', 'still', 'on', 'the', 'fence', 'about', 'the', 'trip'],
        ARRAY['the', 'stills', 'fence', 'trip', 'on', 'still', 'stilled', 'the', 'stilling', 'I''m', 'about'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/28/expressions/114/practice-examples/136ccb71-a81b-4359-b86d-609da96c2d6c.png","sentenceText":"She''s on the fence about changing jobs.","sentenceWords":["She''s","on","the","fence","about","changing","jobs"],"highlightingPart":"on the fence","practiceQuestion":"Has she decided whether to change jobs?","sentenceTranslation":"걔 이직할지 말지 갈팡질팡하고 있어.","sentenceWordChoices":["fence","jobs","onto","on","about","in","the","She''s","changing","at"],"practiceQuestionTranslation":"걔는 이직할지 결정했어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/28/expressions/114/practice-examples/2d947f97-7d76-4a88-a14d-84401428fa7d.png","sentenceText":"I was on the fence, but the reviews convinced me.","sentenceWords":["I","was","on","the","fence","but","the","reviews","convinced","me"],"highlightingPart":"on the fence","practiceQuestion":"What finally helped you decide?","sentenceTranslation":"고민했는데 후기 보고 마음 정했어.","sentenceWordChoices":["was","I","but","we","convinced","he","the","on","you","the","me","fence","reviews"],"practiceQuestionTranslation":"결국 무엇 때문에 결정했어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/28/expressions/114/practice-examples/b335bbaa-2dff-4792-bac0-5bd5d34755a5.png","sentenceText":"Are you still on the fence about which one to buy?","sentenceWords":["Are","you","still","on","the","fence","about","which","one","to","buy"],"highlightingPart":"on the fence","practiceQuestion":"I still haven''t picked one.","sentenceTranslation":"어떤 걸 살지 아직도 못 정했어?","sentenceWordChoices":["were","you","about","buy","the","fence","is","to","on","still","Are","be","which","one"],"practiceQuestionTranslation":"아직 어느 걸 살지 못 정했어."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/28/expressions/114/practice-examples/dafb19aa-d586-46a5-b040-f6bf877d44cb.png","sentenceText":"Stop sitting on the fence and pick a side.","sentenceWords":["Stop","sitting","on","the","fence","and","pick","a","side"],"highlightingPart":"on the fence","practiceQuestion":"I keep going back and forth between the two sides.","sentenceTranslation":"그만 애매하게 굴고 한쪽을 골라.","sentenceWordChoices":["Stops","the","side","a","Stoped","Stoping","sitting","Stop","pick","fence","and","on"],"practiceQuestionTranslation":"두 쪽 사이에서 계속 갈팡질팡하고 있어."}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        115,
        28,
        'SCENARIO',
        'MONEY_SPENDING',
        'BASIC',
        'EN',
        'KR',
        4,
        'can/can''t afford',
        '~할 (금전적) 여유가 있다/없다',
        '금전적 부담을 표현하는 can/can''t afford',
        '''~을 살 여유가 있다/없다''라고 경제적 부담을 말하는 표현입니다. barely afford(간신히 감당하다)처럼 부사와 함께 쓰면 뉘앙스를 더 살릴 수 있어요.',
        'Why don''t you just buy a new car?',
        '새 차 그냥 사지 그래?',
        'I can''t afford a new car.',
        '새 차 살 여유가 없어.',
        ARRAY['I', 'can''t', 'afford', 'a', 'new', 'car'],
        ARRAY['we', 'I', 'you', 'afford', 'car', 'a', 'he', 'new', 'can''t'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/28/expressions/115/practice-examples/974230d3-c146-4f40-81be-1d542beba57f.png","sentenceText":"We can''t afford to eat out every day.","sentenceWords":["We","can''t","afford","to","eat","out","every","day"],"highlightingPart":"can''t afford","practiceQuestion":"Why don''t you eat out more often?","sentenceTranslation":"매일 외식할 형편은 안 돼.","sentenceWordChoices":["out","I","can''t","every","afford","eat","to","day","you","they","We"],"practiceQuestionTranslation":"왜 외식을 더 자주 하지 않아?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/28/expressions/115/practice-examples/46256e88-4f6f-4c80-a26e-e62cb0a2fbfe.png","sentenceText":"Can you afford the rent there?","sentenceWords":["Can","you","afford","the","rent","there"],"highlightingPart":"Can you afford","practiceQuestion":"I''m thinking about renting an apartment there.","sentenceTranslation":"거기 월세 감당할 수 있겠어?","sentenceWordChoices":["would","there","you","afford","could","rent","should","the","Can"],"practiceQuestionTranslation":"거기에 아파트를 빌릴까 생각 중이야."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/28/expressions/115/practice-examples/541cacfe-8518-4337-b3b2-3bcdc9f1674c.png","sentenceText":"I could barely afford my tuition.","sentenceWords":["I","could","barely","afford","my","tuition"],"highlightingPart":"could barely afford","practiceQuestion":"How did you pay for college?","sentenceTranslation":"등록금을 간신히 낼 수 있었어.","sentenceWordChoices":["barely","my","we","tuition","you","afford","I","could","he"],"practiceQuestionTranslation":"대학 등록금은 어떻게 냈어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/28/expressions/115/practice-examples/c622c5ea-3c3b-49e6-a108-a4310b22db97.png","sentenceText":"I can''t afford to lose this job.","sentenceWords":["I","can''t","afford","to","lose","this","job"],"highlightingPart":"can''t afford","practiceQuestion":"Why can''t you quit your job?","sentenceTranslation":"이 일자리를 잃을 여유가 없어.","sentenceWordChoices":["I","can''t","afford","lose","we","to","this","you","job","he"],"practiceQuestionTranslation":"왜 일을 그만둘 수 없어?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        116,
        29,
        'SCENARIO',
        'TIME_PLANNING',
        'CLASSIC_COMMON',
        'EN',
        'KR',
        1,
        'play it by ear',
        '상황 봐가며 정하다, 즉흥적으로 하다',
        '계획 없이 상황 봐서 하는 play it by ear',
        '''즉흥적으로 하다'', ''상황 봐가면서 정하다''라는 뜻의 관용구입니다. 악보 없이 귀로 듣고 연주한다는 데서 온 표현으로, 미리 계획을 못 박기 싫을 때 딱이에요.',
        'What''s the plan for Saturday?',
        '토요일 계획이 뭐야?',
        'No plans yet. Let''s just play it by ear.',
        '아직 계획 없어. 그냥 상황 봐서 하자.',
        ARRAY['No', 'plans', 'yet', 'Let''s', 'just', 'play', 'it', 'by', 'ear'],
        ARRAY['it', 'plan', 'planed', 'ear', 'play', 'yet', 'just', 'Let''s', 'No', 'plans', 'planing', 'by'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/29/expressions/116/practice-examples/90c93b5d-aba3-42da-8d21-63f07a1c438c.png","sentenceText":"We might eat out, or cook — we''ll play it by ear.","sentenceWords":["We","might","eat","out","or","cook","we''ll","play","it","by","ear"],"highlightingPart":"play it by ear","practiceQuestion":"Are we eating out or cooking?","sentenceTranslation":"외식할 수도 있고 해 먹을 수도 있고, 봐서 정하려고.","sentenceWordChoices":["I","we''ll","by","might","eat","play","it","or","cook","they","We","ear","you","out"],"practiceQuestionTranslation":"외식할 거야, 아니면 요리할 거야?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/29/expressions/116/practice-examples/5841aa05-a33b-4d79-965d-89fb149e372f.png","sentenceText":"I don''t know how long I''ll stay. I''ll play it by ear.","sentenceWords":["I","don''t","know","how","long","I''ll","stay","I''ll","play","it","by","ear"],"highlightingPart":"play it by ear","practiceQuestion":"How long are you planning to stay?","sentenceTranslation":"얼마나 있을지 몰라. 상황 봐서 정할래.","sentenceWordChoices":["I''ll","play","by","don''t","you","long","ear","I","stay","he","we","how","know","it","I''ll"],"practiceQuestionTranslation":"얼마나 머물 계획이야?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/29/expressions/116/practice-examples/cc5ee01d-173e-4630-8d44-7f04318f61a5.png","sentenceText":"The weather looks iffy, so let''s play it by ear.","sentenceWords":["The","weather","looks","iffy","so","let''s","play","it","by","ear"],"highlightingPart":"play it by ear","practiceQuestion":"What should we do if the weather changes?","sentenceTranslation":"날씨가 애매하니까 봐가면서 하자.","sentenceWordChoices":["The","play","some","an","it","weather","iffy","ear","so","let''s","looks","a","by"],"practiceQuestionTranslation":"날씨가 바뀌면 어떻게 할까?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/29/expressions/116/practice-examples/2039e0e3-224f-4238-b61a-83b559ee92d8.png","sentenceText":"She likes plans, but I prefer playing it by ear.","sentenceWords":["She","likes","plans","but","I","prefer","playing","it","by","ear"],"highlightingPart":"playing it by ear","practiceQuestion":"Does she prefer making detailed plans?","sentenceTranslation":"걔는 계획파인데 난 즉흥파야.","sentenceWordChoices":["likeing","it","likes","prefer","plans","by","like","She","likeed","but","I","ear","playing"],"practiceQuestionTranslation":"걔는 꼼꼼하게 계획하는 걸 좋아해?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        117,
        29,
        'SCENARIO',
        'OPINION_JUDGMENT',
        'BASIC',
        'EN',
        'KR',
        2,
        'have second thoughts',
        '다시 생각하니 망설여지다, 마음이 흔들리다',
        '마음이 흔들릴 때 have second thoughts',
        '''다시 생각해 보니 망설여지다'', ''마음이 흔들리다''라는 뜻입니다. 이미 내린 결정에 대해 확신이 사라지기 시작할 때 쓰는 표현이에요.',
        'Are you excited for the plan?',
        '그 계획 기대돼?',
        'I''m having second thoughts about the plan.',
        '그 계획, 다시 생각하니 망설여져.',
        ARRAY['I''m', 'having', 'second', 'thoughts', 'about', 'the', 'plan'],
        ARRAY['haved', 'about', 'second', 'plan', 'havs', 'having', 'the', 'hav', 'thoughts', 'I''m'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/29/expressions/117/practice-examples/b44f455a-e088-40e1-bd77-fe0851912a80.png","sentenceText":"She''s having second thoughts about the wedding.","sentenceWords":["She''s","having","second","thoughts","about","the","wedding"],"highlightingPart":"having second thoughts","practiceQuestion":"Is she still sure about getting married?","sentenceTranslation":"걔 결혼에 대해 마음이 흔들리고 있어.","sentenceWordChoices":["wedding","hav","haved","having","thoughts","havs","She''s","about","second","the"],"practiceQuestionTranslation":"걔는 아직 결혼할 마음이 확실해?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/29/expressions/117/practice-examples/3452041b-66e0-4481-86a3-5ca2fff3e393.png","sentenceText":"No second thoughts — just go for it.","sentenceWords":["No","second","thoughts","just","go","for","it"],"highlightingPart":"second thoughts","practiceQuestion":"I''m nervous about doing this.","sentenceTranslation":"망설이지 말고 그냥 질러.","sentenceWordChoices":["just","seconded","it","for","thoughts","No","go","seconds","seconding","second"],"practiceQuestionTranslation":"이걸 하려니 망설여져."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/29/expressions/117/practice-examples/d8179c04-c8c0-45f9-b806-dcdcc840ef41.png","sentenceText":"He had second thoughts and canceled the order.","sentenceWords":["He","had","second","thoughts","and","canceled","the","order"],"highlightingPart":"had second thoughts","practiceQuestion":"Why did he cancel the order?","sentenceTranslation":"걔 다시 생각해 보고 주문을 취소했어.","sentenceWordChoices":["order","have","He","had","having","and","second","the","thoughts","canceled","has"],"practiceQuestionTranslation":"걔는 왜 주문을 취소했어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/29/expressions/117/practice-examples/df21e2cf-0cc0-4ade-97d1-86675138b9bd.png","sentenceText":"Are you having second thoughts about moving?","sentenceWords":["Are","you","having","second","thoughts","about","moving"],"highlightingPart":"having second thoughts","practiceQuestion":"I''m not as sure about moving as I was before.","sentenceTranslation":"이사하는 거 다시 고민되는 거야?","sentenceWordChoices":["be","you","about","Are","having","moving","thoughts","were","second","is"],"practiceQuestionTranslation":"이사하는 게 전처럼 확신이 안 서."}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        118,
        29,
        'SCENARIO',
        'TIME_PLANNING',
        'BASIC',
        'EN',
        'KR',
        3,
        'up in the air',
        '아직 미정인, 불확실한',
        '아직 미정일 때 up in the air',
        '''공중에 붕 떠 있는'', 즉 ''아직 결정되지 않은'' 상태를 뜻합니다. 계획이나 일정이 확정되지 못하고 유동적일 때 딱 맞는 표현이에요.',
        'When exactly are you traveling?',
        '여행 정확히 언제 가?',
        'Our travel plans are still up in the air.',
        '우리 여행 계획은 아직 미정이야.',
        ARRAY['Our', 'travel', 'plans', 'are', 'still', 'up', 'in', 'the', 'air'],
        ARRAY['in', 'traveling', 'are', 'travel', 'still', 'traveled', 'travels', 'Our', 'plans', 'the', 'up', 'air'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/29/expressions/118/practice-examples/60cb5c0d-6e92-4490-8ec4-323c2592b69a.png","sentenceText":"The wedding date is up in the air.","sentenceWords":["The","wedding","date","is","up","in","the","air"],"highlightingPart":"up in the air","practiceQuestion":"Have they chosen a wedding date?","sentenceTranslation":"결혼 날짜는 아직 안 정해졌어.","sentenceWordChoices":["in","a","the","The","some","air","date","up","wedding","an","is"],"practiceQuestionTranslation":"결혼 날짜 정했대?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/29/expressions/118/practice-examples/a6575730-c156-48cd-8712-d3989717b17f.png","sentenceText":"Everything''s up in the air until we hear back.","sentenceWords":["Everything''s","up","in","the","air","until","we","hear","back"],"highlightingPart":"up in the air","practiceQuestion":"When will we know the final plan?","sentenceTranslation":"답을 듣기 전까진 모든 게 불확실해.","sentenceWordChoices":["in","hear","at","into","until","Everything''s","we","back","the","air","on","up"],"practiceQuestionTranslation":"최종 계획은 언제 알 수 있어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/29/expressions/118/practice-examples/3b5d070d-0f9e-4612-9dd1-f5839e7788a5.png","sentenceText":"My job situation is kind of up in the air right now.","sentenceWords":["My","job","situation","is","kind","of","up","in","the","air","right","now"],"highlightingPart":"up in the air","practiceQuestion":"Have you decided what to do about your job?","sentenceTranslation":"내 직장 상황이 지금 좀 불확실해.","sentenceWordChoices":["up","My","right","situation","in","our","the","now","your","his","air","is","job","of","kind"],"practiceQuestionTranslation":"직장 문제는 어떻게 할지 정했어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/29/expressions/118/practice-examples/e1f2b17f-a3e8-4a26-a7a4-1147c0ed6be5.png","sentenceText":"Let''s not book anything while things are up in the air.","sentenceWords":["Let''s","not","book","anything","while","things","are","up","in","the","air"],"highlightingPart":"up in the air","practiceQuestion":"Should we book the tickets now?","sentenceTranslation":"상황이 불확실할 땐 아무것도 예약하지 말자.","sentenceWordChoices":["books","while","book","air","up","anything","Let''s","not","in","are","booked","things","the","booking"],"practiceQuestionTranslation":"지금 표를 예약할까?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        119,
        29,
        'SCENARIO',
        'GRAMMAR_FUNCTION_WORD',
        'BASIC',
        'EN',
        'KR',
        4,
        'unless',
        '~하지 않는 한',
        '조건을 나타내는 unless',
        '''~하지 않는 한'', ''~가 아니라면''이라는 뜻의 접속사로, if not을 한 단어로 표현합니다. 예외 조건을 깔끔하게 제시할 수 있어 문장이 세련돼져요.',
        'Will you go to the gym today?',
        '오늘 헬스장 갈 거야?',
        'I won''t go unless you come with me.',
        '네가 같이 가지 않는 한 나 안 가.',
        ARRAY['I', 'won''t', 'go', 'unless', 'you', 'come', 'with', 'me'],
        ARRAY['unlesss', 'won''t', 'I', 'you', 'me', 'with', 'come', 'he', 'go', 'we', 'unless'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/29/expressions/119/practice-examples/1e120dfe-69f8-4b2a-9802-a762207f156b.png","sentenceText":"Don''t call me unless it''s urgent.","sentenceWords":["Don''t","call","me","unless","it''s","urgent"],"highlightingPart":"unless","practiceQuestion":"When is it okay to call you?","sentenceTranslation":"급한 일 아니면 전화하지 마.","sentenceWordChoices":["calls","urgent","call","calling","it''s","called","Don''t","me","unless"],"practiceQuestionTranslation":"언제 전화해도 돼?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/29/expressions/119/practice-examples/5b97cc08-901f-4e52-9e6f-7ca6048ccdd6.png","sentenceText":"Unless it rains, we''ll have the picnic.","sentenceWords":["Unless","it","rains","we''ll","have","the","picnic"],"highlightingPart":"Unless","practiceQuestion":"What if it rains tomorrow?","sentenceTranslation":"비만 안 오면 피크닉 할 거야.","sentenceWordChoices":["it","Unlessed","Unless","have","rains","picnic","the","we''ll","Unlesss","Unlessing"],"practiceQuestionTranslation":"내일 비가 오면 어떡해?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/29/expressions/119/practice-examples/888fc81f-9623-4bf5-9bf5-982d375b14fb.png","sentenceText":"I don''t drink coffee unless I''m really tired.","sentenceWords":["I","don''t","drink","coffee","unless","I''m","really","tired"],"highlightingPart":"unless","practiceQuestion":"When do you drink coffee?","sentenceTranslation":"진짜 피곤하지 않으면 커피 안 마셔.","sentenceWordChoices":["I''m","really","unless","we","don''t","he","you","drink","I","tired","coffee"],"practiceQuestionTranslation":"커피는 언제 마셔?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/29/expressions/119/practice-examples/2209c873-82e8-4a32-a766-b49128a57f3a.png","sentenceText":"You can''t enter unless you have a ticket.","sentenceWords":["You","can''t","enter","unless","you","have","a","ticket"],"highlightingPart":"unless","practiceQuestion":"What do I need to get inside?","sentenceTranslation":"표가 없으면 입장할 수 없어요.","sentenceWordChoices":["enter","You","we","can''t","unless","ticket","a","have","you","I","they"],"practiceQuestionTranslation":"입장하려면 무엇이 필요해요?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        120,
        30,
        'SCENARIO',
        'POLITE_EXPRESSION',
        'BASIC',
        'EN',
        'KR',
        1,
        'Sorry to bother you, but',
        '귀찮게 해서 죄송한데요',
        '말 걸기 전에 붙이는 Sorry to bother you, but',
        '상대의 시간을 뺏기 전에 ''방해해서 미안한데요''라고 먼저 깔아주는 쿠션 표현입니다. 사무실, 가게, 길거리 어디서든 말 걸기의 정석 오프너예요.',
        'Yes? I''m in the middle of something.',
        '네? 제가 지금 뭐 좀 하는 중이라서요.',
        'Sorry to bother you, but do you have a minute?',
        '바쁘신데 죄송한데, 잠깐 시간 되세요?',
        ARRAY['Sorry', 'to', 'bother', 'you', 'but', 'do', 'you', 'have', 'a', 'minute'],
        ARRAY['you', 'have', 'but', 'Sorrying', 'bother', 'Sorryed', 'a', 'to', 'minute', 'do', 'Sorrys', 'Sorry', 'you'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/30/expressions/120/practice-examples/e9544d3c-6fbd-45b9-9423-1696d867cb93.png","sentenceText":"Sorry to bother you, but is this seat taken?","sentenceWords":["Sorry","to","bother","you","but","is","this","seat","taken"],"highlightingPart":"Sorry to bother you, but","practiceQuestion":"You need to ask a stranger whether the empty seat is available.","sentenceTranslation":"실례지만 이 자리 주인 있나요?","sentenceWordChoices":["but","bother","Sorrys","to","seat","Sorrying","Sorry","taken","this","you","is","Sorryed"],"practiceQuestionTranslation":"낯선 사람에게 빈자리를 사용해도 되는지 물어봐야 해."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/30/expressions/120/practice-examples/7122aa6b-404b-4c0b-b88e-a4eb5a680778.png","sentenceText":"Sorry to bother you so late.","sentenceWords":["Sorry","to","bother","you","so","late"],"highlightingPart":"Sorry to bother","practiceQuestion":"Why are you calling me at this hour?","sentenceTranslation":"이렇게 늦게 연락해서 미안해요.","sentenceWordChoices":["Sorrys","Sorrying","Sorryed","bother","Sorry","so","you","late","to"],"practiceQuestionTranslation":"이 시간에 왜 전화했어요?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/30/expressions/120/practice-examples/1db0391d-57e8-416a-b460-5cc5a5951041.png","sentenceText":"Sorry to bother you again — one more question.","sentenceWords":["Sorry","to","bother","you","again","one","more","question"],"highlightingPart":"Sorry to bother","practiceQuestion":"Do you need anything else before I go?","sentenceTranslation":"또 귀찮게 해서 미안한데, 하나만 더 물어볼게요.","sentenceWordChoices":["bother","Sorry","you","one","to","Sorrys","Sorryed","more","again","question","Sorrying"],"practiceQuestionTranslation":"제가 가기 전에 더 필요한 게 있으세요?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/30/expressions/120/practice-examples/91e16780-b822-441d-87be-e832ad0616c1.png","sentenceText":"I hate to bother you on your day off.","sentenceWords":["I","hate","to","bother","you","on","your","day","off"],"highlightingPart":"bother","practiceQuestion":"Why are you calling on my day off?","sentenceTranslation":"쉬는 날 귀찮게 해서 미안해요.","sentenceWordChoices":["he","hates","on","bother","your","day","hate","we","you","to","off","I"],"practiceQuestionTranslation":"쉬는 날에 왜 전화했어요?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        121,
        30,
        'SCENARIO',
        'OPINION_JUDGMENT',
        'BASIC',
        'EN',
        'KR',
        2,
        'If I''m not mistaken',
        '내 기억이 맞다면',
        '기억을 열어두는 If I''m not mistaken',
        '''내 기억이 맞다면''이라며 확신이 덜한 정보를 조심스럽게 꺼내는 표현입니다. as far as I know가 지식의 한계를 밝힌다면, 이건 기억의 한계를 열어두는 안전장치예요.',
        'Have we met somewhere before?',
        '우리 어디서 본 적 있지 않아요?',
        'If I''m not mistaken, we met at Sarah''s party.',
        '제 기억이 맞다면, 사라네 파티에서 뵀어요.',
        ARRAY['If', 'I''m', 'not', 'mistaken', 'we', 'met', 'at', 'Sarah''s', 'party'],
        ARRAY['I''m', 'met', 'at', 'we', 'mistakening', 'not', 'mistakens', 'Sarah''s', 'party', 'mistakened', 'If', 'mistaken'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/30/expressions/121/practice-examples/e4ec29e1-2384-4ed7-81ef-0e65b9f29ef6.png","sentenceText":"If I''m not mistaken, the store closes at nine.","sentenceWords":["If","I''m","not","mistaken","the","store","closes","at","nine"],"highlightingPart":"If I''m not mistaken","practiceQuestion":"What time does the store close?","sentenceTranslation":"내 기억이 맞으면 그 가게 9시에 닫아.","sentenceWordChoices":["mistakens","store","nine","I''m","mistaken","the","not","mistakened","closes","If","mistakening","at"],"practiceQuestionTranslation":"그 가게 몇 시에 닫아?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/30/expressions/121/practice-examples/13c08dea-8048-4772-b0ac-edf65f03e7e3.png","sentenceText":"If I''m not mistaken, this is her third album.","sentenceWords":["If","I''m","not","mistaken","this","is","her","third","album"],"highlightingPart":"If I''m not mistaken","practiceQuestion":"How many albums has she released?","sentenceTranslation":"내가 알기론 이게 걔 세 번째 앨범이야.","sentenceWordChoices":["mistakens","mistakening","mistakened","her","I''m","this","is","not","album","third","mistaken","If"],"practiceQuestionTranslation":"걔 앨범을 몇 장 냈어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/30/expressions/121/practice-examples/7ce45749-f952-4315-a406-d84cd7a59ca7.png","sentenceText":"She''s from Busan, if I''m not mistaken.","sentenceWords":["She''s","from","Busan","if","I''m","not","mistaken"],"highlightingPart":"if I''m not mistaken","practiceQuestion":"Where is she from?","sentenceTranslation":"걔 부산 출신일걸, 내 기억이 맞다면.","sentenceWordChoices":["mistaken","Busan","not","for","to","if","from","She''s","I''m","at"],"practiceQuestionTranslation":"걔 어디 출신이야?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/30/expressions/121/practice-examples/6b3289e8-91d8-4da6-a722-06664943d05f.png","sentenceText":"Unless I''m mistaken, we''ve already paid.","sentenceWords":["Unless","I''m","mistaken","we''ve","already","paid"],"highlightingPart":"Unless I''m mistaken","practiceQuestion":"Do we still need to pay?","sentenceTranslation":"내가 잘못 안 게 아니라면 우리 이미 계산했어.","sentenceWordChoices":["I''m","Unless","mistaken","Unlessed","already","Unlesss","paid","Unlessing","we''ve"],"practiceQuestionTranslation":"우리 아직 계산해야 해?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        122,
        30,
        'SCENARIO',
        'CONVERSATION_SKILL',
        'BASIC',
        'EN',
        'KR',
        3,
        'Are you sure ~?',
        '정말 ~인 거 맞아?',
        '의사를 재확인하는 Are you sure ~?',
        '''정말 ~인 거 맞아?''라며 상대의 의사를 한 번 더 확인하는 표현입니다. 거절이나 결정을 들었을 때 진심인지 배려 차원에서 되묻는 용도로 많이 써요.',
        'I can manage this on my own.',
        '이거 나 혼자 할 수 있어.',
        'Are you sure you don''t need help?',
        '정말 도움 필요 없는 거 맞아?',
        ARRAY['Are', 'you', 'sure', 'you', 'don''t', 'need', 'help'],
        ARRAY['you', 'sure', 'don''t', 'is', 'be', 'you', 'were', 'help', 'need', 'Are'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/30/expressions/122/practice-examples/d63d2947-ff73-494b-8827-3f4e3913bdd8.png","sentenceText":"Are you sure about this?","sentenceWords":["Are","you","sure","about","this"],"highlightingPart":"Are you sure","practiceQuestion":"I''ve decided to do this.","sentenceTranslation":"이거 확실해?","sentenceWordChoices":["this","were","you","about","sure","is","be","Are"],"practiceQuestionTranslation":"나 이걸 하기로 결정했어."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/30/expressions/122/practice-examples/a40d26b2-90b9-45e3-b923-963a1bf781d4.png","sentenceText":"Are you sure you don''t want to come?","sentenceWords":["Are","you","sure","you","don''t","want","to","come"],"highlightingPart":"Are you sure","practiceQuestion":"I don''t want to go with you.","sentenceTranslation":"정말 안 올 거야?","sentenceWordChoices":["you","you","is","want","were","sure","don''t","Are","be","to","come"],"practiceQuestionTranslation":"나 같이 가고 싶지 않아."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/30/expressions/122/practice-examples/3d634092-5015-4131-aaac-29fca0fa11fc.png","sentenceText":"Are you sure it''s OK to park here?","sentenceWords":["Are","you","sure","it''s","OK","to","park","here"],"highlightingPart":"Are you sure","practiceQuestion":"Let''s leave the car here.","sentenceTranslation":"여기 주차해도 진짜 괜찮은 거야?","sentenceWordChoices":["you","to","were","be","Are","OK","here","it''s","sure","is","park"],"practiceQuestionTranslation":"차를 여기에 세우자."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/30/expressions/122/practice-examples/b9d5bd1b-ec48-4114-9b3a-717c7828cd07.png","sentenceText":"Are you sure you locked the door?","sentenceWords":["Are","you","sure","you","locked","the","door"],"highlightingPart":"Are you sure","practiceQuestion":"I think I locked the door.","sentenceTranslation":"문 잠근 거 확실해?","sentenceWordChoices":["you","were","Are","be","locked","you","the","is","sure","door"],"practiceQuestionTranslation":"문을 잠근 것 같아."}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        123,
        30,
        'SCENARIO',
        'OPINION_JUDGMENT',
        'BASIC',
        'EN',
        'KR',
        4,
        'I''m not sure',
        '잘 모르겠어, 확실하지 않아',
        '확신이 없을 때 I''m not sure',
        '''잘 모르겠어'', ''확실하지 않아''라며 불확실함을 솔직하게 표현하는 기본 문형입니다. 뒤에 if/whether절이나 의문사절을 붙여 무엇이 불확실한지 구체적으로 말할 수 있어요.',
        'Is he coming tonight?',
        '걔 오늘 밤에 와?',
        'I''m not sure if he''s coming.',
        '걔가 올지 잘 모르겠어.',
        ARRAY['I''m', 'not', 'sure', 'if', 'he''s', 'coming'],
        ARRAY['coming', 'sureing', 'not', 'sure', 'if', 'sures', 'I''m', 'he''s', 'sureed'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/30/expressions/123/practice-examples/bfa99738-494c-4d9d-bbb6-c506d229f87b.png","sentenceText":"I''m not sure what to wear.","sentenceWords":["I''m","not","sure","what","to","wear"],"highlightingPart":"I''m not sure","practiceQuestion":"What are you going to wear?","sentenceTranslation":"뭘 입어야 할지 모르겠어.","sentenceWordChoices":["sure","not","sureed","wear","to","I''m","sureing","sures","what"],"practiceQuestionTranslation":"뭘 입을 거야?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/30/expressions/123/practice-examples/537abd69-972d-4285-9f39-b3825eccb2e1.png","sentenceText":"I''m not sure if this is the right size.","sentenceWords":["I''m","not","sure","if","this","is","the","right","size"],"highlightingPart":"I''m not sure","practiceQuestion":"Does that size fit you?","sentenceTranslation":"이게 맞는 사이즈인지 잘 모르겠어요.","sentenceWordChoices":["sure","I''m","sures","sureing","sureed","the","not","if","size","right","this","is"],"practiceQuestionTranslation":"그 사이즈가 맞아?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/30/expressions/123/practice-examples/2906346f-179f-484f-ba9e-6a362b866dfd.png","sentenceText":"I''m not sure how to explain it.","sentenceWords":["I''m","not","sure","how","to","explain","it"],"highlightingPart":"I''m not sure","practiceQuestion":"Can you explain what happened?","sentenceTranslation":"그걸 어떻게 설명해야 할지 모르겠네.","sentenceWordChoices":["I''m","to","sure","explain","sureing","sureed","not","sures","it","how"],"practiceQuestionTranslation":"무슨 일이 있었는지 설명해 줄래?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/30/expressions/123/practice-examples/724297f7-a584-4ba9-bfff-32cc0f62c514.png","sentenceText":"To be honest, I''m not sure yet.","sentenceWords":["To","be","honest","I''m","not","sure","yet"],"highlightingPart":"I''m not sure","practiceQuestion":"Have you decided yet?","sentenceTranslation":"솔직히 아직 잘 모르겠어.","sentenceWordChoices":["not","yet","I''m","for","be","To","from","sure","at","honest"],"practiceQuestionTranslation":"이제 결정했어?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        124,
        31,
        'SCENARIO',
        'WORK_STUDY',
        'BASIC',
        'EN',
        'KR',
        1,
        'keep up with',
        '~을 따라가다, 뒤처지지 않다',
        '따라가기 벅찰 때 keep up with',
        '''~을 따라가다, ~에 뒤처지지 않다''라는 뜻입니다. 일정, 유행, 사람의 속도 등 빠르게 움직이는 대상을 쫓아가는 상황에 두루 쓰여요.',
        'Isn''t technology moving so fast?',
        '기술 진짜 빠르게 바뀌지 않아?',
        'I can''t keep up with all the changes.',
        '그 많은 변화를 다 따라갈 수가 없어.',
        ARRAY['I', 'can''t', 'keep', 'up', 'with', 'all', 'the', 'changes'],
        ARRAY['keep', 'with', 'up', 'he', 'changes', 'we', 'all', 'can''t', 'I', 'the', 'you'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/31/expressions/124/practice-examples/399595fa-b4e5-4148-84d7-a6fa94988515.png","sentenceText":"Slow down! I can''t keep up with you.","sentenceWords":["Slow","down","I","can''t","keep","up","with","you"],"highlightingPart":"keep up with","practiceQuestion":"Why are you walking so fast?","sentenceTranslation":"천천히 가! 너 못 따라가겠어.","sentenceWordChoices":["Slow","I","with","up","Slowing","you","Slows","Slowed","keep","down","can''t"],"practiceQuestionTranslation":"왜 그렇게 빨리 걸어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/31/expressions/124/practice-examples/d94c0677-fcc7-4470-b4de-c8a18ef0e0da.png","sentenceText":"It''s hard to keep up with the latest trends.","sentenceWords":["It''s","hard","to","keep","up","with","the","latest","trends"],"highlightingPart":"keep up with","practiceQuestion":"Why don''t you follow every new trend?","sentenceTranslation":"최신 유행을 따라가기가 힘들어.","sentenceWordChoices":["hards","with","keep","hard","up","It''s","to","harding","harded","latest","the","trends"],"practiceQuestionTranslation":"왜 새로운 유행을 전부 따라가지 않아?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/31/expressions/124/practice-examples/e4aba370-b5a9-4102-99c7-ca1a384fa845.png","sentenceText":"She keeps up with the news every morning.","sentenceWords":["She","keeps","up","with","the","news","every","morning"],"highlightingPart":"keeps up with","practiceQuestion":"How does she stay informed?","sentenceTranslation":"걔는 매일 아침 뉴스를 챙겨 봐.","sentenceWordChoices":["keeped","keeping","keep","news","every","the","morning","She","up","with","keeps"],"practiceQuestionTranslation":"걔는 어떻게 계속 새 소식을 알아?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/31/expressions/124/practice-examples/5d2b6fd9-2394-4d73-bea6-80ee6c50dedc.png","sentenceText":"I''m struggling to keep up with my classes.","sentenceWords":["I''m","struggling","to","keep","up","with","my","classes"],"highlightingPart":"keep up with","practiceQuestion":"How are your classes going?","sentenceTranslation":"수업 진도를 따라가는 게 벅차.","sentenceWordChoices":["struggled","up","classes","my","with","to","I''m","struggl","keep","struggling","struggls"],"practiceQuestionTranslation":"수업은 잘 따라가고 있어?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        125,
        31,
        'SCENARIO',
        'OPINION_JUDGMENT',
        'BASIC',
        'EN',
        'KR',
        2,
        'be leaning toward',
        '~쪽으로 마음이 기울다',
        '마음이 기우는 be leaning toward',
        '아직 확정은 아니지만 ''~쪽으로 마음이 기울고 있다''는 뜻입니다. 결정 전 단계의 미묘한 상태를 정확히 전달할 수 있어서 진로, 쇼핑, 계획 얘기에 딱이에요.',
        'Have you decided between the two offers?',
        '두 제안 중에 결정했어?',
        'I''m leaning toward the startup.',
        '스타트업 쪽으로 기울고 있어.',
        ARRAY['I''m', 'leaning', 'toward', 'the', 'startup'],
        ARRAY['leans', 'startup', 'leaning', 'leaned', 'the', 'toward', 'I''m', 'lean'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/31/expressions/125/practice-examples/7f4435f9-ba54-4e77-abff-6e2ff9c19423.png","sentenceText":"I''m leaning toward staying home this weekend.","sentenceWords":["I''m","leaning","toward","staying","home","this","weekend"],"highlightingPart":"leaning toward","practiceQuestion":"Are you going out this weekend?","sentenceTranslation":"이번 주말에는 집에 있는 쪽으로 마음이 기울고 있어.","sentenceWordChoices":["home","toward","lean","this","leaning","leaned","leans","I''m","staying","weekend"],"practiceQuestionTranslation":"이번 주말에 나갈 거야?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/31/expressions/125/practice-examples/7d3b80d7-d856-4894-b427-fe56f7512081.png","sentenceText":"She''s leaning toward the design major.","sentenceWords":["She''s","leaning","toward","the","design","major"],"highlightingPart":"leaning toward","practiceQuestion":"Which major is she thinking about?","sentenceTranslation":"걔는 디자인 전공 쪽으로 기울었어.","sentenceWordChoices":["design","lean","toward","leaning","major","the","She''s","leaned","leans"],"practiceQuestionTranslation":"걔는 어느 전공을 고민하고 있어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/31/expressions/125/practice-examples/9ba7f177-b02f-4959-8933-0622562deb43.png","sentenceText":"We''re leaning toward the cheaper option.","sentenceWords":["We''re","leaning","toward","the","cheaper","option"],"highlightingPart":"leaning toward","practiceQuestion":"Which option are you likely to choose?","sentenceTranslation":"우린 더 싼 옵션 쪽으로 마음이 기울고 있어.","sentenceWordChoices":["We''re","cheaper","option","leans","lean","leaning","leaned","the","toward"],"practiceQuestionTranslation":"어느 선택지를 고를 것 같아?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/31/expressions/125/practice-examples/658535f5-e784-494e-99db-c9b83202fb1f.png","sentenceText":"Which way are you leaning?","sentenceWords":["Which","way","are","you","leaning"],"highlightingPart":"way are you leaning","practiceQuestion":"I still haven''t chosen between the two options.","sentenceTranslation":"넌 어느 쪽으로 기울어?","sentenceWordChoices":["you","are","Which","way","Whiching","Whiched","leaning","Whichs"],"practiceQuestionTranslation":"아직 두 선택지 중 하나를 못 골랐어."}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        126,
        31,
        'SCENARIO',
        'TIME_PLANNING',
        'BASIC',
        'EN',
        'KR',
        3,
        'plan on ~ing',
        '~할 계획이다',
        '계획을 말하는 plan on ~ing',
        '''~할 계획이다''를 뜻하는 구동사로, plan to와 함께 회화에서 정말 많이 쓰입니다. on 뒤에 동명사가 온다는 점만 기억하면 돼요.',
        'Any plans this weekend?',
        '이번 주말에 뭐 해?',
        'I''m planning on staying home this weekend.',
        '이번 주말엔 집에 있을 계획이야.',
        ARRAY['I''m', 'planning', 'on', 'staying', 'home', 'this', 'weekend'],
        ARRAY['weekend', 'planning', 'on', 'plann', 'staying', 'planned', 'this', 'I''m', 'home', 'planns'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/31/expressions/126/practice-examples/450b4a1a-22b4-4ab0-9780-85828890c9eb.png","sentenceText":"Are you planning on coming to the party?","sentenceWords":["Are","you","planning","on","coming","to","the","party"],"highlightingPart":"planning on","practiceQuestion":"I heard you were invited to the party.","sentenceTranslation":"파티에 올 계획이야?","sentenceWordChoices":["the","were","to","you","Are","planning","party","on","is","be","coming"],"practiceQuestionTranslation":"너 그 파티에 초대받았다며."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/31/expressions/126/practice-examples/3ca35ddb-1970-4fd7-95db-34e24b4bd1b6.png","sentenceText":"We''re planning on eating out tonight.","sentenceWords":["We''re","planning","on","eating","out","tonight"],"highlightingPart":"planning on","practiceQuestion":"What are we doing for dinner?","sentenceTranslation":"오늘 밤엔 외식할 계획이야.","sentenceWordChoices":["out","on","eating","plann","We''re","planns","tonight","planned","planning"],"practiceQuestionTranslation":"저녁은 어떻게 할 거야?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/31/expressions/126/practice-examples/2a0c5ee9-453d-40fe-8d93-2c4dfb3e16b4.png","sentenceText":"I wasn''t planning on buying anything.","sentenceWords":["I","wasn''t","planning","on","buying","anything"],"highlightingPart":"planning on","practiceQuestion":"Did you come here to buy something?","sentenceTranslation":"뭘 살 생각은 없었는데.","sentenceWordChoices":["anything","wasn''t","on","buying","I","planning","you","we","he"],"practiceQuestionTranslation":"뭔가 사려고 여기 온 거야?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/31/expressions/126/practice-examples/e639b135-010f-4329-b863-d414d7fefccd.png","sentenceText":"He plans on retiring early.","sentenceWords":["He","plans","on","retiring","early"],"highlightingPart":"plans on","practiceQuestion":"What are his plans for retirement?","sentenceTranslation":"걔는 조기 은퇴할 계획이야.","sentenceWordChoices":["retiring","planing","He","plans","on","plan","planed","early"],"practiceQuestionTranslation":"걔는 은퇴를 어떻게 계획하고 있어?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        127,
        31,
        'SCENARIO',
        'WORK_STUDY',
        'BASIC',
        'EN',
        'KR',
        4,
        'put in',
        '(시간·노력을) 들이다',
        '노력을 들이는 put in',
        '''(시간·노력을) 들이다, 투입하다''라는 뜻입니다. put in effort, put in hours처럼 무언가를 이루기 위해 쏟아붓는 것을 표현해요.',
        'Why is she so good at her job?',
        '걔는 왜 이렇게 일을 잘해?',
        'She puts in a lot of effort at work.',
        '걔는 일에 정말 많은 노력을 쏟아.',
        ARRAY['She', 'puts', 'in', 'a', 'lot', 'of', 'effort', 'at', 'work'],
        ARRAY['work', 'at', 'puts', 'of', 'puting', 'She', 'effort', 'lot', 'put', 'puted', 'a', 'in'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/31/expressions/127/practice-examples/2dc01463-631e-4d66-943c-aee2f9366d79.png","sentenceText":"I put in extra hours this week.","sentenceWords":["I","put","in","extra","hours","this","week"],"highlightingPart":"put in","practiceQuestion":"Why are you so tired this week?","sentenceTranslation":"이번 주에 추가 근무를 했어.","sentenceWordChoices":["I","extra","you","this","we","week","in","put","he","hours"],"practiceQuestionTranslation":"이번 주에 왜 그렇게 피곤해?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/31/expressions/127/practice-examples/b9b9b2cb-aa4a-4135-afd4-f66579844619.png","sentenceText":"You get out what you put in.","sentenceWords":["You","get","out","what","you","put","in"],"highlightingPart":"put in","practiceQuestion":"What determines how much you get back?","sentenceTranslation":"들인 만큼 얻는 거야.","sentenceWordChoices":["we","out","put","get","I","they","You","what","in","you"],"practiceQuestionTranslation":"무엇에 따라 얻는 결과가 달라져?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/31/expressions/127/practice-examples/fb01d85e-0011-4ee8-8f5b-65c4073c00ef.png","sentenceText":"He put in years of practice to get here.","sentenceWords":["He","put","in","years","of","practice","to","get","here"],"highlightingPart":"put in","practiceQuestion":"How did he become so skilled?","sentenceTranslation":"걔는 여기까지 오려고 수년간 연습했어.","sentenceWordChoices":["at","here","get","in","put","to","on","into","He","years","practice","of"],"practiceQuestionTranslation":"걔는 어떻게 그렇게 실력이 좋아졌어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/31/expressions/127/practice-examples/6b46a2a9-2242-4c2d-960f-f1f623282df5.png","sentenceText":"We need to put in more time on this project.","sentenceWords":["We","need","to","put","in","more","time","on","this","project"],"highlightingPart":"put in","practiceQuestion":"How can we improve this project?","sentenceTranslation":"이 프로젝트에 시간을 더 들여야 해.","sentenceWordChoices":["they","you","put","project","time","in","this","need","I","more","to","We","on"],"practiceQuestionTranslation":"이 프로젝트를 어떻게 더 좋게 만들 수 있을까?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        128,
        32,
        'SCENARIO',
        'DAILY_ROUTINE',
        'BASIC',
        'EN',
        'KR',
        1,
        'don''t feel (quite) right',
        '몸이 좀 이상하다',
        '몸 상태를 표현하는 don''t feel (quite) right',
        '어디가 아프다고 콕 집어 말하기 애매하지만 컨디션이 안 좋을 때 쓰는 표현입니다. ''몸이 좀 이상해'', ''컨디션이 별로야'' 정도의 뉘앙스예요.',
        'You look a little pale.',
        '너 안색이 좀 창백해.',
        'I don''t feel right this morning.',
        '오늘 아침 컨디션이 좀 이상해.',
        ARRAY['I', 'don''t', 'feel', 'right', 'this', 'morning'],
        ARRAY['don''t', 'morning', 'he', 'I', 'we', 'feel', 'this', 'right', 'you'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/32/expressions/128/practice-examples/1caad99f-5e21-460b-ac01-197f9e3aa43a.png","sentenceText":"My stomach doesn''t feel right.","sentenceWords":["My","stomach","doesn''t","feel","right"],"highlightingPart":"doesn''t feel right","practiceQuestion":"How is your stomach?","sentenceTranslation":"속이 좀 안 좋아.","sentenceWordChoices":["doesn''t","My","our","your","feel","stomach","right","his"],"practiceQuestionTranslation":"속은 좀 어때?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/32/expressions/128/practice-examples/c10c72c0-adae-49d7-876d-ee6f4e42f717.png","sentenceText":"I haven''t felt right since yesterday.","sentenceWords":["I","haven''t","felt","right","since","yesterday"],"highlightingPart":"haven''t felt right","practiceQuestion":"How have you been feeling since yesterday?","sentenceTranslation":"어제부터 몸이 좀 이상해.","sentenceWordChoices":["right","I","we","you","since","he","yesterday","haven''t","felt"],"practiceQuestionTranslation":"어제부터 몸 상태가 어때?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/32/expressions/128/practice-examples/77db3bf4-4d21-4256-a2f5-4fb8111776ce.png","sentenceText":"I don''t feel quite right today.","sentenceWords":["I","don''t","feel","quite","right","today"],"highlightingPart":"don''t feel quite right","practiceQuestion":"You don''t look well today. What''s wrong?","sentenceTranslation":"나 오늘 몸이 좀 안 좋은 것 같아.","sentenceWordChoices":["today","quite","doesn''t","right","I","feel","felt","don''t","feeling"],"practiceQuestionTranslation":"오늘 안 좋아 보이는데. 무슨 일이야?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/32/expressions/128/practice-examples/7b76348d-9021-4f6e-ab4d-fbba44859653.png","sentenceText":"If you don''t feel right, you should go home early.","sentenceWords":["If","you","don''t","feel","right","you","should","go","home","early"],"highlightingPart":"don''t feel right","practiceQuestion":"Should I stay at work even though I feel sick?","sentenceTranslation":"몸이 안 좋으면 일찍 들어가.","sentenceWordChoices":["I","should","feel","don''t","they","right","home","go","we","you","you","If","early"],"practiceQuestionTranslation":"몸이 안 좋은데도 회사에 있어야 할까?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        129,
        32,
        'SCENARIO',
        'RELATIONSHIP_SOCIAL',
        'BASIC',
        'EN',
        'KR',
        2,
        'catch up',
        '밀린 근황을 나누다, 따라잡다',
        '밀린 얘기를 나누는 catch up',
        '''밀린 근황을 나누다'', ''따라잡다''라는 뜻의 구동사입니다. 오랜만에 만난 사람과 Let''s catch up!이라고 하면 ''우리 회포 좀 풀자''는 의미가 돼요.',
        'Want to meet up soon?',
        '조만간 만날래?',
        'We have so much to catch up on.',
        '우리 밀린 얘기가 산더미야.',
        ARRAY['We', 'have', 'so', 'much', 'to', 'catch', 'up', 'on'],
        ARRAY['I', 'much', 'they', 'We', 'catch', 'on', 'have', 'to', 'up', 'so', 'you'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/32/expressions/129/practice-examples/8e06d39f-6aa8-43fb-8228-c67e0743234e.png","sentenceText":"It was great catching up with you.","sentenceWords":["It","was","great","catching","up","with","you"],"highlightingPart":"catching up","practiceQuestion":"How was coffee together after all this time?","sentenceTranslation":"오랜만에 얘기 나눠서 정말 좋았어.","sentenceWordChoices":["It","great","catching","been","were","with","you","up","was","is"],"practiceQuestionTranslation":"오랜만에 같이 커피 마시니까 어땠어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/32/expressions/129/practice-examples/1f05b1e7-1195-4445-8e84-856a93f5cfec.png","sentenceText":"Let''s grab coffee and catch up soon.","sentenceWords":["Let''s","grab","coffee","and","catch","up","soon"],"highlightingPart":"catch up","practiceQuestion":"We haven''t talked in ages.","sentenceTranslation":"조만간 커피 마시면서 회포 풀자.","sentenceWordChoices":["grabs","Let''s","coffee","grab","grabed","catch","and","soon","up","grabing"],"practiceQuestionTranslation":"우리 정말 오랫동안 얘기 못 했네."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/32/expressions/129/practice-examples/f2087efe-9183-4ea0-a09c-19740db5a107.png","sentenceText":"I need to catch up on the news.","sentenceWords":["I","need","to","catch","up","on","the","news"],"highlightingPart":"catch up","practiceQuestion":"Have you read the latest news?","sentenceTranslation":"밀린 뉴스 좀 챙겨봐야겠어.","sentenceWordChoices":["he","to","on","I","news","up","catch","you","need","we","the"],"practiceQuestionTranslation":"최근 뉴스 읽어 봤어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/32/expressions/129/practice-examples/f1e3422d-f824-4575-bc21-9465a51522c4.png","sentenceText":"I stayed home to catch up on sleep.","sentenceWords":["I","stayed","home","to","catch","up","on","sleep"],"highlightingPart":"catch up","practiceQuestion":"Why did you stay home?","sentenceTranslation":"밀린 잠을 자려고 집에 있었어.","sentenceWordChoices":["you","catch","stayed","I","up","he","sleep","we","home","to","on"],"practiceQuestionTranslation":"왜 집에 있었어?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        130,
        32,
        'SCENARIO',
        'TRAVEL_MOVEMENT',
        'BASIC',
        'EN',
        'KR',
        3,
        'swing by',
        '잠깐 들르다',
        '잠깐 들르는 swing by',
        '''잠깐 들르다''라는 뜻의 캐주얼한 구동사입니다. 오래 머무는 게 아니라 스치듯 가볍게 들른다는 뉘앙스가 있어서 stop by와 거의 같은 의미로 쓰여요.',
        'Can you come over later?',
        '이따 들를 수 있어?',
        'I''ll swing by after work.',
        '퇴근하고 잠깐 들를게.',
        ARRAY['I''ll', 'swing', 'by', 'after', 'work'],
        ARRAY['swing', 'swings', 'work', 'by', 'I''ll', 'swinged', 'swinging', 'after'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/32/expressions/130/practice-examples/1efe1b9b-5b0f-42c1-90c8-ac4fba20a207.png","sentenceText":"Can you swing by the store on your way?","sentenceWords":["Can","you","swing","by","the","store","on","your","way"],"highlightingPart":"swing by","practiceQuestion":"We''re out of milk.","sentenceTranslation":"오는 길에 가게 좀 들러줄래?","sentenceWordChoices":["on","store","could","would","the","swing","way","you","your","Can","by","should"],"practiceQuestionTranslation":"우유가 다 떨어졌어."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/32/expressions/130/practice-examples/1282e1bf-728c-4d79-9c57-69555b51ca4c.png","sentenceText":"Swing by anytime!","sentenceWords":["Swing","by","anytime"],"highlightingPart":"Swing by","practiceQuestion":"When can I visit?","sentenceTranslation":"언제든 편하게 들러!","sentenceWordChoices":["Swinging","Swing","by","Swings","Swinged","anytime"],"practiceQuestionTranslation":"언제 들러도 돼?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/32/expressions/130/practice-examples/76a988cb-09b5-432d-9453-bf0ec4570d81.png","sentenceText":"I swung by your office, but you weren''t there.","sentenceWords":["I","swung","by","your","office","but","you","weren''t","there"],"highlightingPart":"swung by","practiceQuestion":"Did you come to my office yesterday?","sentenceTranslation":"네 사무실 들렀는데 없더라.","sentenceWordChoices":["we","office","you","there","he","swung","swungs","your","by","I","but","weren''t"],"practiceQuestionTranslation":"어제 내 사무실에 왔었어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/32/expressions/130/practice-examples/ad676c12-6422-4ce7-a4be-0818cc9e8166.png","sentenceText":"Let me swing by the ATM first.","sentenceWords":["Let","me","swing","by","the","ATM","first"],"highlightingPart":"swing by","practiceQuestion":"Can we go straight to the restaurant?","sentenceTranslation":"먼저 ATM 좀 잠깐 들를게.","sentenceWordChoices":["Let","ATM","swing","swings","the","by","swinging","me","first","swinged"],"practiceQuestionTranslation":"식당으로 바로 가면 될까?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        131,
        32,
        'SCENARIO',
        'CONVERSATION_SKILL',
        'BASIC',
        'EN',
        'KR',
        4,
        'I really appreciate it',
        '정말 감사해요',
        '진심을 담아 감사하는 I really appreciate it',
        'thank you보다 한 단계 진심이 담긴 감사 표현입니다. appreciate 뒤에는 사람이 아니라 행동이나 것(it, your help)이 온다는 게 한국인이 가장 많이 틀리는 포인트예요.',
        'I fixed your laptop while you were out.',
        '너 나간 사이에 노트북 고쳐놨어.',
        'You''re a lifesaver — I really appreciate it.',
        '진짜 은인이다. 정말 고마워.',
        ARRAY['You''re', 'a', 'lifesaver', 'I', 'really', 'appreciate', 'it'],
        ARRAY['the', 'some', 'You''re', 'a', 'an', 'I', 'really', 'it', 'appreciate', 'lifesaver'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/32/expressions/131/practice-examples/3118cd5d-3112-4123-946f-2c7295b63f69.png","sentenceText":"I really appreciate your help.","sentenceWords":["I","really","appreciate","your","help"],"highlightingPart":"really appreciate","practiceQuestion":"Was my help useful?","sentenceTranslation":"도와주셔서 정말 감사해요.","sentenceWordChoices":["really","we","he","help","you","appreciate","I","your"],"practiceQuestionTranslation":"내 도움이 도움이 됐어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/32/expressions/131/practice-examples/dc9b2402-5a22-4649-bcbb-e9516c6eb26e.png","sentenceText":"I appreciate the offer, but I''m okay.","sentenceWords":["I","appreciate","the","offer","but","I''m","okay"],"highlightingPart":"appreciate","practiceQuestion":"Would you like me to do that for you?","sentenceTranslation":"제안은 감사하지만 전 괜찮아요.","sentenceWordChoices":["okay","appreciate","the","you","we","he","I''m","but","I","offer"],"practiceQuestionTranslation":"내가 그걸 해줄까?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/32/expressions/131/practice-examples/00b5d113-f148-44dd-a334-ea1910e59e3b.png","sentenceText":"We appreciate your patience.","sentenceWords":["We","appreciate","your","patience"],"highlightingPart":"appreciate","practiceQuestion":"Sorry for making you wait.","sentenceTranslation":"기다려 주셔서 감사합니다.","sentenceWordChoices":["appreciate","you","I","patience","We","your","they"],"practiceQuestionTranslation":"기다리게 해서 죄송해요."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/32/expressions/131/practice-examples/9d62d786-d023-42bb-bd25-eec481a52af0.png","sentenceText":"I appreciate you taking the time.","sentenceWords":["I","appreciate","you","taking","the","time"],"highlightingPart":"appreciate","practiceQuestion":"Was the meeting worth your time?","sentenceTranslation":"시간 내주셔서 감사해요.","sentenceWordChoices":["the","time","appreciates","taking","appreciate","we","he","you","I"],"practiceQuestionTranslation":"시간 내서 만난 보람이 있었어?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        132,
        33,
        'SCENARIO',
        'CONVERSATION_SKILL',
        'BASIC',
        'EN',
        'KR',
        1,
        'make sense',
        '말이 되다, 이해가 되다',
        '이해가 안 될 때 make sense',
        '''말이 되다'', ''이해가 되다''라는 뜻으로, 논리적으로 납득되는지를 표현합니다. That makes sense(그렇구나, 말 되네)는 대화 필수 리액션이에요.',
        'That''s why I chose the blue one.',
        '그래서 내가 파란 걸 골랐어.',
        'That makes sense.',
        '그렇구나, 이해돼.',
        ARRAY['That', 'makes', 'sense'],
        ARRAY['these', 'those', 'sense', 'That', 'makes', 'this'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/33/expressions/132/practice-examples/5c86b2f7-b9b3-4a8c-b54b-ecb69e27c2eb.png","sentenceText":"It doesn''t make sense to pay that much.","sentenceWords":["It","doesn''t","make","sense","to","pay","that","much"],"highlightingPart":"make sense","practiceQuestion":"Would you really pay that much?","sentenceTranslation":"그렇게 많이 내는 건 말이 안 돼.","sentenceWordChoices":["that","make","to","makes","makeing","pay","much","sense","It","makeed","doesn''t"],"practiceQuestionTranslation":"정말 그만큼 돈을 낼 거야?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/33/expressions/132/practice-examples/6a79969b-1060-4564-bc9e-99d050ff704a.png","sentenceText":"Does that make sense?","sentenceWords":["Does","that","make","sense"],"highlightingPart":"make sense","practiceQuestion":"I think I follow what you''re saying.","sentenceTranslation":"제 말이 이해되시나요?","sentenceWordChoices":["sense","that","do","doing","did","Does","make"],"practiceQuestionTranslation":"무슨 말인지 알 것 같아요."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/33/expressions/132/practice-examples/645136bf-787e-4768-a7cf-d2b1198884b7.png","sentenceText":"Her explanation didn''t make sense to me.","sentenceWords":["Her","explanation","didn''t","make","sense","to","me"],"highlightingPart":"make sense","practiceQuestion":"Did you understand what she meant?","sentenceTranslation":"걔 설명이 난 이해가 안 됐어.","sentenceWordChoices":["explanation","make","Her","sense","explanationed","explanationing","to","me","didn''t","explanations"],"practiceQuestionTranslation":"걔가 무슨 말인지 이해했어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/33/expressions/132/practice-examples/dc4f2c02-bb46-4466-895e-451f40eb8d24.png","sentenceText":"Now it all makes sense!","sentenceWords":["Now","it","all","makes","sense"],"highlightingPart":"makes sense","practiceQuestion":"Do you understand it now?","sentenceTranslation":"이제야 전부 이해가 되네!","sentenceWordChoices":["makes","it","sense","all","then","soon","Now","later"],"practiceQuestionTranslation":"이제 이해가 돼?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        133,
        33,
        'SCENARIO',
        'CONVERSATION_SKILL',
        'BASIC',
        'EN',
        'KR',
        2,
        'in a nutshell',
        '요컨대, 한마디로',
        '한마디로 요약하면 in a nutshell',
        '''견과류 껍데기 안에 들어갈 만큼 짧게'', 즉 ''요컨대, 한마디로''라는 뜻입니다. 복잡한 내용을 압축해서 전달할 때 문장 앞뒤에 붙여요.',
        'Can you sum up the project?',
        '프로젝트 한마디로 요약해줄래?',
        'In a nutshell, the project was a success.',
        '한마디로, 프로젝트는 성공이었어.',
        ARRAY['In', 'a', 'nutshell', 'the', 'project', 'was', 'a', 'success'],
        ARRAY['success', 'a', 'a', 'In', 'at', 'the', 'on', 'was', 'project', 'nutshell', 'into'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/33/expressions/133/practice-examples/fdb0e5be-e356-4cdc-a6a5-b241a041bba3.png","sentenceText":"That''s the plan in a nutshell.","sentenceWords":["That''s","the","plan","in","a","nutshell"],"highlightingPart":"in a nutshell","practiceQuestion":"Is that the whole plan?","sentenceTranslation":"그게 계획의 요지야.","sentenceWordChoices":["plans","plan","That''s","the","some","a","an","in","nutshell"],"practiceQuestionTranslation":"그게 계획의 전부야?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/33/expressions/133/practice-examples/3fe342a4-c753-4e83-b6b6-0e1781c649f2.png","sentenceText":"In a nutshell, we can''t afford it.","sentenceWords":["In","a","nutshell","we","can''t","afford","it"],"highlightingPart":"In a nutshell","practiceQuestion":"Why can''t we choose the expensive option?","sentenceTranslation":"요약하면, 우린 그럴 형편이 안 돼.","sentenceWordChoices":["nutshell","can''t","at","In","on","we","afford","into","a","it"],"practiceQuestionTranslation":"왜 비싼 선택지는 고를 수 없는 거야?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/33/expressions/133/practice-examples/fc8d3265-76b2-434d-8bae-3e5fc8a7b20c.png","sentenceText":"Can you explain it in a nutshell?","sentenceWords":["Can","you","explain","it","in","a","nutshell"],"highlightingPart":"in a nutshell","practiceQuestion":"I don''t have much time for the explanation.","sentenceTranslation":"핵심만 간단히 설명해 줄래?","sentenceWordChoices":["in","nutshell","could","you","explain","should","a","it","Can","would"],"practiceQuestionTranslation":"설명을 들을 시간이 많지 않아요."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/33/expressions/133/practice-examples/51c52ea8-5f5c-4252-a666-93b2d151ba99.png","sentenceText":"In a nutshell, the movie is about second chances.","sentenceWords":["In","a","nutshell","the","movie","is","about","second","chances"],"highlightingPart":"In a nutshell","practiceQuestion":"What is the movie about?","sentenceTranslation":"한마디로 그 영화는 두 번째 기회에 관한 얘기야.","sentenceWordChoices":["about","a","is","chances","movie","the","second","on","In","at","nutshell","into"],"practiceQuestionTranslation":"그 영화는 무슨 내용이야?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        134,
        33,
        'SCENARIO',
        'CONVERSATION_SKILL',
        'BASIC',
        'EN',
        'KR',
        3,
        'This might be a silly question, but',
        '바보 같은 질문일 수도 있는데',
        '질문 문턱을 낮추는 This might be a silly question, but',
        '질문하기 전에 ''별거 아닌 질문일 수도 있는데''라고 깔아 부담을 낮추는 표현입니다. 새 직장, 새 수업처럼 기본적인 걸 물어야 하는 상황에서 특히 요긴해요.',
        'Any questions so far?',
        '여기까지 질문 있나요?',
        'This might be a silly question, but where do I submit this?',
        '바보 같은 질문일 수도 있는데, 이거 어디에 제출해요?',
        ARRAY['This', 'might', 'be', 'a', 'silly', 'question', 'but', 'where', 'do', 'I', 'submit', 'this'],
        ARRAY['silly', 'a', 'submit', 'I', 'be', 'this', 'This', 'these', 'do', 'those', 'question', 'but', 'that', 'might', 'where'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/33/expressions/134/practice-examples/da5635f5-eb38-41b3-9171-1981097a1c9f.png","sentenceText":"This might be a dumb question, but how do I turn this on?","sentenceWords":["This","might","be","a","dumb","question","but","how","do","I","turn","this","on"],"highlightingPart":"This might be a dumb question","practiceQuestion":"Do you know how to use this device?","sentenceTranslation":"멍청한 질문일 수 있는데, 이거 어떻게 켜?","sentenceWordChoices":["how","question","turn","on","a","do","that","but","those","I","be","might","dumb","This","this","these"],"practiceQuestionTranslation":"이 기기 어떻게 쓰는지 알아?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/33/expressions/134/practice-examples/2650fabf-c5f6-4eea-ac9b-e3d72226a190.png","sentenceText":"Silly question, but do I need a ticket for the kids?","sentenceWords":["Silly","question","but","do","I","need","a","ticket","for","the","kids"],"highlightingPart":"Silly question","practiceQuestion":"I''m buying tickets for my family.","sentenceTranslation":"사소한 질문인데, 애들도 표가 필요한가요?","sentenceWordChoices":["for","kids","ticket","a","question","I","Silly","Sillyed","Sillys","need","Sillying","but","the","do"],"practiceQuestionTranslation":"가족 표를 사고 있어요."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/33/expressions/134/practice-examples/7cb0499c-645a-4955-9e75-54fb3726c941.png","sentenceText":"This might be obvious, but where''s the restroom?","sentenceWords":["This","might","be","obvious","but","where''s","the","restroom"],"highlightingPart":"This might be obvious","practiceQuestion":"Is there anything else you need?","sentenceTranslation":"뻔한 질문일 수 있는데, 화장실이 어디예요?","sentenceWordChoices":["those","might","restroom","obvious","but","be","these","This","that","where''s","the"],"practiceQuestionTranslation":"더 필요한 게 있으세요?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/33/expressions/134/practice-examples/126616ce-6e9c-4d97-a992-f77d52f61918.png","sentenceText":"There are no silly questions — ask away.","sentenceWords":["There","are","no","silly","questions","ask","away"],"highlightingPart":"no silly questions","practiceQuestion":"I''m worried my question sounds stupid.","sentenceTranslation":"바보 같은 질문 같은 건 없어요. 얼마든지 물어보세요.","sentenceWordChoices":["silly","There","ask","are","Theres","questions","Thereed","Thereing","no","away"],"practiceQuestionTranslation":"내 질문이 바보같이 들릴까 봐 걱정돼."}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        135,
        33,
        'SCENARIO',
        'WORK_STUDY',
        'BASIC',
        'EN',
        'KR',
        4,
        'come up with',
        '(아이디어를) 생각해 내다',
        '아이디어를 떠올리는 come up with',
        '''(아이디어·해결책을) 생각해 내다''라는 뜻의 필수 구동사입니다. 없던 것을 머리에서 만들어낸다는 창의의 뉘앙스가 담겨 있어요.',
        'What should we do about this?',
        '이거 어떻게 해야 하지?',
        'We need to come up with a plan.',
        '우리 계획을 짜내야 해.',
        ARRAY['We', 'need', 'to', 'come', 'up', 'with', 'a', 'plan'],
        ARRAY['I', 'they', 'to', 'We', 'come', 'up', 'plan', 'you', 'need', 'a', 'with'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/33/expressions/135/practice-examples/c5d0f9fe-ab06-4661-b620-9795c5381b21.png","sentenceText":"She came up with a brilliant solution.","sentenceWords":["She","came","up","with","a","brilliant","solution"],"highlightingPart":"came up with","practiceQuestion":"How did she solve the problem?","sentenceTranslation":"걔가 기가 막힌 해결책을 생각해 냈어.","sentenceWordChoices":["a","coming","solution","comes","She","brilliant","with","come","came","up"],"practiceQuestionTranslation":"걔는 그 문제를 어떻게 해결했어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/33/expressions/135/practice-examples/3e0bb85d-f412-4b43-89d2-f2e79bb6f2da.png","sentenceText":"How did you come up with that idea?","sentenceWords":["How","did","you","come","up","with","that","idea"],"highlightingPart":"come up with","practiceQuestion":"I designed the whole layout myself.","sentenceTranslation":"그 아이디어 어떻게 떠올린 거야?","sentenceWordChoices":["did","does","How","come","done","with","up","that","you","do","idea"],"practiceQuestionTranslation":"전체 레이아웃을 내가 직접 디자인했어."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/33/expressions/135/practice-examples/99575ddd-f139-4e38-8b13-15f7dfcecab1.png","sentenceText":"I can''t come up with a good title.","sentenceWords":["I","can''t","come","up","with","a","good","title"],"highlightingPart":"come up with","practiceQuestion":"Have you chosen a title?","sentenceTranslation":"좋은 제목이 안 떠올라.","sentenceWordChoices":["with","up","can''t","good","he","come","we","you","a","title","I"],"practiceQuestionTranslation":"제목 정했어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/33/expressions/135/practice-examples/7d7279f3-5162-4ae6-a5b4-fc46a7a5a43f.png","sentenceText":"Give me a minute to come up with something.","sentenceWords":["Give","me","a","minute","to","come","up","with","something"],"highlightingPart":"come up with","practiceQuestion":"Do you have any ideas yet?","sentenceTranslation":"뭔가 생각해 낼 시간을 좀 줘.","sentenceWordChoices":["a","to","Giveed","Give","with","Giveing","come","something","Gives","up","me","minute"],"practiceQuestionTranslation":"아직 떠오른 아이디어 없어?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        136,
        34,
        'SCENARIO',
        'DAILY_ROUTINE',
        'BASIC',
        'EN',
        'KR',
        1,
        'run out of',
        '~이 다 떨어지다, 바닥나다',
        '다 떨어졌을 때 run out of',
        '''~이 다 떨어지다, 바닥나다''라는 뜻의 필수 구동사입니다. 시간, 돈, 재료, 인내심까지 소진되는 모든 것에 쓸 수 있어요.',
        'Why do we need to stop by the store?',
        '왜 마트에 들러야 해?',
        'We ran out of milk.',
        '우유가 다 떨어졌어.',
        ARRAY['We', 'ran', 'out', 'of', 'milk'],
        ARRAY['We', 'I', 'out', 'milk', 'ran', 'you', 'they', 'of'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/34/expressions/136/practice-examples/e2278849-59db-4465-9aa6-6c1f6f17c2fe.png","sentenceText":"I''m running out of time.","sentenceWords":["I''m","running","out","of","time"],"highlightingPart":"running out of","practiceQuestion":"Can you finish before the deadline?","sentenceTranslation":"시간이 얼마 안 남았어.","sentenceWordChoices":["of","I''m","runn","runns","time","running","out","runned"],"practiceQuestionTranslation":"마감 전에 끝낼 수 있어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/34/expressions/136/practice-examples/610d455b-7b01-4746-b1e0-5fd8405f5412.png","sentenceText":"My phone ran out of battery.","sentenceWords":["My","phone","ran","out","of","battery"],"highlightingPart":"ran out of","practiceQuestion":"Why did your phone turn off?","sentenceTranslation":"폰 배터리가 다 됐어.","sentenceWordChoices":["your","ran","battery","out","our","phone","his","My","of"],"practiceQuestionTranslation":"휴대폰이 왜 꺼졌어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/34/expressions/136/practice-examples/8111586b-50e0-42da-9b16-e6d3a1dd1008.png","sentenceText":"We''re running out of ideas.","sentenceWords":["We''re","running","out","of","ideas"],"highlightingPart":"running out of","practiceQuestion":"Do we have another idea?","sentenceTranslation":"아이디어가 바닥나고 있어.","sentenceWordChoices":["runns","running","We''re","runn","of","runned","ideas","out"],"practiceQuestionTranslation":"우리 다른 아이디어 있어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/34/expressions/136/practice-examples/77f713a0-f00e-4d30-bb47-4a19b2c7e114.png","sentenceText":"The store ran out of umbrellas because of the rain.","sentenceWords":["The","store","ran","out","of","umbrellas","because","of","the","rain"],"highlightingPart":"ran out of","practiceQuestion":"Why couldn''t I buy an umbrella there?","sentenceTranslation":"비 때문에 그 가게 우산이 동났대.","sentenceWordChoices":["of","an","the","rain","some","out","The","of","umbrellas","ran","because","store","a"],"practiceQuestionTranslation":"거기서 왜 우산을 못 샀어?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        137,
        34,
        'SCENARIO',
        'DAILY_ROUTINE',
        'BASIC',
        'EN',
        'KR',
        2,
        'give it a shot',
        '한번 해보다, 시도해 보다',
        '한번 시도해 보는 give it a shot',
        '''한번 해보다'', ''시도해 보다''라는 뜻의 캐주얼한 표현입니다. give it a try, give it a go도 같은 의미로, 부담 없이 도전을 권하거나 받아들일 때 써요.',
        'You''ve never done this before, right?',
        '이거 처음 해보는 거지?',
        'I''ve never tried it, but I''ll give it a shot.',
        '해본 적은 없는데 한번 해볼게.',
        ARRAY['I''ve', 'never', 'tried', 'it', 'but', 'I''ll', 'give', 'it', 'a', 'shot'],
        ARRAY['never', 'I''ll', 'tried', 'shot', 'I''ve', 'give', 'nevers', 'it', 'but', 'a', 'nevering', 'nevered', 'it'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/34/expressions/137/practice-examples/42c11647-9252-41df-a600-e92f50213d94.png","sentenceText":"Why not give it a shot? You might like it.","sentenceWords":["Why","not","give","it","a","shot","You","might","like","it"],"highlightingPart":"give it a shot","practiceQuestion":"Do you think I should try it?","sentenceTranslation":"한번 해보지 그래? 마음에 들지도 몰라.","sentenceWordChoices":["it","give","giveing","it","not","might","gives","You","a","shot","Why","like","giveed"],"practiceQuestionTranslation":"나도 한번 해볼까?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/34/expressions/137/practice-examples/b520512d-1c50-46a9-b099-40b6a52a1edf.png","sentenceText":"She gave baking a shot and loved it.","sentenceWords":["She","gave","baking","a","shot","and","loved","it"],"highlightingPart":"gave baking a shot","practiceQuestion":"How did she discover she liked baking?","sentenceTranslation":"걔 베이킹 한번 해봤는데 완전 빠졌어.","sentenceWordChoices":["give","and","shot","a","it","gave","gives","She","loved","giving","baking"],"practiceQuestionTranslation":"걔는 베이킹을 좋아하는지 어떻게 알게 됐어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/34/expressions/137/practice-examples/66a137af-1fa1-4739-a90c-078535f86d93.png","sentenceText":"It''s worth giving it a shot.","sentenceWords":["It''s","worth","giving","it","a","shot"],"highlightingPart":"giving it a shot","practiceQuestion":"Is it worth trying?","sentenceTranslation":"한번 해볼 만한 가치는 있어.","sentenceWordChoices":["it","worth","worthed","giving","a","worths","It''s","worthing","shot"],"practiceQuestionTranslation":"한번 해볼 만해?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/34/expressions/137/practice-examples/315bdb2e-376e-4aef-b909-2b7f617f51bd.png","sentenceText":"Give it a go — what''s the worst that can happen?","sentenceWords":["Give","it","a","go","what''s","the","worst","that","can","happen"],"highlightingPart":"Give it a go","practiceQuestion":"I''m afraid I''ll fail.","sentenceTranslation":"해봐. 잘못돼 봤자 뭐 얼마나 잘못되겠어?","sentenceWordChoices":["happen","worst","that","Give","can","go","Giveing","a","Gives","what''s","Giveed","the","it"],"practiceQuestionTranslation":"실패할까 봐 무서워."}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        138,
        34,
        'SCENARIO',
        'OPINION_JUDGMENT',
        'BASIC',
        'EN',
        'KR',
        3,
        'It might be better to ~',
        '~하는 게 나을 수도 있다',
        '조심스럽게 제안하는 It might be better to ~',
        '''~하는 게 나을 수도 있어''라며 반대나 제안을 부드럽게 포장하는 표현입니다. You should보다 훨씬 덜 지시적이라 상대의 계획을 조심스럽게 바꿀 때 좋아요.',
        'Should we drive there tonight?',
        '오늘 밤에 차로 갈까?',
        'It might be better to leave in the morning.',
        '아침에 출발하는 게 나을 수도 있어.',
        ARRAY['It', 'might', 'be', 'better', 'to', 'leave', 'in', 'the', 'morning'],
        ARRAY['to', 'in', 'morning', 'It', 'might', 'better', 'mighting', 'the', 'leave', 'mights', 'mighted', 'be'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/34/expressions/138/practice-examples/9c818036-b798-437f-b1d5-5f1bee2bab31.png","sentenceText":"It might be better to ask first.","sentenceWords":["It","might","be","better","to","ask","first"],"highlightingPart":"It might be better","practiceQuestion":"Can I use it without asking?","sentenceTranslation":"먼저 물어보는 게 나을 것 같아.","sentenceWordChoices":["It","might","to","mights","first","be","mighting","better","ask","mighted"],"practiceQuestionTranslation":"물어보지 않고 써도 될까?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/34/expressions/138/practice-examples/5d81fa1c-9b75-458b-b447-c4b15752d865.png","sentenceText":"It might be better to wait for the sale.","sentenceWords":["It","might","be","better","to","wait","for","the","sale"],"highlightingPart":"It might be better","practiceQuestion":"Should I buy it before the discount starts?","sentenceTranslation":"세일 기다리는 게 나을지도 몰라.","sentenceWordChoices":["better","mighting","sale","mights","to","be","the","wait","mighted","for","might","It"],"practiceQuestionTranslation":"할인 전에 사야 할까?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/34/expressions/138/practice-examples/ecaa6acb-e3eb-4c97-909e-5d6b63f656f5.png","sentenceText":"Maybe it''d be better to split into two groups.","sentenceWords":["Maybe","it''d","be","better","to","split","into","two","groups"],"highlightingPart":"it''d be better","practiceQuestion":"How should we organize such a large group?","sentenceTranslation":"두 조로 나누는 게 나을 수도 있겠다.","sentenceWordChoices":["to","groups","Maybe","two","Maybes","Maybeed","it''d","split","better","Maybeing","be","into"],"practiceQuestionTranslation":"이렇게 큰 모임을 어떻게 나누면 좋을까?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/34/expressions/138/practice-examples/763cc1fb-8f8e-48c1-b1e7-c6f6ad9f0987.png","sentenceText":"It might be better not to mention it.","sentenceWords":["It","might","be","better","not","to","mention","it"],"highlightingPart":"It might be better","practiceQuestion":"Should I tell her what happened?","sentenceTranslation":"그 얘긴 안 꺼내는 게 나을 수도 있어.","sentenceWordChoices":["better","to","mention","It","mighted","mights","mighting","might","be","it","not"],"practiceQuestionTranslation":"걔한테 무슨 일이 있었는지 말해야 할까?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        139,
        34,
        'SCENARIO',
        'OPINION_JUDGMENT',
        'BASIC',
        'EN',
        'KR',
        4,
        'appeal to',
        '~의 마음에 들다, 매력적으로 다가가다',
        '마음을 끄는 appeal to',
        '''~의 마음에 들다, ~에게 매력적으로 다가가다''라는 뜻입니다. 주어가 사물이고 사람이 to 뒤에 오는 어순이라, 취향을 세련되게 말할 수 있어요.',
        'Do you like this design?',
        '이 디자인 마음에 들어?',
        'This design really appeals to me.',
        '이 디자인 진짜 내 마음에 들어.',
        ARRAY['This', 'design', 'really', 'appeals', 'to', 'me'],
        ARRAY['to', 'really', 'me', 'that', 'appeals', 'design', 'these', 'those', 'This'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/34/expressions/139/practice-examples/790e54a5-fa62-4a4c-ab53-09c280782d11.png","sentenceText":"The idea of traveling alone appeals to me.","sentenceWords":["The","idea","of","traveling","alone","appeals","to","me"],"highlightingPart":"appeals to","practiceQuestion":"Would you like to travel by yourself?","sentenceTranslation":"혼자 여행한다는 게 마음에 끌려.","sentenceWordChoices":["an","The","idea","of","to","some","traveling","me","appeals","a","alone"],"practiceQuestionTranslation":"혼자 여행해 보고 싶어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/34/expressions/139/practice-examples/b38d554a-f99f-46dc-8951-52016cdcedbc.png","sentenceText":"That kind of humor doesn''t appeal to me.","sentenceWords":["That","kind","of","humor","doesn''t","appeal","to","me"],"highlightingPart":"appeal to","practiceQuestion":"Do you like that kind of joke?","sentenceTranslation":"그런 유머는 나한테 안 와닿아.","sentenceWordChoices":["kind","these","doesn''t","appeal","That","this","those","to","of","me","humor"],"practiceQuestionTranslation":"그런 농담 좋아해?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/34/expressions/139/practice-examples/dd20d6f4-df87-4abf-b0f0-7b7612ed9e0a.png","sentenceText":"The menu appeals to all ages.","sentenceWords":["The","menu","appeals","to","all","ages"],"highlightingPart":"appeals to","practiceQuestion":"Who would enjoy this menu?","sentenceTranslation":"그 메뉴는 모든 연령대에게 매력적이야.","sentenceWordChoices":["appeals","menu","some","to","The","all","ages","an","a"],"practiceQuestionTranslation":"이 메뉴는 어떤 사람들이 좋아할까?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/34/expressions/139/practice-examples/7515cd97-c1a1-4802-8af4-1d12fc4026e2.png","sentenceText":"Minimalist styles appeal to young buyers.","sentenceWords":["Minimalist","styles","appeal","to","young","buyers"],"highlightingPart":"appeal to","practiceQuestion":"Why do young buyers like that style?","sentenceTranslation":"미니멀한 스타일이 젊은 구매자들에게 인기야.","sentenceWordChoices":["to","Minimalisted","Minimalisting","styles","buyers","appeal","Minimalist","Minimalists","young"],"practiceQuestionTranslation":"젊은 구매자들은 왜 그 스타일을 좋아해?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        140,
        35,
        'SCENARIO',
        'CONVERSATION_SKILL',
        'BASIC',
        'EN',
        'KR',
        1,
        'look good on',
        '~에게 잘 어울리다',
        '잘 어울린다고 말하는 look good on',
        '옷이나 액세서리가 상대에게 잘 어울릴 때 쓰는 대표적인 칭찬 표현입니다. 주어가 사람이 아니라 ''옷''이라는 점이 한국어와 어순이 달라 헷갈리기 쉬워요.',
        'Does this shirt suit me?',
        '이 셔츠 나한테 어울려?',
        'That color looks good on you.',
        '그 색 너한테 잘 어울린다.',
        ARRAY['That', 'color', 'looks', 'good', 'on', 'you'],
        ARRAY['on', 'good', 'you', 'That', 'this', 'color', 'these', 'those', 'looks'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/35/expressions/140/practice-examples/ac8a4640-56dc-42df-8300-552e57940ac7.png","sentenceText":"Those glasses look great on you.","sentenceWords":["Those","glasses","look","great","on","you"],"highlightingPart":"look great on","practiceQuestion":"What do you think of these glasses?","sentenceTranslation":"그 안경 너한테 진짜 잘 어울려.","sentenceWordChoices":["glasses","great","these","look","you","that","Those","on","this"],"practiceQuestionTranslation":"이 안경 어때 보여?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/35/expressions/140/practice-examples/9f14e65b-61ff-41f8-8f29-6d0b728544ba.png","sentenceText":"Does this jacket look good on me?","sentenceWords":["Does","this","jacket","look","good","on","me"],"highlightingPart":"look good on","practiceQuestion":"That jacket is on sale today.","sentenceTranslation":"이 재킷 나한테 어울려?","sentenceWordChoices":["this","look","Does","jacket","do","on","me","doing","good","did"],"practiceQuestionTranslation":"그 재킷 오늘 세일해."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/35/expressions/140/practice-examples/30860e74-1c2e-4a67-bb4b-ad6f18e77ea0.png","sentenceText":"Short hair looks good on her.","sentenceWords":["Short","hair","looks","good","on","her"],"highlightingPart":"looks good on","practiceQuestion":"Does short hair suit her?","sentenceTranslation":"걔는 짧은 머리가 잘 어울려.","sentenceWordChoices":["Shorted","hair","good","on","looks","Shorting","Shorts","Short","her"],"practiceQuestionTranslation":"걔한테 짧은 머리가 어울려?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/35/expressions/140/practice-examples/e7036aaf-46ef-4ba8-b9f6-7896a0b1e027.png","sentenceText":"That dress would look amazing on you.","sentenceWords":["That","dress","would","look","amazing","on","you"],"highlightingPart":"would look amazing on","practiceQuestion":"Can you picture me wearing that dress?","sentenceTranslation":"그 원피스 너한테 진짜 잘 어울릴 거야.","sentenceWordChoices":["on","those","look","you","dress","this","That","amazing","would","these"],"practiceQuestionTranslation":"내가 저 원피스를 입은 모습이 상상돼?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        141,
        35,
        'SCENARIO',
        'DAILY_ROUTINE',
        'BASIC',
        'EN',
        'KR',
        2,
        'put on',
        '(옷을) 입다, 착용하다',
        '옷을 입는 put on',
        '''(옷·신발·화장 등을) 입다, 착용하다''라는 뜻으로, wear(입고 있는 상태)와 달리 착용하는 ''동작''을 나타냅니다. 이 차이가 회화의 디테일을 살려줘요.',
        'Brr, I''m freezing.',
        '으, 추워.',
        'Put on your coat. It''s cold outside.',
        '코트 입어. 밖에 추워.',
        ARRAY['Put', 'on', 'your', 'coat', 'It''s', 'cold', 'outside'],
        ARRAY['onto', 'outside', 'Put', 'at', 'coat', 'in', 'cold', 'on', 'your', 'It''s'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/35/expressions/141/practice-examples/f49db6dd-8957-44ac-ae5e-7886d23714d6.png","sentenceText":"She put on her makeup in five minutes.","sentenceWords":["She","put","on","her","makeup","in","five","minutes"],"highlightingPart":"put on","practiceQuestion":"How did she get ready so quickly?","sentenceTranslation":"걔는 5분 만에 화장을 했어.","sentenceWordChoices":["her","She","at","in","put","makeups","five","makeup","minutes","on","onto"],"practiceQuestionTranslation":"걔는 어떻게 그렇게 빨리 준비했어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/35/expressions/141/practice-examples/8b39ae1f-46b6-4448-806a-17705d289207.png","sentenceText":"Put on your seatbelt.","sentenceWords":["Put","on","your","seatbelt"],"highlightingPart":"Put on","practiceQuestion":"What should I do before the car starts moving?","sentenceTranslation":"안전벨트 매.","sentenceWordChoices":["Put","on","in","at","seatbelt","onto","your"],"practiceQuestionTranslation":"차가 출발하기 전에 뭘 해야 해?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/35/expressions/141/practice-examples/8e911469-e9d3-4444-ace7-6123c8a95ad4.png","sentenceText":"He put on his glasses to read the menu.","sentenceWords":["He","put","on","his","glasses","to","read","the","menu"],"highlightingPart":"put on","practiceQuestion":"Why did he need his glasses?","sentenceTranslation":"걔는 메뉴를 보려고 안경을 썼어.","sentenceWordChoices":["menu","put","glasses","read","his","the","on","at","He","in","onto","to"],"practiceQuestionTranslation":"걔는 왜 안경이 필요했어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/35/expressions/141/practice-examples/f62eb362-9ac2-47c9-be60-4dd5cdfb339f.png","sentenceText":"Don''t forget to put on sunscreen.","sentenceWords":["Don''t","forget","to","put","on","sunscreen"],"highlightingPart":"put on","practiceQuestion":"What should I remember before going outside?","sentenceTranslation":"선크림 바르는 거 잊지 마.","sentenceWordChoices":["forgeted","put","sunscreen","to","forget","forgets","forgeting","Don''t","on"],"practiceQuestionTranslation":"밖에 나가기 전에 뭘 기억해야 해?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        142,
        35,
        'SCENARIO',
        'MONEY_SPENDING',
        'BASIC',
        'EN',
        'KR',
        3,
        'out of my price range',
        '내 예산 밖이다',
        '비싸다고 말하는 out of my price range',
        '''내 예산 범위를 벗어났다'', 즉 ''나한테는 너무 비싸다''를 세련되게 표현하는 방법입니다. expensive보다 내 상황 기준으로 말하는 거라 덜 직설적이에요.',
        'Why don''t you get that bag?',
        '그 가방 사지 그래?',
        'That bag is out of my price range.',
        '그 가방은 내 예산 밖이야.',
        ARRAY['That', 'bag', 'is', 'out', 'of', 'my', 'price', 'range'],
        ARRAY['out', 'That', 'is', 'my', 'these', 'this', 'range', 'of', 'price', 'bag', 'those'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/35/expressions/142/practice-examples/e1dc4cfd-3528-4a1b-9175-aeceadb14e18.png","sentenceText":"Most apartments here are out of my price range.","sentenceWords":["Most","apartments","here","are","out","of","my","price","range"],"highlightingPart":"out of my price range","practiceQuestion":"Why don''t you rent an apartment here?","sentenceTranslation":"여기 아파트는 대부분 내 예산을 넘어.","sentenceWordChoices":["price","my","Most","Mosted","here","Mosting","range","Mosts","apartments","are","of","out"],"practiceQuestionTranslation":"왜 여기 아파트를 빌리지 않아?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/35/expressions/142/practice-examples/d3b32596-0eec-4286-ad8d-3be1887f9f10.png","sentenceText":"Is this within your price range?","sentenceWords":["Is","this","within","your","price","range"],"highlightingPart":"within your price range","practiceQuestion":"I really like this one.","sentenceTranslation":"이건 예산 범위 안에 들어오나요?","sentenceWordChoices":["within","be","price","your","Is","range","was","this","are"],"practiceQuestionTranslation":"난 이게 정말 마음에 들어."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/35/expressions/142/practice-examples/939daff4-ff83-4706-906a-f7170de6f5e6.png","sentenceText":"The hotel was way out of our price range.","sentenceWords":["The","hotel","was","way","out","of","our","price","range"],"highlightingPart":"out of our price range","practiceQuestion":"Why didn''t you stay at that hotel?","sentenceTranslation":"그 호텔은 우리 예산을 한참 벗어났어.","sentenceWordChoices":["way","hotel","our","out","price","was","an","a","of","range","The","some"],"practiceQuestionTranslation":"왜 그 호텔에 묵지 않았어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/35/expressions/142/practice-examples/eac5945c-e146-49cb-9f97-dcbc80399d33.png","sentenceText":"Anything under 50 dollars is in my price range.","sentenceWords":["Anything","under","50","dollars","is","in","my","price","range"],"highlightingPart":"in my price range","practiceQuestion":"How much are you willing to spend?","sentenceTranslation":"50달러 아래면 내 예산 안이야.","sentenceWordChoices":["Anyth","in","range","my","Anyths","50","is","Anythed","dollars","Anything","price","under"],"practiceQuestionTranslation":"얼마까지 쓸 수 있어?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        143,
        35,
        'SCENARIO',
        'MONEY_SPENDING',
        'BASIC',
        'EN',
        'KR',
        4,
        'marked down',
        '가격이 인하된',
        '가격이 확 내려간 marked down',
        '''가격이 인하된''이라는 뜻으로, mark down은 가격표를 내려 적는 행위에서 왔습니다. marked way down이라고 하면 ''가격이 확 깎였다''는 강조 표현이 돼요.',
        'Why is this store so busy?',
        '이 가게 왜 이렇게 붐벼?',
        'Everything is marked down 50%.',
        '전부 50% 할인 중이야.',
        ARRAY['Everything', 'is', 'marked', 'down', '50%'],
        ARRAY['is', 'Everyths', '50%', 'Everyth', 'down', 'marked', 'Everythed', 'Everything'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/35/expressions/143/practice-examples/e29c65fb-1f29-47ec-9f24-b36ead79cceb.png","sentenceText":"These boots were marked down from 100 to 40 dollars.","sentenceWords":["These","boots","were","marked","down","from","100","to","40","dollars"],"highlightingPart":"marked down","practiceQuestion":"How much did you pay for those boots?","sentenceTranslation":"이 부츠 100달러에서 40달러로 내려갔어.","sentenceWordChoices":["boots","to","this","marked","down","40","These","that","dollars","100","those","from","were"],"practiceQuestionTranslation":"그 부츠 얼마에 샀어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/35/expressions/143/practice-examples/ace10d11-a1ad-4570-9a7f-961ffd537f38.png","sentenceText":"Winter coats are marked down this week.","sentenceWords":["Winter","coats","are","marked","down","this","week"],"highlightingPart":"marked down","practiceQuestion":"Are any winter coats on sale?","sentenceTranslation":"이번 주에 겨울 코트가 할인 중이야.","sentenceWordChoices":["this","Winters","are","marked","Winter","Wintered","week","Wintering","coats","down"],"practiceQuestionTranslation":"할인 중인 겨울 코트가 있어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/35/expressions/143/practice-examples/94c23ef4-d81d-452d-a534-6bb39f37c57e.png","sentenceText":"I only buy things when they''re marked down.","sentenceWords":["I","only","buy","things","when","they''re","marked","down"],"highlightingPart":"marked down","practiceQuestion":"When do you usually buy things?","sentenceTranslation":"난 할인할 때만 물건을 사.","sentenceWordChoices":["marked","he","we","down","things","buy","only","I","when","you","they''re"],"practiceQuestionTranslation":"보통 언제 물건을 사?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/35/expressions/143/practice-examples/c885db34-fa71-4510-b434-23c399ae32f1.png","sentenceText":"The bakery marks down bread in the evening.","sentenceWords":["The","bakery","marks","down","bread","in","the","evening"],"highlightingPart":"marks down","practiceQuestion":"When can I get bread for less?","sentenceTranslation":"그 빵집은 저녁에 빵 가격을 내려.","sentenceWordChoices":["bread","some","down","an","The","the","a","marks","evening","in","bakery"],"practiceQuestionTranslation":"언제 빵을 더 싸게 살 수 있어?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        144,
        36,
        'SCENARIO',
        'RELATIONSHIP_SOCIAL',
        'BASIC',
        'EN',
        'KR',
        1,
        'get you ~',
        '너 주려고 ~ 사 오다/가져오다',
        '뭔가를 가져다줄 때 get you ~',
        '''너 주려고 ~ 사 왔어/가져왔어''라고 말할 때 get someone something 구조를 씁니다. buy보다 훨씬 캐주얼하고 폭넓게 쓰이는 원어민식 표현이에요.',
        'Why are you hiding something behind your back?',
        '등 뒤에 뭐 숨겼어?',
        'I got you a present!',
        '너 주려고 선물 샀어!',
        ARRAY['I', 'got', 'you', 'a', 'present'],
        ARRAY['got', 'a', 'he', 'I', 'get', 'present', 'you', 'we'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/36/expressions/144/practice-examples/3b761b85-ab4e-49d0-9f2f-1122d227ddc7.png","sentenceText":"I got you some snacks on the way.","sentenceWords":["I","got","you","some","snacks","on","the","way"],"highlightingPart":"got you","practiceQuestion":"Did you bring me anything?","sentenceTranslation":"오는 길에 간식 좀 사 왔어.","sentenceWordChoices":["I","the","on","he","you","got","some","we","way","snacks","get"],"practiceQuestionTranslation":"나 주려고 뭐 가져왔어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/36/expressions/144/practice-examples/bbd40ae0-17ac-474c-b0ad-664f502e6bfc.png","sentenceText":"Can I get you anything?","sentenceWords":["Can","I","get","you","anything"],"highlightingPart":"get you","practiceQuestion":"Thanks for having me over.","sentenceTranslation":"뭐 갖다드릴까요?","sentenceWordChoices":["would","I","you","Can","anything","should","get","could"],"practiceQuestionTranslation":"초대해 줘서 고마워."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/36/expressions/144/practice-examples/58e64a8f-e354-49c0-bd2b-e310675b1819.png","sentenceText":"She got me a scarf for my birthday.","sentenceWords":["She","got","me","a","scarf","for","my","birthday"],"highlightingPart":"got me","practiceQuestion":"What did she give you for your birthday?","sentenceTranslation":"걔가 내 생일 선물로 목도리를 사줬어.","sentenceWordChoices":["getting","get","birthday","gets","my","for","got","a","me","She","scarf"],"practiceQuestionTranslation":"걔가 생일에 뭘 줬어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/36/expressions/144/practice-examples/fc5dd6d4-0f68-47cd-92f7-76c72ce176c5.png","sentenceText":"I''ll get you a chair.","sentenceWords":["I''ll","get","you","a","chair"],"highlightingPart":"get you","practiceQuestion":"There aren''t enough seats.","sentenceTranslation":"의자 갖다줄게.","sentenceWordChoices":["they","get","we","you","a","I","chair","I''ll"],"practiceQuestionTranslation":"자리가 부족하네."}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        145,
        36,
        'SCENARIO',
        'MONEY_SPENDING',
        'BASIC',
        'EN',
        'KR',
        2,
        'shop around',
        '여러 곳을 둘러보며 비교하다',
        '그냥 둘러보는 중일 때 shop around',
        '''여러 곳을 둘러보며 비교하다''라는 뜻입니다. 가게에서 점원이 도와줄까 물을 때 I''m just shopping around(그냥 둘러보는 중이에요)로 답하면 자연스러워요.',
        'Can I help you find something?',
        '뭐 찾으시는 거 도와드릴까요?',
        'I''m just shopping around for now.',
        '지금은 그냥 둘러보는 중이에요.',
        ARRAY['I''m', 'just', 'shopping', 'around', 'for', 'now'],
        ARRAY['justed', 'I''m', 'shopping', 'justing', 'around', 'now', 'just', 'for', 'justs'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/36/expressions/145/practice-examples/21b98032-9eb8-49e3-aab9-d0d3c5ab405d.png","sentenceText":"Shop around before you buy a laptop.","sentenceWords":["Shop","around","before","you","buy","a","laptop"],"highlightingPart":"Shop around","practiceQuestion":"Should I buy the first laptop I see?","sentenceTranslation":"노트북 사기 전에 여기저기 비교해 봐.","sentenceWordChoices":["Shoped","Shops","laptop","before","you","buy","around","Shop","a","Shoping"],"practiceQuestionTranslation":"처음 본 노트북을 바로 살까?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/36/expressions/145/practice-examples/48a1917e-012b-4e31-ba66-b65353367f62.png","sentenceText":"We shopped around for the best insurance rate.","sentenceWords":["We","shopped","around","for","the","best","insurance","rate"],"highlightingPart":"shopped around","practiceQuestion":"How did you find the best insurance price?","sentenceTranslation":"제일 좋은 보험료 찾으려고 여기저기 알아봤어.","sentenceWordChoices":["We","you","shopped","best","insurance","I","around","for","they","rate","the"],"practiceQuestionTranslation":"가장 좋은 보험료를 어떻게 찾았어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/36/expressions/145/practice-examples/3ae5fcc1-1e19-4b50-bd87-9cef03a5f2a2.png","sentenceText":"It pays to shop around.","sentenceWords":["It","pays","to","shop","around"],"highlightingPart":"shop around","practiceQuestion":"Is comparing several stores worth the effort?","sentenceTranslation":"발품 팔면 그만한 값을 해.","sentenceWordChoices":["payed","to","pay","around","shop","It","pays","paying"],"practiceQuestionTranslation":"여러 곳을 비교할 가치가 있어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/36/expressions/145/practice-examples/e03b7fb1-38ac-4fbc-8109-c47bac0a4445.png","sentenceText":"I''m shopping around for a new dentist.","sentenceWords":["I''m","shopping","around","for","a","new","dentist"],"highlightingPart":"shopping around","practiceQuestion":"Have you chosen a new dentist?","sentenceTranslation":"새 치과를 알아보는 중이야.","sentenceWordChoices":["shopps","shopp","shopped","for","shopping","a","new","around","I''m","dentist"],"practiceQuestionTranslation":"새 치과 정했어?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        146,
        36,
        'SCENARIO',
        'MONEY_SPENDING',
        'BASIC',
        'EN',
        'KR',
        3,
        'worth of',
        '~어치',
        '금액만큼의 양을 말하는 worth of',
        '''~어치''를 표현할 때 [금액 + worth of + 명사] 구조를 씁니다. 돈뿐 아니라 a week''s worth of(일주일 치)처럼 시간의 분량에도 쓸 수 있어요.',
        'How much did you spend on snacks?',
        '간식에 얼마 썼어?',
        'I bought twenty dollars'' worth of snacks.',
        '간식을 20달러어치 샀어.',
        ARRAY['I', 'bought', 'twenty', 'dollars', 'worth', 'of', 'snacks'],
        ARRAY['we', 'twenty', 'dollars', 'he', 'snacks', 'you', 'of', 'bought', 'worth', 'I'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/36/expressions/146/practice-examples/ce8fee0a-858e-4467-85ea-4b41215ec971.png","sentenceText":"Can I get five dollars'' worth of stamps?","sentenceWords":["Can","I","get","five","dollars","worth","of","stamps"],"highlightingPart":"worth of","practiceQuestion":"How many stamps would you like?","sentenceTranslation":"우표 5달러어치 주시겠어요?","sentenceWordChoices":["five","Can","of","dollars","could","would","worth","stamps","get","I","should"],"practiceQuestionTranslation":"우표를 얼마나 드릴까요?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/36/expressions/146/practice-examples/0eee9836-4fda-4518-a0ff-9e5bb96d5680.png","sentenceText":"We ordered fifty thousand won''s worth of chicken.","sentenceWords":["We","ordered","fifty","thousand","won''s","worth","of","chicken"],"highlightingPart":"worth of","practiceQuestion":"How much chicken did you order?","sentenceTranslation":"치킨을 5만 원어치 시켰어.","sentenceWordChoices":["We","ordered","of","fifty","thousand","they","worth","you","won''s","chicken","I"],"practiceQuestionTranslation":"치킨을 얼마나 시켰어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/36/expressions/146/practice-examples/5367d3ae-0d5b-487e-9158-181540c3a8b5.png","sentenceText":"I packed a week''s worth of clothes.","sentenceWords":["I","packed","a","week''s","worth","of","clothes"],"highlightingPart":"worth of","practiceQuestion":"How much clothing did you pack?","sentenceTranslation":"일주일 치 옷을 챙겼어.","sentenceWordChoices":["packed","of","worth","a","we","clothes","he","I","week''s","you"],"practiceQuestionTranslation":"옷을 얼마나 챙겼어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/36/expressions/146/practice-examples/e35f0d16-ab6f-4e67-b1b7-5d937abb7003.png","sentenceText":"There''s years'' worth of photos on this phone.","sentenceWords":["There''s","years","worth","of","photos","on","this","phone"],"highlightingPart":"worth of","practiceQuestion":"How many photos are on this phone?","sentenceTranslation":"이 폰엔 몇 년 치 사진이 들어 있어.","sentenceWordChoices":["worth","year","yearing","this","on","yeared","There''s","photos","years","of","phone"],"practiceQuestionTranslation":"이 휴대폰에 사진이 얼마나 있어?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        147,
        36,
        'SCENARIO',
        'MONEY_SPENDING',
        'BASIC',
        'EN',
        'KR',
        4,
        'money is tight',
        '돈이 빠듯하다',
        '돈이 부족할 때 money is tight',
        '''요즘 돈이 빠듯해''라고 경제 사정을 에둘러 표현하는 방법입니다. tight(빡빡한)를 써서 여유가 없는 상태를 감각적으로 전달해요.',
        'Want to go shopping this weekend?',
        '이번 주말에 쇼핑 갈래?',
        'Money is tight this month.',
        '이번 달은 돈이 빠듯해.',
        ARRAY['Money', 'is', 'tight', 'this', 'month'],
        ARRAY['is', 'Moneys', 'tight', 'Moneying', 'Money', 'Moneyed', 'month', 'this'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/36/expressions/147/practice-examples/c77d6aa5-40d7-4c59-950d-10353d92f8e4.png","sentenceText":"Things are a bit tight until payday.","sentenceWords":["Things","are","a","bit","tight","until","payday"],"highlightingPart":"Things are a bit tight","practiceQuestion":"Can you buy it before payday?","sentenceTranslation":"월급날까지는 좀 빠듯해.","sentenceWordChoices":["Thinging","a","bit","until","Thing","are","payday","Things","Thinged","tight"],"practiceQuestionTranslation":"월급날 전에 그걸 살 수 있어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/36/expressions/147/practice-examples/291053bd-08ca-4cba-9011-574c6093faf1.png","sentenceText":"Money was tight when I was in college.","sentenceWords":["Money","was","tight","when","I","was","in","college"],"highlightingPart":"Money was tight","practiceQuestion":"What was college life like financially?","sentenceTranslation":"대학 다닐 땐 돈이 늘 부족했어.","sentenceWordChoices":["in","I","tight","Money","was","college","Moneyed","Moneying","when","Moneys","was"],"practiceQuestionTranslation":"대학 때 경제적으로 어땠어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/36/expressions/147/practice-examples/d20b361a-2839-4c21-9603-e088943b79fe.png","sentenceText":"We''re on a tight budget right now.","sentenceWords":["We''re","on","a","tight","budget","right","now"],"highlightingPart":"tight budget","practiceQuestion":"Can we spend more this month?","sentenceTranslation":"우리 지금 예산이 빡빡해.","sentenceWordChoices":["tight","in","at","budget","We''re","on","right","now","onto","a"],"practiceQuestionTranslation":"이번 달에 돈을 더 써도 될까?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/36/expressions/147/practice-examples/5bb5f155-6893-4fd7-a050-212183d61203.png","sentenceText":"With money being tight, we''re skipping the trip.","sentenceWords":["With","money","being","tight","we''re","skipping","the","trip"],"highlightingPart":"money being tight","practiceQuestion":"Why did you cancel the trip?","sentenceTranslation":"돈이 빠듯해서 여행은 건너뛰기로 했어.","sentenceWordChoices":["money","we''re","trip","skipping","tight","being","the","With","to","for","without"],"practiceQuestionTranslation":"왜 여행을 취소했어?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        148,
        37,
        'SCENARIO',
        'MONEY_SPENDING',
        'CLASSIC_COMMON',
        'EN',
        'KR',
        1,
        'You get what you pay for',
        '싼 게 비지떡이다',
        '싼 게 비지떡, You get what you pay for',
        '''지불한 만큼 얻는다'', 즉 싼 물건은 그만한 이유가 있다는 뜻의 속담 같은 표현입니다. 품질과 가격의 관계를 말할 때 원어민이 입버릇처럼 쓰는 문장이에요.',
        'Those cheap headphones broke after a week.',
        '그 싼 헤드폰은 일주일 만에 고장 났어.',
        'Well, you get what you pay for.',
        '뭐, 싼 게 비지떡이지.',
        ARRAY['Well', 'you', 'get', 'what', 'you', 'pay', 'for'],
        ARRAY['paid', 'you', 'Well', 'what', 'for', 'get', 'buy', 'pay', 'you', 'pays'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/37/expressions/148/practice-examples/175c493b-4649-419e-a4bd-19098b2c4677.png","sentenceText":"Cheap shoes hurt my feet. You get what you pay for.","sentenceWords":["Cheap","shoes","hurt","my","feet","You","get","what","you","pay","for"],"highlightingPart":"You get what you pay for","practiceQuestion":"Why do those shoes hurt?","sentenceTranslation":"싼 신발은 발이 아파. 싼 게 비지떡이야.","sentenceWordChoices":["Cheap","for","hurt","what","Cheaps","shoes","you","Cheaped","Cheaping","pay","get","my","You","feet"],"practiceQuestionTranslation":"그 신발은 왜 발이 아파?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/37/expressions/148/practice-examples/1d1bb02b-1472-4aad-9651-79a23daeb606.png","sentenceText":"The flight was cheap, but you get what you pay for.","sentenceWords":["The","flight","was","cheap","but","you","get","what","you","pay","for"],"highlightingPart":"you get what you pay for","practiceQuestion":"How was that really cheap flight?","sentenceTranslation":"비행기표는 쌌는데, 딱 그 값을 하더라.","sentenceWordChoices":["pay","get","for","what","cheap","you","was","some","flight","The","but","a","an","you"],"practiceQuestionTranslation":"그 엄청 싼 항공편은 어땠어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/37/expressions/148/practice-examples/63432361-ccc5-4cd8-ac9e-ce7c031ce3e3.png","sentenceText":"With furniture, you really get what you pay for.","sentenceWords":["With","furniture","you","really","get","what","you","pay","for"],"highlightingPart":"get what you pay for","practiceQuestion":"Does price matter when buying furniture?","sentenceTranslation":"가구는 정말 돈을 들인 만큼 값어치를 해.","sentenceWordChoices":["you","for","pay","With","you","furniture","get","without","really","Withs","to","what"],"practiceQuestionTranslation":"가구를 살 때 가격이 중요해?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/37/expressions/148/practice-examples/469e982f-b991-4c6e-9aa1-3bc2ae3cd743.png","sentenceText":"I bought the expensive one because you get what you pay for.","sentenceWords":["I","bought","the","expensive","one","because","you","get","what","you","pay","for"],"highlightingPart":"you get what you pay for","practiceQuestion":"Why did you choose the expensive one?","sentenceTranslation":"돈값을 하니까 비싼 걸로 샀어.","sentenceWordChoices":["what","bought","he","the","get","I","you","you","for","because","one","expensive","pay","we","boughts"],"practiceQuestionTranslation":"왜 비싼 걸 골랐어?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        149,
        37,
        'SCENARIO',
        'MONEY_SPENDING',
        'BASIC',
        'EN',
        'KR',
        2,
        'for its price',
        '가격에 비해서',
        '가격 대비를 말하는 for its price',
        '''가격에 비해서'', ''이 가격치고는''이라고 가성비를 표현할 때 전치사 for를 씁니다. for its size(크기에 비해)처럼 다양한 기준에 응용할 수 있어요.',
        'Is this phone worth it?',
        '이 폰 살 만해?',
        'This phone is great for its price.',
        '이 폰은 가격 대비 훌륭해.',
        ARRAY['This', 'phone', 'is', 'great', 'for', 'its', 'price'],
        ARRAY['This', 'price', 'phone', 'those', 'that', 'is', 'these', 'for', 'its', 'great'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/37/expressions/149/practice-examples/e129076e-e3eb-4e4b-83d6-aa9215aa221f.png","sentenceText":"The food was amazing for the price.","sentenceWords":["The","food","was","amazing","for","the","price"],"highlightingPart":"for the price","practiceQuestion":"Was the food good for the price?","sentenceTranslation":"그 가격치고 음식이 엄청 훌륭했어.","sentenceWordChoices":["food","the","some","amazing","for","an","a","price","The","was"],"practiceQuestionTranslation":"음식은 가격 대비 괜찮았어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/37/expressions/149/practice-examples/5d3bdaec-9ffc-48b2-a681-17e5e561bd1e.png","sentenceText":"For its price, this laptop is hard to beat.","sentenceWords":["For","its","price","this","laptop","is","hard","to","beat"],"highlightingPart":"For its price","practiceQuestion":"Is this laptop good value?","sentenceTranslation":"이 가격대에서 이 노트북 이길 게 없어.","sentenceWordChoices":["with","is","hard","to","prices","For","its","of","laptop","beat","this","price"],"practiceQuestionTranslation":"이 노트북은 가격 대비 괜찮아?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/37/expressions/149/practice-examples/9dc2669b-2ef5-488a-a4a2-a42e2acf0637.png","sentenceText":"The hotel was pretty nice for what we paid.","sentenceWords":["The","hotel","was","pretty","nice","for","what","we","paid"],"highlightingPart":"for what we paid","practiceQuestion":"Was the hotel worth what you paid?","sentenceTranslation":"낸 돈에 비해 호텔이 꽤 괜찮았어.","sentenceWordChoices":["for","nice","pretty","some","we","paid","hotel","an","The","what","was","a"],"practiceQuestionTranslation":"그 호텔은 낸 돈만큼 괜찮았어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/37/expressions/149/practice-examples/ca4c2822-0354-4c93-9a17-c4aeb2f30219.png","sentenceText":"It''s excellent for its price.","sentenceWords":["It''s","excellent","for","its","price"],"highlightingPart":"for its price","practiceQuestion":"Is this camera any good?","sentenceTranslation":"가격 대비 아주 훌륭해.","sentenceWordChoices":["excellent","their","price","for","It''s","its","expensive","at"],"practiceQuestionTranslation":"이 카메라 괜찮아?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        150,
        37,
        'SCENARIO',
        'MONEY_SPENDING',
        'BASIC',
        'EN',
        'KR',
        3,
        'get a good deal',
        '싸게 잘 사다',
        '싸게 잘 샀을 때 get a good deal',
        '''좋은 조건에 사다'', ''싸게 잘 사다''라는 뜻입니다. deal은 ''거래''라서, get a good deal on 뒤에 산 물건을 붙이면 돼요.',
        'That phone was so cheap. How?',
        '그 폰 엄청 싸던데. 어떻게?',
        'I got a good deal on my new phone.',
        '새 폰 싸게 잘 샀어.',
        ARRAY['I', 'got', 'a', 'good', 'deal', 'on', 'my', 'new', 'phone'],
        ARRAY['good', 'on', 'phone', 'got', 'new', 'he', 'a', 'we', 'you', 'I', 'my', 'deal'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/37/expressions/150/practice-examples/70164b37-a2d0-4dde-8124-2f3758732ef2.png","sentenceText":"You got a really good deal!","sentenceWords":["You","got","a","really","good","deal"],"highlightingPart":"good deal","practiceQuestion":"Did I pay too much for it?","sentenceTranslation":"너 진짜 싸게 잘 샀다!","sentenceWordChoices":["I","really","a","we","good","deal","You","got","they"],"practiceQuestionTranslation":"내가 너무 비싸게 산 걸까?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/37/expressions/150/practice-examples/31b00ae5-5f6d-4f80-9bf0-35d5f7a3919c.png","sentenceText":"We got a great deal on the hotel.","sentenceWords":["We","got","a","great","deal","on","the","hotel"],"highlightingPart":"got a great deal","practiceQuestion":"Was the hotel expensive?","sentenceTranslation":"호텔을 아주 좋은 가격에 잡았어.","sentenceWordChoices":["the","got","a","great","I","hotel","on","deal","you","they","We"],"practiceQuestionTranslation":"호텔이 비쌌어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/37/expressions/150/practice-examples/13990ab0-cbd4-42ef-91ed-3324bc24153d.png","sentenceText":"Is this a good deal, or should I wait?","sentenceWords":["Is","this","a","good","deal","or","should","I","wait"],"highlightingPart":"good deal","practiceQuestion":"The seller says this is the lowest price.","sentenceTranslation":"이거 괜찮은 가격이야, 아니면 기다릴까?","sentenceWordChoices":["deal","are","a","I","this","or","wait","should","was","good","Is","be"],"practiceQuestionTranslation":"판매자는 이게 최저가라고 하네."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/37/expressions/150/practice-examples/bae1d07c-64ee-4f24-98db-2c296f6138b3.png","sentenceText":"She always gets good deals during sales.","sentenceWords":["She","always","gets","good","deals","during","sales"],"highlightingPart":"good deal","practiceQuestion":"How does she save so much when shopping?","sentenceTranslation":"걔는 세일 때마다 득템을 해.","sentenceWordChoices":["gets","sales","alwaying","deals","She","during","alwayed","alway","always","good"],"practiceQuestionTranslation":"걔는 쇼핑할 때 어떻게 그렇게 돈을 아껴?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        151,
        37,
        'SCENARIO',
        'OPINION_JUDGMENT',
        'BASIC',
        'EN',
        'KR',
        4,
        'I''d say ~',
        '~쯤 될 것 같다, 내 생각엔 ~다',
        '부드럽게 의견을 내는 I''d say ~',
        '단정하지 않고 ''~쯤 될 것 같은데'', ''내 생각엔 ~야''라고 말하는 부드러운 의견 제시법입니다. 수치, 평가, 추측 앞에 붙이면 어감이 훨씬 유연해져요.',
        'How long will the project take?',
        '그 프로젝트 얼마나 걸릴까?',
        'I''d say about two weeks.',
        '한 2주쯤 걸릴 것 같은데.',
        ARRAY['I''d', 'say', 'about', 'two', 'weeks'],
        ARRAY['two', 'I''d', 'abouts', 'about', 'say', 'abouted', 'weeks', 'abouting'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/37/expressions/151/practice-examples/3a9e71db-1331-49d4-8461-36210fbac272.png","sentenceText":"I''d say it''s worth the price.","sentenceWords":["I''d","say","it''s","worth","the","price"],"highlightingPart":"I''d say","practiceQuestion":"Do you think it''s worth buying?","sentenceTranslation":"그 정도 값어치는 한다고 봐.","sentenceWordChoices":["I''d","worths","worthing","it''s","the","price","say","worthed","worth"],"practiceQuestionTranslation":"그거 살 가치가 있다고 생각해?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/37/expressions/151/practice-examples/73e10951-56c6-4c21-b8e2-cf02118c8295.png","sentenceText":"I''d say she''s in her early thirties.","sentenceWords":["I''d","say","she''s","in","her","early","thirties"],"highlightingPart":"I''d say","practiceQuestion":"How old do you think she is?","sentenceTranslation":"걔 삼십 대 초반쯤 됐을걸.","sentenceWordChoices":["say","her","on","in","early","thirties","into","she''s","I''d","at"],"practiceQuestionTranslation":"걔 몇 살쯤인 것 같아?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/37/expressions/151/practice-examples/9f6e64ae-62c9-45d8-9bd6-6c497a7ecf02.png","sentenceText":"Overall, I''d say it went pretty well.","sentenceWords":["Overall","I''d","say","it","went","pretty","well"],"highlightingPart":"I''d say","practiceQuestion":"How did the event go?","sentenceTranslation":"전반적으로 꽤 잘됐다고 봐.","sentenceWordChoices":["it","Overalls","went","I''d","well","Overalled","Overalling","say","pretty","Overall"],"practiceQuestionTranslation":"행사는 어떻게 됐어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/37/expressions/151/practice-examples/357280e0-284c-47bb-8886-c8c068c98fad.png","sentenceText":"I wouldn''t say it''s the best, but it''s solid.","sentenceWords":["I","wouldn''t","say","it''s","the","best","but","it''s","solid"],"highlightingPart":"I wouldn''t say","practiceQuestion":"Is it the best option available?","sentenceTranslation":"최고라곤 못 해도 탄탄해.","sentenceWordChoices":["it''s","it''s","he","wouldn''t","the","say","best","but","solid","we","I","you"],"practiceQuestionTranslation":"그게 가장 좋은 선택지야?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        152,
        38,
        'SCENARIO',
        'OPINION_JUDGMENT',
        'BASIC',
        'EN',
        'KR',
        1,
        'sleep on it',
        '하룻밤 자며 생각해 보다',
        '하룻밤 자며 고민하는 sleep on it',
        '''하룻밤 자면서 생각해 보다'', 즉 결정을 서두르지 않고 미뤄두겠다는 표현입니다. 중요한 결정 앞에서 시간을 벌 때 아주 자연스러운 한마디예요.',
        'So, do we have a deal?',
        '그래서, 계약하는 거야?',
        'Can I sleep on it and tell you tomorrow?',
        '하루 생각해 보고 내일 말해도 될까?',
        ARRAY['Can', 'I', 'sleep', 'on', 'it', 'and', 'tell', 'you', 'tomorrow'],
        ARRAY['tell', 'on', 'it', 'tomorrow', 'Can', 'I', 'you', 'and', 'could', 'should', 'sleep', 'would'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/38/expressions/152/practice-examples/79fb7b8f-95b4-4e1a-9bb0-cf154a3e8319.png","sentenceText":"Don''t decide now. Sleep on it.","sentenceWords":["Don''t","decide","now","Sleep","on","it"],"highlightingPart":"Sleep on it","practiceQuestion":"Should I decide right now?","sentenceTranslation":"지금 정하지 마. 하루 자고 생각해 봐.","sentenceWordChoices":["Don''t","decideed","it","Sleep","decideing","now","decide","on","decides"],"practiceQuestionTranslation":"지금 바로 결정해야 할까?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/38/expressions/152/practice-examples/3f23baf9-5a13-4f55-8128-c6821e158a77.png","sentenceText":"I slept on it, and I''ve decided to take the offer.","sentenceWords":["I","slept","on","it","and","I''ve","decided","to","take","the","offer"],"highlightingPart":"slept on it","practiceQuestion":"Have you made a decision about the offer?","sentenceTranslation":"하루 고민해 봤는데, 그 제안 받기로 했어.","sentenceWordChoices":["take","slept","I''ve","on","I","offer","you","we","decided","it","to","and","the","he"],"practiceQuestionTranslation":"그 제안을 어떻게 할지 결정했어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/38/expressions/152/practice-examples/a1d032af-3f6b-4b36-ac73-d94e861b5be4.png","sentenceText":"Big decisions? Always sleep on them.","sentenceWords":["Big","decisions","Always","sleep","on","them"],"highlightingPart":"sleep on them","practiceQuestion":"What do you do before making a big decision?","sentenceTranslation":"큰 결정? 무조건 하루는 묵혀봐야지.","sentenceWordChoices":["Big","them","decision","on","sleep","decisioned","Always","decisions","decisioning"],"practiceQuestionTranslation":"큰 결정을 하기 전에 뭘 해?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/38/expressions/152/practice-examples/4318108b-3c9e-419c-84ab-f50d77d671d9.png","sentenceText":"Let me sleep on it and get back to you.","sentenceWords":["Let","me","sleep","on","it","and","get","back","to","you"],"highlightingPart":"sleep on it","practiceQuestion":"Can you give me an answer now?","sentenceTranslation":"하루 생각해 보고 다시 연락드릴게요.","sentenceWordChoices":["me","you","sleeps","sleeped","sleep","Let","sleeping","back","it","on","to","get","and"],"practiceQuestionTranslation":"지금 답을 줄 수 있어?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        153,
        38,
        'SCENARIO',
        'POLITE_EXPRESSION',
        'BASIC',
        'EN',
        'KR',
        2,
        'Could you possibly ~?',
        '혹시 ~해 주실 수 있을까요?',
        '한층 더 정중한 Could you possibly ~?',
        'Could you ~?에 possibly를 더하면 ''혹시 ~해 주실 수 있을까요?''라고 정중함이 한 단계 올라갑니다. 무리일 수 있는 부탁에 미안함을 실어주는 부사예요.',
        'Is there anything else you need from me?',
        '저한테 더 필요하신 거 있나요?',
        'Could you possibly give me one more day?',
        '혹시 하루만 더 주실 수 있을까요?',
        ARRAY['Could', 'you', 'possibly', 'give', 'me', 'one', 'more', 'day'],
        ARRAY['more', 'me', 'give', 'day', 'would', 'Could', 'one', 'should', 'can', 'possibly', 'you'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/38/expressions/153/practice-examples/6f0a5dd9-58ce-4a1f-ba36-fa9015e46721.png","sentenceText":"Could you possibly send that file again?","sentenceWords":["Could","you","possibly","send","that","file","again"],"highlightingPart":"Could you possibly","practiceQuestion":"I can''t find the file you sent.","sentenceTranslation":"혹시 그 파일 다시 보내주실 수 있어요?","sentenceWordChoices":["should","would","can","that","file","send","possibly","Could","you","again"],"practiceQuestionTranslation":"보내 준 파일을 찾을 수가 없어요."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/38/expressions/153/practice-examples/ad62b53f-4941-44ee-b4a1-ed46e0e1bfc2.png","sentenceText":"Could you possibly lower the price a little?","sentenceWords":["Could","you","possibly","lower","the","price","a","little"],"highlightingPart":"Could you possibly","practiceQuestion":"The price is a little higher than my budget.","sentenceTranslation":"혹시 가격을 조금만 깎아주실 수 있나요?","sentenceWordChoices":["would","the","possibly","a","you","lower","should","Could","little","can","price"],"practiceQuestionTranslation":"가격이 제 예산보다 조금 높아요."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/38/expressions/153/practice-examples/03d6615d-3867-48c6-8f1b-c065cc2bb613.png","sentenceText":"Could you possibly watch my bag for a second?","sentenceWords":["Could","you","possibly","watch","my","bag","for","a","second"],"highlightingPart":"Could you possibly","practiceQuestion":"I need to step away for a moment.","sentenceTranslation":"혹시 잠깐 제 가방 좀 봐주실 수 있어요?","sentenceWordChoices":["can","should","my","Could","a","for","possibly","you","second","would","bag","watch"],"practiceQuestionTranslation":"잠깐 자리를 비워야 해요."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/38/expressions/153/practice-examples/98f14a87-0c3e-48a7-8c30-04b06e5d440f.png","sentenceText":"Could you possibly speak a bit slower?","sentenceWords":["Could","you","possibly","speak","a","bit","slower"],"highlightingPart":"Could you possibly","practiceQuestion":"I''m having trouble understanding you.","sentenceTranslation":"혹시 조금만 천천히 말씀해 주시겠어요?","sentenceWordChoices":["you","can","should","a","bit","slower","possibly","Could","would","speak"],"practiceQuestionTranslation":"말씀을 이해하기가 조금 어려워요."}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        154,
        38,
        'SCENARIO',
        'CONVERSATION_SKILL',
        'BASIC',
        'EN',
        'KR',
        3,
        'off the top of my head',
        '당장 떠오르는 대로, 즉석에서',
        '당장 떠오르는 대로 말하는 off the top of my head',
        '''즉석에서 생각나는 대로'', ''확실친 않지만 지금 떠오르기로는''이라는 뜻입니다. 정확한 정보를 찾아보지 않고 기억에 의존해 답할 때 붙이는 안전장치예요.',
        'How much do you think it''ll cost?',
        '그거 얼마쯤 할 것 같아?',
        'Off the top of my head, it''s around 50 dollars.',
        '당장 떠오르기로는 50달러쯤이야.',
        ARRAY['Off', 'the', 'top', 'of', 'my', 'head', 'it''s', 'around', '50', 'dollars'],
        ARRAY['my', 'top', 'Off', 'an', 'it''s', 'of', 'around', 'head', 'the', 'dollars', '50', 'a', 'some'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/38/expressions/154/practice-examples/7c5acf2f-75a8-4a47-a2e9-4e1e4d2d784f.png","sentenceText":"I can''t remember the exact date off the top of my head.","sentenceWords":["I","can''t","remember","the","exact","date","off","the","top","of","my","head"],"highlightingPart":"off the top of my head","practiceQuestion":"Do you remember the exact date?","sentenceTranslation":"정확한 날짜는 당장 기억이 안 나네.","sentenceWordChoices":["off","the","of","top","we","the","he","my","can''t","I","remember","head","exact","date","you"],"practiceQuestionTranslation":"정확한 날짜 기억나?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/38/expressions/154/practice-examples/cc742428-7e46-493c-92a6-f6fe91eaea67.png","sentenceText":"Off the top of my head, I''d say ten people are coming.","sentenceWords":["Off","the","top","of","my","head","I''d","say","ten","people","are","coming"],"highlightingPart":"Off the top of my head","practiceQuestion":"About how many people are coming?","sentenceTranslation":"지금 당장 떠오르는 대로 말하면 열 명쯤 올 것 같아.","sentenceWordChoices":["Off","an","ten","are","people","the","some","top","coming","I''d","say","head","my","of","a"],"practiceQuestionTranslation":"대략 몇 명 정도 올 것 같아?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/38/expressions/154/practice-examples/584b2f81-db10-434a-972a-f533cb19e1c7.png","sentenceText":"Can you name three examples off the top of your head?","sentenceWords":["Can","you","name","three","examples","off","the","top","of","your","head"],"highlightingPart":"off the top of your head","practiceQuestion":"Let''s see how quickly you can think of examples.","sentenceTranslation":"바로 생각나는 예시 세 개 말해볼래?","sentenceWordChoices":["you","head","the","examples","of","your","name","off","Can","could","top","should","three","would"],"practiceQuestionTranslation":"예시를 얼마나 빨리 떠올리는지 보자."},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/38/expressions/154/practice-examples/a8de79d7-c55a-4028-98d9-a42f6399176c.png","sentenceText":"That''s just off the top of my head, so double-check it.","sentenceWords":["That''s","just","off","the","top","of","my","head","so","double-check","it"],"highlightingPart":"off the top of my head","practiceQuestion":"How reliable is that estimate?","sentenceTranslation":"그냥 떠오르는 대로 말한 거니까 다시 확인해 봐.","sentenceWordChoices":["the","it","head","off","so","my","just","of","That''s","justing","justed","double-check","justs","top"],"practiceQuestionTranslation":"그 추정은 얼마나 정확해?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        155,
        38,
        'SCENARIO',
        'MONEY_SPENDING',
        'CLASSIC_COMMON',
        'EN',
        'KR',
        4,
        'cost an arm and a leg',
        '터무니없이 비싸다',
        '터무니없이 비쌀 때 cost an arm and a leg',
        '''팔과 다리를 내줄 만큼 비싸다'', 즉 ''터무니없이 비싸다''라는 과장 표현입니다. 그냥 expensive보다 훨씬 생생하게 가격 충격을 전달해요.',
        'Why don''t you just buy that bag?',
        '그 가방 그냥 사지 그래?',
        'That bag costs an arm and a leg.',
        '그 가방 억 소리 나게 비싸.',
        ARRAY['That', 'bag', 'costs', 'an', 'arm', 'and', 'a', 'leg'],
        ARRAY['an', 'arm', 'That', 'bag', 'this', 'leg', 'those', 'and', 'costs', 'these', 'a'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/38/expressions/155/practice-examples/ad768583-b3d2-4d0b-90dc-d94c44a5639c.png","sentenceText":"Rent in this city costs an arm and a leg.","sentenceWords":["Rent","in","this","city","costs","an","arm","and","a","leg"],"highlightingPart":"costs an arm and a leg","practiceQuestion":"How expensive is rent in this city?","sentenceTranslation":"이 도시 월세는 살인적이야.","sentenceWordChoices":["costs","Rent","leg","arm","Renting","a","city","and","Rented","this","in","an","Rents"],"practiceQuestionTranslation":"이 도시 월세는 얼마나 비싸?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/38/expressions/155/practice-examples/ba46b9af-d216-4930-87d3-7b8a96f8c20c.png","sentenceText":"The repair cost me an arm and a leg.","sentenceWords":["The","repair","cost","me","an","arm","and","a","leg"],"highlightingPart":"cost me an arm and a leg","practiceQuestion":"Was the repair expensive?","sentenceTranslation":"수리비가 어마어마하게 나왔어.","sentenceWordChoices":["repair","repairs","arm","The","and","leg","me","cost","a","an","some","repaired"],"practiceQuestionTranslation":"수리비 많이 나왔어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/38/expressions/155/practice-examples/8bbc07a8-e411-49d5-af75-749008fed913.png","sentenceText":"Concert tickets these days cost an arm and a leg.","sentenceWords":["Concert","tickets","these","days","cost","an","arm","and","a","leg"],"highlightingPart":"cost an arm and a leg","practiceQuestion":"Why aren''t you going to the concert?","sentenceTranslation":"요즘 콘서트 티켓 값이 엄청 비싸.","sentenceWordChoices":["days","Concerted","and","Concert","leg","an","Concerts","cost","these","arm","Concerting","a","tickets"],"practiceQuestionTranslation":"왜 콘서트에 안 가?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/38/expressions/155/practice-examples/c033ad58-db1a-4324-83b2-b4e74594cb9d.png","sentenceText":"It''s nice, but it''ll cost you an arm and a leg.","sentenceWords":["It''s","nice","but","it''ll","cost","you","an","arm","and","a","leg"],"highlightingPart":"cost you an arm and a leg","practiceQuestion":"Should I buy it even though it''s expensive?","sentenceTranslation":"좋긴 한데 값이 어마어마할걸.","sentenceWordChoices":["but","a","niceed","arm","nices","It''s","nice","you","leg","cost","niceing","it''ll","an","and"],"practiceQuestionTranslation":"비싸도 그걸 사야 할까?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        156,
        39,
        'SCENARIO',
        'MONEY_SPENDING',
        'BASIC',
        'EN',
        'KR',
        1,
        'send back',
        '돌려보내다, 반품하다',
        '반품하는 send back',
        '''돌려보내다, 반품하다''라는 뜻입니다. 온라인 쇼핑 시대의 필수 표현으로, 음식이 잘못 나왔을 때 주방으로 돌려보내는 상황에도 쓸 수 있어요.',
        'Why did you return the shirt?',
        '왜 셔츠 반품했어?',
        'The shirt didn''t fit, so I sent it back.',
        '셔츠가 안 맞아서 반품했어.',
        ARRAY['The', 'shirt', 'didn''t', 'fit', 'so', 'I', 'sent', 'it', 'back'],
        ARRAY['an', 'it', 'The', 'back', 'so', 'didn''t', 'a', 'fit', 'shirt', 'some', 'I', 'sent'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/39/expressions/156/practice-examples/7b48c1ca-fd60-40b8-be99-2fb933366fd0.png","sentenceText":"Can I send this back for a refund?","sentenceWords":["Can","I","send","this","back","for","a","refund"],"highlightingPart":"send this back","practiceQuestion":"Is there a problem with the item?","sentenceTranslation":"이거 반품하고 환불받을 수 있나요?","sentenceWordChoices":["back","should","Can","send","for","this","I","would","a","refund","could"],"practiceQuestionTranslation":"상품에 문제가 있나요?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/39/expressions/156/practice-examples/7d0053b9-de18-4848-984c-7dd32db24236.png","sentenceText":"She sent back the steak because it was undercooked.","sentenceWords":["She","sent","back","the","steak","because","it","was","undercooked"],"highlightingPart":"sent back","practiceQuestion":"Why didn''t she eat the steak?","sentenceTranslation":"스테이크가 덜 익어서 걔가 돌려보냈어.","sentenceWordChoices":["She","steak","it","undercooked","send","was","sending","back","sends","sent","because","the"],"practiceQuestionTranslation":"걔는 왜 스테이크를 먹지 않았어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/39/expressions/156/practice-examples/9e48ae9b-0dce-41e7-88ad-cfb8d334783f.png","sentenceText":"I need to send back these shoes by Friday.","sentenceWords":["I","need","to","send","back","these","shoes","by","Friday"],"highlightingPart":"send back","practiceQuestion":"When is the return deadline?","sentenceTranslation":"금요일까지 이 신발 반품해야 해.","sentenceWordChoices":["these","shoes","need","send","he","by","back","we","you","I","to","Friday"],"practiceQuestionTranslation":"반품 마감이 언제야?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/39/expressions/156/practice-examples/8280a7a6-2603-494f-b42a-5a93e7e23f93.png","sentenceText":"If it''s damaged, just send it back.","sentenceWords":["If","it''s","damaged","just","send","it","back"],"highlightingPart":"send it back","practiceQuestion":"What should I do with a damaged item?","sentenceTranslation":"파손됐으면 그냥 반품해.","sentenceWordChoices":["just","damaging","back","damag","send","damaged","it","If","it''s","damags"],"practiceQuestionTranslation":"파손된 상품은 어떻게 해야 해?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        157,
        39,
        'SCENARIO',
        'TRAVEL_MOVEMENT',
        'BASIC',
        'EN',
        'KR',
        2,
        'break down',
        '(기계·차가) 고장 나다',
        '차가 고장 났을 때 break down',
        '''(기계·차가) 고장 나다''라는 뜻의 구동사입니다. 사람에게 쓰면 ''(감정이) 무너지다, 오열하다''는 의미가 되니 문맥으로 구분하면 돼요.',
        'Why didn''t you drive here?',
        '왜 차 안 타고 왔어?',
        'My car broke down on the highway.',
        '고속도로에서 차가 고장 났어.',
        ARRAY['My', 'car', 'broke', 'down', 'on', 'the', 'highway'],
        ARRAY['our', 'his', 'your', 'car', 'down', 'highway', 'the', 'on', 'My', 'broke'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/39/expressions/157/practice-examples/de71dfad-634b-4c6f-9be0-b88de18f69f9.png","sentenceText":"The washing machine broke down again.","sentenceWords":["The","washing","machine","broke","down","again"],"highlightingPart":"broke down","practiceQuestion":"Why can''t you do the laundry?","sentenceTranslation":"세탁기가 또 고장 났어.","sentenceWordChoices":["again","The","down","machine","an","broke","a","some","washing"],"practiceQuestionTranslation":"왜 빨래를 못 해?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/39/expressions/157/practice-examples/ee39f7f5-b172-42af-a829-fdb6ec20be84.png","sentenceText":"What do I do if the car breaks down?","sentenceWords":["What","do","I","do","if","the","car","breaks","down"],"highlightingPart":"breaks down","practiceQuestion":"You''re driving a long way tomorrow, right?","sentenceTranslation":"차가 고장 나면 어떡하지?","sentenceWordChoices":["breaks","I","Whated","the","Whats","What","if","down","car","Whating","do","do"],"practiceQuestionTranslation":"내일 장거리 운전한다며?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/39/expressions/157/practice-examples/da01d573-1007-4219-bddb-d21c05e81a15.png","sentenceText":"The elevator broke down this morning.","sentenceWords":["The","elevator","broke","down","this","morning"],"highlightingPart":"broke down","practiceQuestion":"Why did you take the stairs?","sentenceTranslation":"오늘 아침에 엘리베이터가 고장 났어.","sentenceWordChoices":["down","this","elevator","a","some","The","broke","an","morning"],"practiceQuestionTranslation":"왜 계단으로 갔어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/39/expressions/157/practice-examples/e1468a70-07b0-4815-a0d1-04a016a7e623.png","sentenceText":"She broke down in tears at the news.","sentenceWords":["She","broke","down","in","tears","at","the","news"],"highlightingPart":"broke down","practiceQuestion":"What happened when she heard the news?","sentenceTranslation":"걔는 그 소식에 무너져서 울음을 터뜨렸어.","sentenceWordChoices":["in","down","She","broke","the","tears","breaks","at","news","break","broken"],"practiceQuestionTranslation":"걔는 그 소식을 듣고 어떻게 됐어?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        158,
        39,
        'SCENARIO',
        'DAILY_ROUTINE',
        'BASIC',
        'EN',
        'KR',
        3,
        'hassle',
        '귀찮은 일, 번거로움',
        '번거로움을 표현하는 hassle',
        '''귀찮은 일'', ''번거로움''을 뜻하는 명사로, 뭔가가 성가시고 수고스러울 때 딱 맞는 단어입니다. such a hassle, a huge hassle처럼 강조해서 자주 써요.',
        'Want to drive downtown?',
        '시내까지 차 몰고 갈래?',
        'Parking downtown is such a hassle.',
        '시내 주차는 진짜 번거로워.',
        ARRAY['Parking', 'downtown', 'is', 'such', 'a', 'hassle'],
        ARRAY['such', 'is', 'a', 'hassle', 'downtown', 'Parks', 'Parked', 'Parking', 'Park'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/39/expressions/158/practice-examples/f0d64bf0-aeda-402a-bec5-96647fd34e08.png","sentenceText":"Returning things online is a hassle.","sentenceWords":["Returning","things","online","is","a","hassle"],"highlightingPart":"hassle","practiceQuestion":"What''s difficult about returning things online?","sentenceTranslation":"온라인 반품은 귀찮은 일이야.","sentenceWordChoices":["online","things","hassle","Returns","Returning","Returned","Return","a","is"],"practiceQuestionTranslation":"온라인 반품의 어떤 점이 힘들어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/39/expressions/158/practice-examples/a1860c67-8c0e-4dab-afd6-7e1988ead6d3.png","sentenceText":"It''s too much of a hassle to cook for one person.","sentenceWords":["It''s","too","much","of","a","hassle","to","cook","for","one","person"],"highlightingPart":"hassle","practiceQuestion":"Why don''t you cook more often?","sentenceTranslation":"혼자 먹자고 요리하기엔 너무 번거로워.","sentenceWordChoices":["of","a","hassle","muching","person","too","cook","one","for","muched","much","muchs","to","It''s"],"practiceQuestionTranslation":"왜 요리를 더 자주 하지 않아?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/39/expressions/158/practice-examples/0cd4bca2-cdac-404f-b408-f1547408c1ed.png","sentenceText":"Moving is always a huge hassle.","sentenceWords":["Moving","is","always","a","huge","hassle"],"highlightingPart":"hassle","practiceQuestion":"How do you feel about moving?","sentenceTranslation":"이사는 언제나 엄청 성가셔.","sentenceWordChoices":["Moving","huge","Movs","hassle","is","Moved","a","always","Mov"],"practiceQuestionTranslation":"이사하는 거 어떻게 생각해?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/39/expressions/158/practice-examples/4069bac0-6c15-440d-8644-c2bc42c5f65a.png","sentenceText":"Save yourself the hassle and just take a taxi.","sentenceWords":["Save","yourself","the","hassle","and","just","take","a","taxi"],"highlightingPart":"hassle","practiceQuestion":"Should I drive or take a taxi?","sentenceTranslation":"번거로우니까 그냥 택시 타.","sentenceWordChoices":["Saveing","and","Save","yourself","a","take","hassle","Saveed","just","Saves","taxi","the"],"practiceQuestionTranslation":"운전할까, 택시 탈까?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        159,
        39,
        'SCENARIO',
        'CONVERSATION_SKILL',
        'BASIC',
        'EN',
        'KR',
        4,
        'get back to',
        '~에게 다시 연락하다, 회신하다',
        '나중에 회신하는 get back to',
        '''~에게 다시 연락하다, 회신하다''라는 뜻으로, 즉답 대신 시간을 버는 업무 필수 표현입니다. Let me get back to you on that(그건 확인해 보고 알려드릴게요)이 대표 활용형이에요.',
        'So can you make it to the workshop?',
        '그래서 워크숍 올 수 있어요?',
        'Let me check my schedule and get back to you.',
        '일정 확인해 보고 다시 연락드릴게요.',
        ARRAY['Let', 'me', 'check', 'my', 'schedule', 'and', 'get', 'back', 'to', 'you'],
        ARRAY['get', 'checks', 'back', 'check', 'you', 'Let', 'checked', 'me', 'and', 'schedule', 'checking', 'my', 'to'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/39/expressions/159/practice-examples/bf3b7a30-9e26-49cc-a051-48b7389cb064.png","sentenceText":"I''ll get back to you by end of day.","sentenceWords":["I''ll","get","back","to","you","by","end","of","day"],"highlightingPart":"get back to","practiceQuestion":"When will you have an answer for me?","sentenceTranslation":"오늘 안으로 회신드릴게요.","sentenceWordChoices":["backing","of","day","end","backed","backs","get","you","by","I''ll","back","to"],"practiceQuestionTranslation":"언제 답을 줄 수 있어요?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/39/expressions/159/practice-examples/96ec07e4-b678-46a2-87c1-a1ecc98248c1.png","sentenceText":"Sorry it took me so long to get back to you.","sentenceWords":["Sorry","it","took","me","so","long","to","get","back","to","you"],"highlightingPart":"get back to","practiceQuestion":"Why did your reply take so long?","sentenceTranslation":"답이 늦어서 미안해요.","sentenceWordChoices":["Sorryed","Sorrying","me","get","long","you","Sorry","it","to","back","took","so","Sorrys","to"],"practiceQuestionTranslation":"왜 답장이 그렇게 늦었어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/39/expressions/159/practice-examples/fbe59967-8d9e-4278-8d96-90aaf6274ec7.png","sentenceText":"Did the landlord ever get back to you?","sentenceWords":["Did","the","landlord","ever","get","back","to","you"],"highlightingPart":"get back to","practiceQuestion":"You contacted the landlord yesterday, right?","sentenceTranslation":"집주인한테서 연락 왔어?","sentenceWordChoices":["ever","do","back","done","Did","does","you","the","landlord","to","get"],"practiceQuestionTranslation":"어제 집주인한테 연락했다며?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/39/expressions/159/practice-examples/03f87573-7c92-4826-9ea8-fbdf1be77daa.png","sentenceText":"Let me get back to you on that.","sentenceWords":["Let","me","get","back","to","you","on","that"],"highlightingPart":"get back to","practiceQuestion":"Can you answer that question now?","sentenceTranslation":"그건 확인해 보고 알려드릴게요.","sentenceWordChoices":["back","backed","that","Let","get","you","backs","to","me","backing","on"],"practiceQuestionTranslation":"그 질문에 지금 답할 수 있어?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        160,
        40,
        'SCENARIO',
        'RELATIONSHIP_SOCIAL',
        'BASIC',
        'EN',
        'KR',
        1,
        'I just wanted to ~',
        '그냥 ~하고 싶었어',
        '용건을 꺼내는 I just wanted to ~',
        '''그냥 ~하고 싶어서 연락했어''라며 용건이나 목적을 부담 없이 밝히는 표현입니다. just가 들어가서 ''별거 아니고~'' 하는 가벼운 뉘앙스를 만들어줘요.',
        'Why are you calling so late?',
        '왜 이렇게 늦게 전화했어?',
        'I just wanted to say thank you.',
        '그냥 고맙다고 말하고 싶었어.',
        ARRAY['I', 'just', 'wanted', 'to', 'say', 'thank', 'you'],
        ARRAY['I', 'just', 'to', 'justs', 'we', 'say', 'he', 'wanted', 'thank', 'you'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/40/expressions/160/practice-examples/97736958-4e7d-408f-9d69-d17b1d5a02ad.png","sentenceText":"I just wanted to check in on you.","sentenceWords":["I","just","wanted","to","check","in","on","you"],"highlightingPart":"I just wanted to","practiceQuestion":"Why did you call me?","sentenceTranslation":"그냥 잘 지내나 궁금해서 연락했어.","sentenceWordChoices":["wanted","he","on","we","I","to","you","in","justs","check","just"],"practiceQuestionTranslation":"왜 나한테 전화했어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/40/expressions/160/practice-examples/9b266f45-c2aa-46ad-acaa-651bc9f66b4f.png","sentenceText":"I just wanted to let you know I arrived.","sentenceWords":["I","just","wanted","to","let","you","know","I","arrived"],"highlightingPart":"I just wanted to","practiceQuestion":"Why did you message me after landing?","sentenceTranslation":"도착했다고 알려주려고.","sentenceWordChoices":["justs","I","arrived","he","just","know","we","you","I","wanted","let","to"],"practiceQuestionTranslation":"도착하고 왜 연락했어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/40/expressions/160/practice-examples/a999e774-a96d-47af-917f-16a5d328d6fa.png","sentenceText":"I just wanted to ask a quick question.","sentenceWords":["I","just","wanted","to","ask","a","quick","question"],"highlightingPart":"I just wanted to","practiceQuestion":"What did you need to ask?","sentenceTranslation":"간단한 질문 하나만 하려고요.","sentenceWordChoices":["he","to","wanted","quick","we","you","just","question","ask","I","a"],"practiceQuestionTranslation":"뭘 물어보려고 했어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/40/expressions/160/practice-examples/4fb62297-05b8-435f-b767-c8adeeb8c06b.png","sentenceText":"I just wanted to see how the project is going.","sentenceWords":["I","just","wanted","to","see","how","the","project","is","going"],"highlightingPart":"I just wanted to","practiceQuestion":"Why are you checking on the project?","sentenceTranslation":"프로젝트 어떻게 되어가는지 궁금해서요.","sentenceWordChoices":["going","just","he","how","wanted","to","the","I","project","you","we","is","see"],"practiceQuestionTranslation":"프로젝트는 왜 확인하고 있어?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        161,
        40,
        'SCENARIO',
        'WORK_STUDY',
        'BASIC',
        'EN',
        'KR',
        2,
        'in the loop',
        '진행 상황을 계속 공유받는',
        '정보 공유망에 넣어달라는 in the loop',
        '''진행 상황을 계속 공유받는 상태''를 뜻합니다. Keep me in the loop이라고 하면 ''나한테도 계속 알려줘''라는 뜻으로, 직장에서 특히 많이 쓰여요. 반대는 out of the loop(소식을 모르는)입니다.',
        'I''ll email you when there''s news.',
        '소식 있으면 메일할게.',
        'Keep me in the loop, please.',
        '진행 상황 계속 공유해 줘.',
        ARRAY['Keep', 'me', 'in', 'the', 'loop', 'please'],
        ARRAY['the', 'me', 'Keep', 'Keeps', 'Keeped', 'Keeping', 'in', 'please', 'loop'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/40/expressions/161/practice-examples/a2c105f4-b442-4304-8b1f-88815ea8c7a6.png","sentenceText":"I''ll keep you in the loop on the schedule.","sentenceWords":["I''ll","keep","you","in","the","loop","on","the","schedule"],"highlightingPart":"in the loop","practiceQuestion":"How will I hear about schedule changes?","sentenceTranslation":"일정 관련해서 계속 알려줄게.","sentenceWordChoices":["the","keeps","keeped","on","I''ll","loop","in","you","schedule","the","keep","keeping"],"practiceQuestionTranslation":"일정 변경은 어떻게 알 수 있어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/40/expressions/161/practice-examples/e3cd0317-08ec-4120-8398-1d12fe100ab0.png","sentenceText":"Make sure the whole team is in the loop.","sentenceWords":["Make","sure","the","whole","team","is","in","the","loop"],"highlightingPart":"in the loop","practiceQuestion":"Does everyone on the team know?","sentenceTranslation":"팀 전체가 다 알고 있게 해줘.","sentenceWordChoices":["the","Makeing","Make","team","the","Makes","sure","is","whole","in","Makeed","loop"],"practiceQuestionTranslation":"팀원 모두가 알고 있어?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/40/expressions/161/practice-examples/7ad4a2b9-7154-4c2a-8487-2cd1b45747f6.png","sentenceText":"I''ve been on vacation, so I''m out of the loop.","sentenceWords":["I''ve","been","on","vacation","so","I''m","out","of","the","loop"],"highlightingPart":"out of the loop","practiceQuestion":"Why don''t you know what''s been happening?","sentenceTranslation":"휴가 다녀와서 요즘 소식을 잘 몰라.","sentenceWordChoices":["the","been","loop","I''ve","beens","on","so","beened","vacation","I''m","out","beening","of"],"practiceQuestionTranslation":"왜 최근 상황을 몰라?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/40/expressions/161/practice-examples/4f0ee6ca-b746-4fd9-a80e-c257d61b4d03.png","sentenceText":"Why wasn''t I kept in the loop?","sentenceWords":["Why","wasn''t","I","kept","in","the","loop"],"highlightingPart":"in the loop","practiceQuestion":"Did anyone tell you about the changes?","sentenceTranslation":"왜 나한테는 공유가 안 된 거야?","sentenceWordChoices":["we","in","the","wasn''t","Why","he","you","I","kept","loop"],"practiceQuestionTranslation":"변경 사항을 누가 알려줬어?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        162,
        40,
        'SCENARIO',
        'TRAVEL_MOVEMENT',
        'BASIC',
        'EN',
        'KR',
        3,
        'drop off',
        '내려주다, 갖다주다',
        '내려달라고 하는 drop off',
        '''(사람을) 내려주다, (물건을) 갖다주다''라는 뜻입니다. drop me off at(~에 내려줘)은 차 얻어 탈 때 필수 표현이에요.',
        'How will I get to the station?',
        '역까지 어떻게 가지?',
        'Can you drop me off at the station?',
        '역에 좀 내려줄래?',
        ARRAY['Can', 'you', 'drop', 'me', 'off', 'at', 'the', 'station'],
        ARRAY['off', 'would', 'could', 'me', 'at', 'you', 'drop', 'Can', 'station', 'should', 'the'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/40/expressions/162/practice-examples/7288f988-236b-4cfa-8ff7-d68081c137cd.png","sentenceText":"I''ll drop you off on my way to work.","sentenceWords":["I''ll","drop","you","off","on","my","way","to","work"],"highlightingPart":"drop you off","practiceQuestion":"How am I getting to work?","sentenceTranslation":"출근길에 내려줄게.","sentenceWordChoices":["way","droped","on","drop","my","work","droping","drops","I''ll","off","you","to"],"practiceQuestionTranslation":"회사에는 어떻게 갈 거야?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/40/expressions/162/practice-examples/338d0482-d270-436c-82e0-f232f5d95ce3.png","sentenceText":"She drops off her kids at school every morning.","sentenceWords":["She","drops","off","her","kids","at","school","every","morning"],"highlightingPart":"drops off","practiceQuestion":"What does she do before going to work?","sentenceTranslation":"걔는 매일 아침 애들을 학교에 데려다줘.","sentenceWordChoices":["droped","off","morning","school","She","at","droping","kids","her","drop","every","drops"],"practiceQuestionTranslation":"걔는 출근 전에 뭘 해?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/40/expressions/162/practice-examples/2cc93b86-27ed-46ab-9d2f-328cc438d4f2.png","sentenceText":"Can I drop off this package here?","sentenceWords":["Can","I","drop","off","this","package","here"],"highlightingPart":"drop off","practiceQuestion":"How can I help you today?","sentenceTranslation":"여기서 이 소포 맡길 수 있나요?","sentenceWordChoices":["could","I","would","Can","here","package","this","off","should","drop"],"practiceQuestionTranslation":"무엇을 도와드릴까요?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/40/expressions/162/practice-examples/082dca6f-535d-4056-804d-c19fffe2a22e.png","sentenceText":"Just drop me off at the corner.","sentenceWords":["Just","drop","me","off","at","the","corner"],"highlightingPart":"drop me off","practiceQuestion":"Where should I stop the car?","sentenceTranslation":"그냥 저 모퉁이에 내려줘.","sentenceWordChoices":["at","drop","Justs","Justed","Justing","Just","off","the","corner","me"],"practiceQuestionTranslation":"차를 어디에 세우면 돼?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);
INSERT INTO writing_expression (
    id, scenario_id, expression_source, expression_type, usage_frequency_level, target_locale, base_locale, display_order, target_expression_text, base_expression_meaning_text, usage_summary, usage_description, representative_question_text, representative_question_translation, representative_sentence_text, representative_sentence_translation, representative_sentence_words, representative_sentence_word_choices, representative_image_url, practice_examples_payload, status, created_at, updated_at
)
VALUES (
        163,
        40,
        'SCENARIO',
        'POLITE_EXPRESSION',
        'BASIC',
        'EN',
        'KR',
        4,
        'I''d appreciate it if you could ~',
        '~해 주시면 감사하겠습니다',
        '격식 있게 부탁하는 I''d appreciate it if you could ~',
        '''~해 주시면 감사하겠습니다''라는 뜻의 격식 있는 요청 표현입니다. 특히 업무 이메일에서 부탁이 명령처럼 들리지 않게 만들어주는 필수 문형이에요.',
        'I''ll send over the contract today.',
        '오늘 계약서 보내드릴게요.',
        'I''d appreciate it if you could get back to me by Friday.',
        '금요일까지 회신 주시면 감사하겠습니다.',
        ARRAY['I''d', 'appreciate', 'it', 'if', 'you', 'could', 'get', 'back', 'to', 'me', 'by', 'Friday'],
        ARRAY['appreciateing', 'if', 'appreciates', 'back', 'get', 'it', 'could', 'I''d', 'Friday', 'me', 'to', 'by', 'you', 'appreciateed', 'appreciate'],
        NULL,
        '[{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/40/expressions/163/practice-examples/efcf90de-59aa-4782-8292-c57e4604c4f1.png","sentenceText":"I''d appreciate it if you could keep this between us.","sentenceWords":["I''d","appreciate","it","if","you","could","keep","this","between","us"],"highlightingPart":"I''d appreciate","practiceQuestion":"Can I tell anyone else about this?","sentenceTranslation":"이건 우리끼리 비밀로 해주면 고맙겠어.","sentenceWordChoices":["us","I''d","keep","appreciate","if","between","it","this","appreciateed","appreciates","you","appreciateing","could"],"practiceQuestionTranslation":"이걸 다른 사람에게 말해도 돼?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/40/expressions/163/practice-examples/2495f817-50c2-4573-8ccf-e9b699aa6194.png","sentenceText":"I''d appreciate it if you could double-check the numbers.","sentenceWords":["I''d","appreciate","it","if","you","could","double-check","the","numbers"],"highlightingPart":"I''d appreciate","practiceQuestion":"What should I do with these numbers?","sentenceTranslation":"숫자를 한 번 더 확인해 주시면 감사하겠습니다.","sentenceWordChoices":["I''d","appreciate","you","appreciateing","double-check","if","appreciates","it","could","the","numbers","appreciateed"],"practiceQuestionTranslation":"이 숫자들은 어떻게 해야 하나요?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/40/expressions/163/practice-examples/a8ad3ac1-0038-410d-9f6d-62378f9e3174.png","sentenceText":"I''d appreciate it if you could let me know in advance.","sentenceWords":["I''d","appreciate","it","if","you","could","let","me","know","in","advance"],"highlightingPart":"I''d appreciate","practiceQuestion":"When should I tell you about changes?","sentenceTranslation":"미리 알려주시면 감사하겠습니다.","sentenceWordChoices":["it","you","in","advance","I''d","if","appreciateing","know","could","appreciates","appreciateed","appreciate","me","let"],"practiceQuestionTranslation":"변경 사항을 언제 알려드리면 될까요?"},{"imageUrl":"https://d19azau1un4t7r.cloudfront.net/content/scenarios/40/expressions/163/practice-examples/b6d3825d-e3b4-44da-aec8-90339258efe5.png","sentenceText":"I''d really appreciate any feedback.","sentenceWords":["I''d","really","appreciate","any","feedback"],"highlightingPart":"I''d really appreciate","practiceQuestion":"Would feedback be helpful?","sentenceTranslation":"어떤 피드백이든 주시면 정말 감사하겠습니다.","sentenceWordChoices":["any","appreciate","I''d","reallyed","reallys","reallying","feedback","really"],"practiceQuestionTranslation":"피드백이 도움이 될까요?"}]',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);

SELECT setval(pg_get_serial_sequence('writing_expression', 'id'), (SELECT MAX(id) FROM writing_expression));
