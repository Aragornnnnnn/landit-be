// 발음 기준 데이터 JSON 파일의 "틀"을 정의한다.

package com.landit.landitbe.feature.content.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.landit.landitbe.shared.domain.AccentLocale;
import java.util.List;

/**
 * 발음 기준 데이터 JSON 파일의 "틀"을 정의한다.
 *
 * <p>S3에서 파일을 내려받으면 자바한테는 그냥 긴 문자열이다. 이 record는 그 문자열이 어떤 모양의 데이터인지 자바에 알려주는 틀이고, 파서(ObjectMapper)가
 * 이 틀에 맞춰 문자열을 객체로 바꿔준다. 컨트롤러의 @RequestBody DTO와 같은 역할인데, JSON이 HTTP가 아니라 S3에서 온다는 점만 다르다.
 *
 * <p>대상 파일: AI 파이프라인이 만든 locale별 기준 데이터 (예: reference_EN_US.json, 981건 전량). 실제 파일 내용 예시:
 *
 * <pre>
 * [
 *   { "expressionId": 164,
 *     "accentLocale": "EN_US",
 *     "sentenceText": "I'm super tired today.",
 *     "words": [ { "order": 2, "word": "nothing", "syllables": ["nuh","thing"],
 *                  "stressIndex": 0, "pronunciationDisplay": "nuh·thing" } ] },
 *   ... (표현 수만큼 반복)
 * ]
 * </pre>
 *
 * <p>임포트 1단계(importReference)가 이 데이터로 자산 행을 만든다. 단어별 음성 URL(audioUrl)은 이 파일에 없고, 2단계(TTS 매니페스트
 * 임포트)에서 채워진다.
 *
 * @param entries 표현별 기준 데이터 목록 (파일 최상위 배열)
 */
public record PronunciationReferenceManifest(List<Entry> entries) {

  /**
   * 파일 배열의 항목 1개 = 표현 1개의 기준 데이터.
   *
   * @param expressionId 어느 표현의 데이터인지 (writing_expression.id)
   * @param accentLocale 어느 억양의 데이터인지 (EN_US / EN_GB / EN_AU)
   * @param sentenceText 이 기준 데이터를 만들 때 사용한 대표 예문. 임포트할 때 DB의 현재 문장과 비교해서, 문장이 바뀐 뒤 만든 낡은 데이터면 걸러내는
   *     데 쓴다 (V61처럼 문장이 수정되는 경우 대비)
   * @param words 단어별 발음 기준 데이터 배열. 세부 구조가 자주 바뀔 수 있어 통째로(JsonNode) 받아 저장한다. 항목: order(순번),
   *     word(단어), syllables(음절 분해), stressIndex(강세 위치, 무강세 기능어는 -1), pronunciationDisplay(원어민 발음
   *     표기), accentContrast(억양 대조 힌트, 대조 단어에만 있음)
   */
  public record Entry(
      Long expressionId, AccentLocale accentLocale, String sentenceText, JsonNode words) {}
}
