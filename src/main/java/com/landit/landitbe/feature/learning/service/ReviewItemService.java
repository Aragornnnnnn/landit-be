// 복습 문항을 소유하며 다른 기능에 복습 대상 조회 계약을 제공한다.

package com.landit.landitbe.feature.learning.service;

import com.landit.landitbe.feature.learning.domain.ReviewItemStatus;
import com.landit.landitbe.feature.learning.repository.ReviewItemRepository;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 복습 문항을 소유하며 다른 기능에 복습 대상 조회 계약을 제공한다. */
@Service
@RequiredArgsConstructor
public class ReviewItemService {

  private final ReviewItemRepository reviewItemRepository;
  private final UserProfileService userProfileService;

  /**
   * 기준 날짜에 READY 복습 문항이 있는 활성 사용자 ID를 조회한다.
   *
   * @param reviewDate 복습 기준 날짜
   * @return 복습 리마인더 대상 사용자 ID 목록
   */
  @Transactional(readOnly = true)
  public List<Long> findReminderTargetUserIds(LocalDate reviewDate) {
    return reviewItemRepository
        .findDistinctUserProfileIds(reviewDate, ReviewItemStatus.READY)
        .stream()
        .filter(userProfileService::existsActive)
        .toList();
  }
}
