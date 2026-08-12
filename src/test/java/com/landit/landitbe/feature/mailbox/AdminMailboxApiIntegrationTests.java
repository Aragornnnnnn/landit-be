// 편지함 어드민 API의 콘텐츠 관리와 피드백 일괄 처리를 통합 검증한다.

package com.landit.landitbe.feature.mailbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
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

/** 편지함 어드민 API의 콘텐츠 관리와 피드백 일괄 처리를 통합 검증한다. */
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@TestPropertySource(
    properties = {
      "landit.auth.oidc.fake-enabled=true",
      "landit.auth.token.secret=landit-test-token-secret-that-is-long-enough"
    })
class AdminMailboxApiIntegrationTests {

  @Autowired private MockMvc mockMvc;

  @Autowired private JdbcTemplate jdbcTemplate;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void clearMailboxData() {
    jdbcTemplate.update("delete from mailbox_letter_recipient");
    jdbcTemplate.update("delete from mailbox_letter_read");
    jdbcTemplate.update("delete from mailbox_letter");
    jdbcTemplate.update("delete from mailbox_feedback");
  }

  @Test
  void adminCanCreateDraftNotice() throws Exception {
    String accessToken = loginAsAdmin("mailbox-admin-create");

    mockMvc
        .perform(
            post("/api/v1/admin/mailbox/letters")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "type":"NOTICE",
                      "title":"새 공지",
                      "contentBlocks":[{"type":"TEXT","text":"공지 본문"}],
                      "preview":"공지 본문"
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.publicationStatus").value("DRAFT"))
        .andExpect(jsonPath("$.data.title").value("새 공지"))
        .andExpect(jsonPath("$.data.contentBlocks[0].type").value("TEXT"))
        .andExpect(jsonPath("$.data.contentBlocks[0].text").value("공지 본문"));
  }

  @Test
  void adminCanUpdatePublishAndUnpublishLetter() throws Exception {
    String accessToken = loginAsAdmin("mailbox-admin-publish");
    MvcResult created = createNotice(accessToken, "초안 공지");
    long letterId = responseData(created).get("letterId").asLong();

    mockMvc
        .perform(
            patch("/api/v1/admin/mailbox/letters/{letterId}", letterId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title":"게시 공지","publicationStatus":"PUBLISHED","pinned":true}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.publicationStatus").value("PUBLISHED"))
        .andExpect(jsonPath("$.data.pinned").value(true));

    mockMvc
        .perform(
            patch("/api/v1/admin/mailbox/letters/{letterId}", letterId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"publicationStatus\":\"UNPUBLISHED\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.publicationStatus").value("UNPUBLISHED"))
        .andExpect(jsonPath("$.data.pinned").value(false));
  }

  @Test
  void adminRejectsInvalidLetterStateChanges() throws Exception {
    String accessToken = loginAsAdmin("mailbox-admin-invalid-state");
    long letterId = responseData(createNotice(accessToken, "상태 검증 공지")).get("letterId").asLong();

    mockMvc
        .perform(
            patch("/api/v1/admin/mailbox/letters/{letterId}", letterId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"publicationStatus\":\"UNPUBLISHED\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));

    mockMvc
        .perform(
            patch("/api/v1/admin/mailbox/letters/{letterId}", letterId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"pinned\":true}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
  }

  @Test
  void adminLetterUpdateReturnsPersistedUpdatedAt() throws Exception {
    String accessToken = loginAsAdmin("mailbox-admin-updated-at");
    long letterId = responseData(createNotice(accessToken, "수정 시각 공지")).get("letterId").asLong();
    LocalDateTime oldUpdatedAt = LocalDateTime.of(2020, 1, 1, 0, 0);
    jdbcTemplate.update(
        "update mailbox_letter set updated_at = ? where id = ?", oldUpdatedAt, letterId);

    MvcResult result =
        mockMvc
            .perform(
                patch("/api/v1/admin/mailbox/letters/{letterId}", letterId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"title\":\"수정된 공지\"}"))
            .andExpect(status().isOk())
            .andReturn();

    LocalDateTime responseUpdatedAt =
        LocalDateTime.parse(responseData(result).get("updatedAt").asText());
    LocalDateTime persistedUpdatedAt =
        jdbcTemplate.queryForObject(
            "select updated_at from mailbox_letter where id = ?", LocalDateTime.class, letterId);
    assertThat(responseUpdatedAt).isEqualTo(persistedUpdatedAt).isAfter(oldUpdatedAt);
  }

  @Test
  void adminLetterContentUpdateRecordsChangedFields() throws Exception {
    String accessToken = loginAsAdmin("mailbox-admin-audit-fields");
    long letterId = responseData(createNotice(accessToken, "감사 로그 공지")).get("letterId").asLong();

    mockMvc
        .perform(
            patch("/api/v1/admin/mailbox/letters/{letterId}", letterId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"변경된 감사 로그 공지\"}"))
        .andExpect(status().isOk());

    Map<String, Object> auditLog =
        jdbcTemplate.queryForMap(
            """
            select before_value, after_value
            from admin_audit_log
            where action = 'MAILBOX_LETTER_UPDATED' and target_id = ?
            order by id desc
            limit 1
            """,
            String.valueOf(letterId));
    assertThat(auditLog.get("BEFORE_VALUE")).isNotEqualTo(auditLog.get("AFTER_VALUE"));
    assertThat(auditLog.get("AFTER_VALUE")).asString().contains("changedFields=title");
  }

  @Test
  void adminLetterListFiltersByPublicationStatusAndPinned() throws Exception {
    String accessToken = loginAsAdmin("mailbox-admin-letter-list");
    MvcResult published = createNotice(accessToken, "게시된 고정 공지");
    long publishedId = responseData(published).get("letterId").asLong();
    createNotice(accessToken, "다른 초안");

    mockMvc
        .perform(
            patch("/api/v1/admin/mailbox/letters/{letterId}", publishedId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"publicationStatus\":\"PUBLISHED\",\"pinned\":true}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            get("/api/v1/admin/mailbox/letters")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .param("publicationStatus", "PUBLISHED")
                .param("pinned", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items.length()").value(1))
        .andExpect(jsonPath("$.data.items[0].title").value("게시된 고정 공지"));
  }

  @Test
  void adminLetterListExcludesReplies() throws Exception {
    String adminToken = loginAsAdmin("mailbox-admin-letter-reply-filter");
    Long userId = loginAndFindUserId("mailbox-letter-reply-user");
    long feedbackId = insertFeedback(userId, "목록 제외 답장 문의", "QUESTION", "PENDING", 10);
    sendReply(adminToken, List.of(feedbackId), "목록 제외 답장");
    createNotice(adminToken, "목록에 남는 공지");

    mockMvc
        .perform(
            get("/api/v1/admin/mailbox/letters")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items.length()").value(1))
        .andExpect(jsonPath("$.data.items[0].type").value("NOTICE"));
  }

  @Test
  void adminLetterListRejectsReplyType() throws Exception {
    String adminToken = loginAsAdmin("mailbox-admin-reply-type-filter");

    mockMvc
        .perform(
            get("/api/v1/admin/mailbox/letters")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .param("type", "REPLY"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
  }

  @Test
  void adminLetterCreationValidatesTypeAndContentBlocks() throws Exception {
    String accessToken = loginAsAdmin("mailbox-admin-letter-validation");
    mockMvc
        .perform(
            post("/api/v1/admin/mailbox/letters")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"type\":\"REPLY\",\"title\":\"답장\","
                        + "\"contentBlocks\":[{}],\"preview\":\"답장\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));

    mockMvc
        .perform(
            post("/api/v1/admin/mailbox/letters")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"type\":\"NOTICE\",\"title\":\"빈 본문\",\"contentBlocks\":[],\"preview\":\"빈"
                        + " 본문\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
  }

  @Test
  void adminCanSearchFilterAndPaginateFeedbacks() throws Exception {
    final String adminToken = loginAsAdmin("mailbox-admin-search");
    Long firstUserId = loginAndFindUserId("mailbox-search-first");
    Long secondUserId = loginAndFindUserId("mailbox-search-second");
    insertFeedback(firstUserId, "로그인 문의 첫 번째", "QUESTION", "PENDING", 10);
    insertFeedback(firstUserId, "로그인 문의 두 번째", "QUESTION", "PENDING", 11);
    insertFeedback(secondUserId, "로그인 버그", "BUG_REPORT", "COMPLETED", 12);

    mockMvc
        .perform(
            get("/api/v1/admin/mailbox/feedbacks")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .param("keyword", "로그인 문의")
                .param("type", "QUESTION")
                .param("status", "PENDING")
                .param("page", "0")
                .param("size", "1")
                .param("sort", "OLDEST"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items.length()").value(1))
        .andExpect(jsonPath("$.data.items[0].content").value("로그인 문의 첫 번째"))
        .andExpect(jsonPath("$.data.totalElements").value(2))
        .andExpect(jsonPath("$.data.totalPages").value(2));
  }

  @Test
  void adminFeedbackSearchTreatsLikeWildcardsAsText() throws Exception {
    final String adminToken = loginAsAdmin("mailbox-admin-literal-search");
    Long userId = loginAndFindUserId("mailbox-literal-search-user");
    insertFeedback(userId, "진행률 100% 문의", "QUESTION", "PENDING", 10);
    insertFeedback(userId, "설정_key 문의", "QUESTION", "PENDING", 11);
    insertFeedback(userId, "일반 문의", "QUESTION", "PENDING", 12);

    assertFeedbackSearchResult(adminToken, "%", "진행률 100% 문의");
    assertFeedbackSearchResult(adminToken, "_", "설정_key 문의");
  }

  @Test
  void adminSendsOneReplyPerUserAndCompletesPendingFeedbacks() throws Exception {
    String adminToken = loginAsAdmin("mailbox-admin-reply");
    Long firstUserId = loginAndFindUserId("mailbox-reply-first");
    Long secondUserId = loginAndFindUserId("mailbox-reply-second");
    long firstFeedbackId = insertFeedback(firstUserId, "첫 번째 문의", "QUESTION", "PENDING", 10);
    long secondFeedbackId = insertFeedback(firstUserId, "두 번째 문의", "QUESTION", "PENDING", 11);
    long thirdFeedbackId = insertFeedback(secondUserId, "세 번째 문의", "QUESTION", "PENDING", 12);

    MvcResult result =
        sendReply(adminToken, List.of(secondFeedbackId, firstFeedbackId, thirdFeedbackId), "답변입니다");

    JsonNode response = responseData(result);
    final long replyId = response.get("letterId").asLong();
    assertThat(response.get("recipientCount").asInt()).isEqualTo(2);
    assertThat(response.get("completedFeedbackCount").asInt()).isEqualTo(3);
    assertCompletedFeedback(firstFeedbackId, null);
    assertCompletedFeedback(secondFeedbackId, firstFeedbackId);
    assertCompletedFeedback(thirdFeedbackId, null);
    assertReplyStoredAndAudited(replyId, 2);
  }

  @Test
  void adminPreservesExistingCompletedFeedbackRelation() throws Exception {
    String adminToken = loginAsAdmin("mailbox-admin-completed-relation");
    Long userId = loginAndFindUserId("mailbox-completed-relation-user");
    long representativeId = insertFeedback(userId, "기존 대표 문의", "QUESTION", "COMPLETED", 10);
    long completedFeedbackId = insertFeedback(userId, "기존 연결 문의", "QUESTION", "COMPLETED", 11);
    jdbcTemplate.update(
        "update mailbox_feedback set resolved_by_feedback_id = ? where id = ?",
        representativeId,
        completedFeedbackId);

    MvcResult result = sendReply(adminToken, List.of(completedFeedbackId), "처리 완료 문의 추가 답변");

    assertThat(responseData(result).get("completedFeedbackCount").asInt()).isZero();
    assertCompletedFeedback(completedFeedbackId, representativeId);
  }

  @Test
  void userSeesReplyOnlyOnRepresentativeFeedback() throws Exception {
    String adminToken = loginAsAdmin("mailbox-admin-visible-reply");
    LoginResult user = login("mailbox-visible-reply-user");
    long firstFeedbackId =
        insertFeedback(user.userProfileId(), "첫 번째 표시 문의", "QUESTION", "PENDING", 10);
    long secondFeedbackId =
        insertFeedback(user.userProfileId(), "두 번째 표시 문의", "QUESTION", "PENDING", 11);

    MvcResult result = sendReply(adminToken, List.of(secondFeedbackId, firstFeedbackId), "표시할 답변");
    long replyId = responseData(result).get("letterId").asLong();

    mockMvc
        .perform(
            get("/api/v1/mailbox/received")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + user.accessToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items.length()").value(1))
        .andExpect(jsonPath("$.data.items[0].letterId").value(replyId));
    mockMvc
        .perform(
            get("/api/v1/mailbox/sent/{feedbackId}", firstFeedbackId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + user.accessToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.replies.length()").value(1));
    mockMvc
        .perform(
            get("/api/v1/mailbox/sent/{feedbackId}", secondFeedbackId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + user.accessToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.replies.length()").value(0));
  }

  @Test
  void adminCanSendAdditionalReplyToCompletedFeedback() throws Exception {
    String adminToken = loginAsAdmin("mailbox-admin-additional-reply");
    Long userId = loginAndFindUserId("mailbox-additional-reply-user");
    long feedbackId = insertFeedback(userId, "추가 답변 문의", "QUESTION", "PENDING", 10);

    sendReply(adminToken, List.of(feedbackId), "첫 번째 답변");
    sendReply(adminToken, List.of(feedbackId), "추가 답변");

    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from mailbox_letter where letter_type = 'REPLY'", Integer.class))
        .isEqualTo(2);
  }

  @Test
  void adminReplyRejectsTitleLongerThanColumnLimit() throws Exception {
    String adminToken = loginAsAdmin("mailbox-admin-long-reply-title");
    Long userId = loginAndFindUserId("mailbox-long-reply-title-user");
    long feedbackId = insertFeedback(userId, "긴 제목 답장 문의", "QUESTION", "PENDING", 10);

    mockMvc
        .perform(
            post("/api/v1/admin/mailbox/replies")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "feedbackIds", List.of(feedbackId),
                            "title", "가".repeat(201),
                            "bodyText", "답장 본문"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
  }

  @Test
  void adminBatchReplyRollsBackWhenAnyFeedbackIsMissing() throws Exception {
    String adminToken = loginAsAdmin("mailbox-admin-reply-rollback");
    Long userId = loginAndFindUserId("mailbox-reply-rollback-user");
    long feedbackId = insertFeedback(userId, "롤백 대상 문의", "QUESTION", "PENDING", 10);

    mockMvc
        .perform(
            post("/api/v1/admin/mailbox/replies")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "feedbackIds", List.of(feedbackId, 999999L),
                            "title", "답변",
                            "bodyText", "문의 확인했습니다."))))
        .andExpect(status().isNotFound());

    assertThat(feedback(feedbackId).get("PROCESSING_STATUS")).isEqualTo("PENDING");
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from mailbox_letter where letter_type = 'REPLY'", Integer.class))
        .isZero();
  }

  @Test
  void documentsAdminMailboxContentBlocksAsArray() throws Exception {
    String schemas = "$.components.schemas.";
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath(schemas + "AdminMailboxLetterCreateRequest.properties.contentBlocks.type")
                .value("array"))
        .andExpect(
            jsonPath(schemas + "AdminMailboxLetterPatchRequest.properties.contentBlocks.type")
                .value("array"))
        .andExpect(
            jsonPath(schemas + "AdminMailboxLetterResponse.properties.contentBlocks.type")
                .value("array"));
  }

  private MvcResult createNotice(String accessToken, String title) throws Exception {
    return mockMvc
        .perform(
            post("/api/v1/admin/mailbox/letters")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"type":"NOTICE","title":"%s","contentBlocks":[{"type":"TEXT","text":"본문"}],"preview":"본문"}
                    """
                        .formatted(title)))
        .andExpect(status().isCreated())
        .andReturn();
  }

  private MvcResult sendReply(String accessToken, List<Long> feedbackIds, String title)
      throws Exception {
    return mockMvc
        .perform(
            post("/api/v1/admin/mailbox/replies")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "feedbackIds", feedbackIds,
                            "title", title,
                            "bodyText", "답장 본문"))))
        .andExpect(status().isCreated())
        .andReturn();
  }

  private void assertFeedbackSearchResult(
      String accessToken, String keyword, String expectedContent) throws Exception {
    mockMvc
        .perform(
            get("/api/v1/admin/mailbox/feedbacks")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .param("keyword", keyword))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items.length()").value(1))
        .andExpect(jsonPath("$.data.items[0].content").value(expectedContent));
  }

  private Long loginAndFindUserId(String userKey) throws Exception {
    return login(userKey).userProfileId();
  }

  private long insertFeedback(
      Long userProfileId, String content, String type, String status, int hour) {
    jdbcTemplate.update(
        """
        insert into mailbox_feedback (
            user_profile_id, feedback_type, content_text, processing_status,
            created_at, updated_at)
        values (?, ?, ?, ?, ?, ?)
        """,
        userProfileId,
        type,
        content,
        status,
        LocalDateTime.of(2026, 1, 1, hour, 0),
        LocalDateTime.of(2026, 1, 1, hour, 0));
    return jdbcTemplate.queryForObject(
        "select id from mailbox_feedback where content_text = ? order by id desc limit 1",
        Long.class,
        content);
  }

  private Map<String, Object> feedback(long feedbackId) {
    return jdbcTemplate.queryForMap(
        "select processing_status, resolved_by_feedback_id from mailbox_feedback where id = ?",
        feedbackId);
  }

  private void assertCompletedFeedback(long feedbackId, Long representativeFeedbackId) {
    Map<String, Object> savedFeedback = feedback(feedbackId);
    assertThat(savedFeedback.get("PROCESSING_STATUS")).isEqualTo("COMPLETED");
    assertThat(savedFeedback.get("RESOLVED_BY_FEEDBACK_ID")).isEqualTo(representativeFeedbackId);
  }

  private void assertReplyStoredAndAudited(long replyId, int recipientCount) {
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from mailbox_letter where id = ? and letter_type = 'REPLY'",
                Integer.class,
                replyId))
        .isEqualTo(1);
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from mailbox_letter_recipient where letter_id = ?",
                Integer.class,
                replyId))
        .isEqualTo(recipientCount);
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from admin_audit_log where action = 'MAILBOX_REPLY_SENT' and"
                    + " target_id = ?",
                Integer.class,
                String.valueOf(replyId)))
        .isEqualTo(1);
  }

  private JsonNode responseData(MvcResult result) throws Exception {
    return objectMapper.readTree(result.getResponse().getContentAsByteArray()).get("data");
  }

  private String loginAsAdmin(String userKey) throws Exception {
    LoginResult loginResult = login(userKey);
    jdbcTemplate.update(
        "update user_profile set role = 'ADMIN' where id = ?", loginResult.userProfileId());
    return loginResult.accessToken();
  }

  private LoginResult login(String userKey) throws Exception {
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
                          "idToken":"%s|%s@example.com|Admin User|%s",
                          "nonce":"%s"
                        }
                        """
                            .formatted(userKey, userKey, nonce, nonce)))
            .andExpect(status().isOk())
            .andReturn();
    JsonNode body = objectMapper.readTree(result.getResponse().getContentAsByteArray());
    Long userProfileId =
        jdbcTemplate.queryForObject(
            "select id from user_profile where email = ?", Long.class, userKey + "@example.com");
    return new LoginResult(userProfileId, body.get("data").get("accessToken").asText());
  }

  private record LoginResult(Long userProfileId, String accessToken) {}
}
