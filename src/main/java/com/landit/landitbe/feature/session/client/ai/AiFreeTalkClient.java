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
   * 프리톡의 마지막 AI 메시지를 생성한다.
   *
   * @param request 종료 사유와 누적 대화 정보
   * @return 생성된 마무리 메시지와 속마음
   * @throws ApiException AI 생성에 실패하거나 응답 형식이 올바르지 않을 때
   */
  AiFreeTalkClosingResult generateClosing(AiFreeTalkClosingRequest request);
}
