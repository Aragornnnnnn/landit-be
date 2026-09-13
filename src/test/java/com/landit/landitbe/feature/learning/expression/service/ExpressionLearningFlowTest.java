// 콘텐츠와 표현 학습 Service를 조합해 목록·학습 시작의 기존 동작을 검증한다.

package com.landit.landitbe.feature.learning.expression.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.landitbe.feature.content.exception.ContentErrorCode;
import com.landit.landitbe.feature.content.expression.domain.WritingExpression;
import com.landit.landitbe.feature.content.expression.domain.WritingExpressionSource;
import com.landit.landitbe.feature.content.expression.dto.ExpressionLearningResponse;
import com.landit.landitbe.feature.content.expression.dto.ExpressionResponse;
import com.landit.landitbe.feature.content.expression.practice.dto.ExpressionPracticeResponse;
import com.landit.landitbe.feature.content.expression.practice.service.ExpressionPracticeService;
import com.landit.landitbe.feature.content.expression.pronunciation.repository.ExpressionPronunciationAssetRepository;
import com.landit.landitbe.feature.content.expression.pronunciation.service.ExpressionPronunciationQueryService;
import com.landit.landitbe.feature.content.expression.pronunciation.service.UserAccentLocaleResolver;
import com.landit.landitbe.feature.content.expression.recommendation.repository.ExpressionEmbeddingSearchRepository;
import com.landit.landitbe.feature.content.expression.recommendation.service.ExpressionRecommendationService;
import com.landit.landitbe.feature.content.expression.repository.WritingExpressionRepository;
import com.landit.landitbe.feature.content.expression.service.ExpressionQueryService;
import com.landit.landitbe.feature.content.scenario.service.ScenarioLearningLevelService;
import com.landit.landitbe.feature.content.scenario.service.ScenarioService;
import com.landit.landitbe.feature.learning.progress.dto.CompletedExpressionIds;
import com.landit.landitbe.feature.learning.progress.service.LearningProgressService;
import com.landit.landitbe.feature.profile.learning.dto.UserLocale;
import com.landit.landitbe.feature.profile.learning.service.ProfileLearningService;
import com.landit.landitbe.shared.domain.ActiveStatus;
import com.landit.landitbe.shared.domain.Locale;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 콘텐츠와 학습 Service를 조합한 학습 요청 계약을 검증한다. */
@ExtendWith(MockitoExtension.class)
class ExpressionLearningFlowTest {

  private static final Long USER_ID = 1L;
  private static final Long SCENARIO_ID = 999L;
  private static final Long EXPRESSION_ID = 101L;

  @Mock private ScenarioService scenarioService;

  @Mock
  private com.landit.landitbe.feature.subscription.service.LearningAccessGrantService accessGrants;

  @Mock private ProfileLearningService userProfileService;
  @Mock private ScenarioLearningLevelService scenarioLearningLevelService;

  @Mock private WritingExpressionRepository writingExpressionRepository;

  @Mock private ExpressionEmbeddingSearchRepository expressionEmbeddingSearchRepository;

  @Mock private LearningProgressService learningProgressService;

  // learning-start의 발음 음성 URL 조회에 쓰는 의존성. 목으로 선언하지 않으면 @InjectMocks가
  // null로 둔 채 지나가서 관련 테스트가 NPE로 깨진다.
  @Mock private ExpressionPronunciationAssetRepository pronunciationAssetRepository;
  @Mock private UserAccentLocaleResolver accentLocaleResolver;

  private ExpressionLearningQueryService expressionLearningQueryService;
  private ExpressionLearningStartService expressionLearningStartService;
  private ExpressionPracticeService expressionPracticeService;
  private ExpressionRecommendationService expressionRecommendationService;
  @InjectMocks private ExpressionQueryService expressionQueryService;

  @org.junit.jupiter.api.BeforeEach
  void allowLearningStart() {
    expressionLearningQueryService =
        new ExpressionLearningQueryService(expressionQueryService, learningProgressService);
    expressionLearningStartService =
        new ExpressionLearningStartService(
            expressionQueryService,
            accessGrants,
            new ExpressionPronunciationQueryService(
                pronunciationAssetRepository, accentLocaleResolver),
            learningProgressService);
    expressionPracticeService =
        new ExpressionPracticeService(writingExpressionRepository, scenarioLearningLevelService);
    expressionRecommendationService =
        new ExpressionRecommendationService(
            writingExpressionRepository, expressionEmbeddingSearchRepository);

    org.mockito.Mockito.lenient()
        .when(
            accessGrants.startExpression(
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong()))
        .thenReturn(
            new com.landit.landitbe.feature.subscription.dto.ExpressionLearningAttempt(
                "test-attempt", java.time.LocalDateTime.of(2026, 9, 12, 12, 0)));
  }

  @Test
  void shouldUnlockOnlyEarliestIncompleteExpression() {
    givenExpressions(expression(101L, 1), expression(102L, 2), expression(103L, 3));
    givenCompletedExpressionIds(101L);

    List<ExpressionResponse> responses =
        expressionLearningQueryService.getExpressionsPerScenario(USER_ID, SCENARIO_ID);

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
        expressionLearningQueryService.getExpressionsPerScenario(USER_ID, SCENARIO_ID);

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
        expressionLearningQueryService.getExpressionsPerScenario(USER_ID, SCENARIO_ID);

    assertThat(responses.get(0).locked()).isFalse();
    assertThat(responses.get(1).locked()).isTrue();
    assertThat(responses.get(2).locked()).isTrue();
    assertThat(responses).allSatisfy(response -> assertThat(response.completed()).isFalse());
  }

