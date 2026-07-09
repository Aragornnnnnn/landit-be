// 시나리오에 속한 Writing 표현을 학습 순서 기준으로 조회한다.
package com.landit.landitbe.content.infrastructure;

import com.landit.landitbe.common.domain.ActiveStatus;
import com.landit.landitbe.content.domain.WritingExpression;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WritingExpressionRepository extends JpaRepository<WritingExpression, Long> {

    /** 특정 시나리오의 활성 상태 Writing 표현을 표시 순서 오름차순으로 조회한다. */
    List<WritingExpression> findByScenarioIdAndStatusOrderByDisplayOrderAsc(Long scenarioId, ActiveStatus status);
}
