// 표현 추가 연습 예문 계약을 검증한다.

package com.landit.landitbe.feature.content.expression.practice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.landitbe.feature.content.expression.domain.WritingExpression;
import com.landit.landitbe.feature.content.expression.practice.dto.ExpressionPracticeResponse;
import com.landit.landitbe.feature.content.expression.practice.dto.PracticeSentenceResponse;
import com.landit.landitbe.feature.content.expression.practice.dto.WritingSentenceResponse;
import com.landit.landitbe.feature.content.expression.recommendation.repository.ExpressionEmbeddingSearchRepository;
import com.landit.landitbe.feature.content.expression.repository.WritingExpressionRepository;
import com.landit.landitbe.feature.learning.scenario.level.service.ScenarioLearningLevelService;
import com.landit.landitbe.shared.domain.ActiveStatus;
import com.landit.landitbe.shared.domain.Locale;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

/** 표현 업무의 조회 계약을 검증한다. */
@ExtendWith(MockitoExtension.class)
class ExpressionPracticeServiceTest {

  private static final Long USER_ID = 1L;
  private static final Long SCENARIO_ID = 999L;
  private static final Long EXPRESSION_ID = 101L;
  @Mock private WritingExpressionRepository writingExpressionRepository;
  @Mock private ExpressionEmbeddingSearchRepository expressionEmbeddingSearchRepository;
  @Mock private ScenarioLearningLevelService scenarioLearningLevelService;
  private com.landit.landitbe.feature.learning.expression.service.ExpressionLearningContentService
      expressionPracticeService;

  @org.junit.jupiter.api.BeforeEach
  void composeLearningContent() {
    expressionPracticeService =
        new com.landit.landitbe.feature.learning.expression.service
            .ExpressionLearningContentService(
            new com.landit.landitbe.feature.content.expression.service.ExpressionQueryService(
                writingExpressionRepository),
            org.mockito.Mockito.mock(
                com.landit.landitbe.feature.content.scenario.service.ScenarioService.class),
            org.mockito.Mockito.mock(
                com.landit.landitbe.feature.profile.learning.service.ProfileLearningService.class),
            scenarioLearningLevelService,
            new ExpressionPracticeService());
  }

  // ===== 추가 예문 조회(getExtraPracticeExamples) 테스트 =====

  /** 없는 표현 ID로 추가 예문을 조회하면 RESOURCE_NOT_FOUND 예외를 던지고, 어떤 ID가 없었는지 warn 로그를 남긴다. */
  @DisplayName("없는 표현 ID로 추가 예문을 조회하면 RESOURCE_NOT_FOUND 예외를 던지고, 어떤 ID가 없었는지 warn 로그를 남긴다.")
  @Test
  void shouldLogAndThrowWhenExpressionIdNotFound() {
    // given: 로그를 검증하기 위해 서비스 로거에 ListAppender(로그를 리스트에 담아주는 가짜 출력지)를 부착
    Logger logger =
        (Logger)
            LoggerFactory.getLogger(
                com.landit.landitbe.feature.content.expression.service.ExpressionQueryService
                    .class);
    ListAppender<ILoggingEvent> logAppender = new ListAppender<>();
    logAppender.start();
    logger.addAppender(logAppender);

    // given: DB에 해당 표현이 없는 상황
    when(writingExpressionRepository.findByIdAndStatus(EXPRESSION_ID, ActiveStatus.ACTIVE))
        .thenReturn(Optional.empty());

    // when & then: RESOURCE_NOT_FOUND 예외가 발생한다
    assertThatThrownBy(
            () -> expressionPracticeService.getExtraPracticeExamples(USER_ID, EXPRESSION_ID))
        .isInstanceOf(ApiException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);

    // then: 없는 표현 ID(101)가 포함된 warn 로그가 남는다
    assertThat(logAppender.list)
        .anySatisfy(
            event -> {
              assertThat(event.getLevel()).isEqualTo(Level.WARN);
              assertThat(event.getFormattedMessage()).contains(String.valueOf(EXPRESSION_ID));
            });

    logger.detachAppender(logAppender);
  }

