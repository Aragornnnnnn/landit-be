// 장기기억 후보 상태 판정 AI 응답을 담는다.

package com.landit.landitbe.feature.memory.client.ai;

import java.util.List;

/** 장기기억 후보 상태 판정 AI 응답을 담는다. */
public record AiMemoryResolutionResult(List<Resolution> resolutions) {

  /** 후보 하나에 대한 장기기억 상태 판정 결과를 담는다. */
  public record Resolution(
      Integer candidateIndex, AiMemoryOperation operation, List<Long> supersededMemoryIds) {}
}
