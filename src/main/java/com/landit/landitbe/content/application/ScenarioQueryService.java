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
    private static final String PREVIOUS_SCENARIO_NOT_COMPLETED = "PREVIOUS_SCENARIO_NOT_COMPLETED";

    private final ScenarioListQueryRepository scenarioListQueryRepository;

    /** 인증된 사용자의 시나리오 목록 응답을 조회한다. */
    @Transactional(readOnly = true)
    public ScenarioListResponse getScenarioList(long userId) {
        List<ScenarioListRow> scenarioRows = scenarioListQueryRepository.findScenarioList(userId);
        return new ScenarioListResponse(categoryGroups(scenarioRows).stream()
                .map(CategoryGroup::toResponse)
                .toList());
    }

    /** 평탄한 조회 결과를 응답 구조에 맞게 카테고리 단위로 묶는다. */
    private List<CategoryGroup> categoryGroups(List<ScenarioListRow> scenarioRows) {
        Map<Long, CategoryGroup> categoryGroupsById = new LinkedHashMap<>();
        for (ScenarioListRow scenarioRow : scenarioRows) {
            CategoryGroup categoryGroup = categoryGroupsById.computeIfAbsent(
                    scenarioRow.categoryId(),
                    ignored -> new CategoryGroup(scenarioRow)
            );
            categoryGroup.addScenarioRow(scenarioRow);
        }
        return categoryGroupsById.values().stream().toList();
    }

    private static ScenarioResponse toScenarioResponse(
            ScenarioListRow scenarioRow,
            boolean categoryLocked,
            boolean previousScenarioCompleted
    ) {
        boolean completed = scenarioRow.progressStatus() == UserScenarioProgressStatus.CLEARED;
        boolean scenarioOrVariantInactive = inactive(scenarioRow.scenarioStatus())
                || inactive(scenarioRow.variantStatus());
        boolean scenarioLocked = scenarioOrVariantInactive || !previousScenarioCompleted;
        boolean locked = categoryLocked || scenarioLocked;
        return new ScenarioResponse(
                scenarioRow.scenarioId(),
                completedStarRating(completed, scenarioRow.bestStarRating()),
                scenarioRow.scenarioDisplayOrder(),
                scenarioRow.scenarioTitle(),
                scenarioRow.briefing(),
                scenarioRow.conversationGoal(),
                scenarioRow.difficulty().name(),
                scenarioRow.firstSpeaker().name(),
                scenarioRow.thumbnailUrl(),
                completed,
                locked,
                lockReason(categoryLocked, scenarioOrVariantInactive, previousScenarioCompleted),
                openingPreview(scenarioRow, locked)
        );
    }

    private static String lockReason(
            boolean categoryLocked,
            boolean scenarioInactive,
            boolean previousScenarioCompleted
    ) {
        if (categoryLocked) {
            return CATEGORY_LOCK_REASON;
        }
        if (scenarioInactive) {
            return SCENARIO_LOCK_REASON;
        }
        if (!previousScenarioCompleted) {
            return PREVIOUS_SCENARIO_NOT_COMPLETED;
        }
        return null;
    }

    private static BigDecimal completedStarRating(boolean completed, BigDecimal bestStarRating) {
        if (!completed || bestStarRating == null) {
            return null;
        }
        return bestStarRating;
    }

    private static OpeningPreviewResponse openingPreview(ScenarioListRow scenarioRow, boolean locked) {
        if (locked) {
            return null;
        }
        // 첫 발화자가 AI인 경우에만 AI 시작 메시지와 속마음을 미리보기로 내려준다.
        if (scenarioRow.firstSpeaker() == ConversationSpeaker.AI) {
            return new OpeningPreviewResponse(
                    scenarioRow.aiOpeningMessage(),
                    scenarioRow.aiOpeningMessageTranslation(),
                    null,
                    scenarioRow.innerThought(),
                    scenarioRow.innerThoughtType() == null ? null : scenarioRow.innerThoughtType().name(),
                    scenarioRow.ttsVoiceSetId()
            );
        }
        return new OpeningPreviewResponse(
                null,
                null,
                scenarioRow.userOpeningInstruction(),
                null,
                null,
                scenarioRow.ttsVoiceSetId()
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
            List<ScenarioListRow> scenarioRows
    ) {

        /** 카테고리 메타데이터는 같은 카테고리의 첫 조회 결과에서 가져오고, 시나리오는 이후에 누적한다. */
        private CategoryGroup(ScenarioListRow firstScenarioRow) {
            this(
                    firstScenarioRow.categoryId(),
                    firstScenarioRow.categoryName(),
                    firstScenarioRow.categoryDisplayOrder(),
                    inactive(firstScenarioRow.categoryStatus()),
                    inactive(firstScenarioRow.categoryStatus()) ? CATEGORY_LOCK_REASON : null,
                    new ArrayList<>()
            );
        }

        private void addScenarioRow(ScenarioListRow scenarioRow) {
            scenarioRows.add(scenarioRow);
        }

        private CategoryResponse toResponse() {
            List<ScenarioResponse> scenarios = new ArrayList<>();
            boolean previousScenarioCompleted = true;
            for (ScenarioListRow scenarioRow : scenarioRows) {
                ScenarioResponse scenario = toScenarioResponse(
                        scenarioRow,
                        categoryLocked,
                        previousScenarioCompleted
                );
                scenarios.add(scenario);
                previousScenarioCompleted = scenario.completed();
            }
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
