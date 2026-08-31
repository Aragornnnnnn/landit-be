// AI 서버가 판정한 단어 1개의 발음 결과를 표현한다.

package com.landit.landitbe.feature.content.client.ai.dto;

/**
 * AI → BE. AI 서버가 판정한 단어 1개의 발음 결과를 표현한다.
 *
 * <p>AI는 판정 데이터만 반환하고, 점수 계산과 코칭 문구 조립은 BE가 담당한다.
 *
 * <p>예시 — "nothing"을 "nuh·ssing"처럼 발음한 경우: {@code order=2, word="nothing", status=PHONEME_ERROR,
 * userDisplay="nuh·ssing", errorTargetSpan="th", errorUserSpan="ss", userStressIndex=null}
 *
 * <p>예시 — "hiking"의 강세를 뒷 음절에 잘못 둔 경우: {@code order=4, word="hiking", status=STRESS_ERROR,
 * userDisplay=null, errorTargetSpan=null, errorUserSpan=null, userStressIndex=1}
 *
 * @param order 문장 내 순번 (1부터). 예: 2 (문장의 두 번째 단어)
 * @param word 단어 표면형. 예: "nothing"
 * @param status 판정 상태. 예: PHONEME_ERROR (th를 s처럼 발음)
 * @param userDisplay 유저 발음이 어떻게 들렸는지의 respelling. PHONEME_ERROR만. 예: "nuh·ssing"
 * @param errorTargetSpan 원어민 표기에서 다르게 들린 부분(빨강 표시 대상). PHONEME_ERROR만. 예: "th"
 * @param errorUserSpan 유저 표기에서 다르게 들린 부분(빨강 표시 대상). PHONEME_ERROR만. 예: "ss"
 * @param userStressIndex 유저가 힘준 음절 인덱스(0부터). STRESS_ERROR만. 예: 1 — hik·ing에서 뒷 음절 ing에 잘못 힘을 줬다는 뜻
 *     (정답 강세 위치는 자산 테이블의 stressIndex가 앎)
 */
public record AiPronunciationJudgedWord(
    int order,
    String word,
    AiPronunciationWordStatus status,
    String userDisplay,
    String errorTargetSpan,
    String errorUserSpan,
    Integer userStressIndex) {}
