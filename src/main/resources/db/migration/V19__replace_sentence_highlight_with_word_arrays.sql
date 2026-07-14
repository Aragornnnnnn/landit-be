-- 대표 예문 학습 방식을 '해석 하이라이트'에서 '단어 배열 맞추기'로 전환한다.
-- highlight 텍스트 컬럼을 제거하고, 정답 단어 배열/선택지 배열 컬럼을 추가한다.
-- ARRAY[...] 리터럴과 VARCHAR ARRAY는 H2/PostgreSQL 공통 문법이라 공통(db/migration) 폴더에 둔다.
-- 배열 컬럼은 기존 행 백필을 위해 우선 nullable로 추가하고, 백필 후 NOT NULL로 전환한다.

ALTER TABLE writing_expression DROP COLUMN representative_sentence_translation_highlight_text;

ALTER TABLE writing_expression ADD COLUMN representative_sentence_words VARCHAR ARRAY;
ALTER TABLE writing_expression ADD COLUMN representative_sentence_word_choices VARCHAR ARRAY;

-- 기존 행 백필 (id 1~83). 테스트(H2)는 행이 없어 0행 UPDATE로 무해하게 통과한다.


UPDATE writing_expression
SET representative_sentence_words = ARRAY['There''s', 'nothing', 'like', 'hiking', 'to', 'clear', 'my', 'head'],
    representative_sentence_word_choices = ARRAY['my', 'hiking', 'like', 'head', 'anything', 'There''s', 'his', 'nothing', 'to', 'cleared', 'clear'],
    updated_at = now()
WHERE id = 1;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['Gyeongbokgung', 'Palace', 'will', 'blow', 'your', 'mind'],
    representative_sentence_word_choices = ARRAY['Gyeongbokgung', 'mind', 'would', 'Palace', 'blow', 'will', 'brain', 'your', 'blew'],
    updated_at = now()
WHERE id = 2;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['I', 'grew', 'up', 'in', 'Busan', 'near', 'the', 'beach'],
    representative_sentence_word_choices = ARRAY['at', 'grow', 'up', 'in', 'beach', 'the', 'near', 'grew', 'on', 'I', 'Busan'],
    updated_at = now()
WHERE id = 3;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['You', 'should', 'check', 'out', 'Bukchon', 'Hanok', 'Village'],
    representative_sentence_word_choices = ARRAY['You', 'Bukchon', 'out', 'would', 'checked', 'in', 'Hanok', 'Village', 'should', 'check'],
    updated_at = now()
WHERE id = 4;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['I''m', 'really', 'into', 'hiking', 'these', 'days'],
    representative_sentence_word_choices = ARRAY['these', 'really', 'days', 'into', 'was', 'onto', 'those', 'I''m', 'hiking'],
    updated_at = now()
WHERE id = 5;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['We', 'can', 'work', 'out', 'a', 'cleaning', 'schedule', 'together'],
    representative_sentence_word_choices = ARRAY['the', 'in', 'can', 'work', 'schedule', 'We', 'cleaning', 'works', 'together', 'out', 'a'],
    updated_at = now()
WHERE id = 6;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['I''m', 'a', 'night', 'owl', 'so', 'I', 'usually', 'stay', 'up', 'late'],
    representative_sentence_word_choices = ARRAY['usually', 'up', 'owl', 'stay', 'I', 'a', 'down', 'stays', 'I''m', 'night', 'so', 'lately', 'late'],
    updated_at = now()
WHERE id = 7;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['I', 'get', 'sick', 'of', 'dishes', 'piling', 'up', 'in', 'the', 'sink'],
    representative_sentence_word_choices = ARRAY['sick', 'get', 'on', 'gets', 'tired', 'in', 'piling', 'I', 'sink', 'of', 'dishes', 'the', 'up'],
    updated_at = now()
WHERE id = 8;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['Saturday', 'works', 'better', 'for', 'me'],
    representative_sentence_word_choices = ARRAY['Saturday', 'works', 'to', 'better', 'good', 'me', 'work', 'for'],
    updated_at = now()
WHERE id = 9;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['I', 'can''t', 'wait', 'to', 'try', 'a', 'real', 'American', 'diner'],
    representative_sentence_word_choices = ARRAY['American', 'waiting', 'wait', 'diner', 'a', 'real', 'can''t', 'to', 'for', 'try', 'I', 'tried'],
    updated_at = now()
