// 직전 스몰톡에서 교정받은 실수 패턴 중 이번 세션의 턴 교정에서 지켜볼 것을 고른다.

package com.landit.landitbe.feature.learning.freetalk.feedback.service;

import com.landit.landitbe.feature.learning.conversation.history.service.SessionHistoryService;
import com.landit.landitbe.feature.learning.freetalk.domain.FreeTalkSession;
import com.landit.landitbe.feature.learning.freetalk.feedback.domain.FreeTalkMistakePattern;
import com.landit.landitbe.feature.learning.freetalk.feedback.dto.FreeTalkTurnCorrection;
import com.landit.landitbe.feature.learning.freetalk.innerthought.client.ai.AiFreeTalkInnerThoughtRequest;
import com.landit.landitbe.feature.learning.freetalk.repository.FreeTalkSessionRepository;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이번 세션의 턴 교정 요청에 실을 "지켜볼 실수 패턴"을 고른다.
 *
 * <p>직전 완료 스몰톡 하나의 교정에서 지켜볼 수 있는 유형만 세어 많이 틀린 순으로 최대 세 개다. 동률이면 유형의 선언 순서다. 직전 세션은 끝난 세션이라 값이 바뀌지
 * 않으므로 저장하지 않고 턴마다 다시 고른다. 첫 시도와 다시 요청하는 시도가 같은 값을 보낸다.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class FreeTalkWatchPatternService {

  private static final String FAILED_LOG =
      "지켜볼 실수 패턴을 고르지 못해 이번 턴은 사용례 판정 없이 교정만 요청한다. learningSessionId={}";

  private final FreeTalkSessionRepository freeTalkSessionRepository;
  private final SessionHistoryService sessionHistoryService;
  private final FreeTalkMessageFeedbackService messageFeedbackService;

  /**
   * 세션의 턴 교정 요청에 실을 지켜볼 실수 패턴을 고른다.
   *
   * <p>부가 판정의 재료라 고르다 실패해도 교정 요청을 막지 않는다. 그 턴은 지켜볼 패턴 없이 나간다.
   *
   * @param learningSessionId 지금 진행 중이거나 다시 교정하는 프리톡 학습 세션 ID
   * @return 많이 틀린 순의 지켜볼 패턴. 직전 스몰톡이 없거나 지켜볼 교정이 없으면 비어 있다
   */
  @Transactional(readOnly = true)
  public List<FreeTalkMistakePattern> watchPatterns(long learningSessionId) {
    try {
      return freeTalkSessionRepository
          .findPreviousCompleted(learningSessionId, PageRequest.of(0, 1))
          .stream()
          .findFirst()
          .map(this::mostFrequentWatchablePatterns)
          .orElse(List.of());
    } catch (RuntimeException exception) {
      log.warn(FAILED_LOG, learningSessionId, exception);
      return List.of();
    }
  }

  private List<FreeTalkMistakePattern> mostFrequentWatchablePatterns(FreeTalkSession previous) {
    Map<FreeTalkMistakePattern, Integer> counts = new EnumMap<>(FreeTalkMistakePattern.class);
    sessionHistoryService
        .findByLearningSessionId(previous.getLearningSessionId())
        .map(history -> messageFeedbackService.findBySessionHistoryId(history.getId()))
        .orElse(Map.of())
        .values()
        .stream()
        .map(FreeTalkTurnCorrection::sentence)
        .filter(Objects::nonNull)
        .map(FreeTalkTurnCorrection.Sentence::mistakePattern)
        .filter(pattern -> pattern != null && pattern.isWatchable())
        .forEach(pattern -> counts.merge(pattern, 1, Integer::sum));
    return counts.entrySet().stream()
        .sorted(
            Comparator.comparing(Map.Entry<FreeTalkMistakePattern, Integer>::getValue)
                .reversed()
                .thenComparing(Map.Entry::getKey))
        .map(Map.Entry::getKey)
        .limit(AiFreeTalkInnerThoughtRequest.MAX_WATCH_PATTERNS)
        .toList();
  }
}
