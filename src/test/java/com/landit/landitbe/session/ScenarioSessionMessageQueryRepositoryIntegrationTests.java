// 사용자 발화 제출용 시나리오 컨텍스트 조회의 DTO 생성자 매핑을 검증한다.
package com.landit.landitbe.session;

import static org.assertj.core.api.Assertions.assertThat;

import com.landit.landitbe.session.infrastructure.ScenarioSessionMessageQueryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class ScenarioSessionMessageQueryRepositoryIntegrationTests {

    @Autowired
    private ScenarioSessionMessageQueryRepository scenarioSessionMessageQueryRepository;

    @Test
    void returnsEmptyWhenLearningSessionDoesNotExist() {
        assertThat(scenarioSessionMessageQueryRepository.findContextByLearningSessionId(999999L)).isEmpty();
    }
}
