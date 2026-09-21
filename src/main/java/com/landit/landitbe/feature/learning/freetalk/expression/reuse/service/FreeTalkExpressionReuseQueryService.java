// 저장된 표현 재사용 기록을 지난 스몰톡 상세와 요약이 쓰는 모양으로 읽는다.

package com.landit.landitbe.feature.learning.freetalk.expression.reuse.service;

import com.landit.landitbe.feature.learning.freetalk.expression.domain.ExpressionGenerationStatus;
import com.landit.landitbe.feature.learning.freetalk.expression.reuse.domain.FreeTalkExpressionReuse;
import com.landit.landitbe.feature.learning.freetalk.expression.reuse.dto.FreeTalkExpressionReuseSummary;
import com.landit.landitbe.feature.learning.freetalk.expression.reuse.dto.FreeTalkReusedExpression;
import com.landit.landitbe.feature.learning.freetalk.expression.reuse.repository.FreeTalkExpressionReuseRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 표현 재사용 기록을 읽는다. 세션 소유권은 호출하는 쪽이 먼저 확인한다.
 *
 * <p>기록에 저장된 값만 쓴다. 표현·시나리오·세션 테이블의 지금 값을 읽지 않으므로 같은 세션은 언제 조회해도 같은 결과다.
 */
@RequiredArgsConstructor
@Service
public class FreeTalkExpressionReuseQueryService {

  private final FreeTalkExpressionReuseRepository reuseRepository;

  /**
   * 대화 보기에서 발화에 밑줄을 그을 재사용 표현을 세션 단위로 한 번에 읽는다.
   *
   * @param freeTalkSessionId 프리톡 세션 ID
   * @return 발화 ID별 재사용 표현. 한 발화에 여러 표현을 썼으면 먼저 기록된 하나만 담는다
   */
  @Transactional(readOnly = true)
  public Map<Long, FreeTalkReusedExpression> findFirstByMessageId(long freeTalkSessionId) {
    Map<Long, FreeTalkReusedExpression> reusedByMessageId = new LinkedHashMap<>();
    for (FreeTalkExpressionReuse reuse :
        reuseRepository.findByFreeTalkSessionIdOrderByIdAsc(freeTalkSessionId)) {
      reusedByMessageId.putIfAbsent(
          reuse.getSessionHistoryMessageId(), FreeTalkReusedExpression.of(reuse));
    }
    return reusedByMessageId;
  }

  /**
   * 요약 화면의 표현 재사용 카드를 조회한다.
   *
   * <p>재사용 기록은 세션 종료 후 표현 작업이 추천과 함께 저장한다. 그 작업이 아직 끝나지 않았으면 기다리는 중으로 알린다. 작업이 실패로 끝났으면 기록 없이 끝난
   * 것으로 본다.
   *
   * @param freeTalkSessionId 프리톡 세션 ID
   * @param expressionGenerationStatus 그 세션의 표현 작업 상태
   * @return 기다리는 중이거나, 다시 쓴 표현 목록(없으면 빈 목록)
   */
  @Transactional(readOnly = true)
  public FreeTalkExpressionReuseSummary findSummary(
      long freeTalkSessionId, ExpressionGenerationStatus expressionGenerationStatus) {
    List<FreeTalkExpressionReuse> reuses =
        reuseRepository.findByFreeTalkSessionIdOrderByIdAsc(freeTalkSessionId);
    if (reuses.isEmpty() && expressionGenerationStatus == ExpressionGenerationStatus.PREPARING) {
      return FreeTalkExpressionReuseSummary.waiting();
    }
    // 같은 표현을 여러 발화에서 썼어도 카드에는 처음 쓴 한 번만 보여 준다.
    Map<Long, FreeTalkExpressionReuseSummary.Item> itemsByExpressionId = new LinkedHashMap<>();
    for (FreeTalkExpressionReuse reuse : reuses) {
      itemsByExpressionId.putIfAbsent(
          reuse.getWritingExpressionId(), FreeTalkExpressionReuseSummary.Item.of(reuse));
    }
    return new FreeTalkExpressionReuseSummary(false, List.copyOf(itemsByExpressionId.values()));
  }
}
