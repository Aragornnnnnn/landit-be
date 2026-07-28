// 프리톡 공통 표현 학습 콘텐츠를 저장하고 조회한다.

package com.landit.landitbe.feature.session.repository;

import com.landit.landitbe.feature.session.domain.FreeTalkExpression;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 프리톡 공통 표현 학습 콘텐츠를 저장하고 조회한다. */
public interface FreeTalkExpressionRepository extends JpaRepository<FreeTalkExpression, Long> {

  /** Writing 표현을 참조하는 프리톡 공통 표현을 조회한다. */
  Optional<FreeTalkExpression> findByWritingExpressionId(Long writingExpressionId);
}
