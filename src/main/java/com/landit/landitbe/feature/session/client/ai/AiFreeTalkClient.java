// 프리톡에 필요한 AI 서버 호출을 추상화한다.

package com.landit.landitbe.feature.session.client.ai;

import com.landit.landitbe.shared.exception.ApiException;

/** 프리톡에 필요한 AI 서버 호출을 추상화한다. */
public interface AiFreeTalkClient {

  /**
   * 프리톡 첫 AI 메시지를 생성한다.
   *
   * @param request 첫 메시지 생성에 필요한 세션과 주제 정보
   * @return 생성된 첫 AI 메시지
   * @throws ApiException AI 생성에 실패하거나 응답 형식이 올바르지 않을 때
   */
  AiFreeTalkOpeningResult generateOpening(AiFreeTalkOpeningRequest request);

  /**
   * 사용자 발화에 대한 프리톡 응답을 생성한다.
   *
   * @param request 사용자 발화와 누적 대화 정보
   * @return 종료 의사 또는 생성된 AI 응답
   * @throws ApiException AI 생성에 실패하거나 응답 형식이 올바르지 않을 때
   */
  AiFreeTalkTurnResult generateTurn(AiFreeTalkTurnRequest request);

  /**
   * 사용자 발화에 대한 상대 역할의 속마음을 생성한다.
   *
   * @param request 사용자 발화와 누적 대화 정보
   * @return 생성된 속마음과 속마음 유형
   * @throws ApiException AI 생성에 실패하거나 응답 형식이 올바르지 않을 때
   */
  AiFreeTalkInnerThoughtResult generateInnerThought(AiFreeTalkInnerThoughtRequest request);

  /**
   * 프리톡의 마지막 AI 메시지를 생성한다.
   *
   * @param request 종료 사유와 누적 대화 정보
   * @return 생성된 마무리 메시지
   * @throws ApiException AI 생성에 실패하거나 응답 형식이 올바르지 않을 때
   */
  AiFreeTalkClosingResult generateClosing(AiFreeTalkClosingRequest request);

  /**
   * 완료된 프리톡에 맞는 학습 표현을 추천한다.
   *
   * @param request 완료 대화와 기존 표현 후보
   * @return 추천된 표현 목록
   * @throws ApiException AI 생성에 실패하거나 응답 형식이 올바르지 않을 때
   */
  AiFreeTalkExpressionRecommendationsResult recommendExpressions(
      AiFreeTalkExpressionRecommendationsRequest request);

  /**
   * 완료된 프리톡 대화에서 학습 가치가 있는 사용자 발화를 추출하고 임베딩한다.
   *
   * @param request 완료 대화와 언어 정보
   * @return 추출된 핵심 발화와 임베딩 목록
   * @throws ApiException AI 생성에 실패하거나 응답 형식이 올바르지 않을 때
   */
  AiConversationEmbeddingsResult extractConversationEmbeddings(
      AiConversationEmbeddingsRequest request);

  /**
   * 완료된 프리톡 대화에서 장기기억 후보를 추출한다.
   *
   * @param request 세션과 대화 히스토리
   * @return 검증된 장기기억 후보 목록
   * @throws ApiException AI 생성에 실패하거나 응답 형식이 올바르지 않을 때
   */
  AiMemoryCandidatesResult extractMemoryCandidates(AiMemoryCandidatesRequest request);

  /**
   * 장기기억 후보를 기존 기억과 비교해 상태를 판정한다.
   *
   * @param request 후보와 후보별 비교 대상 기억
   * @return 후보별 ADD, SUPERSEDE 또는 IGNORE 판정
   * @throws ApiException AI 생성에 실패하거나 응답 형식이 올바르지 않을 때
   */
  AiMemoryResolutionResult resolveMemory(AiMemoryResolutionRequest request);
}
