// 시나리오 총평의 직전 교정 비교 카드를 전달한다.

package com.landit.landitbe.feature.learning.scenario.feedback.dto;

import com.landit.landitbe.feature.learning.scenario.feedback.domain.ScenarioMistakePattern;
import java.time.LocalDate;

/**
 * 실제 교정 근거와 현재 사용례가 모두 검증된 성장 카드다.
 *
 * @param pattern 비교할 문법 실수 유형
 * @param patternLabel 화면에서 사용할 한국어 실수 유형 이름
 * @param succeeded 현재 발화에서 해당 패턴을 올바르게 사용했는지
 * @param previousDate 직전 완료 시나리오 날짜
 * @param previousSentence 직전 발화 중 비교할 문장
 * @param previousWrongSpan 직전 발화에서 틀린 구절
 * @param currentSentence 현재 발화 중 비교할 문장
 * @param currentSpan 현재 발화의 비교 구절
 */
public record ScenarioGrowthCard(
    ScenarioMistakePattern pattern,
    String patternLabel,
    boolean succeeded,
    LocalDate previousDate,
    String previousSentence,
    String previousWrongSpan,
    String currentSentence,
    String currentSpan) {}
