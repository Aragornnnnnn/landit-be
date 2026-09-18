// 발음 코칭 문구 템플릿의 조립 규칙을 검증한다.

package com.landit.landitbe.feature.content.expression.pronunciation.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 발음 코칭 문구 템플릿의 조립 규칙을 검증한다. */
class PronunciationCoachingTemplateTest {

  private final PronunciationCoachingTemplate template = new PronunciationCoachingTemplate();

  @DisplayName("음소 코칭은 발음 구간과 정의된 팁을 조합한다.")
  @Test
  void phonemeCoachingCombinesSpansWithKnownTip() {
    String coaching = template.phonemeCoaching("th", "ss");

    assertThat(coaching).isEqualTo("'th'가 'ss'처럼 들렸어요. 혀끝을 윗니와 아랫니 사이에 살짝 내밀어 대고 바람을 내보내세요.");
  }

  @DisplayName("알 수 없는 음소는 기본 코칭 팁을 사용한다.")
  @Test
  void phonemeCoachingFallsBackToDefaultTipForUnknownPhoneme() {
    String coaching = template.phonemeCoaching("xx", "yy");

    assertThat(coaching).startsWith("'xx'가 'yy'처럼 들렸어요.").contains("원어민 발음을 듣고 따라 해보세요.");
  }

  @DisplayName("발음 구간이 없어도 음소 코칭을 생성한다.")
  @Test
  void phonemeCoachingHandlesMissingSpans() {
    String coaching = template.phonemeCoaching(null, null);

    assertThat(coaching).startsWith("원어민과 발음이 달라요.");
  }

  @DisplayName("음절 삽입 코칭에 교정할 음절을 표시한다.")
  @Test
  void syllableInsertionCoachingNamesTargetSyllables() {
    String coaching = template.syllableInsertionCoaching(List.of("bad"));

    assertThat(coaching).isEqualTo("'으' 같은 모음 소리가 끼어들어 음절이 늘었어요. 원어민처럼 1음절(bad)로 이어서 발음해보세요!");
  }

  @DisplayName("교정할 음절이 여러 개이면 함께 표시한다.")
  @Test
  void syllableInsertionCoachingJoinsMultipleSyllables() {
    String coaching = template.syllableInsertionCoaching(List.of("hon", "est", "ly"));

    assertThat(coaching).contains("3음절(hon·est·ly)");
  }

  @DisplayName("강세 코칭에 올바른 강세 음절을 표시한다.")
  @Test
  void stressCoachingNamesTheCorrectSyllable() {
    String coaching = template.stressCoaching(List.of("hik", "ing"), 0);

    assertThat(coaching).isEqualTo("원어민과 강세의 위치가 달라요. 'hik' 음절에 힘을 줘보세요!");
  }

  @DisplayName("강세 기준 데이터가 부족하면 기본 코칭을 사용한다.")
  @Test
  void stressCoachingFallsBackWhenReferenceDataIsIncomplete() {
    assertThat(template.stressCoaching(null, 0)).contains("힘주는 위치를 따라 해보세요");
    assertThat(template.stressCoaching(List.of("hik", "ing"), 5)).contains("힘주는 위치를 따라 해보세요");
  }
}
