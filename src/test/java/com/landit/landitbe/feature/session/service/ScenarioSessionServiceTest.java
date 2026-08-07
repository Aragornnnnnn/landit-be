// ScenarioSessionService의 조회 실패 변환과 저장 위임을 단위 테스트한다.

package com.landit.landitbe.feature.session.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.session.repository.ScenarioSessionMessageQueryRepository;
import com.landit.landitbe.feature.session.repository.ScenarioSessionRepository;
import com.landit.landitbe.feature.session.repository.ScenarioSessionStartQueryRepository;
import com.landit.landitbe.feature.session.repository.projection.ScenarioSessionStartProjection;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** ScenarioSessionService의 조회 실패 변환과 저장 위임을 단위 테스트한다. */
class ScenarioSessionServiceTest {

  private final ScenarioSessionRepository scenarioSessionRepository =
      mock(ScenarioSessionRepository.class);
  private final ScenarioSessionStartQueryRepository startQueryRepository =
      mock(ScenarioSessionStartQueryRepository.class);
  private final ScenarioSessionMessageQueryRepository messageQueryRepository =
      mock(ScenarioSessionMessageQueryRepository.class);
  private final ScenarioSessionService service =
      new ScenarioSessionService(
          scenarioSessionRepository, startQueryRepository, messageQueryRepository);

  /** 사용자 언어에 맞는 시작 Projection을 반환한다. */
  @Test
  void returnsStartProjection() {
    ScenarioSessionStartProjection projection = mock(ScenarioSessionStartProjection.class);
    when(startQueryRepository.findStartRow(1L, 2L)).thenReturn(Optional.of(projection));

    assertThat(service.requireStartProjection(1L, 2L)).isSameAs(projection);
  }

  /** 시작 Projection이 없으면 시나리오 없음 오류로 변환한다. */
  @Test
  void rejectsMissingStartProjection() {
    when(startQueryRepository.findStartRow(1L, 2L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.requireStartProjection(1L, 2L))
        .isInstanceOf(ApiException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.SCENARIO_NOT_FOUND);
  }
}