WHERE id = 10;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['Honestly', 'I''m', 'always', 'up', 'for', 'exploring', 'the', 'city'],
    representative_sentence_word_choices = ARRAY['explore', 'to', 'for', 'was', 'city', 'the', 'Honestly', 'always', 'up', 'exploring', 'I''m'],
    updated_at = now()
WHERE id = 11;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['Honestly', 'I''d', 'rather', 'just', 'rot', 'in', 'bed', 'all', 'day'],
    representative_sentence_word_choices = ARRAY['Honestly', 'rotting', 'day', 'bed', 'just', 'would', 'all', 'rather', 'rot', 'I''d', 'in', 'on'],
    updated_at = now()
WHERE id = 12;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['Hi', 'I''m', 'calling', 'about', 'my', 'AC', 'bill', 'for', 'July'],
    representative_sentence_word_choices = ARRAY['I''m', 'to', 'of', 'July', 'my', 'for', 'bill', 'Hi', 'call', 'calling', 'AC', 'about'],
    updated_at = now()
WHERE id = 13;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['Could', 'you', 'look', 'into', 'why', 'the', 'bill', 'is', 'so', 'high'],
    representative_sentence_word_choices = ARRAY['so', 'looking', 'high', 'the', 'bill', 'at', 'is', 'Could', 'why', 'you', 'into', 'look', 'much'],
    updated_at = now()
WHERE id = 14;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['Would', 'you', 'like', 'me', 'to', 'email', 'you', 'my', 'flight', 'tickets'],
    representative_sentence_word_choices = ARRAY['my', 'email', 'to', 'want', 'flight', 'sending', 'you', 'your', 'tickets', 'like', 'me', 'Would', 'you'],
    updated_at = now()
WHERE id = 15;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['I''m', 'afraid', 'I''d', 'prefer', 'a', 'cash', 'refund'],
    representative_sentence_word_choices = ARRAY['I''d', 'preferred', 'refund', 'I''m', 'would', 'afraid', 'cash', 'a', 'prefer', 'scared'],
    updated_at = now()
WHERE id = 16;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['I', 'just', 'want', 'us', 'to', 'be', 'on', 'the', 'same', 'page', 'about', 'guests'],
    representative_sentence_word_choices = ARRAY['them', 'guests', 'pages', 'at', 'be', 'just', 'about', 'on', 'same', 'the', 'I', 'page', 'want', 'to', 'us'],
    updated_at = now()
WHERE id = 17;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['Do', 'you', 'mind', 'giving', 'me', 'a', 'heads-up', 'first'],
    representative_sentence_word_choices = ARRAY['first', 'me', 'you', 'last', 'Do', 'a', 'give', 'giving', 'would', 'mind', 'heads-up'],
    updated_at = now()
WHERE id = 18;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['I''m', 'good', 'with', 'a', 'little', 'background', 'noise'],
    representative_sentence_word_choices = ARRAY['a', 'at', 'noises', 'with', 'noise', 'good', 'few', 'background', 'little', 'I''m'],
    updated_at = now()
WHERE id = 19;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['Feel', 'free', 'to', 'grab', 'my', 'milk', 'anytime'],
    representative_sentence_word_choices = ARRAY['feels', 'for', 'my', 'grab', 'grabbing', 'Feel', 'free', 'anytime', 'milk', 'to'],
    updated_at = now()
WHERE id = 20;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['Not', 'many', 'yet', 'but', 'I', 'hit', 'it', 'off', 'with', 'my', 'roommate'],
    representative_sentence_word_choices = ARRAY['much', 'Not', 'hits', 'roommate', 'yet', 'off', 'on', 'it', 'but', 'many', 'hit', 'my', 'with', 'I'],
    updated_at = now()
WHERE id = 21;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['Honestly', 'big', 'parties', 'aren''t', 'really', 'for', 'me'],
    representative_sentence_word_choices = ARRAY['big', 'isn''t', 'parties', 'to', 'for', 'aren''t', 'Honestly', 'mine', 'me', 'really'],
    updated_at = now()
WHERE id = 22;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['I', 'was', 'thinking', 'of', 'making', 'some', 'Korean', 'fried', 'chicken'],
    representative_sentence_word_choices = ARRAY['Korean', 'fried', 'think', 'I', 'making', 'of', 'some', 'made', 'thinking', 'was', 'chicken', 'to'],
    updated_at = now()
