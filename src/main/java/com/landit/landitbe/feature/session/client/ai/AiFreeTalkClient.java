// 프리톡에 필요한 AI 서버 호출을 추상화한다.

package com.landit.landitbe.feature.session.client.ai;

/** 프리톡에 필요한 AI 서버 호출을 추상화한다. */
public interface AiFreeTalkClient {

  /**
   * 프리톡 첫 AI 메시지를 생성한다.
   *
   * @param request 첫 메시지 생성에 필요한 세션과 주제 정보
   * @return 생성된 첫 AI 메시지
   */
  AiFreeTalkOpeningResult generateOpening(AiFreeTalkOpeningRequest request);

  /**
   * 사용자 발화에 대한 프리톡 응답을 생성한다.
   *
   * @param request 사용자 발화와 누적 대화 정보
   * @return 종료 의사 또는 생성된 AI 응답
   */
  AiFreeTalkTurnResult generateTurn(AiFreeTalkTurnRequest request);

  /**
   * 프리톡의 마지막 AI 메시지를 생성한다.
   *
   * @param request 종료 사유와 누적 대화 정보
   * @return 생성된 마무리 메시지와 속마음
   */
  AiFreeTalkClosingResult generateClosing(AiFreeTalkClosingRequest request);
}
