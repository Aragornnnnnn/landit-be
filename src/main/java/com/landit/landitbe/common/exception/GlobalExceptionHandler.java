// 컨트롤러에서 발생한 예외를 공통 API 오류 응답으로 변환한다.
package com.landit.landitbe.common.exception;

import com.landit.landitbe.common.observability.SentryEventReporter;
import com.landit.landitbe.common.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final SentryEventReporter sentryEventReporter;

    public GlobalExceptionHandler(SentryEventReporter sentryEventReporter) {
        this.sentryEventReporter = sentryEventReporter;
    }

    /** 애플리케이션에서 명시적으로 던진 API 예외를 오류 응답으로 변환한다. */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException exception) {
        if (exception.getStatus().is5xxServerError()) {
            sentryEventReporter.captureException(exception);
        }
        return ResponseEntity.status(exception.getStatus())
                .body(ApiResponse.error(exception.getErrorCode(), exception.getMessage()));
    }

    /** 요청 본문 Bean Validation 실패를 공통 검증 오류로 변환한다. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        return error(ErrorCode.VALIDATION_FAILED);
    }

    /** 요청 파라미터 Bean Validation 실패를 공통 검증 오류로 변환한다. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException exception) {
        return error(ErrorCode.VALIDATION_FAILED);
    }

    /** 잘못된 요청 본문이나 필수 파라미터 누락을 공통 검증 오류로 변환한다. */
    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception exception) {
        return error(ErrorCode.VALIDATION_FAILED);
    }

    /** Spring Security 접근 거부를 공통 권한 오류로 변환한다. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException exception) {
        return error(ErrorCode.FORBIDDEN);
    }

    /** 예상하지 못한 예외를 Sentry에 전송하고 서버 오류로 변환한다. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
        sentryEventReporter.captureException(exception);
        return error(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ApiResponse<Void>> error(ErrorCode errorCode) {
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode));
    }
}
