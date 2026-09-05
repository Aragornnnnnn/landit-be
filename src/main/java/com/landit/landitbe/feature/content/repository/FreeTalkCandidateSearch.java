// 공용 프리톡 표현 후보 검색의 조건을 담는다.

package com.landit.landitbe.feature.content.repository;

import com.landit.landitbe.shared.domain.Locale;
import java.util.List;

/**
 * 공용 프리톡 표현 후보 검색의 조건을 담는다.
 *
 * <p>사용자 ID와 난이도 상한, 후보 수가 모두 숫자 타입이라 위치 인자로 넘기면 순서를 바꿔도 컴파일이 통과한다. 이름으로 넘기도록 레코드로 묶는다.
 *
 * @param embedding 쿼리 임베딩 벡터 (1,536차원)
 * @param userProfileId 학습 완료 표현을 제외할 사용자 ID
 * @param targetLocale 학습 언어
 * @param baseLocale 기준 언어
 * @param maxDifficultyLevel 노출할 표현 난이도의 상한. 이 값 이하인 표현만 후보가 된다
 * @param limit 최대 후보 수
 */
public record FreeTalkCandidateSearch(
    List<Float> embedding,
    long userProfileId,
    Locale targetLocale,
    Locale baseLocale,
    int maxDifficultyLevel,
    int limit) {}