WHERE id = 23;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['You', 'can''t', 'go', 'wrong', 'with', 'Korean', 'fried', 'chicken'],
    representative_sentence_word_choices = ARRAY['went', 'You', 'with', 'Korean', 'wrong', 'fried', 'go', 'chicken', 'right', 'for', 'can''t'],
    updated_at = now()
WHERE id = 24;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['I', 'get', 'along', 'really', 'well', 'with', 'my', 'older', 'sister'],
    representative_sentence_word_choices = ARRAY['on', 'I', 'sister', 'my', 'gets', 'good', 'along', 'with', 'get', 'well', 'older', 'really'],
    updated_at = now()
WHERE id = 25;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['I''m', 'really', 'looking', 'forward', 'to', 'studying', 'abroad', 'someday'],
    representative_sentence_word_choices = ARRAY['I''m', 'look', 'looking', 'really', 'abroad', 'for', 'someday', 'studying', 'study', 'to', 'forward'],
    updated_at = now()
WHERE id = 26;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['I''ve', 'always', 'been', 'interested', 'in', 'psychology'],
    representative_sentence_word_choices = ARRAY['been', 'in', 'interesting', 'be', 'I''ve', 'always', 'on', 'interested', 'psychology'],
    updated_at = now()
WHERE id = 27;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['Yeah', 'I''ve', 'just', 'been', 'having', 'a', 'lot', 'of', 'off', 'days'],
    representative_sentence_word_choices = ARRAY['have', 'lot', 'off', 'I''ve', 'on', 'Yeah', 'a', 'just', 'lots', 'been', 'of', 'having', 'days'],
    updated_at = now()
WHERE id = 28;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['Thanks', 'It''s', 'just', 'hard', 'for', 'me', 'to', 'open', 'up', 'sometimes'],
    representative_sentence_word_choices = ARRAY['up', 'me', 'opens', 'for', 'down', 'sometimes', 'Thanks', 'at', 'open', 'hard', 'It''s', 'to', 'just'],
    updated_at = now()
WHERE id = 29;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['I''m', 'going', 'to', 'London', 'for', 'my', 'best', 'friend''s', 'wedding'],
    representative_sentence_word_choices = ARRAY['to', 'wedding', 'friend''s', 'for', 'London', 'best', 'friends', 'I''m', 'go', 'going', 'my', 'at'],
    updated_at = now()
WHERE id = 30;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['Big', 'cities', 'give', 'off', 'an', 'exciting', 'vibe', 'but', 'I', 'recharge', 'in', 'nature'],
    representative_sentence_word_choices = ARRAY['vibe', 'recharge', 'cities', 'excited', 'a', 'exciting', 'give', 'but', 'off', 'I', 'in', 'gives', 'an', 'nature', 'Big'],
    updated_at = now()
WHERE id = 31;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['There', 'was', 'something', 'magical', 'about', 'the', 'whole', 'city'],
    representative_sentence_word_choices = ARRAY['magic', 'anything', 'magical', 'the', 'was', 'is', 'There', 'whole', 'about', 'city', 'something'],
    updated_at = now()
WHERE id = 32;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['Iceland', 'I''d', 'never', 'seen', 'anything', 'like', 'the', 'northern', 'lights'],
    representative_sentence_word_choices = ARRAY['saw', 'never', 'I''d', 'northern', 'anything', 'ever', 'lights', 'something', 'Iceland', 'like', 'the', 'seen'],
    updated_at = now()
WHERE id = 33;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['My', 'suitcase', 'broke', 'during', 'the', 'flight', 'I''d', 'like', 'to', 'get', 'it', 'fixed', 'or', 'replaced'],
    representative_sentence_word_choices = ARRAY['suitcase', 'flight', 'get', 'or', 'I''d', 'broken', 'to', 'fixed', 'fix', 'broke', 'the', 'replaced', 'during', 'My', 'it', 'like', 'while'],
    updated_at = now()
WHERE id = 34;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['Is', 'there', 'any', 'compensation', 'available', 'by', 'any', 'chance'],
    representative_sentence_word_choices = ARRAY['available', 'there', 'chance', 'at', 'some', 'by', 'Is', 'chances', 'any', 'any', 'compensation'],
    updated_at = now()
