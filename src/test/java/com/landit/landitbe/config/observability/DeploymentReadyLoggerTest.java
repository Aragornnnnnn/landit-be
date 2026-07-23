// 애플리케이션 준비 시 배포 버전 로그가 남는지 검증한다.

package com.landit.landitbe.config.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/** 애플리케이션 준비 시 배포 버전 로그가 남는지 검증한다. */
@ExtendWith(OutputCaptureExtension.class)
class DeploymentReadyLoggerTest {

  @Test
  void logsDeploymentVersionWhenApplicationReadyEventIsPublished(CapturedOutput output) {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      TestPropertyValues.of("APP_VERSION=be-v1.2.3").applyTo(context);
      context.registerBean(DeploymentReadyLogger.class);
      context.refresh();
      context.publishEvent(
          new ApplicationReadyEvent(
              new SpringApplication(DeploymentReadyLogger.class),
              new String[0],
              context,
              Duration.ZERO));
    }

    assertThat(output).contains("workflow=deployment_started").contains("serviceVersion=be-v1.2.3");
  }
}
