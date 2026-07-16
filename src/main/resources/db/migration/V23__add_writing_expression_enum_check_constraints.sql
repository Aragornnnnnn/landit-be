-- 표현 타입·사용 빈도 컬럼에 enum 상수명만 허용하는 CHECK 제약을 추가한다.
-- 시딩 파이프라인이 한글 라벨 등 enum에 없는 값을 넣으면(@Enumerated(STRING) 매핑 실패로 조회 API 500)
-- 조회 시점이 아니라 INSERT 시점에 바로 거부되도록 막는다. (V21 데이터 정규화의 재발 방지)

ALTER TABLE writing_expression
    ADD CONSTRAINT chk_writing_expression_type CHECK (expression_type IN (
        'DAILY_ROUTINE',
        'EMOTION_EMPATHY',
        'RELATIONSHIP_SOCIAL',
        'CONVERSATION_SKILL',
        'POLITE_EXPRESSION',
        'MONEY_SPENDING',
        'TIME_PLANNING',
        'WORK_STUDY',
        'TRAVEL_MOVEMENT',
        'OPINION_JUDGMENT',
        'GRAMMAR_FUNCTION_WORD'
    ));

ALTER TABLE writing_expression
    ADD CONSTRAINT chk_writing_expression_usage_frequency_level CHECK (usage_frequency_level IN (
        'CLASSIC_COMMON',
        'BASIC',
        'SLANG_NEOLOGISM'
    ));
