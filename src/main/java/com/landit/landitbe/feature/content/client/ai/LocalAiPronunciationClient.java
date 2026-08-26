// 로컬 개발과 테스트에서 사용할 결정적 발음 판정 대체 클라이언트다.

package com.landit.landitbe.feature.content.client.ai;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 로컬 개발과 테스트에서 사용할 결정적 발음 판정 대체 클라이언트다.
 *
 * <p>AI 서버 없이 전체 발음 평가 플로우를 확인할 수 있게, 2번째 단어는 음소 오류·4번째 단어는 강세 오류·나머지는 정상이라는 고정 판정을 반환한다. 단어가 그보다
 * 적으면 있는 단어까지만 판정한다.
 */
@Component
@ConditionalOnProperty(
    prefix = "landit.ai",
    name = "client-mode",
    havingValue = "local",
    matchIfMissing = true)
public class LocalAiPronunciationClient implements AiPronunciationClient {

  // 단어 구간 타임스탬프 스텁 값. 단어마다 500ms 간격으로 배치한다.
  private static final int WORD_DURATION_MS = 400;
  private static final int WORD_GAP_MS = 500;

  /** 고정 판정을 반환한다. 2번째 단어는 PHONEME_ERROR, 4번째 단어는 STRESS_ERROR, 나머지는 CORRECT다. */
  @Override
  public AiPronunciationAnalysisResult analyze(AiPronunciationAnalysisRequest request) {
    List<AiPronunciationAnalysisResult.Word> words = new ArrayList<>();
    for (AiPronunciationAnalysisRequest.Word word : request.words()) {
      words.add(judge(word));
    }
    return new AiPronunciationAnalysisResult(words);
  }

  // 단어 1개의 고정 판정을 만든다.
  private AiPronunciationAnalysisResult.Word judge(AiPronunciationAnalysisRequest.Word word) {
    int startMs = (word.order() - 1) * WORD_GAP_MS + 100;
    int endMs = startMs + WORD_DURATION_MS;
    if (word.order() == 2) {
      return new AiPronunciationAnalysisResult.Word(
          word.order(),
          word.word(),
          AiPronunciationWordStatus.PHONEME_ERROR,
          startMs,
          endMs,
          "nuh·ssing",
          "th",
          "ss",
          null);
    }
    if (word.order() == 4) {
      return new AiPronunciationAnalysisResult.Word(
          word.order(),
          word.word(),
          AiPronunciationWordStatus.STRESS_ERROR,
          startMs,
          endMs,
          null,
          null,
          null,
          1);
    }
    return new AiPronunciationAnalysisResult.Word(
        word.order(),
        word.word(),
        AiPronunciationWordStatus.CORRECT,
        startMs,
        endMs,
        null,
        null,
        null,
        null);
  }
}