  /** 적절한 표현 ID로 조회하면 표현 정보 + 눈으로 익히는 예문 2개 + 작문 문제 2개가 담긴 응답을 반환한다. */
  @DisplayName("적절한 표현 ID로 조회하면 표현 정보 + 눈으로 익히는 예문 2개 + 작문 문제 2개가 담긴 응답을 반환한다.")
  @Test
  void shouldReturnDetailForValidExpressionId() {
    // given: 예문 4개가 payload에 담긴 표현이 DB에 있는 상황
    WritingExpression expression =
        makeWritingExpressionMockWithInfo(makePracticeExamplesPayload(4));
    when(writingExpressionRepository.findByIdAndStatus(EXPRESSION_ID, ActiveStatus.ACTIVE))
        .thenReturn(Optional.of(expression));

    // when
    ExpressionPracticeResponse response =
        expressionPracticeService.getExtraPracticeExamples(USER_ID, EXPRESSION_ID);

    // then: 표현 정보가 매핑된다
    assertThat(response.targetExpressionText()).isEqualTo("blow my mind");
    assertThat(response.baseExpressionMeaningText()).isEqualTo("끝내주게 놀랍다");
    assertThat(response.usageDescription()).isEqualTo("강렬한 인상을 받았을 때 최고의 리액션이에요.");

    // then: 예문 4개 중 뒤 2개가 눈으로 익히는 예문으로 내려간다
    assertThat(response.practiceSentence()).hasSize(2);
    assertThat(response.practiceSentence())
        .allSatisfy(
            sentence -> {
              // payload의 sentence-N 형식을 그대로 따르며 필드가 빠짐없이 매핑된다
              String index = sentence.sentenceText().substring("sentence-".length());
              assertThat(sentence.highlightingPart()).isEqualTo("highlight-" + index);
              assertThat(sentence.sentenceTranslation()).isEqualTo("해석-" + index);
              assertThat(sentence.practiceQuestion()).isEqualTo("question-" + index);
              assertThat(sentence.practiceQuestionTranslation()).isEqualTo("질문해석-" + index);
            });

    // then: 작문 문제는 2건이며 출제 언어가 영어와 한국어 하나씩이다
    assertThat(response.writingSentence()).hasSize(2);
    assertThat(response.writingSentence())
        .extracting(WritingSentenceResponse::quizLanguage)
        .containsExactlyInAnyOrder(Locale.EN, Locale.KR);

    // then: 분배는 payload 순서로 고정이다. 뒤 2건이 예문, 앞 2건이 작문 문제다.
    assertThat(response.practiceSentence())
        .extracting(PracticeSentenceResponse::sentenceText)
        .containsExactly("sentence-2", "sentence-3");
    assertThat(response.writingSentence())
        .extracting(WritingSentenceResponse::writingSentenceText)
        .containsExactly("sentence-0", "sentence-1");
  }

