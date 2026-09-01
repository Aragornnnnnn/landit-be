// 사용자 학습 수준에 따라 노출할 표현 난이도의 상한을 정한다.

package com.landit.landitbe.feature.content.domain;

/**
 * 사용자 학습 수준에 따라 노출할 표현 난이도의 상한을 정한다.
 *
 * <p>학습 수준과 표현 난이도는 모두 1부터 5까지의 같은 척도를 쓰지만, 둘을 그대로 맞추지 않고 두 구간으로 묶는다. 표현 데이터가 3~4급에 몰려 있어 실제로 의미 있는
 * 경계가 3과 4 사이 하나뿐이기 때문이다.
 *
 * <p>노출 규칙을 코드 한 곳에 모아 두면 마이그레이션 없이 상수만 바꿔 조정할 수 있다. 배포 없이 조정해야 할 만큼 자주 바뀌면 설정 ({@code
 * landit.expression-search})으로 옮긴다.
 */
public final class ExpressionDifficultyPolicy {

  // 학습 수준이 이 값 이하면 쉬운 표현만 노출한다.
  private static final int EASY_BUCKET_MAX_LEARNING_LEVEL = 3;

  // 쉬운 구간 사용자에게 허용하는 표현 난이도 상한.
  private static final int EASY_BUCKET_MAX_DIFFICULTY = 3;

  // 어려운 구간 사용자에게 허용하는 표현 난이도 상한. 전체 표현이 대상이다.
  private static final int FULL_MAX_DIFFICULTY = 5;

  private ExpressionDifficultyPolicy() {}

  /**
   * 학습 수준에 맞는 표현 난이도 상한을 돌려준다.
   *
   * <p>학습 수준 1~3은 난이도 1~3만, 4~5는 난이도 1~5 전부를 받는다. 학습 수준을 모르는 경우(온보딩 미완료로 값이 없거나 프로필을 읽지 못한 경우)에는
   * 후보를 좁히지 않고 전체를 대상으로 한다.
   *
   * @param learningLevel 사용자가 온보딩에서 선택한 1부터 5까지의 학습 수준. 모르면 {@code null}
   * @return 노출할 표현 난이도의 상한 (이 값 이하인 표현만 후보가 된다)
   */
  public static int maxDifficultyFor(Integer learningLevel) {
    if (learningLevel == null) {
      return FULL_MAX_DIFFICULTY;
    }
    return learningLevel <= EASY_BUCKET_MAX_LEARNING_LEVEL
        ? EASY_BUCKET_MAX_DIFFICULTY
        : FULL_MAX_DIFFICULTY;
  }
}
