// 관리자 푸시 입력의 URI 안전성, 실제 JSON 크기와 멱등성 해시를 검증한다.

package com.landit.landitbe.feature.notification.service;

import com.landit.landitbe.feature.notification.dto.AdminPushCampaignRequest;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

/** 관리자 푸시 입력의 URI 안전성, 실제 JSON 크기와 멱등성 해시를 검증한다. */
@Service
public class AdminPushInputService {

  private static final Pattern REQUEST_KEY = Pattern.compile("[A-Za-z0-9_-]{1,128}");
  private static final Pattern ENCODED_BYTES = Pattern.compile("(?:%[0-9a-fA-F]{2})+");
  private static final Pattern DNS_LABEL =
      Pattern.compile("[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?");
  private static final int MAX_PAYLOAD_BYTES = 3000;

  private final JsonMapper jsonMapper;

  /**
   * 실제 발송과 동일한 JSON 직렬화기를 주입한다.
   *
   * @param jsonMapper JSON 직렬화기
   */
  public AdminPushInputService(JsonMapper jsonMapper) {
    this.jsonMapper = jsonMapper;
  }

  /**
   * 정규화된 원문과 딥 링크, 실제 표시 필드의 UTF-8 크기를 검증한다.
   *
   * @param request 원문 요청
   * @return 검증된 정규화 요청
   * @throws ApiException 입력 또는 payload 크기가 허용 범위를 벗어나면 발생
   */
  public AdminPushCampaignRequest validate(AdminPushCampaignRequest request) {
    if (request == null) {
      throw invalid();
    }
    validateText(request.title(), 255, false);
    validateText(request.body(), 500, true);
    validateText(request.deepLink(), 1000, false);
    validateDeepLink(request.deepLink());
    if (payloadBytes(request).length > MAX_PAYLOAD_BYTES) {
      throw new ApiException(ErrorCode.PUSH_PAYLOAD_TOO_LARGE);
    }
    return request;
  }

  /**
   * ASCII 영숫자와 대시, 밑줄로 구성된 요청 키를 검증한다.
   *
   * @param key 멱등성 요청 키
   * @throws ApiException 키가 없거나 형식이 올바르지 않으면 발생
   */
  public void validateKey(String key) {
    if (key == null || !REQUEST_KEY.matcher(key).matches()) {
      throw invalid();
    }
  }

  /**
   * 정규화한 표시 필드의 결정적인 SHA-256 해시를 반환한다.
   *
   * @param request 해시를 계산할 원문
   * @return 소문자 16진수 SHA-256 해시
   * @throws ApiException 요청이 유효하지 않으면 발생
   */
  public String fingerprint(AdminPushCampaignRequest request) {
    validate(request);
    try {
      return HexFormat.of()
          .formatHex(MessageDigest.getInstance("SHA-256").digest(payloadBytes(request)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 해시를 생성할 수 없습니다.", exception);
    }
  }

  private void validateText(String value, int maximumLength, boolean allowNewline) {
    if (value == null || value.isBlank() || value.length() > maximumLength) {
      throw invalid();
    }
    if (value.codePoints().anyMatch(code -> forbiddenControl(code, allowNewline))) {
      throw invalid();
    }
  }

  private boolean forbiddenControl(int code, boolean allowNewline) {
    if (allowNewline && (code == '\n' || code == '\r')) {
      return false;
    }
    return Character.isISOControl(code)
        || code == 0x2028
        || code == 0x2029
        || Character.getType(code) == Character.SURROGATE;
  }

  private void validateDeepLink(String value) {
    if (value
        .codePoints()
        .anyMatch(code -> Character.isWhitespace(code) || Character.isSpaceChar(code))) {
      throw invalid();
    }
    try {
      URI uri = new URI(value);
      boolean internal = value.startsWith("/");
      if (internal) {
        if (value.startsWith("//") || uri.getScheme() != null || uri.getRawAuthority() != null) {
          throw invalid();
        }
      } else {
        validateExternalUri(uri);
      }
      validateDecodedLink(value, internal);
    } catch (URISyntaxException exception) {
      throw invalid();
    }
  }

  private void validateExternalUri(URI uri) {
    String host = uri.getHost();
    if (!"https".equalsIgnoreCase(uri.getScheme())
        || uri.getRawUserInfo() != null
        || (uri.getPort() != -1 && uri.getPort() != 443)
        || host == null
        || host.length() > 253) {
      throw invalid();
    }
    String[] labels = host.split("\\.", -1);
    if (labels.length < 2 || !labels[labels.length - 1].matches(".*[A-Za-z].*")) {
      throw invalid();
    }
    for (String label : labels) {
      if (!DNS_LABEL.matcher(label).matches()) {
        throw invalid();
      }
    }
    if (uri.getRawAuthority().endsWith(":")) {
      throw invalid();
    }
  }

  // 중첩 인코딩을 풀어도 authority, 역슬래시, 제어문자가 생기지 않아야 한다.
  private void validateDecodedLink(String value, boolean internal) {
    String current = value;
    while (true) {
      if (current.indexOf('\\') >= 0
          || current.codePoints().anyMatch(code -> forbiddenControl(code, false))
          || (internal && current.startsWith("//"))) {
        throw invalid();
      }
      Matcher matcher = ENCODED_BYTES.matcher(current);
      if (!matcher.find()) {
        return;
      }
      current = matcher.replaceAll(match -> Matcher.quoteReplacement(decodeBytes(match.group())));
    }
  }

  private String decodeBytes(String encoded) {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    for (int index = 0; index < encoded.length(); index += 3) {
      bytes.write(Integer.parseInt(encoded.substring(index + 1, index + 3), 16));
    }
    try {
      return StandardCharsets.UTF_8
          .newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT)
          .decode(ByteBuffer.wrap(bytes.toByteArray()))
          .toString();
    } catch (CharacterCodingException exception) {
      throw invalid();
    }
  }

  private byte[] payloadBytes(AdminPushCampaignRequest request) {
    Map<String, Object> fields = new LinkedHashMap<>();
    fields.put("title", request.title());
    fields.put("body", request.body());
    fields.put("data", Map.of("url", request.deepLink()));
    fields.put("sound", "default");
    fields.put("channelId", "default");
    return jsonMapper.writeValueAsBytes(fields);
  }

  private ApiException invalid() {
    return new ApiException(ErrorCode.INVALID_REQUEST);
  }
}
