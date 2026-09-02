// Apple 사용자 이전 HTTP 클라이언트의 공식 API 요청과 응답 계약을 검증한다.

package com.landit.landitbe.feature.auth.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
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

  @Test
  void acceptsRecipientResponseWithoutPrivateRelayEmail() {
    respond(200, "{\"sub\":\"new-sub\",\"is_private_email\":false}");

    assertThat(client.exchangeTransferSub("access-token", "transfer-sub"))
        .isEqualTo(new AppleRecipientUser("new-sub", null));
  }

  @Test
  void rejectsSuccessfulResponseWhenRequiredIdentifierIsMissing() {
    respond(200, "{\"email\":\"private@privaterelay.appleid.com\"}");

    assertThatThrownBy(() -> client.exchangeTransferSub("access-token", "transfer-sub"))
        .isInstanceOf(AppleUserMigrationException.class)
        .extracting("failureCode")
        .isEqualTo("APPLE_RESPONSE_INVALID");
  }

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
