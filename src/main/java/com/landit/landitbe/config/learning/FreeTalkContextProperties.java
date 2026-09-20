// 프리톡 컨텍스트 요약 기능의 활성화와 기본 경계를 관리한다.

package com.landit.landitbe.config.learning;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** 프리톡 컨텍스트 요약의 안전한 기본 설정을 보유한다. */
@ConfigurationProperties(prefix = "landit.free-talk.context")
public record FreeTalkContextProperties(
    boolean enabled,
    List<Long> allowedUserIds,
    int recentRounds,
    int summaryTriggerRounds,
    int summarySourceMaxBytes,
    int leaseSeconds,
    int retryDelaySeconds) {

  /** 기능을 끈 보수적인 기본값을 적용한다. */
  public FreeTalkContextProperties {
    allowedUserIds = allowedUserIds == null ? List.of() : List.copyOf(allowedUserIds);
    if (recentRounds <= 0) {
      recentRounds = 8;
    }
    if (summaryTriggerRounds <= 0) {
      summaryTriggerRounds = 12;
    }
    if (summarySourceMaxBytes <= 0) {
      summarySourceMaxBytes = 6000;
    }
    if (leaseSeconds <= 0) {
      leaseSeconds = 30;
    }
    if (retryDelaySeconds <= 0) {
      retryDelaySeconds = 30;
    }
  }
}
