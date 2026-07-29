// 인증과 세션 기능에 활성 AI 튜터 조회 경계를 제공한다.

package com.landit.landitbe.feature.content.service;

import com.landit.landitbe.feature.content.domain.AiTutor;
import com.landit.landitbe.feature.content.repository.AiTutorRepository;
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

  private static final String FREE_TALK_PARTNER_DISPLAY_NAME = "Harper";

  private final AiTutorRepository aiTutorRepository;

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
   * 프리톡 AI 요청에 필요한 상대 이름과 억양을 조회한다.
   *
   * @param aiTutorId 사용할 AI 튜터 ID
   * @return AI 상대 이름과 억양
   * @throws ApiException 활성 튜터가 없을 때
   */
  @Transactional(readOnly = true)
  public FreeTalkPartner requireFreeTalkPartner(Long aiTutorId) {
    AiTutor aiTutor =
        aiTutorRepository
            .findById(aiTutorId)
            .filter(tutor -> tutor.getStatus() == ActiveStatus.ACTIVE)
            .orElseThrow(() -> new ApiException(ErrorCode.DEFAULT_AI_TUTOR_NOT_CONFIGURED));
    return new FreeTalkPartner(FREE_TALK_PARTNER_DISPLAY_NAME, aiTutor.getAccentLocale().name());
  }

  /**
   * 프리톡에 사용할 AI 상대의 이름과 억양이다.
   *
   * @param displayName 튜터 언어별 표시명과 분리된 프리톡 상대 이름
   * @param accentLocale AI 상대의 억양 locale
   */
  public record FreeTalkPartner(String displayName, String accentLocale) {}
}
