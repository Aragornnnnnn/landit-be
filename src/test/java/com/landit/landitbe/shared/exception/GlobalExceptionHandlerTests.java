// 공통 예외 핸들러의 오류 응답 매핑을 검증한다.

package com.landit.landitbe.shared.exception;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.landit.landitbe.feature.profile.exception.UserProfileErrorCode;
import com.landit.landitbe.feature.profile.exception.UserProfileException;
import com.landit.landitbe.feature.session.exception.SessionErrorCode;
import com.landit.landitbe.feature.session.exception.SessionException;
import com.landit.landitbe.shared.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** 공통 예외 핸들러의 오류 응답 매핑을 검증한다. */
class GlobalExceptionHandlerTests {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
  private final Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
  private final ListAppender<ILoggingEvent> logAppender = new ListAppender<>();

  @BeforeEach
  void attachLogAppender() {
    logAppender.start();
    logger.addAppender(logAppender);
  }

  @AfterEach
  void detachLogAppender() {
    logger.detachAppender(logAppender);
    logAppender.stop();
  }

  @Test
  void clientApiExceptionUsesErrorCodeStatusAndMessageWithoutErrorLog() {
    ApiException exception = new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "리소스가 없습니다.");

    ResponseEntity<ApiResponse<Void>> response = handler.handleApiException(exception);

    assertError(response, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "리소스가 없습니다.");
    assertThat(errorLogs()).isEmpty();
  }

  @Test
  void serverApiExceptionWritesSingleErrorLogWithThrowable() {
    ApiException exception = new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, "처리할 수 없습니다.");

    ResponseEntity<ApiResponse<Void>> response = handler.handleApiException(exception);

    assertError(response, HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "처리할 수 없습니다.");
    assertSingleErrorLog(exception, "INTERNAL_SERVER_ERROR");
  }

  @Test
  void validationExceptionUsesValidationFailedErrorWithoutErrorLog() {
    ConstraintViolationException exception = new ConstraintViolationException(Set.of());

    ResponseEntity<ApiResponse<Void>> response = handler.handleConstraintViolation(exception);

    assertError(response, HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "요청 값이 올바르지 않습니다.");
    assertThat(errorLogs()).isEmpty();
  }

  @Test
  void sessionExceptionUsesFeatureStatusAndCode() {
    ResponseEntity<ApiResponse<Void>> response =
        handler.handleFeatureException(new SessionException(SessionErrorCode.SESSION_NOT_FOUND));

    assertError(response, HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND", "세션을 찾을 수 없습니다.");
  }

  @Test
  void userProfileExceptionUsesFeatureStatusAndCode() {
    ResponseEntity<ApiResponse<Void>> response =
        handler.handleFeatureException(
            new UserProfileException(UserProfileErrorCode.INVALID_TOKEN));

    assertError(response, HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "유효하지 않은 토큰입니다.");
  }

  @Test
  void missingStaticResourceUsesNotFoundErrorWithoutSentryCapture() {
    NoResourceFoundException exception =
        new NoResourceFoundException(HttpMethod.GET, "/", "No static resource for request '/'.");

    ResponseEntity<ApiResponse<Void>> response = handler.handleNoResourceFound(exception);

    assertError(response, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "요청한 리소스를 찾을 수 없습니다.");
    assertThat(errorLogs()).isEmpty();
  }

  @Test
  void unexpectedExceptionWritesSingleErrorLogWithThrowable() {
    RuntimeException exception = new RuntimeException("boom");

    ResponseEntity<ApiResponse<Void>> response = handler.handleUnexpectedException(exception);

    assertError(
        response, HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다.");
    assertSingleErrorLog(exception, "예상하지 못한");
  }

  @Test
  void handlerUsesDefaultConstructor() {
    assertThat(GlobalExceptionHandler.class.getDeclaredConstructors())
        .singleElement()
        .satisfies(constructor -> assertThat(constructor.getParameterCount()).isZero());
  }

  private void assertError(
      ResponseEntity<ApiResponse<Void>> response, HttpStatus status, String code, String message) {
    assertThat(response.getStatusCode()).isEqualTo(status);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().success()).isFalse();
    assertThat(response.getBody().data()).isNull();
    assertThat(response.getBody().error()).isNotNull();
    assertThat(response.getBody().error().code()).isEqualTo(code);
    assertThat(response.getBody().error().message()).isEqualTo(message);
  }

  private void assertSingleErrorLog(Throwable exception, String messageFragment) {
    assertThat(errorLogs())
        .singleElement()
        .satisfies(
            event -> {
              assertThat(event.getFormattedMessage()).contains(messageFragment);
              assertThat(event.getThrowableProxy()).isNotNull();
              assertThat(event.getThrowableProxy().getClassName())
                  .isEqualTo(exception.getClass().getName());
              assertThat(event.getThrowableProxy().getMessage()).isEqualTo(exception.getMessage());
            });
  }

  private List<ILoggingEvent> errorLogs() {
    return logAppender.list.stream().filter(event -> event.getLevel() == Level.ERROR).toList();
  }
}
