// 프리톡 세션별 개인화 표현을 저장하고 조회한다.

package com.landit.landitbe.feature.session.repository;

import com.landit.landitbe.feature.session.domain.FreeTalkSessionExpression;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 프리톡 세션별 개인화 표현을 저장하고 조회한다. */
public interface FreeTalkSessionExpressionRepository
    extends JpaRepository<FreeTalkSessionExpression, Long> {

  /** 세션에 연결된 표현을 노출 순서대로 조회한다. */
  List<FreeTalkSessionExpression> findByFreeTalkSessionIdOrderByDisplayOrderAsc(
      Long freeTalkSessionId);

  /** 세션에 연결된 모든 맞춤 표현을 삭제한다. */
  void deleteByFreeTalkSessionId(Long freeTalkSessionId);
}
