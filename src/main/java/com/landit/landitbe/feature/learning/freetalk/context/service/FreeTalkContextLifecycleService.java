// 요약 작업이 사용자 탈퇴와 세션 완료 이후에 저장되지 않도록 상태를 잠근다.

package com.landit.landitbe.feature.learning.freetalk.context.service;

import com.landit.landitbe.feature.learning.conversation.domain.LearningSessionStatus;
import com.landit.landitbe.feature.learning.conversation.service.LearningSessionService;
import com.landit.landitbe.feature.learning.freetalk.domain.FreeTalkConversationStatus;
import com.landit.landitbe.feature.learning.freetalk.repository.FreeTalkSessionRepository;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 사용자·프리톡·학습 세션의 잠금을 호출 트랜잭션까지 유지한다. */
@Service
@RequiredArgsConstructor
public class FreeTalkContextLifecycleService {
  private final UserProfileService profiles;
  private final FreeTalkSessionRepository sessions;
  private final LearningSessionService learningSessions;

  /**
   * 탈퇴 및 완료와 직렬화하고 진행 중인 소유 세션인지 확인한다.
   *
   * @param userId 소유 사용자 ID
   * @param learningSessionId 학습 세션 ID
   * @param freeTalkSessionId 프리톡 세션 ID
   * @return 세 상태를 잠그고 유효성을 확인했으면 true
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public boolean lockActive(long userId, long learningSessionId, long freeTalkSessionId) {
    if (!profiles.lockActive(userId)) {
      return false;
    }
    var session = sessions.findByLearningSessionIdForUpdate(learningSessionId);
    if (session.isEmpty()
        || session.get().getId() != freeTalkSessionId
        || session.get().getConversationStatus() != FreeTalkConversationStatus.IN_PROGRESS) {
      return false;
    }
    return learningSessions
        .lockOwnedSnapshot(learningSessionId, userId)
        .filter(value -> value.getStatus() == LearningSessionStatus.IN_PROGRESS)
        .isPresent();
  }
}
