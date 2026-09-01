// 문장 발화 발음 평가 응답을 표현한다.

package com.landit.landitbe.feature.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 문장 발화 발음 평가 응답을 표현한다.
 *
 * <p>AI 판정(어느 단어가 어떻게 틀렸나)과 사전 생성 자산(원어민 발음 표기·단어 TTS URL), BE가 계산한 점수·코칭 문구를 합쳐 내려준다.
 *
 * @param score 점수 0~100. 정상 단어 수 / 전체 단어 수 × 100 반올림
 * @param passed 통과 여부. 오류 단어가 0개면 true
 * @param words 단어별 판정. order 오름차순, 대표 예문의 단어와 1:1. 통과 여부와 무관하게 항상 전체 반환
 */
@Schema(description = "문장 발화 발음 평가 응답")
public record PronunciationAnalysisResponse(
    @Schema(description = "점수 0~100. 정상 단어 수 / 전체 단어 수 × 100 반올림. 오류 없으면 100", example = "75")
        int score,
    @Schema(description = "통과 여부. 오류 단어(PHONEME_ERROR·STRESS_ERROR)가 0개면 true", example = "false")
        boolean passed,
    @Schema(description = "단어별 판정. order 오름차순, 대표 예문의 단어와 1:1") List<Word> words) {

  /**
   * 단어 1개의 판정 결과를 표현한다.
   *
   * @param order 문장 내 순번 (1부터)
   * @param word 단어 표면형
   * @param status CORRECT, PHONEME_ERROR, STRESS_ERROR
   * @param startTimeMs 사용자 녹음에서 이 단어 구간 시작(ms). 컷 보정 적용 완료 값
   * @param endTimeMs 사용자 녹음에서 이 단어 구간 끝(ms). 컷 보정 적용 완료 값
   * @param nativeWordAudioUrl 원어민 단어 TTS CDN URL. 오류 단어만
   * @param nativeDisplay 원어민 발음 respelling. PHONEME_ERROR만
   * @param userDisplay 사용자 발음 respelling. PHONEME_ERROR만
   * @param errorTargetSpan 원어민 표기에서 빨강 처리할 부분. PHONEME_ERROR만
   * @param errorUserSpan 사용자 표기에서 빨강 처리할 부분. PHONEME_ERROR만
   * @param syllables 음절 분해 배열. STRESS_ERROR만
   * @param stressIndex 원어민 강세 음절 인덱스(초록 점). STRESS_ERROR만
   * @param userStressIndex 사용자가 힘준 음절 인덱스(빨간 점). STRESS_ERROR만
   * @param coachingText 코칭 문구. 오류 단어만
   */
  @Schema(description = "단어 1개의 발음 판정")
  public record Word(
      @Schema(description = "문장 내 단어 순번", example = "2") int order,
      @Schema(description = "단어 표면형", example = "nothing") String word,
      @Schema(description = "판정 상태", example = "PHONEME_ERROR") String status,
      @Schema(description = "사용자 녹음에서 단어 구간 시작(ms)", example = "500") Integer startTimeMs,
      @Schema(description = "사용자 녹음에서 단어 구간 끝(ms)", example = "940") Integer endTimeMs,
      @Schema(description = "원어민 단어 TTS CDN URL. 오류 단어만") String nativeWordAudioUrl,
      @Schema(description = "원어민 발음 respelling. PHONEME_ERROR만", example = "nuh·thing")
          String nativeDisplay,
      @Schema(description = "사용자 발음 respelling. PHONEME_ERROR만", example = "nuh·ssing")
          String userDisplay,
      @Schema(description = "원어민 표기에서 빨강 처리할 부분. PHONEME_ERROR만", example = "th")
          String errorTargetSpan,
      @Schema(description = "사용자 표기에서 빨강 처리할 부분. PHONEME_ERROR만", example = "ss")
          String errorUserSpan,
      @Schema(description = "음절 분해 배열. STRESS_ERROR만", example = "[\"hik\", \"ing\"]")
          List<String> syllables,
      @Schema(description = "원어민 강세 음절 인덱스(초록 점). STRESS_ERROR만", example = "0")
          Integer stressIndex,
      @Schema(description = "사용자가 힘준 음절 인덱스(빨간 점). STRESS_ERROR만", example = "1")
          Integer userStressIndex,
      @Schema(
              description = "코칭 문구. 오류 단어만",
              example = "'th'가 'ss'처럼 들렸어요. 혀끝을 윗니와 아랫니 사이에 살짝 내밀어 대고 바람을 내보내세요.")
          String coachingText) {}
}
