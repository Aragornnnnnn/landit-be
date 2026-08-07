// 프리톡 사용자 발화에 대한 속마음 생성 결과를 담는다.

package com.landit.landitbe.feature.session.client.ai;

import com.landit.landitbe.shared.domain.InnerThoughtType;

/**
 * 프리톡 사용자 발화에 대한 속마음 생성 결과를 담는다.
 *
 * @param innerThought AI 상대의 한국어 속마음
 * @param innerThoughtType 판단 근거로 계산된 속마음 유형
 */
public record AiFreeTalkInnerThoughtResult(
    String innerThought, InnerThoughtType innerThoughtType) {}
