// ScenarioContentService가 Repository Projection을 공개 record로 변환하는지 검증한다.

package com.landit.landitbe.feature.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.content.dto.NextQuestionContext;
import com.landit.landitbe.feature.content.repository.ScenarioQuestionQueryRepository;
import com.landit.landitbe.feature.content.repository.projection.ScenarioQuestionProjection;
import com.landit.landitbe.shared.domain.Locale;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** ScenarioContentService의 기능 간 질문 조회 계약을 단위 검증한다. */
class ScenarioContentServiceTest {

  private final ScenarioQuestionQueryRepository repository =
      mock(ScenarioQuestionQueryRepository.class);
  private final ScenarioContentService service = new ScenarioContentService(repository);

  /** Repository Projection을 Session이 사용할 공개 record로 변환한다. */
  @Test
  void returnsNextQuestionContext() {
    ScenarioQuestionProjection projection =
        new ScenarioQuestionProjection(10L, 2, "question", "translation");
    when(repository.findActiveQuestion(1L, 2, Locale.EN, Locale.KR))
        .thenReturn(Optional.of(projection));

    assertThat(service.findActiveQuestion(1L, 2, Locale.EN, Locale.KR))
        .contains(new NextQuestionContext(10L, 2, "question", "translation"));
  }
}