WHERE id = 35;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['I''ll', 'take', 'the', 'mileage', 'points', 'they''re', 'more', 'useful', 'in', 'the', 'long', 'run'],
    representative_sentence_word_choices = ARRAY['run', 'I''ll', 'more', 'mileage', 'long', 'most', 'they''re', 'takes', 'short', 'the', 'useful', 'in', 'the', 'take', 'points'],
    updated_at = now()
WHERE id = 36;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['Hi', 'I''d', 'like', 'to', 'check', 'in', 'I', 'booked', 'online', 'under', 'Kim'],
    representative_sentence_word_choices = ARRAY['I', 'out', 'under', 'to', 'checked', 'online', 'in', 'booked', 'I''d', 'check', 'Kim', 'book', 'like', 'Hi'],
    updated_at = now()
WHERE id = 37;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['I', 'could', 'use', 'a', 'quiet', 'room', 'it', 'was', 'a', 'long', 'flight'],
    representative_sentence_word_choices = ARRAY['a', 'I', 'it', 'is', 'quietly', 'was', 'a', 'quiet', 'room', 'flight', 'could', 'use', 'can', 'long'],
    updated_at = now()
WHERE id = 38;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['Yes', 'what', 'are', 'the', 'hours', 'for', 'breakfast'],
    representative_sentence_word_choices = ARRAY['Yes', 'at', 'the', 'are', 'what', 'is', 'breakfast', 'for', 'hours', 'hour'],
    updated_at = now()
WHERE id = 39;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['Is', 'the', 'subway', 'station', 'a', 'five-minute', 'walk', 'from', 'here'],
    representative_sentence_word_choices = ARRAY['walks', 'subway', 'from', 'Is', 'five-minute', 'station', 'minutes', 'a', 'to', 'walk', 'here', 'the'],
    updated_at = now()
WHERE id = 40;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['Sorry', 'I', 'don''t', 'think', 'I', 'can', 'make', 'it', 'tonight'],
    representative_sentence_word_choices = ARRAY['don''t', 'I', 'tonight', 'make', 'can', 'this', 'it', 'Sorry', 'I', 'makes', 'think', 'won''t'],
    updated_at = now()
WHERE id = 41;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['I''m', 'flattered', 'but', 'I', 'don''t', 'feel', 'the', 'same', 'way'],
    representative_sentence_word_choices = ARRAY['way', 'I', 'I''m', 'same', 'feels', 'flattered', 'so', 'but', 'feel', 'don''t', 'the', 'ways'],
    updated_at = now()
WHERE id = 42;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['It', 'has', 'nothing', 'to', 'do', 'with', 'you', 'I''m', 'just', 'not', 'looking', 'to', 'date', 'right', 'now'],
    representative_sentence_word_choices = ARRAY['right', 'has', 'not', 'to', 'to', 'something', 'just', 'I''m', 'doing', 'date', 'do', 'you', 'now', 'look', 'with', 'It', 'nothing', 'looking'],
    updated_at = now()
WHERE id = 43;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['No', 'offense', 'but', 'I''d', 'rather', 'not', 'share', 'my', 'socials'],
    representative_sentence_word_choices = ARRAY['offense', 'sharing', 'rather', 'offensive', 'share', 'my', 'not', 'socials', 'would', 'but', 'No', 'I''d'],
    updated_at = now()
WHERE id = 44;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['I', 'feel', 'like', 'something', 'sweet', 'today'],
    representative_sentence_word_choices = ARRAY['sweet', 'today', 'sweets', 'anything', 'feels', 'I', 'something', 'feel', 'like'],
    updated_at = now()
WHERE id = 45;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['Can', 'I', 'get', 'that', 'to', 'go', 'please'],
    representative_sentence_word_choices = ARRAY['get', 'that', 'please', 'gets', 'here', 'for', 'to', 'go', 'Can', 'I'],
    updated_at = now()
WHERE id = 46;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['Actually', 'does', 'it', 'come', 'in', 'decaf'],
    representative_sentence_word_choices = ARRAY['Actually', 'in', 'come', 'decaf', 'comes', 'does', 'it', 'at', 'do'],
    updated_at = now()
WHERE id = 47;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['Sure', 'that', 'sounds', 'great'],
    representative_sentence_word_choices = ARRAY['greatly', 'this', 'great', 'Sure', 'that', 'sound', 'sounds'],
    updated_at = now()
