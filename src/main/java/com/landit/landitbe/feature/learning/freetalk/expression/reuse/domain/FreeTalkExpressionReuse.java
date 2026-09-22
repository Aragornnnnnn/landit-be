// 스몰톡에서 이전에 배운 표현을 실제로 쓴 기록을 저장 시점 그대로 남긴다.

package com.landit.landitbe.feature.learning.freetalk.expression.reuse.domain;

import com.landit.landitbe.shared.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;

/**
 * 한 발화에서 배운 표현 하나를 다시 쓴 기록이다.
 *
 * <p>요약의 표현 재사용 카드와 대화 보기의 밑줄에 쓴다. 지난 기록은 조회할 때마다 같아야 하므로 표현 원문·뜻과 출처를 저장 시점의 값으로 복사해 두고, 저장한 뒤에는
 * 어떤 값도 바꾸지 않는다. 한 발화에 같은 표현은 한 번만 기록한다.
 */
@Getter
@Entity
@Table(name = "free_talk_expression_reuse")
public class FreeTalkExpressionReuse extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id; // 예: 4101

  @Column(name = "user_profile_id", nullable = false, updatable = false)
  private Long userProfileId; // 예: 1207

  @Column(name = "free_talk_session_id", nullable = false, updatable = false)
  private Long freeTalkSessionId; // 예: 30 (표현을 다시 쓴 세션)

  @Column(name = "session_history_message_id", nullable = false, updatable = false)
  private Long sessionHistoryMessageId; // 예: 55020 (표현을 쓴 USER 메시지)

  @Column(name = "writing_expression_id", nullable = false, updatable = false)
  private Long writingExpressionId; // 예: 812 (다시 쓴 표현. 표현이 지워져도 기록은 남는다)

  @Column(name = "expression_text", nullable = false, length = 500, updatable = false)
  private String expressionText; // 예: "grab a coffee"

  @Column(
      name = "expression_meaning",
      nullable = false,
      columnDefinition = "text",
      updatable = false)
  private String expressionMeaning; // 예: "커피 한잔하다"

  @Enumerated(EnumType.STRING)
  @Column(name = "source_type", nullable = false, length = 20, updatable = false)
  private FreeTalkExpressionReuseSource sourceType; // 예: SCENARIO

  @Column(name = "source_title", length = 255, updatable = false)
  private String sourceTitle; // 예: "주말 계획". 출처의 제목을 찾지 못했으면 null

  @Column(name = "source_learned_on", nullable = false, updatable = false)
  private LocalDate sourceLearnedOn; // 예: 2026-09-10 (그 표현을 배운 날)

  // 발화 원문에서 찾은 조각 그대로다. 화면이 이 문자열로 밑줄 위치를 다시 찾는다.
  @Column(name = "matched_text", nullable = false, columnDefinition = "text", updatable = false)
  private String matchedText; // 예: "grab a coffee"

  // 예: "Let's grab a coffee after work."
  @Column(name = "quoted_sentence", nullable = false, columnDefinition = "text", updatable = false)
  private String quotedSentence;

  /** JPA에서 사용하는 기본 생성자다. */
  protected FreeTalkExpressionReuse() {}

  private FreeTalkExpressionReuse(
      Long userProfileId,
      Long freeTalkSessionId,
      Long sessionHistoryMessageId,
      Long writingExpressionId,
      String expressionText,
      String expressionMeaning,
      FreeTalkExpressionReuseSource sourceType,
      String sourceTitle,
      LocalDate sourceLearnedOn,
      String matchedText,
      String quotedSentence) {
    this.userProfileId = userProfileId;
    this.freeTalkSessionId = freeTalkSessionId;
    this.sessionHistoryMessageId = sessionHistoryMessageId;
    this.writingExpressionId = writingExpressionId;
    this.expressionText = expressionText;
    this.expressionMeaning = expressionMeaning;
    this.sourceType = sourceType;
    this.sourceTitle = sourceTitle;
    this.sourceLearnedOn = sourceLearnedOn;
    this.matchedText = matchedText;
    this.quotedSentence = quotedSentence;
  }

  /**
   * 배운 표현을 다시 쓴 기록을 만든다.
   *
   * @param userProfileId 사용자 프로필 ID
   * @param freeTalkSessionId 표현을 다시 쓴 프리톡 세션 ID
   * @param sessionHistoryMessageId 표현을 쓴 사용자 발화 ID
   * @param writingExpressionId 다시 쓴 표현 ID
   * @param expressionText 저장 시점의 표현 원문
   * @param expressionMeaning 저장 시점의 표현 뜻
   * @param sourceType 그 표현을 배운 곳
   * @param sourceTitle 저장 시점의 출처 제목. 찾지 못했으면 null
   * @param sourceLearnedOn 그 표현을 배운 날
   * @param matchedText 발화 원문에서 찾은 조각
   * @param quotedSentence 그 조각이 든 문장
   * @return 저장할 재사용 기록
   * @throws IllegalArgumentException 필수 값이 없거나 문구가 비었을 때
   */
  public static FreeTalkExpressionReuse of(
      long userProfileId,
      long freeTalkSessionId,
      long sessionHistoryMessageId,
      long writingExpressionId,
      String expressionText,
      String expressionMeaning,
      FreeTalkExpressionReuseSource sourceType,
      String sourceTitle,
      LocalDate sourceLearnedOn,
      String matchedText,
      String quotedSentence) {
    if (sourceType == null
        || sourceLearnedOn == null
        || blank(expressionText)
        || blank(expressionMeaning)
        || blank(matchedText)
        || blank(quotedSentence)) {
      throw new IllegalArgumentException("표현 재사용 기록의 출처와 문구는 필수입니다.");
    }
    return new FreeTalkExpressionReuse(
        userProfileId,
        freeTalkSessionId,
        sessionHistoryMessageId,
        writingExpressionId,
        expressionText,
        expressionMeaning,
        sourceType,
        blank(sourceTitle) ? null : sourceTitle,
        sourceLearnedOn,
        matchedText,
        quotedSentence);
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }
}
