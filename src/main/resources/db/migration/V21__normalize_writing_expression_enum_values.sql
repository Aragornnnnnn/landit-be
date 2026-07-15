-- writing_expression의 expression_type / usage_frequency_level이 enum 상수명이 아니라
-- 한글 라벨로 저장돼 @Enumerated(STRING) 매핑 실패(엔티티 로드 시 예외 → 500)를 일으키는 문제를 교정한다.
-- V13의 locale 소문자 정규화와 같은 부류. 테스트(H2)는 행이 없어 0행 UPDATE로 무해하게 통과한다.

-- expression_type: 한글 분류 라벨 → WritingExpressionType 상수명
UPDATE writing_expression SET expression_type = 'DAILY_ROUTINE' WHERE expression_type = '일상·루틴';
UPDATE writing_expression SET expression_type = 'EMOTION_EMPATHY' WHERE expression_type = '감정·공감';
UPDATE writing_expression SET expression_type = 'RELATIONSHIP_SOCIAL' WHERE expression_type = '관계·사교';
UPDATE writing_expression SET expression_type = 'CONVERSATION_SKILL' WHERE expression_type = '대화 기술';
UPDATE writing_expression SET expression_type = 'POLITE_EXPRESSION' WHERE expression_type = '완곡한 표현';
UPDATE writing_expression SET expression_type = 'MONEY_SPENDING' WHERE expression_type = '돈·소비';
UPDATE writing_expression SET expression_type = 'TIME_PLANNING' WHERE expression_type = '시간·계획';
UPDATE writing_expression SET expression_type = 'WORK_STUDY' WHERE expression_type = '일·공부';
UPDATE writing_expression SET expression_type = 'TRAVEL_MOVEMENT' WHERE expression_type = '이동·여행';
UPDATE writing_expression SET expression_type = 'OPINION_JUDGMENT' WHERE expression_type = '의견·판단';
UPDATE writing_expression SET expression_type = 'GRAMMAR_FUNCTION_WORD' WHERE expression_type = '문법·기능어';

-- usage_frequency_level: 한글 어감 라벨 → ExpressionUsageFrequencyLevel 상수명
UPDATE writing_expression SET usage_frequency_level = 'BASIC' WHERE usage_frequency_level = '기본';
UPDATE writing_expression SET usage_frequency_level = 'SLANG_NEOLOGISM' WHERE usage_frequency_level = '슬랭·신조어';
UPDATE writing_expression SET usage_frequency_level = 'CLASSIC_COMMON' WHERE usage_frequency_level = '클래식·통용';
