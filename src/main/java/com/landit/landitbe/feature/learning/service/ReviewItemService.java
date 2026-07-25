// 복습 문항을 소유하며 다른 기능에 복습 대상 조회 계약을 제공한다.

package com.landit.landitbe.feature.learning.service;

import com.landit.landitbe.feature.learning.domain.ReviewItemStatus;
import com.landit.landitbe.feature.learning.repository.ReviewItemRepository;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 복습 문항을 소유하며 다른 기능에 복습 대상 조회 계약을 제공한다. */
@Service
@RequiredArgsConstructor
public class ReviewItemService {

  private final ReviewItemRepository reviewItemRepository;
  private final UserProfileService userProfileService;

  /**
   * 기준 날짜에 READY 복습 문항이 있는 활성 사용자를 Cursor 페이지로 조회한다.
   *
   * @param reviewDate 복습 기준 날짜
   * @param afterUserProfileId 이전 페이지의 마지막 후보 사용자 ID. 첫 페이지면 {@code null}
   * @param pageSize 한 페이지에 조회할 후보 사용자 수
   * @return 복습 리마인더 대상 사용자 페이지
   */
  @Transactional(readOnly = true)
  public ReviewReminderTargetPage findReminderTargetPage(
      LocalDate reviewDate, Long afterUserProfileId, int pageSize) {
    List<Long> candidateUserProfileIds =
        reviewItemRepository.findDistinctUserProfileIdsAfter(
            reviewDate,
            ReviewItemStatus.READY,
            afterUserProfileId,
            PageRequest.of(0, pageSize + 1));
    boolean hasNext = candidateUserProfileIds.size() > pageSize;
    List<Long> currentPageUserProfileIds =
        candidateUserProfileIds.subList(0, Math.min(candidateUserProfileIds.size(), pageSize));
    Long nextUserProfileId =
        hasNext ? currentPageUserProfileIds.get(currentPageUserProfileIds.size() - 1) : null;
    return new ReviewReminderTargetPage(
        userProfileService.findActiveUserProfileIds(currentPageUserProfileIds), nextUserProfileId);
  }
}
