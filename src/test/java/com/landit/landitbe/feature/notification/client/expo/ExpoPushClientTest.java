// Expo Push API 요청 형식과 Ticket·Receipt 응답 변환을 검증한다.

package com.landit.landitbe.feature.notification.client.expo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.landit.landitbe.config.notification.NotificationProperties;
import com.landit.landitbe.feature.notification.client.PushMessage;
import com.landit.landitbe.feature.notification.client.PushNotificationException;
import com.landit.landitbe.feature.notification.client.PushReceiptStatus;
import com.landit.landitbe.feature.notification.client.PushTicketResult;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Expo Push API 요청 형식과 Ticket·Receipt 응답 변환을 검증한다. */
class ExpoPushClientTest {

  private static final String SEND_PATH = "/--/api/v2/push/send";
  private static final String RECEIPT_PATH = "/--/api/v2/push/getReceipts";

  private final JsonMapper jsonMapper = JsonMapper.builder().build();
  private HttpServer server;

  /** 각 테스트가 사용할 로컬 Expo 대역 서버를 시작한다. */
  @BeforeEach
  void startServer() throws Exception {
    server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    server.start();
  }

  /** 테스트 종료 후 로컬 Expo 대역 서버를 중지한다. */
  @AfterEach
  void stopServer() {
    server.stop(0);
  }

  /** 정해진 여섯 필드와 선택 Access Token으로 알림을 보내고 Ticket ID를 반환한다. */
  @Test
  void sendsPushMessageAndMapsAcceptedTicket() throws Exception {
    AtomicReference<String> requestBody = new AtomicReference<>();
    AtomicReference<String> authorization = new AtomicReference<>();
    server.createContext(
        SEND_PATH,
        exchange -> {
          requestBody.set(readBody(exchange));
          authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
          respond(exchange, 200, "{\"data\":{\"status\":\"ok\",\"id\":\"ticket-1\"}}");
        });

    PushTicketResult result = expoPushClient("expo-access-token").send(reviewReminder());

    JsonNode request = jsonMapper.readTree(requestBody.get());
    assertThat(request.propertyNames())
        .containsExactlyInAnyOrder("to", "title", "body", "data", "sound", "channelId");
    assertThat(request.get("to").asString()).isEqualTo("ExponentPushToken[device-token]");
    assertThat(request.get("title").asString()).isEqualTo("복습할 시간이에요");
    assertThat(request.get("body").asString()).isEqualTo("오늘의 표현을 다시 볼까요?");
    assertThat(request.get("data").get("url").asString())
        .isEqualTo(
            "/expressions?utm_source=push&utm_medium=notification&utm_campaign=review_reminder");
    assertThat(request.get("sound").asString()).isEqualTo("default");
    assertThat(request.get("channelId").asString()).isEqualTo("default");
    assertThat(authorization.get()).isEqualTo("Bearer expo-access-token");
    assertThat(result.accepted()).isTrue();
    assertThat(result.ticketId()).isEqualTo("ticket-1");
    assertThat(result.errorCode()).isNull();
  }

  /** Expo Access Token이 없으면 Authorization Header를 보내지 않는다. */
  @Test
  void omitsAuthorizationHeaderWithoutExpoAccessToken() {
    AtomicReference<String> authorization = new AtomicReference<>();
    server.createContext(
        SEND_PATH,
        exchange -> {
          authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
          respond(exchange, 200, "{\"data\":{\"status\":\"ok\",\"id\":\"ticket-1\"}}");
        });

    expoPushClient(null).send(reviewReminder());

    assertThat(authorization.get()).isNull();
  }

  /** Ticket 단위 Expo 오류를 재시도 예외가 아닌 실패 결과로 변환한다. */
  @Test
  void mapsRejectedTicketError() {
    stubResponse(
        SEND_PATH,
        200,
        """
        {"data":{"status":"error","details":{"error":"DeviceNotRegistered"}}}
        """);

    PushTicketResult result = expoPushClient(null).send(reviewReminder());

    assertThat(result.accepted()).isFalse();
    assertThat(result.ticketId()).isNull();
    assertThat(result.errorCode()).isEqualTo("DeviceNotRegistered");
  }

  /** Expo가 Receipt 성공을 반환하면 배달 완료 결과로 변환한다. */
  @Test
  void mapsDeliveredReceipt() {
    stubResponse(RECEIPT_PATH, 200, "{\"data\":{\"ticket-1\":{\"status\":\"ok\"}}}");

    var result = expoPushClient(null).getReceipt("ticket-1");

    assertThat(result.status()).isEqualTo(PushReceiptStatus.DELIVERED);
    assertThat(result.errorCode()).isNull();
  }

