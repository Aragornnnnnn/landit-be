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
  // ("There is nothing like"만 읽은 음성)
  @Column(name = "expression_audio_url", nullable = false, length = 500)
  private String expressionAudioUrl;

  // 대표 예문 전체를 읽어주는 원어민 TTS mp3의 CDN URL.
  // 예: "There's nothing like hiking to clear my head." 전체를 읽은 음성.
  // 앱의 "원어민 발음 듣기" 재생용이자, AI 서버가 유저 음성과 대조하는 판정 기준 음성.
  @Column(name = "sentence_audio_url", nullable = false, length = 500)
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
   * 발음 평가 자산을 생성한다.
   *
   * @param writingExpressionId Writing 표현 ID
   * @param accentLocale 억양 locale
   * @param expressionAudioUrl 표현 TTS URL
   * @param sentenceAudioUrl 대표 예문 TTS URL
   * @param words 단어별 발음 기준 데이터
   */
  public ExpressionPronunciationAsset(
      Long writingExpressionId,
      AccentLocale accentLocale,
      String expressionAudioUrl,
      String sentenceAudioUrl,
      JsonNode words) {
    this.writingExpressionId = writingExpressionId;
    this.accentLocale = accentLocale;
    this.expressionAudioUrl = expressionAudioUrl;
    this.sentenceAudioUrl = sentenceAudioUrl;
    this.words = words;
  }

  /**
   * 자산 내용을 새 값으로 교체한다. 음성 재생성이나 기준 데이터 갱신 시 사용한다.
   *
   * @param expressionAudioUrl 표현 TTS URL
   * @param sentenceAudioUrl 대표 예문 TTS URL
   * @param words 단어별 발음 기준 데이터
   */
  public void replaceContents(String expressionAudioUrl, String sentenceAudioUrl, JsonNode words) {
    this.expressionAudioUrl = expressionAudioUrl;
    this.sentenceAudioUrl = sentenceAudioUrl;
    this.words = words;
  }
}
