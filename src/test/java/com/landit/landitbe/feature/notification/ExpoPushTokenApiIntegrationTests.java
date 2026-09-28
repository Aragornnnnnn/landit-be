// Expo Push Token 상태 관리 API의 인증과 저장 계약을 검증한다.

package com.landit.landitbe.feature.notification;

import static com.landit.landitbe.support.AuthenticatedJsonRequests.putJsonWithToken;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.landitbe.feature.notification.delivery.dto.PreparePushDeliveryCommand;
import com.landit.landitbe.feature.notification.delivery.service.PushDeliveryService;
import com.landit.landitbe.feature.notification.domain.NotificationType;
import com.landit.landitbe.feature.notification.token.dto.ExpoPushTokenUpdateRequest;
import com.landit.landitbe.feature.notification.token.dto.PushDeviceUpdateRequest;
import com.landit.landitbe.feature.notification.token.service.ExpoPushTokenService;
import com.landit.landitbe.feature.notification.token.service.PushDeviceService;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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

  @Autowired private PushDeviceService pushDeviceService;

  @Autowired private PushDeliveryService pushDeliveryService;

  private final ObjectMapper objectMapper = new ObjectMapper();

  /** 인증된 사용자는 PUT 요청으로 Expo Push Token을 등록하고 비활성화할 수 있다. */
  @DisplayName("인증된 사용자는 PUT 요청으로 Expo Push Token을 등록하고 비활성화할 수 있다.")
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
  @DisplayName("Expo Push Token을 활성화하면 사용자의 푸시 권한을 허용 상태로 기록한다.")
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
  @DisplayName("다른 사용자는 본인 소유가 아닌 Expo Push Token을 비활성화할 수 없다.")
  @Test
  void doesNotRevokeAnotherUsersExpoPushToken() throws Exception {
    String ownerAccessToken = login("expo-push-token-real-owner");
    String otherAccessToken = login("expo-push-token-other-user");
    String expoPushToken = "ExponentPushToken[owner-only-revoke]";
    registerToken(ownerAccessToken, expoPushToken);

    mockMvc
        .perform(
            putJsonWithToken(
                "/api/v1/me/expo-push-token",
                otherAccessToken,
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
  @DisplayName("인증되지 않은 요청은 Expo Push Token 상태를 변경할 수 없다.")
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
  @DisplayName("APNs나 FCM 형식의 Token은 Expo Push Token으로 저장할 수 없다.")
  @Test
  void rejectsNonExpoPushToken() throws Exception {
    String accessToken = login("expo-push-token-invalid-format");
    String nativePushToken = "native-apns-or-fcm-token";

    mockMvc
        .perform(
            putJsonWithToken(
                "/api/v1/me/expo-push-token",
                accessToken,
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
  @DisplayName("같은 신규 Token의 동시 PUT 요청은 모두 성공하고 하나의 행만 저장한다.")
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
  @DisplayName("사용자 프로필 권한 갱신에 실패하면 Expo Push Token 등록도 함께 롤백한다.")
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

  /** 같은 설치의 계정 전환과 Token 회전은 현재 계정의 활성 Token 하나만 남긴다. */
  @DisplayName("같은 설치의 계정 전환과 Token 회전은 현재 계정의 활성 Token 하나만 남긴다.")
  @Test
  void keepsOnlyCurrentAccountTokenForInstallation() throws Exception {
    String accountA = login("push-installation-account-a");
    String accountB = login("push-installation-account-b");
    UUID installationId = UUID.randomUUID();
    String oldToken = "ExponentPushToken[installation-old-token]";
    String newToken = "ExponentPushToken[installation-new-token]";

    updateDevice(accountA, installationId, oldToken, true);
    updateDevice(accountB, installationId, newToken, true);

    assertThat(tokenCount(oldToken)).isZero();
    assertThat(tokenOwner(newToken)).isEqualTo(userProfileId("push-installation-account-b"));
    assertThat(activeInstallationTokenCount(installationId)).isEqualTo(1);

    updateDevice(accountB, installationId, null, false);
    assertTokenStatus(newToken, "REVOKED");
  }

  /** 동일 Expo Token을 다른 설치가 등록하면 설치 소유권도 이동한다. */
  @DisplayName("동일 Expo Token을 다른 설치가 등록하면 설치 소유권도 이동한다.")
  @Test
  void movesInstallationOwnershipWithExpoToken() throws Exception {
    String accountA = login("push-token-reuse-account-a");
    String accountB = login("push-token-reuse-account-b");
    UUID oldInstallation = UUID.randomUUID();
    UUID newInstallation = UUID.randomUUID();
    String token = "ExponentPushToken[installation-reused-token]";

    updateDevice(accountA, oldInstallation, token, true);
    updateDevice(accountB, newInstallation, token, true);

    assertThat(activeInstallationTokenCount(oldInstallation)).isZero();
    assertThat(activeInstallationTokenCount(newInstallation)).isEqualTo(1);
    assertThat(tokenOwner(token)).isEqualTo(userProfileId("push-token-reuse-account-b"));
  }

  /** 설치 등록 시 다른 계정에 남은 구형 Token의 발송은 유지한다. */
  @DisplayName("설치 등록 시 다른 계정에 남은 구형 Token의 발송은 유지한다.")
  @Test
  void migratesMatchingLegacyTokenWithoutGlobalCutoff() throws Exception {
    String accountA = login("push-legacy-owner-a");
    String accountB = login("push-legacy-owner-b");
    String matchingToken = "ExponentPushToken[legacy-matching-token]";
    String unrelatedToken = "ExponentPushToken[legacy-unrelated-token]";
    UUID installationId = UUID.randomUUID();
    registerToken(accountA, matchingToken);
    registerToken(accountA, unrelatedToken);

    updateDevice(accountB, installationId, matchingToken, true);

    assertThat(tokenOwner(matchingToken)).isEqualTo(userProfileId("push-legacy-owner-b"));
    assertThat(activeInstallationTokenCount(installationId)).isEqualTo(1);
    assertTokenStatus(unrelatedToken, "ACTIVE");
  }

  /** 현재 구형 Token을 설치에 연결하고 같은 계정의 다른 구형 Token만 정리한다. */
  @DisplayName("현재 구형 Token을 설치에 연결하고 같은 계정의 다른 구형 Token만 정리한다.")
  @Test
  void revokesLegacyDuplicatesWhilePreservingOtherInstallationsAndAccounts() throws Exception {
    String account = login("push-legacy-cleanup-owner");
    final String otherAccount = login("push-legacy-cleanup-other");
    final UUID installationId = UUID.randomUUID();
    UUID otherInstallationId = UUID.randomUUID();
    String currentToken = "ExponentPushToken[legacy-cleanup-current]";
    String oldToken = "ExponentPushToken[legacy-cleanup-old]";
    String otherDeviceToken = "ExponentPushToken[legacy-cleanup-other-device]";
    final String otherAccountToken = "ExponentPushToken[legacy-cleanup-other-account]";
    updateDevice(account, otherInstallationId, otherDeviceToken, true);
    registerToken(account, oldToken);
    registerToken(account, currentToken);
    registerToken(otherAccount, otherAccountToken);

    updateDevice(account, installationId, currentToken, true);
    updateDevice(account, installationId, currentToken, true);

    assertTokenStatus(currentToken, "ACTIVE");
    assertTokenStatus(oldToken, "REVOKED");
    assertTokenStatus(otherDeviceToken, "ACTIVE");
    assertTokenStatus(otherAccountToken, "ACTIVE");
    assertThat(activeInstallationTokenCount(installationId)).isEqualTo(1);
    assertThat(activeInstallationTokenCount(otherInstallationId)).isEqualTo(1);

    // 아직 전환하지 않은 다른 기기도 설치 API로 등록하면 다시 수신할 수 있다.
    UUID returningInstallationId = UUID.randomUUID();
    updateDevice(account, returningInstallationId, oldToken, true);
    assertTokenStatus(oldToken, "ACTIVE");
    assertTokenStatus(currentToken, "ACTIVE");
    assertThat(activeInstallationTokenCount(returningInstallationId)).isEqualTo(1);
  }

  /** 새 Token을 등록해도 같은 계정의 구형 Token을 정리한다. */
  @DisplayName("새 Token을 등록해도 같은 계정의 구형 Token을 정리한다.")
  @Test
  void revokesLegacyTokensWhenRegisteringNewToken() throws Exception {
    String account = login("push-legacy-new-token-owner");
    String oldToken = "ExponentPushToken[legacy-new-token-old]";
    String newToken = "ExponentPushToken[legacy-new-token-current]";
    UUID installationId = UUID.randomUUID();
    registerToken(account, oldToken);

    updateDevice(account, installationId, newToken, true);

    assertTokenStatus(oldToken, "REVOKED");
    assertTokenStatus(newToken, "ACTIVE");
    assertThat(activeInstallationTokenCount(installationId)).isEqualTo(1);
  }

  /** 푸시 비활성 요청은 현재 Token을 등록하지 않으므로 구형 Token을 정리하지 않는다. */
  @DisplayName("푸시 비활성 요청은 현재 Token을 등록하지 않으므로 구형 Token을 정리하지 않는다.")
  @Test
  void preservesLegacyTokensWhenDisablingInstallation() throws Exception {
    String account = login("push-legacy-disabled-owner");
    String legacyToken = "ExponentPushToken[legacy-disabled-old]";
    String installedToken = "ExponentPushToken[legacy-disabled-installed]";
    UUID installationId = UUID.randomUUID();
    updateDevice(account, installationId, installedToken, true);
    registerToken(account, legacyToken);

    updateDevice(account, installationId, null, false);

    assertTokenStatus(installedToken, "REVOKED");
    assertTokenStatus(legacyToken, "ACTIVE");
  }

  /** 설치 등록이 실패하면 구형 Token 비활성화도 함께 롤백한다. */
  @DisplayName("설치 등록이 실패하면 구형 Token 비활성화도 함께 롤백한다.")
  @Test
  void rollsBackLegacyCleanupWhenInstallationRegistrationFails() throws Exception {
    String userKey = "push-legacy-cleanup-rollback";
    String account = login(userKey);
    String oldToken = "ExponentPushToken[legacy-cleanup-rollback-old]";
    String newToken = "ExponentPushToken[legacy-cleanup-rollback-new]";
    registerToken(account, oldToken);
    Long ownerId = userProfileId(userKey);
    jdbcTemplate.update("update user_profile set status = 'WITHDRAWN' where id = ?", ownerId);
    UUID installationId = UUID.randomUUID();
    PushDeviceUpdateRequest request = new PushDeviceUpdateRequest(AppPlatform.IOS, newToken, true);

    assertThatThrownBy(() -> pushDeviceService.update(ownerId, installationId, request))
        .isInstanceOf(UserProfileException.class);

    assertTokenStatus(oldToken, "ACTIVE");
    assertThat(tokenCount(newToken)).isZero();
    assertThat(activeInstallationTokenCount(installationId)).isZero();
  }

  /** 알림을 거부한 새 계정으로 전환해도 이전 계정에 발송하지 않는다. */
  @DisplayName("알림을 거부한 새 계정으로 전환해도 이전 계정에 발송하지 않는다.")
  @Test
  void accountSwitchWithPushDisabledRevokesPreviousOwner() throws Exception {
    String accountA = login("push-disabled-switch-account-a");
    String accountB = login("push-disabled-switch-account-b");
    UUID installationId = UUID.randomUUID();
    String token = "ExponentPushToken[disabled-switch-token]";

    updateDevice(accountA, installationId, token, true);
    updateDevice(accountB, installationId, null, false);

    assertTokenStatus(token, "REVOKED");
    assertThat(tokenOwner(token)).isEqualTo(userProfileId("push-disabled-switch-account-b"));
  }

  /** 예약 후 계정이 바뀌면 이전 계정의 딥링크 발송을 건너뛴다. */
  @DisplayName("예약 후 계정이 바뀌면 이전 계정의 딥링크 발송을 건너뛴다.")
  @Test
  void skipsQueuedDeliveryForPreviousAccount() throws Exception {
    String accountA = login("push-queued-account-a");
    String accountB = login("push-queued-account-b");
    UUID installationId = UUID.randomUUID();
    String token = "ExponentPushToken[queued-account-switch]";
    updateDevice(accountA, installationId, token, true);
    Long tokenId =
        jdbcTemplate.queryForObject(
            "select id from user_push_token where expo_push_token = ?", Long.class, token);
    Long userA = userProfileId("push-queued-account-a");
    updateDevice(accountB, installationId, token, true);

    assertThat(
            pushDeliveryService.prepare(
                new PreparePushDeliveryCommand(
                    userA,
                    tokenId,
                    NotificationType.CONTINUE_EXPRESSION,
                    "lan591:previous:" + installationId,
                    "표현",
                    "본문",
                    "/expressions/scenario/1/1")))
        .isEmpty();
    assertThat(tokenOwner(token)).isEqualTo(userProfileId("push-queued-account-b"));
  }

  /** 설치 API는 인증과 활성 Expo Token 형식을 검증한다. */
  @DisplayName("설치 API는 인증과 활성 Expo Token 형식을 검증한다.")
  @Test
  void validatesPushDeviceUpdate() throws Exception {
    UUID installationId = UUID.randomUUID();
    String path = "/api/v1/me/push-devices/" + installationId;
    mockMvc
        .perform(
            put(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"platform\":\"IOS\",\"pushEnabled\":false}"))
        .andExpect(status().isUnauthorized());
    String accessToken = login("push-device-invalid-token");
    mockMvc
        .perform(putJsonWithToken(path, accessToken, "{\"platform\":\"IOS\",\"pushEnabled\":true}"))
        .andExpect(status().isBadRequest());
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
            putJsonWithToken(
                "/api/v1/me/expo-push-token",
                accessToken,
                objectMapper.writeValueAsString(
                    new ExpoPushTokenUpdateRequest(platform, expoPushToken, enabled))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));
  }

  private void updateDevice(
      String accessToken, UUID installationId, String expoPushToken, boolean enabled)
      throws Exception {
    mockMvc
        .perform(
            putJsonWithToken(
                "/api/v1/me/push-devices/" + installationId,
                accessToken,
                objectMapper.writeValueAsString(
                    new com.landit.landitbe.feature.notification.token.dto.PushDeviceUpdateRequest(
                        AppPlatform.IOS, expoPushToken, enabled))))
        .andExpect(status().isOk());
  }

  private Long tokenOwner(String token) {
    return jdbcTemplate.queryForObject(
        "select user_profile_id from user_push_token where expo_push_token = ?", Long.class, token);
  }

  private Integer activeInstallationTokenCount(UUID installationId) {
    return jdbcTemplate.queryForObject(
        "select count(*) from user_push_token where installation_id = ? and status = 'ACTIVE'",
        Integer.class,
        installationId);
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
