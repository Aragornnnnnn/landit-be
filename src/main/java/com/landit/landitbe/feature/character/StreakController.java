// 스트릭 조회 HTTP 요청을 처리한다.

package com.landit.landitbe.feature.character;

import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.feature.character.docs.StreakControllerDocs;
import com.landit.landitbe.feature.character.dto.CurrentStreakResponse;
import com.landit.landitbe.feature.character.dto.StreakCalendarResponse;
import com.landit.landitbe.feature.character.service.StreakService;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
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
      @RequestParam(required = false) Integer year,
      @RequestParam(required = false) Integer month) {
    return ApiResponse.success(
        StreakCalendarResponse.from(
            streakService.getCalendar(principal.userId(), requestedMonth(year, month))));
  }

  private static YearMonth requestedMonth(Integer year, Integer month) {
    if (year == null && month == null) {
      return null;
    }
    if (year == null || month == null) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED);
    }
    return YearMonth.of(year, month);
  }
}
