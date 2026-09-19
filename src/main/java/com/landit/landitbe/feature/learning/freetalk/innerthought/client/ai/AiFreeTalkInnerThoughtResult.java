// 프리톡 사용자 발화에 대한 속마음과 턴 교정 생성 결과를 담는다.

package com.landit.landitbe.feature.learning.freetalk.innerthought.client.ai;

import com.landit.landitbe.feature.learning.conversation.dto.FreeTalkTurnCorrection;
import com.landit.landitbe.shared.domain.InnerThoughtType;
import java.util.Objects;

/**
 * 프리톡 사용자 발화에 대한 속마음과 턴 교정 생성 결과를 담는다.
 *
 * @param innerThought AI 상대의 한국어 속마음
 * @param innerThoughtType 판단 근거로 계산된 속마음 유형
 * @param correction 같은 응답에 실려 온 턴 교정 판정. 판정에 실패했으면 {@code FAILED} 상태
 */
public record AiFreeTalkInnerThoughtResult(
    String innerThought, InnerThoughtType innerThoughtType, FreeTalkTurnCorrection correction) {

  /** 교정 판정이 빠진 결과는 저장 단계에서 속마음까지 잃게 하므로 생성 시점에 거부한다. */
  public AiFreeTalkInnerThoughtResult {
    Objects.requireNonNull(correction, "correction");
  }
}
