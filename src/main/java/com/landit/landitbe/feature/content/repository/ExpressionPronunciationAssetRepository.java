// 표현별 발음 평가 자산 엔티티를 저장하고 조회한다.

package com.landit.landitbe.feature.content.repository;

import com.landit.landitbe.feature.content.domain.ExpressionPronunciationAsset;
import com.landit.landitbe.shared.domain.AccentLocale;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 표현별 발음 평가 자산 엔티티를 저장하고 조회한다. */
public interface ExpressionPronunciationAssetRepository
    extends JpaRepository<ExpressionPronunciationAsset, Long> {

  /**
   * 표현과 억양이 일치하는 발음 평가 자산을 조회한다.
   *
   * @param writingExpressionId Writing 표현 ID
   * @param accentLocale 억양 locale
   * @return 조건에 맞는 자산, 없으면 빈 Optional
   */
  Optional<ExpressionPronunciationAsset> findByWritingExpressionIdAndAccentLocale(
      Long writingExpressionId, AccentLocale accentLocale);

  /**
   * 여러 표현에 속한 발음 평가 자산을 한 번에 조회한다. 일괄 임포트의 upsert 판별에 사용한다.
   *
   * @param writingExpressionIds Writing 표현 ID 목록
   * @return 해당 표현들의 자산 목록 (모든 억양 포함)
   */
  List<ExpressionPronunciationAsset> findAllByWritingExpressionIdIn(
      Collection<Long> writingExpressionIds);

  /**
   * 전체 자산의 (표현 ID, 억양)만 가볍게 조회한다. words JSONB를 읽지 않으므로 커버리지 계산에 적합하다.
   *
   * @return 전체 자산의 표현 ID·억양 목록
   */
  List<AssetLocaleView> findAllBy();

  /** 자산의 표현 ID와 억양만 담는 조회 전용 뷰다. */
  interface AssetLocaleView {

    /** Writing 표현 ID를 반환한다. */
    Long getWritingExpressionId();

    /** 억양 locale을 반환한다. */
    AccentLocale getAccentLocale();
  }
}
