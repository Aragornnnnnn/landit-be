// 직전 스몰톡의 교정에서 지켜볼 실수 패턴을 많이 틀린 순으로 최대 세 개 고르는지 검증한다.

package com.landit.landitbe.feature.learning.freetalk.feedback.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.learning.conversation.dto.SessionHistorySnapshot;
import com.landit.landitbe.feature.learning.conversation.history.service.SessionHistoryService;
import com.landit.landitbe.feature.learning.freetalk.domain.FreeTalkSession;
import com.landit.landitbe.feature.learning.freetalk.feedback.domain.FreeTalkMistakePattern;
import com.landit.landitbe.feature.learning.freetalk.feedback.dto.FreeTalkTurnCorrection;
import com.landit.landitbe.feature.learning.freetalk.repository.FreeTalkSessionRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

/** 직전 스몰톡의 교정에서 지켜볼 실수 패턴을 많이 틀린 순으로 최대 세 개 고르는지 검증한다. */
class FreeTalkWatchPatternServiceTest {

  private static final long LEARNING_SESSION_ID = 300L;
  private static final long PREVIOUS_LEARNING_SESSION_ID = 200L;
  private static final long PREVIOUS_HISTORY_ID = 2100L;

  private final FreeTalkSessionRepository freeTalkSessionRepository =
      mock(FreeTalkSessionRepository.class);
  private final SessionHistoryService sessionHistoryService = mock(SessionHistoryService.class);
  private final FreeTalkMessageFeedbackService messageFeedbackService =
      mock(FreeTalkMessageFeedbackService.class);

  private final FreeTalkWatchPatternService service =
      new FreeTalkWatchPatternService(
          freeTalkSessionRepository, sessionHistoryService, messageFeedbackService);

  @DisplayName("지켜볼 수 있는 유형만 세어 많이 틀린 순, 동률은 선언 순으로 최대 세 개를 고른다.")
  @Test
  void picksMostFrequentWatchablePatterns() {
    stubPreviousSession();
    stubCorrections(
        FreeTalkMistakePattern.ARTICLE,
        FreeTalkMistakePattern.ARTICLE,
        FreeTalkMistakePattern.PLURAL,
        FreeTalkMistakePattern.TENSE,
        // 지켜볼 수 없는 유형은 아무리 많아도 뽑지 않는다.
        FreeTalkMistakePattern.WORD_CHOICE,
        FreeTalkMistakePattern.WORD_CHOICE,
        FreeTalkMistakePattern.WORD_CHOICE,
        FreeTalkMistakePattern.PRONOUN,
        // 고칠 것이 없던 턴은 세지 않는다.
        null);

    assertThat(service.watchPatterns(LEARNING_SESSION_ID))
        .containsExactly(
            FreeTalkMistakePattern.ARTICLE,
            FreeTalkMistakePattern.TENSE,
            FreeTalkMistakePattern.PLURAL);
  }

  @DisplayName("첫 스몰톡이거나 직전 스몰톡에 지켜볼 교정이 없으면 비어 있다.")
  @Test
  void returnsEmptyWithoutPreviousSessionOrWatchableCorrections() {
    when(freeTalkSessionRepository.findPreviousCompleted(anyLong(), any())).thenReturn(List.of());
    assertThat(service.watchPatterns(LEARNING_SESSION_ID)).isEmpty();

    stubPreviousSession();
    stubCorrections(FreeTalkMistakePattern.NATURALNESS, null);
    assertThat(service.watchPatterns(LEARNING_SESSION_ID)).isEmpty();
  }

  @DisplayName("고르다 실패해도 예외를 밖으로 내지 않고 비어 있는 목록으로 교정 요청을 이어 가게 한다.")
  @Test
  void returnsEmptyWhenLookupFails() {
    when(freeTalkSessionRepository.findPreviousCompleted(anyLong(), any()))
        .thenThrow(new IllegalStateException("db down"));
    assertThat(service.watchPatterns(LEARNING_SESSION_ID)).isEmpty();

    org.mockito.Mockito.reset(freeTalkSessionRepository);
    stubPreviousSession();
    when(messageFeedbackService.findBySessionHistoryId(PREVIOUS_HISTORY_ID))
        .thenThrow(new IllegalStateException("db down"));
    assertThat(service.watchPatterns(LEARNING_SESSION_ID)).isEmpty();
  }

  @DisplayName("연결을 얻지 못하는 실패가 catch를 지나치지 않도록 트랜잭션을 열지 않는다.")
  @Test
  void doesNotOpenItsOwnTransaction() throws NoSuchMethodException {
    assertThat(
            FreeTalkWatchPatternService.class
                .getMethod("watchPatterns", long.class)
                .isAnnotationPresent(
                    org.springframework.transaction.annotation.Transactional.class))
        .isFalse();
    assertThat(
            FreeTalkWatchPatternService.class.isAnnotationPresent(
                org.springframework.transaction.annotation.Transactional.class))
        .isFalse();
  }

  private void stubPreviousSession() {
    FreeTalkSession previous = mock(FreeTalkSession.class);
    when(previous.getLearningSessionId()).thenReturn(PREVIOUS_LEARNING_SESSION_ID);
    when(freeTalkSessionRepository.findPreviousCompleted(LEARNING_SESSION_ID, PageRequest.of(0, 1)))
        .thenReturn(List.of(previous));
    SessionHistorySnapshot history = mock(SessionHistorySnapshot.class);
    when(history.getId()).thenReturn(PREVIOUS_HISTORY_ID);
    when(sessionHistoryService.findByLearningSessionId(PREVIOUS_LEARNING_SESSION_ID))
        .thenReturn(Optional.of(history));
  }

  private void stubCorrections(FreeTalkMistakePattern... patterns) {
    Map<Long, FreeTalkTurnCorrection> corrections = new LinkedHashMap<>();
    for (int index = 0; index < patterns.length; index++) {
      corrections.put(5000L + index, correction(patterns[index]));
    }
    when(messageFeedbackService.findBySessionHistoryId(PREVIOUS_HISTORY_ID))
        .thenReturn(corrections);
  }

  private static FreeTalkTurnCorrection correction(FreeTalkMistakePattern pattern) {
    if (pattern == null) {
      return FreeTalkTurnCorrection.completed(null, true);
    }
    return FreeTalkTurnCorrection.completed(
        new FreeTalkTurnCorrection.Sentence("I go.", "I went.", "과거", pattern), true);
  }
}
