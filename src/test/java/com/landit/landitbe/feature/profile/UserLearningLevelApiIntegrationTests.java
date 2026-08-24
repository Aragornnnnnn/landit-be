// 사용자 학습 수준 API의 인증과 저장 계약을 검증한다.

package com.landit.landitbe.feature.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
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

/** 사용자 학습 수준 API의 인증과 저장 계약을 검증한다. */
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@TestPropertySource(
    properties = {
      "landit.auth.oidc.fake-enabled=true",
      "landit.auth.token.secret=landit-test-token-secret-that-is-long-enough"
    })
class UserLearningLevelApiIntegrationTests {

  @Autowired private MockMvc mockMvc;

  @Autowired private JdbcTemplate jdbcTemplate;

  private final ObjectMapper objectMapper = new ObjectMapper();

  /** 인증된 사용자가 선택한 학습 수준을 저장한다. */
  @Test
  void storesAuthenticatedUsersLearningLevel() throws Exception {
    String userKey = "learning-level-owner";
    String accessToken = login(userKey);

    updateLearningLevel(accessToken, 3);

    assertThat(learningLevel(userKey)).isEqualTo(3);
  }

  /** 같은 사용자가 학습 수준을 다시 설정하면 기존 값을 덮어쓴다. */
  @Test
  void overwritesExistingLearningLevel() throws Exception {
    String userKey = "learning-level-overwrite";
    String accessToken = login(userKey);

    updateLearningLevel(accessToken, 2);
    updateLearningLevel(accessToken, 5);

    assertThat(learningLevel(userKey)).isEqualTo(5);
  }

  /** 학습 수준의 최솟값과 최댓값을 저장할 수 있다. */
  @Test
  void storesMinimumAndMaximumLearningLevels() throws Exception {
    String minimumUserKey = "learning-level-minimum";
    String maximumUserKey = "learning-level-maximum";

    updateLearningLevel(login(minimumUserKey), 1);
    updateLearningLevel(login(maximumUserKey), 5);

    assertThat(learningLevel(minimumUserKey)).isEqualTo(1);
    assertThat(learningLevel(maximumUserKey)).isEqualTo(5);
  }

  /** 한 사용자의 학습 수준 변경은 다른 사용자의 값을 변경하지 않는다. */
  @Test
  void doesNotChangeAnotherUsersLearningLevel() throws Exception {
    String ownerKey = "learning-level-isolated-owner";
    String otherKey = "learning-level-isolated-other";
    String ownerAccessToken = login(ownerKey);
    String otherAccessToken = login(otherKey);
    updateLearningLevel(otherAccessToken, 4);

    updateLearningLevel(ownerAccessToken, 2);

    assertThat(learningLevel(ownerKey)).isEqualTo(2);
    assertThat(learningLevel(otherKey)).isEqualTo(4);
  }

  /** 학습 수준이 없거나 1부터 5까지의 범위를 벗어나면 요청을 거절한다. */
  @Test
  void rejectsMissingOrOutOfRangeLearningLevel() throws Exception {
    String accessToken = login("learning-level-invalid");

    for (String content : List.of("{}", "{\"learningLevel\":0}", "{\"learningLevel\":6}")) {
      mockMvc
          .perform(
              put("/api/v1/me/learning-level")
                  .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(content))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }
  }

  /** 인증되지 않은 사용자는 학습 수준을 변경할 수 없다. */
  @Test
  void rejectsUnauthenticatedLearningLevelUpdate() throws Exception {
    mockMvc
        .perform(
            put("/api/v1/me/learning-level")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"learningLevel\":3}"))
        .andExpect(status().isUnauthorized());
  }

  /** OpenAPI 문서에 학습 수준 변경의 성공과 실패 응답을 공개한다. */
  @Test
  void openApiDocsDescribeLearningLevelUpdate() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/v1/me/learning-level'].put.responses['200']").exists())
        .andExpect(jsonPath("$.paths['/api/v1/me/learning-level'].put.responses['400']").exists())
        .andExpect(jsonPath("$.paths['/api/v1/me/learning-level'].put.responses['401']").exists());
  }

  /** 사용자 학습 수준 변경 요청을 보내고 성공 응답을 검증한다. */
  private void updateLearningLevel(String accessToken, int learningLevel) throws Exception {
    mockMvc
        .perform(
            put("/api/v1/me/learning-level")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"learningLevel\":%d}".formatted(learningLevel)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));
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

  /** 테스트 사용자의 현재 학습 수준을 조회한다. */
  private Integer learningLevel(String userKey) {
    return jdbcTemplate.queryForObject(
        "select learning_level from user_profile where email = ?",
        Integer.class,
        userKey + "@example.com");
  }
}
