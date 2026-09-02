// ExpressionQueryService의 완료/잠김 계산, 학습 시작 상세 조회, 추가 예문 조회를 단위 검증한다.

package com.landit.landitbe.feature.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.landitbe.feature.content.domain.WritingExpression;
import com.landit.landitbe.feature.content.domain.WritingExpressionSource;
import com.landit.landitbe.feature.content.dto.ExpressionLearningResponse;
import com.landit.landitbe.feature.content.dto.ExpressionPracticeResponse;
import com.landit.landitbe.feature.content.dto.ExpressionRecommendationCandidate;
import com.landit.landitbe.feature.content.dto.ExpressionResponse;
import com.landit.landitbe.feature.content.dto.PracticeSentenceResponse;
import com.landit.landitbe.feature.content.dto.WritingSentenceResponse;
import com.landit.landitbe.feature.content.repository.ExpressionEmbeddingMatch;
import com.landit.landitbe.feature.content.repository.ExpressionEmbeddingSearchRepository;
import com.landit.landitbe.feature.content.repository.ExpressionPronunciationAssetRepository;
import com.landit.landitbe.feature.content.repository.FreeTalkCandidateSearch;
import com.landit.landitbe.feature.content.repository.WritingExpressionRepository;
import com.landit.landitbe.feature.learning.dto.CompletedExpressionIds;
import com.landit.landitbe.feature.learning.service.LearningProgressService;
import com.landit.landitbe.feature.profile.dto.UserLocale;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import com.landit.landitbe.shared.domain.ActiveStatus;
import com.landit.landitbe.shared.domain.Locale;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

/** ExpressionQueryService의 완료/잠김 계산, 학습 시작 상세 조회, 추가 예문 조회를 단위 검증한다. */
@ExtendWith(MockitoExtension.class)
class ExpressionQueryServiceTest {

  private static final Long USER_ID = 1L;
  private static final Long SCENARIO_ID = 999L;
  private static final Long EXPRESSION_ID = 101L;

  @Mock private ScenarioService scenarioService;

  @Mock private UserProfileService userProfileService;

  @Mock private WritingExpressionRepository writingExpressionRepository;

  @Mock private ExpressionEmbeddingSearchRepository expressionEmbeddingSearchRepository;

  @Mock private LearningProgressService learningProgressService;

  // learning-start의 발음 음성 URL 조회에 쓰는 의존성. 목으로 선언하지 않으면 @InjectMocks가
  // null로 둔 채 지나가서 관련 테스트가 NPE로 깨진다.
  @Mock private ExpressionPronunciationAssetRepository pronunciationAssetRepository;
  @Mock private UserAccentLocaleResolver accentLocaleResolver;

  @InjectMocks private ExpressionQueryService expressionQueryService;

  @Test
  void returnsCandidatesByIdsPreservingInputOrder() {
    WritingExpression first = mock(WritingExpression.class);
    when(first.getId()).thenReturn(101L);
    when(first.getTargetExpressionText()).thenReturn("target-101");
    when(first.getBaseExpressionMeaningText()).thenReturn("base-101");
    when(first.getUsageSummary()).thenReturn("제안에 동의할 때 사용");
    WritingExpression second = mock(WritingExpression.class);
    when(second.getId()).thenReturn(102L);
    when(second.getTargetExpressionText()).thenReturn("target-102");
    when(second.getBaseExpressionMeaningText()).thenReturn("base-102");
    when(second.getUsageSummary()).thenReturn("정중하게 거절할 때 사용");
    // 저장소는 순서를 보장하지 않아도 서비스가 입력 ID 순서를 유지해야 한다.
    when(writingExpressionRepository.findPublicExpressionCandidatesByIds(
            eq(List.of(102L, 101L)),
            eq(WritingExpressionSource.FREE_TALK),
            eq(Locale.EN),
            eq(Locale.KR),
            eq(ActiveStatus.ACTIVE)))
        .thenReturn(List.of(first, second));

    List<ExpressionRecommendationCandidate> candidates =
        expressionQueryService.getExpressionCandidatesByIds(
            List.of(102L, 101L), Locale.EN, Locale.KR);

    assertThat(candidates)
        .containsExactly(
            new ExpressionRecommendationCandidate(102L, "target-102", "base-102", "정중하게 거절할 때 사용"),
            new ExpressionRecommendationCandidate(101L, "target-101", "base-101", "제안에 동의할 때 사용"));
  }

