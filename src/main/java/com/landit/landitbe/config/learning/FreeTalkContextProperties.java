// 프리톡 컨텍스트 요약 기능의 활성화와 기본 경계를 관리한다.

package com.landit.landitbe.config.learning;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 프리톡 컨텍스트 요약의 안전한 기본 설정을 보유한다.
 *
 * @param enabled 컨텍스트 요약 기능 활성화 여부
 * @param allowedUserIds 새 세션에 정책을 적용할 사용자 ID 목록
 * @param recentRounds 원문으로 보존할 최근 완료 왕복 수
 * @param summaryTriggerRounds 요약을 시작할 미요약 완료 왕복 수
 * @param summarySourceMaxBytes 요약 원문 구간의 초기 UTF-8 바이트 목표
 * @param leaseSeconds 요약 작업 선점 유효 시간(초)
 * @param retryDelaySeconds 실패 후 다음 시도까지 대기 시간(초)
 */
@ConfigurationProperties(prefix = "landit.free-talk.context")
public record FreeTalkContextProperties(
    boolean enabled,
    List<Long> allowedUserIds,
    int recentRounds,
    int summaryTriggerRounds,
    int summarySourceMaxBytes,
    int leaseSeconds,
    int retryDelaySeconds) {

  /**
   * 기능을 끈 보수적인 기본값을 적용한다.
   *
   * @param enabled 컨텍스트 요약 기능 활성화 여부
   * @param allowedUserIds 새 세션에 정책을 적용할 사용자 ID 목록
   * @param recentRounds 원문으로 보존할 최근 완료 왕복 수
   * @param summaryTriggerRounds 요약을 시작할 미요약 완료 왕복 수
   * @param summarySourceMaxBytes 요약 원문 구간의 초기 UTF-8 바이트 목표
   * @param leaseSeconds 요약 작업 선점 유효 시간(초)
   * @param retryDelaySeconds 실패 후 다음 시도까지 대기 시간(초)
   */
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
