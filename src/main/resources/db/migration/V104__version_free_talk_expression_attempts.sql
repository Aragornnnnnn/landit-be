-- 새 인스턴스의 재시도 뒤 도착한 이전 표현 생성 결과를 구분한다.
ALTER TABLE free_talk_session ADD COLUMN expression_generation_attempt INTEGER NOT NULL DEFAULT 0;
