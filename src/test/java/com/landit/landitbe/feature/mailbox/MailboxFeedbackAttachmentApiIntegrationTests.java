// 문의 이미지의 저장·검증·접근 권한과 실패 보상을 검증한다.

package com.landit.landitbe.feature.mailbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.landitbe.feature.mailbox.feedback.attachment.client.MailboxAttachmentClient;
import com.landit.landitbe.feature.mailbox.feedback.attachment.repository.MailboxFeedbackAttachmentRepository;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 문의 이미지의 저장·검증·접근 권한과 실패 보상을 검증한다. */
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@TestPropertySource(
    properties = {
      "landit.auth.oidc.fake-enabled=true",
      "landit.auth.token.secret=landit-test-token-secret-that-is-long-enough"
    })
class MailboxFeedbackAttachmentApiIntegrationTests {

  @Autowired private MockMvc mvc;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private EntityManager entityManager;
  @MockitoBean private MailboxAttachmentClient storage;
  @MockitoSpyBean private MailboxFeedbackAttachmentRepository attachments;
  private final ObjectMapper mapper = new ObjectMapper();

  @BeforeEach
  void clearData() {
    reset(storage, attachments);
    jdbc.update("DELETE FROM mailbox_letter_recipient");
    jdbc.update("DELETE FROM mailbox_letter_read");
    jdbc.update("DELETE FROM mailbox_letter");
    jdbc.update("DELETE FROM mailbox_feedback_attachment");
    jdbc.update("DELETE FROM mailbox_feedback");
  }

  @Test
  void ownerAndAdminCanDownloadOrderedPrivateImages() throws Exception {
    String owner = login("owner", false);
    String other = login("other", false);
    String admin = login("admin", true);
    byte[] png = image("png");
    mvc.perform(request(owner).file(file("image/png", png)).file(file("image/jpeg", image("jpeg"))))
        .andExpect(status().isCreated());
    long feedbackId = feedbackId();
    String detail = "/api/v1/mailbox/sent/" + feedbackId;
    JsonNode data =
        mapper
            .readTree(
                mvc.perform(get(detail).header("Authorization", owner))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.attachments.length()").value(2))
                    .andExpect(jsonPath("$.data.attachments[0].contentType").value("image/png"))
                    .andExpect(jsonPath("$.data.attachments[1].contentType").value("image/jpeg"))
                    .andReturn()
                    .getResponse()
                    .getContentAsByteArray())
            .get("data");
    String url = data.get("attachments").get(0).get("downloadUrl").asText();
    mvc.perform(get("/api/v1/admin/mailbox/feedbacks/" + feedbackId).header("Authorization", admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.attachments[0].downloadUrl").value(url));
    mvc.perform(get(url).header("Authorization", other)).andExpect(status().isNotFound());
    mvc.perform(get(url)).andExpect(status().isUnauthorized());
    mvc.perform(
            get(url.replace("/feedbacks/" + feedbackId, "/feedbacks/999999"))
                .header("Authorization", admin))
        .andExpect(status().isNotFound());
    verify(storage, times(0)).download(anyString(), anyLong());
    when(storage.download(anyString(), anyLong())).thenReturn(png);
    for (String token : List.of(owner, admin)) {
      mvc.perform(get(url).header("Authorization", token))
          .andExpect(status().isOk())
          .andExpect(content().bytes(png))
          .andExpect(content().contentType("image/png"))
          .andExpect(header().string("Cache-Control", "private, no-store"))
          .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }
    assertThat(
            jdbc.queryForList("SELECT object_key FROM mailbox_feedback_attachment", String.class))
        .hasSize(2)
        .doesNotHaveDuplicates()
        .allSatisfy(key -> assertThat(key).startsWith("mailbox/feedback/"));
  }

  @Test
  void textOnlyJsonAndMultipartRemainSupported() throws Exception {
    String owner = login("text", false);
    mvc.perform(request(owner)).andExpect(status().isCreated());
    mvc.perform(
            post("/api/v1/mailbox/feedbacks")
                .header("Authorization", owner)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload()))
        .andExpect(status().isCreated());
    mvc.perform(get("/api/v1/mailbox/sent/" + feedbackId()).header("Authorization", owner))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.attachments").isEmpty());
    verifyNoInteractions(storage);
  }

  @Test
  void invalidImagesAreRejectedBeforeAnyUploadOrDatabaseWrite() throws Exception {
    String owner = login("invalid", false);
    byte[] png = image("png");
    for (MockMultipartFile invalid :
        List.of(
            file("image/png", new byte[0]),
            file("image/png", new byte[5 * 1024 * 1024 + 1]),
            file("image/jpeg", png),
            file("image/png", "not an image".getBytes()),
            file("image/svg+xml", "<svg/>".getBytes()),
            file("image/png", Arrays.copyOf(png, 24)))) {
      mvc.perform(request(owner).file(invalid)).andExpect(status().isBadRequest());
    }
    var tooMany = request(owner);
    for (int index = 0; index < 4; index++) {
      tooMany.file(file("image/png", png));
    }
    mvc.perform(tooMany).andExpect(status().isBadRequest());
    byte[] padded = Arrays.copyOf(png, 5 * 1024 * 1024);
    mvc.perform(
            request(owner)
                .file(file("image/png", padded))
                .file(file("image/png", padded))
                .file(file("image/png", png)))
        .andExpect(status().isBadRequest());
    mvc.perform(
            multipart("/api/v1/mailbox/feedbacks")
                .header("Authorization", owner)
                .file(file("image/png", png)))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(storage);
    assertNoRows();
  }

  @Test
  void malformedMultipartFeedbackIsRejectedWithoutUpload() throws Exception {
    String owner = login("invalid-feedback", false);
    for (String body : List.of("{}", "{\"type\":\"QUESTION\",\"content\":\" \"}")) {
      mvc.perform(
              multipart("/api/v1/mailbox/feedbacks")
                  .header("Authorization", owner)
                  .file(new MockMultipartFile("feedback", "", "application/json", body.getBytes()))
                  .file(file("image/png", image("png"))))
          .andExpect(status().isBadRequest());
    }
    mvc.perform(
            multipart("/api/v1/mailbox/feedbacks")
                .header("Authorization", owner)
                .file(new MockMultipartFile("feedback", "", "text/plain", payload().getBytes())))
        .andExpect(status().isUnsupportedMediaType());
    mvc.perform(request("").file(file("image/png", image("png"))))
        .andExpect(status().isUnauthorized());
    assertNoRows();
    verifyNoInteractions(storage);
  }

  @Test
  void failedSecondUploadCleansEveryAttemptEvenIfOneDeleteFails() throws Exception {
    String owner = login("upload-failure", false);
    doNothing()
        .doThrow(new ApiException(ErrorCode.SERVICE_UNAVAILABLE))
        .when(storage)
        .upload(anyString(), any(), anyString());
    doThrow(new IllegalStateException("cleanup failure"))
        .doNothing()
        .when(storage)
        .delete(anyString());
    mvc.perform(
            request(owner)
                .file(file("image/png", image("png")))
                .file(file("image/jpeg", image("jpeg"))))
        .andExpect(status().isServiceUnavailable());
    verify(storage, times(2)).delete(anyString());
    assertNoRows();
  }

  @Test
  void commitFailureRollsBackMetadataAndCleansUploadedObjects() throws Exception {
    String owner = login("commit-failure", false);
    doAnswer(
            invocation -> {
              Object saved = invocation.getArgument(0);
              entityManager.persist(saved);
              TransactionSynchronizationManager.registerSynchronization(
                  new TransactionSynchronization() {
                    @Override
                    public void beforeCommit(boolean readOnly) {
                      throw new ApiException(ErrorCode.SERVICE_UNAVAILABLE);
                    }
                  });
              return saved;
            })
        .when(attachments)
        .save(any());
    mvc.perform(request(owner).file(file("image/png", image("png"))))
        .andExpect(status().isServiceUnavailable());
    verify(storage).delete(anyString());
    assertNoRows();
  }

  @Test
  void openApiIncludesBothSubmissionFormatsAndPrivateDownload() throws Exception {
    String operation = "$.paths['/api/v1/mailbox/feedbacks'].post.requestBody.content";
    mvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath(operation + "['application/json']").exists())
        .andExpect(jsonPath(operation + "['multipart/form-data']").exists())
        .andExpect(
            jsonPath(
                    "$.paths['/api/v1/mailbox/feedbacks/{feedbackId}"
                        + "/attachments/{attachmentId}'].get.responses['200']")
                .exists());
  }