  /** 요청한 Ticket ID가 응답에 없으면 Receipt 미준비 결과로 변환한다. */
  @Test
  void mapsMissingReceiptAsNotReady() {
    AtomicReference<String> requestBody = new AtomicReference<>();
    server.createContext(
        RECEIPT_PATH,
        exchange -> {
          requestBody.set(readBody(exchange));
          respond(exchange, 200, "{\"data\":{}}");
        });

    var result = expoPushClient(null).getReceipt("ticket-1");

    assertThat(jsonMapper.readTree(requestBody.get()).get("ids").get(0).asString())
        .isEqualTo("ticket-1");
    assertThat(result.status()).isEqualTo(PushReceiptStatus.NOT_READY);
  }

  /** Receipt 단위 Expo 오류 코드가 있는 응답을 배달 실패 결과로 변환한다. */
  @Test
  void mapsFailedReceiptError() {
    stubResponse(
        RECEIPT_PATH,
        200,
        """
        {"data":{"ticket-1":{"status":"error","details":{"error":"DeviceNotRegistered"}}}}
        """);

    var result = expoPushClient(null).getReceipt("ticket-1");

    assertThat(result.status()).isEqualTo(PushReceiptStatus.FAILED);
    assertThat(result.errorCode()).isEqualTo("DeviceNotRegistered");
  }

  /** HTTP 400 전체 요청 오류는 Ticket 실패 결과로 변환한다. */
  @Test
  void mapsNonRetryableRequestError() {
    stubResponse(
        SEND_PATH,
        400,
        """
        {"errors":[{"code":"PUSH_TOO_MANY_NOTIFICATIONS"}]}
        """);

    PushTicketResult result = expoPushClient(null).send(reviewReminder());

    assertThat(result.accepted()).isFalse();
    assertThat(result.errorCode()).isEqualTo("PUSH_TOO_MANY_NOTIFICATIONS");
  }

  /** HTTP 429와 5xx는 SQS 재시도를 유도하는 예외로 변환한다. */
  @Test
  void throwsRetryableExceptionForTemporaryHttpFailure() {
    AtomicInteger requestCount = new AtomicInteger();
    server.createContext(
        SEND_PATH,
        exchange ->
            respond(exchange, requestCount.getAndIncrement() == 0 ? 429 : 503, "{\"errors\":[]}"));

    assertThatThrownBy(() -> expoPushClient(null).send(reviewReminder()))
        .isInstanceOf(PushNotificationException.class);
    assertThatThrownBy(() -> expoPushClient(null).send(reviewReminder()))
        .isInstanceOf(PushNotificationException.class);
  }

  /** Expo 응답 JSON이 계약과 다르면 재시도 가능한 예외로 변환한다. */
  @Test
  void throwsRetryableExceptionForMalformedResponse() {
    stubResponse(SEND_PATH, 200, "{\"data\":{}}");

    assertThatThrownBy(() -> expoPushClient(null).send(reviewReminder()))
        .isInstanceOf(PushNotificationException.class);
  }

  /** 테스트용 설정으로 Expo Push Client를 생성한다. */
  private ExpoPushClient expoPushClient(String accessToken) {
    NotificationProperties properties =
        new NotificationProperties(
            "http://localhost:" + server.getAddress().getPort(),
            accessToken,
            Duration.ofSeconds(1),
            Duration.ofSeconds(2));
    return new ExpoPushClient(jsonMapper, properties);
  }

  /** 복습 리마인더 Expo 메시지를 생성한다. */
  private PushMessage reviewReminder() {
    return new PushMessage(
        "ExponentPushToken[device-token]",
        "복습할 시간이에요",
        "오늘의 표현을 다시 볼까요?",
        "/expressions?utm_source=push&utm_medium=notification&utm_campaign=review_reminder");
  }

  /** HTTP 요청 본문을 UTF-8 문자열로 읽는다. */
  private String readBody(HttpExchange exchange) throws IOException {
    return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
  }

  /** 고정된 Expo HTTP 응답을 반환하도록 테스트 서버를 설정한다. */
  private void stubResponse(String path, int status, String response) {
    server.createContext(path, exchange -> respond(exchange, status, response));
  }

  /** 로컬 Expo 대역 서버의 HTTP 응답을 작성한다. */
  private void respond(HttpExchange exchange, int status, String response) throws IOException {
    byte[] responseBody = response.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().add("Content-Type", "application/json");
    exchange.sendResponseHeaders(status, responseBody.length);
    exchange.getResponseBody().write(responseBody);
    exchange.close();
  }
}
