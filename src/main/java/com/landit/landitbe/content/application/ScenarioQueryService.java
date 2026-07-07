// 시나리오 목록 조회 결과를 사용자별 응답 형태로 조립한다.
package com.landit.landitbe.content.application;

import com.landit.landitbe.common.domain.ActiveStatus;
import com.landit.landitbe.common.domain.ConversationSpeaker;
import com.landit.landitbe.content.api.dto.ScenarioListResponse;
import com.landit.landitbe.content.api.dto.ScenarioListResponse.CategoryResponse;
import com.landit.landitbe.content.api.dto.ScenarioListResponse.OpeningPreviewResponse;
import com.landit.landitbe.content.api.dto.ScenarioListResponse.ScenarioResponse;
import com.landit.landitbe.content.infrastructure.ScenarioListQueryRepository;
import com.landit.landitbe.content.infrastructure.ScenarioListRow;
import com.landit.landitbe.learning.domain.UserScenarioProgressStatus;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class ScenarioQueryService {

    private static final String CATEGORY_LOCK_REASON = "현재 사용할 수 없는 카테고리입니다.";
    private static final String SCENARIO_LOCK_REASON = "현재 사용할 수 없는 시나리오입니다.";

    private final ScenarioListQueryRepository scenarioListQueryRepository;

    /** 인증된 사용자의 시나리오 목록 응답을 조회한다. */
    @Transactional(readOnly = true)
    public ScenarioListResponse getScenarioList(long userId) {
        List<ScenarioListRow> rows = scenarioListQueryRepository.findScenarioList(userId);
        return new ScenarioListResponse(categoryGroups(rows).stream()
                .map(CategoryGroup::toResponse)
                .toList());
    }

    /** 평탄한 조회 결과를 응답 구조에 맞게 카테고리 단위로 묶는다. */
    private List<CategoryGroup> categoryGroups(List<ScenarioListRow> rows) {
        Map<Long, CategoryGroup> categories = new LinkedHashMap<>();
        for (ScenarioListRow row : rows) {
            CategoryGroup category = categories.computeIfAbsent(
                    row.categoryId(),
                    ignored -> new CategoryGroup(row)
            );
            category.add(toScenarioResponse(row, category.categoryLocked()));
        }
        return categories.values().stream().toList();
    }

    private ScenarioResponse toScenarioResponse(ScenarioListRow row, boolean categoryLocked) {
        boolean completed = row.progressStatus() == UserScenarioProgressStatus.CLEARED;
        // 카테고리가 잠기면 하위 시나리오도 함께 잠긴 것으로 응답한다.
        boolean scenarioLocked = inactive(row.scenarioStatus()) || inactive(row.variantStatus());
        boolean locked = categoryLocked || scenarioLocked;
        return new ScenarioResponse(
                row.scenarioId(),
                starRating(completed, row.bestStarRating()),
                row.scenarioDisplayOrder(),
                row.scenarioTitle(),
                row.briefing(),
                row.conversationGoal(),
                row.difficulty().name(),
                row.firstSpeaker().name(),
                row.thumbnailUrl(),
                completed,
                locked,
                lockReason(categoryLocked, scenarioLocked),
                openingPreview(row, locked)
        );
    }

    private String lockReason(boolean categoryLocked, boolean scenarioLocked) {
        if (categoryLocked) {
            return CATEGORY_LOCK_REASON;
        }
        if (scenarioLocked) {
            return SCENARIO_LOCK_REASON;
        }
        return null;
    }

    private BigDecimal starRating(boolean completed, Integer bestStarRating) {
        if (!completed || bestStarRating == null) {
            return null;
        }
        return BigDecimal.valueOf(bestStarRating + 1L).divide(BigDecimal.valueOf(2L));
    }

    private OpeningPreviewResponse openingPreview(ScenarioListRow row, boolean locked) {
        if (locked) {
            return null;
        }
        // 첫 발화자가 AI인 경우에만 AI 시작 메시지와 속마음을 미리보기로 내려준다.
        if (row.firstSpeaker() == ConversationSpeaker.AI) {
            return new OpeningPreviewResponse(
                    row.aiOpeningMessage(),
                    row.aiOpeningMessageTranslation(),
                    null,
                    row.innerThought(),
                    row.innerThoughtType() == null ? null : row.innerThoughtType().name(),
                    row.ttsVoiceSetId()
            );
        }
        return new OpeningPreviewResponse(
                null,
                null,
                row.userOpeningInstruction(),
                null,
                null,
                row.ttsVoiceSetId()
        );
    }

    private static boolean inactive(ActiveStatus status) {
        return status != ActiveStatus.ACTIVE;
    }

    private record CategoryGroup(
            Long categoryId,
            String categoryName,
            int displayOrder,
            boolean categoryLocked,
            String categoryLockReason,
            List<ScenarioResponse> scenarios
    ) {

        /** 카테고리 메타데이터는 같은 카테고리의 첫 row에서 가져오고, 시나리오는 이후에 누적한다. */
        private CategoryGroup(ScenarioListRow row) {
            this(
                    row.categoryId(),
                    row.categoryName(),
                    row.categoryDisplayOrder(),
                    inactive(row.categoryStatus()),
                    inactive(row.categoryStatus()) ? CATEGORY_LOCK_REASON : null,
                    new ArrayList<>()
            );
        }

        private void add(ScenarioResponse scenario) {
            scenarios.add(scenario);
        }

        private CategoryResponse toResponse() {
            return new CategoryResponse(
                    categoryId,
                    categoryName,
                    displayOrder,
                    categoryLocked,
                    categoryLockReason,
                    List.copyOf(scenarios)
            );
        }
    }
}