WHERE id = 48;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['I''ve', 'been', 'feeling', 'under', 'the', 'weather', 'since', 'yesterday'],
    representative_sentence_word_choices = ARRAY['over', 'the', 'under', 'weather', 'feeling', 'I''ve', 'yesterday', 'for', 'been', 'felt', 'since'],
    updated_at = now()
WHERE id = 49;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['A', 'few', 'days', 'now', 'I', 'just', 'can''t', 'get', 'over', 'this', 'headache'],
    representative_sentence_word_choices = ARRAY['A', 'now', 'won''t', 'this', 'just', 'days', 'I', 'over', 'under', 'can''t', 'get', 'few', 'got', 'headache'],
    updated_at = now()
WHERE id = 50;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['No', 'fever', 'I', 'think', 'it''s', 'from', 'walking', 'around', 'all', 'day', 'in', 'the', 'sun'],
    representative_sentence_word_choices = ARRAY['No', 'it''s', 'walking', 'think', 'from', 'in', 'the', 'all', 'I', 'by', 'fever', 'sun', 'walk', 'around', 'day', 'of'],
    updated_at = now()
WHERE id = 51;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['As', 'far', 'as', 'I', 'know', 'I''m', 'not', 'allergic', 'to', 'anything'],
    representative_sentence_word_choices = ARRAY['long', 'not', 'I', 'allergic', 'know', 'anything', 'to', 'As', 'far', 'as', 'I''m', 'knew', 'something'],
    updated_at = now()
WHERE id = 52;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['Yes', 'please', 'I''m', 'not', 'familiar', 'with', 'this', 'area', 'Which', 'exit', 'is', 'closest', 'to', 'Tower', 'Bridge'],
    representative_sentence_word_choices = ARRAY['Which', 'near', 'Yes', 'is', 'Bridge', 'at', 'please', 'exit', 'this', 'with', 'to', 'familiar', 'Tower', 'I''m', 'closest', 'not', 'area', 'knows'],
    updated_at = now()
WHERE id = 53;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['It''s', 'been', 'amazing', 'for', 'the', 'most', 'part', 'except', 'for', 'the', 'weather'],
    representative_sentence_word_choices = ARRAY['the', 'except', 'mostly', 'for', 'amazing', 'besides', 'for', 'most', 'It''s', 'part', 'parts', 'weather', 'the', 'been'],
    updated_at = now()
WHERE id = 54;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['Something', 'warm', 'like', 'ramen', 'would', 'really', 'hit', 'the', 'spot', 'right', 'now'],
    representative_sentence_word_choices = ARRAY['warm', 'spot', 'hits', 'spots', 'ramen', 'warmly', 'now', 'the', 'would', 'hit', 'like', 'Something', 'right', 'really'],
    updated_at = now()
WHERE id = 55;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['It''s', 'so', 'nice', 'of', 'you', 'to', 'offer'],
    representative_sentence_word_choices = ARRAY['so', 'for', 'offer', 'nicely', 'you', 'offering', 'It''s', 'nice', 'of', 'to'],
    updated_at = now()
WHERE id = 56;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['Kyoto', 'it', 'totally', 'lived', 'up', 'to', 'the', 'hype'],
    representative_sentence_word_choices = ARRAY['up', 'hype', 'to', 'lived', 'Kyoto', 'live', 'totally', 'it', 'down', 'the', 'hyped'],
    updated_at = now()
WHERE id = 57;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['Unlike', 'other', 'big', 'cities', 'it', 'felt', 'calm', 'and', 'personal'],
    representative_sentence_word_choices = ARRAY['feels', 'big', 'it', 'Like', 'felt', 'cities', 'Unlike', 'other', 'and', 'calm', 'calmly', 'personal'],
    updated_at = now()
WHERE id = 58;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['Long', 'story', 'short', 'we', 'missed', 'our', 'flight', 'and', 'slept', 'in', 'the', 'airport'],
    representative_sentence_word_choices = ARRAY['missing', 'short', 'sleep', 'slept', 'flight', 'Long', 'the', 'we', 'story', 'airport', 'missed', 'our', 'at', 'in', 'and'],
    updated_at = now()
WHERE id = 59;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['We', 'got', 'stuck', 'in', 'traffic', 'on', 'the', 'way', 'to', 'the', 'airport'],
    representative_sentence_word_choices = ARRAY['road', 'got', 'to', 'airport', 'the', 'stuck', 'We', 'in', 'get', 'on', 'way', 'at', 'the', 'traffic'],
    updated_at = now()
