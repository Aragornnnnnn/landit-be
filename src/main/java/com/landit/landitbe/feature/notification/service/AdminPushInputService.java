// 관리자 푸시 입력과 딥 링크 형식을 검증한다.

package com.landit.landitbe.feature.notification.service;

import com.landit.landitbe.feature.notification.dto.AdminPushCampaignRequest;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

/** 관리자 푸시 입력의 기본 제약과 실제 payload 크기를 검증한다. */
@Service
public class AdminPushInputService {

  private static final Pattern REQUEST_KEY = Pattern.compile("[A-Za-z0-9_-]{1,128}");
  private static final int MAX_PAYLOAD_BYTES = 3000;

  private final JsonMapper jsonMapper;

  /**
   * 실제 발송과 같은 JSON 직렬화기를 사용한다.
   *
   * @param jsonMapper JSON 직렬화기
   */
  public AdminPushInputService(JsonMapper jsonMapper) {
    this.jsonMapper = jsonMapper;
  }

  /**
   * 알림 내용과 딥 링크를 검증한다.
   *
   * @param request 알림 내용
   * @return 검증된 요청
   * @throws ApiException 입력 형식이나 payload 크기가 올바르지 않을 때 발생
   */
  public AdminPushCampaignRequest validate(AdminPushCampaignRequest request) {
    if (request == null
        || request.title() == null
        || request.title().isBlank()
        || request.body() == null
        || request.body().isBlank()
        || request.deepLink() == null
        || request.deepLink().isBlank()) {
      throw invalid();
    }
    validateDeepLink(request.deepLink());
    if (payload(request).length > MAX_PAYLOAD_BYTES) {
      throw new ApiException(ErrorCode.PUSH_PAYLOAD_TOO_LARGE);
    }
    return request;
  }

  /**
   * 멱등성 키 형식을 검증한다.
   *
   * @param key 멱등성 키
   */
  public void validateKey(String key) {
    if (key == null || !REQUEST_KEY.matcher(key).matches()) {
      throw invalid();
    }
  }

  /**
   * 캠페인 원문의 SHA-256 해시를 반환한다.
   *
   * @param request 캠페인 원문
   * @return 16진수 해시
   */
  public String fingerprint(AdminPushCampaignRequest request) {
    try {
      return HexFormat.of()
          .formatHex(MessageDigest.getInstance("SHA-256").digest(payload(validate(request))));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 해시를 생성할 수 없습니다.", exception);
    }
  }

  private void validateDeepLink(String value) {
    try {
      URI uri = new URI(value);
      if (value.startsWith("/")) {
        if (value.startsWith("//") || value.indexOf('\\') >= 0 || uri.getRawAuthority() != null) {
          throw invalid();
        }
        return;
      }
      if (!"https".equalsIgnoreCase(uri.getScheme())
          || uri.getHost() == null
          || uri.getRawUserInfo() != null) {
        throw invalid();
      }
    } catch (URISyntaxException exception) {
      throw invalid();
    }
  }

  private byte[] payload(AdminPushCampaignRequest request) {
    return jsonMapper
        .writeValueAsString(
            Map.of(
                "title", request.title(),
                "body", request.body(),
                "data", Map.of("url", request.deepLink()),
                "sound", "default",
                "channelId", "default"))
        .getBytes(StandardCharsets.UTF_8);
  }

  private ApiException invalid() {
    return new ApiException(ErrorCode.INVALID_REQUEST);
  }
}
