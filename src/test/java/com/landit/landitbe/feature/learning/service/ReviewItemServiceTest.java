// 복습 리마인더 대상 사용자를 조회하는 학습 기능 공개 계약을 검증한다.

package com.landit.landitbe.feature.learning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.learning.domain.ReviewItemStatus;
import com.landit.landitbe.feature.learning.repository.ReviewItemRepository;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 복습 리마인더 대상 사용자를 조회하는 학습 기능 공개 계약을 검증한다. */
@ExtendWith(MockitoExtension.class)
class ReviewItemServiceTest {

  private static final LocalDate REVIEW_DATE = LocalDate.of(2026, 7, 24);

  @Mock private ReviewItemRepository reviewItemRepository;

  @Mock private UserProfileService userProfileService;

  @InjectMocks private ReviewItemService reviewItemService;

  /** 기준 날짜의 READY 복습 항목을 가진 활성 사용자만 반환한다. */
  @Test
  void findsActiveUsersWithReadyReviewItems() {
    when(reviewItemRepository.findDistinctUserProfileIds(REVIEW_DATE, ReviewItemStatus.READY))
        .thenReturn(List.of(1L, 2L, 3L));
    when(userProfileService.existsActive(1L)).thenReturn(true);
    when(userProfileService.existsActive(2L)).thenReturn(false);
    when(userProfileService.existsActive(3L)).thenReturn(true);

    assertThat(reviewItemService.findReminderTargetUserIds(REVIEW_DATE)).containsExactly(1L, 3L);
    verify(reviewItemRepository).findDistinctUserProfileIds(REVIEW_DATE, ReviewItemStatus.READY);
  }
}
