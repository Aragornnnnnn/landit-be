// 오늘의 스몰톡 요약 조회 응답을 표현한다.

package com.landit.landitbe.feature.learning.freetalk.summary.dto;

import com.landit.landitbe.feature.learning.freetalk.expression.reuse.dto.FreeTalkExpressionReuseSummary;
import com.landit.landitbe.feature.learning.freetalk.feedback.domain.FreeTalkMistakePattern;
import com.landit.landitbe.feature.learning.freetalk.followup.domain.FreeTalkFollowUpTriggerType;
import com.landit.landitbe.feature.learning.freetalk.followup.dto.FreeTalkFollowUpSummary;
import com.landit.landitbe.feature.learning.freetalk.summary.domain.FreeTalkHeadlinePose;
import com.landit.landitbe.feature.learning.freetalk.summary.domain.FreeTalkSessionSummary;
import java.time.LocalDate;
import java.util.List;

/**
 * 완료된 스몰톡의 종료 후 요약이다.
 *
 * <p>총평(헤드라인·비교·실수 기억 카드·교정 개수)은 이 세션의 턴 교정이 모두 끝난 뒤 한 번 계산해 저장한 값이고, 계산 전이면 {@code pending}이
 * true이며 그 값들은 null이다. 표현 재사용과 후속 질문은 종료 후 비동기 작업의 결과라 각자의 {@code pending}을 가진다.
 *
 * @param sessionId 프리톡 학습 세션 ID
 * @param title 스몰톡 제목. 없으면 null
 * @param pending 총평을 아직 계산하지 않았으면 true
 * @param firstSession 첫 스몰톡 여부. 총평 계산 전이면 null
 * @param headline 래디 말풍선. 총평 계산 전이면 null
 * @param comparison 직전 스몰톡과의 비교. 총평 계산 전이면 null
 * @param growth 실수 기억 카드. 해당 없거나 총평 계산 전이면 null
 * @param reusedExpressions 다시 쓴 배운 표현
 * @param followUp 다음 스몰톡의 후속 질문
 * @param correctionCount 교정이 있는 사용자 발화 수. 총평 계산 전이면 null
 */
