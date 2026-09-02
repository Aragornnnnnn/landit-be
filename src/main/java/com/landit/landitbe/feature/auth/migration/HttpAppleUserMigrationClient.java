// Apple 사용자 이전 REST API를 HTTP form 요청으로 호출한다.

package com.landit.landitbe.feature.auth.migration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/** Apple 사용자 이전 REST API를 HTTP form 요청으로 호출한다. */
public class HttpAppleUserMigrationClient implements AppleUserMigrationClient {

  private static final String CONTENT_TYPE = "application/x-www-form-urlencoded";

  private final HttpClient httpClient;
  private final URI baseUri;
  private final String clientId;
  private final String clientSecret;
  private final String recipientTeamId;
  private final Duration requestTimeout;
  private final ObjectMapper objectMapper;

  /**
   * Apple 사용자 이전 API 설정으로 HTTP 클라이언트를 생성한다.
   *
   * @param baseUri Apple ID API 기준 URI
   * @param clientId 이전 대상 앱의 App ID 또는 Services ID
   * @param clientSecret 현재 Team의 client secret JWT
   * @param recipientTeamId 수신 Team ID, COMPLETE 전용 실행이면 null 가능
   * @param requestTimeout HTTP 요청 제한 시간
   * @param objectMapper JSON 응답 파서
   */
  public HttpAppleUserMigrationClient(
      URI baseUri,
      String clientId,
      String clientSecret,
      String recipientTeamId,
      Duration requestTimeout,
      ObjectMapper objectMapper) {
    this.httpClient = HttpClient.newBuilder().connectTimeout(requestTimeout).build();
    this.baseUri = baseUri;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.recipientTeamId = recipientTeamId;
    this.requestTimeout = requestTimeout;
    this.objectMapper = objectMapper;
  }

  @Override
  public String requestAccessToken() {
    Map<String, String> form = new LinkedHashMap<>();
    form.put("grant_type", "client_credentials");
    form.put("scope", "user.migration");
    form.put("client_id", clientId);
    form.put("client_secret", clientSecret);
    JsonNode response = postForm("/auth/token", form, null);
    return requiredText(response, "access_token");
  }

  @Override
  public String createTransferSub(String accessToken, String providerUserId) {
    if (recipientTeamId == null || recipientTeamId.isBlank()) {
      throw new AppleUserMigrationException("RECIPIENT_TEAM_ID_REQUIRED");
    }
    Map<String, String> form = migrationCredentials();
    form.put("sub", providerUserId);
    form.put("target", recipientTeamId);
    JsonNode response = postForm("/auth/usermigrationinfo", form, accessToken);
    return requiredText(response, "transfer_sub");
  }

  @Override
  public AppleRecipientUser exchangeTransferSub(String accessToken, String transferSub) {
    Map<String, String> form = migrationCredentials();
    form.put("transfer_sub", transferSub);
    JsonNode response = postForm("/auth/usermigrationinfo", form, accessToken);
    return new AppleRecipientUser(requiredText(response, "sub"), optionalText(response, "email"));
  }

  private Map<String, String> migrationCredentials() {
    Map<String, String> form = new LinkedHashMap<>();
    form.put("client_id", clientId);
    form.put("client_secret", clientSecret);
    return form;
  }

  private JsonNode postForm(String path, Map<String, String> form, String accessToken) {
    HttpRequest.Builder request =
        HttpRequest.newBuilder(baseUri.resolve(path))
            .timeout(requestTimeout)
            .header("Content-Type", CONTENT_TYPE)
            .POST(HttpRequest.BodyPublishers.ofString(encodeForm(form)));
    if (accessToken != null) {
      request.header("Authorization", "Bearer " + accessToken);
    }

    HttpResponse<String> response;
    try {
      response = httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new AppleUserMigrationException("APPLE_REQUEST_INTERRUPTED", exception);
    } catch (IOException exception) {
      throw new AppleUserMigrationException("APPLE_REQUEST_FAILED", exception);
    }
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new AppleUserMigrationException("APPLE_HTTP_" + response.statusCode());
    }
    try {
      return objectMapper.readTree(response.body());
    } catch (IOException exception) {
      throw new AppleUserMigrationException("APPLE_RESPONSE_INVALID", exception);
    }
  }

  private String encodeForm(Map<String, String> form) {
    return form.entrySet().stream()
        .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
        .collect(Collectors.joining("&"));
  }

  private String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  private String requiredText(JsonNode response, String field) {
    String value = optionalText(response, field);
    if (value == null || value.isBlank()) {
      throw new AppleUserMigrationException("APPLE_RESPONSE_INVALID");
    }
    return value;
  }

  private String optionalText(JsonNode response, String field) {
    JsonNode value = response.get(field);
    return value == null || value.isNull() ? null : value.asText();
  }
}
