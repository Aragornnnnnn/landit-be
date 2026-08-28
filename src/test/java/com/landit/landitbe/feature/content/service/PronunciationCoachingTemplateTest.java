// 발음 코칭 문구 템플릿의 조립 규칙을 검증한다.

package com.landit.landitbe.feature.content.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/** 발음 코칭 문구 템플릿의 조립 규칙을 검증한다. */
class PronunciationCoachingTemplateTest {

  private final PronunciationCoachingTemplate template = new PronunciationCoachingTemplate();

  @Test
  void phonemeCoachingCombinesSpansWithKnownTip() {
    String coaching = template.phonemeCoaching("th", "ss");

    assertThat(coaching).isEqualTo("'th'가 'ss'처럼 들렸어요. 혀끝을 윗니와 아랫니 사이에 살짝 내밀어 대고 바람을 내보내세요.");
  }

  @Test
  void phonemeCoachingFallsBackToDefaultTipForUnknownPhoneme() {
    String coaching = template.phonemeCoaching("xx", "yy");

    assertThat(coaching).startsWith("'xx'가 'yy'처럼 들렸어요.").contains("원어민 발음을 듣고 따라 해보세요.");
  }

  @Test
  void phonemeCoachingHandlesMissingSpans() {
    String coaching = template.phonemeCoaching(null, null);

    assertThat(coaching).startsWith("원어민과 발음이 달라요.");
  }

  @Test
  void stressCoachingNamesTheCorrectSyllable() {
    String coaching = template.stressCoaching(List.of("hik", "ing"), 0);

    assertThat(coaching).isEqualTo("원어민과 강세의 위치가 달라요. 'hik' 음절에 힘을 줘보세요!");
  }

  @Test
  void stressCoachingFallsBackWhenReferenceDataIsIncomplete() {
    assertThat(template.stressCoaching(null, 0)).contains("힘주는 위치를 따라 해보세요");
    assertThat(template.stressCoaching(List.of("hik", "ing"), 5)).contains("힘주는 위치를 따라 해보세요");
  }
}
