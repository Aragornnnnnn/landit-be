-- 클라이언트가 예약하는 사용자별 일일 알람 설정을 저장한다.

CREATE TABLE user_alarm (
    user_profile_id BIGINT PRIMARY KEY,
    alarm_time TIME(0) NOT NULL,
    enabled BOOLEAN NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_user_alarm_user_profile_id
        FOREIGN KEY (user_profile_id) REFERENCES user_profile (id)
);
