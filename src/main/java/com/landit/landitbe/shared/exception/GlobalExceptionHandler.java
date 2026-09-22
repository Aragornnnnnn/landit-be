// 컨트롤러에서 발생한 예외를 공통 API 오류 응답으로 변환한다.

package com.landit.landitbe.shared.exception;

import com.landit.landitbe.shared.observability.FailureObservation;
import com.landit.landitbe.shared.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** 컨트롤러에서 발생한 예외를 공통 API 오류 응답으로 변환한다. */
@RestControllerAdvice
public class GlobalExceptionHandler {

  /**
   * 애플리케이션에서 명시적으로 던진 API 예외를 오류 응답으로 변환한다.
   *
   * @param exception API 처리 예외
   * @return 오류 코드에 해당하는 API 응답
   */
  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException exception) {
    if (exception.getStatus().is5xxServerError() || exception.getCause() != null) {
      FailureObservation.failed("api", "execution", exception.getErrorCode().name(), exception);
    } else if (exception.getErrorCode() == ErrorCode.INVALID_REQUEST
        || exception.getErrorCode() == ErrorCode.VALIDATION_FAILED) {
      observeRequest(exception);
    } else {
      FailureObservation.observed(
          "api", "request", exception.getErrorCode().name(), "expected_rejection");
    }
    return ResponseEntity.status(exception.getStatus())
        .body(ApiResponse.error(exception.getErrorCode(), exception.getMessage()));
  }

  /**
   * 기능 모듈에서 의도한 예외를 해당 오류 코드와 HTTP 상태로 변환한다.
   *
   * @param exception 기능 모듈 예외
   * @return 기능별 오류 응답
   */
  @ExceptionHandler(FeatureException.class)
  public ResponseEntity<ApiResponse<Void>> handleFeatureException(FeatureException exception) {
    if (exception.getStatus().is5xxServerError() || exception.getCause() != null) {
      FailureObservation.failed("api", "execution", exception.getCode(), exception);
    } else {
      FailureObservation.observed("api", "request", exception.getCode(), "expected_rejection");
    }
    return ResponseEntity.status(exception.getStatus())
        .body(ApiResponse.error(exception.getCode(), exception.getMessage()));
  }

  /**
   * 메서드 인자 검증 실패를 요청 오류로 변환한다. 반환값 위반은 서버 오류다.
   *
   * @param exception 메서드 검증 실패
   * @return 기존 요청 오류 형식
   */
  @ExceptionHandler(HandlerMethodValidationException.class)
  public ResponseEntity<ApiResponse<Void>> handleMethodValidation(
      HandlerMethodValidationException exception) {
    if (exception.isForReturnValue()) {
      return handleUnexpectedException(exception);
    }
    observeRequest(exception);
    return error(ErrorCode.INVALID_REQUEST);
  }

  /**
   * 요청 본문 Bean Validation 실패를 공통 검증 오류로 변환한다.
   *
   * @param exception 본문 검증 예외
   * @return 검증 오류 응답
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(
      MethodArgumentNotValidException exception) {
    observeRequest(exception);
    return error(ErrorCode.VALIDATION_FAILED);
  }

  /**
   * 요청 파라미터 Bean Validation 실패를 공통 검증 오류로 변환한다.
   *
   * @param exception 파라미터 검증 예외
   * @return 검증 오류 응답
   */
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
      ConstraintViolationException exception) {
    observeRequest(exception);
    return error(ErrorCode.VALIDATION_FAILED);
  }

  /**
   * 잘못된 요청 본문이나 필수 파라미터 누락을 공통 검증 오류로 변환한다.
   *
   * @param exception 요청 형식 예외
   * @return 검증 오류 응답
   */
  @ExceptionHandler({
    MissingServletRequestParameterException.class,
    org.springframework.web.bind.MissingRequestHeaderException.class,
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class,
    MultipartException.class
  })
  public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception exception) {
    observeRequest(exception);
    return error(ErrorCode.VALIDATION_FAILED);
  }

  /**
   * Spring Security 접근 거부를 공통 권한 오류로 변환한다.
   *
   * @param exception 접근 거부 예외
   * @return 권한 오류 응답
   */
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException exception) {
    FailureObservation.observed("api", "authorization", "access_denied", "expected_rejection");
    return error(ErrorCode.FORBIDDEN);
  }

  /**
   * 존재하지 않는 정적 리소스 요청을 공통 404 오류로 변환한다.
   *
   * @param exception 없는 리소스 요청 예외
   * @return 404 오류 응답
   */
  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(
      NoResourceFoundException exception) {
    observeRequest(exception);
    return error(ErrorCode.RESOURCE_NOT_FOUND);
  }

  /**
   * 예상하지 못한 예외를 오류 로그로 기록하고 서버 오류로 변환한다.
   *
   * @param exception 미처리 예외
   * @return 서버 오류 응답
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
    FailureObservation.failed("api", "execution", "unexpected_exception", exception);
    return error(ErrorCode.INTERNAL_SERVER_ERROR);
  }

  /**
   * 알려진 Spring 요청 오류의 HTTP 상태와 Allow·Accept 헤더를 보존한다.
   *
   * @param exception 프레임워크 요청 예외
   * @return 공통 오류 본문과 원래 HTTP 상태·헤더
   */
  @ExceptionHandler({
    org.springframework.web.HttpRequestMethodNotSupportedException.class,
    org.springframework.web.HttpMediaTypeNotSupportedException.class,
    org.springframework.web.HttpMediaTypeNotAcceptableException.class,
    org.springframework.web.bind.MissingPathVariableException.class
  })
  public ResponseEntity<ApiResponse<Void>> handleHttpContract(Exception exception) {
    org.springframework.web.ErrorResponse response =
        (org.springframework.web.ErrorResponse) exception;
    if (response.getStatusCode().is5xxServerError()) {
      FailureObservation.failed("api", "request_mapping", "server_contract", exception);
    } else {
      observeRequest(exception);
    }
    return ResponseEntity.status(response.getStatusCode())
        .headers(response.getHeaders())
        .body(
            ApiResponse.error(
                response.getStatusCode().is5xxServerError()
                    ? ErrorCode.INTERNAL_SERVER_ERROR
                    : ErrorCode.INVALID_REQUEST));
  }

  private void observeRequest(Exception exception) {
    var authentication =
        org.springframework.security.core.context.SecurityContextHolder.getContext()
            .getAuthentication();
    boolean trusted =
        authentication != null
            && authentication.isAuthenticated()
            && !(authentication
                instanceof
                org.springframework.security.authentication.AnonymousAuthenticationToken);
    boolean internal =
        exception instanceof ConstraintViolationException violation
            && violation.getConstraintViolations().stream()
                .anyMatch(
                    value -> {
                      for (var node : value.getPropertyPath()) {
                        if (node.getKind() == jakarta.validation.ElementKind.METHOD
                            || node.getKind() == jakarta.validation.ElementKind.RETURN_VALUE) {
                          return true;
                        }
                      }
                      return false;
                    });
    if (trusted || internal) {
      FailureObservation.failed("api", "request_validation", "application_contract", exception);
    } else {
      FailureObservation.observed(
          "api", "request_validation", "invalid_external_request", "expected_rejection");
    }
  }

  private ResponseEntity<ApiResponse<Void>> error(ErrorCode errorCode) {
    return ResponseEntity.status(errorCode.getStatus()).body(ApiResponse.error(errorCode));
  }
}
