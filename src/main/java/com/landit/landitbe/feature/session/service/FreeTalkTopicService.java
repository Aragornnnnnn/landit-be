// 프리톡 추천 주제 조회 규칙을 제공한다.

package com.landit.landitbe.feature.session.service;

import com.landit.landitbe.feature.session.dto.FreeTalkMainResponse;
import com.landit.landitbe.feature.session.dto.FreeTalkTopicResponse;
import com.landit.landitbe.feature.session.repository.FreeTalkTopicRepository;
import com.landit.landitbe.shared.domain.ActiveStatus;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 프리톡 추천 주제 조회 규칙을 제공한다. */
@RequiredArgsConstructor
@Service
public class FreeTalkTopicService {

  private final FreeTalkTopicRepository freeTalkTopicRepository;
  private final FreeTalkDailySpeakingUsageService dailySpeakingUsageService;

  /**
   * 활성 프리톡 주제를 노출 순서대로 반환한다.
   *
   * @return 활성 추천 주제 응답 목록
   */
  @Transactional(readOnly = true)
  public List<FreeTalkTopicResponse> getActiveTopics() {
    return freeTalkTopicRepository
        .findAllByStatusOrderByDisplayOrderAsc(ActiveStatus.ACTIVE)
        .stream()
        .map(FreeTalkTopicResponse::from)
        .toList();
  }

  /**
   * 활성 주제와 KST 당일의 남은 사용자 발화 시간을 반환한다.
   *
   * @param userId 사용자 ID
   * @return 활성 주제와 일일 발화 사용량을 담은 메인 화면 응답
   */
  @Transactional(readOnly = true)
  public FreeTalkMainResponse getMain(long userId) {
    FreeTalkDailySpeakingUsageService.DailySpeakingUsage dailyUsage =
        dailySpeakingUsageService.usage(userId);
    return FreeTalkMainResponse.of(
        getActiveTopics(), dailyUsage.usedSpeakingDurationMs(), dailyUsage.remainingMs());
  }
}
