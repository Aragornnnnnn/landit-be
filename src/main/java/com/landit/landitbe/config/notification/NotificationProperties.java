// Expo Push Service 연결 설정 값을 바인딩한다.

package com.landit.landitbe.config.notification;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Expo Push Service 연결 설정 값을 바인딩한다.
 *
 * @param expoBaseUrl Expo Push Service 기본 URL
 * @param expoAccessToken 선택 Expo Push 보안 Access Token
 * @param connectTimeout Expo 연결 제한 시간
 * @param requestTimeout Expo 요청 제한 시간
 */
@ConfigurationProperties(prefix = "landit.notification")
public record NotificationProperties(
    String expoBaseUrl, String expoAccessToken, Duration connectTimeout, Duration requestTimeout) {}
