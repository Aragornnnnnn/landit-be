// 사용자 일일 알람 API의 저장·변경·검증·접근 제어와 동시 등록을 검증한다.

package com.landit.landitbe.feature.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.landitbe.feature.profile.dto.UserAlarmResponse;
import com.landit.landitbe.feature.profile.dto.UserAlarmUpdateRequest;
import com.landit.landitbe.feature.profile.service.UserAlarmService;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
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
import org.springframework.test.web.servlet.ResultActions;

/** 사용자 일일 알람 API의 저장·변경·검증·접근 제어와 동시 등록을 검증한다. */
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@TestPropertySource(
    properties = {
      "landit.auth.oidc.fake-enabled=true",
      "landit.auth.token.secret=landit-test-token-secret-that-is-long-enough"
    })
class UserAlarmApiIntegrationTests {

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private UserAlarmService userAlarmService;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void returnsDisabledWithoutCreatingAlarmForUnconfiguredUser() throws Exception {
    String userKey = "alarm-default";
    String token = login(userKey);

    readAlarm(token)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.enabled").value(false))
        .andExpect(jsonPath("$.data.time").value(org.hamcrest.Matchers.nullValue()));
    assertThat(alarmCount(userId(userKey))).isZero();
  }

  @Test
  void createsUpdatesDisablesAndReenablesOneAlarm() throws Exception {
    String userKey = "alarm-lifecycle";
    String token = login(userKey);

    for (UserAlarmUpdateRequest request :
        List.of(
            new UserAlarmUpdateRequest("07:30", true),
            new UserAlarmUpdateRequest("07:30", true),
            new UserAlarmUpdateRequest("00:00", true),
            new UserAlarmUpdateRequest("23:59", false),
            new UserAlarmUpdateRequest("23:59", true))) {
      updateAlarm(token, objectMapper.writeValueAsString(request))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.time").value(request.time()))
          .andExpect(jsonPath("$.data.enabled").value(request.enabled()));
      readAlarm(token)
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.time").value(request.time()))
          .andExpect(jsonPath("$.data.enabled").value(request.enabled()));
    }
    assertThat(alarmCount(userId(userKey))).isEqualTo(1);
    assertThat(
            jdbcTemplate.queryForObject(
                "select alarm_time from user_alarm where user_profile_id = ?",
                LocalTime.class,
                userId(userKey)))
        .isEqualTo(LocalTime.of(23, 59));
  }

  @Test
  void rejectsInvalidSettingsWithoutChangingSavedAlarm() throws Exception {
    String token = login("alarm-invalid");
    updateAlarm(token, "{\"time\":\"07:30\",\"enabled\":true}").andExpect(status().isOk());
    List<String> invalidRequests = new ArrayList<>();
    for (String time :
        List.of("", "7:30", "24:00", "23:60", "-1:00", "07:30:00", "07:30Z", " 07:30", "07:30 ")) {
      invalidRequests.add(objectMapper.writeValueAsString(new UserAlarmUpdateRequest(time, true)));
    }
    invalidRequests.addAll(
        List.of(
            "{}",
            "{\"enabled\":true}",
            "{\"time\":\"07:30\"}",
            "{\"time\":null,\"enabled\":false}",
            "{\"time\":\"07:30\",\"enabled\":null}"));
    for (String content : invalidRequests) {
      updateAlarm(token, content)
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }
    readAlarm(token)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.time").value("07:30"))
        .andExpect(jsonPath("$.data.enabled").value(true));
  }

  @Test
  void isolatesSettingsBetweenUsers() throws Exception {
    String ownerToken = login("alarm-owner");
    String otherToken = login("alarm-other");
    updateAlarm(ownerToken, "{\"time\":\"07:30\",\"enabled\":true}").andExpect(status().isOk());
    updateAlarm(otherToken, "{\"time\":\"09:00\",\"enabled\":false}").andExpect(status().isOk());

    readAlarm(ownerToken)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.time").value("07:30"))
        .andExpect(jsonPath("$.data.enabled").value(true));
    readAlarm(otherToken)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.time").value("09:00"))
        .andExpect(jsonPath("$.data.enabled").value(false));
  }

