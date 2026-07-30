// 프리톡 메인 조회의 주제와 남은 발화 시간을 검증한다.

package com.landit.landitbe.feature.session.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.session.domain.FreeTalkTopic;
import com.landit.landitbe.feature.session.dto.FreeTalkMainResponse;
import com.landit.landitbe.feature.session.repository.FreeTalkTopicRepository;
import com.landit.landitbe.shared.domain.ActiveStatus;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 프리톡 메인 조회의 주제와 남은 발화 시간을 검증한다. */
class FreeTalkTopicServiceTest {

  private final FreeTalkTopicRepository topicRepository = mock(FreeTalkTopicRepository.class);
  private final FreeTalkDailySpeakingUsageService dailySpeakingUsageService =
      mock(FreeTalkDailySpeakingUsageService.class);
  private final FreeTalkTopicService service =
      new FreeTalkTopicService(topicRepository, dailySpeakingUsageService);

  /** 메인 조회는 활성 주제와 하루 한도, 현재 남은 시간을 함께 반환한다. */
  @Test
  void returnsTopicsWithRemainingDailySpeakingTime() {
    FreeTalkTopic topic = mock(FreeTalkTopic.class);
    when(topic.getId()).thenReturn(1L);
    when(topic.getDisplayName()).thenReturn("오늘 하루 얘기");
    when(topic.getDisplayOrder()).thenReturn(1);
    when(topicRepository.findAllByStatusOrderByDisplayOrderAsc(ActiveStatus.ACTIVE))
        .thenReturn(List.of(topic));
    when(dailySpeakingUsageService.remainingMs(1L)).thenReturn(18_000L);

    FreeTalkMainResponse response = service.getMain(1L);

    assertThat(response.dailySpeakingTimeLimitMs()).isEqualTo(60_000L);
    assertThat(response.remainingSpeakingTimeMs()).isEqualTo(18_000L);
    assertThat(response.topics()).hasSize(1);
  }
}
