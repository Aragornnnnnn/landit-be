// 프리톡 메인 조회의 무작위 주제 선택과 남은 발화 시간을 검증한다.

package com.landit.landitbe.feature.learning.freetalk.topic.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.learning.freetalk.topic.domain.FreeTalkTopic;
import com.landit.landitbe.feature.learning.freetalk.topic.dto.FreeTalkMainResponse;
import com.landit.landitbe.feature.learning.freetalk.topic.dto.FreeTalkTopicResponse;
import com.landit.landitbe.feature.learning.freetalk.topic.repository.FreeTalkTopicRepository;
import com.landit.landitbe.feature.learning.freetalk.usage.dto.DailySpeakingUsage;
import com.landit.landitbe.feature.learning.freetalk.usage.service.FreeTalkDailySpeakingUsageService;
import com.landit.landitbe.shared.domain.ActiveStatus;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 프리톡 메인 조회의 무작위 주제 선택과 남은 발화 시간을 검증한다. */
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
    List<FreeTalkTopic> topics = List.of(topic(1L, "오늘 하루 얘기"));
    when(topicRepository.findAllByStatusOrderByDisplayOrderAsc(ActiveStatus.ACTIVE))
        .thenReturn(topics);
    when(dailySpeakingUsageService.usage(1L))
        .thenReturn(new DailySpeakingUsage(java.time.LocalDate.now(), 42_000L, 18_000L));
    when(dailySpeakingUsageService.speakingTimeLimitMs()).thenReturn(9_999_999L);

    FreeTalkMainResponse response = service.getMain(1L);

    assertThat(response.dailySpeakingTimeLimitMs()).isEqualTo(9_999_999L);
    assertThat(response.usedSpeakingTimeMs()).isEqualTo(42_000L);
    assertThat(response.remainingSpeakingTimeMs()).isEqualTo(18_000L);
    assertThat(response.canStart()).isTrue();
    assertThat(response.topics()).hasSize(1);
    assertThat(response.topics().get(0).displayOrder()).isEqualTo(1);
  }

  /** 활성 주제가 5개를 넘으면 중복 없이 5개만 뽑고 노출 순서를 1~5로 다시 매긴다. */
  @Test
  void picksAtMostFiveTopicsWithSequentialDisplayOrder() {
    List<FreeTalkTopic> topics =
        IntStream.rangeClosed(1, 7).mapToObj(id -> topic((long) id, "주제 " + id)).toList();
    when(topicRepository.findAllByStatusOrderByDisplayOrderAsc(ActiveStatus.ACTIVE))
        .thenReturn(topics);

    List<FreeTalkTopicResponse> picked = service.getActiveTopics();

    assertThat(picked).hasSize(FreeTalkTopicService.MAIN_TOPIC_COUNT);
    assertThat(picked.stream().map(FreeTalkTopicResponse::topicId))
        .doesNotHaveDuplicates()
        .allMatch(id -> id >= 1L && id <= 7L);
    assertThat(picked.stream().map(FreeTalkTopicResponse::displayOrder))
        .containsExactly(1, 2, 3, 4, 5);
  }

  /** 활성 주제가 5개보다 적으면 있는 만큼 전부 반환하고 순서를 1부터 매긴다. */
  @Test
  void returnsAllTopicsWhenFewerThanFive() {
    List<FreeTalkTopic> topics = List.of(topic(1L, "a"), topic(2L, "b"), topic(3L, "c"));
    when(topicRepository.findAllByStatusOrderByDisplayOrderAsc(ActiveStatus.ACTIVE))
        .thenReturn(topics);

    List<FreeTalkTopicResponse> picked = service.getActiveTopics();

    assertThat(picked.stream().map(FreeTalkTopicResponse::topicId))
        .containsExactlyInAnyOrder(1L, 2L, 3L);
    assertThat(picked.stream().map(FreeTalkTopicResponse::displayOrder)).containsExactly(1, 2, 3);
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
    assertThat(response.topics()).isEmpty();
  }

  private static FreeTalkTopic topic(Long id, String displayName) {
    FreeTalkTopic topic = mock(FreeTalkTopic.class);
    when(topic.getId()).thenReturn(id);
    when(topic.getDisplayName()).thenReturn(displayName);
    return topic;
  }
}
