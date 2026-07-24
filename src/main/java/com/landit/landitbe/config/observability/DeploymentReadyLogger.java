// 애플리케이션 준비 시 현재 배포 버전을 구조화된 로그로 기록한다.

package com.landit.landitbe.config.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** 애플리케이션 준비 시 현재 배포 버전을 구조화된 로그로 기록한다. */
@Component
public class DeploymentReadyLogger {

  private static final Logger log = LoggerFactory.getLogger(DeploymentReadyLogger.class);

  private final String appVersion;

  DeploymentReadyLogger(@Value("${APP_VERSION:local}") String appVersion) {
    this.appVersion = appVersion;
  }

  @EventListener(ApplicationReadyEvent.class)
  void logDeploymentReady() {
    log.info("Landit BE 배포가 준비되었습니다. workflow=deployment_started serviceVersion={}", appVersion);
  }
}
