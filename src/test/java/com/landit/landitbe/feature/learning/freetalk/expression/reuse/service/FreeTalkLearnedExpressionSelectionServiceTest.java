// 표현 재사용 판정의 후보가 상한 안에서는 전부, 넘으면 발화와 겹치는 최근 표현으로 골라지는지 검증한다.

package com.landit.landitbe.feature.learning.freetalk.expression.reuse.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.content.expression.dto.ExpressionText;
import com.landit.landitbe.feature.content.expression.service.ExpressionContentService;
import com.landit.landitbe.feature.learning.expression.progress.domain.ExpressionLearningSource;
import com.landit.landitbe.feature.learning.expression.progress.dto.LearnedExpression;
import com.landit.landitbe.feature.learning.expression.progress.service.ExpressionCompletionService;
import com.landit.landitbe.feature.learning.freetalk.expression.reuse.domain.FreeTalkExpressionReuseSource;
import com.landit.landitbe.feature.learning.freetalk.expression.reuse.dto.FreeTalkLearnedExpression;
import com.landit.landitbe.shared.domain.Locale;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.LongStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 표현 재사용 판정의 후보가 상한 안에서는 전부, 넘으면 발화와 겹치는 최근 표현으로 골라지는지 검증한다. */
class FreeTalkLearnedExpressionSelectionServiceTest {

  private static final long USER_ID = 1207L;
  private static final LocalDateTime LEARNED_AT = LocalDateTime.of(2026, 9, 10, 21, 4);

  private final ExpressionCompletionService completionService =
      mock(ExpressionCompletionService.class);
  private final ExpressionContentService contentService = mock(ExpressionContentService.class);
  private final FreeTalkLearnedExpressionSelectionService service =
      new FreeTalkLearnedExpressionSelectionService(completionService, contentService);

  @DisplayName("배운 표현이 상한 이하면 발화와 겹치지 않아도 전부, 최근에 배운 순서 그대로 후보로 보낸다.")
  @Test
  void sendsEveryLearnedExpressionWithinLimit() {
    learned(
        new LearnedExpression(
            812L, ExpressionLearningSource.SCENARIO, 41L, LEARNED_AT, LEARNED_AT.plusDays(8)),
        new LearnedExpression(
            813L, ExpressionLearningSource.FREE_TALK, null, LEARNED_AT.minusDays(3), LEARNED_AT));
    texts(
        new ExpressionText(813L, "work out", "운동하다"),
        new ExpressionText(812L, "grab a coffee", "커피 한잔하다"));

    List<FreeTalkLearnedExpression> candidates =
        service.select(USER_ID, Locale.EN, Locale.KR, List.of("I like movies."));

    // 배운 날은 처음 학습을 마친 날이다. 다시 학습한 날(9월 18일)이 아니다.
    assertThat(candidates)
        .containsExactly(
            new FreeTalkLearnedExpression(
                812L,
                "grab a coffee",
                "커피 한잔하다",
                FreeTalkExpressionReuseSource.SCENARIO,
                41L,
                LocalDate.of(2026, 9, 10)),
            new FreeTalkLearnedExpression(
                813L,
                "work out",
                "운동하다",
                FreeTalkExpressionReuseSource.FREE_TALK,
                null,
                LocalDate.of(2026, 9, 7)));
  }

  @DisplayName("지금은 비활성이거나 다른 언어로 배워 본문을 읽지 못한 표현은 후보에서 뺀다.")
  @Test
  void dropsExpressionsWhoseTextIsNotAvailable() {
    learned(learnedAt(812L), learnedAt(999L));
    texts(new ExpressionText(812L, "grab a coffee", "커피 한잔하다"));

    assertThat(service.select(USER_ID, Locale.EN, Locale.KR, List.of("hi")))
        .extracting(FreeTalkLearnedExpression::expressionId)
        .containsExactly(812L);
  }

  @DisplayName("배운 표현이 상한을 넘으면 발화와 단어가 겹치는 표현만, 최근에 배운 순서로 상한까지 고른다.")
  @Test
  void keepsOnlyOverlappingExpressionsBeyondLimit() {
    List<LearnedExpression> learned = new ArrayList<>();
    List<ExpressionText> texts = new ArrayList<>();
    // 120개를 배웠고 번호가 작을수록 최근에 배웠다. 짝수 번호(60개)만 발화와 단어가 겹친다.
    LongStream.rangeClosed(1, 120)
        .forEach(
            id -> {
              learned.add(learnedAt(id));
              texts.add(
                  new ExpressionText(
                      id, id % 2 == 0 ? "Grab a COFFEE " + id : "work out " + id, "뜻"));
            });
    learned(learned.toArray(LearnedExpression[]::new));
    texts(texts.toArray(ExpressionText[]::new));

    List<FreeTalkLearnedExpression> candidates =
        service.select(
            USER_ID, Locale.EN, Locale.KR, List.of("Hi!", "Let's get some coffee, okay?"));

    // 겹치지 않는 홀수 번호는 최근에 배웠어도 빠지고, 겹치는 짝수 번호 중 최근 50개(2~100)만 남는다.
    assertThat(candidates)
        .extracting(FreeTalkLearnedExpression::expressionId)
        .containsExactlyElementsOf(
            LongStream.rangeClosed(1, 50).map(index -> index * 2).boxed().toList());
  }

  @DisplayName("상한을 넘을 때 관사·대명사 같은 흔한 단어만 겹치는 표현은 겹친 것으로 보지 않는다.")
  @Test
  void ignoresStopWordsWhenMatchingBeyondLimit() {
    List<LearnedExpression> learned = new ArrayList<>();
    List<ExpressionText> texts = new ArrayList<>();
    LongStream.rangeClosed(1, 51)
        .forEach(
            id -> {
              learned.add(learnedAt(id));
              texts.add(new ExpressionText(id, id == 7 ? "hit the gym" : "it is up to you", "뜻"));
            });
    learned(learned.toArray(LearnedExpression[]::new));
    texts(texts.toArray(ExpressionText[]::new));

    assertThat(
            service.select(
                USER_ID, Locale.EN, Locale.KR, List.of("I went to the GYM. It is you, right?")))
        .extracting(FreeTalkLearnedExpression::expressionId)
        .containsExactly(7L);
  }

  private void learned(LearnedExpression... expressions) {
    when(completionService.findLearnedExpressions(USER_ID)).thenReturn(List.of(expressions));
  }

  private void texts(ExpressionText... texts) {
    when(contentService.findActiveExpressionTexts(any(), eq(Locale.EN), eq(Locale.KR)))
        .thenReturn(List.of(texts));
  }

  private static LearnedExpression learnedAt(long expressionId) {
    return new LearnedExpression(
        expressionId, ExpressionLearningSource.SCENARIO, 41L, LEARNED_AT, LEARNED_AT);
  }
}
