// 발음 평가 자산(기준 데이터·TTS)이 없어 평가를 열 수 없을 때 던지는 예외다.

package com.landit.landitbe.feature.content.exception;

import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;

/**
 * 발음 평가 자산(기준 데이터·TTS)이 없어 평가를 열 수 없을 때 던지는 예외다.
 *
 * <p>공통 {@link ApiException}을 상속해 전역 핸들러·응답 형식은 그대로 두고, 예외 타입만으로 원인을 알 수 있게 도메인이 소유한다 (404
 * PRONUNCIATION_DATA_NOT_FOUND).
 *
 * <p>예시 — 임포트를 아직 안 한 표현의 평가를 호출한 경우, 기준 데이터만 있고 TTS가 안 붙은 반쪽 자산인 경우, 자산 행은 있는데 words가 빈 경우. 앱은
 * 404를 받으면 그 표현의 발음 파트를 숨긴다 (자산을 단계적으로 채우는 동안의 의도된 동작).
 */
public class PronunciationDataNotFoundException extends ApiException {

  /** 오류 코드의 기본 메시지를 사용하는 예외를 생성한다. */
  public PronunciationDataNotFoundException() {
    super(ErrorCode.PRONUNCIATION_DATA_NOT_FOUND);
  }
}
