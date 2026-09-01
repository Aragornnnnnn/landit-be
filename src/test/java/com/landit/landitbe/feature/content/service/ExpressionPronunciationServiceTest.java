// 발음 평가 서비스가 AI 응답의 무결성을 검증하는지 단위 검증한다.

package com.landit.landitbe.feature.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.landitbe.feature.content.client.ai.AiPronunciationClient;
import com.landit.landitbe.feature.content.client.ai.dto.AiPronunciationJudgedWord;
import com.landit.landitbe.feature.content.client.ai.dto.AiPronunciationWordStatus;
import com.landit.landitbe.feature.content.domain.ExpressionPronunciationAsset;
import com.landit.landitbe.feature.content.domain.WritingExpression;
import com.landit.landitbe.feature.content.dto.PronunciationAnalysisResponse;
import com.landit.landitbe.feature.content.exception.AiPronunciationResponseInvalidException;
import com.landit.landitbe.feature.content.repository.ExpressionPronunciationAssetRepository;
import com.landit.landitbe.feature.content.repository.WritingExpressionRepository;
import com.landit.landitbe.shared.domain.AccentLocale;
import com.landit.landitbe.shared.domain.ActiveStatus;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

/**
 * 발음 평가 서비스가 AI 응답의 무결성을 검증하는지 단위 검증한다.
 *
 * <p>AI 응답은 개수가 맞아도 order 중복([1,1,2])·자산에 없는 order·null 판정·단어 텍스트 불일치가 섞일 수 있다. 이런 응답이 조용히 병합되지 않고
 * AI_RESPONSE_INVALID로 거부되는지를 가짜 클라이언트 응답으로 고정한다.
 */
@ExtendWith(MockitoExtension.class)
class ExpressionPronunciationServiceTest {

  private static final Long USER_ID = 1L;
  private static final Long EXPRESSION_ID = 101L;

  @Mock private WritingExpressionRepository writingExpressionRepository;
  @Mock private ExpressionPronunciationAssetRepository assetRepository;
  @Mock private UserAccentLocaleResolver accentLocaleResolver;
  @Mock private AiPronunciationClient aiPronunciationClient;

  // 코칭 템플릿은 순수 컴포넌트라 목 대신 실물을 쓴다.
  private final PronunciationCoachingTemplate coachingTemplate =
      new PronunciationCoachingTemplate();

  private ExpressionPronunciationService service;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() throws Exception {
    service =
        new ExpressionPronunciationService(
            writingExpressionRepository,
            assetRepository,
            accentLocaleResolver,
            aiPronunciationClient,
            coachingTemplate);

    WritingExpression expression = mock(WritingExpression.class);
    when(expression.getRepresentativeSentenceText()).thenReturn("There's nothing like hiking.");
    when(writingExpressionRepository.findByIdAndStatus(EXPRESSION_ID, ActiveStatus.ACTIVE))
        .thenReturn(Optional.of(expression));
    when(accentLocaleResolver.require(USER_ID)).thenReturn(AccentLocale.EN_US);

    // 3단어 자산: There's / nothing / like (TTS 완성 상태).
    ExpressionPronunciationAsset asset =
        new ExpressionPronunciationAsset(
            EXPRESSION_ID,
            AccentLocale.EN_US,
            objectMapper.readTree(
                """
                [
                  {"order": 1, "word": "There's", "pronunciationDisplay": "thairz",
                   "syllables": ["There's"], "stressIndex": 0,
                   "audioUrl": "https://cdn.example.com/1.mp3"},
                  {"order": 2, "word": "nothing", "pronunciationDisplay": "nuh·thing",
                   "syllables": ["nuh", "thing"], "stressIndex": 0,
                   "audioUrl": "https://cdn.example.com/2.mp3"},
                  {"order": 3, "word": "like", "pronunciationDisplay": "laik",
                   "syllables": ["like"], "stressIndex": 0,
                   "audioUrl": "https://cdn.example.com/3.mp3"}
                ]
                """));
    asset.attachTts(
        "https://cdn.example.com/expression.mp3",
        "https://cdn.example.com/sentence.mp3",
        asset.getWords());
    when(assetRepository.findByWritingExpressionIdAndAccentLocale(
            EXPRESSION_ID, AccentLocale.EN_US))
        .thenReturn(Optional.of(asset));
  }

