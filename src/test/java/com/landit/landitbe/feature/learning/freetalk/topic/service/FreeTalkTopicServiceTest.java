// 프리톡 메인 조회의 주제와 남은 발화 시간을 검증한다.

package com.landit.landitbe.feature.learning.freetalk.topic.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.learning.freetalk.topic.domain.FreeTalkTopic;
import com.landit.landitbe.feature.learning.freetalk.topic.dto.FreeTalkMainResponse;
import com.landit.landitbe.feature.learning.freetalk.topic.repository.FreeTalkTopicRepository;
import com.landit.landitbe.feature.learning.freetalk.usage.dto.DailySpeakingUsage;
import com.landit.landitbe.feature.learning.freetalk.usage.service.FreeTalkDailySpeakingUsageService;
import com.landit.landitbe.shared.domain.ActiveStatus;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 프리톡 메인 조회의 주제와 남은 발화 시간을 검증한다. */
class FreeTalkTopicServiceTest {

  private final FreeTalkTopicRepository topicRepository = mock(FreeTalkTopicRepository.class);
  private final FreeTalkDailySpeakingUsageService dailySpeakingUsageService =
      mock(FreeTalkDailySpeakingUsageService.class);
  private final FreeTalkTopicService service =
      new FreeTalkTopicService(topicRepository, dailySpeakingUsageService);

  /** 메인 조회는 활성 주제와 하루 한도, 현재 남은 시간을 함께 반환한다. */
  @DisplayName("메인 조회는 활성 주제와 하루 한도, 현재 남은 시간을 함께 반환한다.")
  @Test
  void returnsTopicsWithRemainingDailySpeakingTime() {
    FreeTalkTopic topic = mock(FreeTalkTopic.class);
    when(topic.getId()).thenReturn(1L);
    when(topic.getDisplayName()).thenReturn("오늘 하루 얘기");
    when(topic.getDisplayOrder()).thenReturn(1);
    when(topicRepository.findAllByStatusOrderByDisplayOrderAsc(ActiveStatus.ACTIVE))
        .thenReturn(List.of(topic));
    when(dailySpeakingUsageService.usage(1L))
        .thenReturn(new DailySpeakingUsage(java.time.LocalDate.now(), 42_000L, 18_000L));
    when(dailySpeakingUsageService.speakingTimeLimitMs()).thenReturn(9_999_999L);

    FreeTalkMainResponse response = service.getMain(1L);

    assertThat(response.dailySpeakingTimeLimitMs()).isEqualTo(9_999_999L);
    assertThat(response.usedSpeakingTimeMs()).isEqualTo(42_000L);
    assertThat(response.remainingSpeakingTimeMs()).isEqualTo(18_000L);
    assertThat(response.canStart()).isTrue();
    assertThat(response.topics()).hasSize(1);
  }

  /** 남은 발화 시간이 없으면 메인 화면에서 세션 시작을 막는다. */
  @DisplayName("남은 발화 시간이 없으면 메인 화면에서 세션 시작을 막는다.")
  @Test
  void cannotStartWhenDailySpeakingTimeIsUsed() {
    when(topicRepository.findAllByStatusOrderByDisplayOrderAsc(ActiveStatus.ACTIVE))
        .thenReturn(List.of());
    when(dailySpeakingUsageService.usage(1L))
        .thenReturn(new DailySpeakingUsage(java.time.LocalDate.now(), 60_000L, 0L));

    FreeTalkMainResponse response = service.getMain(1L);

    assertThat(response.canStart()).isFalse();
  }
}
