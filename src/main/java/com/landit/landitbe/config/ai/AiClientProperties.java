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
 */
@ConfigurationProperties(prefix = "landit.ai")
public record AiClientProperties(
    String baseUrl,
    String clientMode,
    String serviceAudience,
    Duration connectTimeout,
    Duration requestTimeout,
    Duration sessionFeedbackRequestTimeout)
    implements AiConversationSettings {

  /** 비어 있는 AI 클라이언트 모드와 서비스 대상을 기본값으로 정규화한다. */
  public AiClientProperties {
    if (clientMode == null || clientMode.isBlank()) {
      clientMode = "local";
    }
    if (serviceAudience == null || serviceAudience.isBlank()) {
      serviceAudience = "KOREAN_LEARNER";
    }
  }
}
