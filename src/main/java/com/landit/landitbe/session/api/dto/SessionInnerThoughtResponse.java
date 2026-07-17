// 사용자 메시지의 상대 역할 속마음 조회 응답을 정의한다.

package com.landit.landitbe.session.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 사용자 메시지의 상대 역할 속마음 조회 응답을 정의한다. */
@Schema(description = "사용자 메시지 속마음 조회 응답")
public record SessionInnerThoughtResponse(
    @Schema(
            description = "속마음 처리 상태",
            allowableValues = {"PREPARING", "COMPLETED", "FAILED"})
        String processingStatus,
    @Schema(description = "상대 역할의 속마음. COMPLETED에서만 제공") String innerThought,
    @Schema(description = "속마음 유형. COMPLETED에서만 제공") String innerThoughtType) {}
