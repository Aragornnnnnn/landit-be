// AI 서버 발음 분석 요청을 표현한다.

package com.landit.landitbe.feature.content.client.ai;

import com.landit.landitbe.shared.domain.AccentLocale;
import java.util.List;

/**
 * AI 서버 발음 분석 요청을 표현한다.
 *
 * @param userAudio 유저 발화 녹음의 base64 문자열
 * @param userAudioFormat 녹음 파일 형식 (m4a·wav·mp3)
 * @param sentenceText 정답 문장 (강제 정렬 기준)
 * @param referenceAudioUrl 대조 기준이 되는 원어민 문장 TTS URL
 * @param accentLocale 판정 기준 억양
 * @param words 문장의 단어 목록 (order 오름차순)
 */
public record AiPronunciationAnalysisRequest( // ai 서버에 보내는 것
    String userAudio,
    String userAudioFormat,
    String sentenceText,
    String referenceAudioUrl,
    AccentLocale accentLocale,
    List<Word> words) {

  /**
   * 문장의 단어 1개를 표현한다.
   *
   * <p>단어 목록은 퀴즈용 배열이 아니라 발음 자산의 words_payload 기준이다 — 발음 정렬에서는 "late-night"이 late/night 2단어로 나뉘는 등
   * 토큰화가 다르다.
   *
   * @param order 문장 내 순번 (1부터)
   * @param word 단어 표면형
   * @param accentContrast 억양 대조 힌트. 억양 대조 단어에만 있고, 없으면 null (AI 서버는 null을 생략과 동일하게 처리)
   */
  public record Word(int order, String word, AccentContrast accentContrast) {}

  /**
   * 억양 대조 힌트를 표현한다. 미국·영국식 발음이 갈리는 단어에서 판정 기준을 명확히 하기 위해 기준 데이터에 미리 정의돼 있다.
   *
   * @param expected 이 억양에서 기대되는 발음 설명 (예: sounds like 「nuh·thing」)
   * @param other 다른 억양에서의 발음 설명
   * @param errorType 대조 유형 (PHONEME 또는 STRESS)
   */
  public record AccentContrast(String expected, String other, String errorType) {}

  /** base64 오디오가 로그·예외 메시지에 통째로 찍히지 않게 요약 표현만 반환한다. */
  @Override
  public String toString() {
    return "AiPronunciationAnalysisRequest[userAudio=<%d bytes base64>, userAudioFormat=%s,"
        + " sentenceText=%s, referenceAudioUrl=%s, accentLocale=%s, words=%s]"
            .formatted(
                userAudio == null ? 0 : userAudio.length(),
                userAudioFormat,
                sentenceText,
                referenceAudioUrl,
                accentLocale,
                words);
  }
}
