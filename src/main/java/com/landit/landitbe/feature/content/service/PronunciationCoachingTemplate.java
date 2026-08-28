// 발음 오류 유형별 코칭 문구를 조립한다.

package com.landit.landitbe.feature.content.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 발음 오류 유형별 코칭 문구를 조립한다.
 *
 * <p>AI가 준 진단 데이터(어디가 어떻게 다르게 들렸나)를 유저에게 보여줄 한국어 문구로 바꾼다. LLM 생성이 아니라 템플릿 조합이라 말투가 항상 일정하고, 문구 수정은
 * 이 클래스의 상수만 고치면 된다.
 */
@Component
public class PronunciationCoachingTemplate {

  // 한국 학습자가 자주 틀리는 음소별 조음 팁. errorTargetSpan(원어민 표기 기준)으로 찾는다.
  // 조음 위치 검수 완료 (2026-08-26). 문구 톤은 기획 확인 후 조정 가능.
  private static final Map<String, String> PHONEME_TIPS =
      Map.ofEntries(
          Map.entry("th", "혀끝을 윗니와 아랫니 사이에 살짝 내밀어 대고 바람을 내보내세요."),
          Map.entry("r", "혀를 입천장에 닿지 않게 뒤로 말아 소리 내보세요."),
          Map.entry("l", "혀끝을 윗니 뒤 잇몸에 대고 소리 내보세요."),
          Map.entry("f", "윗니를 아랫입술에 살짝 대고 바람을 내보내며 소리 내보세요."),
          Map.entry("v", "윗니를 아랫입술에 살짝 대고 성대를 울리며 소리 내보세요."),
          Map.entry("p", "두 입술을 붙였다가 터뜨리며 소리 내보세요."),
          Map.entry("b", "두 입술을 붙였다가 터뜨리며 성대를 울려보세요."),
          Map.entry("z", "혀끝을 잇몸에 붙이지 말고 가까이만 둔 채, 바람 소리에 성대 울림을 더해보세요."),
          Map.entry("j", "혀끝을 잇몸에 붙였다가 떼면서 '쥐'처럼 성대를 울리며 터뜨려보세요."),
          Map.entry("w", "입술을 동그랗게 모았다가 풀며 소리 내보세요."),
          Map.entry("s", "혀끝을 잇몸 가까이 두고 바람 새는 소리만 내보세요. 성대는 울리지 않아요."),
          Map.entry("sh", "입술을 살짝 앞으로 내밀고 '쉬'처럼 바람 소리를 내보세요."),
          Map.entry("ch", "혀끝을 잇몸에 붙였다가 떼면서 '취'처럼 터뜨려보세요."),
          Map.entry("t", "혀끝을 윗니 뒤 잇몸에 붙였다가 터뜨리며 소리 내보세요."),
          Map.entry("d", "혀끝을 윗니 뒤 잇몸에 붙였다가 터뜨리며 성대를 울려보세요."),
          Map.entry("k", "혀 뒷부분을 입천장 뒤쪽에 붙였다가 터뜨려보세요."),
          Map.entry("g", "혀 뒷부분을 입천장 뒤쪽에 붙였다가 터뜨리며 성대를 울려보세요."),
          Map.entry("n", "혀끝을 윗니 뒤 잇몸에 대고 코로 소리를 내보세요."),
          Map.entry("m", "두 입술을 다물고 코로 소리를 내보세요."),
          Map.entry("ng", "혀 뒷부분을 입천장에 대고 받침 'ㅇ'처럼 코로 소리 내보세요."),
          Map.entry("h", "목에서 바람만 살짝 내보내며 소리 내보세요."),
          Map.entry("y", "혀 가운데를 입천장 가까이 올리고 '이'에서 미끄러지듯 시작해보세요."),
          Map.entry("ee", "입꼬리를 옆으로 당기고 '이'를 길게 소리 내보세요."),
          Map.entry("i", "'이'와 '에' 사이의 짧고 힘 뺀 소리로 내보세요."),
          Map.entry("er", "혀를 살짝 뒤로 말면서 '어'를 굴리듯 소리 내보세요."));

  // 매핑에 없는 음소일 때 사용하는 기본 팁.
  private static final String DEFAULT_PHONEME_TIP = "원어민 발음을 듣고 따라 해보세요.";

  /**
   * 음소 오류 코칭 문구를 만든다.
   *
   * <p>예: targetSpan "th", userSpan "ss" → "'th'가 'ss'처럼 들렸어요. 혀끝을 윗니 뒤에 살짝 대고 소리 내보세요."
   *
   * <p>한 단어에 오류가 여러 곳이어도 span은 AI가 고른 가장 두드러진 1곳만 온다. 나머지 차이는 화면의 respelling 대조(원어민 표기 vs 유저 표기)에서
   * 드러난다.
   *
   * @param errorTargetSpan 원어민 표기에서 다르게 들린 부분
   * @param errorUserSpan 사용자 표기에서 다르게 들린 부분
   * @return 코칭 문구
   */
  public String phonemeCoaching(String errorTargetSpan, String errorUserSpan) {
    String tip = lookupTip(errorTargetSpan);
    if (errorTargetSpan == null
        || errorTargetSpan.isBlank()
        || errorUserSpan == null
        || errorUserSpan.isBlank()) {
      // AI가 상세 span 없이 음소 오류만 알려준 경우의 폴백 문구.
      return "원어민과 발음이 달라요. " + tip;
    }
    return "'%s'가 '%s'처럼 들렸어요. %s".formatted(errorTargetSpan, errorUserSpan, tip);
  }

  /**
   * 강세 오류 코칭 문구를 만든다.
   *
   * <p>예: syllables ["hik","ing"], stressIndex 0 → "원어민과 강세의 위치가 달라요. 'hik' 음절에 힘을 줘보세요!"
   *
   * @param syllables 음절 분해 배열 (자산 기준 데이터)
   * @param stressIndex 원어민 강세 음절 인덱스 (자산 기준 데이터, 무강세 기능어는 -1)
   * @return 코칭 문구
   */
  public String stressCoaching(List<String> syllables, Integer stressIndex) {
    if (syllables == null
        || stressIndex == null
        || stressIndex < 0
        || stressIndex >= syllables.size()) {
      // 기준 데이터가 불완전하거나 무강세 단어인 경우의 폴백 문구.
      return "원어민과 강세의 위치가 달라요. 원어민 발음을 듣고 힘주는 위치를 따라 해보세요!";
    }
    return "원어민과 강세의 위치가 달라요. '%s' 음절에 힘을 줘보세요!".formatted(syllables.get(stressIndex));
  }

  // 음소 팁을 대소문자 무관하게 찾는다.
  private String lookupTip(String errorTargetSpan) {
    if (errorTargetSpan == null) {
      return DEFAULT_PHONEME_TIP;
    }
    return PHONEME_TIPS.getOrDefault(errorTargetSpan.toLowerCase(Locale.ROOT), DEFAULT_PHONEME_TIP);
  }
}
