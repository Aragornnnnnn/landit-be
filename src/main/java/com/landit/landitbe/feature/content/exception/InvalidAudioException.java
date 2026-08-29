// 발음 평가 녹음 파일이 형식·크기 제한에 맞지 않을 때 던지는 예외다.

package com.landit.landitbe.feature.content.exception;

import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;

/**
 * 발음 평가 녹음 파일이 형식·크기 제한에 맞지 않을 때 던지는 예외다.
 *
 * <p>공통 {@link ApiException}을 상속해 전역 핸들러·응답 형식은 그대로 두고, 예외 타입만으로 원인을 알 수 있게 도메인이 소유한다 (400
 * INVALID_AUDIO).
 *
 * <p>예시 — 유저가 텍스트 파일(notes.txt)을 올렸거나, 12MB짜리 녹음을 올렸거나, AI 서버의 길이 검증(30초 초과)에 걸린 경우. 전부 유저 입력 문제라
 * 400으로 응답한다.
 */
public class InvalidAudioException extends ApiException {

  /**
   * 사용자에게 보여줄 사유와 함께 예외를 생성한다.
   *
   * @param message 실패 사유. 예: "오디오 파일이 10MB를 초과했습니다."
   */
  public InvalidAudioException(String message) {
    super(ErrorCode.INVALID_AUDIO, message);
  }

  /** 오류 코드의 기본 메시지를 사용하는 예외를 생성한다. */
  public InvalidAudioException() {
    super(ErrorCode.INVALID_AUDIO);
  }
}
