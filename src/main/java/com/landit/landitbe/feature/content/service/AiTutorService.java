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

  private final AiTutorRepository aiTutorRepository;

  /** 언어 조건에 맞는 활성 AI 튜터가 정확히 하나일 때 해당 ID를 반환한다. */
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
}
