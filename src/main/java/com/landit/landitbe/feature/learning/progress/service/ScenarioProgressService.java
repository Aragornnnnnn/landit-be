// 사용자별 시나리오 시작과 완료 진행 상태를 조회하고 변경한다.

package com.landit.landitbe.feature.learning.progress.service;

import com.landit.landitbe.feature.learning.progress.domain.UserScenarioProgress;
import com.landit.landitbe.feature.learning.progress.domain.UserScenarioProgressStatus;
import com.landit.landitbe.feature.learning.progress.repository.UserScenarioProgressRepository;
import com.landit.landitbe.shared.domain.Locale;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 사용자별 시나리오 시작과 완료 진행 상태를 조회하고 변경한다. */
@Service
@RequiredArgsConstructor
public class ScenarioProgressService {
  private final UserScenarioProgressRepository userScenarioProgressRepository;

  /**
   * 마지막 완료 시각을 기준으로 지정 시각 이후의 완료 이력을 확인한다.
   *
   * @param userId 사용자 ID
   * @param since 완료 이력의 시작 시각
   * @return 재완료를 포함한 완료 이력이 있으면 true
   */
  @Transactional(readOnly = true)
  public boolean hasClearedScenarioSince(Long userId, LocalDateTime since) {
    return userScenarioProgressRepository
        .existsByUserProfileIdAndStatusAndLastClearedAtGreaterThanEqual(
            userId, UserScenarioProgressStatus.CLEARED, since);
  }

  /**
   * 시나리오 시작 진행도를 생성하거나 최근 시작 시각을 갱신한다.
   *
   * @param userId 사용자 ID
   * @param scenarioId 시나리오 ID
   * @param targetLocale 학습 대상 언어
   * @param startedAt 시작 시각
   */
  @Transactional
  public void startScenario(
      Long userId, Long scenarioId, Locale targetLocale, LocalDateTime startedAt) {
    userScenarioProgressRepository
        .findByUserProfileIdAndScenarioIdAndTargetLocale(userId, scenarioId, targetLocale)
        .ifPresentOrElse(
            progress -> progress.markStarted(startedAt),
            () ->
                userScenarioProgressRepository.save(
                    UserScenarioProgress.start(userId, scenarioId, targetLocale, startedAt)));
  }

  /**
   * 시나리오 완료 결과와 최고 성과를 갱신한다.
   *
   * @param userId 사용자 ID
   * @param scenarioId 시나리오 ID
   * @param targetLocale 학습 대상 언어
   * @param starRating 별점
   * @param nativeScore 원어민 유사도 점수
   * @param endedAt 종료 시각
   * @throws ApiException 시작 진행도가 없을 때
   */
  @Transactional
  public void completeScenario(
      Long userId,
      Long scenarioId,
      Locale targetLocale,
      BigDecimal starRating,
      int nativeScore,
      LocalDateTime endedAt) {
    UserScenarioProgress progress =
        userScenarioProgressRepository
            .findByUserProfileIdAndScenarioIdAndTargetLocale(userId, scenarioId, targetLocale)
            .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_SERVER_ERROR));
    progress.complete(starRating, nativeScore, endedAt);
  }
}
