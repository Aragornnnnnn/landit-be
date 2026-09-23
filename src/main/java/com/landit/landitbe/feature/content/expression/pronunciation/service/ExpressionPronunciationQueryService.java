// 표현 발음 자산의 음성 조회 계약을 제공한다.

package com.landit.landitbe.feature.content.expression.pronunciation.service;

import com.landit.landitbe.feature.content.expression.pronunciation.domain.ExpressionPronunciationAsset;
import com.landit.landitbe.feature.content.expression.pronunciation.dto.ExpressionAudio;
import com.landit.landitbe.feature.content.expression.pronunciation.repository.ExpressionPronunciationAssetRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 표현 발음 자산의 음성 조회 계약을 제공한다. */
@Service
@RequiredArgsConstructor
public class ExpressionPronunciationQueryService {

  private final ExpressionPronunciationAssetRepository pronunciationAssetRepository;
  private final UserAccentLocaleResolver accentLocaleResolver;

  /**
   * 사용자의 억양에 맞는 준비된 발음 음성을 조회한다.
   *
   * @param userId 요청 사용자
   * @param expressionId 표현 ID
   * @return 준비된 음성 주소. 미준비인 각 음성은 null
   */
  @Transactional(readOnly = true)
  public ExpressionAudio findAudio(Long userId, Long expressionId) {
    return findReadyAsset(userId, expressionId)
        .map(
            asset ->
                new ExpressionAudio(asset.getSentenceAudioUrl(), asset.getExpressionAudioUrl()))
        .orElseGet(() -> new ExpressionAudio(null, null));
  }

  /**
   * 사용자의 목표 억양에 맞는, TTS까지 완성된 발음 자산을 찾는다.
   *
   * <p>자산이 아직 없거나 TTS 미완성이면 빈 값 — 표현 981개의 자산을 단계적으로 채우는 동안 학습 시작 화면이 깨지지 않게 하기 위한 의도된 동작이다 (앱은
   * URL이 null이면 해당 파트를 숨긴다). 표현 TTS(expressionAudioUrl)는 완성된 자산이라도 패턴형 표현(발화 불가)이면 null이다.
   *
   * @param userId 사용자 ID
   * @param expressionId Writing 표현 ID
   * @return TTS까지 완성된 발음 자산. 없으면 빈 값
   */
  private Optional<ExpressionPronunciationAsset> findReadyAsset(Long userId, Long expressionId) {
    return accentLocaleResolver
        .tryResolve(userId)
        .flatMap(
            accentLocale ->
                pronunciationAssetRepository.findByWritingExpressionIdAndAccentLocale(
                    expressionId, accentLocale))
        .filter(ExpressionPronunciationAsset::hasTts);
  }
}
