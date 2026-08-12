// 편지함 사용자 API의 저장, 공개 범위, 페이지 조회와 읽음 정책을 검증한다.

package com.landit.landitbe.feature.mailbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.landitbe.feature.mailbox.domain.MailboxLetter;
import com.landit.landitbe.feature.mailbox.domain.MailboxLetterRecipient;
import com.landit.landitbe.feature.mailbox.domain.MailboxLetterType;
import com.landit.landitbe.feature.mailbox.domain.MailboxPublicationStatus;
import com.landit.landitbe.feature.mailbox.repository.MailboxLetterRecipientRepository;
import com.landit.landitbe.feature.mailbox.repository.MailboxLetterRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** 편지함 사용자 API의 저장, 공개 범위, 페이지 조회와 읽음 정책을 검증한다. */
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@TestPropertySource(
    properties = {
      "landit.auth.oidc.fake-enabled=true",
      "landit.auth.token.secret=landit-test-token-secret-that-is-long-enough"
    })
class MailboxApiIntegrationTests {

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private MailboxLetterRepository mailboxLetterRepository;
  @Autowired private MailboxLetterRecipientRepository mailboxLetterRecipientRepository;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void clearMailboxData() {
    jdbcTemplate.update("DELETE FROM mailbox_letter_recipient");
    jdbcTemplate.update("DELETE FROM mailbox_letter_read");
    jdbcTemplate.update("DELETE FROM mailbox_letter");
    jdbcTemplate.update("DELETE FROM mailbox_feedback");
  }

