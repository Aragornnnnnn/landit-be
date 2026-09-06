// AI 서버 발음 분석 호출이 실패했을 때 던지는 예외다.

package com.landit.landitbe.feature.content.exception;

import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;

/**
 * AI 서버 발음 분석 호출이 실패했을 때 던지는 예외다.
 *
 * <p>공통 {@link ApiException}을 상속해 전역 핸들러·응답 형식은 그대로 두고, 예외 타입만으로 원인을 알 수 있게 도메인이 소유한다 (502
 * PRONUNCIATION_ANALYSIS_FAILED).
 *
 * <p>예시 — AI 서버가 20초 안에 응답하지 않은 경우(타임아웃), 네트워크가 끊긴 경우, base-url 설정이 빈 경우, AI가 해석 불가능한 오류 본문을 준 경우.
 * 유저 입력 문제가 아니라 호출 실패라 502로 응답한다.
 */
public class PronunciationAnalysisFailedException extends ApiException {

  /**
   * 실패 사유와 함께 예외를 생성한다. 사유는 원인 구분용이다 (예: 응답 대기 중단 / 통신 실패).
   *
   * @param message 실패 사유
   */
  public PronunciationAnalysisFailedException(String message) {
    super(ErrorCode.PRONUNCIATION_ANALYSIS_FAILED, message);
  }

  /** 오류 코드의 기본 메시지를 사용하는 예외를 생성한다. */
  public PronunciationAnalysisFailedException() {
    super(ErrorCode.PRONUNCIATION_ANALYSIS_FAILED);
  }
}
