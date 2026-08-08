// develop 외 환경에서 관리자 시나리오 세션 API가 등록되지 않는지 검증한다.

package com.landit.landitbe.feature.session;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

/** Develop 외 환경에서 관리자 시나리오 세션 API가 등록되지 않는지 검증한다. */
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

  private final ObjectMapper objectMapper = new ObjectMapper();

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