  @Test
  void shouldPropagateWhenScenarioNotFound() {
    doThrow(new ApiException(ContentErrorCode.SCENARIO_NOT_FOUND))
        .when(scenarioService)
        .validateExists(SCENARIO_ID);

    assertThatThrownBy(
            () -> expressionLearningQueryService.getExpressionsPerScenario(USER_ID, SCENARIO_ID))
        .isInstanceOf(ApiException.class);

    verify(writingExpressionRepository, never())
        .findScenarioExpressions(any(), any(), any(), anyInt(), anyInt(), any());
  }

  /** 표현 목록은 사용자 프로필의 locale(target/base) 기준으로 조회되는지 검증한다. (LAN-59 리뷰 반영) */
  @Test
  void shouldFindExpressionsByUserLocale() {
    givenExpressions(expression(101L, 1));
    givenCompletedExpressionIds();

    expressionLearningQueryService.getExpressionsPerScenario(USER_ID, SCENARIO_ID);

    // 사용자 locale(en/ko)이 repository 조회 조건으로 그대로 전달된다
    verify(writingExpressionRepository)
        .findScenarioExpressions(SCENARIO_ID, Locale.EN, Locale.KR, 2, 3, ActiveStatus.ACTIVE);
  }

  @Test
  void shouldRejectScenarioExpressionAboveUserDifficulty() {
    WritingExpression expression = mock(WritingExpression.class);
    when(expression.getExpressionSource()).thenReturn(WritingExpressionSource.SCENARIO);
    when(expression.getDifficultyLevel()).thenReturn(4);
    when(writingExpressionRepository.findByIdAndStatus(EXPRESSION_ID, ActiveStatus.ACTIVE))
        .thenReturn(Optional.of(expression));
    when(scenarioLearningLevelService.expressionLevel(
            org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong()))
        .thenReturn(com.landit.landitbe.feature.content.domain.ContentLearningLevel.LEVEL_2_TO_3);

    assertThatThrownBy(() -> expressionLearningStartService.startLearning(USER_ID, EXPRESSION_ID))
        .isInstanceOf(ApiException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
  }

  @Test
  void shouldRejectScenarioExpressionBelowUserDifficultyGroup() {
    WritingExpression expression = mock(WritingExpression.class);
    when(expression.getExpressionSource()).thenReturn(WritingExpressionSource.SCENARIO);
    when(expression.getDifficultyLevel()).thenReturn(1);
    when(writingExpressionRepository.findByIdAndStatus(EXPRESSION_ID, ActiveStatus.ACTIVE))
        .thenReturn(Optional.of(expression));
    when(scenarioLearningLevelService.expressionLevel(
            org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong()))
        .thenReturn(com.landit.landitbe.feature.content.domain.ContentLearningLevel.LEVEL_2_TO_3);

    assertThatThrownBy(() -> expressionLearningStartService.startLearning(USER_ID, EXPRESSION_ID))
        .isInstanceOf(ApiException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
  }

  @Test
  void shouldReturnScenarioExpressionAtUserMaximumDifficulty() {
    WritingExpression expression = learningExpression();
    when(expression.getExpressionSource()).thenReturn(WritingExpressionSource.SCENARIO);
    when(expression.getDifficultyLevel()).thenReturn(3);
    when(writingExpressionRepository.findByIdAndStatus(EXPRESSION_ID, ActiveStatus.ACTIVE))
        .thenReturn(Optional.of(expression));
    when(scenarioLearningLevelService.expressionLevel(
            org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong()))
        .thenReturn(com.landit.landitbe.feature.content.domain.ContentLearningLevel.LEVEL_2_TO_3);

    ExpressionLearningResponse response =
        expressionLearningStartService.startLearning(USER_ID, EXPRESSION_ID);

    assertThat(response.expressionId()).isEqualTo(EXPRESSION_ID);
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
        expressionLearningStartService.startLearning(USER_ID, EXPRESSION_ID);

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
    when(expression.getExpressionSource()).thenReturn(WritingExpressionSource.FREE_TALK);
    when(expression.getPracticeExamplesPayload()).thenReturn(makePracticeExamplesPayload(4));
    when(writingExpressionRepository.findByIdAndStatus(EXPRESSION_ID, ActiveStatus.ACTIVE))
        .thenReturn(Optional.of(expression));

    ExpressionLearningResponse learningResponse =
        expressionLearningStartService.startLearning(USER_ID, EXPRESSION_ID);
    ExpressionPracticeResponse practiceResponse =
        expressionPracticeService.getExtraPracticeExamples(USER_ID, EXPRESSION_ID);

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
    assertThatThrownBy(() -> expressionLearningStartService.startLearning(USER_ID, EXPRESSION_ID))
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
    when(scenarioLearningLevelService.expressionLevel(
            org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong()))
        .thenReturn(com.landit.landitbe.feature.content.domain.ContentLearningLevel.LEVEL_2_TO_3);
    when(writingExpressionRepository.findScenarioExpressions(
            eq(SCENARIO_ID), eq(Locale.EN), eq(Locale.KR), eq(2), eq(3), eq(ActiveStatus.ACTIVE)))
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
