// 프리톡 추천 주제 조회 규칙을 제공한다.

package com.landit.landitbe.feature.learning.freetalk.topic.service;

import com.landit.landitbe.feature.learning.freetalk.topic.domain.FreeTalkTopic;
import com.landit.landitbe.feature.learning.freetalk.topic.dto.FreeTalkMainResponse;
import com.landit.landitbe.feature.learning.freetalk.topic.dto.FreeTalkTopicResponse;
import com.landit.landitbe.feature.learning.freetalk.topic.repository.FreeTalkTopicRepository;
import com.landit.landitbe.feature.learning.freetalk.usage.dto.DailySpeakingUsage;
import com.landit.landitbe.feature.learning.freetalk.usage.service.FreeTalkDailySpeakingUsageService;
import com.landit.landitbe.shared.domain.ActiveStatus;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 프리톡 추천 주제 조회 규칙을 제공한다. */
@RequiredArgsConstructor
@Service
public class FreeTalkTopicService {

  /** 메인 화면에 한 번에 노출하는 추천 주제 수다. */
  static final int MAIN_TOPIC_COUNT = 5;

  private final FreeTalkTopicRepository freeTalkTopicRepository;
  private final FreeTalkDailySpeakingUsageService dailySpeakingUsageService;
  private final Random random = new Random();

  /**
   * 활성 프리톡 주제 중 무작위로 최대 5개를 뽑아 반환한다.
   *
   * <p>DB의 display_order는 주제 풀 관리용이라 응답 순서에 쓰지 않는다. 뽑힌 주제에 1부터 순서를 새로 매기므로 요청마다 주제 구성과 순서가 달라진다. 활성
   * 주제가 5개보다 적으면 있는 만큼만 반환한다.
   *
   * @return 무작위로 뽑은 추천 주제 응답 목록(최대 5개)
   */
  @Transactional(readOnly = true)
  public List<FreeTalkTopicResponse> getActiveTopics() {
    List<FreeTalkTopic> topics =
        new ArrayList<>(
            freeTalkTopicRepository.findAllByStatusOrderByDisplayOrderAsc(ActiveStatus.ACTIVE));
    Collections.shuffle(topics, random);
    List<FreeTalkTopic> picked = topics.subList(0, Math.min(MAIN_TOPIC_COUNT, topics.size()));
    return IntStream.range(0, picked.size())
        .mapToObj(index -> FreeTalkTopicResponse.of(picked.get(index), index + 1))
        .toList();
  }

  /**
   * 무작위 추천 주제와 KST 당일의 남은 사용자 발화 시간을 반환한다.
   *
   * @param userId 사용자 ID
   * @return 무작위 주제(최대 5개)와 일일 발화 사용량을 담은 메인 화면 응답
   */
  @Transactional(readOnly = true)
  public FreeTalkMainResponse getMain(long userId) {
    DailySpeakingUsage dailyUsage = dailySpeakingUsageService.usage(userId);
    return FreeTalkMainResponse.of(
        getActiveTopics(),
        dailySpeakingUsageService.speakingTimeLimitMs(),
        dailyUsage.usedSpeakingDurationMs(),
        dailyUsage.remainingMs());
  }
}
