// 시나리오별 Writing 표현 목록을 사용자 완료 여부와 함께 조회한다.
package com.landit.landitbe.content.application;

import com.landit.landitbe.common.domain.ActiveStatus;
import com.landit.landitbe.content.api.dto.ExpressionResponse;
import com.landit.landitbe.content.domain.UserWritingExpressionCompletion;
import com.landit.landitbe.content.domain.WritingExpression;
import com.landit.landitbe.content.infrastructure.UserWritingExpressionCompletionRepository;
import com.landit.landitbe.content.infrastructure.WritingExpressionRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class ExpressionQueryService {

    private final ScenarioService scenarioService;
    private final WritingExpressionRepository writingExpressionRepository;
    private final UserWritingExpressionCompletionRepository userWritingExpressionCompletionRepository;

    /** 시나리오별 Writing 표현 목록을 학습 순서대로 조회하고 사용자 완료 여부를 반영한다. */
    @Transactional(readOnly = true)
    public List<ExpressionResponse> getExpressionsPerScenario(Long userId, Long scenarioId) {
        scenarioService.validateExists(scenarioId);

        List<WritingExpression> expressions = writingExpressionRepository
                .findByScenarioIdAndStatusOrderByDisplayOrderAsc(scenarioId, ActiveStatus.ACTIVE);

        // 해당 유저가 클리어한 Writing 표현의 ID를 Set으로 수집한다.
        Set<Long> completedExpressionIds = userWritingExpressionCompletionRepository
                .findAllByUserProfileIdAndScenarioId(userId, scenarioId)
                .stream()
                .map(UserWritingExpressionCompletion::getWritingExpressionId)
                .collect(Collectors.toSet());

        // 미완료 표현 중 학습 순서가 가장 앞선 하나만 해금되고 그 뒤로는 잠긴다. (리스트는 displayOrder 오름차순)
        Optional<Long> firstUnlockedExpressionId = firstIncompleteExpressionId(expressions, completedExpressionIds);

        return expressions.stream()
                .map(expression -> toResponse(expression, completedExpressionIds, firstUnlockedExpressionId))
                .toList();
    }

    /** 미완료 표현 중 학습 순서가 가장 앞선 표현의 ID를 반환한다. 모두 완료했으면 빈 값을 반환한다. */
    private Optional<Long> firstIncompleteExpressionId(
            List<WritingExpression> expressions,
            Set<Long> completedExpressionIds
    ) {
        return expressions.stream()
                .map(WritingExpression::getId)
                .filter(expressionId -> !completedExpressionIds.contains(expressionId))
                .findFirst();
    }

    /** Writing 표현을 완료 여부와 잠김 여부를 계산한 응답으로 변환한다. */
    private ExpressionResponse toResponse(
            WritingExpression expression,
            Set<Long> completedExpressionIds,
            Optional<Long> firstUnlockedExpressionId
    ) {
        // 미완료 표현 중 학습 순서가 가장 앞선(=지금 학습할 차례인) 표현인지 확인한다.
        // firstUnlockedExpressionId가 비어 있으면(모두 완료) 해금 대상 표현이 없으므로 false다.
        boolean isFirstUnlockedExpression = firstUnlockedExpressionId.isPresent()
                && firstUnlockedExpressionId.get().equals(expression.getId());

        // 완료했거나 지금 학습할 차례인 표현만 잠기지 않고, 나머지 미완료 표현은 잠긴다.
        boolean completed = completedExpressionIds.contains(expression.getId());
        boolean locked = !completed && !isFirstUnlockedExpression;

        return new ExpressionResponse(
                expression.getId(),
                expression.getDisplayOrder(),
                expression.getTargetExpressionText(),
                expression.getBaseExpressionMeaningText(),
                completed,
                locked
        );
    }
}
