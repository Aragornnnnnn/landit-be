// 테스트에서 토큰을 포함한 JSON 요청을 동일한 HTTP 계약으로 구성한다.

package com.landit.landitbe.support;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/** 토큰 헤더와 JSON 본문을 가진 테스트 요청을 구성한다. */
public final class AuthenticatedJsonRequests {
  private AuthenticatedJsonRequests() {}

  /**
   * POST 요청에 Bearer 토큰과 JSON 본문을 설정한다.
   *
   * @param uri 요청 경로 또는 URI 템플릿
   * @param token 인증 헤더에 넣을 토큰
   * @param body 직렬화된 JSON 본문
   * @param uriVariables URI 템플릿에 대입할 값
   * @return 상태나 응답을 검증하기 전의 요청 빌더
   */
  public static MockHttpServletRequestBuilder postJsonWithToken(
      String uri, String token, String body, Object... uriVariables) {
    return MockMvcRequestBuilders.post(uri, uriVariables)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
        .contentType(MediaType.APPLICATION_JSON)
        .content(body);
  }

  /**
   * PUT 요청에 Bearer 토큰과 JSON 본문을 설정한다.
   *
   * @param uri 요청 경로 또는 URI 템플릿
   * @param token 인증 헤더에 넣을 토큰
   * @param body 직렬화된 JSON 본문
   * @param uriVariables URI 템플릿에 대입할 값
   * @return 상태나 응답을 검증하기 전의 요청 빌더
   */
  public static MockHttpServletRequestBuilder putJsonWithToken(
      String uri, String token, String body, Object... uriVariables) {
    return MockMvcRequestBuilders.put(uri, uriVariables)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
        .contentType(MediaType.APPLICATION_JSON)
        .content(body);
  }

  /**
   * PATCH 요청에 Bearer 토큰과 JSON 본문을 설정한다.
   *
   * @param uri 요청 경로 또는 URI 템플릿
   * @param token 인증 헤더에 넣을 토큰
   * @param body 직렬화된 JSON 본문
   * @param uriVariables URI 템플릿에 대입할 값
   * @return 상태나 응답을 검증하기 전의 요청 빌더
   */
  public static MockHttpServletRequestBuilder patchJsonWithToken(
      String uri, String token, String body, Object... uriVariables) {
    return MockMvcRequestBuilders.patch(uri, uriVariables)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
        .contentType(MediaType.APPLICATION_JSON)
        .content(body);
  }
}
