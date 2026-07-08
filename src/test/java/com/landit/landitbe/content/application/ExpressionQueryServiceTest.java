// ExpressionQueryService의 완료 여부와 잠김 여부 계산 로직을 단위 검증한다.
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
import com.landit.landitbe.content.api.dto.ExpressionResponse;
import com.landit.landitbe.content.domain.UserWritingExpressionCompletion;
import com.landit.landitbe.content.domain.WritingExpression;
import com.landit.landitbe.content.infrastructure.UserWritingExpressionCompletionRepository;
import com.landit.landitbe.content.infrastructure.WritingExpressionRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExpressionQueryServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long SCENARIO_ID = 999L;

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

    private UserWritingExpressionCompletion completion(Long writingExpressionId) {
        UserWritingExpressionCompletion completion = mock(UserWritingExpressionCompletion.class);
        when(completion.getWritingExpressionId()).thenReturn(writingExpressionId);
        return completion;
    }
}
