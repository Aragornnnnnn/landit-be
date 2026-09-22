// 스몰톡이 끝난 뒤 요약 화면의 총평(헤드라인·지난번과 비교·실수 기억 카드)을 세션마다 한 번 계산해 저장한다.

package com.landit.landitbe.feature.learning.freetalk.summary.domain;

import com.landit.landitbe.feature.learning.freetalk.feedback.domain.FreeTalkMistakePattern;
import com.landit.landitbe.feature.learning.freetalk.summary.dto.FreeTalkGrowthCard;
import com.landit.landitbe.feature.learning.freetalk.summary.dto.FreeTalkHeadline;
import com.landit.landitbe.feature.learning.freetalk.summary.dto.FreeTalkSessionMetrics;
import com.landit.landitbe.shared.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Objects;
import lombok.Getter;

/**
 * 한 스몰톡 세션의 총평이다.
 *
 * <p>헤드라인은 문구를 돌려 쓰고 비교는 직전 세션에 기대므로 조회할 때마다 계산하면 값이 바뀐다. 그래서 세션의 턴 교정이 모두 끝난 뒤 한 번 계산해 저장하고, 저장한
 * 뒤에는 어떤 값도 바꾸지 않는다. 세션마다 하나만 두며 행이 있으면 확정된 것이다.
 */
