// 웹 프론트 호출을 허용할 CORS 정책 설정을 바인딩한다.
package com.landit.landitbe.common.web;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "landit.cors")
public record CorsProperties(List<String> allowedOrigins) {

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
