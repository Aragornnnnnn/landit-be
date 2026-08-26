// AI 서버에 발음 분석을 요청하는 클라이언트를 정의한다.

package com.landit.landitbe.feature.content.client.ai;

/**
 * AI 서버에 발음 분석을 요청하는 클라이언트를 정의한다.
 *
 * <p>운영에서는 원격 AI 서버를 호출하고, 로컬·테스트에서는 고정 판정을 반환한다. 구현 선택은 {@code landit.ai.client-mode} 설정을 따른다.
 */
public interface AiPronunciationClient {

  /**
   * 유저 발화를 원어민 TTS와 대조해 단어별 발음·강세 판정을 받는다.
   *
   * @param request 발음 분석 요청
   * @return 단어별 판정 결과
   * @throws com.landit.landitbe.shared.exception.ApiException AI 서버 호출이 실패했을 때
   */
  AiPronunciationAnalysisResult analyze(AiPronunciationAnalysisRequest request);
}