WHERE id = 60;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['I''d', 'take', 'a', 'whole', 'year', 'off', 'and', 'travel', 'the', 'world'],
    representative_sentence_word_choices = ARRAY['travels', 'I''d', 'a', 'off', 'year', 'whole', 'world', 'take', 'took', 'on', 'the', 'and', 'travel'],
    updated_at = now()
WHERE id = 61;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['We''re', 'in', 'the', 'same', 'boat', 'I', 'don''t', 'know', 'anyone', 'here', 'either'],
    representative_sentence_word_choices = ARRAY['someone', 'same', 'don''t', 'anyone', 'here', 'I', 'know', 'in', 'the', 'boat', 'boats', 'either', 'We''re', 'too'],
    updated_at = now()
WHERE id = 62;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['Don''t', 'worry', 'we''ll', 'figure', 'it', 'out', 'together'],
    representative_sentence_word_choices = ARRAY['out', 'it', 'worried', 'figure', 'Don''t', 'figures', 'worry', 'we''ll', 'together', 'them'],
    updated_at = now()
WHERE id = 63;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['I', 'use', 'a', 'planner', 'to', 'keep', 'track', 'of', 'all', 'the', 'deadlines'],
    representative_sentence_word_choices = ARRAY['deadlines', 'tracks', 'to', 'track', 'all', 'the', 'keeps', 'a', 'used', 'use', 'of', 'keep', 'I', 'planner'],
    updated_at = now()
WHERE id = 64;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['My', 'history', 'teacher', 'did', 'a', 'good', 'job', 'of', 'making', 'class', 'fun'],
    representative_sentence_word_choices = ARRAY['work', 'history', 'did', 'good', 'does', 'class', 'teacher', 'job', 'make', 'a', 'making', 'fun', 'My', 'of'],
    updated_at = now()
WHERE id = 65;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['Once', 'I', 'didn''t', 'prepare', 'enough', 'so', 'I', 'just', 'winged', 'it'],
    representative_sentence_word_choices = ARRAY['don''t', 'winged', 'just', 'it', 'wing', 'Once', 'enough', 'didn''t', 'prepare', 'I', 'so', 'I', 'prepared'],
    updated_at = now()
WHERE id = 66;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['I''m', 'not', 'cut', 'out', 'for', 'presenting', 'so', 'I''ll', 'take', 'the', 'research'],
    representative_sentence_word_choices = ARRAY['take', 'so', 'not', 'cut', 'I''m', 'out', 'research', 'for', 'I''ll', 'presenting', 'in', 'cuts', 'present', 'the'],
    updated_at = now()
WHERE id = 67;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['Don''t', 'worry', 'I', 'can', 'handle', 'the', 'slides', 'too'],
    representative_sentence_word_choices = ARRAY['can', 'could', 'handles', 'I', 'either', 'handle', 'Don''t', 'worry', 'too', 'the', 'slides'],
    updated_at = now()
WHERE id = 68;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['I''m', 'always', 'scared', 'I''ll', 'mess', 'up', 'my', 'lines'],
    representative_sentence_word_choices = ARRAY['mess', 'down', 'I''m', 'messed', 'scared', 'my', 'up', 'line', 'I''ll', 'always', 'lines'],
    updated_at = now()
WHERE id = 69;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['Professor', 'have', 'you', 'got', 'a', 'minute', 'It''s', 'about', 'my', 'last', 'assignment'],
    representative_sentence_word_choices = ARRAY['assignment', 'got', 'minute', 'gets', 'have', 'Professor', 'about', 'my', 'you', 'It''s', 'last', 'at', 'minutes', 'a'],
    updated_at = now()
WHERE id = 70;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['I', 'might', 'be', 'wrong', 'but', 'I', 'thought', 'I', 'covered', 'all', 'the', 'requirements'],
    representative_sentence_word_choices = ARRAY['I', 'but', 'all', 'cover', 'the', 'think', 'be', 'right', 'I', 'covered', 'I', 'might', 'thought', 'wrong', 'requirements'],
    updated_at = now()
