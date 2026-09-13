// 프리톡 한도 설정의 검증과 신규·기존 환경변수 호환을 확인한다.

package com.landit.landitbe.config.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.ClassPathResource;

class FreeTalkPropertiesTest {

  @ParameterizedTest
  @CsvSource({"0,1000,20", "-1,1000,20", "7200000,0,20", "7200000,1000,-1"})
  void rejectsNonpositiveLimits(long speakingTime, int dailyRequests, int minuteRequests) {
    assertThatThrownBy(() -> new FreeTalkProperties(speakingTime, dailyRequests, minuteRequests))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest
  @CsvSource({"7200000,60000,7200000", ",90000,90000", ",,7200000"})
  void resolvesNewSettingBeforeLegacySetting(String dailyLimit, String legacyLimit, long expected) {
    Map<String, Object> overrides = new HashMap<>();
    if (dailyLimit != null) {
      overrides.put("LANDIT_FREE_TALK_DAILY_SPEAKING_TIME_LIMIT_MS", dailyLimit);
    }
    if (legacyLimit != null) {
      overrides.put("LANDIT_FREE_TALK_SPEAKING_TIME_LIMIT_MS", legacyLimit);
    }
    YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
    yaml.setResources(new ClassPathResource("application.yml"));
    MutablePropertySources sources = new MutablePropertySources();
    sources.addLast(new MapPropertySource("environment", overrides));
    sources.addLast(new PropertiesPropertySource("application", yaml.getObject()));
    PropertySourcesPropertyResolver resolver = new PropertySourcesPropertyResolver(sources);

    assertThat(resolver.getProperty("landit.free-talk.speaking-time-limit-ms", Long.class))
        .isEqualTo(expected);
  }
}