  private void assertNoRows() {
    assertThat(jdbc.queryForObject("SELECT count(*) FROM mailbox_feedback", Integer.class))
        .isZero();
    assertThat(
            jdbc.queryForObject("SELECT count(*) FROM mailbox_feedback_attachment", Integer.class))
        .isZero();
  }

  private long feedbackId() {
    return jdbc.queryForObject("SELECT max(id) FROM mailbox_feedback", Long.class);
  }

  private MockMultipartHttpServletRequestBuilder request(String token) {
    var request = multipart("/api/v1/mailbox/feedbacks");
    request.header("Authorization", token);
    return request.file(
        new MockMultipartFile(
            "feedback",
            "",
            "application/json",
            payload().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
  }

  private String payload() {
    return "{\"type\":\"QUESTION\",\"content\":\"첨부 문의\"}";
  }

  private MockMultipartFile file(String type, byte[] bytes) {
    return new MockMultipartFile("images", "untrusted-name", type, bytes);
  }

  private byte[] image(String format) throws Exception {
    var output = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), format, output);
    return output.toByteArray();
  }

  private String login(String key, boolean admin) throws Exception {
    String email = "attachment-" + key + "@example.com";
    String body =
        """
        {"provider":"GOOGLE","idToken":"attachment-%s|%s|User|nonce","nonce":"nonce"}
        """
            .formatted(key, email);
    byte[] response =
        mvc.perform(
                post("/api/v1/auth/social-login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    if (admin) {
      jdbc.update("UPDATE user_profile SET role = 'ADMIN' WHERE email = ?", email);
    }
    return "Bearer " + mapper.readTree(response).get("data").get("accessToken").asText();
  }
}
