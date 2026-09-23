// Apple 사용자 이전 HTTP 클라이언트의 공식 API 요청과 응답 계약을 검증한다.

package com.landit.landitbe.feature.auth.migration.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.landitbe.feature.auth.migration.dto.AppleRecipientUser;
import com.landit.landitbe.feature.auth.migration.exception.AppleUserMigrationException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HttpAppleUserMigrationClientTest {

  private HttpServer server;
  private AtomicReference<CapturedRequest> capturedRequest;
  private AtomicReference<StubResponse> stubResponse;
  private HttpAppleUserMigrationClient client;

  @BeforeEach
  void setUp() throws IOException {
    capturedRequest = new AtomicReference<>();
    stubResponse = new AtomicReference<>();
    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext("/", this::handle);
    server.start();
    URI baseUri = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
    client =
        new HttpAppleUserMigrationClient(
            baseUri,
            "app.client",
            "client-secret",
            "RECIPIENT_TEAM",
            Duration.ofSeconds(2),
            new ObjectMapper());
  }

  @AfterEach
  void tearDown() {
    server.stop(0);
  }

  @DisplayName("Apple 사용자 이전 토큰은 client_credentials와 이전 scope로 요청한다.")
  @Test
  void requestsMigrationTokenWithClientCredentialsScope() {
    respond(
        200, "{\"access_token\":\"access-token\",\"token_type\":\"Bearer\",\"expires_in\":3600}");

    assertThat(client.requestAccessToken()).isEqualTo("access-token");

    CapturedRequest request = capturedRequest.get();
    assertThat(request.path()).isEqualTo("/auth/token");
    assertThat(request.authorization()).isNull();
    assertThat(request.form())
        .containsExactlyInAnyOrderEntriesOf(
            Map.of(
                "grant_type",
                "client_credentials",
                "scope",
                "user.migration",
                "client_id",
                "app.client",
                "client_secret",
                "client-secret"));
  }

  @DisplayName("기존 Apple sub와 수신 팀 ID로 이전 식별자를 발급한다.")
  @Test
  void createsTransferIdentifierWithOldSubAndRecipientTeam() {
    respond(200, "{\"transfer_sub\":\"transfer-sub\"}");

    assertThat(client.createTransferSub("access-token", "old-sub")).isEqualTo("transfer-sub");

    CapturedRequest request = capturedRequest.get();
    assertThat(request.path()).isEqualTo("/auth/usermigrationinfo");
    assertThat(request.authorization()).isEqualTo("Bearer access-token");
    assertThat(request.form())
        .containsExactlyInAnyOrderEntriesOf(
            Map.of(
                "sub",
                "old-sub",
                "target",
                "RECIPIENT_TEAM",
                "client_id",
                "app.client",
                "client_secret",
                "client-secret"));
  }

  @DisplayName("Apple 이전 식별자를 수신 팀의 사용자 정보로 교환한다.")
  @Test
  void exchangesTransferIdentifierForRecipientUser() {
    respond(
        200,
        """
        {"sub":"new-sub","email":"new@privaterelay.appleid.com","is_private_email":true}
        """);

    assertThat(client.exchangeTransferSub("access-token", "transfer-sub"))
        .isEqualTo(new AppleRecipientUser("new-sub", "new@privaterelay.appleid.com"));

    CapturedRequest request = capturedRequest.get();
    assertThat(request.authorization()).isEqualTo("Bearer access-token");
    assertThat(request.form())
        .containsExactlyInAnyOrderEntriesOf(
            Map.of(
                "transfer_sub",
                "transfer-sub",
                "client_id",
                "app.client",
                "client_secret",
                "client-secret"));
  }

  @DisplayName("Apple 이전 응답에 비공개 릴레이 이메일이 없어도 허용한다.")
  @Test
  void acceptsRecipientResponseWithoutPrivateRelayEmail() {
    respond(200, "{\"sub\":\"new-sub\",\"is_private_email\":false}");

    assertThat(client.exchangeTransferSub("access-token", "transfer-sub"))
        .isEqualTo(new AppleRecipientUser("new-sub", null));
  }

  @DisplayName("Apple 성공 응답에 필수 식별자가 없으면 거부한다.")
  @Test
  void rejectsSuccessfulResponseWhenRequiredIdentifierIsMissing() {
    respond(200, "{\"email\":\"private@privaterelay.appleid.com\"}");

    assertThatThrownBy(() -> client.exchangeTransferSub("access-token", "transfer-sub"))
        .isInstanceOf(AppleUserMigrationException.class)
        .extracting("failureCode")
        .isEqualTo("APPLE_RESPONSE_INVALID");
  }

  @DisplayName("Apple HTTP 오류에 응답 본문과 자격 증명을 노출하지 않는다.")
  @Test
  void sanitizesAppleHttpErrorsWithoutResponseOrCredentials() {
    respond(400, "{\"error\":\"invalid_client_secret client-secret transfer-sub\"}");

    assertThatThrownBy(() -> client.exchangeTransferSub("access-token", "transfer-sub"))
        .isInstanceOf(AppleUserMigrationException.class)
        .satisfies(
            exception -> {
              AppleUserMigrationException migrationException =
                  (AppleUserMigrationException) exception;
              assertThat(migrationException.failureCode()).isEqualTo("APPLE_HTTP_400");
              assertThat(migrationException.getMessage())
                  .doesNotContain("client-secret", "transfer-sub", "invalid_client_secret");
            });
  }

  private void respond(int statusCode, String body) {
    stubResponse.set(new StubResponse(statusCode, body));
  }

  private void handle(HttpExchange exchange) throws IOException {
    String requestBody =
        new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    capturedRequest.set(
        new CapturedRequest(
            exchange.getRequestURI().getPath(),
            exchange.getRequestHeaders().getFirst("Authorization"),
            parseForm(requestBody)));
    StubResponse response = stubResponse.get();
    byte[] responseBody = response.body().getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "application/json");
    exchange.sendResponseHeaders(response.statusCode(), responseBody.length);
    exchange.getResponseBody().write(responseBody);
    exchange.close();
  }

  private Map<String, String> parseForm(String body) {
    Map<String, String> form = new LinkedHashMap<>();
    Arrays.stream(body.split("&"))
        .map(pair -> pair.split("=", 2))
        .forEach(
            pair ->
                form.put(
                    URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(pair[1], StandardCharsets.UTF_8)));
    return form;
  }

  private record CapturedRequest(String path, String authorization, Map<String, String> form) {}

  private record StubResponse(int statusCode, String body) {}
}
