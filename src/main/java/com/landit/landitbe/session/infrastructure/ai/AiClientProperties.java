// AI 서버 연동 설정 값을 바인딩한다.
package com.landit.landitbe.session.infrastructure.ai;

import com.landit.landitbe.session.application.port.AiConversationSettings;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "landit.ai")
public record AiClientProperties(
        String baseUrl,
        String clientMode,
        String serviceAudience,
        Duration connectTimeout,
        Duration requestTimeout
) implements AiConversationSettings {

    public AiClientProperties {
        if (clientMode == null || clientMode.isBlank()) {
            clientMode = "local";
        }
        if (serviceAudience == null || serviceAudience.isBlank()) {
            serviceAudience = "KOREAN_LEARNER";
        }
    }
}
