// 직전 세션에서 교정받은 실수 패턴이 이번 턴에 등장한 사용례 하나를 판정 시점 그대로 남긴다.

package com.landit.landitbe.feature.learning.freetalk.feedback.domain;

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

/**
 * 지켜보던 실수 패턴이 사용자 발화 한 턴에 등장한 사용례다.
 *
 * <p>스몰톡 요약의 실수 기억 카드가 "오늘은 맞게 썼다 / 오늘도 틀렸다"를 가리고 강조할 문장을 고르는 재료다. 지난 기록은 조회할 때마다 같아야 하므로 판정 시점의 값을
 * 그대로 남기고 저장한 뒤에는 바꾸지 않는다. 한 턴에 여러 건일 수 있다.
 */
@Getter
@Entity
@Table(name = "free_talk_pattern_usage")
public class FreeTalkPatternUsage extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id; // 예: 7301

  @Column(name = "session_history_message_id", nullable = false, updatable = false)
  private Long sessionHistoryMessageId; // 예: 55020 (그 패턴이 등장한 USER 메시지)

  @Column(name = "session_history_id", nullable = false, updatable = false)
  private Long sessionHistoryId; // 예: 3100 (그 메시지가 속한 대화 기록)

  @Enumerated(EnumType.STRING)
  @Column(name = "pattern", nullable = false, length = 40, updatable = false)
  private FreeTalkMistakePattern pattern; // 예: TENSE

  // 예: "I went to the gym with my friend." (발화 원문에서 그 패턴이 쓰인 한 문장)
  @Column(name = "sentence", nullable = false, columnDefinition = "text", updatable = false)
  private String sentence;

  // 예: "went" (문장 안에 대소문자까지 그대로 정확히 한 번 나오는 구절. 화면이 이 문자열로 강조 위치를 찾는다)
  @Column(name = "span", nullable = false, columnDefinition = "text", updatable = false)
  private String span;

  @Column(name = "correct", nullable = false, updatable = false)
  private boolean correct; // 예: true (맞게 썼으면 true, 틀리게 썼으면 false)

  /** JPA에서 사용하는 기본 생성자다. */
  protected FreeTalkPatternUsage() {}

  private FreeTalkPatternUsage(
      Long sessionHistoryMessageId,
      Long sessionHistoryId,
      FreeTalkMistakePattern pattern,
      String sentence,
      String span,
      boolean correct) {
    this.sessionHistoryMessageId = sessionHistoryMessageId;
    this.sessionHistoryId = sessionHistoryId;
    this.pattern = pattern;
    this.sentence = sentence;
    this.span = span;
    this.correct = correct;
  }

  /**
   * 사용례 하나를 만든다.
   *
   * @param sessionHistoryMessageId 그 패턴이 등장한 사용자 발화 ID
   * @param sessionHistoryId 그 발화가 속한 대화 기록 ID
   * @param pattern 지켜보던 실수 패턴
   * @param sentence 발화 원문에서 그 패턴이 쓰인 한 문장
   * @param span 그 문장 안에서 강조할 구절
   * @param correct 맞게 썼는지 여부
   * @return 저장할 사용례
   * @throws IllegalArgumentException 패턴이 없거나 문장·구절이 비었을 때
   */
  public static FreeTalkPatternUsage of(
      long sessionHistoryMessageId,
      long sessionHistoryId,
      FreeTalkMistakePattern pattern,
      String sentence,
      String span,
      boolean correct) {
    if (pattern == null || blank(sentence) || blank(span)) {
      throw new IllegalArgumentException("실수 패턴 사용례의 패턴·문장·구절은 필수입니다.");
    }
    return new FreeTalkPatternUsage(
        sessionHistoryMessageId, sessionHistoryId, pattern, sentence, span, correct);
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }
}
