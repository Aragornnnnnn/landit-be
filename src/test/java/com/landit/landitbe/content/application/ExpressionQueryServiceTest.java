// ExpressionQueryService의 완료/잠김 계산과 학습 시작 상세 조회를 단위 검증한다.
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

import com.landit.landitbe.common.domain.ActiveStatus;
import com.landit.landitbe.common.exception.ApiException;
import com.landit.landitbe.common.exception.ErrorCode;
import com.landit.landitbe.content.api.dto.ExpressionLearningResponse;
import com.landit.landitbe.content.api.dto.ExpressionResponse;
import com.landit.landitbe.content.domain.UserWritingExpressionCompletion;
import com.landit.landitbe.content.domain.WritingExpression;
import com.landit.landitbe.content.infrastructure.UserWritingExpressionCompletionRepository;
import com.landit.landitbe.content.infrastructure.WritingExpressionRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExpressionQueryServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long SCENARIO_ID = 999L;
    private static final Long EXPRESSION_ID = 101L;

    @Mock
    private ScenarioService scenarioService;

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
                .findByScenarioIdAndStatusOrderByDisplayOrderAsc(any(), any());
    }

    @Test
    void 표현을_찾으면_학습_시작_상세_정보를_응답으로_반환한다() {
        // given: DB에 학습하려는 표현 데이터가 있는 상황 가정
        // (learningExpression() 내부의 getter 스터빙이 findById 스터빙과 중첩되지 않도록 mock을 먼저 만든다)
        WritingExpression expression = learningExpression();
        when(writingExpressionRepository.findById(EXPRESSION_ID))
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
        when(writingExpressionRepository.findById(EXPRESSION_ID))
                .thenReturn(Optional.empty());

        // when & then : 존재않는 표현 id로 getExpressionForLearning()를 호출하면 ApiException이 발생하고, errorCode가 RESOURCE_NOT_FOUND인지 검증
        assertThatThrownBy(() -> expressionQueryService.getExpressionForLearning(EXPRESSION_ID))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    private void givenExpressions(WritingExpression... expressions) {
        when(writingExpressionRepository
                .findByScenarioIdAndStatusOrderByDisplayOrderAsc(eq(SCENARIO_ID), eq(ActiveStatus.ACTIVE)))
                .thenReturn(List.of(expressions));
    }

    private void givenCompletedExpressionIds(Long... completedExpressionIds) {
        List<UserWritingExpressionCompletion> completions = java.util.Arrays.stream(completedExpressionIds)
                .map(this::completion)
                .toList();
        when(userWritingExpressionCompletionRepository
                .findAllByUserProfileIdAndScenarioId(USER_ID, SCENARIO_ID))
                .thenReturn(completions);
    }

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

    private UserWritingExpressionCompletion completion(Long writingExpressionId) {
        UserWritingExpressionCompletion completion = mock(UserWritingExpressionCompletion.class);
        when(completion.getWritingExpressionId()).thenReturn(writingExpressionId);
        return completion;
    }
}
