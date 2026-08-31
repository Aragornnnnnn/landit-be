// Expo Push Token 상태 관리 API의 인증과 저장 계약을 검증한다.

package com.landit.landitbe.feature.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.landitbe.feature.notification.dto.ExpoPushTokenUpdateRequest;
import com.landit.landitbe.feature.notification.service.ExpoPushTokenService;
import com.landit.landitbe.feature.profile.exception.UserProfileException;
import com.landit.landitbe.shared.domain.AppPlatform;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Expo Push Token 상태 관리 API의 인증과 저장 계약을 검증한다. */
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@TestPropertySource(
    properties = {
      "landit.auth.oidc.fake-enabled=true",
      "landit.auth.token.secret=landit-test-token-secret-that-is-long-enough"
    })
class ExpoPushTokenApiIntegrationTests {

  @Autowired private MockMvc mockMvc;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private ExpoPushTokenService expoPushTokenService;

  private final ObjectMapper objectMapper = new ObjectMapper();

  /** 인증된 사용자는 PUT 요청으로 Expo Push Token을 등록하고 비활성화할 수 있다. */
  @Test
  void upsertsAndRevokesExpoPushToken() throws Exception {
    String userKey = "expo-push-token-owner";
    String accessToken = login(userKey);
    String expoPushToken = "ExponentPushToken[upsert-and-revoke]";

    updateToken(accessToken, AppPlatform.IOS, expoPushToken, true);
    assertTokenStatus(expoPushToken, "ACTIVE");
    assertThat(pushPermissionStatus(userKey)).isEqualTo("GRANTED");

    updateToken(accessToken, AppPlatform.IOS, expoPushToken, false);
    assertTokenStatus(expoPushToken, "REVOKED");
    assertThat(pushPermissionStatus(userKey)).isEqualTo("GRANTED");

    updateToken(accessToken, AppPlatform.ANDROID, expoPushToken, true);
    assertTokenStatus(expoPushToken, "ACTIVE");
    assertTokenPlatform(expoPushToken, "ANDROID");
  }

  /** Expo Push Token을 활성화하면 사용자의 푸시 권한을 허용 상태로 기록한다. */
  @Test
  void grantsPushPermissionWhenExpoPushTokenIsEnabled() throws Exception {
    String userKey = "expo-push-permission-granted";
    String accessToken = login(userKey);

    assertThat(pushPermissionStatus(userKey)).isEqualTo("NOT_DETERMINED");
    assertThat(pushPermissionUpdatedAt(userKey)).isNull();

    updateToken(accessToken, AppPlatform.IOS, "ExponentPushToken[permission-granted]", true);

    assertThat(pushPermissionStatus(userKey)).isEqualTo("GRANTED");
    assertThat(pushPermissionUpdatedAt(userKey)).isNotNull();
  }