  /**
   * 예문 분배는 payload 순서로 고정하되 출제 언어만 매 요청 달라지는지 검증한다. 랜덤이라 "항상 다름"은 보장할 수 없으므로 100회 호출해 두 언어가 모두
   * 등장하는지 확인한다.
   */
  @DisplayName("예문의 분배 순서는 고정하고 반복 출제에서 영어와 한국어를 모두 사용한다.")
  @Test
  void shouldKeepSentenceSplitFixedAndVaryQuizLanguage() {
    // given
    WritingExpression expression =
        makeWritingExpressionMockWithInfo(makePracticeExamplesPayload(4));
    when(writingExpressionRepository.findByIdAndStatus(EXPRESSION_ID, ActiveStatus.ACTIVE))
        .thenReturn(Optional.of(expression));

    // when: 100회 호출하며 분배 결과와 출제 언어를 모은다
    Set<List<String>> practiceSplits = new HashSet<>();
    Set<List<String>> writingSplits = new HashSet<>();
    Set<Locale> pickedLanguages = new HashSet<>();
    for (int i = 0; i < 100; i++) {
      ExpressionPracticeResponse response =
          expressionPracticeService.getExtraPracticeExamples(USER_ID, EXPRESSION_ID);
      practiceSplits.add(
          response.practiceSentence().stream()
              .map(PracticeSentenceResponse::sentenceText)
              .toList());
      writingSplits.add(
          response.writingSentence().stream()
              .map(WritingSentenceResponse::writingSentenceText)
              .toList());
      response.writingSentence().forEach(writing -> pickedLanguages.add(writing.quizLanguage()));
    }

    // then: 분배는 payload 순서 그대로 고정이다
    assertThat(practiceSplits).containsExactly(List.of("sentence-2", "sentence-3"));
    assertThat(writingSplits).containsExactly(List.of("sentence-0", "sentence-1"));

    // then: 출제 언어는 매번 달라져 두 언어가 모두 등장한다
    assertThat(pickedLanguages).containsExactlyInAnyOrder(Locale.EN, Locale.KR);
  }

