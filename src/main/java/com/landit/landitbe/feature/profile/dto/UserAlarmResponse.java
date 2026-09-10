// 클라이언트가 매일 반복 예약에 사용할 사용자 알람 설정을 반환한다.

package com.landit.landitbe.feature.profile.dto;

import com.landit.landitbe.feature.profile.domain.UserAlarm;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.format.DateTimeFormatter;

/**
 * 클라이언트가 예약에 사용할 사용자별 일일 알람 설정이다.
 *
 * @param time 기기 현지 시간 기준 HH:mm 시각. 한 번도 설정하지 않았으면 {@code null}
 * @param enabled 알람 활성화 여부. 한 번도 설정하지 않았으면 {@code false}
 */
@Schema(description = "사용자 일일 알람 설정. 실제 기기 예약 성공 여부와는 별개입니다.")
public record UserAlarmResponse(
    @Schema(
            description = "기기 현지 시간 기준 HH:mm 시각. 미설정이면 null",
            example = "07:30",
            nullable = true,
            types = {"string", "null"})
        String time,
    @Schema(description = "저장된 알람 활성화 여부", example = "true") boolean enabled) {

  private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

  /**
   * 저장된 알람 설정을 클라이언트 응답으로 변환한다.
   *
   * @param alarm 저장된 알람 설정
   * @return 분 단위 시각과 활성 상태
   */
  public static UserAlarmResponse from(UserAlarm alarm) {
    return new UserAlarmResponse(alarm.getAlarmTime().format(TIME_FORMAT), alarm.isEnabled());
  }
}
