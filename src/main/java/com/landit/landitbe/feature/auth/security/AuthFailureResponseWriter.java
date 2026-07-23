// 인증 실패를 공통 API 오류 응답으로 쓰는 컴포넌트다.

package com.landit.landitbe.feature.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.landitbe.shared.exception.ErrorCode;
import com.landit.landitbe.shared.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
    response.setStatus(errorCode.getStatus().value());
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(response.getWriter(), ApiResponse.error(errorCode));
  }
}
