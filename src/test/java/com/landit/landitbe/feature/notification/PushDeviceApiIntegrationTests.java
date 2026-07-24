// 푸시 설치 상태 동기화 API의 저장, 검증, 인증 계약을 검증한다.

package com.landit.landitbe.feature.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.landitbe.feature.notification.dto.PushDeviceSyncRequest;
import com.landit.landitbe.shared.domain.AppPlatform;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** 푸시 설치 상태 동기화 API의 저장, 검증, 인증 계약을 검증한다. */
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@TestPropertySource(
    properties = {
      "landit.auth.oidc.fake-enabled=true",
      "landit.auth.token.secret=landit-test-token-secret-that-is-long-enough"
    })
class PushDeviceApiIntegrationTests {

  private static final UUID INSTALLATION_ID =
      UUID.fromString("550e8400-e29b-41d4-a716-446655440010");
  private static final String EXPO_PUSH_TOKEN = "ExponentPushToken[api-device-token]";

  @Autowired private MockMvc mockMvc;

  @Autowired private JdbcTemplate jdbcTemplate;

  private final ObjectMapper objectMapper = new ObjectMapper();

  /** Token이 있는 활성 설치를 등록하고 원문 Token을 제외한 동기화 결과를 반환한다. */
  @Test
  void synchronizesEnabledPushDevice() throws Exception {
    String userKey = "push-device-enabled";
    String accessToken = login(userKey);

    mockMvc
        .perform(
            pushDeviceRequest(
                accessToken,
                INSTALLATION_ID,
                new PushDeviceSyncRequest(AppPlatform.IOS, true, EXPO_PUSH_TOKEN)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.installationId").value(INSTALLATION_ID.toString()))
        .andExpect(jsonPath("$.data.platform").value("IOS"))
        .andExpect(jsonPath("$.data.pushEnabled").value(true))
        .andExpect(jsonPath("$.data.pushTokenRegistered").value(true))
        .andExpect(jsonPath("$.data.updatedAt").isString())
        .andExpect(jsonPath("$.data.expoPushToken").doesNotExist())
        .andExpect(jsonPath("$.error").value(nullValue()));

    assertThat(pushDevicesByEmail(email(userKey)))
        .singleElement()
        .satisfies(
            device -> {
              assertThat(device.get("installation_id")).isEqualTo(INSTALLATION_ID);
              assertThat(device.get("platform")).isEqualTo("IOS");
              assertThat(device.get("push_enabled")).isEqualTo(true);
              assertThat(device.get("token")).isEqualTo(EXPO_PUSH_TOKEN);
              assertThat(device.get("status")).isEqualTo("ACTIVE");
            });
  }

  /** 같은 설치를 반복 동기화하면 행을 추가하지 않고 현재 상태로 갱신한다. */
  @Test
  void synchronizingSameInstallationIsIdempotent() throws Exception {
    String userKey = "push-device-idempotent";
    String accessToken = login(userKey);

    synchronize(accessToken, INSTALLATION_ID, AppPlatform.IOS, true, EXPO_PUSH_TOKEN);
    synchronize(accessToken, INSTALLATION_ID, AppPlatform.ANDROID, false, EXPO_PUSH_TOKEN);

    assertThat(pushDevicesByEmail(email(userKey)))
        .singleElement()
        .satisfies(
            device -> {
              assertThat(device.get("platform")).isEqualTo("ANDROID");
              assertThat(device.get("push_enabled")).isEqualTo(false);
              assertThat(device.get("token")).isEqualTo(EXPO_PUSH_TOKEN);
            });
  }

  /** 같은 설치에서 사용자가 바뀌면 현재 인증 사용자에게 설치 소유권을 이전한다. */
  @Test
  void movesInstallationOwnershipToCurrentUser() throws Exception {
    String previousUserKey = "push-device-previous";
    String currentUserKey = "push-device-current";

    synchronize(login(previousUserKey), INSTALLATION_ID, AppPlatform.IOS, true, EXPO_PUSH_TOKEN);
    synchronize(login(currentUserKey), INSTALLATION_ID, AppPlatform.IOS, true, EXPO_PUSH_TOKEN);

    assertThat(pushDevicesByEmail(email(previousUserKey))).isEmpty();
    assertThat(pushDevicesByEmail(email(currentUserKey))).hasSize(1);
  }

  /** 다른 설치가 사용 중인 Expo Token은 기존 연결을 해제하고 현재 설치로 옮긴다. */
  @Test
  void movesDuplicateExpoTokenToCurrentInstallation() throws Exception {
    String accessToken = login("push-device-token-owner");
    UUID previousInstallationId = UUID.fromString("550e8400-e29b-41d4-a716-446655440011");

    synchronize(accessToken, previousInstallationId, AppPlatform.IOS, true, EXPO_PUSH_TOKEN);
    synchronize(accessToken, INSTALLATION_ID, AppPlatform.ANDROID, true, EXPO_PUSH_TOKEN);

    List<Map<String, Object>> devices =
        jdbcTemplate.queryForList(
            """
            select installation_id, token, status
            from user_push_token
            where installation_id in (?, ?)
            order by installation_id
            """,
            previousInstallationId,
            INSTALLATION_ID);
    assertThat(devices).hasSize(2);
    assertThat(devices)
        .filteredOn(device -> previousInstallationId.equals(device.get("installation_id")))
        .singleElement()
        .satisfies(
            device -> {
              assertThat(device.get("token")).isNull();
              assertThat(device.get("status")).isNull();
            });
    assertThat(devices)
        .filteredOn(device -> INSTALLATION_ID.equals(device.get("installation_id")))
        .singleElement()
        .satisfies(device -> assertThat(device.get("token")).isEqualTo(EXPO_PUSH_TOKEN));
  }

  /** 알림 비활성 요청은 Token 없이 저장할 수 있다. */
  @Test
  void synchronizesDisabledPushDeviceWithoutToken() throws Exception {
    String accessToken = login("push-device-disabled");

    mockMvc
        .perform(
            pushDeviceRequest(
                accessToken,
                INSTALLATION_ID,
                new PushDeviceSyncRequest(AppPlatform.ANDROID, false, null)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.pushEnabled").value(false))
        .andExpect(jsonPath("$.data.pushTokenRegistered").value(false));
  }

  /** 알림 활성 요청에 Expo Token이 없으면 검증 오류를 반환한다. */
  @Test
  void rejectsEnabledPushDeviceWithoutToken() throws Exception {
    String accessToken = login("push-device-invalid");

    mockMvc
        .perform(
            pushDeviceRequest(
                accessToken,
                INSTALLATION_ID,
                new PushDeviceSyncRequest(AppPlatform.IOS, true, null)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
  }

  /** 지원하지 않는 플랫폼과 잘못된 설치 ID는 검증 오류를 반환한다. */
  @Test
  void rejectsInvalidPlatformAndInstallationId() throws Exception {
    String accessToken = login("push-device-invalid-fields");

    mockMvc
        .perform(
            put("/api/v1/me/push-devices/{installationId}", INSTALLATION_ID)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "platform": "WINDOWS",
                      "pushEnabled": false,
                      "expoPushToken": null
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    mockMvc
        .perform(
            put("/api/v1/me/push-devices/not-a-uuid")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "platform": "IOS",
                      "pushEnabled": false,
                      "expoPushToken": null
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
  }

  /** 접근 토큰 없이 설치 상태를 동기화할 수 없다. */
  @Test
  void rejectsRequestWithoutAccessToken() throws Exception {
    mockMvc
        .perform(
            put("/api/v1/me/push-devices/{installationId}", INSTALLATION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "platform": "IOS",
                      "pushEnabled": false,
                      "expoPushToken": null
                    }
                    """))
        .andExpect(status().isUnauthorized());
  }

  /** OpenAPI 문서에 설치 상태 동기화 성공·검증·인증 응답을 노출한다. */
  @Test
  void openApiDocsDescribePushDeviceSynchronization() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.paths['/api/v1/me/push-devices/{installationId}'].put.responses['200']")
                .exists())
        .andExpect(
            jsonPath("$.paths['/api/v1/me/push-devices/{installationId}'].put.responses['400']")
                .exists())
        .andExpect(
            jsonPath("$.paths['/api/v1/me/push-devices/{installationId}'].put.responses['401']")
                .exists())
        .andExpect(
            jsonPath("$.paths['/api/v1/me/push-devices/{installationId}'].put.responses['500']")
                .exists());
  }

  /** 인증 정보와 설치 상태를 포함한 동기화 요청을 만든다. */
  private MockHttpServletRequestBuilder pushDeviceRequest(
      String accessToken, UUID installationId, PushDeviceSyncRequest request) throws Exception {
    return put("/api/v1/me/push-devices/{installationId}", installationId)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsBytes(request));
  }

  /** 성공 응답을 검증하며 설치 상태를 동기화한다. */
  private void synchronize(
      String accessToken,
      UUID installationId,
      AppPlatform platform,
      boolean pushEnabled,
      String expoPushToken)
      throws Exception {
    mockMvc
        .perform(
            pushDeviceRequest(
                accessToken,
                installationId,
                new PushDeviceSyncRequest(platform, pushEnabled, expoPushToken)))
        .andExpect(status().isOk());
  }

  /** 이메일에 연결된 Push Device를 조회한다. */
  private List<Map<String, Object>> pushDevicesByEmail(String email) {
    return jdbcTemplate.queryForList(
        """
        select
            user_push_token.installation_id,
            user_push_token.platform,
            user_push_token.push_enabled,
            user_push_token.token,
            user_push_token.status
        from user_push_token
        join user_profile on user_profile.id = user_push_token.user_profile_id
        where user_profile.email = ?
        order by user_push_token.id
        """,
        email);
  }

  /** 테스트 식별자로 이메일을 생성한다. */
  private String email(String userKey) {
    return userKey + "@example.com";
  }

  /** 테스트 식별자를 사용하는 가짜 소셜 로그인으로 접근 토큰을 발급한다. */
  private String login(String userKey) throws Exception {
    String nonce = userKey + "-nonce";
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/auth/social-login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "provider":"GOOGLE",
                          "idToken":"%s|%s|%s|%s",
                          "nonce":"%s"
                        }
                        """
                            .formatted(userKey, email(userKey), userKey, nonce, nonce)))
            .andExpect(status().isOk())
            .andReturn();
    JsonNode body = objectMapper.readTree(result.getResponse().getContentAsByteArray());
    return body.get("data").get("accessToken").asText();
  }
}
