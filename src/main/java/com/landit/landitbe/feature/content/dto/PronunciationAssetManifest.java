// TTS 사전 생성 배치가 S3에 올린 발음 평가 자산 매니페스트를 표현한다.

package com.landit.landitbe.feature.content.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.landit.landitbe.shared.domain.AccentLocale;
import java.util.List;

/**
 * TTS 사전 생성 배치(landit-iac)가 S3에 올린 발음 평가 자산 매니페스트를 표현한다.
 *
 * <p>어드민 임포트 API가 S3에서 이 형식의 JSON 파일을 내려받아 파싱한다. HTTP 요청 본문이 아니므로 검증은 서비스에서 코드로 수행한다.
 *
 * @param assets 임포트할 자산 목록
 */
public record PronunciationAssetManifest(List<Asset> assets) {

  /**
   * 임포트할 자산 1건을 표현한다.
   *
   * @param expressionId Writing 표현 ID
   * @param accentLocale 억양 locale
   * @param expressionAudioUrl 표현 TTS URL
   * @param sentenceAudioUrl 대표 예문 TTS URL
   * @param words 단어별 발음 기준 데이터 배열 (order, word, nativeWordAudioUrl, nativeDisplay,
   *     syllables, stressIndex)
   */
  public record Asset(
      Long expressionId,
      AccentLocale accentLocale,
      String expressionAudioUrl,
      String sentenceAudioUrl,
      JsonNode words) {}
}
