// 사용자 발화 제출용 시나리오 컨텍스트 조회의 DTO 생성자 매핑을 검증한다.

package com.landit.landitbe.feature.learning.scenario.session;

import static org.assertj.core.api.Assertions.assertThat;

import com.landit.landitbe.feature.learning.scenario.session.repository.ScenarioSessionMessageQueryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** 사용자 발화 제출용 시나리오 컨텍스트 조회의 DTO 생성자 매핑을 검증한다. */
@ActiveProfiles("test")
@SpringBootTest
@Transactional
class ScenarioSessionMessageQueryRepositoryIntegrationTests {

  @Autowired private ScenarioSessionMessageQueryRepository scenarioSessionMessageQueryRepository;

  @DisplayName("학습 세션이 없으면 시나리오 메시지 문맥을 빈 결과로 반환한다.")
  @Test
  void returnsEmptyWhenLearningSessionDoesNotExist() {
    assertThat(scenarioSessionMessageQueryRepository.findContextByLearningSessionId(999999L))
        .isEmpty();
  }
}
