// 프리톡 추천 표현의 소유권 검증과 완료 상태 변경을 담당한다.

package com.landit.landitbe.feature.session.freetalk.expression.service;

import com.landit.landitbe.feature.session.domain.LearningSessionStatus;
import com.landit.landitbe.feature.session.dto.LearningSessionSnapshot;
import com.landit.landitbe.feature.session.freetalk.domain.FreeTalkConversationStatus;
import com.landit.landitbe.feature.session.freetalk.domain.FreeTalkSession;
import com.landit.landitbe.feature.session.freetalk.expression.domain.ExpressionGenerationStatus;
import com.landit.landitbe.feature.session.freetalk.expression.dto.FreeTalkExpressionCompletion;
import com.landit.landitbe.feature.session.freetalk.expression.repository.FreeTalkSessionExpressionRepository;
import com.landit.landitbe.feature.session.freetalk.repository.FreeTalkSessionRepository;
import com.landit.landitbe.feature.session.service.LearningSessionService;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 프리톡 추천 표현의 소유권과 완료 상태를 관리한다. */
@Service
@RequiredArgsConstructor
public class FreeTalkExpressionLearningService {
  private final FreeTalkSessionRepository freeTalkSessionRepository;
  private final LearningSessionService learningSessionService;
  private final FreeTalkSessionExpressionRepository sessionExpressionRepository;

  /**
   * 사용자가 완료한 프리톡에 연결된 추천 표현인지 검증한다.
   *
   * @param userId 사용자 ID
   * @param freeTalkSessionId 학습 세션 ID
   * @param expressionId 표현 ID
   * @return 검증된 세션 표현 식별자
   * @throws ApiException 소유자가 아니거나 완료 상태 또는 표현 연결이 유효하지 않을 때
   */
  @Transactional(readOnly = true)
  public FreeTalkExpressionCompletion validateCompletion(
      Long userId, Long freeTalkSessionId, Long expressionId) {
    FreeTalkSession freeTalkSession =
        freeTalkSessionRepository
            .findByLearningSessionId(freeTalkSessionId)
            .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
    LearningSessionSnapshot learningSession =
        learningSessionService
            .findSession(freeTalkSession.getLearningSessionId())
            .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
    if (!userId.equals(learningSession.getUserProfileId())) {
      throw new ApiException(ErrorCode.FORBIDDEN);
    }
    if (learningSession.getStatus() != LearningSessionStatus.COMPLETED
        || freeTalkSession.getConversationStatus() != FreeTalkConversationStatus.COMPLETED
        || freeTalkSession.getExpressionGenerationStatus() != ExpressionGenerationStatus.READY) {
      throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
    }
    return sessionExpressionRepository
        .findByFreeTalkSessionIdAndWritingExpressionId(freeTalkSession.getId(), expressionId)
        .map(expression -> new FreeTalkExpressionCompletion(expression.getId()))
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
  }

  /**
   * 검증된 추천 표현을 완료한다. 호출자는 검증과 콘텐츠 잠금을 같은 트랜잭션에서 수행한다.
   *
   * @param sessionExpressionId 검증된 세션 표현 ID
   * @throws ApiException 연결된 표현이 없을 때
   */
  @Transactional
  public void completeExpression(Long sessionExpressionId) {
    sessionExpressionRepository
        .findById(sessionExpressionId)
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND))
        .complete();
  }
}
