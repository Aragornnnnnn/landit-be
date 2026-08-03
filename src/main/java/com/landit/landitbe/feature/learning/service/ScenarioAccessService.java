// 사용자 시나리오 복습 권한의 보유 여부와 획득 처리를 담당한다.

package com.landit.landitbe.feature.learning.service;

import com.landit.landitbe.feature.learning.domain.UserScenarioAccess;
import com.landit.landitbe.feature.learning.repository.UserScenarioAccessRepository;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import com.landit.landitbe.shared.domain.Locale;
import com.landit.landitbe.shared.exception.ApiException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 사용자 시나리오 복습 권한의 보유 여부와 획득 처리를 담당한다. */
@Service
@RequiredArgsConstructor
public class ScenarioAccessService {

  private final UserScenarioAccessRepository userScenarioAccessRepository;
  private final UserProfileService userProfileService;

  /**
   * 사용자가 대상 언어의 특정 시나리오 복습 권한을 보유하는지 확인한다.
   *
   * @param userProfileId 사용자 프로필 ID
   * @param scenarioId 시나리오 ID
   * @param targetLocale 학습 대상 언어
   * @return 복습 권한 보유 여부
   */
  @Transactional(readOnly = true)
  public boolean hasAccess(Long userProfileId, Long scenarioId, Locale targetLocale) {
    return userScenarioAccessRepository.existsByUserProfileIdAndScenarioIdAndTargetLocale(
        userProfileId, scenarioId, targetLocale);
  }

  /**
   * 사용자가 대상 언어로 복습할 수 있는 시나리오 ID 목록을 조회한다.
   *
   * @param userProfileId 사용자 프로필 ID
   * @param targetLocale 학습 대상 언어
   * @return 복습 가능한 시나리오 ID 목록
   */
  @Transactional(readOnly = true)
  public List<Long> findAccessibleScenarioIds(Long userProfileId, Locale targetLocale) {
    return userScenarioAccessRepository
        .findAllByUserProfileIdAndTargetLocaleOrderByScenarioIdAsc(userProfileId, targetLocale)
        .stream()
        .map(UserScenarioAccess::getScenarioId)
        .toList();
  }

  /**
   * 사용자가 해당 날짜에 새 복습 권한을 얻었는지 확인한다.
   *
   * @param userProfileId 사용자 프로필 ID
   * @param targetLocale 학습 대상 언어
   * @param date 확인할 날짜
   * @return 해당 날짜에 새 복습 권한을 얻었으면 true
   */
  @Transactional(readOnly = true)
  public boolean hasAccessGrantedOn(Long userProfileId, Locale targetLocale, LocalDate date) {
    return userScenarioAccessRepository.existsGrantedOn(
        userProfileId, targetLocale, date.atStartOfDay(), date.plusDays(1).atStartOfDay());
  }

  /**
   * 사용자가 특정 날짜에 최초 획득한 시나리오 복습 권한을 조회한다.
   *
   * @param userProfileId 사용자 프로필 ID
   * @param targetLocale 학습 대상 언어
   * @param date 조회 날짜
   * @return 해당 날짜에 최초 완료한 시나리오 이력
   */
  @Transactional(readOnly = true)
  public Optional<ScenarioAccessHistory> findAccessGrantedOn(
      Long userProfileId, Locale targetLocale, LocalDate date) {
    return userScenarioAccessRepository
        .findAllGrantedOn(
            userProfileId, targetLocale, date.atStartOfDay(), date.plusDays(1).atStartOfDay())
        .stream()
        .findFirst()
        .map(access -> new ScenarioAccessHistory(access.getScenarioId(), access.getGrantedAt()));
  }

