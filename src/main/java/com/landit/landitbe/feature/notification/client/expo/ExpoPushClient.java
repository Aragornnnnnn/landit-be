// Expo Push API를 호출해 Push Ticket과 Receipt 결과를 변환한다.

package com.landit.landitbe.feature.notification.client.expo;

import com.landit.landitbe.config.notification.NotificationProperties;
import com.landit.landitbe.feature.notification.client.NotificationSender;
import com.landit.landitbe.feature.notification.client.PushMessage;
import com.landit.landitbe.feature.notification.client.PushNotificationException;
import com.landit.landitbe.feature.notification.client.PushReceiptResult;
import com.landit.landitbe.feature.notification.client.PushTicketResult;
import com.landit.landitbe.feature.notification.client.RetryablePushNotificationException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Expo Push API를 호출해 Push Ticket과 Receipt 결과를 변환한다. */
@Component
public class ExpoPushClient implements NotificationSender {

  private static final String SEND_PATH = "/--/api/v2/push/send";
  private static final String RECEIPT_PATH = "/--/api/v2/push/getReceipts";
  private static final String UNKNOWN_ERROR_CODE = "EXPO_REQUEST_REJECTED";

  private final HttpClient httpClient;
  private final JsonMapper jsonMapper;
  private final NotificationProperties properties;

