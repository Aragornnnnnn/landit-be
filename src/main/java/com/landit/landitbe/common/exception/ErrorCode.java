// 공통 예외가 사용할 애플리케이션 오류 코드와 HTTP 상태를 정의한다.
package com.landit.landitbe.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    ACCESS_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "인증 토큰이 만료됐습니다."),
    REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "재발급 토큰이 올바르지 않습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    CATEGORY_LOCKED(HttpStatus.FORBIDDEN, "잠긴 카테고리입니다."),
    SCENARIO_LOCKED(HttpStatus.FORBIDDEN, "잠긴 시나리오입니다."),
    EXPRESSION_LOCKED(HttpStatus.FORBIDDEN, "잠긴 표현입니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    SCENARIO_NOT_FOUND(HttpStatus.NOT_FOUND, "시나리오를 찾을 수 없습니다."),
    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "세션을 찾을 수 없습니다."),
    CONFLICT(HttpStatus.CONFLICT, "요청이 현재 상태와 충돌합니다."),
    SESSION_ALREADY_COMPLETED(HttpStatus.CONFLICT, "이미 완료된 세션입니다."),
    SESSION_NOT_COMPLETED(HttpStatus.CONFLICT, "완료되지 않은 세션입니다."),
    FEEDBACK_NOT_READY(HttpStatus.CONFLICT, "메시지별 피드백이 아직 준비되지 않았습니다."),
    AI_RESPONSE_INVALID(HttpStatus.BAD_GATEWAY, "AI 응답 형식이 올바르지 않습니다."),
    AI_GENERATION_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "AI 응답 생성에 실패했습니다."),
    FEEDBACK_GENERATION_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "최종 피드백 생성에 실패했습니다."),
    UNSUPPORTED_SOCIAL_PROVIDER(HttpStatus.BAD_REQUEST, "지원하지 않는 소셜 로그인 제공자입니다."),
    OIDC_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "소셜 로그인 토큰이 올바르지 않습니다."),
    OIDC_NONCE_MISMATCH(HttpStatus.BAD_REQUEST, "소셜 로그인 요청 검증 값이 일치하지 않습니다."),
    OIDC_PROVIDER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "소셜 로그인 제공자 검증에 실패했습니다."),
    APP_VERSION_POLICY_NOT_CONFIGURED(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "앱 버전 정책이 올바르게 설정되지 않았습니다."
    ),
    DEFAULT_AI_TUTOR_NOT_CONFIGURED(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "기본 AI 튜터가 올바르게 설정되지 않았습니다."
    ),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    /** 오류 코드에 대응하는 HTTP 상태를 반환한다. */
    public HttpStatus getStatus() {
        return status;
    }

    /** 클라이언트에 노출할 기본 오류 메시지를 반환한다. */
    public String getMessage() {
        return message;
    }
}
