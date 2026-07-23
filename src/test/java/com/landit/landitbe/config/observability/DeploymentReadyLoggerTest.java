// 애플리케이션 준비 시 배포 버전 로그가 남는지 검증한다.

package com.landit.landitbe.config.observability;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/** 애플리케이션 준비 시 배포 버전 로그가 남는지 검증한다. */
@ExtendWith(OutputCaptureExtension.class)
class DeploymentReadyLoggerTest {

  @Test
  void logsDeploymentVersion(CapturedOutput output) {
    new DeploymentReadyLogger("be-v1.2.3").logDeploymentReady();

    assertThat(output).contains("workflow=deployment_started").contains("serviceVersion=be-v1.2.3");
  }
}
