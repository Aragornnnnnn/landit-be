// 공통 학습 세션의 중도 종료 API를 처리한다.

package com.landit.landitbe.feature.learning.conversation;

import com.landit.landitbe.feature.learning.conversation.docs.LearningSessionControllerDocs;
import com.landit.landitbe.feature.learning.conversation.service.LearningSessionService;
import com.landit.landitbe.shared.response.ApiResponse;
import com.landit.landitbe.shared.security.AuthUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** 공통 학습 세션의 중도 종료 API를 처리한다. */
@RequiredArgsConstructor
@RestController
public class LearningSessionController implements LearningSessionControllerDocs {
  private final LearningSessionService learningSessionService;

  /** {@inheritDoc} */
  @Override
  @PatchMapping("/api/v1/sessions/{sessionId}/end")
  public ApiResponse<Void> endSession(
      @AuthenticationPrincipal AuthUserPrincipal principal, @PathVariable Long sessionId) {
    learningSessionService.endSession(principal.userId(), sessionId);
    return ApiResponse.success(null);
  }
}
