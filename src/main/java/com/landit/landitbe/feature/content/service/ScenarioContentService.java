// 세션 기능에 시나리오 고정 질문 조회 경계를 제공한다.

package com.landit.landitbe.feature.content.service;

import com.landit.landitbe.feature.content.dto.NextQuestionContext;
import com.landit.landitbe.feature.content.repository.ScenarioQuestionQueryRepository;
import com.landit.landitbe.shared.domain.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 세션 기능에 시나리오 고정 질문 조회 경계를 제공한다. */
@Service
@RequiredArgsConstructor
public class ScenarioContentService {

  private final ScenarioQuestionQueryRepository scenarioQuestionQueryRepository;

  /**
   * 시나리오의 활성 고정 질문을 순서와 언어 조합으로 조회한다.
   *
   * @param scenarioId 시나리오 ID
   * @param displayOrder 질문 순서
   * @param targetLocale 학습 대상 locale
   * @param baseLocale 기준 locale
   * @return 조건에 맞는 다음 질문 컨텍스트
   */
  public Optional<NextQuestionContext> findActiveQuestion(
      long scenarioId, int displayOrder, Locale targetLocale, Locale baseLocale) {
    return scenarioQuestionQueryRepository
        .findActiveQuestion(scenarioId, displayOrder, targetLocale, baseLocale)
        .map(NextQuestionContext::from);
  }
}
