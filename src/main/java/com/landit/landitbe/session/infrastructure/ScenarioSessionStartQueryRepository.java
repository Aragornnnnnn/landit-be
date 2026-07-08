// 시나리오 세션 시작에 필요한 콘텐츠와 잠금 정보를 조회한다.
package com.landit.landitbe.session.infrastructure;

import com.landit.landitbe.content.domain.Scenario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScenarioSessionStartQueryRepository extends JpaRepository<Scenario, Long> {

    /** 사용자 언어 설정에 맞는 시나리오 시작 정보를 조회한다. */
    @Query("""
            SELECT new com.landit.landitbe.session.infrastructure.ScenarioSessionStartRow(
                s.id,
                s.categoryId,
                c.status,
                s.status,
                slv.id,
                slv.status,
                s.firstSpeaker,
                s.totalQuestionCount,
                slv.userOpeningInstruction,
                slv.aiOpeningMessage,
                slv.aiOpeningMessageTranslation,
                slv.aiOpeningInnerThought,
                slv.aiOpeningInnerThoughtType,
                s.ttsVoiceSetId
            )
            FROM UserProfile up
            JOIN Scenario s
              ON s.id = :scenarioId
            JOIN Category c
              ON c.id = s.categoryId
            JOIN ScenarioLanguageVariant slv
              ON slv.scenarioId = s.id
             AND slv.targetLocale = up.targetLocale
             AND slv.baseLocale = up.baseLocale
            WHERE up.id = :userId
            """)
    Optional<ScenarioSessionStartRow> findStartRow(
            @Param("userId") long userId,
            @Param("scenarioId") long scenarioId
    );

    /** 같은 카테고리 안의 사용자별 완료 상태를 displayOrder 순서로 조회한다. */
    @Query("""
            SELECT new com.landit.landitbe.session.infrastructure.ScenarioSessionLockRow(
                s.id,
                usp.status
            )
            FROM UserProfile up
            JOIN Scenario s
              ON s.categoryId = :categoryId
            JOIN ScenarioLanguageVariant slv
              ON slv.scenarioId = s.id
             AND slv.targetLocale = up.targetLocale
             AND slv.baseLocale = up.baseLocale
            LEFT JOIN UserScenarioProgress usp
              ON usp.userProfileId = up.id
             AND usp.scenarioId = s.id
             AND usp.targetLocale = up.targetLocale
            WHERE up.id = :userId
            ORDER BY s.displayOrder ASC
            """)
    List<ScenarioSessionLockRow> findCategoryLockRows(
            @Param("userId") long userId,
            @Param("categoryId") long categoryId
    );
}
