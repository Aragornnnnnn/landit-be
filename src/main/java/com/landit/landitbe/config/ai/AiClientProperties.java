// AI 서버 연동 설정 값을 바인딩한다.

package com.landit.landitbe.config.ai;

import com.landit.landitbe.feature.session.client.ai.AiConversationSettings;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 서버 연동 설정 값을 바인딩한다.
 *
 * @param baseUrl AI 서버 기본 URL
 * @param clientMode AI 클라이언트 실행 모드
 * @param serviceAudience AI 서비스 대상 사용자군
 * @param connectTimeout AI 서버 연결 제한 시간
 * @param requestTimeout 일반 AI 요청 제한 시간
 * @param sessionFeedbackRequestTimeout 최종 피드백 AI 요청 제한 시간
 * @param pronunciationRequestTimeout 발음 평가 AI 요청 제한 시간
 */
@ConfigurationProperties(prefix = "landit.ai")
public record AiClientProperties(
    String baseUrl,
    String clientMode,
    String serviceAudience,
    Duration connectTimeout,
    Duration requestTimeout,
    Duration sessionFeedbackRequestTimeout,
    Duration pronunciationRequestTimeout,
    String internalToken)
    implements AiConversationSettings {

  /** 기존 클라이언트 설정은 내부 인증 토큰 없이 초기화한다. */
  public AiClientProperties(
      String baseUrl,
      String clientMode,
      String serviceAudience,
      Duration connectTimeout,
      Duration requestTimeout,
      Duration sessionFeedbackRequestTimeout,
      Duration pronunciationRequestTimeout) {
    this(
        baseUrl,
        clientMode,
        serviceAudience,
        connectTimeout,
        requestTimeout,
        sessionFeedbackRequestTimeout,
        pronunciationRequestTimeout,
        "");
  }

  /** 비어 있는 AI 클라이언트 모드와 서비스 대상, 발음 평가 제한 시간을 기본값으로 정규화한다. */
  @org.springframework.boot.context.properties.bind.ConstructorBinding
  public AiClientProperties {
    internalToken = internalToken == null ? "" : internalToken.trim();
    if (clientMode == null || clientMode.isBlank()) {
      clientMode = "local";
    }
    if (serviceAudience == null || serviceAudience.isBlank()) {
      serviceAudience = "KOREAN_LEARNER";
    }
    if (pronunciationRequestTimeout == null) {
      pronunciationRequestTimeout = Duration.ofSeconds(20);
    }
  }

  /** 모든 AI HTTP 클라이언트가 같은 내부 인증 헤더를 전달한다. */
  public java.net.http.HttpRequest.Builder authorize(java.net.http.HttpRequest.Builder builder) {
    return internalToken.isBlank()
        ? builder
        : builder.header("X-Landit-Internal-Token", internalToken);
  }

  /** 설정 객체를 출력하더라도 내부 토큰은 노출하지 않는다. */
  @Override
  public String toString() {
    return "AiClientProperties[clientMode=" + clientMode + ", internalToken=***]";
  }
}
