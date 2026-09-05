// 사용자가 온보딩에서 선택한 영어 억양 변경 요청을 전달한다.

package com.landit.landitbe.feature.profile.dto;

import com.landit.landitbe.shared.domain.AccentLocale;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 사용자가 온보딩에서 선택한 영어 억양 변경 요청을 전달한다.
 *
 * @param accentLocale 선택한 영어 억양
 */
@Schema(description = "사용자 영어 억양 변경 요청")
public record UserAccentLocaleUpdateRequest(
    @NotNull @Schema(description = "선택한 영어 억양", example = "EN_GB") AccentLocale accentLocale) {}
