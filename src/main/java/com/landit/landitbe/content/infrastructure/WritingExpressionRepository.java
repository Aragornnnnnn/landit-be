// 시나리오에 속한 Writing 표현을 학습 순서 기준으로 조회한다.
package com.landit.landitbe.content.infrastructure;

import com.landit.landitbe.common.domain.ActiveStatus;
import com.landit.landitbe.content.domain.WritingExpression;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.landit.landitbe.common.domain.Locale;

public interface WritingExpressionRepository extends JpaRepository<WritingExpression, Long> {

    /**
     * 특정 시나리오에서 지정한 locale 조합의 활성 Writing 표현을 표시 순서 오름차순으로 조회한다.
     * (displayOrder 시퀀스는 locale 조합별로 존재하므로, locale 필터 없이는 여러 언어 표현이 섞이고 해금 순서가 깨진다)
     */
    List<WritingExpression> findByScenarioIdAndTargetLocaleAndBaseLocaleAndStatusOrderByDisplayOrderAsc(
            Long scenarioId,
            Locale targetLocale,
            Locale baseLocale,
            ActiveStatus status
    );

    /** 특정 상태의 Writing 표현을 PK로 조회한다. (INACTIVE로 내려간 콘텐츠를 걸러내는 단건 조회용) */
    Optional<WritingExpression> findByIdAndStatus(Long id, ActiveStatus status);
}
