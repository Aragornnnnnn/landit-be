// 스트릭 조회 HTTP 요청을 처리한다.

package com.landit.landitbe.feature.character;

import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.feature.character.docs.StreakControllerDocs;
import com.landit.landitbe.feature.character.dto.CurrentStreakResponse;
import com.landit.landitbe.feature.character.dto.StreakCalendarResponse;
import com.landit.landitbe.feature.character.service.StreakService;
import com.landit.landitbe.shared.response.ApiResponse;
import java.time.YearMonth;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 스트릭 조회 HTTP 요청을 처리한다. */
@RequiredArgsConstructor
@RestController
@Validated
public class StreakController implements StreakControllerDocs {

  private final StreakService streakService;

  /** {@inheritDoc} */
  @Override
  @GetMapping("/api/v1/me/streak")
  public ApiResponse<CurrentStreakResponse> getCurrentStreak(
      @AuthenticationPrincipal AuthUserPrincipal principal) {
    return ApiResponse.success(
        CurrentStreakResponse.from(streakService.getCurrentStreak(principal.userId())));
  }

  /** {@inheritDoc} */
  @Override
  @GetMapping("/api/v1/me/streak/calendar")
  public ApiResponse<StreakCalendarResponse> getCalendar(
      @AuthenticationPrincipal AuthUserPrincipal principal,
      @RequestParam int year,
      @RequestParam int month) {
    return ApiResponse.success(
        StreakCalendarResponse.from(
            year, month, streakService.getCalendar(principal.userId(), YearMonth.of(year, month))));
  }
}
