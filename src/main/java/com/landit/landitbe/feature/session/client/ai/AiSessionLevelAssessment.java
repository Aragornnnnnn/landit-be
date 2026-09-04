// AI가 반환한 질문별 텍스트 수준 평가 계약을 표현한다.

package com.landit.landitbe.feature.session.client.ai;

import java.util.List;

/** AI가 근거와 함께 반환하는 세션 수준 평가다. */
public record AiSessionLevelAssessment(Core core, Details details) {

  /** 반드시 검증할 질문별 평가 목록이다. */
  public record Core(List<Message> messages) {}

  /** 한 사용자 메시지의 과업 수행과 영역 평가다. */
  public record Message(Long messageId, TaskPerformance taskPerformance, Domains domains) {}

  /** 텍스트 회화를 구성하는 다섯 평가 영역이다. */
  public record Domains(
      Domain situationPerformance,
      Domain grammar,
      Domain vocabulary,
      Domain discourse,
      Domain interactionPragmatics) {}

  /** 한 영역의 관찰 수준과 원문 근거다. */
  public record Domain(Integer level, EvidenceStatus evidenceStatus, String evidenceExcerpt) {}

  /** 형식 오류가 나도 Core에 영향을 주지 않는 선택 설명이다. */
  public record Details(String strength, String improvement) {}

  /** 질문에서 요구한 과업을 수행한 정도다. */
  public enum TaskPerformance {
    FAILED,
    PARTIAL,
    ACHIEVED
  }

  /** 영역 수준을 판단할 텍스트 근거의 상태다. */
  public enum EvidenceStatus {
    OBSERVED,
    NOT_OBSERVED,
    INSUFFICIENT_EVIDENCE
  }
}
