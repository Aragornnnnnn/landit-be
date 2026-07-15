// 공통 API 응답 객체의 생성 규칙을 검증한다.
package com.landit.landitbe.common.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.landit.landitbe.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ApiResponseTests {

  @Test
  void successWrapsResponseData() {
    ApiResponse<String> response = ApiResponse.success("ok");

    assertThat(response.success()).isTrue();
    assertThat(response.data()).isEqualTo("ok");
    assertThat(response.error()).isNull();
  }

  @Test
  void successWithStatusReturnsResponseEntityUsingGivenHttpStatus() {
    ResponseEntity<ApiResponse<String>> response =
        ApiResponse.success(HttpStatus.CREATED, "created");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().success()).isTrue();
    assertThat(response.getBody().data()).isEqualTo("created");
    assertThat(response.getBody().error()).isNull();
  }

  @Test
  void errorUsesErrorCodeNameAndMessage() {
    ApiResponse<Void> response = ApiResponse.error(ErrorCode.VALIDATION_FAILED);

    assertThat(response.success()).isFalse();
    assertThat(response.data()).isNull();
    assertThat(response.error()).isNotNull();
    assertThat(response.error().code()).isEqualTo("VALIDATION_FAILED");
    assertThat(response.error().message()).isEqualTo("요청 값이 올바르지 않습니다.");
  }
}