  @Test
  void submitFeedbackStoresPendingFeedback() throws Exception {
    TestUser user = login("submit");

    mockMvc
        .perform(
            feedbackRequest(
                user,
                """
                {"type":"QUESTION","content":"로그인 관련 문의입니다."}
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data").value(nullValue()));

    assertThat(feedbackByEmail(user.email()))
        .singleElement()
        .satisfies(
            feedback -> {
              assertThat(feedback.get("feedback_type")).isEqualTo("QUESTION");
              assertThat(feedback.get("content_text")).isEqualTo("로그인 관련 문의입니다.");
              assertThat(feedback.get("processing_status")).isEqualTo("PENDING");
            });
  }

  @Test
  void submitFeedbackValidatesPayloadAndAuthentication() throws Exception {
    TestUser user = login("invalid");
    for (String body :
        List.of(
            "{\"content\":\"내용\"}",
            "{\"type\":\"UNKNOWN\",\"content\":\"내용\"}",
            "{\"type\":\"QUESTION\",\"content\":\"\"}",
            "{\"type\":\"QUESTION\",\"content\":\"   \"}")) {
      mockMvc
          .perform(feedbackRequest(user, body))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    mockMvc
        .perform(
            post("/api/v1/mailbox/feedbacks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"QUESTION\",\"content\":\"내용\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void sentFeedbacksUseNewestFirstCursorPagination() throws Exception {
    TestUser user = login("sent");
    submit(user, "첫 번째 문의");
    submit(user, "두 번째 문의");
    submit(user, "세 번째 문의");

    MvcResult firstPage =
        mockMvc
            .perform(authorizedGet(user, "/api/v1/mailbox/sent").param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items.length()").value(2))
            .andExpect(jsonPath("$.data.items[0].preview").value("세 번째 문의"))
            .andExpect(jsonPath("$.data.items[1].preview").value("두 번째 문의"))
            .andExpect(jsonPath("$.data.hasNext").value(true))
            .andReturn();

    mockMvc
        .perform(
            authorizedGet(user, "/api/v1/mailbox/sent")
                .param("size", "2")
                .param("cursor", nextCursor(firstPage)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items.length()").value(1))
        .andExpect(jsonPath("$.data.items[0].preview").value("첫 번째 문의"))
        .andExpect(jsonPath("$.data.hasNext").value(false));

    mockMvc
        .perform(authorizedGet(user, "/api/v1/mailbox/sent").param("cursor", "invalid"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
  }

  @Test
  void sentFeedbackDetailIsLimitedToOwnerAndIncludesReplies() throws Exception {
    TestUser owner = login("owner");
    TestUser other = login("other");
    long feedbackId = submit(owner, "내 문의 상세");
    MailboxLetter reply = saveReply("답장 본문", "답장 미리보기", 10);
    mailboxLetterRecipientRepository.saveAndFlush(
        new MailboxLetterRecipient(reply.getId(), owner.id(), feedbackId));
    MailboxLetter otherReply = saveReply("다른 사용자 답장", "다른 사용자 미리보기", 11);
    mailboxLetterRecipientRepository.saveAndFlush(
        new MailboxLetterRecipient(otherReply.getId(), other.id(), feedbackId));

    mockMvc
        .perform(authorizedGet(owner, "/api/v1/mailbox/sent/{feedbackId}", feedbackId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.title").value("문의"))
        .andExpect(jsonPath("$.data.content").value("내 문의 상세"))
        .andExpect(jsonPath("$.data.status").value("PENDING"))
        .andExpect(jsonPath("$.data.replies.length()").value(1))
        .andExpect(jsonPath("$.data.replies[0].bodyText").value("답장 본문"));

    mockMvc
        .perform(authorizedGet(other, "/api/v1/mailbox/sent/{feedbackId}", feedbackId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
  }

  @Test
  void mailboxOpenApiDescribesAllUserEndpoints() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/v1/mailbox/feedbacks'].post.responses['201']").exists())
        .andExpect(jsonPath("$.paths['/api/v1/mailbox/sent'].get.responses['200']").exists())
        .andExpect(
            jsonPath("$.paths['/api/v1/mailbox/sent/{feedbackId}'].get.responses['200']").exists())
        .andExpect(jsonPath("$.paths['/api/v1/mailbox/received'].get.responses['200']").exists())
        .andExpect(
            jsonPath("$.paths['/api/v1/mailbox/received/{letterId}'].get.responses['200']")
                .exists())
        .andExpect(
            jsonPath("$.paths['/api/v1/mailbox/unread-count'].get.responses['200']").exists());
  }

  @Test
  void receivedMailboxCombinesPublishedGlobalAndTargetedLetters() throws Exception {
    saveNotice("고정 공지", "공지 미리보기", true, 10);
    saveUpdate("업데이트", "업데이트 미리보기", false, 11);
    saveDraftNotice("초안", "노출 금지", 12);
    TestUser owner = login("received-owner");
    saveTargetedReply(owner, submit(owner, "대표 문의"), "내 답장", 13);
    TestUser other = login("received-other");
    MailboxLetter otherReply = saveTargetedReply(other, submit(other, "다른 문의"), "다른 사용자 답장", 14);

    MvcResult result =
        mockMvc
            .perform(authorizedGet(owner, "/api/v1/mailbox/received"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items.length()").value(3))
            .andReturn();
    JsonNode items = responseData(result).get("items");
    assertThat(items)
        .extracting(node -> node.get("title").asText())
        .containsExactly("고정 공지", "내 답장", "업데이트");

    mockMvc
        .perform(authorizedGet(owner, "/api/v1/mailbox/received/{letterId}", otherReply.getId()))
        .andExpect(status().isNotFound());
  }

  @Test
  void receivedMailboxUsesCursorAndValidatesPageValues() throws Exception {
    saveNotice("고정 공지", "고정", true, 1);
    saveNotice("공지 2", "미리보기 2", false, 2);
    saveNotice("공지 3", "미리보기 3", false, 3);
    TestUser user = login("received-page");

    MvcResult firstPage =
        mockMvc
            .perform(authorizedGet(user, "/api/v1/mailbox/received").param("size", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items.length()").value(1))
            .andExpect(jsonPath("$.data.items[0].title").value("고정 공지"))
            .andExpect(jsonPath("$.data.hasNext").value(true))
            .andReturn();
    MvcResult secondPage =
        mockMvc
            .perform(
                authorizedGet(user, "/api/v1/mailbox/received")
                    .param("size", "1")
                    .param("cursor", nextCursor(firstPage)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items.length()").value(1))
            .andExpect(jsonPath("$.data.items[0].title").value("공지 3"))
            .andExpect(jsonPath("$.data.hasNext").value(true))
            .andReturn();
    mockMvc
        .perform(
            authorizedGet(user, "/api/v1/mailbox/received")
                .param("size", "1")
                .param("cursor", nextCursor(secondPage)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items.length()").value(1))
        .andExpect(jsonPath("$.data.items[0].title").value("공지 2"))
        .andExpect(jsonPath("$.data.hasNext").value(false));

    for (Map<String, String> params :
        List.of(Map.of("cursor", "invalid"), Map.of("size", "0"), Map.of("size", "101"))) {
      MockHttpServletRequestBuilder request = authorizedGet(user, "/api/v1/mailbox/received");
      params.forEach(request::param);
      mockMvc
          .perform(request)
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }
  }

  @Test
  void receivedDetailMarksLettersReadIdempotentlyAndUpdatesUnreadCount() throws Exception {
    TestUser user = login("read");
    MailboxLetter notice = saveNotice("읽을 공지", "공지 미리보기", false, 10);
    saveUpdate("남은 업데이트", "업데이트 미리보기", false, 11);
    MailboxLetter reply = saveTargetedReply(user, submit(user, "읽음 문의"), "읽을 답장", 12);

    mockMvc
        .perform(authorizedGet(user, "/api/v1/mailbox/unread-count"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.unreadCount").value(3));
    for (int attempt = 0; attempt < 2; attempt++) {
      mockMvc
          .perform(authorizedGet(user, "/api/v1/mailbox/received/{letterId}", notice.getId()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.letterType").value("NOTICE"));
      mockMvc
          .perform(authorizedGet(user, "/api/v1/mailbox/received/{letterId}", reply.getId()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.bodyText").value("읽을 답장"));
    }

    assertThat(countRows("mailbox_letter_read")).isEqualTo(1);
    assertThat(countRows("mailbox_letter_recipient", "read_at IS NOT NULL")).isEqualTo(1);
    mockMvc
        .perform(authorizedGet(user, "/api/v1/mailbox/unread-count"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.unreadCount").value(1));
  }

  @Test
  void concurrentReplyReadsPreserveInitialReadTime() throws Exception {
    TestUser user = login("concurrent-read");
    MailboxLetter reply = saveTargetedReply(user, submit(user, "동시 읽음 문의"), "동시 읽음 답장", 12);
    int requestCount = 12;
    CountDownLatch startSignal = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(requestCount);

    try {
      List<Future<String>> readAtFutures =
          IntStream.range(0, requestCount)
              .mapToObj(
                  ignored ->
                      executor.submit(
                          () -> {
                            startSignal.await();
                            return readLetterAndGetReadAt(user, reply.getId());
                          }))
              .toList();
      startSignal.countDown();

      List<String> readAtValues = new ArrayList<>();
      for (Future<String> readAtFuture : readAtFutures) {
        readAtValues.add(readAtFuture.get());
      }

      assertThat(readAtValues).allMatch(readAt -> !readAt.isBlank());
      assertThat(readAtValues).containsOnly(readAtValues.getFirst());
    } finally {
      executor.shutdownNow();
    }
  }

  private MockHttpServletRequestBuilder feedbackRequest(TestUser user, String body) {
    return post("/api/v1/mailbox/feedbacks")
        .header(HttpHeaders.AUTHORIZATION, bearer(user))
        .contentType(MediaType.APPLICATION_JSON)
        .content(body);
  }

  private MockHttpServletRequestBuilder authorizedGet(
      TestUser user, String path, Object... variables) {
    return get(path, variables).header(HttpHeaders.AUTHORIZATION, bearer(user));
  }

  private String readLetterAndGetReadAt(TestUser user, long letterId) throws Exception {
    MvcResult result =
        mockMvc
            .perform(authorizedGet(user, "/api/v1/mailbox/received/{letterId}", letterId))
            .andExpect(status().isOk())
            .andReturn();
    return responseData(result).get("readAt").asText();
  }

  private long submit(TestUser user, String content) throws Exception {
    mockMvc
        .perform(
            feedbackRequest(
                user,
                objectMapper.writeValueAsString(Map.of("type", "QUESTION", "content", content))))
        .andExpect(status().isCreated());
    return jdbcTemplate.queryForObject(
        "SELECT id FROM mailbox_feedback WHERE content_text = ? ORDER BY id DESC LIMIT 1",
        Long.class,
        content);
  }

  private List<Map<String, Object>> feedbackByEmail(String email) {
    return jdbcTemplate.queryForList(
        """
        SELECT feedback.feedback_type, feedback.content_text, feedback.processing_status
        FROM mailbox_feedback feedback
        JOIN user_profile profile ON profile.id = feedback.user_profile_id
        WHERE profile.email = ?
        """,
        email);
  }

  private MailboxLetter saveNotice(String title, String text, boolean pinned, int hour) {
    return saveGlobalLetter(
        MailboxLetterType.NOTICE, title, text, pinned, hour, MailboxPublicationStatus.PUBLISHED);
  }

  private MailboxLetter saveDraftNotice(String title, String text, int hour) {
    return saveGlobalLetter(
        MailboxLetterType.NOTICE, title, text, false, hour, MailboxPublicationStatus.DRAFT);
  }

  private MailboxLetter saveUpdate(String title, String text, boolean pinned, int hour) {
    return saveGlobalLetter(
        MailboxLetterType.UPDATE, title, text, pinned, hour, MailboxPublicationStatus.PUBLISHED);
  }

  private MailboxLetter saveGlobalLetter(
      MailboxLetterType letterType,
      String title,
      String text,
      boolean pinned,
      int hour,
      MailboxPublicationStatus status) {
    return mailboxLetterRepository.saveAndFlush(
        new MailboxLetter(
            letterType,
            title,
            objectMapper.createArrayNode().addObject().put("text", text),
            null,
            text,
            status,
            pinned,
            sentAt(hour)));
  }

  private MailboxLetter saveReply(String text, String preview, int hour) {
    return mailboxLetterRepository.saveAndFlush(
        new MailboxLetter(
            MailboxLetterType.REPLY,
            text,
            null,
            text,
            preview,
            MailboxPublicationStatus.PUBLISHED,
            false,
            sentAt(hour)));
  }

  private MailboxLetter saveTargetedReply(TestUser user, long feedbackId, String text, int hour) {
    MailboxLetter reply = saveReply(text, text + " 미리보기", hour);
    mailboxLetterRecipientRepository.saveAndFlush(
        new MailboxLetterRecipient(reply.getId(), user.id(), feedbackId));
    return reply;
  }

  private TestUser login(String key) throws Exception {
    String email = "mailbox-" + key + "@example.com";
    String nonce = "mailbox-" + key + "-nonce";
    // 테스트 OIDC 클라이언트는 subject|email|name|nonce 형식의 토큰을 사용한다.
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/auth/social-login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"provider":"GOOGLE","idToken":"%s|%s|Test User|%s","nonce":"%s"}
                        """
                            .formatted("mailbox-" + key, email, nonce, nonce)))
            .andExpect(status().isOk())
            .andReturn();
    String token = responseData(result).get("accessToken").asText();
    Long userId =
        jdbcTemplate.queryForObject(
            "SELECT id FROM user_profile WHERE email = ?", Long.class, email);
    return new TestUser(userId, email, token);
  }

  private JsonNode responseData(MvcResult result) throws Exception {
    return objectMapper.readTree(result.getResponse().getContentAsByteArray()).get("data");
  }

  private String nextCursor(MvcResult result) throws Exception {
    return responseData(result).get("nextCursor").asText();
  }

  private int countRows(String table) {
    String sql = "SELECT COUNT(*) FROM " + table;
    return jdbcTemplate.queryForObject(sql, Integer.class);
  }

  private int countRows(String table, String condition) {
    String sql = "SELECT COUNT(*) FROM " + table + " WHERE " + condition;
    return jdbcTemplate.queryForObject(sql, Integer.class);
  }

  private LocalDateTime sentAt(int hour) {
    return LocalDateTime.of(2026, 1, 1, hour, 0);
  }

  private String bearer(TestUser user) {
    return "Bearer " + user.token();
  }

  private record TestUser(long id, String email, String token) {}
}
