// 시나리오에 속한 Writing 표현을 학습 순서 기준으로 조회한다.

package com.landit.landitbe.feature.content.repository;

import com.landit.landitbe.feature.content.domain.WritingExpression;
import com.landit.landitbe.shared.domain.ActiveStatus;
import com.landit.landitbe.shared.domain.Locale;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** 시나리오에 속한 Writing 표현을 학습 순서 기준으로 조회한다. */
public interface WritingExpressionRepository extends JpaRepository<WritingExpression, Long> {

  /**
   * 특정 시나리오에서 지정한 locale 조합의 활성 Writing 표현을 표시 순서 오름차순으로 조회한다. (displayOrder 시퀀스는 locale 조합별로
   * 존재하므로, locale 필터 없이는 여러 언어 표현이 섞이고 해금 순서가 깨진다)
   *
   * @param scenarioId 표현이 속한 시나리오 ID
   * @param targetLocale 학습 언어 locale
   * @param baseLocale 기준 언어 locale
   * @param status 조회할 콘텐츠 상태
   * @return 표시 순서 오름차순의 Writing 표현 목록
   */
  List<WritingExpression>
      findByScenarioIdAndTargetLocaleAndBaseLocaleAndStatusOrderByDisplayOrderAsc(
          Long scenarioId, Locale targetLocale, Locale baseLocale, ActiveStatus status);

  /**
   * 특정 상태의 Writing 표현을 PK로 조회한다.
   *
   * @param id Writing 표현 ID
   * @param status 조회할 콘텐츠 상태
   * @return 조건에 맞는 표현의 Optional
   */
  Optional<WritingExpression> findByIdAndStatus(Long id, ActiveStatus status);

  /**
   * 학습 언어와 기준 언어에 맞는 활성 Writing 표현을 조회한다.
   *
   * @param targetLocale 학습 언어 locale
   * @param baseLocale 기준 언어 locale
   * @param status 조회할 콘텐츠 상태
   * @return 조건에 맞는 Writing 표현 목록
   */
  List<WritingExpression> findByTargetLocaleAndBaseLocaleAndStatus(
      Locale targetLocale, Locale baseLocale, ActiveStatus status);

  /**
   * 프리톡 표현 추천에 사용할 활성 표현 후보를 생성 순서로 제한 조회한다.
   *
   * @param targetLocale 학습 언어 locale
   * @param baseLocale 기준 언어 locale
   * @param status 조회할 콘텐츠 상태
   * @param pageable 후보 개수와 페이지 조건
   * @return ID 오름차순의 Writing 표현 후보 목록
   */
  List<WritingExpression> findByTargetLocaleAndBaseLocaleAndStatusOrderByIdAsc(
      Locale targetLocale, Locale baseLocale, ActiveStatus status, Pageable pageable);

  /**
   * 프리톡 추천에 사용할 공용 표현 후보만 조회한다.
   *
   * @param targetLocale 학습 언어 locale
   * @param baseLocale 기준 언어 locale
   * @param status 조회할 콘텐츠 상태
   * @param pageable 후보 개수와 페이지 조건
   * @return ID 오름차순의 공용 Writing 표현 후보 목록
   */
  List<WritingExpression>
      findByTargetLocaleAndBaseLocaleAndStatusAndOwnerUserProfileIdIsNullOrderByIdAsc(
          Locale targetLocale, Locale baseLocale, ActiveStatus status, Pageable pageable);
}
