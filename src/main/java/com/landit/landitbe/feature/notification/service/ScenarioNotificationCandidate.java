// 사용자별 시나리오 이어 하기 후보의 현재 접근 가능 상태를 전달한다.

package com.landit.landitbe.feature.notification.service;

record ScenarioNotificationCandidate(
    Long userProfileId,
    Long categoryId,
    boolean categoryActive,
    Long scenarioId,
    int scenarioDisplayOrder,
    boolean scenarioActive,
    boolean variantActive,
    boolean cleared) {}