  /**
   * 이전 날짜에 시작했지만 완료하지 못한 시나리오 세션이 있는지 확인한다.
   *
   * @param userProfileId 사용자 프로필 ID
   * @param scenarioId 시나리오 ID
   * @param targetLocale 학습 대상 언어
   * @param date 오늘 날짜
   * @return 이전 날짜의 미완료 세션이 있으면 true
   */
  @Transactional(readOnly = true)
  public boolean hasUncompletedSessionBefore(
      Long userProfileId, Long scenarioId, Locale targetLocale, LocalDate date) {
    return userScenarioAccessRepository.existsUncompletedSessionBefore(
        userProfileId, scenarioId, targetLocale, date.atStartOfDay());
  }

  /**
   * 사용자가 날짜 구간 안에서 완료한 시나리오를 날짜별로 조회한다. 같은 날 복습 권한이 복수로 생성된 경우 획득 시각이 가장 이른 시나리오를 사용한다.
   *
   * @param userProfileId 사용자 프로필 ID
   * @param targetLocale 학습 대상 언어
   * @param startDate 구간 시작 날짜(포함)
   * @param endDateExclusive 구간 끝 날짜(제외)
   * @return 날짜 오름차순으로 정렬한 날짜별 완료 시나리오 목록
   */
  @Transactional(readOnly = true)
  public List<DailyCompletion> findCompletionsBetween(
      Long userProfileId, Locale targetLocale, LocalDate startDate, LocalDate endDateExclusive) {
    List<UserScenarioAccess> accesses =
        userScenarioAccessRepository.findAllGrantedBetween(
            userProfileId, targetLocale, startDate.atStartOfDay(), endDateExclusive.atStartOfDay());

    Map<LocalDate, Long> scenarioIdsByDate = new LinkedHashMap<>();
    for (UserScenarioAccess access : accesses) {
      scenarioIdsByDate.putIfAbsent(access.getGrantedAt().toLocalDate(), access.getScenarioId());
    }

    return scenarioIdsByDate.entrySet().stream()
        .map(completion -> new DailyCompletion(completion.getKey(), completion.getValue()))
        .toList();
  }

  /**
   * 사용자가 하루 동안 완료한 시나리오를 담는다.
   *
   * @param date 완료한 날짜
   * @param scenarioId 완료한 시나리오 ID
   */
  public record DailyCompletion(LocalDate date, Long scenarioId) {}

  /**
   * 사용자가 대상 언어에서 처음으로 시나리오를 완료한 날짜를 조회한다.
   *
   * @param userProfileId 사용자 프로필 ID
   * @param targetLocale 학습 대상 언어
   * @return 첫 완료 날짜. 완료 이력이 없으면 빈 값
   */
  @Transactional(readOnly = true)
  public Optional<LocalDate> findFirstCompletionDate(Long userProfileId, Locale targetLocale) {
    return userScenarioAccessRepository
        .findTopByUserProfileIdAndTargetLocaleOrderByGrantedAtAsc(userProfileId, targetLocale)
        .map(access -> access.getGrantedAt().toLocalDate());
  }

  /**
   * 시나리오 완료로 복습 권한을 획득한다. 이미 보유한 권한이면 성공으로 처리한다.
   *
   * @param userProfileId 사용자 프로필 ID
   * @param scenarioId 완료한 시나리오 ID
   * @param targetLocale 학습 대상 언어
   * @param grantedAt 복습 권한을 얻은 시각
   * @throws ApiException 사용자 프로필이 없거나 비활성 상태일 때
   */
  @Transactional
  public void grantAccess(
      Long userProfileId, Long scenarioId, Locale targetLocale, LocalDateTime grantedAt) {
    userProfileService.requireActiveForUpdate(userProfileId);
    if (hasAccess(userProfileId, scenarioId, targetLocale)) {
      return;
    }
    userScenarioAccessRepository.save(
        UserScenarioAccess.grant(userProfileId, scenarioId, targetLocale, grantedAt));
  }

  /**
   * 날짜별 시나리오 이력 조회에 필요한 최초 복습 권한 정보를 담는다.
   *
   * @param scenarioId 최초 완료한 시나리오 ID
   * @param grantedAt 최초 완료 시각
   */
  public record ScenarioAccessHistory(Long scenarioId, LocalDateTime grantedAt) {}
}
