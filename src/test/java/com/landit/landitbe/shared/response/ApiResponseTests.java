// 공통 API 응답 객체의 생성 규칙을 검증한다.

package com.landit.landitbe.shared.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.landit.landitbe.shared.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** 공통 API 응답 객체의 생성 규칙을 검증한다. */
class ApiResponseTests {

  @DisplayName("성공 응답은 전달한 데이터를 공통 응답으로 감싼다.")
  @Test
  void successWrapsResponseData() {
    ApiResponse<String> response = ApiResponse.success("ok");

    assertThat(response.success()).isTrue();
    assertThat(response.data()).isEqualTo("ok");
    assertThat(response.error()).isNull();
  }

  @DisplayName("상태를 지정한 성공 응답은 해당 HTTP 상태의 ResponseEntity를 반환한다.")
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

  @DisplayName("오류 응답은 오류 코드 이름과 메시지를 사용한다.")
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
