// TTS 매니페스트 JSON 파일의 "틀"을 정의한다.

package com.landit.landitbe.feature.content.dto;

import com.landit.landitbe.shared.domain.AccentLocale;
import java.util.List;

/**
 * TTS 매니페스트 JSON 파일의 "틀"을 정의한다.
 *
 * <p>S3에서 내려받은 JSON 문자열을 자바 객체로 바꾸기 위한 틀이다 (자세한 배경은 {@link PronunciationReferenceManifest} 주석 참고 —
 * 같은 원리, 대상 파일만 다르다).
 *
 * <p>대상 파일: TTS 생성 배치(landit-iac)가 mp3들을 S3에 올린 뒤 함께 올리는 "뭘 만들었는지 목록". 실제 파일 내용 예시:
 *
 * <pre>
 * { "assets": [
 *     { "expressionId": 164,
 *       "accentLocale": "EN_US",
 *       "expressionAudioUrl": "https://cdn.../expression-{해시}.mp3",
 *       "sentenceAudioUrl": "https://cdn.../sentence-{해시}.mp3",
 *       "words": [ { "order": 2, "audioUrl": "https://cdn.../word-{해시}.mp3" } ] },
 *     ... (표현×억양 수만큼 반복)
 * ] }
 * </pre>
 *
 * <p>임포트 2단계(importTts)가 이 URL들을 기존 자산 행에 붙인다 — 문장·표현 URL 컬럼을 채우고, 단어별 audioUrl은 기준 데이터 words에
 * order를 열쇠로 짝지어(조인) 넣는다. 그래서 기준 데이터(1단계)가 먼저 임포트돼 있어야 한다.
 *
 * @param assets 표현×억양별 음성 URL 목록
 */
public record PronunciationTtsManifest(List<Asset> assets) {

  /**
   * 목록의 항목 1개 = 표현 1개 × 억양 1개의 음성 URL 묶음.
   *
   * @param expressionId 어느 표현의 음성인지 (writing_expression.id)
   * @param accentLocale 어느 억양의 음성인지 (EN_US / EN_GB / EN_AU)
   * @param expressionAudioUrl 타겟 표현만 읽은 mp3 URL (예: "There is nothing like")
   * @param sentenceAudioUrl 대표 예문 전체를 읽은 mp3 URL — 앱의 "원어민 발음 듣기" 재생용이자 AI 판정의 대조 기준 음성
   * @param words 단어별 mp3 URL 목록 — 오류 단어 카드의 "원어민" 버튼 재생용
   */
  public record Asset(
      Long expressionId,
      AccentLocale accentLocale,
      String expressionAudioUrl,
      String sentenceAudioUrl,
      List<WordAudio> words) {

    /**
     * 단어 1개의 음성 URL.
     *
     * @param order 문장 내 순번 — 기준 데이터 words의 order와 짝짓는 열쇠
     * @param audioUrl 그 단어만 읽은 mp3 URL
     */
    public record WordAudio(int order, String audioUrl) {}
  }
}
