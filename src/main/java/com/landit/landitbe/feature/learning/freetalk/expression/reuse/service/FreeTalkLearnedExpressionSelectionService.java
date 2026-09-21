// 표현 재사용 판정에 후보로 보낼, 사용자가 이전에 배운 표현을 고른다.

package com.landit.landitbe.feature.learning.freetalk.expression.reuse.service;

import com.landit.landitbe.feature.content.expression.dto.ExpressionText;
import com.landit.landitbe.feature.content.expression.service.ExpressionContentService;
import com.landit.landitbe.feature.learning.expression.progress.dto.LearnedExpression;
import com.landit.landitbe.feature.learning.expression.progress.service.ExpressionCompletionService;
import com.landit.landitbe.feature.learning.freetalk.expression.reuse.domain.FreeTalkExpressionReuseSource;
import com.landit.landitbe.feature.learning.freetalk.expression.reuse.dto.FreeTalkLearnedExpression;
import com.landit.landitbe.shared.domain.Locale;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 표현 재사용 판정의 후보를 고른다.
 *
 * <p>실제로 썼는지는 AI가 판정하므로 미리 걸러 내지 않는다. 배운 표현이 AI 서버가 받는 상한을 넘을 때만, 이번 대화의 사용자 발화와 단어가 겹치는 표현을 가장 최근에
 * 배운 순으로 상한까지 고른다.
 */
@RequiredArgsConstructor
@Service
public class FreeTalkLearnedExpressionSelectionService {

  /** 한 번의 요청에 실을 수 있는 배운 표현 수. AI 서버 계약의 상한과 같다. */
  public static final int MAX_LEARNED_EXPRESSIONS = 50;

  // 어느 문장에나 나와 겹침의 근거가 되지 못하는 단어다. 어간 처리는 하지 않는다(went와 go는 다른 단어로 본다).
  private static final Set<String> STOP_WORDS =
      Set.of(
          "a", "an", "the", "i", "you", "he", "she", "it", "we", "they", "me", "my", "your", "to",
          "of", "in", "on", "at", "for", "with", "and", "or", "but", "so", "is", "am", "are", "was",
          "were", "be", "been", "do", "does", "did", "have", "has", "had", "that", "this", "not",
          "no", "yes", "up", "out", "as", "if", "can", "will", "would", "just");

  private final ExpressionCompletionService expressionCompletionService;
  private final ExpressionContentService expressionContentService;

  /**
   * 사용자가 이전에 배운 표현 중 판정 후보로 보낼 것을 고른다.
   *
   * @param userProfileId 사용자 프로필 ID
   * @param targetLocale 학습 언어 locale
   * @param baseLocale 기준 언어 locale
   * @param userMessages 이번 대화의 사용자 발화 원문
   * @return 가장 최근에 배운 순의 후보. 상한을 넘지 않는다
   */
  @Transactional(readOnly = true)
  public List<FreeTalkLearnedExpression> select(
      long userProfileId, Locale targetLocale, Locale baseLocale, Collection<String> userMessages) {
    List<LearnedExpression> learned =
        expressionCompletionService.findLearnedExpressions(userProfileId);
    Map<Long, ExpressionText> textsById =
        expressionContentService
            .findActiveExpressionTexts(
                learned.stream().map(LearnedExpression::expressionId).toList(),
                targetLocale,
                baseLocale)
            .stream()
            .collect(Collectors.toMap(ExpressionText::id, Function.identity()));
    // 지금은 비활성이거나 다른 언어로 배운 표현은 본문을 읽지 못해 후보에서 빠진다. 최근에 배운 순서는 그대로 둔다.
    List<FreeTalkLearnedExpression> candidates =
        learned.stream()
            .map(expression -> candidate(expression, textsById.get(expression.expressionId())))
            .filter(Objects::nonNull)
            .toList();
    if (candidates.size() <= MAX_LEARNED_EXPRESSIONS) {
      return candidates;
    }
    Set<String> spokenWords =
        userMessages.stream()
            .flatMap(message -> words(message).stream())
            .collect(Collectors.toSet());
    return candidates.stream()
        .filter(candidate -> words(candidate.text()).stream().anyMatch(spokenWords::contains))
        .limit(MAX_LEARNED_EXPRESSIONS)
        .toList();
  }

  private static FreeTalkLearnedExpression candidate(
      LearnedExpression expression, ExpressionText text) {
    if (text == null) {
      return null;
    }
    return new FreeTalkLearnedExpression(
        expression.expressionId(),
        text.targetExpressionText(),
        text.baseExpressionMeaningText(),
        FreeTalkExpressionReuseSource.valueOf(expression.learningSource().name()),
        expression.scenarioId(),
        expression.completedAt().toLocalDate());
  }

  // 대소문자와 문장부호를 무시한 단어 집합. 축약형(don't)은 한 단어로 둔다.
  private static Set<String> words(String text) {
    return Arrays.stream(text.toLowerCase(java.util.Locale.ROOT).split("[^\\p{L}\\p{N}'’]+"))
        .filter(word -> !word.isBlank() && !STOP_WORDS.contains(word))
        .collect(Collectors.toSet());
  }
}
