// 웹 프론트 호출을 허용할 CORS 정책 설정을 바인딩한다.

package com.landit.landitbe.config.web;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 웹 프론트 호출을 허용할 CORS 정책 설정을 바인딩한다.
 *
 * @param allowedOrigins 허용할 Origin 목록
 */
@ConfigurationProperties(prefix = "landit.cors")
public record CorsProperties(List<String> allowedOrigins) {

  /** 허용 Origin 목록에서 공백과 빈 값을 제거한다. */
  public CorsProperties {
    allowedOrigins = normalize(allowedOrigins);
  }

  private static List<String> normalize(List<String> values) {
    if (values == null) {
      return List.of();
    }
    return values.stream().map(String::trim).filter(value -> !value.isBlank()).toList();
  }
}
