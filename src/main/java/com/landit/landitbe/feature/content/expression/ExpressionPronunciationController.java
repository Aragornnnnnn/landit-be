// 문장 발화 발음 평가 요청을 처리한다.

package com.landit.landitbe.feature.content.expression;

import com.landit.landitbe.feature.content.expression.docs.ExpressionPronunciationControllerDocs;
import com.landit.landitbe.feature.content.expression.dto.PronunciationAnalysisResponse;
import com.landit.landitbe.feature.content.expression.service.ExpressionPronunciationService;
import com.landit.landitbe.shared.response.ApiResponse;
import com.landit.landitbe.shared.security.AuthUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 문장 발화 발음 평가 요청을 처리한다. */
@RequiredArgsConstructor
@RestController
public class ExpressionPronunciationController implements ExpressionPronunciationControllerDocs {

  private final ExpressionPronunciationService expressionPronunciationService;

  /** {@inheritDoc} */
  @Override
  @PostMapping("/api/v1/expressions/{expressionId}/pronunciation/sentence-analysis")
  public ApiResponse<PronunciationAnalysisResponse> analyzeSentence(
      @AuthenticationPrincipal AuthUserPrincipal principal,
      @PathVariable Long expressionId,
      @RequestPart("audio") MultipartFile audio) {
    return ApiResponse.success(
        expressionPronunciationService.analyze(principal.userId(), expressionId, audio));
  }
}
