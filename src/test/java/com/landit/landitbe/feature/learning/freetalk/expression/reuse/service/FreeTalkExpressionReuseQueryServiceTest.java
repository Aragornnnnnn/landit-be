// 저장된 표현 재사용 기록을 대화 보기와 요약 카드의 모양으로 바르게 읽는지 검증한다.

package com.landit.landitbe.feature.learning.freetalk.expression.reuse.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.learning.freetalk.expression.domain.ExpressionGenerationStatus;
import com.landit.landitbe.feature.learning.freetalk.expression.reuse.domain.FreeTalkExpressionReuse;
import com.landit.landitbe.feature.learning.freetalk.expression.reuse.domain.FreeTalkExpressionReuseSource;
import com.landit.landitbe.feature.learning.freetalk.expression.reuse.dto.FreeTalkExpressionReuseSummary;
import com.landit.landitbe.feature.learning.freetalk.expression.reuse.dto.FreeTalkReusedExpression;
import com.landit.landitbe.feature.learning.freetalk.expression.reuse.repository.FreeTalkExpressionReuseRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** 저장된 표현 재사용 기록을 대화 보기와 요약 카드의 모양으로 바르게 읽는지 검증한다. */
class FreeTalkExpressionReuseQueryServiceTest {

  private static final long FREE_TALK_SESSION_ID = 30L;

  private final FreeTalkExpressionReuseRepository reuseRepository =
      mock(FreeTalkExpressionReuseRepository.class);

  private final FreeTalkExpressionReuseQueryService service =
      new FreeTalkExpressionReuseQueryService(reuseRepository);

  @DisplayName("발화마다 먼저 기록된 재사용 표현 하나만 저장된 값 그대로 돌려준다.")
  @Test
  void returnsFirstReusedExpressionPerMessage() {
    stubReuses(
        reuse(5504L, 812L, "grab a coffee", "주말 계획", "grabbed a coffee"),
        reuse(5504L, 813L, "up to you", "주말 계획", "up to you"),
        reuse(5506L, 813L, "up to you", "주말 계획", "Up to you"));

    assertThat(service.findFirstByMessageId(FREE_TALK_SESSION_ID))
        .containsOnlyKeys(5504L, 5506L)
        .containsEntry(
            5504L, new FreeTalkReusedExpression(812L, "grab a coffee", "grabbed a coffee"))
        .containsEntry(5506L, new FreeTalkReusedExpression(813L, "up to you", "Up to you"));
  }

  @DisplayName("요약 카드는 표현마다 처음 쓴 한 번만, 배운 날과 출처(시나리오·스몰톡)를 붙여 돌려주고 출처 제목은 라벨에 넣지 않는다.")
  @Test
  void summarizesEachExpressionOnceWithSourceLabel() {
    stubReuses(
        reuse(5504L, 812L, "grab a coffee", "카페", "grabbed a coffee"),
        reuse(5506L, 812L, "grab a coffee", "카페", "grab a coffee"),
        reuse(5506L, 813L, "up to you", null, "up to you"),
        reuse(
            5507L,
            814L,
            "hit it off",
            FreeTalkExpressionReuseSource.FREE_TALK,
            "주말 계획",
            "hit it off"));

    FreeTalkExpressionReuseSummary summary =
        service.findSummary(FREE_TALK_SESSION_ID, ExpressionGenerationStatus.READY);

    assertThat(summary.pending()).isFalse();
    assertThat(summary.items())
        .containsExactly(
            new FreeTalkExpressionReuseSummary.Item(
                812L,
                "grab a coffee",
                "뜻 812",
                "9월 10일 - 시나리오",
                5504L,
                "문장 grabbed a coffee",
                "grabbed a coffee"),
            new FreeTalkExpressionReuseSummary.Item(
                813L, "up to you", "뜻 813", "9월 10일 - 시나리오", 5506L, "문장 up to you", "up to you"),
            new FreeTalkExpressionReuseSummary.Item(
                814L, "hit it off", "뜻 814", "9월 10일 - 스몰톡", 5507L, "문장 hit it off", "hit it off"));
  }

  @DisplayName("표현 작업이 아직 끝나지 않았고 기록도 없으면 기다리는 중으로 알린다.")
  @Test
  void waitsWhileExpressionGenerationIsPreparing() {
    stubReuses();

    assertThat(service.findSummary(FREE_TALK_SESSION_ID, ExpressionGenerationStatus.PREPARING))
        .isEqualTo(FreeTalkExpressionReuseSummary.waiting());
  }

  @DisplayName("표현 작업이 성공이든 실패든 끝났는데 기록이 없으면 기다리지 않는 빈 카드다.")
  @ParameterizedTest
  @EnumSource(
      value = ExpressionGenerationStatus.class,
      names = "PREPARING",
      mode = EnumSource.Mode.EXCLUDE)
  void returnsEmptySummaryWhenGenerationFinishedWithoutReuse(ExpressionGenerationStatus status) {
    stubReuses();

    assertThat(service.findSummary(FREE_TALK_SESSION_ID, status))
        .isEqualTo(new FreeTalkExpressionReuseSummary(false, List.of()));
  }

  private void stubReuses(FreeTalkExpressionReuse... reuses) {
    when(reuseRepository.findByFreeTalkSessionIdOrderByIdAsc(FREE_TALK_SESSION_ID))
        .thenReturn(List.of(reuses));
  }

  private static FreeTalkExpressionReuse reuse(
      long messageId, long expressionId, String text, String sourceTitle, String matchedText) {
    return reuse(
        messageId,
        expressionId,
        text,
        FreeTalkExpressionReuseSource.SCENARIO,
        sourceTitle,
        matchedText);
  }

  private static FreeTalkExpressionReuse reuse(
      long messageId,
      long expressionId,
      String text,
      FreeTalkExpressionReuseSource source,
      String sourceTitle,
      String matchedText) {
    return FreeTalkExpressionReuse.of(
        1207L,
        FREE_TALK_SESSION_ID,
        messageId,
        expressionId,
        text,
        "뜻 " + expressionId,
        source,
        sourceTitle,
        LocalDate.of(2026, 9, 10),
        matchedText,
        "문장 " + matchedText);
  }
}
