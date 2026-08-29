// AI 서버의 발음 판정 응답이 요청 단어와 맞지 않아 신뢰할 수 없을 때 던지는 예외다.

package com.landit.landitbe.feature.content.exception;

import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;

/**
 * AI 서버의 발음 판정 응답이 요청 단어와 맞지 않아 신뢰할 수 없을 때 던지는 예외다.
 *
 * <p>개수 불일치, order 중복·미존재, 단어 텍스트 불일치, 판정 상태 누락이 여기에 해당한다. 공통 {@link ApiException}을 상속해 전역 핸들러·응답
 * 형식은 그대로 두고, 예외 타입만으로 원인을 알 수 있게 도메인이 소유한다 (502 AI_RESPONSE_INVALID).
 *
 * <p>예시 — 대표 예문이 "I don't like it"이면 BE-> AI 요청은 항상 4단어다: [1: I] [2: don't] [3: like] [4: it]. 그런데 AI가
 * "I like it" 3단어짜리 답안지를 돌려주거나, order를 [1, 1, 2, 3]처럼 중복해 돌려주면 그대로 병합할 수 없어 이 예외로 거부한다.
 *
 * <p>유저가 don't를 빼먹고 읽은 것은 이 예외가 아니다 — 그래도 AI는 4단어 답안지를 돌려주고, 빠진 단어는 판정 결과(점수)로 표현된다. 이 예외는 답안지가 문제지
 * 형식과 안 맞는 경우만 다룬다.
 */
public class AiPronunciationResponseInvalidException extends ApiException {

  /** 오류 코드의 기본 메시지를 사용하는 예외를 생성한다. */
  public AiPronunciationResponseInvalidException() {
    super(ErrorCode.AI_RESPONSE_INVALID);
  }
}