WHERE id = 71;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['Honestly', 'the', 'late', 'submission', 'part', 'rings', 'a', 'bell'],
    representative_sentence_word_choices = ARRAY['bells', 'bell', 'submission', 'Honestly', 'part', 'ring', 'early', 'the', 'late', 'a', 'rings'],
    updated_at = now()
WHERE id = 72;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['How', 'can', 'I', 'get', 'better', 'at', 'developing', 'my', 'arguments'],
    representative_sentence_word_choices = ARRAY['developing', 'at', 'better', 'develop', 'arguments', 'can', 'get', 'good', 'How', 'my', 'gets', 'I'],
    updated_at = now()
WHERE id = 73;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['I''ll', 'stop', 'by', 'your', 'office', 'tomorrow', 'after', 'class'],
    representative_sentence_word_choices = ARRAY['I''ll', 'class', 'by', 'stop', 'after', 'tomorrow', 'at', 'before', 'your', 'stops', 'office'],
    updated_at = now()
WHERE id = 74;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['Not', 'even', 'close', 'If', 'I', 'don''t', 'start', 'today', 'I''m', 'cooked'],
    representative_sentence_word_choices = ARRAY['Not', 'If', 'close', 'started', 'I', 'today', 'don''t', 'I''m', 'won''t', 'even', 'cook', 'start', 'cooked'],
    updated_at = now()
WHERE id = 75;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['Honestly', 'I', 'don''t', 'start', 'studying', 'until', 'the', 'night', 'before'],
    representative_sentence_word_choices = ARRAY['night', 'I', 'the', 'Honestly', 'before', 'don''t', 'after', 'study', 'start', 'starts', 'until', 'studying'],
    updated_at = now()
WHERE id = 76;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['I', 'didn''t', 'want', 'to', 'let', 'my', 'parents', 'down', 'so', 'I', 'told', 'them', 'first'],
    representative_sentence_word_choices = ARRAY['tell', 'lets', 'to', 'my', 'I', 'didn''t', 'I', 'up', 'them', 'down', 'let', 'so', 'first', 'parents', 'want', 'told'],
    updated_at = now()
WHERE id = 77;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['Sure', 'How', 'does', 'Saturday', 'at', 'the', 'library', 'sound'],
    representative_sentence_word_choices = ARRAY['do', 'library', 'sound', 'How', 'Sure', 'at', 'does', 'sounds', 'in', 'Saturday', 'the'],
    updated_at = now()
WHERE id = 78;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['It''s', 'not', 'about', 'the', 'money', 'itself', 'It''s', 'about', 'the', 'freedom', 'it', 'gives', 'you'],
    representative_sentence_word_choices = ARRAY['at', 'freedom', 'It''s', 'about', 'itself', 'myself', 'the', 'the', 'about', 'not', 'It''s', 'give', 'it', 'money', 'you', 'gives'],
    updated_at = now()
WHERE id = 79;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['Having', 'more', 'doesn''t', 'necessarily', 'mean', 'being', 'happier'],
    representative_sentence_word_choices = ARRAY['Having', 'necessarily', 'means', 'being', 'more', 'happier', 'necessary', 'mean', 'doesn''t', 'happy'],
    updated_at = now()
WHERE id = 80;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['It', 'depends', 'on', 'how', 'you', 'spend', 'it'],
    representative_sentence_word_choices = ARRAY['at', 'spends', 'spend', 'you', 'on', 'how', 'it', 'depend', 'It', 'depends'],
    updated_at = now()
WHERE id = 81;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['It', 'did', 'experiences', 'make', 'a', 'bigger', 'difference', 'than', 'things'],
    representative_sentence_word_choices = ARRAY['makes', 'make', 'difference', 'It', 'did', 'a', 'than', 'then', 'experiences', 'things', 'big', 'bigger'],
    updated_at = now()
WHERE id = 82;

UPDATE writing_expression
SET representative_sentence_words = ARRAY['Honestly', 'I', 'think', 'I''d', 'just', 'get', 'used', 'to', 'it'],
    representative_sentence_word_choices = ARRAY['to', 'think', 'I', 'just', 'gets', 'using', 'I''d', 'use', 'it', 'used', 'get', 'Honestly'],
    updated_at = now()
WHERE id = 83;


ALTER TABLE writing_expression ALTER COLUMN representative_sentence_words SET NOT NULL;
ALTER TABLE writing_expression ALTER COLUMN representative_sentence_word_choices SET NOT NULL;
