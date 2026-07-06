// 서비스 로직에서 공통 오류 코드로 실패를 표현하는 런타임 예외다.
package com.landit.landitbe.common.exception;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;

    /** 오류 코드의 기본 메시지를 사용하는 API 예외를 생성한다. */
    public ApiException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /** 오류 코드와 별도 메시지를 사용하는 API 예외를 생성한다. */
    public ApiException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /** 예외에 대응하는 오류 코드를 반환한다. */
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /** 예외에 대응하는 HTTP 상태를 반환한다. */
    public HttpStatus getStatus() {
        return errorCode.getStatus();
    }
}
