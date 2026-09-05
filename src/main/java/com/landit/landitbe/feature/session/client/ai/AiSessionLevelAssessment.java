// AI가 반환한 질문별 텍스트 수준 평가 계약을 표현한다.

package com.landit.landitbe.feature.session.client.ai;

import java.util.List;

/**
 * AI가 근거와 함께 반환하는 세션 수준 평가다.
 *
 * @param core 수준 계산에 사용하는 필수 평가 원본
 * @param details 사용자에게 보여줄 선택 설명
 */
public record AiSessionLevelAssessment(Core core, Details details) {

  /**
   * 반드시 검증할 질문별 평가 목록이다.
   *
   * @param messages 사용자 메시지별 평가
   */
  public record Core(List<Message> messages) {}

  /**
   * 한 사용자 메시지의 과업 수행과 영역 평가다.
   *
   * @param messageId 평가 대상 사용자 메시지 ID
   * @param taskPerformance 질문에서 요구한 과업 수행 정도
   * @param domains 다섯 영역의 관찰 결과
   */
  public record Message(Long messageId, TaskPerformance taskPerformance, Domains domains) {}

  /**
   * 텍스트 회화를 구성하는 다섯 평가 영역이다.
   *
   * @param situationPerformance 상황 수행 평가
   * @param grammar 문법 평가
   * @param vocabulary 어휘 평가
   * @param discourse 대화 구성 평가
   * @param interactionPragmatics 상호작용·화용 평가
   */
  public record Domains(
      Domain situationPerformance,
      Domain grammar,
      Domain vocabulary,
      Domain discourse,
      Domain interactionPragmatics) {}

  /**
   * 한 영역의 관찰 수준과 원문 근거다.
   *
   * @param level 관찰한 Level 1~5. 미관찰이면 {@code null}
   * @param evidenceStatus 근거 관찰 상태
   * @param evidenceExcerpt 사용자 발화에서 인용한 근거
   */
  public record Domain(Integer level, EvidenceStatus evidenceStatus, String evidenceExcerpt) {}

  /**
   * 형식 오류가 나도 Core에 영향을 주지 않는 선택 설명이다.
   *
   * @param strength 사용자가 잘한 점
   * @param improvement 개선할 점
   */
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
