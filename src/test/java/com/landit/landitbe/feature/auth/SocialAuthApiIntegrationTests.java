// 소셜 로그인 API의 사용자 생성, 토큰 발급, nonce 검증을 검증한다.

package com.landit.landitbe.feature.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.landitbe.feature.auth.domain.OauthIdentity;
import com.landit.landitbe.feature.auth.domain.OauthIdentityStatus;
import com.landit.landitbe.feature.auth.domain.RefreshToken;
import com.landit.landitbe.feature.auth.domain.SocialProvider;
import com.landit.landitbe.feature.auth.repository.OauthIdentityRepository;
import com.landit.landitbe.feature.auth.repository.RefreshTokenRepository;
import com.landit.landitbe.feature.auth.service.LanditTokenService;
import com.landit.landitbe.feature.profile.domain.UserProfile;
import com.landit.landitbe.feature.profile.repository.UserProfileRepository;
import java.time.LocalDateTime;
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

/** 소셜 로그인 API의 사용자 생성, 토큰 발급, nonce 검증을 검증한다. */
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@TestPropertySource(
    properties = {
      "landit.auth.oidc.fake-enabled=true",
      "landit.auth.token.secret=landit-test-token-secret-that-is-long-enough"
    })
class SocialAuthApiIntegrationTests {

  @Autowired private MockMvc mockMvc;

  @Autowired private UserProfileRepository userProfileRepository;

  @Autowired private OauthIdentityRepository oauthIdentityRepository;

  @Autowired private RefreshTokenRepository refreshTokenRepository;

  @Autowired private LanditTokenService tokenService;

