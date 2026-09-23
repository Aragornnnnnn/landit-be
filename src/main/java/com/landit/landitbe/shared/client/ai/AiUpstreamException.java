// AI HTTP 오류의 상태 코드만 보존하고 응답 본문은 저장하지 않는다.

package com.landit.landitbe.shared.client.ai;

import java.io.IOException;

/** 외부 AI 호출의 HTTP 실패를 민감정보 없이 구분한다. */
public final class AiUpstreamException extends IOException {
  private final int statusCode;

  /**
   * AI 서버의 실패 상태 코드를 보존한다.
   *
   * @param statusCode AI 서버 HTTP 상태 코드
   */
  public AiUpstreamException(int statusCode) {
    super("AI upstream HTTP failure");
    this.statusCode = statusCode;
  }

  /**
   * 외부 AI 서버가 반환한 HTTP 상태를 조회한다.
   *
   * @return 원본 HTTP 상태 코드
   */
  public int statusCode() {
    return statusCode;
  }
}
