// AI 튜터의 기준 언어별 표시 이름을 조회한다.

package com.landit.landitbe.feature.content.repository;

import com.landit.landitbe.feature.content.domain.AiTutorLanguageVariant;
import com.landit.landitbe.shared.domain.Locale;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** AI 튜터의 기준 언어별 표시 이름을 조회한다. */
public interface AiTutorLanguageVariantRepository
    extends JpaRepository<AiTutorLanguageVariant, Long> {

  /** AI 튜터와 기준 언어가 일치하는 표시 이름을 조회한다. */
  Optional<AiTutorLanguageVariant> findByAiTutorIdAndBaseLocale(Long aiTutorId, Locale baseLocale);
}
