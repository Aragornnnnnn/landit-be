// 공용 fasterxml ObjectMapper 빈을 구성한다.

package com.landit.landitbe.config.jackson;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 공용 fasterxml ObjectMapper 빈을 구성한다.
 *
 * <p>JPA jsonb 컬럼({@code JsonNode})과 S3 매니페스트 파싱은 fasterxml 계열을 쓴다 (HTTP 계층의 tools.jackson과 별개).
 * 서비스마다 매퍼를 새로 만들지 않도록 여기서 한 번만 만들어 주입한다.
 */
@Configuration
public class JacksonConfig {

  /**
   * 알 수 없는 필드를 무시하는 관대한 ObjectMapper를 만든다.
   *
   * <p>S3 매니페스트에 배치 메타데이터 같은 추가 필드가 있어도 파싱이 실패하지 않게 한다.
   *
   * @return 공용 ObjectMapper
   */
  @Bean
  public ObjectMapper objectMapper() {
    return new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  }
}
