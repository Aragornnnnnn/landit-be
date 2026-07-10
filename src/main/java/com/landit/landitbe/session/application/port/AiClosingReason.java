// AI 종료 메시지 생성 요청의 종료 사유를 정의한다.
package com.landit.landitbe.session.application.port;

public enum AiClosingReason {
    GOAL_COMPLETED,
    MAX_TURNS_REACHED,
    USER_ENDED,
    TIME_LIMIT_REACHED
}
