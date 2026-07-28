// 프리톡 사용자 발화 제출 요청을 검증한다.

package com.landit.landitbe.feature.session.dto;

import com.landit.landitbe.feature.session.domain.SessionMessageInputType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * 프리톡 사용자 발화 제출 요청을 검증한다.
 *
 * @param clientMessageId 중복 제출을 막는 클라이언트 메시지 ID
 * @param content 사용자 발화 원문
 * @param inputType 사용자 발화 입력 방식
 * @param utteranceDurationMs 이번 사용자 발화 시간 밀리초
 * @param timeLimitReached 클라이언트 타이머의 제한 도달 신호이며 서버 누적 시간 판정과 함께 사용한다
 */
public record FreeTalkMessageSubmitRequest(
    @NotBlank
        @Pattern(
            regexp =
                "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-"
                    + "[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$")
        String clientMessageId,
    @NotBlank String content,
    @NotNull SessionMessageInputType inputType,
    @NotNull @Min(0) Long utteranceDurationMs,
    @NotNull Boolean timeLimitReached) {}
