// AI가 판정한, 지켜보던 실수 패턴이 이번 턴에 등장한 사용례 하나를 저장 전 값으로 전달한다.

package com.landit.landitbe.feature.learning.freetalk.feedback.dto;

import com.landit.landitbe.feature.learning.freetalk.feedback.domain.FreeTalkMistakePattern;

/**
 * 지켜보던 실수 패턴이 이번 턴에 등장한 사용례다. 원격 응답을 다시 확인해 통과한 값만 담는다.
 *
 * @param pattern 지켜보던 실수 패턴. 예: TENSE
 * @param sentence 발화 원문에서 그 패턴이 쓰인 한 문장. 예: "I went to the gym."
 * @param span 그 문장 안에서 강조할 구절. 문장에 대소문자까지 그대로 들어 있다. 예: "went"
 * @param correct 맞게 썼는지 여부. 예: true
 */
public record FreeTalkPatternUsageDraft(
    FreeTalkMistakePattern pattern, String sentence, String span, boolean correct) {}
