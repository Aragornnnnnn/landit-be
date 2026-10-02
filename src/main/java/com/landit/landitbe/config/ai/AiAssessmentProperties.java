// 호환 배포와 100점 평가 활성화를 분리하는 설정을 바인딩한다.

package com.landit.landitbe.config.ai;

import com.landit.landitbe.feature.learning.scenario.assessment.domain.AssessmentScale;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 요청할 평가 계약을 설정하며 기본값은 기존 5단계다.
 *
 * @param version 요청할 평가 계약. 미설정 시 기존 5단계 계약
 */
@ConfigurationProperties(prefix = "landit.ai.assessment")
public record AiAssessmentProperties(String version) {
  /**
   * 버전을 검증하고 안전한 기본 계약을 선택한다.
   *
   * @param version 요청할 계약 버전
   * @throws IllegalArgumentException 지원하지 않는 버전인 경우
   */
  public AiAssessmentProperties {
    version = version == null ? AssessmentScale.LEGACY.version() : version;
    AssessmentScale.fromVersion(version);
  }

  /**
   * 요청 설정을 평가 척도로 변환한다.
   *
   * @return 설정으로 요청하는 평가 척도
   */
  public AssessmentScale scale() {
    return AssessmentScale.fromVersion(version);
  }
}
