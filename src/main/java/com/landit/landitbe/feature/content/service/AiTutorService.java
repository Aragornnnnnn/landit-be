// 인증과 세션 기능에 활성 AI 튜터 조회 경계를 제공한다.

package com.landit.landitbe.feature.content.service;

import com.landit.landitbe.feature.content.domain.AiTutor;
import com.landit.landitbe.feature.content.domain.AiTutorLanguageVariant;
import com.landit.landitbe.feature.content.domain.TtsVoice;
import com.landit.landitbe.feature.content.domain.TtsVoiceGender;
import com.landit.landitbe.feature.content.domain.TtsVoiceProvider;
import com.landit.landitbe.feature.content.repository.AiTutorLanguageVariantRepository;
import com.landit.landitbe.feature.content.repository.AiTutorRepository;
import com.landit.landitbe.feature.content.repository.TtsVoiceRepository;
import com.landit.landitbe.shared.domain.AccentLocale;
import com.landit.landitbe.shared.domain.ActiveStatus;
import com.landit.landitbe.shared.domain.Locale;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 인증과 세션 기능에 활성 AI 튜터 조회 경계를 제공한다. */
@Service
@RequiredArgsConstructor
public class AiTutorService {

  private final AiTutorRepository aiTutorRepository;
  private final AiTutorLanguageVariantRepository aiTutorLanguageVariantRepository;
  private final TtsVoiceRepository ttsVoiceRepository;

  /**
   * 언어 조건에 맞는 활성 AI 튜터가 정확히 하나일 때 해당 ID를 반환한다.
   *
   * @param accentLocale AI 튜터 억양 locale
   * @param targetLocale 학습 대상 locale
   * @return 유일한 활성 AI 튜터 ID
   * @throws ApiException 활성 후보가 정확히 하나가 아닐 때
   */
  @Transactional(readOnly = true)
  public Long requireSingleActiveTutorId(AccentLocale accentLocale, Locale targetLocale) {
    List<AiTutor> candidates =
        aiTutorRepository.findAllByAccentLocaleAndTargetLocaleAndStatus(
            accentLocale, targetLocale, ActiveStatus.ACTIVE);
    if (candidates.size() != 1) {
      throw new ApiException(ErrorCode.DEFAULT_AI_TUTOR_NOT_CONFIGURED);
    }
    return candidates.getFirst().getId();
  }

  /**
   * 프리톡 AI 요청과 응답에 필요한 상대 이름 및 TTS 설정을 조회한다.
   *
   * @param aiTutorId 사용할 AI 튜터 ID
   * @param baseLocale 사용자 기준 언어
   * @return AI 상대 이름, 억양과 TTS 설정
   * @throws ApiException 활성 튜터, 언어별 이름 또는 TTS 음성이 없을 때
   */
  @Transactional(readOnly = true)
  public FreeTalkPartner requireFreeTalkPartner(Long aiTutorId, Locale baseLocale) {
    AiTutor aiTutor =
        aiTutorRepository
            .findById(aiTutorId)
            .filter(tutor -> tutor.getStatus() == ActiveStatus.ACTIVE)
            .orElseThrow(() -> new ApiException(ErrorCode.DEFAULT_AI_TUTOR_NOT_CONFIGURED));
    AiTutorLanguageVariant languageVariant =
        aiTutorLanguageVariantRepository
            .findByAiTutorIdAndBaseLocale(aiTutorId, baseLocale)
            .orElseThrow(() -> new ApiException(ErrorCode.DEFAULT_AI_TUTOR_NOT_CONFIGURED));
    TtsVoice ttsVoice =
        ttsVoiceRepository
            .findByIdAndStatus(aiTutor.getFreeTalkTtsVoiceId(), ActiveStatus.ACTIVE)
            .orElseThrow(() -> new ApiException(ErrorCode.DEFAULT_AI_TUTOR_NOT_CONFIGURED));
    return new FreeTalkPartner(
        languageVariant.getDisplayName(),
        aiTutor.getAccentLocale().name(),
        ttsVoice.getProvider(),
        ttsVoice.getModel(),
        ttsVoice.getProviderVoiceId(),
        ttsVoice.getGender());
  }

  /**
   * 프리톡에 사용할 AI 상대의 이름, 억양, TTS 음성 설정이다.
   *
   * @param displayName 기준 언어로 표시할 AI 상대 이름
   * @param accentLocale AI 상대의 억양 locale
   * @param ttsVoiceProvider TTS 제공자
   * @param ttsVoiceModel TTS 모델명
   * @param ttsVoiceProviderVoiceId 제공자 음성 ID
   * @param ttsVoiceGender 음성 성별
   */
  public record FreeTalkPartner(
      String displayName,
      String accentLocale,
      TtsVoiceProvider ttsVoiceProvider,
      String ttsVoiceModel,
      String ttsVoiceProviderVoiceId,
      TtsVoiceGender ttsVoiceGender) {}
}
