// NPS 제출 API의 저장, 검증, 인증 정책을 검증한다.

package com.landit.landitbe.nps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
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

/** NPS 제출 API의 저장, 검증, 인증 정책을 검증한다. */
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@TestPropertySource(
    properties = {
      "landit.auth.oidc.fake-enabled=true",
      "landit.auth.token.secret=landit-test-token-secret-that-is-long-enough"
    })
class NpsApiIntegrationTests {

  @Autowired private MockMvc mockMvc;

  @Autowired private JdbcTemplate jdbcTemplate;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void submitNpsStoresAuthenticatedUsersScoreAndOpinion() throws Exception {
    String email = "nps-normal@example.com";
    String accessToken = login("nps-normal", email, "Nps User", "nps-normal-nonce");

    mockMvc
        .perform(
            npsRequest(
                accessToken,
                """
                {
                  "score": 3,
                  "opinionText": "피드백은 좋았어요."
                }
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data").value(nullValue()))
        .andExpect(jsonPath("$.error").value(nullValue()));

    List<Map<String, Object>> responses = responsesByEmail(email);
    assertThat(responses)
        .singleElement()
        .satisfies(
            response -> {
              assertThat(response.get("score")).isEqualTo(3);
              assertThat(response.get("opinion_text")).isEqualTo("피드백은 좋았어요.");
            });
  }

  @Test
  void submitNpsStoresMissingEmptyAndBlankOpinionsAsNull() throws Exception {
    String email = "nps-blank@example.com";
    String accessToken = login("nps-blank", email, "Blank User", "nps-blank-nonce");

    submit(accessToken, "{\"score\":3}");
    submit(accessToken, "{\"score\":3,\"opinionText\":null}");
    submit(accessToken, "{\"score\":3,\"opinionText\":\"\"}");
    submit(accessToken, "{\"score\":3,\"opinionText\":\"   \"}");

    assertThat(responsesByEmail(email))
        .hasSize(4)
        .allSatisfy(response -> assertThat(response.get("opinion_text")).isNull());
  }

  @Test
  void submitNpsStoresEveryRepeatedSubmissionSeparately() throws Exception {
    String email = "nps-repeat@example.com";
    String accessToken = login("nps-repeat", email, "Repeat User", "nps-repeat-nonce");
    String request = "{\"score\":4,\"opinionText\":\"같은 의견\"}";

    submit(accessToken, request);
    submit(accessToken, request);

    assertThat(responsesByEmail(email))
        .hasSize(2)
        .allSatisfy(
            response -> {
              assertThat(response.get("score")).isEqualTo(4);
              assertThat(response.get("opinion_text")).isEqualTo("같은 의견");
            });
  }

  @Test
  void submitNpsAcceptsMinimumAndMaximumScores() throws Exception {
    String email = "nps-boundary@example.com";
    String accessToken = login("nps-boundary", email, "Boundary User", "nps-boundary-nonce");

    submit(accessToken, "{\"score\":1}");
    submit(accessToken, "{\"score\":5}");

    assertThat(responsesByEmail(email))
        .extracting(response -> response.get("score"))
        .containsExactlyInAnyOrder(1, 5);
  }

  @Test
  void submitNpsRejectsMissingOrOutOfRangeScore() throws Exception {
    String accessToken =
        login("nps-invalid", "nps-invalid@example.com", "Invalid User", "nps-invalid-nonce");

    for (String request : List.of("{}", "{\"score\":0}", "{\"score\":6}")) {
      mockMvc
          .perform(npsRequest(accessToken, request))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }
  }

  @Test
  void submitNpsRejectsRequestWithoutAccessToken() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/nps").contentType(MediaType.APPLICATION_JSON).content("{\"score\":3}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void openApiDocsDescribeNpsSubmission() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/v1/nps'].post.responses['201']").exists())
        .andExpect(jsonPath("$.paths['/api/v1/nps'].post.responses['400']").exists())
        .andExpect(jsonPath("$.paths['/api/v1/nps'].post.responses['401']").exists());
  }

  /** NPS 제출 요청을 만든다. */
  private MockHttpServletRequestBuilder npsRequest(String accessToken, String content) {
    return post("/api/v1/nps")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content(content);
  }

  /** 성공 응답을 검증하며 NPS를 제출한다. */
  private void submit(String accessToken, String content) throws Exception {
    mockMvc.perform(npsRequest(accessToken, content)).andExpect(status().isCreated());
  }

  /** 이메일에 연결된 NPS 응답을 조회한다. */
  private List<Map<String, Object>> responsesByEmail(String email) {
    return jdbcTemplate.queryForList(
        """
                SELECT nps_response.score, nps_response.opinion_text
                FROM nps_response
                JOIN user_profile ON user_profile.id = nps_response.user_profile_id
                WHERE user_profile.email = ?
                ORDER BY nps_response.id
        """,
        email);
  }

  /** 가짜 소셜 로그인으로 접근 토큰을 발급한다. */
  private String login(String sub, String email, String nickname, String nonce) throws Exception {
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
                            .formatted(sub, email, nickname, nonce, nonce)))
            .andExpect(status().isOk())
            .andReturn();
    JsonNode body = objectMapper.readTree(result.getResponse().getContentAsByteArray());
    return body.get("data").get("accessToken").asText();
  }
}
