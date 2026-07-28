// 프리톡 종료 의사 확인 결과 요청을 검증한다.

package com.landit.landitbe.feature.session.dto;

import com.landit.landitbe.feature.session.domain.FreeTalkExitDecision;
import jakarta.validation.constraints.NotNull;

/** 프리톡 종료 의사 확인 결과 요청을 검증한다. */
public record FreeTalkExitDecisionRequest(
    @NotNull Long submittedMessageId, @NotNull FreeTalkExitDecision decision) {}
