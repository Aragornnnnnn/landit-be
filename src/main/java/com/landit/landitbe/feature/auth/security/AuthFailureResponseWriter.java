// 인증 실패를 공통 API 오류 응답으로 쓰는 컴포넌트다.

package com.landit.landitbe.feature.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.landitbe.shared.exception.ErrorCode;
import com.landit.landitbe.shared.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/** 인증 실패를 공통 API 오류 응답으로 쓰는 컴포넌트다. */
@Component
public class AuthFailureResponseWriter {

  private final ObjectMapper objectMapper;

  /** 인증 실패 응답을 JSON으로 직렬화할 작성기를 생성한다. */
  public AuthFailureResponseWriter() {
    this.objectMapper = new ObjectMapper();
  }

  /** Security filter 구간에서 발생한 인증 실패를 공통 응답으로 쓴다. */
  public void write(HttpServletResponse response, ErrorCode errorCode) throws IOException {
    write(response, errorCode.getStatus(), ApiResponse.error(errorCode));
  }

  /**
   * Security filter 구간에서 발생한 기능별 접근 거부를 공통 응답으로 쓴다.
   *
   * @param response 응답을 쓸 HTTP 응답
   * @param status 응답 HTTP 상태
   * @param code 기능별 오류 코드
   * @param message 클라이언트에 노출할 오류 메시지
   * @throws IOException 응답 본문을 쓰지 못했을 때
   */
  public void write(HttpServletResponse response, HttpStatus status, String code, String message)
      throws IOException {
    write(response, status, ApiResponse.error(code, message));
  }

  private void write(HttpServletResponse response, HttpStatus status, ApiResponse<Void> body)
      throws IOException {
    response.setStatus(status.value());
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(response.getWriter(), body);
  }
}
