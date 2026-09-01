// 로컬 개발과 테스트에서 사용할 결정적 발음 판정 대체 클라이언트다.

package com.landit.landitbe.feature.content.client.ai;

import com.landit.landitbe.feature.content.client.ai.dto.AiPronunciationAnalysisRequest;
import com.landit.landitbe.feature.content.client.ai.dto.AiPronunciationJudgedWord;
import com.landit.landitbe.feature.content.client.ai.dto.AiPronunciationWordStatus;
import java.time.Duration;
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

  // 단어 구간 타임스탬프 스텁 값. 단어마다 일정 간격으로 배치한다.
  private static final Duration WORD_DURATION = Duration.ofMillis(400);
  private static final Duration WORD_GAP = Duration.ofMillis(500);

  /**
   * 고정 판정을 반환한다. 2번째 단어는 PHONEME_ERROR, 4번째 단어는 STRESS_ERROR, 나머지는 CORRECT다.
   *
   * @param request 발음 분석 요청. 단어 목록의 order 기준으로 판정한다
   * @return 요청 단어와 1:1로 대응하는 고정 판정 목록 (타임스탬프는 단어당 일정 간격의 결정적 값)
   */
  @Override
  public List<AiPronunciationJudgedWord> analyze(AiPronunciationAnalysisRequest request) {
    return request.words().stream().map(this::judge).toList();
  }

  /**
   * 단어 1개의 고정 판정을 만든다.
   *
   * @param word 요청 단어
   * @return order에 따라 정해지는 고정 판정
   */
  private AiPronunciationJudgedWord judge(AiPronunciationAnalysisRequest.Word word) {
    int startMs = (int) ((word.order() - 1) * WORD_GAP.toMillis() + 100);
    int endMs = startMs + (int) WORD_DURATION.toMillis();
    if (word.order() == 2) {
      return new AiPronunciationJudgedWord(
          word.order(),
          word.word(),
          AiPronunciationWordStatus.PHONEME_ERROR,
          startMs,
          endMs,
          "nuh·ssing",
          "th",
          "ss",
          null);
    } else if (word.order() == 4) {
      return new AiPronunciationJudgedWord(
          word.order(),
          word.word(),
          AiPronunciationWordStatus.STRESS_ERROR,
          startMs,
          endMs,
          null,
          null,
          null,
          1);
    } else {
      return new AiPronunciationJudgedWord(
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
}
