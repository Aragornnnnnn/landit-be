// 사용자 발화 제출에 필요한 시나리오 컨텍스트 조회 결과를 담는다.
package com.landit.landitbe.session.infrastructure;

public record ScenarioSessionMessageContextRow(
        Long scenarioId,
        String title,
        String briefing,
        String conversationGoal,
        String counterpartRole,
        int totalQuestionCount,
        String targetLocale,
        String baseLocale
) {
}
