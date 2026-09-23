// 표현 재사용 판정에 후보로 보낼, 사용자가 이전에 배운 표현을 고른다.

package com.landit.landitbe.feature.learning.freetalk.expression.reuse.service;

import com.landit.landitbe.feature.content.expression.dto.ExpressionText;
import com.landit.landitbe.feature.content.expression.service.ExpressionContentService;
import com.landit.landitbe.feature.learning.expression.progress.domain.ExpressionLearningSource;
import com.landit.landitbe.feature.learning.expression.progress.dto.LearnedExpression;
import com.landit.landitbe.feature.learning.expression.progress.service.ExpressionCompletionService;
import com.landit.landitbe.feature.learning.freetalk.expression.client.ai.AiFreeTalkExpressionRecommendationsRequest;
import com.landit.landitbe.feature.learning.freetalk.expression.reuse.domain.FreeTalkExpressionReuseSource;
import com.landit.landitbe.feature.learning.freetalk.expression.reuse.dto.FreeTalkLearnedExpression;
import com.landit.landitbe.shared.domain.Locale;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
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

  /** 한 번의 요청에 실을 수 있는 배운 표현 수. AI 서버 계약의 상한을 그대로 쓴다. */
  public static final int MAX_LEARNED_EXPRESSIONS =
      AiFreeTalkExpressionRecommendationsRequest.MAX_LEARNED_EXPRESSIONS;

  // 어느 문장에나 나와 겹침의 근거가 되지 못하는 단어다. 임의로 고르지 않고 Lucene·Elasticsearch의 영어 기본 불용어 목록
  // (EnglishAnalyzer.ENGLISH_STOP_WORDS_SET)을 그대로 쓴다. 어간 처리는 하지 않는다(went와 go는 다른 단어로 본다).
  private static final Set<String> ENGLISH_STOP_WORDS =
      Set.of(
          "a", "an", "and", "are", "as", "at", "be", "but", "by", "for", "if", "in", "into", "is",
          "it", "no", "not", "of", "on", "or", "such", "that", "the", "their", "then", "there",
          "these", "they", "this", "to", "was", "will", "with");

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
    // 완료 기록은 (표현, 배운 곳)마다 하나라 같은 표현이 두 번 나올 수 있다. AI 서버는 표현 ID가 겹친 요청을 거부하므로 가장 최근 기록 하나만 남긴다.
    Set<Long> seenExpressionIds = new HashSet<>();
    List<FreeTalkLearnedExpression> candidates =
        learned.stream()
            .filter(expression -> seenExpressionIds.add(expression.expressionId()))
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
    Set<String> stopWords = stopWordsOf(targetLocale);
    return candidates.stream()
        .filter(candidate -> overlaps(words(candidate.text()), spokenWords, stopWords))
        .limit(MAX_LEARNED_EXPRESSIONS)
        .toList();
  }

  // 표현의 내용어(불용어가 아닌 단어)가 하나라도 발화에 나오면 겹친 것으로 본다.
  // "up to you"처럼 불용어로만 된 표현은 내용어가 없으므로, 표현의 모든 단어가 발화에 나와야 겹친 것으로 본다.
  // 이 경우를 따로 두지 않으면 그런 표현은 그대로 말해도 후보가 되지 못한다.
  private static boolean overlaps(
      Set<String> expressionWords, Set<String> spokenWords, Set<String> stopWords) {
    if (expressionWords.isEmpty()) {
      return false;
    }
    Set<String> contentWords =
        expressionWords.stream()
            .filter(word -> !stopWords.contains(word))
            .collect(Collectors.toSet());
    if (contentWords.isEmpty()) {
      return spokenWords.containsAll(expressionWords);
    }
    return contentWords.stream().anyMatch(spokenWords::contains);
  }

  // 불용어 목록은 영어용이다. 다른 학습 언어에는 적용하지 않고 단어가 하나라도 겹치면 통과시킨다.
  private static Set<String> stopWordsOf(Locale targetLocale) {
    return targetLocale == Locale.EN ? ENGLISH_STOP_WORDS : Set.of();
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
        sourceOf(expression.learningSource()),
        expression.scenarioId(),
        expression.completedAt().toLocalDate());
  }

  // 배운 곳이 새로 생기면 여기서 컴파일이 막혀, 이름이 안 맞아 재사용 판정이 조용히 꺼지는 일이 없다.
  private static FreeTalkExpressionReuseSource sourceOf(ExpressionLearningSource learningSource) {
    return switch (learningSource) {
      case SCENARIO -> FreeTalkExpressionReuseSource.SCENARIO;
      case FREE_TALK -> FreeTalkExpressionReuseSource.FREE_TALK;
    };
  }

  // 대소문자와 문장부호를 무시한 단어 집합. 축약형(don't)은 한 단어로 두고, 둥근 아포스트로피(iOS 기본)는 곧은 것과 같게 본다.
  private static Set<String> words(String text) {
    return Arrays.stream(
            text.toLowerCase(java.util.Locale.ROOT).replace('’', '\'').split("[^\\p{L}\\p{N}']+"))
        .filter(word -> !word.isBlank())
        .collect(Collectors.toSet());
  }
}