  /** 다른 사용자는 본인 소유가 아닌 Expo Push Token을 비활성화할 수 없다. */
  @Test
  void doesNotRevokeAnotherUsersExpoPushToken() throws Exception {
    String ownerAccessToken = login("expo-push-token-real-owner");
    String otherAccessToken = login("expo-push-token-other-user");
    String expoPushToken = "ExponentPushToken[owner-only-revoke]";
    registerToken(ownerAccessToken, expoPushToken);

    mockMvc
        .perform(
            put("/api/v1/me/expo-push-token")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "platform":"IOS",
                      "expoPushToken":"%s",
                      "enabled":false
                    }
                    """
                        .formatted(expoPushToken)))
        .andExpect(status().isOk());

    assertTokenStatus(expoPushToken, "ACTIVE");
  }

  /** 인증되지 않은 요청은 Expo Push Token 상태를 변경할 수 없다. */
  @Test
  void rejectsUnauthenticatedExpoPushTokenUpdate() throws Exception {
    mockMvc
        .perform(
            put("/api/v1/me/expo-push-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "platform":"IOS",
                      "expoPushToken":"ExponentPushToken[unauthenticated]",
                      "enabled":true
                    }
                    """))
        .andExpect(status().isUnauthorized());
  }

  /** APNs나 FCM 형식의 Token은 Expo Push Token으로 저장할 수 없다. */
  @Test
  void rejectsNonExpoPushToken() throws Exception {
    String accessToken = login("expo-push-token-invalid-format");
    String nativePushToken = "native-apns-or-fcm-token";

    mockMvc
        .perform(
            put("/api/v1/me/expo-push-token")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "platform":"IOS",
                      "expoPushToken":"%s",
                      "enabled":true
                    }
                    """
                        .formatted(nativePushToken)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

    assertThat(tokenCount(nativePushToken)).isZero();
  }

  /** 같은 신규 Token의 동시 PUT 요청은 모두 성공하고 하나의 행만 저장한다. */
  @Test
  void handlesConcurrentUpsertsIdempotently() throws Exception {
    String userKey = "expo-push-token-concurrent-owner";
    login(userKey);
    Long userProfileId = userProfileId(userKey);
    String expoPushToken = "ExponentPushToken[concurrent-upsert]";
    ExpoPushTokenUpdateRequest request =
        new ExpoPushTokenUpdateRequest(AppPlatform.IOS, expoPushToken, true);
    int requestCount = 8;
    CountDownLatch ready = new CountDownLatch(requestCount);
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(requestCount);
    List<Future<Void>> futures = new ArrayList<>();

    try {
      for (int index = 0; index < requestCount; index++) {
        futures.add(
            executor.submit(
                () -> {
                  ready.countDown();
                  start.await();
                  expoPushTokenService.update(userProfileId, request);
                  return null;
                }));
      }
      assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
      start.countDown();
      futures.forEach(future -> assertThatCode(future::get).doesNotThrowAnyException());
    } finally {
      executor.shutdownNow();
    }

    assertThat(tokenCount(expoPushToken)).isEqualTo(1);
    assertTokenStatus(expoPushToken, "ACTIVE");
  }

  /** 사용자 프로필 권한 갱신에 실패하면 Expo Push Token 등록도 함께 롤백한다. */
  @Test
  void rollsBackExpoPushTokenWhenPermissionGrantFails() throws Exception {
    String userKey = "expo-push-permission-grant-failure";
    login(userKey);
    Long userProfileId = userProfileId(userKey);
    jdbcTemplate.update("update user_profile set status = 'WITHDRAWN' where id = ?", userProfileId);
    String expoPushToken = "ExponentPushToken[permission-grant-failure]";
    ExpoPushTokenUpdateRequest request =
        new ExpoPushTokenUpdateRequest(AppPlatform.IOS, expoPushToken, true);

    assertThatThrownBy(() -> expoPushTokenService.update(userProfileId, request))
        .isInstanceOf(UserProfileException.class);

    assertThat(tokenCount(expoPushToken)).isZero();
  }

  /** 테스트 식별자로 가짜 소셜 로그인을 수행하고 access token을 반환한다. */
  private String login(String userKey) throws Exception {
    String nonce = UUID.randomUUID().toString();
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/auth/social-login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "provider":"GOOGLE",
                          "idToken":"%s|%s@example.com|%s|%s",
                          "nonce":"%s"
                        }
                        """
                            .formatted(userKey, userKey, userKey, nonce, nonce)))
            .andExpect(status().isOk())
            .andReturn();
    JsonNode body = objectMapper.readTree(result.getResponse().getContentAsByteArray());
    return body.get("data").get("accessToken").asText();
  }

  /** 테스트 사용자의 Expo Push Token을 활성 상태로 등록한다. */
  private void registerToken(String accessToken, String expoPushToken) throws Exception {
    updateToken(accessToken, AppPlatform.IOS, expoPushToken, true);
  }

  private void updateToken(
      String accessToken, AppPlatform platform, String expoPushToken, boolean enabled)
      throws Exception {
    mockMvc
        .perform(
            put("/api/v1/me/expo-push-token")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new ExpoPushTokenUpdateRequest(platform, expoPushToken, enabled))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));
  }

  /** Expo Push Token의 현재 저장 상태를 검증한다. */
  private void assertTokenStatus(String expoPushToken, String expectedStatus) {
    String actualStatus =
        jdbcTemplate.queryForObject(
            "select status from user_push_token where expo_push_token = ?",
            String.class,
            expoPushToken);
    assertThat(actualStatus).isEqualTo(expectedStatus);
  }

  /** Expo Push Token의 현재 저장 플랫폼을 검증한다. */
  private void assertTokenPlatform(String expoPushToken, String expectedPlatform) {
    String actualPlatform =
        jdbcTemplate.queryForObject(
            "select platform from user_push_token where expo_push_token = ?",
            String.class,
            expoPushToken);
    assertThat(actualPlatform).isEqualTo(expectedPlatform);
  }

  /** 테스트 사용자의 프로필 ID를 조회한다. */
  private Long userProfileId(String userKey) {
    return jdbcTemplate.queryForObject(
        "select id from user_profile where email = ?", Long.class, userKey + "@example.com");
  }

  /** 테스트 사용자의 푸시 권한 상태를 조회한다. */
  private String pushPermissionStatus(String userKey) {
    return jdbcTemplate.queryForObject(
        "select push_permission_status from user_profile where email = ?",
        String.class,
        userKey + "@example.com");
  }

  /** 테스트 사용자의 푸시 권한 갱신 시각을 조회한다. */
  private LocalDateTime pushPermissionUpdatedAt(String userKey) {
    return jdbcTemplate.queryForObject(
        "select push_permission_updated_at from user_profile where email = ?",
        LocalDateTime.class,
        userKey + "@example.com");
  }

  /** 지정한 Expo Push Token 값의 저장 행 수를 조회한다. */
  private Integer tokenCount(String expoPushToken) {
    return jdbcTemplate.queryForObject(
        "select count(*) from user_push_token where expo_push_token = ?",
        Integer.class,
        expoPushToken);
  }
}
