// develop 외 환경의 관리자 시나리오 세션 API 미등록을 검증한다.

package com.landit.landitbe.feature.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.landitbe.feature.session.service.AdminScenarioSessionStartService;
import com.landit.landitbe.feature.session.service.ScenarioSessionStartService;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Develop 외 환경의 관리자 시나리오 세션 API 미등록을 검증한다. */
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@TestPropertySource(
    properties = {
      "landit.auth.oidc.fake-enabled=true",
      "landit.auth.token.secret=landit-test-token-secret-that-is-long-enough"
    })
class AdminScenarioSessionApiUnavailableIntegrationTests {

  @Autowired private MockMvc mockMvc;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private ApplicationContext applicationContext;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @DisplayName("develop 외 환경에서는 관리자 시나리오 세션 API를 등록하지 않는다.")
  @Test
  void doesNotRegisterAdminScenarioSessionApiOutsideDevelopProfile() throws Exception {
    String accessToken = loginAdmin();

    mockMvc
        .perform(
            post("/api/v1/admin/scenarios/{scenarioId}/sessions", 702)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isNotFound());

    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.paths['/api/v1/admin/scenarios/{scenarioId}/sessions']").doesNotExist());
  }

  @DisplayName("공통 세션 Service의 관리자 진행 제한 우회 메서드는 패키지 내부에서만 사용한다.")
  @Test
  void keepsAdminProgressionBypassPackagePrivate() throws NoSuchMethodException {
    Method method =
        ScenarioSessionStartService.class.getDeclaredMethod(
            "startScenarioSessionWithoutProgression", long.class, long.class);

    int visibilityModifiers =
        method.getModifiers() & (Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE);
    assertEquals(0, visibilityModifiers);
  }

  @DisplayName("develop 외 환경에서는 관리자 세션 시작 Service를 등록하지 않는다.")
  @Test
  void doesNotRegisterAdminScenarioSessionStartServiceOutsideDevelopProfile() {
    assertTrue(applicationContext.getBeansOfType(AdminScenarioSessionStartService.class).isEmpty());
  }

  private String loginAdmin() throws Exception {
    String userKey = "admin-session-unavailable-" + UUID.randomUUID();
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
                          "idToken":"%s|%s@example.com|관리자|%s",
                          "nonce":"%s"
                        }
                        """
                            .formatted(userKey, userKey, nonce, nonce)))
            .andExpect(status().isOk())
            .andReturn();
    JsonNode body = objectMapper.readTree(result.getResponse().getContentAsByteArray());
    Long userProfileId =
        jdbcTemplate.queryForObject(
            "SELECT id FROM user_profile WHERE email = ?", Long.class, userKey + "@example.com");
    jdbcTemplate.update("UPDATE user_profile SET role = 'ADMIN' WHERE id = ?", userProfileId);
    return body.get("data").get("accessToken").asText();
  }
}