  @Test
  void rejectsUnauthenticatedAndWithdrawnUsers() throws Exception {
    mockMvc.perform(get("/api/v1/me/alarm")).andExpect(status().isUnauthorized());
    mockMvc
        .perform(
            put("/api/v1/me/alarm")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"time\":\"07:30\",\"enabled\":true}"))
        .andExpect(status().isUnauthorized());

    String userKey = "alarm-withdrawn";
    String token = login(userKey);
    jdbcTemplate.update(
        "update user_profile set status = 'WITHDRAWN' where id = ?", userId(userKey));
    readAlarm(token).andExpect(status().isUnauthorized());
    updateAlarm(token, "{\"time\":\"07:30\",\"enabled\":true}")
        .andExpect(status().isUnauthorized());
    assertThat(alarmCount(userId(userKey))).isZero();
  }

  @Test
  void concurrentFirstWritesStoreOneCompleteSetting() throws Exception {
    String userKey = "alarm-concurrent";
    login(userKey);
    Long userId = userId(userKey);
    int requestCount = 4;
    CountDownLatch ready = new CountDownLatch(requestCount);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<UserAlarmResponse>> results = new ArrayList<>();
    List<UserAlarmResponse> expectedSettings = new ArrayList<>();
    try (var executor = Executors.newFixedThreadPool(requestCount)) {
      for (int index = 0; index < requestCount; index++) {
        UserAlarmUpdateRequest request = new UserAlarmUpdateRequest("07:3" + index, index % 2 == 0);
        expectedSettings.add(new UserAlarmResponse(request.time(), request.enabled()));
        results.add(
            executor.submit(
                () -> {
                  ready.countDown();
                  if (!start.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("동시 요청 시작 대기 시간을 초과했습니다.");
                  }
                  return userAlarmService.updateAlarm(userId, request);
                }));
      }
      assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
      start.countDown();
      for (int index = 0; index < requestCount; index++) {
        assertThat(results.get(index).get(10, TimeUnit.SECONDS))
            .isEqualTo(expectedSettings.get(index));
      }
    }
    assertThat(alarmCount(userId)).isEqualTo(1);
    assertThat(userAlarmService.getAlarm(userId)).isIn(expectedSettings);
  }

  @Test
  void documentsAlarmApisAndValidation() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/v1/me/alarm'].get.responses['200']").exists())
        .andExpect(jsonPath("$.paths['/api/v1/me/alarm'].get.responses['401']").exists())
        .andExpect(jsonPath("$.paths['/api/v1/me/alarm'].put.responses['200']").exists())
        .andExpect(jsonPath("$.paths['/api/v1/me/alarm'].put.responses['400']").exists())
        .andExpect(jsonPath("$.paths['/api/v1/me/alarm'].put.responses['401']").exists())
        .andExpect(
            jsonPath("$.components.schemas.UserAlarmUpdateRequest.required")
                .value(org.hamcrest.Matchers.containsInAnyOrder("time", "enabled")))
        .andExpect(
            jsonPath("$.components.schemas.UserAlarmUpdateRequest.properties.time.pattern")
                .exists())
        .andExpect(
            jsonPath("$.components.schemas.UserAlarmResponse.properties.time.type")
                .value(org.hamcrest.Matchers.containsInAnyOrder("string", "null")));
  }

  private ResultActions readAlarm(String token) throws Exception {
    return mockMvc.perform(
        get("/api/v1/me/alarm").header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
  }

  private ResultActions updateAlarm(String token, String content) throws Exception {
    return mockMvc.perform(
        put("/api/v1/me/alarm")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(content));
  }

  private Long userId(String userKey) {
    return jdbcTemplate.queryForObject(
        "select id from user_profile where email = ?", Long.class, userKey + "@example.com");
  }

  private Integer alarmCount(Long userId) {
    return jdbcTemplate.queryForObject(
        "select count(*) from user_alarm where user_profile_id = ?", Integer.class, userId);
  }

  private String login(String userKey) throws Exception {
    String nonce = UUID.randomUUID().toString();
    var result =
        mockMvc
            .perform(
                post("/api/v1/auth/social-login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"provider":"GOOGLE","idToken":"%s|%s@example.com|%s|%s","nonce":"%s"}
                        """
                            .formatted(userKey, userKey, userKey, nonce, nonce)))
            .andExpect(status().isOk())
            .andReturn();
    return objectMapper
        .readTree(result.getResponse().getContentAsByteArray())
        .get("data")
        .get("accessToken")
        .asText();
  }
}
