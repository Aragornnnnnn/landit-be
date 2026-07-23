// 세션 기능에 시나리오 고정 질문 조회 경계를 제공한다.

package com.landit.landitbe.feature.content.service;

import com.landit.landitbe.feature.content.repository.ScenarioQuestionQueryRepository;
import com.landit.landitbe.feature.content.repository.projection.ScenarioQuestionProjection;
import com.landit.landitbe.shared.domain.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 세션 기능에 시나리오 고정 질문 조회 경계를 제공한다. */
@Service
@RequiredArgsConstructor
public class ScenarioContentService {

  private final ScenarioQuestionQueryRepository scenarioQuestionQueryRepository;

  /** 시나리오의 활성 고정 질문을 순서와 언어 조합으로 조회한다. */
  public Optional<ScenarioQuestionProjection> findActiveQuestion(
      long scenarioId, int displayOrder, Locale targetLocale, Locale baseLocale) {
    return scenarioQuestionQueryRepository.findActiveQuestion(
        scenarioId, displayOrder, targetLocale, baseLocale);
  }
}