  @Test
  void delegatesEmbeddingSearchToOwnedRepository() {
    List<ExpressionEmbeddingMatch> matches = List.of(new ExpressionEmbeddingMatch(101L, 0.2));
    FreeTalkCandidateSearch search =
        new FreeTalkCandidateSearch(List.of(1.0f), USER_ID, Locale.EN, Locale.KR, 3, 30);
    when(expressionEmbeddingSearchRepository.searchFreeTalkCandidates(search)).thenReturn(matches);

    List<ExpressionEmbeddingMatch> result =
        expressionQueryService.searchFreeTalkCandidatesByEmbedding(search);

    assertThat(result).isEqualTo(matches);
  }

  @Test
  void rejectsExpressionOutsidePublicFreeTalkCandidates() {
    when(writingExpressionRepository.findPublicExpressionCandidateById(
            EXPRESSION_ID,
            WritingExpressionSource.FREE_TALK,
            Locale.EN,
            Locale.KR,
            ActiveStatus.ACTIVE))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                expressionQueryService.validatePublicFreeTalkExpression(
                    EXPRESSION_ID, Locale.EN, Locale.KR))
        .isInstanceOf(ApiException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.AI_RESPONSE_INVALID);
  }

  @Test
  void acceptsPublicFreeTalkExpressionWithSameLocale() {
    WritingExpression expression = mock(WritingExpression.class);
    when(writingExpressionRepository.findPublicExpressionCandidateById(
            EXPRESSION_ID,
            WritingExpressionSource.FREE_TALK,
            Locale.EN,
            Locale.KR,
            ActiveStatus.ACTIVE))
        .thenReturn(Optional.of(expression));

    expressionQueryService.validatePublicFreeTalkExpression(EXPRESSION_ID, Locale.EN, Locale.KR);
  }

  @Test
  void shouldUnlockOnlyEarliestIncompleteExpression() {
    givenExpressions(expression(101L, 1), expression(102L, 2), expression(103L, 3));
    givenCompletedExpressionIds(101L);

    List<ExpressionResponse> responses =
        expressionQueryService.getExpressionsPerScenario(USER_ID, SCENARIO_ID);

    // 완료한 표현
    assertThat(responses.get(0).expressionId()).isEqualTo(101L);
    assertThat(responses.get(0).completed()).isTrue();
    assertThat(responses.get(0).locked()).isFalse();
    // 미완료 중 학습 순서가 가장 앞선 표현 → 해금
    assertThat(responses.get(1).completed()).isFalse();
    assertThat(responses.get(1).locked()).isFalse();
    // 그 뒤의 미완료 표현 → 잠김
    assertThat(responses.get(2).completed()).isFalse();
    assertThat(responses.get(2).locked()).isTrue();
  }

  @Test
  void shouldKeepAllExpressionsUnlockedWhenAllCompleted() {
    givenExpressions(expression(101L, 1), expression(102L, 2), expression(103L, 3));
    givenCompletedExpressionIds(101L, 102L, 103L);

    List<ExpressionResponse> responses =
        expressionQueryService.getExpressionsPerScenario(USER_ID, SCENARIO_ID);

    assertThat(responses)
        .allSatisfy(
            response -> {
              assertThat(response.completed()).isTrue();
              assertThat(response.locked()).isFalse();
            });
  }

  @Test
  void shouldUnlockOnlyEarliestExpressionWhenNoneCompleted() {
    givenExpressions(expression(101L, 1), expression(102L, 2), expression(103L, 3));
    givenCompletedExpressionIds();

    List<ExpressionResponse> responses =
        expressionQueryService.getExpressionsPerScenario(USER_ID, SCENARIO_ID);

    assertThat(responses.get(0).locked()).isFalse();
    assertThat(responses.get(1).locked()).isTrue();
    assertThat(responses.get(2).locked()).isTrue();
    assertThat(responses).allSatisfy(response -> assertThat(response.completed()).isFalse());
  }

  @Test
  void shouldPropagateWhenScenarioNotFound() {
    doThrow(new ApiException(ErrorCode.SCENARIO_NOT_FOUND))
        .when(scenarioService)
        .validateExists(SCENARIO_ID);

    assertThatThrownBy(() -> expressionQueryService.getExpressionsPerScenario(USER_ID, SCENARIO_ID))
        .isInstanceOf(ApiException.class);

    verify(writingExpressionRepository, never())
        .findByScenarioIdAndTargetLocaleAndBaseLocaleAndStatusOrderByDisplayOrderAsc(
            any(), any(), any(), any());
  }

  /** 표현 목록은 사용자 프로필의 locale(target/base) 기준으로 조회되는지 검증한다. (LAN-59 리뷰 반영) */
  @Test
  void shouldFindExpressionsByUserLocale() {
    givenExpressions(expression(101L, 1));
    givenCompletedExpressionIds();

    expressionQueryService.getExpressionsPerScenario(USER_ID, SCENARIO_ID);

    // 사용자 locale(en/ko)이 repository 조회 조건으로 그대로 전달된다
    verify(writingExpressionRepository)
        .findByScenarioIdAndTargetLocaleAndBaseLocaleAndStatusOrderByDisplayOrderAsc(
            SCENARIO_ID, Locale.EN, Locale.KR, ActiveStatus.ACTIVE);
  }

  @Test
  void shouldReturnLearningStartDetailsWhenExpressionFound() {
    // given: DB에 학습하려는 표현 데이터가 있는 상황 가정
    // (learningExpression() 내부의 getter 스터빙이 findById 스터빙과 중첩되지 않도록 mock을 먼저 만든다)
    WritingExpression expression = learningExpression();
    when(writingExpressionRepository.findByIdAndStatus(EXPRESSION_ID, ActiveStatus.ACTIVE))
        .thenReturn(Optional.of(expression));
    // 완료 이력이 있는 사용자로 가정해 완료 여부가 응답에 실리는지 함께 본다.
    when(learningProgressService.hasCompletedExpression(USER_ID, EXPRESSION_ID)).thenReturn(true);

    // when: getExpressionForLearning()를 호출하면
    ExpressionLearningResponse response =
        expressionQueryService.getExpressionForLearning(USER_ID, EXPRESSION_ID);

    // then: 응답에 표현 상세 정보가 담겨서 반환된다.
    assertThat(response.expressionId()).isEqualTo(EXPRESSION_ID);
    assertThat(response.completed()).isTrue();
    assertThat(response.targetExpressionText()).isEqualTo("blow my mind");
    assertThat(response.baseExpressionMeaningText()).isEqualTo("끝내주게 놀랍다");
    assertThat(response.usageDescription()).isEqualTo("usage-description입니다.");
    assertThat(response.representativeQuestionText())
        .isEqualTo("What should I definitely see in Korea?");
    assertThat(response.representativeQuestionTranslation()).isEqualTo("한국에서 뭘 꼭 봐야 해?");
    assertThat(response.representativeSentenceText())
        .isEqualTo("Gyeongbokgung Palace will blow your mind.");
    assertThat(response.representativeSentenceTranslation()).isEqualTo("경복궁은 널 완전 놀라게 할 거야.");
    // 정답 단어 배열은 정답 순서 그대로, 선택지 배열은 저장(섞인) 순서 그대로 유지되어야 한다.
    assertThat(response.representativeSentenceWords())
        .containsExactly("Gyeongbokgung", "Palace", "will", "blow", "your", "mind");
    assertThat(response.representativeSentenceWordChoices())
        .containsExactly(
            "Gyeongbokgung", "blow", "will", "Palace", "amazing", "have", "get", "your", "mind");
    assertThat(response.representativeImageUrl())
        .isEqualTo("https://cdn.example.com/images/101.png");
  }

  @Test
  void shouldAllowPublicExpressionForUserSpecificQueries() {
    WritingExpression expression = learningExpression();
    when(expression.getPracticeExamplesPayload()).thenReturn(makePracticeExamplesPayload(4));
    when(writingExpressionRepository.findByIdAndStatus(EXPRESSION_ID, ActiveStatus.ACTIVE))
        .thenReturn(Optional.of(expression));

    ExpressionLearningResponse learningResponse =
        expressionQueryService.getExpressionForLearning(USER_ID, EXPRESSION_ID);
    ExpressionPracticeResponse practiceResponse =
        expressionQueryService.getExtraPracticeExamples(USER_ID, EXPRESSION_ID);

    assertThat(learningResponse.expressionId()).isEqualTo(EXPRESSION_ID);
    assertThat(practiceResponse.practiceSentence()).hasSize(2);
    assertThat(practiceResponse.writingSentence()).hasSize(2);
  }

  @Test
  void shouldThrowWhenExpressionNotFound() {
    // given: DB에 해당 표현 데이터가 없는 상황 가정
    when(writingExpressionRepository.findByIdAndStatus(EXPRESSION_ID, ActiveStatus.ACTIVE))
        .thenReturn(Optional.empty());

    // when & then : 존재않는 표현 id로 getExpressionForLearning()를 호출하면 ApiException이 발생하고, errorCode가
    // RESOURCE_NOT_FOUND인지 검증
    assertThatThrownBy(
            () -> expressionQueryService.getExpressionForLearning(USER_ID, EXPRESSION_ID))
        .isInstanceOf(ApiException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
  }

  /**
   * "시나리오에 이 표현들이 저장되어 있다"는 상황을 만든다. 사용자 locale(en/ko)을 스터빙하고, 가짜 repository가 그 locale 기준 목록 조회로
   * 불리면 전달받은 표현 mock들을 그대로 돌려주도록 스터빙한다. (전달 순서 = displayOrder 오름차순 정렬 결과라고 가정하고 테스트를 작성한다)
   */
  private void givenExpressions(WritingExpression... expressions) {
    when(userProfileService.getUserLocale(USER_ID))
        .thenReturn(new UserLocale(Locale.EN, Locale.KR));
    when(writingExpressionRepository
            .findByScenarioIdAndTargetLocaleAndBaseLocaleAndStatusOrderByDisplayOrderAsc(
                eq(SCENARIO_ID), eq(Locale.EN), eq(Locale.KR), eq(ActiveStatus.ACTIVE)))
        .thenReturn(List.of(expressions));
  }

  /**
   * "사용자가 이 표현 ID들을 이미 완료했다"는 상황을 만든다. 전달받은 ID마다 완료 기록 mock을 만들어, 가짜 repository의 완료 목록 조회 반환값으로
   * 스터빙한다. 아무 인자도 안 넘기면(빈 가변인자) "하나도 완료하지 않은 상황"이 된다.
   */
  private void givenCompletedExpressionIds(Long... completedExpressionIds) {
    when(learningProgressService.findCompletedExpressionIds(USER_ID, SCENARIO_ID))
        .thenReturn(
            new CompletedExpressionIds(
                new HashSet<>(java.util.Arrays.asList(completedExpressionIds))));
  }

  /**
   * 시나리오별 목록 조회 테스트용 표현 mock을 만든다. 실제 WritingExpression은 생성자가 protected라 객체로 만들 수 없어서 mock으로 대체하고,
   * 목록 응답 매핑에 필요한 getter(id, displayOrder, 타겟 표현, 뜻)만 스터빙한다.
   */
  private WritingExpression expression(Long id, int displayOrder) {
    WritingExpression expression = mock(WritingExpression.class);
    when(expression.getId()).thenReturn(id);
    when(expression.getDisplayOrder()).thenReturn(displayOrder);
    when(expression.getTargetExpressionText()).thenReturn("target-" + id);
    when(expression.getBaseExpressionMeaningText()).thenReturn("base-" + id);
    return expression;
  }

  /** 학습 시작 상세 조회 테스트용 표현 mock을 만든다. (목록 조회용 expression()과 달리 상세 필드까지 스터빙) */
  private WritingExpression learningExpression() {
    WritingExpression expression = mock(WritingExpression.class);
    when(expression.getId()).thenReturn(EXPRESSION_ID);
    when(expression.getTargetExpressionText()).thenReturn("blow my mind");
    when(expression.getBaseExpressionMeaningText()).thenReturn("끝내주게 놀랍다");
    when(expression.getUsageDescription()).thenReturn("usage-description입니다.");
    when(expression.getRepresentativeQuestionText())
        .thenReturn("What should I definitely see in Korea?");
    when(expression.getRepresentativeQuestionTranslation()).thenReturn("한국에서 뭘 꼭 봐야 해?");
    when(expression.getRepresentativeSentenceText())
        .thenReturn("Gyeongbokgung Palace will blow your mind.");
    when(expression.getRepresentativeSentenceTranslation()).thenReturn("경복궁은 널 완전 놀라게 할 거야.");
    when(expression.getRepresentativeSentenceWords())
        .thenReturn(List.of("Gyeongbokgung", "Palace", "will", "blow", "your", "mind"));
    when(expression.getRepresentativeSentenceWordChoices())
        .thenReturn(
            List.of(
                "Gyeongbokgung",
                "blow",
                "will",
                "Palace",
                "amazing",
                "have",
                "get",
                "your",
                "mind"));
    when(expression.getRepresentativeImageUrl())
        .thenReturn("https://cdn.example.com/images/101.png");
    return expression;
  }

  // ===== 추가 예문 조회(getExtraPracticeExamples) 테스트 =====

  /** 없는 표현 ID로 추가 예문을 조회하면 RESOURCE_NOT_FOUND 예외를 던지고, 어떤 ID가 없었는지 warn 로그를 남긴다. */
  @Test
  void shouldLogAndThrowWhenExpressionIdNotFound() {
    // given: 로그를 검증하기 위해 서비스 로거에 ListAppender(로그를 리스트에 담아주는 가짜 출력지)를 부착
    Logger logger = (Logger) LoggerFactory.getLogger(ExpressionQueryService.class);
    ListAppender<ILoggingEvent> logAppender = new ListAppender<>();
    logAppender.start();
    logger.addAppender(logAppender);

    // given: DB에 해당 표현이 없는 상황
    when(writingExpressionRepository.findByIdAndStatus(EXPRESSION_ID, ActiveStatus.ACTIVE))
        .thenReturn(Optional.empty());

    // when & then: RESOURCE_NOT_FOUND 예외가 발생한다
    assertThatThrownBy(
            () -> expressionQueryService.getExtraPracticeExamples(USER_ID, EXPRESSION_ID))
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
  @Test
  void shouldReturnDetailForValidExpressionId() {
    // given: 예문 4개가 payload에 담긴 표현이 DB에 있는 상황
    WritingExpression expression =
        makeWritingExpressionMockWithInfo(makePracticeExamplesPayload(4));
    when(writingExpressionRepository.findByIdAndStatus(EXPRESSION_ID, ActiveStatus.ACTIVE))
        .thenReturn(Optional.of(expression));

    // when
    ExpressionPracticeResponse response =
        expressionQueryService.getExtraPracticeExamples(USER_ID, EXPRESSION_ID);

    // then: 표현 정보가 매핑된다
    assertThat(response.targetExpressionText()).isEqualTo("blow my mind");
    assertThat(response.baseExpressionMeaningText()).isEqualTo("끝내주게 놀랍다");
    assertThat(response.usageDescription()).isEqualTo("강렬한 인상을 받았을 때 최고의 리액션이에요.");

    // then: 예문 4개 중 2개만 눈으로 익히는 예문으로 내려간다 (어느 2개인지는 랜덤)
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

    // then: 작문 문제로 쓰인 예문은 눈으로 익히는 예문과 겹치지 않는다
    List<String> practiceTexts =
        response.practiceSentence().stream().map(PracticeSentenceResponse::sentenceText).toList();
    assertThat(response.writingSentence())
        .extracting(WritingSentenceResponse::writingSentenceText)
        .doesNotContainAnyElementsOf(practiceTexts)
        .doesNotHaveDuplicates();
  }

  /**
   * 같은 표현 ID를 여러 번 호출하면 작문 문제로 뽑히는 예문이 고정되지 않는지 검증한다. 랜덤이라 "항상 다름"은 보장할 수 없으므로, 100회 호출해 3가지 이상
   * 등장하는지 확인한다. (예문 4개 중 매번 같은 2개만 뽑힐 확률은 사실상 0)
   */
  @Test
  void shouldSelectDifferentWritingSentencesForRepeatedQueries() {
    // given
    WritingExpression expression =
        makeWritingExpressionMockWithInfo(makePracticeExamplesPayload(4));
    when(writingExpressionRepository.findByIdAndStatus(EXPRESSION_ID, ActiveStatus.ACTIVE))
        .thenReturn(Optional.of(expression));

    // when: 100회 호출하며 작문 문제로 뽑힌 문장 텍스트를 Set에 수집 (Set이라 중복은 1개로 합쳐짐)
    Set<String> pickedSentences = new HashSet<>();
    Set<Locale> pickedLanguages = new HashSet<>();
    for (int i = 0; i < 100; i++) {
      ExpressionPracticeResponse response =
          expressionQueryService.getExtraPracticeExamples(USER_ID, EXPRESSION_ID);
      response
          .writingSentence()
          .forEach(
              writing -> {
                pickedSentences.add(writing.writingSentenceText());
                pickedLanguages.add(writing.quizLanguage());
              });
    }

    // then: 예문 4개가 골고루 뽑히고 두 출제 언어가 모두 등장한다 = 랜덤 배정이 동작한다
    assertThat(pickedSentences.size()).isGreaterThan(2);
    assertThat(pickedLanguages).containsExactlyInAnyOrder(Locale.EN, Locale.KR);
  }

  /**
   * 유효한 예문이 4개보다 적으면 눈으로 익히는 예문 2건과 작문 문제 2건으로 나눌 수 없다. 개수를 줄여 내보내면 문제가 하나 빈 채로 학습이 진행되고 아무도 눈치채지
   * 못하므로, 콘텐츠 결함으로 보고 RESOURCE_NOT_FOUND로 드러낸다.
   */
  @Test
  void shouldThrowWhenValidExamplesAreFewerThanRequired() {
    // given: 예문이 3개뿐인 표현
    WritingExpression expression =
        makeWritingExpressionMockWithInfo(makePracticeExamplesPayload(3));
    when(writingExpressionRepository.findByIdAndStatus(EXPRESSION_ID, ActiveStatus.ACTIVE))
        .thenReturn(Optional.of(expression));

    // when & then
    assertThatThrownBy(
            () -> expressionQueryService.getExtraPracticeExamples(USER_ID, EXPRESSION_ID))
        .isInstanceOf(ApiException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
  }

  /** Payload가 빈 배열이면(예문 0개) writingSentence를 뽑을 수 없으므로 RESOURCE_NOT_FOUND 예외를 던진다. */
  @Test
  void shouldThrowWhenExpressionHasNoExamples() {
    // given: payload가 빈 배열인 표현
    WritingExpression expression = makeWritingExpressionMock(toJson("[]"));
    when(writingExpressionRepository.findByIdAndStatus(EXPRESSION_ID, ActiveStatus.ACTIVE))
        .thenReturn(Optional.of(expression));

    // when & then
    assertThatThrownBy(
            () -> expressionQueryService.getExtraPracticeExamples(USER_ID, EXPRESSION_ID))
        .isInstanceOf(ApiException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
  }

  /**
   * 기획자가 시딩한 예문에 필수 키가 빠졌거나 값이 비어 있으면, 그 예문만 응답에서 제외하고 경고 로그를 남긴다. (빈 예문 카드/빈 작문 문제가 사용자에게 노출되는 것을
   * 막고, 로그로 데이터 오류를 추적한다)
   */
  @Test
  void shouldExcludeInvalidExamplesAndLogWarning() {
    // given: 로그 검증용 ListAppender 부착
    Logger logger = (Logger) LoggerFactory.getLogger(ExpressionQueryService.class);
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
        expressionQueryService.getExtraPracticeExamples(USER_ID, EXPRESSION_ID);

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
  @Test
  void shouldMapWordArraysFromPickedExample() {
    // given: 예문 4개(각자 다른 단어 배열)가 payload에 담긴 표현
    WritingExpression expression =
        makeWritingExpressionMockWithInfo(makePracticeExamplesPayload(4));
    when(writingExpressionRepository.findByIdAndStatus(EXPRESSION_ID, ActiveStatus.ACTIVE))
        .thenReturn(Optional.of(expression));

    // when
    ExpressionPracticeResponse response =
        expressionQueryService.getExtraPracticeExamples(USER_ID, EXPRESSION_ID);

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
  @Test
  void shouldExcludeExamplesWithInvalidWordArrays() {
    // given: 로그 검증용 ListAppender 부착
    Logger logger = (Logger) LoggerFactory.getLogger(ExpressionQueryService.class);
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
        expressionQueryService.getExtraPracticeExamples(USER_ID, EXPRESSION_ID);

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
            () -> expressionQueryService.getExtraPracticeExamples(USER_ID, EXPRESSION_ID))
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

  /** JSON 문자열을 JsonNode로 변환한다. (체크 예외를 테스트에서 편하게 쓰기 위한 래퍼) */
  private JsonNode toJson(String json) {
    try {
      return new ObjectMapper().readTree(json);
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException("테스트 JSON이 잘못됐습니다: " + json, exception);
    }
  }
}
