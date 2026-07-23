// 컨트롤러에서 발생한 예외를 공통 API 오류 응답으로 변환한다.

package com.landit.landitbe.shared.exception;

import com.landit.landitbe.shared.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** 컨트롤러에서 발생한 예외를 공통 API 오류 응답으로 변환한다. */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /** 애플리케이션에서 명시적으로 던진 API 예외를 오류 응답으로 변환한다. */
  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException exception) {
    if (exception.getStatus().is5xxServerError()) {
      log.error("서버 API 예외를 처리했습니다. code={}", exception.getErrorCode(), exception);
    }
    return ResponseEntity.status(exception.getStatus())
        .body(ApiResponse.error(exception.getErrorCode(), exception.getMessage()));
  }

  /** 요청 본문 Bean Validation 실패를 공통 검증 오류로 변환한다. */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(
      MethodArgumentNotValidException exception) {
    return error(ErrorCode.VALIDATION_FAILED);
  }

  /** 요청 파라미터 Bean Validation 실패를 공통 검증 오류로 변환한다. */
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
      ConstraintViolationException exception) {
    return error(ErrorCode.VALIDATION_FAILED);
  }

  /** 잘못된 요청 본문이나 필수 파라미터 누락을 공통 검증 오류로 변환한다. */
  @ExceptionHandler({
    MissingServletRequestParameterException.class,
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class
  })
  public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception exception) {
    return error(ErrorCode.VALIDATION_FAILED);
  }

  /** Spring Security 접근 거부를 공통 권한 오류로 변환한다. */
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException exception) {
    return error(ErrorCode.FORBIDDEN);
  }

  /** 존재하지 않는 정적 리소스 요청을 공통 404 오류로 변환한다. */
  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(
      NoResourceFoundException exception) {
    return error(ErrorCode.RESOURCE_NOT_FOUND);
  }

  /** 예상하지 못한 예외를 오류 로그로 기록하고 서버 오류로 변환한다. */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
    log.error("예상하지 못한 서버 예외를 처리했습니다.", exception);
    return error(ErrorCode.INTERNAL_SERVER_ERROR);
  }

  private ResponseEntity<ApiResponse<Void>> error(ErrorCode errorCode) {
    return ResponseEntity.status(errorCode.getStatus()).body(ApiResponse.error(errorCode));
  }
}
