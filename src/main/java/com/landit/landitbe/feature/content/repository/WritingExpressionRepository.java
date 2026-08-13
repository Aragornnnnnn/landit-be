// Writing 표현을 시나리오 순서와 프리톡 후보 조건으로 조회한다.

package com.landit.landitbe.feature.content.repository;

import com.landit.landitbe.feature.content.domain.WritingExpression;
import com.landit.landitbe.feature.content.domain.WritingExpressionSource;
import com.landit.landitbe.shared.domain.ActiveStatus;
import com.landit.landitbe.shared.domain.Locale;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Writing 표현을 시나리오 순서와 프리톡 후보 조건으로 조회한다. */
public interface WritingExpressionRepository extends JpaRepository<WritingExpression, Long> {

  /**
   * 활성 Writing 표현을 시나리오와 로케일 기준 표시 순서로 조회한다.
   *
   * <p>표시 순서는 로케일별이므로 로케일 조건 없이는 학습 순서가 달라질 수 있다.
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
   * 표현 완료 이력을 원자적으로 처리하기 위해 활성 표현을 잠금 조회한다.
   *
   * @param id 표현 ID
   * @param status 조회할 활성 상태
   * @return 잠금을 획득한 표현. 없으면 빈 Optional
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select expression from WritingExpression expression "
          + "where expression.id = :id and expression.status = :status")
  Optional<WritingExpression> findByIdAndStatusForUpdate(Long id, ActiveStatus status);

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
   * 프리톡 추천에 사용할 공용 표현 후보 전체를 조회한다.
   *
   * @param expressionSource 표현 사용 영역
   * @param targetLocale 학습 언어 locale
   * @param baseLocale 기준 언어 locale
   * @param status 조회할 콘텐츠 상태
   * @return ID 오름차순의 공용 Writing 표현 후보 목록
   */
  @Query(
      """
      SELECT expression
      FROM WritingExpression expression
      WHERE expression.expressionSource = :expressionSource
        AND expression.targetLocale = :targetLocale
        AND expression.baseLocale = :baseLocale
        AND expression.status = :status
      ORDER BY expression.id ASC
      """)
  List<WritingExpression> findPublicExpressionCandidates(
      @Param("expressionSource") WritingExpressionSource expressionSource,
      @Param("targetLocale") Locale targetLocale,
      @Param("baseLocale") Locale baseLocale,
      @Param("status") ActiveStatus status);

  /**
   * 프리톡 세션에 연결할 공용 표현 후보를 ID와 locale로 조회한다.
   *
   * @param id Writing 표현 ID
   * @param expressionSource 표현 사용 영역
   * @param targetLocale 학습 언어 locale
   * @param baseLocale 기준 언어 locale
   * @param status 조회할 콘텐츠 상태
   * @return 조건에 맞는 공용 Writing 표현
   */
  @Query(
      """
      SELECT expression
      FROM WritingExpression expression
      WHERE expression.id = :id
        AND expression.expressionSource = :expressionSource
        AND expression.targetLocale = :targetLocale
        AND expression.baseLocale = :baseLocale
        AND expression.status = :status
      """)
  Optional<WritingExpression> findPublicExpressionCandidateById(
      @Param("id") Long id,
      @Param("expressionSource") WritingExpressionSource expressionSource,
      @Param("targetLocale") Locale targetLocale,
      @Param("baseLocale") Locale baseLocale,
      @Param("status") ActiveStatus status);
}
