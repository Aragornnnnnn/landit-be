// 프리톡 사용자 발화에 대한 속마음 생성 결과를 담는다.

package com.landit.landitbe.feature.session.client.ai;

import com.landit.landitbe.shared.domain.InnerThoughtType;

/** 프리톡 사용자 발화에 대한 속마음 생성 결과를 담는다. */
public record AiFreeTalkInnerThoughtResult(
    String innerThought, InnerThoughtType innerThoughtType) {}
