// 관리자 푸시 입력의 지원 URL과 payload 제한을 검증한다.

package com.landit.landitbe.feature.notification.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.landit.landitbe.feature.notification.dto.AdminPushCampaignRequest;
import com.landit.landitbe.shared.exception.ApiException;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.json.JsonMapper;

/** 입력 형식의 대표 허용·거부 경계를 검증한다. */
class AdminPushInputServiceTest {

  private AdminPushInputService service;

  @BeforeEach
  void setup() {
    service = new AdminPushInputService(JsonMapper.builder().build());
  }

  @ParameterizedTest
  @MethodSource("validLinks")
  void acceptsSupportedLinks(String link) {
    assertThatCode(() -> service.validate(request(link))).doesNotThrowAnyException();
  }

  @ParameterizedTest
  @MethodSource("invalidLinks")
  void rejectsInvalidLinks(String link) {
    assertThatThrownBy(() -> service.validate(request(link))).isInstanceOf(ApiException.class);
  }

  @Test
  void rejectsOversizedPayloadAndInvalidKey() {
    assertThatThrownBy(
            () -> service.validate(new AdminPushCampaignRequest("공지", "가".repeat(1000), "/")))
        .isInstanceOf(ApiException.class);
    assertThatThrownBy(() -> service.validateKey("space key")).isInstanceOf(ApiException.class);
  }

  private AdminPushCampaignRequest request(String link) {
    return new AdminPushCampaignRequest("공지", "내용", link);
  }

  private static Stream<String> validLinks() {
    return Stream.of("/home", "/lesson/1?tab=review", "https://landit.co.kr/notice");
  }

  private static Stream<String> invalidLinks() {
    return Stream.of("//evil.example", "javascript:alert(1)", "http://landit.co.kr", "https:///x");
  }
}
