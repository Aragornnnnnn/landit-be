// DB 프로필 설정이 환경변수 placeholder만 사용하는지 검증한다.

package com.landit.landitbe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

/** DB 프로필 설정이 환경변수 placeholder만 사용하는지 검증한다. */
class DatabaseProfileConfigurationTests {

  private final YamlPropertySourceLoader yamlLoader = new YamlPropertySourceLoader();

  @ParameterizedTest
  @ValueSource(strings = {"local", "develop", "prod"})
  void databaseProfilesReadConnectionSettingsFromEnvironment(String profile) throws IOException {
    PropertySource<?> propertySource =
        yamlLoader
            .load(profile, new ClassPathResource("application-" + profile + ".yml"))
            .getFirst();

    assertEquals("${DB_URL}", propertySource.getProperty("spring.datasource.url"));
    assertEquals("${DB_USERNAME}", propertySource.getProperty("spring.datasource.username"));
    assertEquals("${DB_PASSWORD}", propertySource.getProperty("spring.datasource.password"));
  }
}
