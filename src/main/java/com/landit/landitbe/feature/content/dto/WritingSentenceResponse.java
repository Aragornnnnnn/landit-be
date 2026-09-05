// 추가 예문 중 선택된 작문 연습 문제를 표현한다.

package com.landit.landitbe.feature.content.dto;

import com.landit.landitbe.shared.domain.Locale;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 추가 예문 중 선택된 작문 연습 문제를 표현한다.
 *
 * <p>단어 배열 두 개는 이름이 언어 중립이며, 어느 언어가 담기는지는 {@code quizLanguage}가 정한다. 예문 텍스트와 해석은 출제 언어와 무관하게 항상 함께
 * 내려주고, 화면에서 어느 쪽을 문제로 보여주고 어느 쪽을 정답으로 쓸지는 앱이 {@code quizLanguage}로 판단한다.
 *
 * @param quizLanguage 작문 문제의 출제 언어. {@code EN}이면 영어 문장을, {@code KR}이면 한국어 해석을 조립한다
 * @param writingSentenceText 작문 문제의 영어 예문
 * @param writingSentenceTranslation 작문 문제의 해석
 * @param writingQuestion 작문을 유도하는 연습 질문
 * @param writingQuestionTranslation 연습 질문의 해석
 * @param writingSentenceWords 정답 단어 배열(정답 순서 유지). 언어는 quizLanguage를 따른다
 * @param writingSentenceWordChoices 정답 단어와 오답 단어를 섞은 선택지 배열. 언어는 quizLanguage를 따른다
 */
@Schema(description = "작문 연습 문제. 추가 예문 4건 중 2건이 작문 문제로 선택되며 영어와 한국어가 한 건씩이다")
public record WritingSentenceResponse(
    @Schema(description = "작문 문제의 출제 언어. EN이면 영어 문장을, KR이면 한국어 해석을 조립한다", example = "EN")
        Locale quizLanguage,
    @Schema(description = "작문 문제의 영어 예문", example = "The special effects blew my mind.")
        String writingSentenceText,
    @Schema(description = "작문 문제의 해석", example = "특수효과가 끝내줬어.") String writingSentenceTranslation,
    @Schema(description = "작문을 유도하는 연습 질문", example = "How was the movie?") String writingQuestion,
    @Schema(description = "연습 질문의 해석", example = "영화 어땠어?") String writingQuestionTranslation,
    @Schema(
            description = "정답 단어 배열(정답 순서 유지). 언어는 quizLanguage를 따른다",
            example = "[\"The\", \"special\", \"effects\", \"blew\", \"my\", \"mind\"]")
        List<String> writingSentenceWords,
    @Schema(
            description = "정답 단어와 오답 단어를 섞은 선택지 배열. 언어는 quizLanguage를 따른다",
            example =
                "[\"special\", \"blew\", \"The\", \"mind\", \"amazing\", "
                    + "\"have\", \"get\", \"effects\", \"my\"]")
        List<String> writingSentenceWordChoices) {

  /**
   * 파싱된 추가 예문을 출제 언어에 맞는 작문 문제 응답으로 변환한다.
   *
   * @param parsed 작문 문제로 변환할 파싱된 추가 예문
   * @param quizLanguage 작문 문제의 출제 언어
   * @return 작문 연습 문제 응답
   */
  public static WritingSentenceResponse from(ParsedPracticeSentence parsed, Locale quizLanguage) {
    PracticeSentenceResponse sentence = parsed.sentence();
    boolean english = quizLanguage == Locale.EN;
    return new WritingSentenceResponse(
        quizLanguage,
        sentence.sentenceText(),
        sentence.sentenceTranslation(),
        sentence.practiceQuestion(),
        sentence.practiceQuestionTranslation(),
        english ? parsed.sentenceWords() : parsed.sentenceTranslateWords(),
        english ? parsed.sentenceWordChoices() : parsed.sentenceTranslateWordChoices());
  }
}
