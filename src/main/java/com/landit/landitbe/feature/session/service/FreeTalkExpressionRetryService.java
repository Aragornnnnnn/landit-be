// 실패한 프리톡 표현 생성 작업을 다시 준비 상태로 전환한다.

package com.landit.landitbe.feature.session.service;

import com.landit.landitbe.feature.session.domain.FreeTalkConversationStatus;
import com.landit.landitbe.feature.session.domain.FreeTalkSession;
import com.landit.landitbe.feature.session.domain.LearningSession;
import com.landit.landitbe.feature.session.domain.LearningSessionStatus;
import com.landit.landitbe.feature.session.dto.FreeTalkExpressionRetryResponse;
import com.landit.landitbe.feature.session.repository.FreeTalkSessionRepository;
import com.landit.landitbe.feature.session.repository.LearningSessionRepository;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 실패한 프리톡 표현 생성 작업을 다시 준비 상태로 전환한다. */
@RequiredArgsConstructor
@Service
public class FreeTalkExpressionRetryService {

  private final LearningSessionRepository learningSessionRepository;
  private final FreeTalkSessionRepository freeTalkSessionRepository;

  /**
   * 사용자가 소유한 실패 세션의 표현 생성을 재시도 상태로 전환한다.
   *
   * @param userId 재시도를 요청한 사용자 ID
   * @param learningSessionId 재시도할 학습 세션 ID
   * @return 재시도 요청 뒤의 표현 생성 상태
   * @throws ApiException 세션이 없거나 소유하지 않았거나 재시도할 수 없는 상태일 때
   */
  @Transactional
  public FreeTalkExpressionRetryResponse retry(long userId, long learningSessionId) {
    LearningSession learningSession =
        learningSessionRepository
            .findByIdAndUserProfileIdForUpdate(learningSessionId, userId)
            .orElseGet(
                () -> {
                  if (learningSessionRepository.existsById(learningSessionId)) {
                    throw new ApiException(ErrorCode.FORBIDDEN);
                  }
                  throw new ApiException(ErrorCode.SESSION_NOT_FOUND);
                });
    FreeTalkSession freeTalkSession =
        freeTalkSessionRepository
            .findByLearningSessionIdForUpdate(learningSessionId)
            .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));
    if (learningSession.getStatus() != LearningSessionStatus.COMPLETED
        || freeTalkSession.getConversationStatus() != FreeTalkConversationStatus.COMPLETED) {
      throw new ApiException(ErrorCode.CONFLICT);
    }
    try {
      freeTalkSession.retryExpressionGeneration();
    } catch (IllegalStateException exception) {
      throw new ApiException(ErrorCode.CONFLICT);
    }
    return new FreeTalkExpressionRetryResponse(
        learningSessionId, freeTalkSession.getExpressionGenerationStatus());
  }
}
