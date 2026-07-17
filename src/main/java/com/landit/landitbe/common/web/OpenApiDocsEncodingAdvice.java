// OpenAPI JSON 문서 응답의 문자 인코딩을 명시한다.

package com.landit.landitbe.common.web;

import java.nio.charset.StandardCharsets;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/** OpenAPI JSON 문서 응답의 문자 인코딩을 명시한다. */
@ControllerAdvice
public class OpenApiDocsEncodingAdvice implements ResponseBodyAdvice<Object> {

  @Override
  public boolean supports(
      MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
    return true;
  }

  @Override
  public Object beforeBodyWrite(
      Object body,
      MethodParameter returnType,
      MediaType selectedContentType,
      Class<? extends HttpMessageConverter<?>> selectedConverterType,
      ServerHttpRequest request,
      ServerHttpResponse response) {
    if (request.getURI().getPath().startsWith("/v3/api-docs")) {
      response
          .getHeaders()
          .setContentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8));
    }

    return body;
  }
}
