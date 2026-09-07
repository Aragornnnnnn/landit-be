// 관리자 푸시의 딥 링크 우회, 입력 경계와 멱등성 해시를 검증한다.

package com.landit.landitbe.feature.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.landit.landitbe.feature.notification.dto.AdminPushCampaignRequest;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;

class AdminPushInputServiceTest {

  private final JsonMapper jsonMapper = JsonMapper.builder().build();
  private final AdminPushInputService service = new AdminPushInputService(jsonMapper);

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/",
        "/home",
        "/practice?scenarioId=10#intro",
        "/검색?query=%ED%95%9C%EA%B8%80",
        "/home?q=hello%20world",
        "https://example.com",
        "https://example.com:443/path?q=a+b#section",
        "HTTPS://sub.example.com/path",
        "https://xn--3e0b707e.com/path"
      })
  void acceptsSupportedDeepLinks(String deepLink) {
    assertThat(service.validate(request(deepLink)).deepLink()).isEqualTo(deepLink);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "//example.com/path",
        "/\\example.com",
        "/%2Fexample.com",
        "/%252Fexample.com",
        "/%25252fexample.com",
        "/%5cexample.com",
        "/%255Cexample.com",
        "/home%00",
        "/home%250A",
        "/home%7F",
        "/home%FF",
        "/home%C0%AF",
        "/home%",
        "/home%2",
        "/home%GG",
        "/home path",
        "/home\tpath",
        "home",
        "http://example.com",
        "javascript:alert(1)",
        "intent://example.com",
        "file:///home",
        "https://",
        "https:example.com",
        "https://user:pass@example.com",
        "https://user@example.com",
        "https://example.com:80",
        "https://example.com:",
        "https://example.com:abc",
        "https://127.0.0.1",
        "https://[::1]",
        "https://2130706433",
        "https://0x7f000001",
        "https://127.1",
        "https://example..com",
        "https://-example.com",
        "https://example_.com",
        "https://example.com/%0d%0a",
        "https://example.com/%255cpath"
      })
  void rejectsMalformedAndAmbiguousDeepLinks(String deepLink) {
    assertInvalid(request(deepLink));
  }

  @Test
  void normalizesInputAndAllowsBodyLineBreaks() {
    AdminPushCampaignRequest result =
        service.validate(new AdminPushCampaignRequest("  공지  ", "  첫 줄\n둘째 줄  ", " /home "));

    assertThat(result).isEqualTo(new AdminPushCampaignRequest("공지", "첫 줄\n둘째 줄", "/home"));
  }

  @Test
  void rejectsTitleLineBreaksAndBodyControlCharacters() {
    assertInvalid(new AdminPushCampaignRequest("공지\n제목", "본문", "/home"));
    assertInvalid(new AdminPushCampaignRequest("공지", "본문\t탭", "/home"));
    assertInvalid(new AdminPushCampaignRequest("공지", "본문\u0000문자", "/home"));
    assertInvalid(new AdminPushCampaignRequest("공지", "본문\u2028문자", "/home"));
  }

  @Test
  void rejectsNullBlankAndOverlongFieldsInService() {
    assertInvalid(null);
    assertInvalid(new AdminPushCampaignRequest(null, "본문", "/home"));
    assertInvalid(new AdminPushCampaignRequest("제목", " ", "/home"));
    assertInvalid(new AdminPushCampaignRequest("제목", "본문", null));
    assertInvalid(new AdminPushCampaignRequest("a".repeat(256), "본문", "/home"));
    assertInvalid(new AdminPushCampaignRequest("제목", "a".repeat(501), "/home"));
    assertInvalid(new AdminPushCampaignRequest("제목", "본문", "/" + "a".repeat(1000)));
  }

  @Test
  void beanValidationAppliesToNormalizedValues() {
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      assertThat(
              factory
                  .getValidator()
                  .validate(
                      new AdminPushCampaignRequest(" " + "a".repeat(255) + " ", "본문", "/home")))
          .isEmpty();
      assertThat(
              factory
                  .getValidator()
                  .validate(new AdminPushCampaignRequest("a".repeat(256), "본문", "/home")))
          .hasSize(1);
      assertThat(factory.getValidator().validate(new AdminPushCampaignRequest(" ", "본문", "/home")))
          .hasSize(1);
    }
  }

  @Test
  void enforcesUtf8PayloadSizeAtTheExactBoundary() {
    String title = "가".repeat(255);
    String body = "나".repeat(500);
    int baseLength = payloadLength(title, body, "/");
    String boundaryLink = "/" + "a".repeat(3000 - baseLength);

    assertThatCode(() -> service.validate(new AdminPushCampaignRequest(title, body, boundaryLink)))
        .doesNotThrowAnyException();
    assertThatThrownBy(
            () -> service.validate(new AdminPushCampaignRequest(title, body, boundaryLink + "a")))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PUSH_PAYLOAD_TOO_LARGE));
  }

  @Test
  void payloadLimitIncludesJsonEscapingAndEmojiBytes() {
    String title = "가".repeat(255);
    String body = "나".repeat(300) + "😀".repeat(50) + "\"".repeat(100);
    String link = "/" + "a".repeat(999);

    assertThat(payloadLength(title, body, link)).isGreaterThan(3000);
    assertThatThrownBy(() -> service.validate(new AdminPushCampaignRequest(title, body, link)))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PUSH_PAYLOAD_TOO_LARGE));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" key", "key ", "a.b", "키", "a/b", "a\nb"})
  void rejectsUnsupportedRequestKeys(String key) {
    assertThatThrownBy(() -> service.validateKey(key))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
  }

  @Test
  void requestKeysHaveAnExplicitAsciiLengthBoundary() {
    assertThatCode(() -> service.validateKey("A_9-".repeat(32))).doesNotThrowAnyException();
    assertThatThrownBy(() -> service.validateKey("a".repeat(129))).isInstanceOf(ApiException.class);
  }

  @Test
  void fingerprintUsesNormalizedContentAndIncludesDeepLink() {
    AdminPushCampaignRequest first = new AdminPushCampaignRequest(" 제목 ", " 본문 ", " /home ");
    AdminPushCampaignRequest same = new AdminPushCampaignRequest("제목", "본문", "/home");
    AdminPushCampaignRequest changed = new AdminPushCampaignRequest("제목", "본문", "/practice");

    assertThat(service.fingerprint(first))
        .matches("[0-9a-f]{64}")
        .isEqualTo(service.fingerprint(same));
    assertThat(service.fingerprint(first)).isNotEqualTo(service.fingerprint(changed));
  }

  private int payloadLength(String title, String body, String deepLink) {
    return jsonMapper.writeValueAsBytes(
            Map.of(
                "title",
                title,
                "body",
                body,
                "data",
                Map.of("url", deepLink),
                "sound",
                "default",
                "channelId",
                "default"))
        .length;
  }

  private AdminPushCampaignRequest request(String deepLink) {
    return new AdminPushCampaignRequest("공지", "새로운 소식", deepLink);
  }

  private void assertInvalid(AdminPushCampaignRequest request) {
    assertThatThrownBy(() -> service.validate(request))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
  }
}
