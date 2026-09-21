// 스몰톡 요약의 표현 재사용 카드에 보여 줄 기록과 그 준비 상태를 전달한다.

package com.landit.landitbe.feature.learning.freetalk.expression.reuse.dto;

import com.landit.landitbe.feature.learning.freetalk.expression.reuse.domain.FreeTalkExpressionReuse;
import java.util.List;

/**
 * 요약 화면의 표현 재사용 카드다. 저장된 값을 그대로 담고 조회할 때 다시 만들지 않는다.
 *
 * @param pending 세션 종료 후 표현 작업이 아직 끝나지 않아 기록이 생길 수 있으면 true. 예: true
 * @param items 다시 쓴 표현. 표현마다 하나이며 세션에서 처음 쓴 순서다. 없으면 비어 있다
 */
public record FreeTalkExpressionReuseSummary(boolean pending, List<Item> items) {

  // 월 일 「출처 제목」
  private static final String SOURCE_LABEL_WITH_TITLE = "%d월 %d일 「%s」";
  private static final String SOURCE_LABEL_WITHOUT_TITLE = "%d월 %d일";

  /** 목록을 불변으로 보관한다. */
  public FreeTalkExpressionReuseSummary {
    items = List.copyOf(items);
  }

  /** 작업이 아직 끝나지 않아 기록을 기다리는 상태다. */
  public static FreeTalkExpressionReuseSummary waiting() {
    return new FreeTalkExpressionReuseSummary(true, List.of());
  }

  /**
   * 다시 쓴 표현 하나다.
   *
   * @param expressionId 표현 ID. 예: 812
   * @param text 저장 시점의 표현 원문. 예: "grab a coffee"
   * @param meaning 저장 시점의 표현 뜻. 예: "커피 한잔하다"
   * @param sourceLabel 그 표현을 배운 날과 곳(한국어 고정). 출처 제목을 남기지 못했으면 날짜만이다. 예: "9월 10일 「주말 계획」"
   * @param quotedSentence 표현을 쓴 문장. 예: "I grabbed a coffee with a friend."
   * @param matchedText 그 문장에서 강조할 조각. 예: "grabbed a coffee"
   */
  public record Item(
      long expressionId,
      String text,
      String meaning,
      String sourceLabel,
      String quotedSentence,
      String matchedText) {

    /** 저장된 재사용 기록을 그대로 옮긴다. */
    public static Item of(FreeTalkExpressionReuse reuse) {
      return new Item(
          reuse.getWritingExpressionId(),
          reuse.getExpressionText(),
          reuse.getExpressionMeaning(),
          sourceLabel(reuse),
          reuse.getQuotedSentence(),
          reuse.getMatchedText());
    }

    private static String sourceLabel(FreeTalkExpressionReuse reuse) {
      int month = reuse.getSourceLearnedOn().getMonthValue();
      int day = reuse.getSourceLearnedOn().getDayOfMonth();
      return reuse.getSourceTitle() == null
          ? SOURCE_LABEL_WITHOUT_TITLE.formatted(month, day)
          : SOURCE_LABEL_WITH_TITLE.formatted(month, day, reuse.getSourceTitle());
    }
  }
}