  /**
   * JSON 변환기와 Expo 연결 설정으로 Client를 구성한다.
   *
   * @param jsonMapper 요청과 응답 JSON 변환기
   * @param properties Expo 연결 설정
   */
  public ExpoPushClient(JsonMapper jsonMapper, NotificationProperties properties) {
    this.jsonMapper = jsonMapper;
    this.properties = properties;
    this.httpClient = HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).build();
  }

  /** {@inheritDoc} */
  @Override
  public List<PushTicketResult> send(List<PushMessage> messages) {
    if (messages.isEmpty()) {
      return List.of();
    }
    if (messages.size() > 100) {
      throw new IllegalArgumentException("Expo Push 발송은 한 요청에 최대 100건까지 가능합니다.");
    }

    List<ExpoPushRequest> request = messages.stream().map(this::request).toList();
    HttpResponse<String> response = post(SEND_PATH, request);
    if (isTemporaryFailure(response.statusCode())) {
      throw new RetryablePushNotificationException("Expo Push 발송 요청이 일시적으로 실패했습니다.");
    }
    if (!isSuccess(response.statusCode())) {
      return java.util.Collections.nCopies(
          messages.size(), PushTicketResult.failed(readRequestErrorCode(response.body())));
    }
    return readTickets(response.body(), messages.size());
  }

  /** {@inheritDoc} */
  @Override
  public PushReceiptResult getReceipt(String ticketId) {
    HttpResponse<String> response = post(RECEIPT_PATH, new ExpoReceiptRequest(List.of(ticketId)));
    if (isTemporaryFailure(response.statusCode())) {
      throw new RetryablePushNotificationException("Expo Push Receipt 요청이 일시적으로 실패했습니다.");
    }
    if (!isSuccess(response.statusCode())) {
      return PushReceiptResult.failed(readRequestErrorCode(response.body()));
    }
    return readReceipt(response.body(), ticketId);
  }

  /** Expo API에 JSON POST 요청을 보내고 HTTP 응답을 반환한다. */
  private HttpResponse<String> post(String path, Object payload) {
    try {
      HttpRequest.Builder requestBuilder =
          HttpRequest.newBuilder(baseUri().resolve(path))
              .version(HttpClient.Version.HTTP_1_1)
              .header("Accept", "application/json")
              .header("Content-Type", "application/json")
              .timeout(properties.requestTimeout())
              .POST(
                  HttpRequest.BodyPublishers.ofString(
                      jsonMapper.writeValueAsString(payload), StandardCharsets.UTF_8));
      if (properties.expoAccessToken() != null && !properties.expoAccessToken().isBlank()) {
        requestBuilder.header("Authorization", "Bearer " + properties.expoAccessToken());
      }
      return httpClient.send(
          requestBuilder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new PushNotificationException("Expo Push 요청이 중단됐습니다.", exception);
    } catch (HttpTimeoutException exception) {
      throw new RetryablePushNotificationException("Expo Push 요청 제한시간을 초과했습니다.", exception);
    } catch (IOException | JacksonException exception) {
      throw new PushNotificationException("Expo Push 요청에 실패했습니다.", exception);
    }
  }

  /** Push 메시지를 Expo 요청 형식으로 변환한다. */
  private ExpoPushRequest request(PushMessage message) {
    return new ExpoPushRequest(
        message.expoPushToken(),
        message.title(),
        message.body(),
        new ExpoPushData(message.deepLink()),
        "default",
        "default");
  }

  /** Expo 발송 응답에서 요청 순서와 같은 Push Ticket 결과 목록을 읽는다. */
  private List<PushTicketResult> readTickets(String responseBody, int expectedTicketCount) {
    try {
      JsonNode root = jsonMapper.readTree(responseBody);
      JsonNode data = root.get("data");
      if (data == null || !data.isArray() || data.size() != expectedTicketCount) {
        throw malformedResponse();
      }
      return java.util.stream.StreamSupport.stream(data.spliterator(), false)
          .map(this::readTicket)
          .toList();
    } catch (PushNotificationException exception) {
      throw exception;
    } catch (JacksonException exception) {
      throw malformedResponse(exception);
    }
  }

  /** Expo Ticket JSON 하나를 Push Ticket 결과로 변환한다. */
  private PushTicketResult readTicket(JsonNode ticket) {
    if ("ok".equals(ticket.path("status").asString())) {
      String ticketId = ticket.path("id").asString();
      if (ticketId.isBlank()) {
        throw malformedResponse();
      }
      return PushTicketResult.accepted(ticketId);
    }
    if ("error".equals(ticket.path("status").asString())) {
      return PushTicketResult.failed(readDetailErrorCode(ticket));
    }
    throw malformedResponse();
  }

  /** Expo Receipt 응답에서 요청한 Ticket ID의 결과를 읽는다. */
  private PushReceiptResult readReceipt(String responseBody, String ticketId) {
    try {
      JsonNode root = jsonMapper.readTree(responseBody);
      JsonNode data = root.get("data");
      if (data == null || !data.isObject()) {
        throw malformedResponse();
      }
      JsonNode receipt = data.get(ticketId);
      if (receipt == null) {
        return PushReceiptResult.notReady();
      }
      if ("ok".equals(receipt.path("status").asString())) {
        return PushReceiptResult.delivered();
      }
      if ("error".equals(receipt.path("status").asString())) {
        return PushReceiptResult.failed(readDetailErrorCode(receipt));
      }
      throw malformedResponse();
    } catch (PushNotificationException exception) {
      throw exception;
    } catch (JacksonException exception) {
      throw malformedResponse(exception);
    }
  }

  /** HTTP 4xx 전체 요청 오류에서 Expo 오류 코드를 읽는다. */
  private String readRequestErrorCode(String responseBody) {
    try {
      JsonNode errors = jsonMapper.readTree(responseBody).path("errors");
      if (errors.isArray() && !errors.isEmpty()) {
        String errorCode = errors.get(0).path("code").asString();
        if (!errorCode.isBlank()) {
          return errorCode;
        }
      }
    } catch (JacksonException ignored) {
      // 해석할 수 없는 4xx 응답은 외부 요청 거부 코드로 통일한다.
    }
    return UNKNOWN_ERROR_CODE;
  }

  /** Ticket 또는 Receipt 결과의 details.error 값을 읽는다. */
  private String readDetailErrorCode(JsonNode result) {
    String errorCode = result.path("details").path("error").asString();
    return errorCode.isBlank() ? UNKNOWN_ERROR_CODE : errorCode;
  }

  /** HTTP 상태가 성공 범위인지 확인한다. */
  private boolean isSuccess(int statusCode) {
    return statusCode >= 200 && statusCode < 300;
  }

  /** HTTP 상태가 SQS 재시도로 회복 가능한 Expo 오류인지 확인한다. */
  private boolean isTemporaryFailure(int statusCode) {
    return statusCode == 429 || (statusCode >= 500 && statusCode < 600);
  }

  /** 설정된 Expo 기본 URL을 URI로 변환한다. */
  private URI baseUri() {
    return URI.create(properties.expoBaseUrl());
  }

  /** 원인 예외가 없는 Expo 응답 형식 오류를 생성한다. */
  private PushNotificationException malformedResponse() {
    return new PushNotificationException("Expo Push 응답 형식이 올바르지 않습니다.");
  }

  /** 원인 예외를 포함한 Expo 응답 형식 오류를 생성한다. */
  private PushNotificationException malformedResponse(Throwable cause) {
    return new PushNotificationException("Expo Push 응답 형식이 올바르지 않습니다.", cause);
  }

  /** Expo 발송 API에 전달하는 단일 Push 요청 형식이다. */
  private record ExpoPushRequest(
      String to, String title, String body, ExpoPushData data, String sound, String channelId) {}

  /** 알림을 탭했을 때 앱이 이동할 경로를 담는다. */
  private record ExpoPushData(String url) {}

  /** Expo Receipt API에 전달하는 Ticket ID 목록이다. */
  private record ExpoReceiptRequest(List<String> ids) {}
}
