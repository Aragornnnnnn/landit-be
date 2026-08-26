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
   * @param order 문장 내 순번 (1부터)
   * @param word 단어 표면형
   */
  public record Word(int order, String word) {}

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