  /**
   * 유효한 예문이 4개보다 적으면 눈으로 익히는 예문 2건과 작문 문제 2건으로 나눌 수 없다. 개수를 줄여 내보내면 문제가 하나 빈 채로 학습이 진행되고 아무도 눈치채지
   * 못하므로, 콘텐츠 결함으로 보고 RESOURCE_NOT_FOUND로 드러낸다.
   */
  @DisplayName("유효 예문이 4개보다 적으면 콘텐츠 없음 오류로 연습 구성을 거부한다.")
  @Test
  void shouldThrowWhenValidExamplesAreFewerThanRequired() {
    // given: 예문이 3개뿐인 표현
    WritingExpression expression =
        makeWritingExpressionMockWithInfo(makePracticeExamplesPayload(3));
    when(writingExpressionRepository.findByIdAndStatus(EXPRESSION_ID, ActiveStatus.ACTIVE))
        .thenReturn(Optional.of(expression));

    // when & then
    assertThatThrownBy(
            () -> expressionPracticeService.getExtraPracticeExamples(USER_ID, EXPRESSION_ID))
        .isInstanceOf(ApiException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
  }

  /** Payload가 빈 배열이면(예문 0개) writingSentence를 뽑을 수 없으므로 RESOURCE_NOT_FOUND 예외를 던진다. */
  @DisplayName("Payload가 빈 배열이면(예문 0개) writingSentence를 뽑을 수 없으므로 RESOURCE_NOT_FOUND 예외를 던진다.")
  @Test
  void shouldThrowWhenExpressionHasNoExamples() {
    // given: payload가 빈 배열인 표현
    WritingExpression expression = makeWritingExpressionMock(toJson("[]"));
    when(writingExpressionRepository.findByIdAndStatus(EXPRESSION_ID, ActiveStatus.ACTIVE))
        .thenReturn(Optional.of(expression));

    // when & then
    assertThatThrownBy(
            () -> expressionPracticeService.getExtraPracticeExamples(USER_ID, EXPRESSION_ID))
        .isInstanceOf(ApiException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
  }

  /**
   * 선택 필드인 imageUrl의 매핑을 검증한다. payload에 키가 없어도 파싱이 깨지지 않고 null로 매핑되며, 있으면 값 그대로 내려간다.
   *
   * <p>분배가 payload 순서 고정이므로 [2]는 키가 없는 예문, [3]은 키가 있는 예문으로 둔다.
   */
  @DisplayName("예문 이미지 URL이 없으면 null로 매핑하고 있으면 그대로 반환한다.")
  @Test
  void shouldMapMissingImageUrlToNull() {
    WritingExpression expression =
        makeWritingExpressionMockWithInfo(makePracticeExamplesPayloadWithoutThirdImage());
    when(writingExpressionRepository.findByIdAndStatus(EXPRESSION_ID, ActiveStatus.ACTIVE))
        .thenReturn(Optional.of(expression));

    ExpressionPracticeResponse response =
        expressionPracticeService.getExtraPracticeExamples(USER_ID, EXPRESSION_ID);

    assertThat(response.practiceSentence().get(0).imageUrl()).isNull();
    assertThat(response.practiceSentence().get(1).imageUrl())
        .isEqualTo("https://cdn.example.com/practice/3.png");
  }

  /**
   * 기획자가 시딩한 예문에 필수 키가 빠졌거나 값이 비어 있으면, 그 예문만 응답에서 제외하고 경고 로그를 남긴다. (빈 예문 카드/빈 작문 문제가 사용자에게 노출되는 것을
   * 막고, 로그로 데이터 오류를 추적한다)
   */
  @DisplayName("기획자가 시딩한 예문에 필수 키가 빠졌거나 값이 비어 있으면, 그 예문만 응답에서 제외하고 경고 로그를 남긴다.")
  @Test
  void shouldExcludeInvalidExamplesAndLogWarning() {
    // given: 로그 검증용 ListAppender 부착
    Logger logger = (Logger) LoggerFactory.getLogger(ExpressionPracticeService.class);
    ListAppender<ILoggingEvent> logAppender = new ListAppender<>();
    logAppender.start();
    logger.addAppender(logAppender);

    // given: 정상 4개 + 불량 3개(sentenceText 키 누락 / practiceQuestion 빈 문자열 / sentenceTranslation null)가
    // 섞인 payload. 정상 예문이 4개는 있어야 응답이 성립하므로 3, 4번을 함께 넣는다.
    WritingExpression expression =
        makeWritingExpressionMockWithInfo(
            toJson(
                """
                [
                  {
                    "sentenceText": "valid sentence 1",
                    "highlightingPart": "valid-1",
                    "sentenceTranslation": "정상 예문 1",
                    "practiceQuestion": "question-1?",
                    "practiceQuestionTranslation": "질문 1?",
                    "sentenceWords": ["valid", "sentence", "1"],
                    "sentenceWordChoices": ["sentence", "noise-1", "valid", "noise-2", "1", "noise-3"],
                    "sentenceTranslateWords": ["정상", "예문", "1"],
                    "sentenceTranslateWordChoices": ["예문", "오답-1", "정상", "오답-2", "1", "오답-3"]
                  },
                  {
                    "highlightingPart": "missing-text",
                    "sentenceTranslation": "sentenceText 키가 없음",
                    "practiceQuestion": "question?",
                    "practiceQuestionTranslation": "질문?"
                  },
                  {
                    "sentenceText": "blank question sentence",
                    "highlightingPart": "blank-question",
                    "sentenceTranslation": "practiceQuestion이 빈 문자열",
                    "practiceQuestion": "",
                    "practiceQuestionTranslation": "질문?"
                  },
                  {
                    "sentenceText": "null translation sentence",
                    "highlightingPart": "null-translation",
                    "sentenceTranslation": null,
                    "practiceQuestion": "question?",
                    "practiceQuestionTranslation": "질문?"
                  },
                  {
                    "sentenceText": "valid sentence 2",
                    "highlightingPart": "valid-2",
                    "sentenceTranslation": "정상 예문 2",
                    "practiceQuestion": "question-2?",
                    "practiceQuestionTranslation": "질문 2?",
                    "sentenceWords": ["valid", "sentence", "2"],
                    "sentenceWordChoices": ["sentence", "noise-1", "valid", "noise-2", "2", "noise-3"],
                    "sentenceTranslateWords": ["정상", "예문", "2"],
                    "sentenceTranslateWordChoices": ["예문", "오답-1", "정상", "오답-2", "2", "오답-3"]
                  },
                  {
                    "sentenceText": "valid sentence 3",
                    "highlightingPart": "valid-3",
                    "sentenceTranslation": "정상 예문 3",
                    "practiceQuestion": "question-3?",
                    "practiceQuestionTranslation": "질문 3?",
                    "sentenceWords": ["valid", "sentence", "3"],
                    "sentenceWordChoices": ["sentence", "noise-1", "valid", "noise-2", "3", "noise-3"],
                    "sentenceTranslateWords": ["정상", "예문", "3"],
                    "sentenceTranslateWordChoices": ["예문", "오답-1", "정상", "오답-2", "3", "오답-3"]
                  },
                  {
                    "sentenceText": "valid sentence 4",
                    "highlightingPart": "valid-4",
                    "sentenceTranslation": "정상 예문 4",
                    "practiceQuestion": "question-4?",
                    "practiceQuestionTranslation": "질문 4?",
                    "sentenceWords": ["valid", "sentence", "4"],
                    "sentenceWordChoices": ["sentence", "noise-1", "valid", "noise-2", "4", "noise-3"],
                    "sentenceTranslateWords": ["정상", "예문", "4"],
                    "sentenceTranslateWordChoices": ["예문", "오답-1", "정상", "오답-2", "4", "오답-3"]
                  }
                ]
                """));
    when(writingExpressionRepository.findByIdAndStatus(EXPRESSION_ID, ActiveStatus.ACTIVE))
        .thenReturn(Optional.of(expression));

    // when
    ExpressionPracticeResponse response =
        expressionPracticeService.getExtraPracticeExamples(USER_ID, EXPRESSION_ID);

    // then: 불량 예문 3개는 제외되고 정상 예문 4개만 2+2로 나뉘어 나간다
    List<String> deliveredTexts = new ArrayList<>();
    response.practiceSentence().forEach(sentence -> deliveredTexts.add(sentence.sentenceText()));
    response
        .writingSentence()
        .forEach(writing -> deliveredTexts.add(writing.writingSentenceText()));
    assertThat(deliveredTexts)
        .containsExactlyInAnyOrder(
            "valid sentence 1", "valid sentence 2", "valid sentence 3", "valid sentence 4");

    // then: 어떤 표현의 예문이 불량인지 warn 로그가 남는다
    assertThat(logAppender.list)
        .anySatisfy(
            event -> {
              assertThat(event.getLevel()).isEqualTo(Level.WARN);
              assertThat(event.getFormattedMessage()).contains(String.valueOf(EXPRESSION_ID));
            });

    logger.detachAppender(logAppender);
  }

  /**
   * 작문 문제의 단어 배열이 출제 언어에 맞게 실리는지 검증한다. 필드 이름은 언어 중립이므로, quizLanguage가 EN이면 영어 배열이, KR이면 한국어 배열이 순서
   * 그대로 들어가야 한다. (LAN-229 단어 칩 스펙 + LAN-360 한국어 퀴즈)
   */
  @DisplayName("작문 문제의 언어에 맞는 단어 배열을 원래 순서대로 반환한다.")
  @Test
  void shouldMapWordArraysFromPickedExample() {
    // given: 예문 4개(각자 다른 단어 배열)가 payload에 담긴 표현
    WritingExpression expression =
        makeWritingExpressionMockWithInfo(makePracticeExamplesPayload(4));
    when(writingExpressionRepository.findByIdAndStatus(EXPRESSION_ID, ActiveStatus.ACTIVE))
        .thenReturn(Optional.of(expression));

    // when
    ExpressionPracticeResponse response =
        expressionPracticeService.getExtraPracticeExamples(USER_ID, EXPRESSION_ID);

    // then: 뽑힌 예문의 인덱스(sentence-N의 끝자리)를 알아내, 출제 언어에 맞는 배열이 순서까지 일치하는지 확인
    assertThat(response.writingSentence())
        .allSatisfy(
            writing -> {
              String text = writing.writingSentenceText();
              String index = text.substring(text.length() - 1);
              if (writing.quizLanguage() == Locale.EN) {
                assertThat(writing.writingSentenceWords())
                    .containsExactly("chip-" + index + "-a", "chip-" + index + "-b");
                assertThat(writing.writingSentenceWordChoices())
                    .containsExactly(
                        "chip-" + index + "-b",
                        "noise-" + index + "-1",
                        "chip-" + index + "-a",
                        "noise-" + index + "-2",
                        "noise-" + index + "-3");
              } else {
                assertThat(writing.writingSentenceWords())
                    .containsExactly("조각-" + index + "-가", "조각-" + index + "-나");
                assertThat(writing.writingSentenceWordChoices())
                    .containsExactly(
                        "조각-" + index + "-나",
                        "오답-" + index + "-1",
                        "조각-" + index + "-가",
                        "오답-" + index + "-2",
                        "오답-" + index + "-3");
              }
            });
  }

  /**
   * 단어 배열 키가 누락됐거나, 빈 배열이거나, blank 원소를 담은 예문은 응답에서 제외되고 경고 로그가 남는지 검증한다. (LAN-229: 단어 칩을 만들 수 없는
   * 예문이 작문 문제로 노출되는 것을 막는다)
   */
  @DisplayName("단어 배열 키가 누락됐거나, 빈 배열이거나, blank 원소를 담은 예문은 응답에서 제외되고 경고 로그가 남는지 검증한다.")
  @Test
  void shouldExcludeExamplesWithInvalidWordArrays() {
    // given: 로그 검증용 ListAppender 부착
    Logger logger = (Logger) LoggerFactory.getLogger(ExpressionPracticeService.class);
    ListAppender<ILoggingEvent> logAppender = new ListAppender<>();
    logAppender.start();
    logger.addAppender(logAppender);

    // given: 정상 4개 + 불량 4개(sentenceWords 누락 / sentenceWordChoices 빈 배열 / sentenceWords에 blank 원소
    // / 한국어 배열 누락). 정상 예문이 4개는 있어야 응답이 성립한다.
    WritingExpression expression =
        makeWritingExpressionMockWithInfo(
            toJson(
                """
                [
                  {
                    "sentenceText": "valid sentence 1",
                    "highlightingPart": "valid-1",
                    "sentenceTranslation": "정상 예문 1",
                    "practiceQuestion": "question-1?",
                    "practiceQuestionTranslation": "질문 1?",
                    "sentenceWords": ["valid", "sentence", "1"],
                    "sentenceWordChoices": ["sentence", "noise-1", "valid", "noise-2", "1"],
                    "sentenceTranslateWords": ["정상", "예문", "1"],
                    "sentenceTranslateWordChoices": ["예문", "오답-1", "정상", "오답-2", "1"]
                  },
                  {
                    "sentenceText": "valid sentence 2",
                    "highlightingPart": "valid-2",
                    "sentenceTranslation": "정상 예문 2",
                    "practiceQuestion": "question-2?",
                    "practiceQuestionTranslation": "질문 2?",
                    "sentenceWords": ["valid", "sentence", "2"],
                    "sentenceWordChoices": ["sentence", "noise-1", "valid", "noise-2", "2"],
                    "sentenceTranslateWords": ["정상", "예문", "2"],
                    "sentenceTranslateWordChoices": ["예문", "오답-1", "정상", "오답-2", "2"]
                  },
                  {
                    "sentenceText": "valid sentence 3",
                    "highlightingPart": "valid-3",
                    "sentenceTranslation": "정상 예문 3",
                    "practiceQuestion": "question-3?",
                    "practiceQuestionTranslation": "질문 3?",
                    "sentenceWords": ["valid", "sentence", "3"],
                    "sentenceWordChoices": ["sentence", "noise-1", "valid", "noise-2", "3"],
                    "sentenceTranslateWords": ["정상", "예문", "3"],
                    "sentenceTranslateWordChoices": ["예문", "오답-1", "정상", "오답-2", "3"]
                  },
                  {
                    "sentenceText": "valid sentence 4",
                    "highlightingPart": "valid-4",
                    "sentenceTranslation": "정상 예문 4",
                    "practiceQuestion": "question-4?",
                    "practiceQuestionTranslation": "질문 4?",
                    "sentenceWords": ["valid", "sentence", "4"],
                    "sentenceWordChoices": ["sentence", "noise-1", "valid", "noise-2", "4"],
                    "sentenceTranslateWords": ["정상", "예문", "4"],
                    "sentenceTranslateWordChoices": ["예문", "오답-1", "정상", "오답-2", "4"]
                  },
                  {
                    "sentenceText": "missing words sentence",
                    "highlightingPart": "missing-words",
                    "sentenceTranslation": "sentenceWords 키가 없음",
                    "practiceQuestion": "question?",
                    "practiceQuestionTranslation": "질문?",
                    "sentenceWordChoices": ["sentence", "noise-1"]
                  },
                  {
                    "sentenceText": "empty choices sentence",
                    "highlightingPart": "empty-choices",
                    "sentenceTranslation": "sentenceWordChoices가 빈 배열",
                    "practiceQuestion": "question?",
                    "practiceQuestionTranslation": "질문?",
                    "sentenceWords": ["empty", "choices"],
                    "sentenceWordChoices": []
                  },
                  {
                    "sentenceText": "blank word sentence",
                    "highlightingPart": "blank-word",
                    "sentenceTranslation": "sentenceWords에 blank 원소",
                    "practiceQuestion": "question?",
                    "practiceQuestionTranslation": "질문?",
                    "sentenceWords": ["blank", ""],
                    "sentenceWordChoices": ["blank", "noise-1", "word"]
                  },
                  {
                    "sentenceText": "missing translate words sentence",
                    "highlightingPart": "missing-translate",
                    "sentenceTranslation": "한국어 단어 배열이 없음",
                    "practiceQuestion": "question?",
                    "practiceQuestionTranslation": "질문?",
                    "sentenceWords": ["missing", "translate"],
                    "sentenceWordChoices": ["translate", "noise-1", "missing"]
                  }
                ]
                """));
    when(writingExpressionRepository.findByIdAndStatus(EXPRESSION_ID, ActiveStatus.ACTIVE))
        .thenReturn(Optional.of(expression));

    // when
    ExpressionPracticeResponse response =
        expressionPracticeService.getExtraPracticeExamples(USER_ID, EXPRESSION_ID);

    // then: 불량 예문 4개는 제외되고 정상 예문 4개만 2+2로 나뉘어 나간다
    List<String> deliveredTexts = new ArrayList<>();
    response.practiceSentence().forEach(sentence -> deliveredTexts.add(sentence.sentenceText()));
    response
        .writingSentence()
        .forEach(writing -> deliveredTexts.add(writing.writingSentenceText()));
    assertThat(deliveredTexts)
        .containsExactlyInAnyOrder(
            "valid sentence 1", "valid sentence 2", "valid sentence 3", "valid sentence 4");

    // then: 어떤 표현의 예문이 불량인지 warn 로그가 남는다
    assertThat(logAppender.list)
        .anySatisfy(
            event -> {
              assertThat(event.getLevel()).isEqualTo(Level.WARN);
              assertThat(event.getFormattedMessage()).contains(String.valueOf(EXPRESSION_ID));
            });

    logger.detachAppender(logAppender);
  }

  /** 모든 예문이 불량이면(제외 후 0개) 작문 문제를 뽑을 수 없으므로 RESOURCE_NOT_FOUND 예외를 던진다. */
  @DisplayName("모든 예문이 불량이면(제외 후 0개) 작문 문제를 뽑을 수 없으므로 RESOURCE_NOT_FOUND 예외를 던진다.")
  @Test
  void shouldThrowWhenAllExamplesAreInvalid() {
    // given: 전부 필수 키가 빠진 payload
    WritingExpression expression =
        makeWritingExpressionMock(
            toJson(
                """
                [
                  { "highlightingPart": "only-highlight" },
                  { "sentenceTranslation": "해석만 있음" }
                ]
                """));
    when(writingExpressionRepository.findByIdAndStatus(EXPRESSION_ID, ActiveStatus.ACTIVE))
        .thenReturn(Optional.of(expression));

    // when & then
    assertThatThrownBy(
            () -> expressionPracticeService.getExtraPracticeExamples(USER_ID, EXPRESSION_ID))
        .isInstanceOf(ApiException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
  }

  // ===== 추가 예문 테스트용 헬퍼 =====

  /** Payload만 스터빙한 표현 mock. (표현 정보 getter까지 스터빙하면, 호출 안 되는 테스트에서 Mockito가 불필요 스터빙 오류를 내므로 분리) */
  private WritingExpression makeWritingExpressionMock(JsonNode payload) {
    WritingExpression expression = mock(WritingExpression.class);

    when(expression.getPracticeExamplesPayload()).thenReturn(payload);
    return expression;
  }

  /** Payload + 응답에 들어갈 표현 정보(타겟/뜻/설명)까지 스터빙한 표현 mock. */
  private WritingExpression makeWritingExpressionMockWithInfo(JsonNode payload) {
    WritingExpression expression = makeWritingExpressionMock(payload);

    when(expression.getTargetExpressionText()).thenReturn("blow my mind");
    when(expression.getBaseExpressionMeaningText()).thenReturn("끝내주게 놀랍다");
    when(expression.getUsageDescription()).thenReturn("강렬한 인상을 받았을 때 최고의 리액션이에요.");
    return expression;
  }

  /** 인덱스 0..count-1 값으로 구분되는 예문 count개짜리 payload JSON을 만든다. */
  private JsonNode makePracticeExamplesPayload(int count) {
    StringBuilder json = new StringBuilder("[");

    for (int i = 0; i < count; i++) {
      if (i > 0) {
        json.append(",");
      }
      json.append(
          """
                    {
                      "sentenceText": "sentence-%d",
                      "highlightingPart": "highlight-%d",
                      "sentenceTranslation": "해석-%d",
                      "practiceQuestion": "question-%d",
                      "practiceQuestionTranslation": "질문해석-%d",
                      "imageUrl": "https://cdn.example.com/practice/%d.png",
                      "sentenceWords": ["chip-%d-a", "chip-%d-b"],
                      "sentenceWordChoices": ["chip-%d-b", "noise-%d-1", "chip-%d-a", "noise-%d-2", "noise-%d-3"],
                      "sentenceTranslateWords": ["조각-%d-가", "조각-%d-나"],
                      "sentenceTranslateWordChoices": ["조각-%d-나", "오답-%d-1", "조각-%d-가", "오답-%d-2", "오답-%d-3"]
                    }
          """
              .formatted(i, i, i, i, i, i, i, i, i, i, i, i, i, i, i, i, i, i, i, i, i, i, i));
    }
    return toJson(json.append("]").toString());
  }

  /** 세 번째 예문에만 imageUrl 키가 없는 payload를 만든다. 선택 필드의 null 매핑을 확인할 때 쓴다. */
  private JsonNode makePracticeExamplesPayloadWithoutThirdImage() {
    return toJson(
        makePracticeExamplesPayload(4)
            .toString()
            .replace("\"imageUrl\":\"https://cdn.example.com/practice/2.png\",", ""));
  }

  /** JSON 문자열을 JsonNode로 변환한다. (체크 예외를 테스트에서 편하게 쓰기 위한 래퍼) */
  private JsonNode toJson(String json) {
    try {
      return new ObjectMapper().readTree(json);
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException("테스트 JSON이 잘못됐습니다: " + json, exception);
    }
  }
}