public record FreeTalkSessionSummaryResponse(
    Long sessionId,
    String title,
    boolean pending,
    Boolean firstSession,
    Headline headline,
    Comparison comparison,
    Growth growth,
    ReusedExpressions reusedExpressions,
    FollowUp followUp,
    Integer correctionCount) {

  /**
   * 총평 계산 전의 응답을 만든다.
   *
   * @param sessionId 프리톡 학습 세션 ID
   * @param title 스몰톡 제목
   * @param reusedExpressions 다시 쓴 배운 표현
   * @param followUp 후속 질문
   * @return 총평 자리가 모두 null인 응답
   */
  public static FreeTalkSessionSummaryResponse pending(
      long sessionId,
      String title,
      FreeTalkExpressionReuseSummary reusedExpressions,
      FreeTalkFollowUpSummary followUp) {
    return new FreeTalkSessionSummaryResponse(
        sessionId,
        title,
        true,
        null,
        null,
        null,
        null,
        ReusedExpressions.of(reusedExpressions),
        FollowUp.of(followUp),
        null);
  }

  /**
   * 저장된 총평으로 응답을 만든다.
   *
   * @param sessionId 프리톡 학습 세션 ID
   * @param title 스몰톡 제목
   * @param summary 저장된 총평
   * @param reusedExpressions 다시 쓴 배운 표현
   * @param followUp 후속 질문
   * @return 총평이 채워진 응답
   */
  public static FreeTalkSessionSummaryResponse of(
      long sessionId,
      String title,
      FreeTalkSessionSummary summary,
      FreeTalkExpressionReuseSummary reusedExpressions,
      FreeTalkFollowUpSummary followUp) {
    return new FreeTalkSessionSummaryResponse(
        sessionId,
        title,
        false,
        summary.isFirstSession(),
        new Headline(
            summary.getHeadlineText(), summary.getHeadlineSubline(), summary.getHeadlinePose()),
        new Comparison(
            summary.getPreviousLearningSessionId(),
            summary.getPreviousDate(),
            new Metrics(
                summary.getCurrentSpeakingMs(),
                summary.getCurrentTurnCount(),
                summary.getCurrentMaxWordsInTurn()),
            new Metrics(
                summary.getPreviousSpeakingMs(),
                summary.getPreviousTurnCount(),
                summary.getPreviousMaxWordsInTurn())),
        summary.getGrowthPattern() == null
            ? null
            : new Growth(
                summary.getGrowthPattern(),
                summary.getGrowthPattern().koreanLabel(),
                summary.getGrowthSucceeded(),
                summary.getGrowthPreviousDate(),
                summary.getGrowthPreviousSentence(),
                summary.getGrowthPreviousWrongSpan(),
                summary.getGrowthCurrentSentence(),
                summary.getGrowthCurrentSpan()),
        ReusedExpressions.of(reusedExpressions),
        FollowUp.of(followUp),
        summary.getCorrectionCount());
  }

  /**
   * 래디 말풍선이다.
   *
   * @param text 첫 문장(사실)
   * @param subline 둘째 문장(의미 한 마디)
   * @param pose 래디 포즈
   */
  public record Headline(String text, String subline, FreeTalkHeadlinePose pose) {}

  /**
   * 직전 스몰톡과의 비교다.
   *
   * @param previousSessionId 직전 완료 스몰톡의 학습 세션 ID. 첫 스몰톡이면 null
   * @param previousDate 직전 스몰톡 날짜. 첫 스몰톡이면 null
   * @param current 이번 스몰톡 지표
   * @param previous 직전 스몰톡 지표. 첫 스몰톡이면 모두 0
   */
  public record Comparison(
      Long previousSessionId, LocalDate previousDate, Metrics current, Metrics previous) {}

  /**
   * 한 스몰톡의 지표다.
   *
   * @param speakingMs 말한 시간(ms)
   * @param turnCount 주고받은 말(사용자 발화 수)
   * @param maxWordsInTurn 가장 길게 말한 턴의 단어 수
   */
  public record Metrics(long speakingMs, int turnCount, int maxWordsInTurn) {}

  /**
   * 실수 기억 카드다.
   *
   * @param pattern 실수 패턴 코드. 화면에 노출하지 않는 참고 값
   * @param patternLabel 패턴의 한국어 이름
   * @param succeeded 오늘은 맞게 썼으면 true, 오늘도 틀렸으면 false
   * @param previousDate 직전 스몰톡 날짜
   * @param previousSentence 직전 스몰톡에서 틀렸던 문장
   * @param previousWrongSpan 직전 문장에서 취소선을 그을 구절. 특정하지 못했으면 null
   * @param currentSentence 이번 스몰톡에서 그 패턴이 나온 문장
   * @param currentSpan 이번 문장에서 강조할 구절. 특정하지 못했으면 null
   */
  public record Growth(
      FreeTalkMistakePattern pattern,
      String patternLabel,
      boolean succeeded,
      LocalDate previousDate,
      String previousSentence,
      String previousWrongSpan,
      String currentSentence,
      String currentSpan) {}

  /**
   * 다시 쓴 배운 표현이다.
   *
   * @param pending 종료 후 표현 작업이 끝나지 않았으면 true
   * @param items 표현마다 하나. 없으면 빈 목록
   */
  public record ReusedExpressions(boolean pending, List<Item> items) {

    private static ReusedExpressions of(FreeTalkExpressionReuseSummary summary) {
      return new ReusedExpressions(
          summary.pending(),
          summary.items().stream()
              .map(
                  item ->
                      new Item(
                          item.expressionId(),
                          item.text(),
                          item.meaning(),
                          item.sourceLabel(),
                          item.messageId(),
                          item.quotedSentence(),
                          item.matchedText()))
              .toList());
    }

    /**
     * 다시 쓴 표현 하나다.
     *
     * @param expressionId 공통 표현 ID
     * @param text 표현 원형
     * @param meaning 한국어 뜻
     * @param sourceLabel 배운 날과 곳. 예: "9월 10일 「주말 계획」"
     * @param messageId 이 표현을 쓴 사용자 발화 ID
     * @param quotedSentence 표현을 쓴 문장
     * @param matchedText 그 문장에서 굵게 처리할 구절
     */
    public record Item(
        long expressionId,
        String text,
        String meaning,
        String sourceLabel,
        long messageId,
        String quotedSentence,
        String matchedText) {}
  }

  /**
   * 다음 스몰톡의 후속 질문이다.
   *
   * @param pending 종료 후 장기기억 작업이 끝나지 않았으면 true
   * @param triggerType 질문의 계기. 질문이 없으면 null
   * @param question 굵게 나갈 질문. 질문이 없으면 null
   * @param invite 초대 한 줄. 질문이 없으면 null
   */
  public record FollowUp(
      boolean pending, FreeTalkFollowUpTriggerType triggerType, String question, String invite) {

    private static FollowUp of(FreeTalkFollowUpSummary summary) {
      return new FollowUp(
          summary.pending(), summary.triggerType(), summary.question(), summary.invite());
    }
  }
}
