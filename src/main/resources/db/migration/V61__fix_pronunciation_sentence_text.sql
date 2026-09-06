-- 발음 평가 파이프라인이 처리할 수 없는 문장 표기를 고친다 (LAN-373).
--
-- 대상: "a.m."(토큰이 a/m으로 쪼개짐) 4건, "%"(발화 "percent"가 표기에 없음) 1건.
-- 숫자 자체(0~99)와 20/20·24/7·10/10은 AI 서버가 발화 철자로 처리하므로 두고,
-- 특히 20/20·24/7·10/10은 타겟 표현 그 자체라 문장을 바꾸지 않는다.
--
-- 문장 텍스트만 바꾸면 단어 배열 퀴즈(words/word_choices)가 어긋나므로 함께 갱신한다.
-- 옛 표기 토큰("a.m" 등)은 오답 선택지로도 남기지 않는다 — 문법적으로 틀린 표현이 아니라서
-- 오답 취급하면 유저에게 부당하다.
-- 617은 시간 표현이 사라져 번역도 함께 고친다.
-- WHERE에 기존 문장을 함께 걸어 이미 수정됐거나 다른 값이면 건드리지 않는다.

UPDATE writing_expression
SET representative_sentence_text = 'Everything is marked down 50 percent.',
    representative_sentence_words = ARRAY['Everything','is','marked','down','50','percent'],
    representative_sentence_word_choices =
        ARRAY['is','Anything','50','percent','Something','down','marked','Somehow','Everything']
WHERE id = 143
  AND representative_sentence_text = 'Everything is marked down 50%.';

UPDATE writing_expression
SET representative_sentence_text = 'I''ve been on the go since six in the morning.',
    representative_sentence_words =
        ARRAY['I''ve','been','on','the','go','since','six','in','the','morning'],
    representative_sentence_word_choices =
        ARRAY['going','go','the','six','been','on','since','I''ve','for','in','morning','the']
WHERE id = 208
  AND representative_sentence_text = 'I''ve been on the go since 6 a.m.';

UPDATE writing_expression
SET representative_sentence_text = 'No, I ended up watching Netflix until 3 in the morning.',
    representative_sentence_words =
        ARRAY['No','I','ended','up','watching','Netflix','until','3','in','the','morning'],
    representative_sentence_word_choices =
        ARRAY['up','No','until','watch','end','watching','I','by','Netflix','3','ended','in','the','morning']
WHERE id = 352
  AND representative_sentence_text = 'No, I ended up watching Netflix until 3 a.m.';

UPDATE writing_expression
SET representative_sentence_text = 'Don''t ask. It was a late-night impulse buy.',
    representative_sentence_translation = '묻지 마. 밤늦게 충동구매한 거야.',
    representative_sentence_words =
        ARRAY['Don''t','ask','It','was','a','late-night','impulse','buy'],
    representative_sentence_word_choices =
        ARRAY['buy','It','a','buys','Doesn''t','was','ask','impulse','Don''t','bought','late-night']
WHERE id = 617
  AND representative_sentence_text = 'Don''t ask. It was a 2 a.m. impulse buy.';

UPDATE writing_expression
SET representative_sentence_text = 'Whoa, take it down a notch. It''s 8 in the morning.',
    representative_sentence_words =
        ARRAY['Whoa','take','it','down','a','notch','It''s','8','in','the','morning'],
    representative_sentence_word_choices =
        ARRAY['down','notch','8','it','up','takes','notches','It''s','Whoa','take','a','in','the','morning']
WHERE id = 690
  AND representative_sentence_text = 'Whoa, take it down a notch. It''s 8 a.m.';
