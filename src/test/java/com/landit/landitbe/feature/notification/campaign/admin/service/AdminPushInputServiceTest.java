// 관리자 푸시 입력의 지원 URL과 payload 제한을 검증한다.

package com.landit.landitbe.feature.notification.campaign.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.landit.landitbe.feature.notification.campaign.admin.dto.AdminPushCampaignRequest;
import com.landit.landitbe.feature.notification.campaign.domain.AdminPushAudienceType;
import com.landit.landitbe.shared.exception.ApiException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

  @DisplayName("관리자 푸시에서 지원하는 링크를 허용한다.")
  @ParameterizedTest
  @MethodSource("validLinks")
  void acceptsSupportedLinks(String link) {
    assertThatCode(() -> service.validate(request(link))).doesNotThrowAnyException();
  }

  @DisplayName("관리자 푸시에서 잘못된 링크를 거부한다.")
  @ParameterizedTest
  @MethodSource("invalidLinks")
  void rejectsInvalidLinks(String link) {
    assertThatThrownBy(() -> service.validate(request(link))).isInstanceOf(ApiException.class);
  }

  @DisplayName("대상 없는 제외 목록은 거부하되 선택 대상 전체를 제외하는 것은 허용한다.")
  @Test
  void rejectsExclusionsWithoutAnAudienceButAllowsAllSelectedUsersToBeExcluded() {
    var excludedOnly =
        new AdminPushCampaignRequest(
            "공지", "내용", "/home", AdminPushAudienceType.SELECTED, List.of(), null, List.of(1L));
    assertThatThrownBy(() -> service.validate(excludedOnly)).isInstanceOf(ApiException.class);
    var allExcluded =
        new AdminPushCampaignRequest(
            "공지", "내용", "/home", AdminPushAudienceType.SELECTED, List.of(1L), null, List.of(1L));
    assertThat(service.validate(allExcluded).userProfileIds()).isEmpty();
    assertThat(service.fingerprint(allExcluded)).isNotBlank();
  }

  @DisplayName("푸시 페이로드가 너무 크거나 멱등 키가 잘못되면 거부한다.")
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

  @DisplayName("기존 전체 대상 요청을 허용하고 선택 ID를 정규화하되 푸시 페이로드 크기에서 제외한다.")
  @Test
  void acceptsLegacyAllAndNormalizesSelectedIdsWithoutCountingThemInPushPayload() {
    AdminPushCampaignRequest legacy =
        JsonMapper.builder()
            .build()
            .readValue(
                "{\"title\":\"공지\",\"body\":\"내용\",\"deepLink\":\"/home\"}",
                AdminPushCampaignRequest.class);
    assertThat(service.validate(legacy).audienceType()).isEqualTo(AdminPushAudienceType.ALL);
    AdminPushCampaignRequest large =
        new AdminPushCampaignRequest(
            "공지",
            "내용",
            "/home",
            AdminPushAudienceType.SELECTED,
            LongStream.rangeClosed(1, 10001).boxed().toList());
    assertThatCode(() -> service.validate(large)).doesNotThrowAnyException();
    assertThat(
            service
                .validate(
                    new AdminPushCampaignRequest(
                        "공지", "내용", "/home", AdminPushAudienceType.SELECTED, List.of(2L, 1L, 2L)))
                .userProfileIds())
        .containsExactly(1L, 2L);
  }

  @DisplayName("잘못된 관리자 푸시 대상 선택을 거부한다.")
  @ParameterizedTest
  @MethodSource("invalidAudiences")
  void rejectsInvalidAudienceSelections(AdminPushCampaignRequest request) {
    assertThatThrownBy(() -> service.validate(request)).isInstanceOf(ApiException.class);
  }

  private static Stream<AdminPushCampaignRequest> invalidAudiences() {
    return Stream.of(
        new AdminPushCampaignRequest("공지", "내용", "/home", AdminPushAudienceType.ALL, List.of(1L)),
        new AdminPushCampaignRequest("공지", "내용", "/home", null, List.of(1L)),
        new AdminPushCampaignRequest("공지", "내용", "/home", AdminPushAudienceType.SELECTED, null),
        new AdminPushCampaignRequest(
            "공지", "내용", "/home", AdminPushAudienceType.SELECTED, List.of()),
        new AdminPushCampaignRequest(
            "공지", "내용", "/home", AdminPushAudienceType.SELECTED, Arrays.asList(1L, null)),
        new AdminPushCampaignRequest(
            "공지", "내용", "/home", AdminPushAudienceType.SELECTED, List.of(0L)));
  }

  private static Stream<String> validLinks() {
    return Stream.of("/home", "/lesson/1?tab=review", "https://landit.co.kr/notice");
  }

  private static Stream<String> invalidLinks() {
    return Stream.of("//evil.example", "javascript:alert(1)", "http://landit.co.kr", "https:///x");
  }
}
