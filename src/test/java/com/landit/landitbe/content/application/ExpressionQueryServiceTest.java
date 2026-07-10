// ExpressionQueryService의 완료/잠김 계산, 학습 시작 상세 조회, 추가 예문 조회를 단위 검증한다.
package com.landit.landitbe.content.application;

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
import com.landit.landitbe.auth.application.UserLocale;
import com.landit.landitbe.auth.application.UserProfileService;
import com.landit.landitbe.common.domain.ActiveStatus;
import com.landit.landitbe.common.exception.ApiException;
import com.landit.landitbe.common.exception.ErrorCode;
import com.landit.landitbe.content.api.dto.ExpressionLearningResponse;
import com.landit.landitbe.content.api.dto.ExpressionPracticeResponse;
import com.landit.landitbe.content.api.dto.ExpressionResponse;
import com.landit.landitbe.content.api.dto.PracticeSentenceResponse;
import com.landit.landitbe.content.domain.UserWritingExpressionCompletion;
import com.landit.landitbe.content.domain.WritingExpression;
import com.landit.landitbe.content.infrastructure.UserWritingExpressionCompletionRepository;
import com.landit.landitbe.content.infrastructure.WritingExpressionRepository;
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
import com.landit.landitbe.common.domain.Locale;

@ExtendWith(MockitoExtension.class)
class ExpressionQueryServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long SCENARIO_ID = 999L;
    private static final Long EXPRESSION_ID = 101L;

    @Mock
    private ScenarioService scenarioService;

    @Mock
    private UserProfileService userProfileService;

    @Mock
    private WritingExpressionRepository writingExpressionRepository;

    @Mock
    private UserWritingExpressionCompletionRepository userWritingExpressionCompletionRepository;

    @InjectMocks
    private ExpressionQueryService expressionQueryService;

    @Test
    void 미완료_표현_중_학습_순서가_가장_앞선_하나만_해금된다() {
        givenExpressions(expression(101L, 1), expression(102L, 2), expression(103L, 3));
        givenCompletedExpressionIds(101L);

        List<ExpressionResponse> responses = expressionQueryService.getExpressionsPerScenario(USER_ID, SCENARIO_ID);

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
    void 모든_표현을_완료하면_전부_잠기지_않는다() {
        givenExpressions(expression(101L, 1), expression(102L, 2), expression(103L, 3));
        givenCompletedExpressionIds(101L, 102L, 103L);

        List<ExpressionResponse> responses = expressionQueryService.getExpressionsPerScenario(USER_ID, SCENARIO_ID);

        assertThat(responses).allSatisfy(response -> {
            assertThat(response.completed()).isTrue();
            assertThat(response.locked()).isFalse();
        });
    }

    @Test
    void 아무것도_완료하지_않으면_학습_순서가_가장_앞선_표현만_해금된다() {
        givenExpressions(expression(101L, 1), expression(102L, 2), expression(103L, 3));
        givenCompletedExpressionIds();

        List<ExpressionResponse> responses = expressionQueryService.getExpressionsPerScenario(USER_ID, SCENARIO_ID);

        assertThat(responses.get(0).locked()).isFalse();
        assertThat(responses.get(1).locked()).isTrue();
        assertThat(responses.get(2).locked()).isTrue();
        assertThat(responses).allSatisfy(response -> assertThat(response.completed()).isFalse());
    }

    @Test
    void 존재하지_않는_시나리오면_표현을_조회하지_않고_예외를_전파한다() {
        doThrow(new ApiException(ErrorCode.SCENARIO_NOT_FOUND))
                .when(scenarioService).validateExists(SCENARIO_ID);

        assertThatThrownBy(() -> expressionQueryService.getExpressionsPerScenario(USER_ID, SCENARIO_ID))
                .isInstanceOf(ApiException.class);

        verify(writingExpressionRepository, never())
                .findByScenarioIdAndTargetLocaleAndBaseLocaleAndStatusOrderByDisplayOrderAsc(any(), any(), any(), any());
    }

    /** 표현 목록은 사용자 프로필의 locale(target/base) 기준으로 조회되는지 검증한다. (LAN-59 리뷰 반영) */
    @Test
    void 표현_목록은_사용자_locale_기준으로_조회한다() {
        givenExpressions(expression(101L, 1));
        givenCompletedExpressionIds();

        expressionQueryService.getExpressionsPerScenario(USER_ID, SCENARIO_ID);

        // 사용자 locale(en/ko)이 repository 조회 조건으로 그대로 전달된다
        verify(writingExpressionRepository)
                .findByScenarioIdAndTargetLocaleAndBaseLocaleAndStatusOrderByDisplayOrderAsc(
                        SCENARIO_ID, Locale.EN, Locale.KR, ActiveStatus.ACTIVE);
    }

    @Test
    void 표현을_찾으면_학습_시작_상세_정보를_응답으로_반환한다() {
        // given: DB에 학습하려는 표현 데이터가 있는 상황 가정
        // (learningExpression() 내부의 getter 스터빙이 findById 스터빙과 중첩되지 않도록 mock을 먼저 만든다)
        WritingExpression expression = learningExpression();
        when(writingExpressionRepository.findByIdAndStatus(EXPRESSION_ID, ActiveStatus.ACTIVE))
                .thenReturn(Optional.of(expression));

        // when: getExpressionForLearning()를 호출하면
        ExpressionLearningResponse response = expressionQueryService.getExpressionForLearning(EXPRESSION_ID);

        // then: 응답에 표현 상세 정보가 담겨서 반환된다.
        assertThat(response.expressionId()).isEqualTo(EXPRESSION_ID);
        assertThat(response.targetExpressionText()).isEqualTo("blow my mind");
        assertThat(response.baseExpressionMeaningText()).isEqualTo("끝내주게 놀랍다");
        assertThat(response.usageDescription()).isEqualTo("usage-description입니다.");
        assertThat(response.representativeQuestionText()).isEqualTo("What should I definitely see in Korea?");
        assertThat(response.representativeQuestionTranslation()).isEqualTo("한국에서 뭘 꼭 봐야 해?");
        assertThat(response.representativeSentenceText()).isEqualTo("Gyeongbokgung Palace will blow your mind.");
        assertThat(response.representativeSentenceTranslation()).isEqualTo("경복궁은 널 완전 놀라게 할 거야.");
        assertThat(response.highlightingPart()).isEqualTo("널 완전 놀라게 할 거야."); // highlightingPart는 엔티티의 representativeSentenceTranslationHighlightText에서 온다.
        assertThat(response.representativeImageUrl()).isEqualTo("https://cdn.example.com/images/101.png");
    }

    @Test
    void 표현을_찾지_못하면_RESOURCE_NOT_FOUND_예외를_던진다() {
        // given: DB에 해당 표현 데이터가 없는 상황 가정
        when(writingExpressionRepository.findByIdAndStatus(EXPRESSION_ID, ActiveStatus.ACTIVE))
                .thenReturn(Optional.empty());

        // when & then : 존재않는 표현 id로 getExpressionForLearning()를 호출하면 ApiException이 발생하고, errorCode가 RESOURCE_NOT_FOUND인지 검증
        assertThatThrownBy(() -> expressionQueryService.getExpressionForLearning(EXPRESSION_ID))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    /**
     * "시나리오에 이 표현들이 저장되어 있다"는 상황을 만든다.
     * 사용자 locale(en/ko)을 스터빙하고, 가짜 repository가 그 locale 기준 목록 조회로 불리면
     * 전달받은 표현 mock들을 그대로 돌려주도록 스터빙한다.
     * (전달 순서 = displayOrder 오름차순 정렬 결과라고 가정하고 테스트를 작성한다)
     */
    private void givenExpressions(WritingExpression... expressions) {
        when(userProfileService.getUserLocale(USER_ID)).thenReturn(new UserLocale(Locale.EN, Locale.KR));
        when(writingExpressionRepository
                .findByScenarioIdAndTargetLocaleAndBaseLocaleAndStatusOrderByDisplayOrderAsc(
                        eq(SCENARIO_ID), eq(Locale.EN), eq(Locale.KR), eq(ActiveStatus.ACTIVE)))
                .thenReturn(List.of(expressions));
    }

    /**
     * "사용자가 이 표현 ID들을 이미 완료했다"는 상황을 만든다.
     * 전달받은 ID마다 완료 기록 mock을 만들어, 가짜 repository의 완료 목록 조회 반환값으로 스터빙한다.
     * 아무 인자도 안 넘기면(빈 가변인자) "하나도 완료하지 않은 상황"이 된다.
     */
    private void givenCompletedExpressionIds(Long... completedExpressionIds) {
        List<UserWritingExpressionCompletion> completions = java.util.Arrays.stream(completedExpressionIds)
                .map(this::completion)
                .toList();
        when(userWritingExpressionCompletionRepository
                .findAllByUserProfileIdAndScenarioId(USER_ID, SCENARIO_ID))
                .thenReturn(completions);
    }

    /**
     * 시나리오별 목록 조회 테스트용 표현 mock을 만든다.
     * 실제 WritingExpression은 생성자가 protected라 객체로 만들 수 없어서 mock으로 대체하고,
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
        when(expression.getRepresentativeQuestionText()).thenReturn("What should I definitely see in Korea?");
        when(expression.getRepresentativeQuestionTranslation()).thenReturn("한국에서 뭘 꼭 봐야 해?");
        when(expression.getRepresentativeSentenceText()).thenReturn("Gyeongbokgung Palace will blow your mind.");
        when(expression.getRepresentativeSentenceTranslation()).thenReturn("경복궁은 널 완전 놀라게 할 거야.");
        when(expression.getRepresentativeSentenceTranslationHighlightText()).thenReturn("널 완전 놀라게 할 거야.");
        when(expression.getRepresentativeImageUrl()).thenReturn("https://cdn.example.com/images/101.png");
        return expression;
    }

    /**
     * "특정 표현을 완료했다"는 기록 1건의 mock을 만든다.
     * 서비스가 완료 여부 판정에 쓰는 getWritingExpressionId()만 스터빙한다.
     */
    private UserWritingExpressionCompletion completion(Long writingExpressionId) {
        UserWritingExpressionCompletion completion = mock(UserWritingExpressionCompletion.class);

        when(completion.getWritingExpressionId()).thenReturn(writingExpressionId);
        return completion;
    }

    // ===== 추가 예문 조회(getExtraPracticeExamples) 테스트 =====

    /** 없는 표현 ID로 추가 예문을 조회하면 RESOURCE_NOT_FOUND 예외를 던지고, 어떤 ID가 없었는지 warn 로그를 남긴다. */
    @Test
    void 없는_표현_ID로_추가_예문을_조회하면_예외를_던지고_로그를_남긴다() {
        // given: 로그를 검증하기 위해 서비스 로거에 ListAppender(로그를 리스트에 담아주는 가짜 출력지)를 부착
        Logger logger = (Logger) LoggerFactory.getLogger(ExpressionQueryService.class);
        ListAppender<ILoggingEvent> logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);

        // given: DB에 해당 표현이 없는 상황
        when(writingExpressionRepository.findByIdAndStatus(EXPRESSION_ID, ActiveStatus.ACTIVE)).thenReturn(Optional.empty());

        // when & then: RESOURCE_NOT_FOUND 예외가 발생한다
        assertThatThrownBy(() -> expressionQueryService.getExtraPracticeExamples(EXPRESSION_ID))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);

        // then: 없는 표현 ID(101)가 포함된 warn 로그가 남는다
        assertThat(logAppender.list)
                .anySatisfy(event -> {
                    assertThat(event.getLevel()).isEqualTo(Level.WARN);
                    assertThat(event.getFormattedMessage()).contains(String.valueOf(EXPRESSION_ID));
                });

        logger.detachAppender(logAppender);
    }

    /** 적절한 표현 ID로 조회하면 표현 정보 + 예문 4개 + 작문 문제(writingSentence)가 담긴 응답을 반환한다. */
    @Test
    void 적절한_표현_ID로_추가_예문을_조회하면_상세_응답을_반환한다() {
        // given: 예문 4개가 payload에 담긴 표현이 DB에 있는 상황
        WritingExpression expression = makeWritingExpressionMockWithInfo(makePracticeExamplesPayload(4));
        when(writingExpressionRepository.findByIdAndStatus(EXPRESSION_ID, ActiveStatus.ACTIVE)).thenReturn(Optional.of(expression));

        // when
        ExpressionPracticeResponse response = expressionQueryService.getExtraPracticeExamples(EXPRESSION_ID);

        // then: 표현 정보가 매핑된다
        assertThat(response.targetExpressionText()).isEqualTo("blow my mind");
        assertThat(response.baseExpressionMeaningText()).isEqualTo("끝내주게 놀랍다");
        assertThat(response.usageDescription()).isEqualTo("강렬한 인상을 받았을 때 최고의 리액션이에요.");

        // then: 예문 4개가 payload에 넣은 순서/값 그대로 매핑된다 (첫 번째 예문으로 전 필드 검증)
        assertThat(response.practiceSentence()).hasSize(4);
        PracticeSentenceResponse first = response.practiceSentence().get(0);
        assertThat(first.sentenceText()).isEqualTo("sentence-0");
        assertThat(first.highlightingPart()).isEqualTo("highlight-0");
        assertThat(first.sentenceTranslation()).isEqualTo("해석-0");
        assertThat(first.practiceQuestion()).isEqualTo("question-0");
        assertThat(first.practiceQuestionTranslation()).isEqualTo("질문해석-0");
        assertThat(first.imageUrl()).isEqualTo("https://cdn.example.com/practice/0.png");

        // then: writingSentence는 예문 4개 중 하나에서 만들어진다 (랜덤이므로 "어느 하나와 일치"로 검증)
        assertThat(response.practiceSentence())
                .anySatisfy(sentence -> {
                    assertThat(response.writingSentence().writingSentenceText()).isEqualTo(sentence.sentenceText());
                    assertThat(response.writingSentence().writingSentenceTranslation()).isEqualTo(sentence.sentenceTranslation());
                    assertThat(response.writingSentence().writingQuestion()).isEqualTo(sentence.practiceQuestion());
                    assertThat(response.writingSentence().writingQuestionTranslation()).isEqualTo(sentence.practiceQuestionTranslation());
                });
    }

    /**
     * 같은 표현 ID를 여러 번 호출하면 writingSentence가 매번 같지 않고 다양한 예문이 뽑히는지 검증한다.
     * 랜덤이라 "항상 다름"은 보장할 수 없으므로, 100회 호출해 2가지 이상 등장하는지 확인한다.
     * (예문 4개 중 100번 모두 같은 게 나올 확률은 (1/4)^99 수준이라 사실상 0)
     */
    @Test
    void 같은_표현을_여러_번_조회하면_writingSentence가_다양하게_뽑힌다() {
        // given
        WritingExpression expression = makeWritingExpressionMockWithInfo(makePracticeExamplesPayload(4));
        when(writingExpressionRepository.findByIdAndStatus(EXPRESSION_ID, ActiveStatus.ACTIVE)).thenReturn(Optional.of(expression));

        // when: 100회 호출하며 뽑힌 작문 문장 텍스트를 Set에 수집 (Set이라 중복은 1개로 합쳐짐)
        Set<String> pickedSentences = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            pickedSentences.add(
                    expressionQueryService.getExtraPracticeExamples(EXPRESSION_ID).writingSentence().writingSentenceText());
        }

        // then: 2가지 이상의 예문이 뽑혔다 = 랜덤 선택이 동작한다
        assertThat(pickedSentences.size()).isGreaterThan(1);
    }

    /**
     * 랜덤 인덱스가 예문 개수에 맞춰 동작하는지 검증한다. (범위: 0 ~ 리스트 길이-1)
     * 지금은 예문이 항상 4개지만 나중에 바뀔 수 있으므로, 4개가 아닌 payload(2개)로도
     * writingSentence가 항상 목록 안의 값인지(=인덱스가 범위를 벗어나지 않는지) 확인한다.
     */
    @Test
    void 예문_개수가_4개가_아니어도_writingSentence는_항상_목록_안에서_뽑힌다() {
        // given: 예문이 2개뿐인 표현
        WritingExpression expression = makeWritingExpressionMockWithInfo(makePracticeExamplesPayload(2));
        when(writingExpressionRepository.findByIdAndStatus(EXPRESSION_ID, ActiveStatus.ACTIVE)).thenReturn(Optional.of(expression));

        // when & then: 50회 호출해도 IndexOutOfBounds 없이, 항상 2개 예문 중 하나가 뽑힌다
        for (int i = 0; i < 50; i++) {
            ExpressionPracticeResponse response = expressionQueryService.getExtraPracticeExamples(EXPRESSION_ID);
            List<String> sentenceTexts = response.practiceSentence().stream()
                    .map(PracticeSentenceResponse::sentenceText)
                    .toList();
            assertThat(sentenceTexts).contains(response.writingSentence().writingSentenceText());
        }
    }

    /** payload가 빈 배열이면(예문 0개) writingSentence를 뽑을 수 없으므로 RESOURCE_NOT_FOUND 예외를 던진다. */
    @Test
    void 예문이_하나도_없는_표현이면_RESOURCE_NOT_FOUND_예외를_던진다() {
        // given: payload가 빈 배열인 표현
        WritingExpression expression = makeWritingExpressionMock(toJson("[]"));
        when(writingExpressionRepository.findByIdAndStatus(EXPRESSION_ID, ActiveStatus.ACTIVE)).thenReturn(Optional.of(expression));

        // when & then
        assertThatThrownBy(() -> expressionQueryService.getExtraPracticeExamples(EXPRESSION_ID))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    /** imageUrl은 명세상 유일한 선택(N) 필드다. payload에 imageUrl이 없어도 파싱이 깨지지 않고 null로 매핑된다. */
    @Test
    void 예문에_imageUrl이_없으면_null로_매핑된다() {
        // given: imageUrl 키가 아예 없는 예문 1개 + 있는 예문 1개
        WritingExpression expression = makeWritingExpressionMockWithInfo(toJson("""
                [
                  {
                    "sentenceText": "no-image sentence",
                    "highlightingPart": "no-image",
                    "sentenceTranslation": "이미지 없음",
                    "practiceQuestion": "question?",
                    "practiceQuestionTranslation": "질문?"
                  },
                  {
                    "sentenceText": "with-image sentence",
                    "highlightingPart": "with-image",
                    "sentenceTranslation": "이미지 있음",
                    "practiceQuestion": "question2?",
                    "practiceQuestionTranslation": "질문2?",
                    "imageUrl": "https://cdn.example.com/practice/1.png"
                  }
                ]
                """));
        when(writingExpressionRepository.findByIdAndStatus(EXPRESSION_ID, ActiveStatus.ACTIVE)).thenReturn(Optional.of(expression));

        // when
        ExpressionPracticeResponse response = expressionQueryService.getExtraPracticeExamples(EXPRESSION_ID);

        // then: 없는 쪽은 null, 있는 쪽은 값 그대로
        assertThat(response.practiceSentence().get(0).imageUrl()).isNull();
        assertThat(response.practiceSentence().get(1).imageUrl()).isEqualTo("https://cdn.example.com/practice/1.png");
    }

    /**
     * 기획자가 시딩한 예문에 필수 키가 빠졌거나 값이 비어 있으면, 그 예문만 응답에서 제외하고 경고 로그를 남긴다.
     * (빈 예문 카드/빈 작문 문제가 사용자에게 노출되는 것을 막고, 로그로 데이터 오류를 추적한다)
     */
    @Test
    void 필수_키가_빠지거나_빈_예문은_목록에서_제외되고_경고_로그를_남긴다() {
        // given: 로그 검증용 ListAppender 부착
        Logger logger = (Logger) LoggerFactory.getLogger(ExpressionQueryService.class);
        ListAppender<ILoggingEvent> logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);

        // given: 정상 2개 + 불량 3개(sentenceText 키 누락 / practiceQuestion 빈 문자열 / sentenceTranslation null)가 섞인 payload
        WritingExpression expression = makeWritingExpressionMockWithInfo(toJson("""
                [
                  {
                    "sentenceText": "valid sentence 1",
                    "highlightingPart": "valid-1",
                    "sentenceTranslation": "정상 예문 1",
                    "practiceQuestion": "question-1?",
                    "practiceQuestionTranslation": "질문 1?"
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
                    "practiceQuestionTranslation": "질문 2?"
                  }
                ]
                """));
        when(writingExpressionRepository.findByIdAndStatus(EXPRESSION_ID, ActiveStatus.ACTIVE)).thenReturn(Optional.of(expression));

        // when
        ExpressionPracticeResponse response = expressionQueryService.getExtraPracticeExamples(EXPRESSION_ID);

        // then: 불량 예문 3개는 제외되고 정상 예문 2개만 남는다
        assertThat(response.practiceSentence()).hasSize(2);
        assertThat(response.practiceSentence())
                .extracting(PracticeSentenceResponse::sentenceText)
                .containsExactly("valid sentence 1", "valid sentence 2");

        // then: 어떤 표현의 예문이 불량인지 warn 로그가 남는다
        assertThat(logAppender.list)
                .anySatisfy(event -> {
                    assertThat(event.getLevel()).isEqualTo(Level.WARN);
                    assertThat(event.getFormattedMessage()).contains(String.valueOf(EXPRESSION_ID));
                });

        logger.detachAppender(logAppender);
    }

    /** 모든 예문이 불량이면(제외 후 0개) 작문 문제를 뽑을 수 없으므로 RESOURCE_NOT_FOUND 예외를 던진다. */
    @Test
    void 모든_예문이_불량이면_RESOURCE_NOT_FOUND_예외를_던진다() {
        // given: 전부 필수 키가 빠진 payload
        WritingExpression expression = makeWritingExpressionMock(toJson("""
                [
                  { "highlightingPart": "only-highlight" },
                  { "sentenceTranslation": "해석만 있음" }
                ]
                """));
        when(writingExpressionRepository.findByIdAndStatus(EXPRESSION_ID, ActiveStatus.ACTIVE)).thenReturn(Optional.of(expression));

        // when & then
        assertThatThrownBy(() -> expressionQueryService.getExtraPracticeExamples(EXPRESSION_ID))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    // ===== 추가 예문 테스트용 헬퍼 =====

    /** payload만 스터빙한 표현 mock. (표현 정보 getter까지 스터빙하면, 호출 안 되는 테스트에서 Mockito가 불필요 스터빙 오류를 내므로 분리) */
    private WritingExpression makeWritingExpressionMock(JsonNode payload) {
        WritingExpression expression = mock(WritingExpression.class);

        when(expression.getPracticeExamplesPayload()).thenReturn(payload);
        return expression;
    }

    /** payload + 응답에 들어갈 표현 정보(타겟/뜻/설명)까지 스터빙한 표현 mock. */
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
            json.append("""
                    {
                      "sentenceText": "sentence-%d",
                      "highlightingPart": "highlight-%d",
                      "sentenceTranslation": "해석-%d",
                      "practiceQuestion": "question-%d",
                      "practiceQuestionTranslation": "질문해석-%d",
                      "imageUrl": "https://cdn.example.com/practice/%d.png"
                    }
                    """.formatted(i, i, i, i, i, i));
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
