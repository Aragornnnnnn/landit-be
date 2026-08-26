// 표현별 발음 평가 자산 엔티티를 저장하고 조회한다.

package com.landit.landitbe.feature.content.repository;

import com.landit.landitbe.feature.content.domain.ExpressionPronunciationAsset;
import com.landit.landitbe.shared.domain.AccentLocale;
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
}
