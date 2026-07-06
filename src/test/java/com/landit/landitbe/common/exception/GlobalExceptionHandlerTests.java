// 공통 예외 핸들러의 오류 응답 매핑을 검증한다.
package com.landit.landitbe.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.landit.landitbe.common.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTests {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void apiExceptionUsesErrorCodeStatusAndMessage() {
        ApiException exception = new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "리소스가 없습니다.");

        ResponseEntity<ApiResponse<Void>> response = handler.handleApiException(exception);

        assertError(response, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "리소스가 없습니다.");
    }

    @Test
    void validationExceptionUsesValidationFailedError() {
        ConstraintViolationException exception = new ConstraintViolationException(Set.of());

        ResponseEntity<ApiResponse<Void>> response = handler.handleConstraintViolation(exception);

        assertError(response, HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "요청 값이 올바르지 않습니다.");
    }

    @Test
    void unexpectedExceptionUsesInternalServerError() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleUnexpectedException(new RuntimeException("boom"));

        assertError(response, HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다.");
    }

    private void assertError(
            ResponseEntity<ApiResponse<Void>> response,
            HttpStatus status,
            String code,
            String message
    ) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().data()).isNull();
        assertThat(response.getBody().error()).isNotNull();
        assertThat(response.getBody().error().code()).isEqualTo(code);
        assertThat(response.getBody().error().message()).isEqualTo(message);
    }
}
