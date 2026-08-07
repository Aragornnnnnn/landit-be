-- 프리톡 사용자 발화의 최초 처리 결과 상태를 저장한다.

ALTER TABLE session_history_message
    ADD COLUMN free_talk_turn_status VARCHAR(40);
