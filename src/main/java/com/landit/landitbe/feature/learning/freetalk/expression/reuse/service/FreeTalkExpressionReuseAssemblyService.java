// AI가 판정한 표현 재사용을 다시 확인하고, 저장 시점의 표현·출처 값을 붙여 저장할 기록으로 만든다.

package com.landit.landitbe.feature.learning.freetalk.expression.reuse.service;

import com.landit.landitbe.feature.content.scenario.schedule.dto.ScenarioTitle;
import com.landit.landitbe.feature.content.scenario.service.ScenarioCatalogService;
import com.landit.landitbe.feature.learning.conversation.client.ai.AiConversationHistoryMessage;
import com.landit.landitbe.feature.learning.freetalk.domain.FreeTalkSession;
import com.landit.landitbe.feature.learning.freetalk.expression.client.ai.AiFreeTalkUsedExpression;
import com.landit.landitbe.feature.learning.freetalk.expression.domain.FreeTalkSessionExpression;
import com.landit.landitbe.feature.learning.freetalk.expression.repository.FreeTalkSessionExpressionRepository;
import com.landit.landitbe.feature.learning.freetalk.expression.reuse.domain.FreeTalkExpressionReuse;
import com.landit.landitbe.feature.learning.freetalk.expression.reuse.domain.FreeTalkExpressionReuseSource;
import com.landit.landitbe.feature.learning.freetalk.expression.reuse.dto.FreeTalkLearnedExpression;
import com.landit.landitbe.feature.learning.freetalk.repository.FreeTalkSessionRepository;
import com.landit.landitbe.shared.domain.ConversationSpeaker;
import com.landit.landitbe.shared.domain.Locale;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI의 표현 재사용 판정을 저장할 기록으로 바꾼다.
 *
 * <p>AI의 판정은 주장일 뿐이라 그대로 믿지 않는다. 보낸 후보에 있는 표현인지, 이번 대화의 사용자 발화인지, 그 조각이 발화 원문에 글자 그대로 있는지 다시 확인하고,
 * 맞지 않는 판정은 사유를 남기고 버린다. 하나가 틀려도 나머지 판정은 살린다.
 *
 * <p>지난 기록은 조회할 때마다 같아야 하므로 표현 원문·뜻·출처 제목·배운 날을 지금 값으로 복사해 기록에 담는다.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class FreeTalkExpressionReuseAssemblyService {

  private static final String DROPPED_LOG =
      "workflow=free_talk_expression_reuse reason={} freeTalkSessionId={} expressionId={}"
          + " messageId={}";
  private static final String SENTENCE_ENDS = ".!?";

  private final ScenarioCatalogService scenarioCatalogService;
  private final FreeTalkSessionExpressionRepository sessionExpressionRepository;
  private final FreeTalkSessionRepository freeTalkSessionRepository;

  /**
   * 판정을 다시 확인해 저장할 재사용 기록을 만든다. 저장은 하지 않는다.
   *
   * @param userProfileId 사용자 프로필 ID
   * @param freeTalkSessionId 표현을 다시 쓴 프리톡 세션 ID
   * @param targetLocale 학습 언어 locale
   * @param baseLocale 기준 언어 locale
   * @param history AI에 보낸 이번 대화
   * @param learnedExpressions AI에 보낸 배운 표현 후보
   * @param usedExpressions AI가 다시 썼다고 판정한 표현
   * @return 확인을 통과한 재사용 기록. AI가 돌려준 순서를 따른다
   */
  @Transactional(readOnly = true)
  public List<FreeTalkExpressionReuse> assemble(
      long userProfileId,
      long freeTalkSessionId,
      Locale targetLocale,
      Locale baseLocale,
      List<AiConversationHistoryMessage> history,
      List<FreeTalkLearnedExpression> learnedExpressions,
      List<AiFreeTalkUsedExpression> usedExpressions) {
    Map<Long, FreeTalkLearnedExpression> learnedById =
        learnedExpressions.stream()
            .collect(
                Collectors.toMap(FreeTalkLearnedExpression::expressionId, Function.identity()));
    Map<Long, String> userContentByMessageId =
        history.stream()
            .filter(message -> ConversationSpeaker.USER.name().equals(message.role()))
            .filter(message -> message.messageId() != null && message.content() != null)
            .collect(
                Collectors.toMap(
                    AiConversationHistoryMessage::messageId,
                    AiConversationHistoryMessage::content));
    List<AiFreeTalkUsedExpression> accepted = new ArrayList<>();
    Set<List<Long>> seen = new HashSet<>();
    for (AiFreeTalkUsedExpression used : usedExpressions) {
      String dropReason = dropReason(used, learnedById, userContentByMessageId, seen);
      if (dropReason != null) {
        log.warn(DROPPED_LOG, dropReason, freeTalkSessionId, used.expressionId(), used.messageId());
        continue;
      }
      accepted.add(used);
    }
    if (accepted.isEmpty()) {
      return List.of();
    }

    List<FreeTalkLearnedExpression> acceptedLearned =
        accepted.stream().map(used -> learnedById.get(used.expressionId())).distinct().toList();
    Map<Long, String> scenarioTitles = scenarioTitles(acceptedLearned, targetLocale, baseLocale);
    Map<Long, String> freeTalkTitles = freeTalkTitles(userProfileId, acceptedLearned);
    return accepted.stream()
        .map(
            used -> {
              FreeTalkLearnedExpression learned = learnedById.get(used.expressionId());
              return FreeTalkExpressionReuse.of(
                  userProfileId,
                  freeTalkSessionId,
                  used.messageId(),
                  learned.expressionId(),
                  learned.text(),
                  learned.meaning(),
                  learned.sourceType(),
                  learned.sourceType() == FreeTalkExpressionReuseSource.SCENARIO
                      ? scenarioTitles.get(learned.scenarioId())
                      : freeTalkTitles.get(learned.expressionId()),
                  learned.learnedOn(),
                  used.matchedText(),
                  sentenceContaining(
                      userContentByMessageId.get(used.messageId()), used.matchedText()));
            })
        .toList();
  }

  // 버려야 하면 사유를, 받아들이면 null을 돌려준다. 받아들인 (발화, 표현) 쌍은 seen에 남겨 같은 판정이 두 번 오면 거른다.
  private static String dropReason(
      AiFreeTalkUsedExpression used,
      Map<Long, FreeTalkLearnedExpression> learnedById,
      Map<Long, String> userContentByMessageId,
      Set<List<Long>> seen) {
    if (used.matchedText() == null || used.matchedText().isBlank()) {
      return "blank_text";
    }
    if (used.expressionId() == null || !learnedById.containsKey(used.expressionId())) {
      return "unknown_expression_id";
    }
    if (used.messageId() == null || !userContentByMessageId.containsKey(used.messageId())) {
      return "unknown_message_id";
    }
    // 화면이 이 조각을 발화 원문에서 대소문자까지 그대로 찾아 밑줄을 긋는다. 그래서 대소문자를 무시하지 않는다.
    if (!userContentByMessageId.get(used.messageId()).contains(used.matchedText())) {
      return "text_not_in_message";
    }
    if (!seen.add(List.of(used.messageId(), used.expressionId()))) {
      return "duplicate";
    }
    return null;
  }

  private Map<Long, String> scenarioTitles(
      List<FreeTalkLearnedExpression> learnedExpressions, Locale targetLocale, Locale baseLocale) {
    Set<Long> scenarioIds =
        learnedExpressions.stream()
            .filter(learned -> learned.sourceType() == FreeTalkExpressionReuseSource.SCENARIO)
            .map(FreeTalkLearnedExpression::scenarioId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    return scenarioCatalogService.findTitles(scenarioIds, targetLocale, baseLocale).stream()
        .collect(Collectors.toMap(ScenarioTitle::scenarioId, ScenarioTitle::title));
  }

  // 스몰톡에서 배운 표현의 출처는 그 표현을 학습 완료한 세션이다. 여러 세션에서 완료했으면 처음 완료한 세션을 고른다.
  // 완료한 세션을 찾지 못하거나 그 세션에 제목이 없으면 제목 없이 남긴다.
  private Map<Long, String> freeTalkTitles(
      long userProfileId, List<FreeTalkLearnedExpression> learnedExpressions) {
    List<Long> expressionIds =
        learnedExpressions.stream()
            .filter(learned -> learned.sourceType() == FreeTalkExpressionReuseSource.FREE_TALK)
            .map(FreeTalkLearnedExpression::expressionId)
            .toList();
    if (expressionIds.isEmpty()) {
      return Map.of();
    }
    Map<Long, Long> sessionIdByExpressionId =
        sessionExpressionRepository
            .findAllByUserProfileIdAndWritingExpressionIdIn(userProfileId, expressionIds)
            .stream()
            .filter(link -> link.getCompletedAt() != null)
            .sorted(
                Comparator.comparing(FreeTalkSessionExpression::getCompletedAt)
                    .thenComparing(FreeTalkSessionExpression::getId))
            .collect(
                Collectors.toMap(
                    FreeTalkSessionExpression::getWritingExpressionId,
                    FreeTalkSessionExpression::getFreeTalkSessionId,
                    (first, later) -> first));
    Map<Long, String> titleBySessionId =
        freeTalkSessionRepository
            .findAllById(new HashSet<>(sessionIdByExpressionId.values()))
            .stream()
            .filter(session -> session.getTitle() != null && !session.getTitle().isBlank())
            .collect(Collectors.toMap(FreeTalkSession::getId, FreeTalkSession::getTitle));
    return sessionIdByExpressionId.entrySet().stream()
        .filter(entry -> titleBySessionId.containsKey(entry.getValue()))
        .collect(
            Collectors.toMap(Map.Entry::getKey, entry -> titleBySessionId.get(entry.getValue())));
  }

  // 조각이 든 문장을 발화 원문에서 잘라 낸다. 문장은 마침표·느낌표·물음표나 줄바꿈으로 나눈다.
  // 조각이 두 문장에 걸쳐 있으면 걸친 문장을 모두 담는다. "Mr."처럼 약어의 마침표도 문장 끝으로 보므로 문장이 짧게 잘릴 수 있다.
  static String sentenceContaining(String content, String matchedText) {
    int matchStart = content.indexOf(matchedText);
    int matchEnd = matchStart + matchedText.length();
    int start = matchStart;
    while (start > 0 && !endsSentence(content.charAt(start - 1))) {
      start--;
    }
    int end = matchEnd;
    // 조각이 이미 문장 끝 부호로 끝났으면 그 뒤 문장까지 넘어가지 않는다.
    if (!endsSentence(content.charAt(matchEnd - 1))) {
      while (end < content.length() && !endsSentence(content.charAt(end))) {
        end++;
      }
    }
    while (end < content.length() && SENTENCE_ENDS.indexOf(content.charAt(end)) >= 0) {
      end++;
    }
    String sentence = content.substring(start, end).strip();
    return sentence.isEmpty() ? matchedText : sentence;
  }

  private static boolean endsSentence(char character) {
    return SENTENCE_ENDS.indexOf(character) >= 0 || character == '\n';
  }
}
