// Landit 백엔드 애플리케이션의 진입점을 정의한다.

package com.landit.landitbe;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import jakarta.annotation.PostConstruct;
import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Landit 백엔드 애플리케이션의 진입점을 정의한다. */
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT")
@EnableScheduling
@ConfigurationPropertiesScan
@SpringBootApplication
public class LanditBeApplication {

  private static final String APPLICATION_TIME_ZONE = "Asia/Seoul";

  /**
   * Landit 백엔드 애플리케이션을 시작한다.
   *
   * @param args 애플리케이션 실행 인자
   */
  public static void main(String[] args) {
    SpringApplication.run(LanditBeApplication.class, args);
  }

  @PostConstruct
  void setApplicationTimeZone() {
    TimeZone.setDefault(TimeZone.getTimeZone(APPLICATION_TIME_ZONE));
  }
}