@Getter
@Entity
@Table(name = "free_talk_session_summary")
public class FreeTalkSessionSummary extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id; // 예: 4801

  @Column(name = "user_profile_id", nullable = false, updatable = false)
  private Long userProfileId; // 예: 1207

  @Column(name = "free_talk_session_id", nullable = false, updatable = false)
  private Long freeTalkSessionId; // 예: 30 (총평을 낸 세션)

  @Column(name = "session_history_id", nullable = false, updatable = false)
  private Long sessionHistoryId; // 예: 3100 (그 세션의 대화 기록)

  @Column(name = "first_session", nullable = false, updatable = false)
  private boolean firstSession; // 예: false (사용자의 첫 스몰톡이면 true)

  @Column(name = "days_since_previous", updatable = false)
  private Integer daysSincePrevious; // 예: 5 (직전 스몰톡 날부터 이번 시작 날까지). 첫 스몰톡이면 null

  @Enumerated(EnumType.STRING)
  @Column(name = "headline_trigger", nullable = false, length = 30, updatable = false)
  private FreeTalkHeadlineTrigger headlineTrigger; // 예: SPEAKING_TIME_UP

  // 예: "지난번보다 1분 24초 더 말했어요!"
  @Column(name = "headline_text", nullable = false, columnDefinition = "text", updatable = false)
  private String headlineText;

  // 예: "할 말이 그만큼 늘었다는 거예요."
  @Column(name = "headline_subline", nullable = false, columnDefinition = "text", updatable = false)
  private String headlineSubline;

  @Enumerated(EnumType.STRING)
  @Column(name = "headline_pose", nullable = false, length = 20, updatable = false)
  private FreeTalkHeadlinePose headlinePose; // 예: POINT

  @Column(name = "previous_learning_session_id", updatable = false)
  private Long previousLearningSessionId; // 예: 1201 (직전 완료 스몰톡의 학습 세션). 첫 스몰톡이면 null

  @Column(name = "previous_date", updatable = false)
  private LocalDate previousDate; // 예: 2026-09-10 (직전 스몰톡 날). 첫 스몰톡이면 null

  @Column(name = "current_speaking_ms", nullable = false, updatable = false)
  private long currentSpeakingMs; // 예: 245000

  @Column(name = "current_turn_count", nullable = false, updatable = false)
  private int currentTurnCount; // 예: 18

  @Column(name = "current_max_words_in_turn", nullable = false, updatable = false)
  private int currentMaxWordsInTurn; // 예: 23

  @Column(name = "previous_speaking_ms", nullable = false, updatable = false)
  private long previousSpeakingMs; // 예: 161000. 첫 스몰톡이면 0

  @Column(name = "previous_turn_count", nullable = false, updatable = false)
  private int previousTurnCount; // 예: 14. 첫 스몰톡이면 0

  @Column(name = "previous_max_words_in_turn", nullable = false, updatable = false)
  private int previousMaxWordsInTurn; // 예: 12. 첫 스몰톡이면 0

  @Enumerated(EnumType.STRING)
  @Column(name = "growth_pattern", length = 40, updatable = false)
  private FreeTalkMistakePattern growthPattern; // 예: TENSE. 실수 기억 카드가 없으면 null

  @Column(name = "growth_succeeded", updatable = false)
  private Boolean growthSucceeded; // 예: true (오늘은 맞게 씀). 카드가 없으면 null

  @Column(name = "growth_previous_date", updatable = false)
  private LocalDate growthPreviousDate; // 예: 2026-09-10. 카드가 없으면 null

  // 예: "I go to gym with my friend." 카드가 없으면 null
  @Column(name = "growth_previous_sentence", columnDefinition = "text", updatable = false)
  private String growthPreviousSentence;

  // 예: "go" (지난 문장에서 취소선을 그을 구절). 카드가 없거나 그 교정에 구절이 없었으면 null
  @Column(name = "growth_previous_wrong_span", columnDefinition = "text", updatable = false)
  private String growthPreviousWrongSpan;

  // 예: "I went to the gym with my friend." 카드가 없으면 null
  @Column(name = "growth_current_sentence", columnDefinition = "text", updatable = false)
  private String growthCurrentSentence;

  // 예: "went" (이번 문장에서 강조할 구절). 카드가 없거나 특정하지 못했으면 null
  @Column(name = "growth_current_span", columnDefinition = "text", updatable = false)
  private String growthCurrentSpan;

  @Column(name = "correction_count", nullable = false, updatable = false)
  private int correctionCount; // 예: 3 (확정 시점에 교정이 있는 사용자 발화 수)

  /** JPA에서 사용하는 기본 생성자다. */
  protected FreeTalkSessionSummary() {}

  private FreeTalkSessionSummary(
      long userProfileId,
      long freeTalkSessionId,
      long sessionHistoryId,
      PreviousSession previous,
      FreeTalkHeadline headline,
      FreeTalkSessionMetrics current,
      FreeTalkSessionMetrics previousMetrics,
      FreeTalkGrowthCard growth,
      int correctionCount) {
    this.userProfileId = userProfileId;
    this.freeTalkSessionId = freeTalkSessionId;
    this.sessionHistoryId = sessionHistoryId;
    this.firstSession = previous == null;
    this.daysSincePrevious = previous == null ? null : previous.daysSincePrevious();
    this.previousLearningSessionId = previous == null ? null : previous.learningSessionId();
    this.previousDate = previous == null ? null : previous.date();
    this.headlineTrigger = headline.trigger();
    this.headlineText = headline.text();
    this.headlineSubline = headline.subline();
    this.headlinePose = headline.pose();
    this.currentSpeakingMs = current.speakingMs();
    this.currentTurnCount = current.turnCount();
    this.currentMaxWordsInTurn = current.maxWordsInTurn();
    this.previousSpeakingMs = previousMetrics.speakingMs();
    this.previousTurnCount = previousMetrics.turnCount();
    this.previousMaxWordsInTurn = previousMetrics.maxWordsInTurn();
    if (growth != null) {
      this.growthPattern = growth.pattern();
      this.growthSucceeded = growth.succeeded();
      this.growthPreviousDate = growth.previousDate();
      this.growthPreviousSentence = growth.previousSentence();
      this.growthPreviousWrongSpan = growth.previousWrongSpan();
      this.growthCurrentSentence = growth.currentSentence();
      this.growthCurrentSpan = growth.currentSpan();
    }
    this.correctionCount = correctionCount;
  }

  /**
   * 사용자의 첫 스몰톡 총평을 만든다. 직전 지표는 0이고 실수 기억 카드는 없다.
   *
   * @param userProfileId 사용자 프로필 ID
   * @param freeTalkSessionId 총평을 낸 프리톡 세션 ID
   * @param sessionHistoryId 그 세션의 대화 기록 ID
   * @param headline 헤드라인
   * @param current 이번 세션 지표
   * @param correctionCount 확정 시점에 교정이 있는 사용자 발화 수
   * @return 저장할 총평
   * @throws IllegalArgumentException 헤드라인이 없거나 교정 수가 음수일 때
   */
  public static FreeTalkSessionSummary first(
      long userProfileId,
      long freeTalkSessionId,
      long sessionHistoryId,
      FreeTalkHeadline headline,
      FreeTalkSessionMetrics current,
      int correctionCount) {
    requireCommon(headline, current, correctionCount);
    return new FreeTalkSessionSummary(
        userProfileId,
        freeTalkSessionId,
        sessionHistoryId,
        null,
        headline,
        current,
        FreeTalkSessionMetrics.NONE,
        null,
        correctionCount);
  }

  /**
   * 직전 스몰톡과 비교한 총평을 만든다.
   *
   * @param userProfileId 사용자 프로필 ID
   * @param freeTalkSessionId 총평을 낸 프리톡 세션 ID
   * @param sessionHistoryId 그 세션의 대화 기록 ID
   * @param previous 직전 완료 스몰톡
   * @param headline 헤드라인
   * @param current 이번 세션 지표
   * @param previousMetrics 직전 세션 지표
   * @param growth 실수 기억 카드. 없으면 null
   * @param correctionCount 확정 시점에 교정이 있는 사용자 발화 수
   * @return 저장할 총평
   * @throws IllegalArgumentException 직전 세션·헤드라인·지표가 없거나 교정 수가 음수일 때
   */
  public static FreeTalkSessionSummary compared(
      long userProfileId,
      long freeTalkSessionId,
      long sessionHistoryId,
      PreviousSession previous,
      FreeTalkHeadline headline,
      FreeTalkSessionMetrics current,
      FreeTalkSessionMetrics previousMetrics,
      FreeTalkGrowthCard growth,
      int correctionCount) {
    Objects.requireNonNull(previous, "previous");
    Objects.requireNonNull(previousMetrics, "previousMetrics");
    requireCommon(headline, current, correctionCount);
    return new FreeTalkSessionSummary(
        userProfileId,
        freeTalkSessionId,
        sessionHistoryId,
        previous,
        headline,
        current,
        previousMetrics,
        growth,
        correctionCount);
  }

  private static void requireCommon(
      FreeTalkHeadline headline, FreeTalkSessionMetrics current, int correctionCount) {
    Objects.requireNonNull(headline, "headline");
    Objects.requireNonNull(current, "current");
    if (correctionCount < 0) {
      throw new IllegalArgumentException("교정 수는 음수일 수 없습니다.");
    }
  }

  /**
   * 총평이 비교한 직전 완료 스몰톡이다.
   *
   * @param learningSessionId 직전 스몰톡의 학습 세션 ID. 예: 1201
   * @param date 직전 스몰톡 날짜. 예: 2026-09-10
   * @param daysSincePrevious 직전 스몰톡 날부터 이번 시작 날까지의 일수. 예: 5
   */
  public record PreviousSession(long learningSessionId, LocalDate date, int daysSincePrevious) {

    /**
     * 날짜는 필수이고 일수는 음수가 될 수 없다.
     *
     * @throws IllegalArgumentException 날짜가 없거나 일수가 음수일 때
     */
    public PreviousSession {
      if (date == null || daysSincePrevious < 0) {
        throw new IllegalArgumentException("직전 스몰톡의 날짜는 필수이고 일수는 음수일 수 없습니다.");
      }
    }
  }
}
