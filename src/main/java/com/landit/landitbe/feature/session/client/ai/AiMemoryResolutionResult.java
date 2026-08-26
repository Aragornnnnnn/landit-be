// 장기기억 후보 상태 판정 AI 응답을 담는다.

package com.landit.landitbe.feature.session.client.ai;

import java.util.List;

/**
 * 후보별 장기기억 상태 판정 결과를 담는다.
 *
 * @param resolutions 후보별 상태 판정 목록
 */
public record AiMemoryResolutionResult(List<Resolution> resolutions) {

  /** 후보 한 건에 대한 상태 판정 결과를 담는다. */
  public record Resolution(
      Integer candidateIndex, AiMemoryOperation operation, List<Long> supersededMemoryIds) {}
}
