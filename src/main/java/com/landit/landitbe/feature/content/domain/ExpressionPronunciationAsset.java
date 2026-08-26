// 표현별 발음 평가 자산을 억양 단위로 저장한다.

package com.landit.landitbe.feature.content.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.landit.landitbe.shared.domain.AccentLocale;
import com.landit.landitbe.shared.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 표현별 발음 평가 자산을 억양 단위로 저장한다.
 *
 * <p>사전 생성된 원어민 TTS URL(표현·대표 예문)과 단어별 발음 기준 데이터(단어 TTS URL, respelling, 음절, 강세 위치)를 담는다. 발음 평가와
 * 원어민 발음 듣기가 이 자산을 조회해 사용한다.
 */
@Entity
@Getter
@Table(name = "expression_pronunciation_asset")
public class ExpressionPronunciationAsset extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // 이 자산이 속한 Writing 표현의 ID. 예: 101 (= "There is nothing like" 표현)
  @Column(name = "writing_expression_id", nullable = false)
  private Long writingExpressionId;

  // 이 자산이 어느 억양의 음성·발음 데이터인지. 예: EN_GB (영국 영어)
  // 같은 표현이라도 억양마다 행이 따로 있다 (표현 1개 = 최대 3행).
  @Enumerated(EnumType.STRING)
  @Column(name = "accent_locale", nullable = false, length = 35)
  private AccentLocale accentLocale;

  // 타겟 표현만 읽어주는 원어민 TTS mp3의 CDN URL.
  // 예: https://cdn.landit.com/content/expression-pronunciation-audio/101/EN_US/{fingerprint}.mp3
  // ("There is nothing like"만 읽은 음성). TTS 임포트 전에는 null (TTS 미완성 상태).
  @Column(name = "expression_audio_url", length = 500)
  private String expressionAudioUrl;

  // 대표 예문 전체를 읽어주는 원어민 TTS mp3의 CDN URL.
  // 예: "There's nothing like hiking to clear my head." 전체를 읽은 음성.
  // 앱의 "원어민 발음 듣기" 재생용이자, AI 서버가 유저 음성과 대조하는 판정 기준 음성.
  // TTS 임포트 전에는 null이며, 이 상태의 자산으로는 발음 평가를 열지 않는다.
  @Column(name = "sentence_audio_url", length = 500)
  private String sentenceAudioUrl;

  // 단어별 발음 기준 데이터 배열. order 오름차순으로 대표 예문의 단어와 1:1 대응한다.
  // 각 항목: order(문장 내 순번), word(단어), nativeWordAudioUrl(단어만 읽은 TTS URL),
  //   nativeDisplay(원어민 발음 respelling), syllables(음절 분해), stressIndex(강세 음절 위치, 0부터)
  // 예: [{"order": 2, "word": "nothing",
  //       "nativeWordAudioUrl": "https://cdn.landit.com/.../nothing.mp3",
  //       "nativeDisplay": "nuh·thing", "syllables": ["nuh", "thing"], "stressIndex": 0}, ...]
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private JsonNode words;

  /** JPA에서 사용하는 기본 생성자다. */
  protected ExpressionPronunciationAsset() {}

  /**
   * 기준 데이터만으로 발음 평가 자산을 생성한다. 음성 URL은 TTS 임포트에서 채워진다.
   *
   * @param writingExpressionId Writing 표현 ID
   * @param accentLocale 억양 locale
   * @param words 단어별 발음 기준 데이터
   */
  public ExpressionPronunciationAsset(
      Long writingExpressionId, AccentLocale accentLocale, JsonNode words) {
    this.writingExpressionId = writingExpressionId;
    this.accentLocale = accentLocale;
    this.words = words;
  }

  /**
   * 기준 데이터를 새 값으로 교체한다. 기준 데이터 재임포트 시 사용한다.
   *
   * <p>단어별 audioUrl은 words 안에 저장되므로, 교체 후에는 TTS 임포트를 다시 실행해 audioUrl을 재조인해야 한다.
   *
   * @param words 단어별 발음 기준 데이터
   */
  public void replaceWords(JsonNode words) {
    this.words = words;
  }

  /**
   * TTS 임포트 결과를 붙인다 — 문장·표현 음성 URL을 채우고, audioUrl이 조인된 words로 교체한다.
   *
   * @param expressionAudioUrl 표현 TTS URL
   * @param sentenceAudioUrl 대표 예문 TTS URL
   * @param wordsWithAudio 단어별 audioUrl이 채워진 발음 기준 데이터
   */
  public void attachTts(
      String expressionAudioUrl, String sentenceAudioUrl, JsonNode wordsWithAudio) {
    this.expressionAudioUrl = expressionAudioUrl;
    this.sentenceAudioUrl = sentenceAudioUrl;
    this.words = wordsWithAudio;
  }

  /**
   * TTS까지 완성된 자산인지 반환한다. 발음 평가는 완성된 자산에서만 연다.
   *
   * @return 대표 예문 TTS URL이 있으면 true
   */
  public boolean hasTts() {
    return sentenceAudioUrl != null && !sentenceAudioUrl.isBlank();
  }
}