  @Test
  void mergesValidAiResponseIntoScoreAndWords() {
    givenAiResponse(
        judged(1, "There's", AiPronunciationWordStatus.CORRECT),
        judged(2, "nothing", AiPronunciationWordStatus.PHONEME_ERROR),
        judged(3, "like", AiPronunciationWordStatus.CORRECT));

    PronunciationAnalysisResponse response = analyze();

    // 3단어 중 1오류 → 2/3 = 67점 반올림, 미통과.
    assertThat(response.score()).isEqualTo(67);
    assertThat(response.passed()).isFalse();
    assertThat(response.words()).hasSize(3);
    assertThat(response.words().get(1).nativeDisplay()).isEqualTo("nuh·thing");
  }

  @Test
  void rejectsDuplicatedOrderEvenWhenSizeMatches() {
    // 크기는 3으로 같지만 order가 [1, 1, 2]다 — 크기 검증만으로는 통과해버리는 응답.
    givenAiResponse(
        judged(1, "There's", AiPronunciationWordStatus.CORRECT),
        judged(1, "There's", AiPronunciationWordStatus.CORRECT),
        judged(2, "nothing", AiPronunciationWordStatus.CORRECT));

    assertInvalidAiResponse();
  }

  @Test
  void rejectsUnknownOrder() {
    givenAiResponse(
        judged(1, "There's", AiPronunciationWordStatus.CORRECT),
        judged(2, "nothing", AiPronunciationWordStatus.CORRECT),
        judged(99, "like", AiPronunciationWordStatus.CORRECT));

    assertInvalidAiResponse();
  }

  @Test
  void rejectsNullStatus() {
    givenAiResponse(
        judged(1, "There's", AiPronunciationWordStatus.CORRECT),
        judged(2, "nothing", null),
        judged(3, "like", AiPronunciationWordStatus.CORRECT));

    assertInvalidAiResponse();
  }

  @Test
  void rejectsWordTextMismatch() {
    // order는 맞지만 그 order의 단어가 자산과 다르다 — 판정이 엉뚱한 단어에 붙는 것을 막는다.
    givenAiResponse(
        judged(1, "There's", AiPronunciationWordStatus.CORRECT),
        judged(2, "something", AiPronunciationWordStatus.CORRECT),
        judged(3, "like", AiPronunciationWordStatus.CORRECT));

    assertInvalidAiResponse();
  }

  @Test
  void rejectsNullJudgedWordEntry() {
    // JSON 배열의 null 항목이 NPE(500)를 내는 대신 응답 오류로 처리돼야 한다.
    // List.of는 null을 못 담으므로 이 테스트만 Arrays.asList로 스텁을 만든다.
    when(aiPronunciationClient.analyze(any()))
        .thenReturn(
            Arrays.asList(
                judged(1, "There's", AiPronunciationWordStatus.CORRECT),
                null,
                judged(3, "like", AiPronunciationWordStatus.CORRECT)));

    assertInvalidAiResponse();
  }

  private void givenAiResponse(AiPronunciationJudgedWord... judgedWords) {
    when(aiPronunciationClient.analyze(any())).thenReturn(List.of(judgedWords));
  }

  private AiPronunciationJudgedWord judged(
      int order, String word, AiPronunciationWordStatus status) {
    return new AiPronunciationJudgedWord(order, word, status, 100, 400, null, null, null, null);
  }

  private PronunciationAnalysisResponse analyze() {
    return service.analyze(USER_ID, EXPRESSION_ID, sampleAudio());
  }

  private void assertInvalidAiResponse() {
    // 도메인 예외 타입과 오류 코드가 함께 맞아야 한다 (예외 파일럿 검증).
    assertThatThrownBy(this::analyze)
        .isInstanceOf(AiPronunciationResponseInvalidException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.AI_RESPONSE_INVALID);
  }

  private MockMultipartFile sampleAudio() {
    return new MockMultipartFile("audio", "recording.m4a", "audio/mp4", "fake-audio".getBytes());
  }
}