  @Autowired private JdbcTemplate jdbcTemplate;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void socialLoginCreatesUserAndReturnsTokens() throws Exception {
    MvcResult firstLogin =
        mockMvc
            .perform(
                post("/api/v1/auth/social-login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                                {
                                  "provider":"GOOGLE",
                                  "idToken":"google-sub-1|ryan@example.com|Ryan|nonce-1",
                                  "nonce":"nonce-1"
                                }
                        """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.error").value(nullValue()))
            .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.data.accessTokenExpiresIn").value(1800))
            .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
            .andExpect(jsonPath("$.data.refreshTokenExpiresIn").value(1209600))
            .andExpect(jsonPath("$.data.user.userId").isNotEmpty())
            .andExpect(jsonPath("$.data.user.nickname").value("Ryan"))
            .andExpect(jsonPath("$.data.user.email").value("ryan@example.com"))
            .andExpect(jsonPath("$.data.user.provider").value("GOOGLE"))
            .andExpect(jsonPath("$.data.user.newUser").value(true))
            .andReturn();

    JsonNode firstBody = objectMapper.readTree(firstLogin.getResponse().getContentAsByteArray());
    String userId = firstBody.get("data").get("user").get("userId").asText();
    assertDefaultAiTutorAssigned(Long.parseLong(userId));

    mockMvc
        .perform(
            post("/api/v1/auth/social-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "provider":"GOOGLE",
                                  "idToken":"google-sub-1|ryan@example.com|Ryan|nonce-2",
                                  "nonce":"nonce-2"
                                }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.user.userId").value(userId))
        .andExpect(jsonPath("$.data.user.newUser").value(false));
  }

  @Test
  void socialLoginRejectsMissingNonce() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/social-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "provider":"GOOGLE",
                                  "idToken":"google-sub-2|nonce@example.com|Nonce User|nonce-1"
                                }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("OIDC_NONCE_MISMATCH"));
  }

  @Test
  void socialLoginRejectsNonceMismatch() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/social-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "provider":"GOOGLE",
                                  "idToken":"google-sub-3|nonce@example.com|Nonce User|expected-nonce",
                                  "nonce":"different-nonce"
                                }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("OIDC_NONCE_MISMATCH"));
  }

  @Test
  void socialLoginCreatesGuestForAppleWithoutRequestNickname() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/auth/social-login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                                {
                                  "provider":"APPLE",
                                  "idToken":"apple-sub-1|apple@example.com|Apple User|apple-nonce",
                                  "nonce":"apple-nonce"
                                }
                        """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.user.provider").value("APPLE"))
            .andExpect(jsonPath("$.data.user.nickname").value("Guest"))
            .andExpect(jsonPath("$.data.user.email").value("apple@example.com"))
            .andExpect(jsonPath("$.data.user.newUser").value(true))
            .andReturn();

    long userId =
        objectMapper
            .readTree(result.getResponse().getContentAsByteArray())
            .get("data")
            .get("user")
            .get("userId")
            .asLong();
    assertDefaultAiTutorAssigned(userId);
  }

  @Test
  void socialLoginUsesRequestNicknameForApple() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/social-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "provider":"APPLE",
                                  "idToken":"apple-sub-2|apple-nickname@example.com|Id Token Name|apple-nonce",
                                  "nonce":"apple-nonce",
                                  "nickname":"Apple Request Name"
                                }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.user.nickname").value("Apple Request Name"));
  }

  @Test
  void socialLoginKeepsExistingAppleNicknameWhenRequestNicknameIsMissing() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/social-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "provider":"APPLE",
                                  "idToken":"apple-sub-3|apple-existing@example.com|Id Token Name|apple-nonce-1",
                                  "nonce":"apple-nonce-1",
                                  "nickname":"Apple Request Name"
                                }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.user.nickname").value("Apple Request Name"));

    mockMvc
        .perform(
            post("/api/v1/auth/social-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "provider":"APPLE",
                                  "idToken":"apple-sub-3|apple-existing@example.com|Id Token Name|apple-nonce-2",
                                  "nonce":"apple-nonce-2"
                                }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.user.nickname").value("Apple Request Name"));
  }

  @Test
  void socialLoginIgnoresRequestNicknameForGoogle() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/social-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "provider":"GOOGLE",
                                  "idToken":"google-sub-4|google-nickname@example.com|Id Token Name|google-nonce",
                                  "nonce":"google-nonce",
                                  "nickname":"Ignored Request Name"
                                }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.user.nickname").value("Id Token Name"));
  }

  @Test
  void socialLoginRejectsNewUserWhenDefaultAiTutorIsMissing() throws Exception {
    jdbcTemplate.update(
        """
                UPDATE ai_tutor
                SET status = 'INACTIVE'
                WHERE accent_locale = 'EN_US'
                  AND target_locale = 'EN'
                  AND status = 'ACTIVE'
        """);
    try {
      mockMvc
          .perform(
              post("/api/v1/auth/social-login")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      """
                                    {
                                      "provider":"GOOGLE",
                                      "idToken":"missing-tutor|missing-tutor@example.com|Missing Tutor|nonce",
                                      "nonce":"nonce"
                                    }
                      """))
          .andExpect(status().isInternalServerError())
          .andExpect(jsonPath("$.error.code").value("DEFAULT_AI_TUTOR_NOT_CONFIGURED"));
    } finally {
      jdbcTemplate.update(
          """
                    UPDATE ai_tutor
                    SET status = 'ACTIVE'
                    WHERE accent_locale = 'EN_US'
                      AND target_locale = 'EN'
                      AND status = 'INACTIVE'
          """);
    }
  }

  @Test
  void socialLoginRejectsNewUserWhenDefaultAiTutorIsDuplicated() throws Exception {
    jdbcTemplate.update(
        """
                INSERT INTO ai_tutor (
                    id, accent_locale, target_locale, status, created_at, updated_at
                )
                VALUES (990100, 'EN_US', 'EN', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """);
    try {
      mockMvc
          .perform(
              post("/api/v1/auth/social-login")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      """
                                    {
                                      "provider":"GOOGLE",
                                      "idToken":"duplicate-tutor|duplicate-tutor@example.com|Duplicate Tutor|nonce",
                                      "nonce":"nonce"
                                    }
                      """))
          .andExpect(status().isInternalServerError())
          .andExpect(jsonPath("$.error.code").value("DEFAULT_AI_TUTOR_NOT_CONFIGURED"));
    } finally {
      jdbcTemplate.update("DELETE FROM ai_tutor WHERE id = 990100");
    }
  }

  @Test
  void socialLoginRejectsUnsupportedProvider() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/social-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "provider":"NAVER",
                                  "idToken":"naver-sub|naver@example.com|Naver User|nonce",
                                  "nonce":"nonce"
                                }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("UNSUPPORTED_SOCIAL_PROVIDER"));
  }

  private void assertDefaultAiTutorAssigned(long userId) {
    Long aiTutorId =
        jdbcTemplate.queryForObject(
            "SELECT ai_tutor_id FROM user_profile WHERE id = ?", Long.class, userId);
    assertThat(aiTutorId).isEqualTo(defaultAiTutorId());
  }

  private Long defaultAiTutorId() {
    return jdbcTemplate.queryForObject(
        """
                SELECT id
                FROM ai_tutor
                WHERE accent_locale = 'EN_US'
                  AND target_locale = 'EN'
                  AND status = 'ACTIVE'
        """,
        Long.class);
  }

  @Test
  void refreshRotatesRefreshTokenAndRejectsReusedToken() throws Exception {
    JsonNode loginBody =
        login("GOOGLE", "google-refresh-1", "refresh@example.com", "Refresh User", "refresh-nonce");
    String refreshToken = loginBody.get("data").get("refreshToken").asText();

    MvcResult refreshResult =
        mockMvc
            .perform(
                post("/api/v1/auth/token/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                                {
                                  "refreshToken":"%s"
                                }
                        """
                            .formatted(refreshToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.error").value(nullValue()))
            .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.data.accessTokenExpiresIn").value(1800))
            .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
            .andExpect(jsonPath("$.data.refreshTokenExpiresIn").value(1209600))
            .andReturn();

    JsonNode refreshBody =
        objectMapper.readTree(refreshResult.getResponse().getContentAsByteArray());
    String rotatedRefreshToken = refreshBody.get("data").get("refreshToken").asText();
    assertThat(rotatedRefreshToken).isNotBlank().isNotEqualTo(refreshToken);

    mockMvc
        .perform(
            post("/api/v1/auth/token/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "refreshToken":"%s"
                                }
                    """
                        .formatted(refreshToken)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("REFRESH_TOKEN_INVALID"));
  }

  @Test
  void logoutRevokesRefreshToken() throws Exception {
    JsonNode loginBody =
        login("GOOGLE", "google-logout-1", "logout@example.com", "Logout User", "logout-nonce");
    String refreshToken = loginBody.get("data").get("refreshToken").asText();

    mockMvc
        .perform(
            post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "refreshToken":"%s"
                                }
                    """
                        .formatted(refreshToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data").value(nullValue()))
        .andExpect(jsonPath("$.error").value(nullValue()));

    mockMvc
        .perform(
            post("/api/v1/auth/token/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "refreshToken":"%s"
                                }
                    """
                        .formatted(refreshToken)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error.code").value("REFRESH_TOKEN_INVALID"));
  }

  @Test
  void withdrawUsesAccessTokenAndRevokesRefreshTokens() throws Exception {
    mockMvc
        .perform(delete("/api/v1/auth/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));

    JsonNode loginBody =
        login(
            "GOOGLE",
            "google-withdraw-1",
            "withdraw@example.com",
            "Withdraw User",
            "withdraw-nonce");
    Long userId = loginBody.get("data").get("user").get("userId").asLong();
    String accessToken = loginBody.get("data").get("accessToken").asText();
    String refreshToken = loginBody.get("data").get("refreshToken").asText();

    mockMvc
        .perform(
            delete("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data").value(nullValue()))
        .andExpect(jsonPath("$.error").value(nullValue()));

    UserProfile withdrawnUser = userProfileRepository.findById(userId).orElseThrow();
    OauthIdentity withdrawnIdentity =
        oauthIdentityRepository
            .findAllByUserProfileIdAndStatus(userId, OauthIdentityStatus.UNLINKED)
            .getFirst();
    assertThat(withdrawnUser.getEmail()).isEqualTo("withdraw@example.com");
    assertThat(withdrawnUser.getNickname()).isEqualTo("Withdraw User");
    assertThat(withdrawnIdentity).extracting("providerEmail").isEqualTo("withdraw@example.com");

    mockMvc
        .perform(
            post("/api/v1/auth/token/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "refreshToken":"%s"
                                }
                    """
                        .formatted(refreshToken)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error.code").value("REFRESH_TOKEN_INVALID"));
  }

  @Test
  void withdrawRejectsInvalidAccessToken() throws Exception {
    mockMvc
        .perform(
            delete("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));
  }

  @Test
  void refreshRejectsTokenOwnedByWithdrawnUser() throws Exception {
    UserProfile userProfile =
        userProfileRepository.save(
            new UserProfile(
                "withdrawn-refresh@example.com", "Withdrawn Refresh User", defaultAiTutorId()));
    oauthIdentityRepository.save(
        new OauthIdentity(
            userProfile.getId(),
            SocialProvider.GOOGLE,
            "google-withdrawn-refresh-1",
            "withdrawn-refresh@example.com"));
    String refreshToken = "withdrawn-user-refresh-token";
    refreshTokenRepository.save(
        new RefreshToken(
            userProfile.getId(),
            tokenService.hashToken(refreshToken),
            LocalDateTime.now().plusMinutes(10)));
    userProfile.withdraw();
    userProfileRepository.save(userProfile);

    mockMvc
        .perform(
            post("/api/v1/auth/token/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "refreshToken":"%s"
                                }
                    """
                        .formatted(refreshToken)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("REFRESH_TOKEN_INVALID"));
  }

  private JsonNode login(String provider, String sub, String email, String nickname, String nonce)
      throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/auth/social-login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                                {
                                  "provider":"%s",
                                  "idToken":"%s|%s|%s|%s",
                                  "nonce":"%s"
                                }
                        """
                            .formatted(provider, sub, email, nickname, nonce, nonce)))
            .andExpect(status().isOk())
            .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsByteArray());
  }
}
